# 真实接口与Android联调验收

## 2026-10-17 容器环境完整验证（当前）

基线覆盖10-14之后全部增量：开发注册测试码（`000000`）、V5/V6种子数据与食堂封面、菜单评分快照、管理员评论管理查询、个人资料昵称与头像修改、邀请码（注册可选归因，平台管理员发放、可复用）、写接口限流与孤儿暂存图片清理、Prometheus指标与慢请求日志、V8热点查询索引、评论与审核项作者昵称。本次重跑数据库集成，未重跑SMTP或线上写入测试；线上写入仍限定专用本地库，不在线上库执行。

环境：Windows + Temurin JDK 21.0.10 + 项目 Maven Wrapper + Docker Compose PostgreSQL 17-alpine 与 Redis 7.4-alpine（真实容器，10-14使用的是隔离Redis 5实例）。命令：`.\mvnw.cmd clean verify -B -ntp -Pintegration '-Dit.test=*IT,!RegistrationIT'`。

| 验证范围 | 最终结果 |
| --- | --- |
| 单元与接口测试、完整打包 | 200 项通过，0 失败，构建成功 |
| 数据库约束（含V7邀请码表与V8索引）、评分汇总、基础设施、登录、改密、个人资料、邀请码归因与管理、写接口限流、暂存清理、指标端点、三类列表、投稿审核、评论作者昵称、目录管理、用户管理的集成测试 | 152 项通过，0 失败 |
| `RegistrationIT` 的邮箱/注册集成测试 | 按冻结约定排除（正式邮箱投递暂不验收），不计入通过数量 |

## 2026-10-14 隔离环境结果（历史保留）

本页保留2026-10-14的隔离环境结果，不是当前main每次提交的重新验收。后续已增加开发注册测试码和V5/V6种子数据，分别见[测试验证码](testing-verification.md)与[数据部署](seed-data.md)。本轮文档维护核对源码、配置及本地链接，未重跑数据库集成、SMTP或线上写入测试。

范围以较新FEATURE和当前数据库为准。按用户确认暂不增加点赞、收藏、分类，stall.floor已删除；图片审核字段和日志外键以V4补充。正式邮箱投递与RegistrationIT暂排除，不把模拟邮件测试当作真实邮箱已接通。

| 功能 | 数据与约束 | 验证 |
| --- | --- | --- |
| 三级目录、详情、图册 | 三级active过滤、真实父级关系、图册仅approved | 原有目录/详情/图册真实HTTP测试，Android浏览页面 |
| 评论提交/发布/删除 | pending唯一；正式用户/菜品唯一；复制提交时间；删除不删审核历史 | PublishedReviewIT、ModerationIT及Android评论审核删除 |
| 评分 | dish_rating_summary物化视图；无评分为null/0，每日刷新 | DatabaseSchemaIT、PublishedReviewIT及Android快照检查 |
| 用户资料、个人记录 | 当前会话所有者；隐去认证信息；保留停用内容历史与可空价格 | ProfileIT、个人提交查询测试、Android个人页面 |
| 图片 | 私有暂存、本人引用、JPEG/PNG边界、approved后可读且受父级启停影响 | ImageStorageIT、ImageSubmissionIT及Android上传/图册/私有字节 |
| 菜品/档口投稿 | 先pending，缺价格通过前补齐，名称冲突事务回滚 | CatalogSubmissionIT、ModerationIT及Android真实数据源 |
| 审核与日志 | 管理角色、行锁、仅pending；正式数据/状态/日志同一事务 | 并发审核与日志/回滚HTTP测试，Android审核对话框 |
| 目录维护、违规评论 | DB外键阻止关联删除；停用保留历史；删除评论需原因 | CatalogAdminIT及Android三级目录增删改 |
| 用户管理、密码 | superadmin调整他人角色/状态；禁止自身修改；会话逐次重读及密码版本失效 | UserAdminIT、PasswordChangeIT、Android禁用恢复与改密 |

Backend使用隔离PostgreSQL17、Redis5和JDK21、离线Maven。最终完整构建通过175项单元测试及排除RegistrationIT后的133项集成测试，均无失败；包含损坏PNG返回400的新增验证。该环境未运行Compose中Redis7.4容器，不混称容器环境通过。

Android以adb reverse访问127.0.0.1:8080，在API36.1设备上的最终真实数据源检查22项通过；实际默认RemoteRepository页面在正常字体及1.3倍字体下各19项通过，包含提交过程中Activity重建与唯一写入。截图及Cookie来源绑定、私有预览清理等说明见Android docs/verification.md。

本地样例只在显式启用local-integration和fixtures标志时创建，不进入普通dev/prod启动。正常软件功能无预置客户端身份、假验证码或LocalRepository回退。应用操作日志记录删除/角色状态调整的操作者、目标和时间，不新增数据库设计没有的审计表或动作。

Git实现提交位于各feature，验证后一次非快进合并回dev。复用图片文件功能分支时先将指针同步最新dev，再修复损坏图片的错误分类；没有把dev合并进旧feature。提交与合并说明为中文，main未发布。
