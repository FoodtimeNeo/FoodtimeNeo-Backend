# 数据库与迁移

V1 初始化 `foodtime` schema；V2 依据 [数据库设计源文档](../../docs/database-design.md) 建立全部 10 张业务表和 `dish_rating_summary` 物化视图，采用邮箱账号模型：

| 表 | 用途 |
| --- | --- |
| `users` | 用户、密码哈希、角色与账号状态 |
| `dining_halls` / `stalls` / `dishes` | 食堂、档口与菜品 |
| `dish_images` | 菜品图片与审核状态 |
| `dish_review_submissions` / `dish_reviews` | 评论审核提交及正式评论 |
| `stall_submissions` / `dish_submissions` | 档口与菜品审核提交 |
| `content_moderation_logs` | 内容审核日志 |

SQL 包含文档规定的字段、默认值、状态检查、评分与价格检查、唯一索引和 `ON DELETE RESTRICT` 外键。待审核评论使用部分唯一索引，历史提交可保留；审核通过的档口/菜品必须关联正式记录，菜品还必须补齐价格。审核日志必须且只能关联一种提交记录。含 `updated_at` 的 8 张表使用数据库触发器维护更新时间。

首次启动应用时 Flyway 自动执行 V1、V2；已经执行 V1 的环境只执行新的 V2，不修改已执行的版本。UUID 和昵称由业务层生成；正式评论的创建时间、审核通过后的正式数据写入，以及管理员权限验证由后续业务事务负责，建表脚本不自动执行这些业务流程。

`dish_rating_summary` 仅统计正式评论，包含没有评论的菜品（平均分为 NULL，评论数为 0）。唯一索引支持并发刷新；应用启动后刷新一次，之后每次刷新完成后等待 24 小时再刷新，重启会重新计时。刷新使用事务级 PostgreSQL advisory lock，使多个实例不会同时刷新；失败会记录日志并保留原快照。无需安装 pg_cron。

调度配置：`RATING_SUMMARY_REFRESH_ENABLED`（默认 true）、`RATING_SUMMARY_REFRESH_INTERVAL`（默认 PT24H）、`RATING_SUMMARY_INITIAL_DELAY`（默认 PT0S）。应用停止期间不会自动刷新；需要持续刷新时，应保证至少一个应用实例运行。手动刷新：

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY foodtime.dish_rating_summary;
```

并发刷新需要物化视图已初始化且有覆盖所有行的唯一索引，V2 已满足这两个条件，参见 [PostgreSQL 文档](https://www.postgresql.org/docs/17/sql-refreshmaterializedview.html)。

后续 SQL 从 `V3__描述.sql` 起递增命名。已经执行的迁移文件不得修改，以新版本迁移演进数据库；校验默认开启，禁止 clean，禁止自动 baseline。不要将业务建表脚本放到 Compose 的初始化目录，以免与 Flyway 双重管理。

业务分层及源码目录见 [项目目录与职责](architecture.md)。
