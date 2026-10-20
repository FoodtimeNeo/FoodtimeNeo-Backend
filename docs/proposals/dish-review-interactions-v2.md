# 菜品点评点赞与回复：高频读取表结构 V2

此稿已由 [V3 审查稿](dish-review-interactions-v3.md) 替代。V3 增加回复点赞及菜品实时评分，取消每日评分刷新；两份 SQL 不可叠加执行。

基于 2026-10-20 检查的后端 dev（f9b5d78）、Web develop（439d4a9）、Android dev（de351f2）。

本文件是设计方案，配套 SQL 为 [dish-review-interactions-v2.sql](dish-review-interactions-v2.sql)。没有部署数据库，也没有改动现有接口。SQL 不在 Flyway 自动迁移目录；它要求后端配套改动后才可发布。

## 范围及优化方向

- 点赞对象是菜品的一级点评；回复不带评分。
- 支持回复一级点评或同一点评下的另一条回复，页面保持两层展示。
- 评论列表一次返回计数和本人点赞状态，不逐条请求计数、不轮询每条点评。
- 常规读取不执行点赞或回复明细的 COUNT；计数在写入事务内维护。
- 将热更新计数独立放在一张窄表，减少对正文行的更新，也不触发主点评 updated_at 的修改。
- 一条点评的并发计数更新仍需串行更新同一统计行；本方案优化高频读取，不承诺无限写入吞吐。

## 表结构

保留现有 foodtime.dish_reviews 与 dish_review_submissions，新增下列三张表。

### dish_review_stats：互动统计

| 字段 | 类型 | 约束 / 含义 |
| --- | --- | --- |
| review_id | UUID | 主键；外键关联 dish_reviews.id；与点评一对一 |
| like_count | BIGINT | 非空、默认 0、非负；点赞明细数 |
| reply_count | BIGINT | 非空、默认 0、非负；审核通过且未删除的回复数 |
| counter_version | BIGINT | 非空、默认 0、非负；每次计数变化递增 |
| updated_at | TIMESTAMPTZ | 非空；统计更新时间 |

只保留 review_id 主键索引，不索引计数、版本或更新时间。初始 fillfactor=80，用页内空间换取 HOT 更新的机会，后续按压测调整。它不保证每次更新都能走 HOT。

创建点评时由 AFTER INSERT 触发器初始化统计行；审核 UPSERT 更新已有点评时，不重置计数。当前 V14 没有菜品点评互动明细，因此现有点评回填为零；若未来从已有互动数据迁入，应改为聚合回填。

### dish_review_likes：点赞明细

| 字段 | 类型 | 约束 / 含义 |
| --- | --- | --- |
| review_id | UUID | 非空，外键关联正式点评 |
| user_id | UUID | 非空，外键关联用户 |
| created_at | TIMESTAMPTZ | 非空，默认 now() |

复合主键 (review_id,user_id) 防重复，同时支持对一页点评查本人点赞状态。暂不提供“我的所有点赞”查询，因此不另建 user_id 开头的反向索引。

点赞用 INSERT ... ON CONFLICT DO NOTHING；取消用 DELETE。实际插入/删除后由触发器增减统计；重复点赞或取消不会触发计数变化。禁止 UPDATE 点赞行。

### dish_review_replies：回复

| 字段 | 类型 | 约束 / 含义 |
| --- | --- | --- |
| id | UUID | 主键，服务端生成 |
| review_id | UUID | 非空，所属一级点评外键 |
| reply_to_id | UUID | 可空；为空表示直接回复一级点评 |
| user_id | UUID | 非空，作者外键 |
| content | TEXT | 非空；去首尾空格后 1～500 字符；服务端仍需先 strip 和校验 |
| status | VARCHAR(20) | pending / approved / rejected，默认 pending |
| reviewed_by_user_id | UUID | 可空，人工审核人外键 |
| reviewed_by_ai_model | TEXT | 可空，AI 审核模型 |
| reviewed_at | TIMESTAMPTZ | 审核时间；最终状态必须填写 |
| review_reason | TEXT | 可空，审核说明 |
| created_at | TIMESTAMPTZ | 非空，提交时间，不随审核更新 |
| updated_at | TIMESTAMPTZ | 非空，更新时间，复用现有触发器 |
| deleted_at | TIMESTAMPTZ | 可空，软删除时间 |

约束包括：

