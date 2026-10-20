# 数据库与迁移

V1 初始化 `foodtime` schema；V2 建立全部 10 张业务表和评分物化视图；V3 按用户修订删除 `stalls.floor`；V4 按用户确认补充图片审核信息与日志外键；V5 导入四食堂菜单和批准图片；V6 补充食堂封面并新增两食堂；V7 新增邀请码表与用户归因列；V8 补充热点查询索引；V9 新增AI预检结论表。V5、V6是数据迁移；V8增加索引，V9新增结论表。V15–V18新增六张点评互动/统计表并将评分汇总改为实时普通视图。当前结构见 [数据库设计](database-design.md)：

| 表 | 用途 |
| --- | --- |
| `users` | 用户、密码哈希、角色与账号状态 |
| `dining_halls` / `stalls` / `dishes` | 食堂、档口与菜品 |
| `dish_images` | 菜品图片与审核状态 |
| `dish_review_submissions` / `dish_reviews` | 评论审核提交及正式评论 |
| `stall_submissions` / `dish_submissions` | 档口与菜品审核提交 |
| `content_moderation_logs` | 内容审核日志 |
| `dish_stats` | 菜品评论数与评分总和 |
| `dish_review_stats` / `dish_review_likes` | 一级点评互动统计与点赞明细 |
| `dish_review_replies` | 审核回复、对象关系与软删除 |
| `dish_review_reply_stats` / `dish_review_reply_likes` | 回复独立统计与点赞明细 |

SQL 包含文档规定的字段、默认值、状态检查、评分与价格检查、唯一索引和 `ON DELETE RESTRICT` 外键。待审核评论使用部分唯一索引，历史提交可保留；审核通过的档口/菜品必须关联正式记录，菜品还必须补齐价格。审核日志必须且只能关联一种提交记录。含 `updated_at` 的 8 张表使用数据库触发器维护更新时间。

首次启动应用时 Flyway 自动执行 V1–V18；已有环境只执行未应用的迁移。V3 删除旧楼层值，V4 的旧图片审核信息保持NULL。已执行版本保持原校验和。正式数据、提交状态与审核日志由服务层事务维护；管理员权限由服务端校验。V5 的种子图片显式包含批准状态和导入审核日志，属于初始化数据，不代表普通上传自动公开。

V5、V6仅保存媒体引用，图片文件需另行铺入运行目录，见 [种子数据与图片部署](seed-data.md)。V6新增的东区学生餐厅和学苑餐厅目前没有种子档口与菜品。

V15 新增 `dish_stats`，实时维护未删除正式点评的 `review_count` 和 `rating_sum`；评分是后者除以前者，无评论时为 NULL。点评新增、评分修改、软删除及事务回滚由数据库触发器同步处理。普通读取不执行评论明细 COUNT/AVG。

`dish_rating_summary` 已改为读取 `dish_stats` 的普通兼容视图，仍返回 dish_id/average_score/review_count。已移除每日和启动刷新任务，不再接受 `REFRESH MATERIALIZED VIEW`。`RATING_SUMMARY_*` 环境变量已废弃。暂存图片每日清理不受影响。

正式点评新增 deleted_at，公开和个人列表只返回未删除记录；同用户同菜品仅允许一条未删除点评。重新点评保留原ID和created_at；删除后再提交通过创建新ID。用户及管理员删除都同步扣除评分，审核历史保留。

部署 V15 需同时更新应用：旧版审核UPSERT和物化视图刷新与新结构不兼容。首次部署在停止旧应用写入后执行迁移并启动新应用；先在专用库验证升级与数据回填。不要修改已经应用的V1–V14迁移。

后续 SQL 从 `V19__描述.sql` 起递增命名。已经执行的迁移文件不得修改，以新版本迁移演进数据库；校验默认开启，禁止 clean，禁止自动 baseline。不要将业务建表脚本放到 Compose 的初始化目录，以免与 Flyway 双重管理。

当前设计保存在本仓库 `docs/database-design.md`，包含用户后续修订；现有接口的设计符合性、尚未实现的业务及验证结果见 [数据库设计核查报告](database-design-audit.md)。

业务分层及源码目录见 [项目目录与职责](architecture.md)。

## V16–V18 点评互动

V16增加一级点评统计与点赞，V17增加审核回复和回复计数，V18增加回复独立统计与点赞。累计新增六张表，详见[字段约束](database-design.md)和[API契约](api/dish/review-interactions.md)。正式变更只通过Flyway V15–V18增量执行；提案SQL为V14基线的合并草案，不可在已升级环境重复运行。

升级需停止旧版本写入并停掉旧评分调度；迁移在事务内锁定所需表、一次性回填正式点评评分、初始化互动统计。应用新版本与迁移一起发布。上线前备份并在副本验证；回滚应用不能直接运行旧物化视图版本，不能通过删除迁移记录回退。

常用读取不COUNT点赞/回复明细、不AVG正式点评：菜单和详情读dish_stats；点评列表对本页ID一次关联dish_review_stats及本人点赞；回复用(review_id,created_at,id)游标及部分索引，关联reply_stats。个人记录/后台筛选的精确分页COUNT继续保留，不属于高频公开计数。数据修复需要停写后从明细重新校验和回填，不能与触发器同时重复补计数；没有自动日汇总任务。
