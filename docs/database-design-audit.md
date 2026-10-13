# 数据库设计与接口核查

核查日期：2026-10-13。依据用户提供的 `database-design(1).md`，设计保存为 [database-design.md](database-design.md)，并按后续要求删除档口楼层字段。首次核查代码基线为远端 `dev` 的 `f06a09d`；首次核查已执行 `git fetch origin --prune`，本地 `dev` 与远端一致，开始核查时工作区干净。

## 分支状态

拉取时远端功能分支如下，六个分支的提交均已包含在 `dev` 中：

| 分支 | 提交 | 功能 |
| --- | --- | --- |
| `feature/register` | `e15d724` | 注册与邮箱验证码 |
| `feature/login` | `95b5806` | 登录与会话 |
| `feature/changePassword` | `5019f41` | 修改密码 |
| `feature/diningHallList` | `5152f42` | 食堂列表 |
| `feature/stallList` | `8a86df8` | 食堂下档口列表 |
| `feature/stallDishList` | `d3a4677` | 档口下菜品列表 |

本聊天产生的历史从 `f06a09d` 重新整理，保留该提交及之前的全部历史。公共设计文档使用 `feature/databaseDesign`，其余修改分别使用六个对应功能分支。

每一轮执行 `dev → feature → dev`：先将该 feature 的分支指针设为当时最新的 dev，切换到 feature 后提交本功能的修改，再切回 dev 合并。同步仅移动分支指针，不创建“将 dev 合并到旧 feature”的提交。首次核查整理阶段每轮只有一个功能修改提交和一个合回 dev 的合并提交，共七轮；后续修改继续遵循相同流程。整理前的完整历史及登录修复均已保存为本地 Git bundle，远端同步使用预期旧提交校验，避免覆盖期间新增的提交。

## 已有接口

核查覆盖全部11个已实现的HTTP接口、现有 Markdown 接口文档、HTTP请求示例、Controller / DTO 的 OpenAPI 注解、参数校验、Service、Mapper、数据库迁移和相关测试。重新检查发现登录失败时仍更新成功登录时间的问题，已在 `feature/login` 修复并通过真实数据库回归测试。其余接口未发现把不存在的业务表字段当作持久化字段读写的情况。接口文档原先未区分设计依据与额外约定，本次已逐项补充来源说明及待确认事项。

数据库设计规定持久化字段与业务规则，并未要求每个列表或用户响应返回全部数据库字段；响应字段同时核对本仓库 [接口文档](api/README.md)。公开字段省略不等于表字段缺失；请求 `account` 映射邮箱，`expiresAt`、验证码、统一响应的 `timestamp` / `requestId` 是协议或认证过程数据，不是凭空增加的业务表字段。

| 接口（前缀 `/api/v1`） | 核查结果 | 对应文档与实现 |
| --- | --- | --- |
| `POST /auth/register` | 写入 `users` 已有字段；邮箱规范化且唯一；UUID、默认昵称和验证时间由服务端生成；只存密码哈希；固定默认角色/状态，不接受客户端权限字段；邮件联调暂跳过 | [注册](api/auth/register.md)；`RegisterRequest`、`RegistrationService`、`UserMapper` |
| `POST /auth/register/email-code` | 仅检查已有邮箱并操作Redis/SMTP，不新增业务表字段；验证码、发送配额和强制验证是额外约定；投递暂跳过 | [验证码](api/auth/email-verification.md)；`EmailCodeRequest`、`EmailVerificationService` |
| `GET /auth/csrf` | 生成会话的CSRF令牌，不读写业务表；`headerName` / `token` 是安全协议数据，未假定数据库存在验证码或Token列 | [登录](api/auth/login.md)；`LoginController`、`CsrfResponse` |
| `POST /auth/login` | 按邮箱查询用户，校验哈希和账号状态；成功登录时间在包含会话创建的事务中更新，失败回滚；不返回哈希 | [登录](api/auth/login.md)；`LoginController`、`LoginService`、`UserMapper`；`LoginIT`、`LoginRollbackIT` |
| `GET /auth/me` | 会话校验重新读取用户当前状态、角色及密码版本；公开字段均来自 `users`，不会接受客户端角色或返回哈希 | [登录](api/auth/login.md)；`SessionAuthenticationService`、`UserResponse`；`LoginIT` |
| `POST /auth/logout` | 删除当前Redis会话，不修改用户角色、账号状态或密码；没有凭空增加退出时间字段 | [登录](api/auth/login.md)；`LoginController`、`SessionAuthenticationService`；`LoginIT` |
| `PUT /users/me/password` | 验证当前用户旧密码，条件更新哈希和 `password_changed_at`，防止并发覆盖；触发器维护 `updated_at`；不允许请求指定其他账号或修改权限 | [改密](api/user/password.md)；`PasswordChangeService`、`UserMapper`；`PasswordChangeIT` |
| `GET /dining-halls` | 仅查询启用食堂，按已有 `sort_order` 和ID排序；图片、简介、经纬度可空；经纬度成对约束由数据库保证；不捏造距离/评分/数量 | [食堂列表](api/dining/list.md)；`DiningHallMapper`；`DiningHallListIT`、`DatabaseSchemaIT` |
| `GET /dining-halls/{diningHallId}/stalls` | 查询该食堂的启用档口并检查父级状态；使用已有排序值、可空封面/简介；按后续要求移除楼层字段；不存在或隐藏父级返回404，空档口返回数组 | [档口列表](api/dining/stalls.md)；`StallMapper`、`StallService`；`StallListIT` |
| `GET /stalls/{stallId}/dishes` | 检查食堂、档口、菜品三级状态；只返回所属档口的菜品；非负两位小数价格使用 `BigDecimal`；未从菜品表读取不存在的封面或评分列 | [菜品列表](api/dish/list.md)；`DishMapper`、`DishService`；`StallDishListIT` |
| `GET /system/ping` | 仅返回HTTP服务名与运行状态，不访问业务表；`UP` 是运行检查状态，不能与用户或内容状态混同；依赖健康由Actuator提供 | [接口目录](api/README.md)、README；`SystemController`、`WebInfrastructureTest` |

