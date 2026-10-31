# 配置说明

修改密码限流独立于登录限流，默认每用户15分钟5次、每IP15分钟500次；可通过 `PASSWORD_CHANGE_WINDOW`、`PASSWORD_CHANGE_USER_LIMIT`、`PASSWORD_CHANGE_IP_LIMIT`、`PASSWORD_CHANGE_REDIS_NAMESPACE` 配置，规则见 [修改密码接口](api/user/password.md)。登录限流默认每账号15分钟10次、每IP15分钟1000次。每IP配额按校园网统一出口放大，单人滥用由用户级配额兜住；注册与发码额外使用签名匿名设备 Cookie，按邮箱/设备/IP三层计数，见[邮箱验证码](api/auth/email-verification.md)。反向代理部署须配合 `FORWARD_HEADERS_STRATEGY`，否则全校共用一份IP配额，见[生产部署](deployment.md)。

`application.yml` 保存共享配置；`application-dev.yml` 保存本地默认值；`application-prod.yml` 要求显式提供连接信息并关闭 API 文档。默认启用 `dev`；可通过环境变量 `SPRING_PROFILES_ACTIVE=prod` 或启动参数切换。系统环境变量可覆盖 `.env` 值。

| 环境变量 | 开发默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | 环境选择：dev / prod |
| `INVITATION_SECRET` | 空 | 邀请码HMAC密钥，至少32位随机字符；为空时邀请码创建与带码注册返回503，不填邀请码不受影响。详见[邀请码](api/auth/invitation-codes.md) |
| `SUBMISSION_WINDOW` / `SUBMISSION_USER_LIMIT` / `SUBMISSION_IP_LIMIT` / `SUBMISSION_REDIS_NAMESPACE` | `PT15M` / `20` / `2000` / `foodtime:content:submissions` | 点评与三类投稿共用限流；窗口为60秒至30天的正整数秒 |
| `UPLOAD_WINDOW` / `UPLOAD_USER_LIMIT` / `UPLOAD_IP_LIMIT` / `UPLOAD_REDIS_NAMESPACE` | `PT1H` / `10` / `1000` / `foodtime:content:uploads` | 图片上传限流；窗口同上 |
| `COMMUNITY_ACTION_WINDOW` / `COMMUNITY_ACTION_USER_LIMIT` / `COMMUNITY_ACTION_IP_LIMIT` / `COMMUNITY_ACTION_REDIS_NAMESPACE` | `PT1M` / `100` / `1000` / `foodtime:community:actions` | 社区点赞/收藏/转发的轻量动作限流；与投稿限流独立计数 |
| `STORAGE_CLEANUP_ENABLED` / `STORAGE_CLEANUP_INTERVAL` / `STORAGE_CLEANUP_MAX_AGE` | `true` / `PT24H` / `P7D` | 孤儿暂存图片清理开关、周期与保留时长；保留时长为0至90天的整秒 |
| `MONITORING_SCRAPE_IP` | `127.0.0.1` | Prometheus抓取允许的来源IP或CIDR；留空拒绝一切抓取。详见[监控](monitoring.md) |
| `AI_TRIAGE_ENABLED` | `false` | AI预检总开关；**生产在用户协议披露并审批前保持关闭** |
| `AI_TRIAGE_PROVIDER` | `openai` | `openai`（OpenAI兼容接口）或 `fake`（确定性演示，仅开发测试） |
| `AI_TRIAGE_BASE_URL` / `AI_TRIAGE_API_KEY` / `AI_TRIAGE_PRIMARY_MODEL` | 空 | `openai` 提供方必填；缺任何一项启动失败。密钥只走环境变量、平台密钥或 systemd 加密凭据，见[密钥安全存储](secrets.md) |
| `AI_TRIAGE_ESCALATION_MODEL` / `AI_TRIAGE_FALLBACK_MODEL` / `AI_TRIAGE_PHOTO_MODEL` | 见 application.yml 默认值 | 升级复核 / 故障兜底 / 图片直诊模型；留空表示不启用对应环节 |
| `AI_TRIAGE_ESCALATION_BELOW_CONFIDENCE` | `0.9` | LOW 首判置信度低于此阈值时触发升级复核（0–1） |
| `AI_TRIAGE_AUTO_APPROVE_ABOVE` / `AI_TRIAGE_AUTO_REJECT_ABOVE` | `0.95` / `0.9` | 评论/点评/回复的自动通过线；HIGH 达到自动驳回线即驳回 |
| `AI_TRIAGE_COMMUNITY_APPROVE_ABOVE` | `0.85` | 社区帖子专用的自动通过线（正文与全部配图均需达到）；评论/点评/回复仍用 `0.95` |
| `EMAIL_MAILBOX_VERIFY_ENABLED` / `EMAIL_MAILBOX_VERIFY_HOST` / `EMAIL_MAILBOX_VERIFY_PORT` / `EMAIL_MAILBOX_VERIFY_TIMEOUT` / `EMAIL_MAILBOX_VERIFY_CACHE_TTL` / `EMAIL_MAILBOX_VERIFY_CACHE_NAMESPACE` | `false` / `smg.bjtu.edu.cn` / `465` / `PT3S` / `PT10M` / `foodtime:auth:mailbox` | 注册邮箱存在性 RCPT 预检；`550` 静默 202 不发信且不消耗发送配额，探测异常 fail-open 继续发码 |
| `AI_TRIAGE_REDIS_NAMESPACE` | `foodtime:ai:triage` | 模型映射覆盖的 Redis 哈希；monitor/超管热切换写这里，删字段即回 env 默认 |
| `AI_TRIAGE_TIMEOUT` / `AI_TRIAGE_BATCH_SIZE` | `PT30S` / `10` | 单次调用超时（1–120秒整秒）；每轮最多诊断条数（1–100） |
| `AI_TRIAGE_INTERVAL` / `AI_TRIAGE_INITIAL_DELAY` | `PT30S` / `PT5S` | 兜底扫批周期与启动延迟（整秒；周期不超过1天，延迟不超过1天） |
| `AI_TRIAGE_POOL_CORE` / `AI_TRIAGE_POOL_MAX` / `AI_TRIAGE_POOL_QUEUE` | `4` / `8` / `500` | 即时审核线程池；新投稿先走即时路径，队列满时由扫批兜底 |
| `AI_SINGLE_ENABLED` | `false` | 单 key 直判开关；开启后多 key 轮询、升级复核、兜底、Redis 模型映射与热切换不再进入。密钥用 systemd 加密凭据注入，见[密钥安全存储](secrets.md) |
| `AI_SINGLE_BASE_URL` / `AI_SINGLE_MODEL` | `.env.example` 为 `https://api.deepseek.com` / `deepseek-flash`（应用内默认空） | 单 key 提供方与模型；`AI_SINGLE_API_KEY` 不写 `.env`，由加密凭据注入 |
| `AI_SINGLE_TIMEOUT` | `PT30S` | 单 key 单次调用超时（1–120 秒整秒） |
| `SLOW_REQUEST_THRESHOLD` | `PT2S` | 慢请求日志阈值，0至300秒的整秒数 |
| `DASHBOARD_PRESENCE_TTL` / `DASHBOARD_PRESENCE_NAMESPACE` | `PT5M` / `foodtime:presence` | 在线用户 presence 窗口与 Redis 前缀；TTL 内有已认证请求的去重用户计为在线 |
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
| `AUTH_DEVICE_SECRET` / `AUTH_DEVICE_COOKIE_NAME` / `AUTH_DEVICE_COOKIE_MAX_AGE` | 复用`EMAIL_VERIFICATION_SECRET` / `foodtime_device` / `P365D` | 注册与发码的签名匿名设备Cookie；不采集指纹，仅用于限流。更换密钥会使旧Cookie失效 |
| `EMAIL_SEND_DEVICE_LIMIT` / `EMAIL_SEND_IP_LIMIT` | `5` / `200` | 每浏览器设备 / 每来源IP发码配额；IP只做高上限兜底，避免校园NAT下正常用户互相挤占 |
| `REGISTRATION_ACCOUNT_LIMIT` / `REGISTRATION_DEVICE_LIMIT` / `REGISTRATION_IP_LIMIT` | `10` / `10` / `1000` | 发码与注册共用的邮箱 / 设备 / IP尝试配额 |
| `SESSION_REDIS_NAMESPACE` | `spring:session` | 会话Redis命名空间（映射到 `spring.session.data.redis.namespace`）；共享 Redis 时应改成独立前缀 |
| `TOMCAT_MAX_THREADS` | `200` | Web 工作线程上限 |
| `LOG_LEVEL` / `APP_LOG_LEVEL` | `INFO` / `INFO` | 根日志 / 应用日志等级 |
| `LOG_FILE` | `logs/foodtime-neo-backend.log` | 日志文件路径 |
| `FORWARD_HEADERS_STRATEGY` | `none` | 受信任反向代理统一设置转发头时可改为 `framework` |
| `POSTGRES_DB` / `POSTGRES_PORT` | `foodtime_neo` / `5432` | 仅供 Compose 初始化与本机端口映射 |

