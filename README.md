# FoodTimeNeo Backend

校园食堂菜品展示与点评平台的后端基础项目。当前交付项目骨架、基础接口与基础设施配置，业务登录与用户、食堂、档口、菜品、评价等功能在后续迭代实现。

## 技术栈

| 组件 | 版本 / 用途 |
| --- | --- |
| Java | 21 LTS；编译目标固定为 21 |
| Spring Boot | 4.1.1，Spring MVC |
| Maven | Wrapper 固定为 3.9.16 |
| MyBatis-Plus | 3.5.17，使用 Boot 4 starter；PostgreSQL 分页，上限 100 条 |
| PostgreSQL | Compose 使用 17-alpine；JDBC 驱动由 Spring Boot 管理 |
| Redis | Compose 使用 7.4-alpine；Lettuce + 连接池，提供 StringRedisTemplate |
| Flyway | 由 Spring Boot 管理；使用独立 `foodtime` schema |
| API 文档 | springdoc-openapi 3.1.1 + Swagger UI；仅开发环境启用 |
| 测试 | JUnit、MockMvc、AssertJ；真实依赖测试通过 integration profile 单独运行 |

Spring Boot 管理通用依赖版本，不单独覆盖 Spring、Jackson、数据库驱动和 Flyway 的版本。

## 本地启动（PowerShell）

前提：安装 JDK 21，设置 `JAVA_HOME`；安装并启动 Docker Desktop，选择 Linux 容器。

在本 Backend 目录运行：

```powershell
Copy-Item .env.example .env
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

已有 PostgreSQL / Redis 时，可以跳过 Compose，编辑 `.env` 指向对应服务。数据库需要事先创建，数据库账号需要有 `foodtime` schema 的创建权限及迁移所需的 DDL 权限。

`.env` 通过 Spring Config Import 加载，也由 Compose 读取；启动时工作目录必须为本 Backend 目录。文件使用 UTF-8 的 `KEY=value` 格式，不加 `export` 或引号。示例仅包含本地开发凭据，真实配置不会进入 Git。复杂密码和生产凭据建议由系统环境变量或部署平台注入。

首次运行 Wrapper 需要下载 Maven 与依赖。已安装 Maven 3.9+ 时，也可将 `.\mvnw.cmd` 替换为 `mvn`。Linux/macOS 使用 `bash ./mvnw`。

如果本机端口已占用，修改 `.env` 中 `POSTGRES_PORT` 并同步修改 `DB_URL`；修改 `REDIS_PORT` 会同时改变本地容器映射与应用连接端口。

| 地址 | 用途 |
| --- | --- |
| `http://localhost:8080/api/v1/system/ping` | HTTP 服务检查；不代表数据库和 Redis 可用 |
| `http://localhost:8080/actuator/health` | 汇总健康状态 |
| `http://localhost:8080/actuator/health/liveness` | 应用存活状态 |
| `http://localhost:8080/actuator/health/readiness` | 就绪状态，包含 PostgreSQL 与 Redis |
| `http://localhost:8080/swagger-ui.html` | 开发环境交互式 API 文档 |
| `http://localhost:8080/v3/api-docs` | 开发环境 OpenAPI JSON |

停止本地容器：`docker compose down`。命名数据卷会保留，再次启动仍使用原数据。修改数据库初始用户名、密码或库名不会自动修改已有数据卷中的数据库。

## 构建与测试

```powershell
# 无需数据库和 Redis：编译、接口与跨域测试、打包
.\mvnw.cmd clean verify

# 需要已启动的本地 PostgreSQL 和 Redis：完整应用启动、Flyway、数据库与 Redis 联通验证
docker compose up -d --wait
.\mvnw.cmd -Pintegration verify

# 运行打包产物；从 Backend 目录执行以加载 .env
java -jar target/foodtime-neo-backend.jar
```

`InfrastructureIT` 使用当前配置指向的服务，执行 schema 迁移并读写带随机后缀、30 秒 TTL 的 Redis 测试键，结束后删除该键。数据库约束测试在事务中写入随机 ID 的样本并回滚；物化视图测试使用事务内刷新并回滚。请使用开发或专用测试数据库。普通 `verify` 不执行这组测试；显式开启 integration 时，连接失败会导致构建失败，不静默跳过。

## 配置约定

`application.yml` 保存共享配置；`application-dev.yml` 保存本地默认值；`application-prod.yml` 要求显式提供连接信息并关闭 API 文档。默认启用 `dev`；可通过环境变量 `SPRING_PROFILES_ACTIVE=prod` 或启动参数切换。系统环境变量可覆盖 `.env` 值。