`auth.http`、`password.http`、`dining.http` 中的路径、字段和Cookie/CSRF调用顺序与现有Controller及DTO一致。未将物化视图评分、审核图片、评论或提交审核描述为这些列表已返回的数据。

现有业务额外限制注册邮箱为数字前缀的 `@bjtu.edu.cn` 邮箱，并规定密码格式；数据库设计没有定义这些产品规则。用户在本次核查中明确选择保留已有规则，并要求在接口文档中标注为额外约定及待确认事项。校内邮箱范围、密码字符与复杂度、默认昵称格式、强制验证码流程、会话期限、限流配额、展示接口的登录要求与响应字段选择均已与数据库设计本身区分。注册当前不接受自定义昵称或头像；设计中的字段已建表，其设置入口尚未实现，这一接口范围也已注明待确认。

## 数据库与评分汇总

V2 建立初始设计中的 10 张业务表及 `dish_rating_summary` 物化视图；V3 按后续要求删除 `stalls.floor`，迁移后的结构与当前设计一致，包括字段类型、可空性、默认值、唯一索引、状态取值、价格与评分检查、经纬度成对约束，以及 `ON DELETE RESTRICT` 外键。

审核相关约束已建立：同一用户对同一菜品最多一条待审核评论；正式评论按菜品和用户唯一；非待审核提交必须记录审核人和审核时间；档口、菜品审核状态与正式记录关联一致，菜品通过前价格不得为空；每条审核日志只能关联一种提交，动作仅允许 `approve` 或 `reject`。含 `updated_at` 的 8 张表由触发器维护更新时间。

评分汇总只统计正式评论，无评论菜品的均分为 NULL、评论数为 0，并有支持并发刷新的唯一索引。`DishRatingSummaryRefresher` 默认在启动后刷新一次，此后每次刷新完成后等待 24 小时，使用 PostgreSQL 事务级 advisory lock 避免同时刷新；调度与失败处理见 [数据库说明](database.md)。本次集成测试禁用自动调度，直接调用刷新器验证实际汇总，不以测试结果声称已等待并验证 24 小时周期。

## 尚未实现的业务

以下已有表结构，但没有对应业务接口，不能视为已完整实现数据库设计中的业务流程：

- 菜品图片上传与审核，以及公开图片的审核状态过滤。
- 评论提交、管理员审核、审核通过后创建或更新正式评论，并复制提交时间。
- 档口提交与审核，通过后创建正式档口并复制封面。
- 菜品提交与审核，补齐价格后创建正式菜品与已审核图片。
- 审核动作的管理员权限校验，以及每次审核操作在同一业务事务中新增日志。

本次检查和修复已有接口；上述功能需要后续功能分支开发。表约束通过验证不等于这些业务流程已经实现。

## 本次修正

- 将用户提供的设计文件保存到本仓库 `docs/database-design.md`，导入时 SHA-256 与来源文件一致。
- 修正 `docs/database.md` 指向不存在的仓库外路径 `../../docs/database-design.md` 的链接，改为同目录 `database-design.md`。
- 在 README 与数据库说明中加入设计原文和本核查报告的入口。
- 在现有注册、邮箱验证码、登录、修改密码、食堂列表、档口列表和菜品列表文档中分别补充设计依据、额外约定和待确认事项；在接口目录说明协议字段的来源和未实现功能。
- 修复登录会话创建失败仍写入 `last_login_at` 的问题，增加事务回滚及失败会话清理，新增 `LoginRollbackIT` 验证首次登录和已有成功登录时间两种情况；对应接口文档同步说明。
- 保留已执行的 Flyway V1、V2，不修改迁移历史或新增设计之外的业务字段。