其他共享设置：优雅停机等待 30 秒，SQL 超时 10 秒，日志按 10MB 滚动、保留 14 天、总量上限 200MB；业务时间使用 `Instant`，数据库连接使用 UTC；API 使用 ISO 8601 时间。跨域配置仅应用于 `/api/**`，仅对明确配置的受信任来源允许Cookie凭据。

## 文件存储

`ImageStorageService` 读取 `app.storage.directory`，默认是进程工作目录下的 `uploads`。可通过启动参数 `--app.storage.directory=/var/lib/foodtime/uploads` 指定持久化目录；需保证运行账号可读写。调大multipart配置不会取消存储服务的10MiB和2000万像素业务限制。

数据库迁移不会创建图片字节。V5菜单图片、V6食堂封面及 `.properties` 元数据的铺放方式见 [种子数据与图片部署](seed-data.md)。容器挂载见 [部署说明](deployment.md)。

点评轻量动作使用 REVIEW_ACTION_WINDOW / REVIEW_ACTION_USER_LIMIT / REVIEW_ACTION_IP_LIMIT / REVIEW_ACTION_REDIS_NAMESPACE，默认 PT1M / 100 / 1000 / foodtime:review:actions；与内容投稿及社区互动独立。

## 注册与图片容量保护

注册和发码请求共同按邮箱/设备/IP计数，默认15分钟每邮箱10次、每设备10次、每IP1000次，独立于发码冷却和发送配额；Redis不可用时拒绝请求。发码另按邮箱5次、每设备5次、每IP200次计数。参数见`.env.example`中的`REGISTRATION_*`和`EMAIL_SEND_*`。

上传图片统一解码重编码，移除EXIF/GPS、注释及尾部附加内容；JPEG按EXIF方向修正像素，PNG保留透明度。处理后仍不超过10MB；并发上传/首次缩略图解码默认最多2项，繁忙返回429。

磁盘文件实时计入配额，默认每用户100MiB、上传目录总计10GiB、磁盘预留1GiB。包含原图、缩略图及元数据；全局还计入孤儿文件。超用户配额返回413，全局容量或磁盘余量不足返回503。可用`STORAGE_USER_BYTES`、`STORAGE_TOTAL_BYTES`、`STORAGE_MIN_FREE_BYTES`调整；不会删除已有图片。现有旧图片的元数据不追溯改写，新增上传才进行清理。

单机本地文件系统通过文件锁串行核对配额并写入，跨进程配额不依赖内存计数。当前为平面目录核对；大规模媒体库应迁移至数据库索引/对象存储配额，不应将此实现用于缺少一致文件锁的网络文件系统。