| 环境变量 | 开发默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | 环境选择：dev / prod |
| `SERVER_ADDRESS` / `SERVER_PORT` | `0.0.0.0` / `8080` | 服务监听地址与端口 |
| `DB_URL` | `jdbc:postgresql://localhost:5432/foodtime_neo?currentSchema=foodtime` | JDBC URL；生产必须显式提供，保留 `currentSchema=foodtime` |
| `DB_USERNAME` / `DB_PASSWORD` | `.env.example` 中的本地账号 | 生产必填，不允许为空 |
| `DB_POOL_MIN_IDLE` / `DB_POOL_MAX_SIZE` | `2` / `10` | Hikari 连接池 |
| `DB_CONNECTION_TIMEOUT` | `30000` | 获取数据库连接的超时，毫秒 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | 生产必须显式提供 host |
| `REDIS_USERNAME` | 空 | 可选 Redis ACL 用户名 |
| `REDIS_PASSWORD` | `.env.example` 中的本地密码 | 生产必填，不允许为空 |
| `REDIS_DATABASE` | `0` | Redis 逻辑数据库 |
| `REDIS_SSL_ENABLED` | `false` | Redis TLS 开关 |
| `REDIS_CONNECT_TIMEOUT` / `REDIS_TIMEOUT` | `3s` / `3s` | Redis 建连 / 命令超时 |
| `REDIS_POOL_MAX_ACTIVE` | `8` | Redis 连接池最大连接数 |
| `CORS_ALLOWED_ORIGINS` | localhost:5173、localhost:3000 的 HTTP origin | 逗号分隔的完整 origin；生产默认为空；禁止通配符和带路径的 URL |
| `UPLOAD_MAX_FILE_SIZE` / `UPLOAD_MAX_REQUEST_SIZE` | `10MB` / `20MB` | 单文件 / 整个请求上传大小限制；上传业务尚未实现 |
| `TOMCAT_MAX_THREADS` | `200` | Web 工作线程上限 |
| `LOG_LEVEL` / `APP_LOG_LEVEL` | `INFO` / `INFO` | 根日志 / 应用日志等级 |
| `LOG_FILE` | `logs/foodtime-neo-backend.log` | 日志文件路径 |
| `FORWARD_HEADERS_STRATEGY` | `none` | 受信任反向代理统一设置转发头时可改为 `framework` |
| `POSTGRES_DB` / `POSTGRES_PORT` | `foodtime_neo` / `5432` | 仅供 Compose 初始化与本机端口映射 |

其他共享设置：优雅停机等待 30 秒，SQL 超时 10 秒，日志按 10MB 滚动、保留 14 天、总量上限 200MB；业务时间使用 `Instant`，数据库连接使用 UTC；API 使用 ISO 8601 时间。跨域配置仅应用于 `/api/**`，不启用 Cookie 凭据。

## API 约定

业务接口统一放在 `/api/v1/**` 下。成功响应示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": { "service": "foodtime-neo-backend", "status": "UP" },
  "timestamp": "2026-10-03T14:00:00Z",
  "requestId": "client-123"
}
```

失败响应保留对应 HTTP 状态码，`code` 使用可供前端判断的字符串。参数校验失败的 `data` 返回字段名和错误信息；其他错误通常为 `null`。响应不返回堆栈、SQL 或被拒绝的字段值。Actuator 和 OpenAPI 使用各自的标准响应格式。

所有请求返回 `X-Request-Id`，并将其写入日志。客户端可传入由字母、数字、点、下划线、短横线组成且长度不超过 64 的请求 ID；无效值由服务端重新生成。`BusinessException` 供后续业务以明确错误码、提示和 HTTP 状态返回可预期错误。

## 数据库迁移与开发目录

```text
src/main/java/com/FoodtimeNeo/
  FoodTimeNeoApplication.java
  common/api/           统一响应
  common/exception/     业务异常与全局异常处理
  common/web/           请求 ID 过滤器
  config/               CORS、MyBatis-Plus、OpenAPI、生产配置检查
  system/               基础检查接口
src/main/resources/
  application*.yml      环境与基础设施配置
  db/migration/         Flyway 版本化 SQL
src/test/java/          无外部依赖测试与显式集成测试
```

V1 初始化 `foodtime` schema；V2 依据 `../docs/database-design.md` 建立全部 10 张业务表和 `dish_rating_summary` 物化视图，采用邮箱账号模型：

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

业务代码按领域建立 `user`、`dining`、`dish`、`review` 等包，在各领域内放置 controller、service、mapper、entity、DTO。Mapper 使用 `@Mapper`，XML 放在 `src/main/resources/mapper/`。PostgreSQL `UUID` 字段使用 `java.util.UUID` 并由服务层生成，避免使用 MyBatis-Plus 的字符串 ID 生成器。Redis 优先使用 `StringRedisTemplate`；业务缓存的键名、TTL 和 JSON 类型应在对应业务实现时明确。

## 生产与容器

本地 Compose 提供 PostgreSQL 和 Redis，端口仅绑定本机；生产部署需配置自己的数据库、Redis 与网络访问规则。当前初始化不包含登录、JWT 或角色鉴权，新增受保护业务接口时必须实现服务端权限校验。

生产环境必填：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PASSWORD`。例如在部署平台设置 `SPRING_PROFILES_ACTIVE=prod` 并注入这些变量；远程 PostgreSQL 的 JDBC URL 可配置 `sslmode=verify-full`，Redis TLS 使用 `REDIS_SSL_ENABLED=true`。生产默认仅暴露健康端点，健康详情隐藏，API 文档关闭。

Dockerfile 使用 Java 21 多阶段构建，以非 root 用户运行，容器默认启用 `prod`：

```powershell
docker build -t foodtime-neo-backend:local .
# 创建仅供容器使用、指向可从容器访问的数据库 / Redis 的环境文件
docker run --rm --env-file .env.container -p 8080:8080 foodtime-neo-backend:local
```

容器中的 `localhost` 指向容器本身，因此 `.env.container` 中必须填写实际可达的服务地址，并使用 `SPRING_PROFILES_ACTIVE=prod`。若连接开发机服务，可根据 Docker Desktop 配置使用 `host.docker.internal`。

`.gitignore` 已覆盖 Maven 构建目录、IDE 状态、本地环境配置、日志、运行数据和私钥；保留 `.env.example`、Maven Wrapper 与版本化数据库迁移。`.dockerignore` 将本地凭据、Git 与构建缓存排除在镜像构建上下文之外。