## 后续修订：删除档口楼层

2026-10-13 按用户要求，以最新 dev `fbbc560` 为起点，将 `feature/stallList` 指针同步到 dev 后开发，再合并回 dev。

- 新增 `V3__remove_stall_floor.sql` 删除 `foodtime.stalls.floor`，原 V1、V2 不变。
- 移除档口实体、查询投影、API响应和OpenAPI中的楼层字段；档口列表仅返回 `id`、`diningHallId`、`name`、`coverImageUrl`、`description`。
- 更新当前数据库设计与档口接口文档，移除旧字段说明和响应示例。
- 在原先已应用 V2 的专用 PostgreSQL 17 数据库上成功升级至 V3；测试确认档口列集合符合新设计，实际JSON和OpenAPI均无 `floor`，其余展示和状态规则继续通过验证。

## 验证

使用本机 JDK 21.0.9、离线 Maven 3.9.8 和项目 `.m2` 依赖缓存验证。数据库测试使用本次新初始化、仅监听 `127.0.0.1` 的 PostgreSQL 17.6 与 Redis 5.0.14.1，测试结束后停止两个实例；未连接已有业务数据库。Compose 指定的 Redis 7.4 容器环境本次未运行。

完整构建与排除邮箱后的集成验证：`mvn -o -Dmaven.repo.local=.m2 -Dmaven.jar.forceCreation=true -Pintegration "-Dit.test=*IT,!RegistrationIT" verify`，连接参数由本次测试进程的环境变量提供。

| 验证范围 | 最终结果 |
| --- | --- |
| 单元与接口测试、完整打包 | 143 项通过，0 失败，构建成功 |
| 数据库约束、评分汇总、基础设施、登录、登录失败回滚、修改密码及三类列表的集成测试 | 68 项通过，0 失败 |
| `RegistrationIT` 的 16 项邮箱/注册集成测试 | 按用户要求排除，尚未验证通过；不计入通过数量 |
| 当前设计与迁移一致性、仓库内文档链接、Git差异格式 | 检查通过 |

初始完整集成测试曾运行81项，其中5项本地 SMTP 注册邮件测试超时失败；用户随后说明邮箱尚未正式接通并要求先跳过，因此未继续修复或将这些失败标记为通过。重新核查新增的2项登录回滚测试在修复前均复现时间被错误更新，在修复后通过；删除楼层字段又新增1项数据库设计验证，最终执行排除 `RegistrationIT` 的68项集成测试。真实邮件投递以及本地 SMTP 超时原因均未验证解决。单元测试中的模拟邮箱逻辑检查仍保留，其通过不代表真实邮箱已接通。

## 后续新增的查询接口

该阶段新增菜品基础详情、已审核图册、本人评论审核记录和本人菜品提交查询；本节与上文“尚未实现”均描述当时状态。2026-10-14真实投稿与管理闭环已实现，现状见下节及[联调验收](verification.md)。

- 详情沿用菜品字段并关联档口、食堂，检查三级 `active` 状态；没有菜品封面、评分、分类或楼层列。
- 图册读取 `dish_images` 已有字段，仅展示 `approved`，检查三级展示状态；计数和分页使用同一快照。
- 本人评论审核记录读取 `dish_review_submissions`，所有者来自校验后的会话；返回三种审核状态及已有可空审核信息，保留已停用菜品的历史。
- 本人菜品提交读取 `dish_submissions`，保留可空价格和正式菜品关联；提交内容不被正式菜品修改覆盖，个人历史不套用公开状态过滤。
- 分页的 `items`、`total`、`page`、`size` 是查询协议；关联名称来自已有关系，不新增快照列。未修改设计、V1～V3或业务数据。

新增接口文档逐一标注字段来源、身份与状态边界，以及数据库设计之外的分页和排序约定；验证结果以对应功能测试及最新 CI 为准，前文143/68项结果为首次核查时的历史结果。

## 2026-10-14 当前实现

评论提交/审核/查询/删除、每日评分快照、个人资料、图片文件/投稿/审核、菜品与档口投稿、个人记录、目录管理和用户权限管理均使用真实数据库与服务端权限。没有点赞、收藏、分类、楼层、运营员归属或客户端生成的业务状态。

用户确认允许补充独立图片审核：新增V4中的审核人/时间/原因及日志图片外键，同时更新database-design.md。V1–V3不改写；旧图片审核信息保留NULL，不虚构过去的审核。日志四类外键恰好一个，动作仍为approve/reject；评论删除和用户管理使用应用操作日志，不造数据库日志动作。

协议与派生字段（分页、type/targetId、关联名称、reviewCount等）在各接口文档说明来源。用户身份、提交状态和审核人由服务端决定，金额保持精确、待审核菜品价格可空，权限由后端逐次核查。阶段验收数量和Android真实连接证据见[联调验收](verification.md)；历史离线模拟或旧测试数量不用于证明当前联通。
