# 餐厅与档口点评

2026-11-02 实施。与菜品点评使用独立数据、接口和评分，餐厅对应 `dining_halls`。支持整数 1–5 星、1–500 个 Unicode 码点正文、点赞、楼中楼回复、回复点赞、修改重审、个人记录、管理员审核及下架。

## 数据与一致性

Flyway **V37__venue_reviews.sql** 新增 9 张表，`venue_rating_summary` 为普通视图。以下为实际字段；完整类型、默认值、可空性和约束以迁移为准。

| 表 | 字段 | 作用 |
| --- | --- | --- |
| venue_review_targets | id, dining_hall_id, stall_id, created_at | 两个对象外键恰好一个非空，各自唯一；档口归属从 stalls 查询 |
| venue_reviews | id, target_id, user_id, current_submission_id, publication_version, created_at, first_published_at, updated_at, deleted_at | 稳定楼层身份，部分唯一索引保证同人同对象最多一个未删除身份 |
| venue_review_submissions | id, review_id, client_request_id, base_version, rating, content, status, submitted_at, reviewed_by_user_id, reviewed_by_ai_model, reviewed_at, review_reason, invalidated_at | 不可变版本；全局唯一操作 ID；同楼最多一条有效待审；人工/AI 审核者互斥 |
| venue_rating_stats | target_id, rating, review_count, updated_at | 每对象固定 5 桶，联合主键；人数及加权均分由桶计算 |
| venue_review_stats | review_id, like_count, reply_count, counter_version, updated_at | 楼层公开互动计数 |
| venue_review_likes | review_id, user_id, created_at | 联合主键，重复点赞无副作用 |
| venue_review_replies | id, review_id, reply_to_id, user_id, client_request_id, content, status, reviewed_by_user_id, reviewed_by_ai_model, reviewed_at, review_reason, created_at, updated_at, deleted_at | 回复作者及操作 ID 唯一；复合外键限制回复对象在同楼；正文不可变 |
| venue_review_reply_stats | reply_id, like_count, counter_version, updated_at | 回复点赞计数 |
| venue_review_reply_likes | reply_id, user_id, created_at | 联合主键，回复点赞关系 |

基础明细按第三范式组织，不冗余对象名称、邮箱或档口所属餐厅。三个互动计数表和评分桶是受控派生数据，由数据库触发器随真实插入/删除、公开指针变更在同一事务更新。没有持久化四舍五入后的均分，没有请求时扫描所有评分明细。

公开查询只连接 `current_submission_id` 的已通过版本。本人的 `pending/rejected` 通过独立接口按会话身份读取；列表、人数、评分筛选、回复数不包含待审。修改待审时旧版本继续公开，批准后保留楼层 ID、首次公开时间及互动。删除整楼使当前评分扣除、所有待审版本失效；迟到 AI 不能复活原楼。重新发表使用新身份。

数据库触发器禁止修改提交正文和身份；公开指针复合外键只能引用本楼的批准版本。审批锁顺序为餐厅→档口→楼层→提交/回复，评分桶按 1–5 处理。批准、指针、计数与日志一起提交或回滚。当前为空的对象映射随未使用目录删除；一旦有点评历史，外键阻止物理删除目录，应停用。此处避免空映射阻断已有目录维护流程，历史不级联销毁。

公共点评和回复使用时间 + UUID 游标；游标绑定对象、筛选或本人身份。大小 1–100，客户端默认 20；个人记录与审核历史使用既有页码分页。公开楼层、待审、本人、回复及审核外键均有对应索引；列表卡片一次关联统计，没有逐卡评分请求。筛选按当前版本评分执行，窄评分区间的深页性能仍应随生产量监测。

## 接口

前缀 `/api/v1`，下文 `{target}` 为 `dining-halls/{id}` 或 `stalls/{id}`。沿用 Cookie 会话、CSRF、官方客户端标记、投稿与互动限流。访客可浏览但不能写。个人接口忽略客户端伪造的 userId，非本人返回 404。GET 响应 `private, no-store`。

| 方法与路径 | 行为 |
| --- | --- |
| GET `{target}/rating` | 对象名称/层级、均分、人数、五桶分布；零人均分为 null |
| GET `{target}/reviews?size&cursor&rating` | 公开点评；rating = all / 5 / 4 / 1-3 |
| GET `{target}/reviews/me` | reviewId、publicVersion、privateSubmission |
| POST `{target}/reviews` | rating、content、clientRequestId；202 返回本人版本。同操作同正文重试幂等；冲突 409 |
| GET `users/me/venue-review-submissions/{id}/status` | 本人提交状态 |
| GET `users/me/venue-reviews?type&page&size` | type 可选 hall / stall；每楼最近提交 |
| DELETE `users/me/venue-reviews/{id}` | 删除已发布楼层，包含尚未批准的修改 |
| GET/POST `venue-reviews/{id}/replies` | 公开回复 / 发送 content、replyToId（可空）、clientRequestId |
| GET `venue-reviews/{id}/replies/me` | 本人的待审/驳回回复，独立游标 |
| GET `users/me/venue-review-replies` | 个人回复历史 |
| GET `users/me/venue-review-replies/{id}/status` | 本人回复状态 |
| DELETE `venue-review-replies/{id}` | 本人删除回复 |
| PUT/DELETE `venue-reviews/{id}/like` | 点赞/取消，返回 likedByMe、likeCount、counterVersion |
| PUT/DELETE `venue-review-replies/{id}/like` | 回复点赞/取消 |
| GET `admin/venue-moderation/{type}/{id}` | 当前公开版本和本次提交对照 |
| DELETE `admin/venue-reviews/{id}` / `admin/venue-review-replies/{id}` | 管理员下架，reason 必填，生成 remove 日志 |

既有 `/admin/moderation`、`/admin/moderation/{type}/{id}` 和日志接口扩展 `hall-review`、`stall-review`、`hall-review-reply`、`stall-review-reply`。审核列表含发布者 ID、昵称、邮箱、完整正文、提交时间、AI 结论；公开列表不暴露邮箱。审核接口的一级点评 ID 是 **submissionId**，删除楼层必须用 **reviewId**。

## AI 与部署

复用文字点评及文字回复审核链，包括即时任务、复核/故障回退和补扫。LOW 达 `AI_TRIAGE_AUTO_APPROVE_ABOVE`（默认 .95）通过，HIGH 达 `AI_TRIAGE_AUTO_REJECT_ABOVE`（默认 .90）驳回，其他保留待审。负面评分本身不是违规。关闭、超时、不可用不自动放行；系统占位结论不自动决策。AI 决策与结果、计数、日志同事务；与人工竞争只生效一次。菜品投稿/图片原有“AI 仅建议和驳回”规则不变。

部署先备份数据库，再部署后端执行 V37，随后发布 Web 和 Android。复用现有 PostgreSQL、Redis、AI 配置，无新增密钥或环境变量。未配置 AI 可由管理员人工审核。`fake` 仅用于隔离本地验收，生产使用已有真实审核提供方。本次未发布 main、打标签或改动云端。

验证：`VenueReviewIT`、`VenueReviewAiIT`、`VenueReviewIntegrityIT`，以及完整 integration profile。AI 测试使用本地假提供方，证明自动决策链路与隔离，不代表真实模型识别质量。
