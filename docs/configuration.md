# 配置说明

修改密码限流独立于登录限流，默认每用户15分钟5次、每IP15分钟50次；可通过 `PASSWORD_CHANGE_WINDOW`、`PASSWORD_CHANGE_USER_LIMIT`、`PASSWORD_CHANGE_IP_LIMIT`、`PASSWORD_CHANGE_REDIS_NAMESPACE` 配置，规则见 [修改密码接口](api/user/password.md)。

`application.yml` 保存共享配置；`application-dev.yml` 保存本地默认值；`application-prod.yml` 要求显式提供连接信息并关闭 API 文档。默认启用 `dev`；可通过环境变量 `SPRING_PROFILES_ACTIVE=prod` 或启动参数切换。系统环境变量可覆盖 `.env` 值。

| 环境变量 | 开发默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | 环境选择：dev / prod |
| `TEST_VERIFICATION_ENABLED` | `true` | 仅用于测试：允许验证码 `000000` 模拟邮箱验证；生产默认false且拒绝启用。测试真实验证码时设false，详见[测试验证码说明](testing-verification.md) |
| `INVITATION_SECRET` | 空 | 邀请码HMAC密钥，至少32位随机字符；为空时邀请码创建与带码注册返回503，不填邀请码不受影响。详见[邀请码](api/auth/invitation-codes.md) |
| `SUBMISSION_WINDOW` / `SUBMISSION_USER_LIMIT` / `SUBMISSION_IP_LIMIT` / `SUBMISSION_REDIS_NAMESPACE` | `PT15M` / `20` / `200` / `foodtime:content:submissions` | 点评与三类投稿共用限流；窗口为60秒至30天的正整数秒 |
| `UPLOAD_WINDOW` / `UPLOAD_USER_LIMIT` / `UPLOAD_IP_LIMIT` / `UPLOAD_REDIS_NAMESPACE` | `PT1H` / `10` / `100` / `foodtime:content:uploads` | 图片上传限流；窗口同上 |
| `STORAGE_CLEANUP_ENABLED` / `STORAGE_CLEANUP_INTERVAL` / `STORAGE_CLEANUP_MAX_AGE` | `true` / `PT24H` / `P7D` | 孤儿暂存图片清理开关、周期与保留时长；保留时长为0至90天的整秒 |
| `MONITORING_SCRAPE_IP` | `127.0.0.1` | Prometheus抓取允许的来源IP或CIDR；留空拒绝一切抓取。详见[监控](monitoring.md) |
| `SLOW_REQUEST_THRESHOLD` | `PT2S` | 慢请求日志阈值，0至300秒的整秒数 |
| `STORAGE_DISK_WARN_BYTES` | `1073741824` | 暂存目录字节数告警线，超限记warn |
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
| `UPLOAD_MAX_FILE_SIZE` / `UPLOAD_MAX_REQUEST_SIZE` | `10MB` / `20MB` | multipart单文件 / 整个请求上限；图片上传业务已实现，另有JPEG/PNG、10MiB与2000万像素业务校验 |
| `AUTH_SESSION_TTL` | `P7D` | 登录会话期限，见[登录与会话](api/auth/login.md) |
| `AUTH_COOKIE_SECURE` | dev为`false`，prod为`true` | 生产拒绝关闭Secure Cookie；需要HTTPS入口 |
| `SESSION_REDIS_NAMESPACE` | `foodtime:session` | 会话Redis命名空间；专用联调环境应与其他环境隔离 |
| `TOMCAT_MAX_THREADS` | `200` | Web 工作线程上限 |
| `LOG_LEVEL` / `APP_LOG_LEVEL` | `INFO` / `INFO` | 根日志 / 应用日志等级 |
| `LOG_FILE` | `logs/foodtime-neo-backend.log` | 日志文件路径 |
| `FORWARD_HEADERS_STRATEGY` | `none` | 受信任反向代理统一设置转发头时可改为 `framework` |
| `POSTGRES_DB` / `POSTGRES_PORT` | `foodtime_neo` / `5432` | 仅供 Compose 初始化与本机端口映射 |

其他共享设置：优雅停机等待 30 秒，SQL 超时 10 秒，日志按 10MB 滚动、保留 14 天、总量上限 200MB；业务时间使用 `Instant`，数据库连接使用 UTC；API 使用 ISO 8601 时间。跨域配置仅应用于 `/api/**`，仅对明确配置的受信任来源允许Cookie凭据。

## 文件存储

`ImageStorageService` 读取 `app.storage.directory`，默认是进程工作目录下的 `uploads`。可通过启动参数 `--app.storage.directory=/var/lib/foodtime/uploads` 指定持久化目录；需保证运行账号可读写。调大multipart配置不会取消存储服务的10MiB和2000万像素业务限制。

数据库迁移不会创建图片字节。V5菜单图片、V6食堂封面及 `.properties` 元数据的铺放方式见 [种子数据与图片部署](seed-data.md)。容器挂载见 [部署说明](deployment.md)。
