# FoodTimeNeo Backend

校园食堂菜品展示与点评平台的后端项目。当前实现项目骨架、基础设施、数据库表、邮箱注册、登录与会话管理及修改密码，食堂、档口、菜品、评价等业务接口在后续迭代实现。

## 技术栈

| 组件 | 版本 / 用途 |
| --- | --- |
| Java | 21 LTS；编译目标固定为 21 |
| Spring Boot | 4.1.1，Spring MVC |
| Spring Security | 由Spring Boot管理；CSRF、安全过滤链、密码验证和会话身份 |
| Spring Session Redis | 由Spring Boot管理；Redis服务端会话、HttpOnly Cookie |
| 邮件 | spring-boot-starter-mail，版本由Spring Boot管理；SMTP发送注册验证码 |
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

`RegistrationIT` 通过真实 HTTP 请求检查注册、哈希存储、默认角色与昵称、重复邮箱和并发请求，并检查 OpenAPI 文档。它只创建随机数字邮箱的测试账号，测试结束后删除这些账号。

注册测试还使用仅监听127.0.0.1的SMTP服务和独立Redis命名空间，验证6位邮件验证码、15分钟TTL、重发/发送配额、错误尝试、SMTP失败和并发校验。不会向外部邮箱发信，结束后只清理本轮测试数据。

`LoginIT` 使用真实HTTP和独立Redis命名空间验证登录、Cookie/CSRF、会话替换与注销、到期、账号禁用、密码变更、权限更新和原子限流。结束时只删除本轮测试账号及命名空间内的Redis键。

`PasswordChangeIT` 验证旧密码、新密码哈希、当前及其他设备会话失效、条件更新、并发冲突、用户/IP限流和OpenAPI契约；只清理本轮测试账号和Redis键。测试JVM在启动时加载Mockito代理，避免Java 21动态附加代理的权限问题；该设置只影响Surefire/Failsafe测试，不进入应用启动参数。

## 项目文档

| 内容 | 文档 |
| --- | --- |
| 目录、分层职责与测试组织 | [项目目录与职责](docs/architecture.md) |
| 环境、连接、CORS与运行配置 | [配置说明](docs/configuration.md) |
| 数据库表、迁移与评分汇总 | [数据库说明](docs/database.md) |
| 生产环境与容器运行 | [部署说明](docs/deployment.md) |
| 登录、注册与邮箱验证码 | [接口文档](docs/api/README.md) |
| 认证完整调用顺序 | [HTTP联调请求](docs/api/auth.http) |
| 修改密码与重新登录 | [修改密码接口](docs/api/user/password.md) |

HTTP入口统一位于 `src/main/java/com/FoodtimeNeo/api`，业务逻辑按 `auth`、`user`、`dish` 领域组织。注册需邮箱验证码，登录使用Redis会话和HttpOnly Cookie，默认7天到期。邮件配置模板为 [config/mail.example.yml](config/mail.example.yml)，正式配置填写与启用方式见 [邮箱验证码](docs/api/auth/email-verification.md#邮箱配置)。