- UNIQUE(review_id,id) 配合 FOREIGN KEY(review_id,reply_to_id)，保证目标回复位于同一楼。
- 禁止回复自身。服务端只允许引用已有、已通过、未删除的回复；作者、归属、目标和正文提交后不可修改。不可通过多行直接导入创建循环关系。
- pending 不填写审核人/模型和审核时间；最终状态填写审核时间，并且人工审核人和 AI 模型恰好选一项。
- 最终审核状态不可修改；删除用于下架，不把 approved 改成 rejected。已删除回复不可恢复或继续审核。
- 不冗余 dish_id、被回复用户 ID、昵称、头像；通过 review_id 和 reply_to_id 关联读取。

索引：

| 索引 | 用途 |
| --- | --- |
| 主键 id | 单条读取、审核、删除 |
| 唯一 (review_id,id) | 同楼回复外键 |
| (review_id,created_at ASC,id ASC)，仅 approved 且未删除 | 公开楼内游标分页 |
| (created_at ASC,id ASC)，仅 pending 且未删除 | 审核队列 |
| (user_id,created_at DESC,id ASC)，仅未删除 | 本人回复及审核状态 |
| (review_id,reply_to_id)，仅目标非空 | 目标关联及外键检查 |

公开查询固定使用 status='approved' AND deleted_at IS NULL；审核队列使用 pending 条件，以匹配部分索引。索引不 INCLUDE 正文，避免放大存储和写入成本。

## 高频读取方式

先按现有分页取出一页主点评，再关联 stats 与“本人点赞”的单条明细。下列为查询形态；实际 SQL 的身份来自服务端会话，还需合并现有菜品、档口、食堂可用性检查和作者查询。

```sql
WITH review_page AS (
    SELECT id, user_id, rating, content, created_at, updated_at
    FROM foodtime.dish_reviews
    WHERE dish_id = :dish_id AND deleted_at IS NULL
    ORDER BY created_at DESC, id ASC
    LIMIT :page_size OFFSET :offset
)
SELECT r.*, s.like_count, s.reply_count, s.counter_version,
       (l.user_id IS NOT NULL) AS liked_by_me
FROM review_page r
JOIN foodtime.dish_review_stats s ON s.review_id = r.id
LEFT JOIN foodtime.dish_review_likes l
    ON l.review_id = r.id AND l.user_id = :session_user_id
ORDER BY r.created_at DESC, r.id ASC;
```

stats 与 likes 均通过主键关联，每条点评最多匹配一条记录。响应继续保留原评论字段，新增 likeCount、likedByMe、replyCount、counterVersion。不要把未经分页的点评正文、全部点赞和全部回复直接联接，否则可能放大中间结果。

回复在用户展开时加载，独立分页，不在首屏加载全部楼内内容。首次按 created_at,id 升序取 size+1 条；后续加条件：

```sql
AND (created_at,id) > (:last_created_at,:last_id)
```

replyCount 从 stats 读取，无需为了回复分页再 COUNT。响应包含 hasMore 和 nextCursor。主点评的 page/size 接口可先保留兼容；若以后新增游标，因其排序为时间降序、ID 升序，应使用对应混合方向的条件，不能照抄回复的元组比较。

## 写入事务及锁顺序

所有点赞、取消、回复提交、审核通过以及回复删除入口统一使用：

1. 校验会话、权限、限流。
2. 在同一事务内锁定主点评及其目录上下文：SELECT ... FOR SHARE OF review,dish,stall,hall。点赞、新增回复和审核通过要求它们均可见；取消点赞和删除本人回复可清理不可见历史，仍须检查归属及目标存在。
3. 若有目标回复，检查它同楼、已通过且未删除；按稳定 ID 顺序锁定需要检查/修改的回复，避免彼此反向加锁。只检查的目标可用 FOR SHARE，需要修改的回复从一开始使用 FOR UPDATE，避免先共享锁后升级。
4. 执行明细 INSERT / DELETE 或回复审核、软删除 UPDATE；数据库触发器自动更新统计行。
5. 写审核日志或应用操作日志，读取最终计数和本人点赞状态，提交事务后返回。

业务代码禁止再次手工增减计数，也不先读取旧数再覆盖。所有操作以相同顺序锁定资源；跨多条点评的后台批处理按 review_id 排序。遇到死锁或事务序列化失败，回滚整笔事务并作有界重试。

审核队列当前实现先锁投稿；接入回复时应专门调整回复路径，先读取所属点评 ID，再按上述顺序加锁，并在拿到锁后重新验证状态。不要沿用先锁回复再锁主点评的相反顺序。

| 操作 | like_count | reply_count |
| --- | --- | --- |
| 实际插入点赞 | +1 | 不变 |
| 重复点赞 | 不变 | 不变 |
| 实际删除点赞 | -1 | 不变 |
| 重复取消 | 不变 | 不变 |
| 新建 pending 回复、驳回回复 | 不变 | 不变 |
| pending 变为 approved | 不变 | +1 |
| 首次软删除 approved 回复 | 不变 | -1 |
| 删除 pending 回复、重复删除回复 | 不变 | 不变 |

触发器按实际行变化维护计数，原操作与计数一同提交或回滚；统计缺失、计数下溢直接使操作失败，不用 GREATEST(0,...) 掩盖错误。审核更新应带 WHERE status='pending' AND deleted_at IS NULL，防止重复决策。客户端按每条点评的 counterVersion 忽略较旧的计数响应；它不能代替本人点赞状态的请求排序，客户端仍需对同一点评的点赞操作串行化。

## 删除、重新点评与评分

- 主点评增加 deleted_at，公开列表、个人列表与计数查询按业务过滤已删除记录；管理员可提供查看历史的单独条件。
- 删除主点评只设置 deleted_at，不批量删除整楼明细，也不清零历史计数。这样删除动作不会随楼内回复数量线性增长；公开回复接口必须先检查主点评可见性。
- 删除某条回复隐藏正文，后续回复继续保留，目标摘要显示“该回复已删除”。普通公开数据不得返回已删除回复正文。
- 正常重新点评保留当前“更新同一条正式点评 ID”的行为，保留该点评的点赞和讨论。此规则需向前端明确；若要按正文版本重新归属点赞，应另行设计版本化。
- 删除后重新点评产生新的点评 ID，重新初始化统计，不复活旧讨论。
- 原 UNIQUE(dish_id,user_id) 改成仅 deleted_at IS NULL 的部分唯一索引，审核发布 UPSERT 改成 ON CONFLICT(dish_id,user_id) WHERE deleted_at IS NULL DO UPDATE。
- 用户和管理员的点评 DELETE 均改为软删除。回复软删除仍保留审核日志外键。
- dish_rating_summary 只统计未删除的正式点评；回复不参与均分和 reviewCount。继续沿用当前每 24 小时刷新约定。

## 审核和接口接入

统一审核日志新增 dish_review_reply_id，并把“恰好一个审核目标”的约束扩展为七种关联。AI 结论类型、即时事件、定时补扫、人工队列、审核详情及日志响应均接入 dish-review-reply。审核通过前重新核验主点评与目录可见性。

接口建议保留上一版：PUT/DELETE /api/v1/reviews/{id}/like、GET/POST /api/v1/reviews/{id}/replies、DELETE /api/v1/review-replies/{id}，另加本人回复查询。Web 与 Android 都需补齐真实入口、字段和调用。

高频读首先合并到列表响应，并按展开动作加载回复。若上线压测发现热点，再考虑对公共计数做短时缓存；likedByMe 必须按用户隔离，不能共享整份个性化响应。新增缓存会引入时效和失效管理，当前表结构不依赖缓存。

## 核对和后续压测

此方案尚未在 PostgreSQL 实例执行。当前环境没有可用 Docker daemon，也没有发现 PostgreSQL 客户端；交付前只完成了与现有 V2/V9/V11/V12/V14 约束、触发器及审核逻辑的静态核对。

实施时应验证：重复和并发点赞/取消、审核与删除竞态、事务回滚、跨楼回复拒绝、软删除过滤、删除后重新点评、审核日志保留，以及不同热点评分布下的延迟和统计行锁等待。不要凭表结构承诺固定 QPS。

离线校验或修复计数时，用 READ COMMITTED 事务先单独 SELECT stats ... FOR UPDATE 获取统计行锁，再以后一条语句统计明细并覆盖计数、递增版本，随后提交；避免在等待锁之前取得的旧快照上直接覆盖。常规读取不执行这类全量校验。

参考：[PostgreSQL HOT](https://www.postgresql.org/docs/current/storage-hot.html)、[部分索引](https://www.postgresql.org/docs/current/indexes-partial.html)、[并发锁](https://www.postgresql.org/docs/current/explicit-locking.html)、[INSERT / ON CONFLICT](https://www.postgresql.org/docs/current/sql-insert.html)。
