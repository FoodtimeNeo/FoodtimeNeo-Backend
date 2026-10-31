# 生产与容器部署

本地 Compose 提供 PostgreSQL 和 Redis，端口仅绑定本机；生产部署需配置自己的数据库、Redis 与网络访问规则。登录使用Redis服务端会话和HttpOnly Cookie；新增业务接口默认需要登录，管理员接口还须实现对应的服务端角色权限校验。

GitHub Actions 在面向main/dev的PR和push时执行CI。CD按用户选择仅保留模板，当前未启用；main合并不会自动发布镜像或部署服务。后续启用步骤及预留条件见 [CI/CD](ci-cd.md)。

生产环境必填：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PASSWORD`。例如在部署平台设置 `SPRING_PROFILES_ACTIVE=prod` 并注入这些变量；远程 PostgreSQL 的 JDBC URL 可配置 `sslmode=verify-full`，Redis TLS 使用 `REDIS_SSL_ENABLED=true`。生产默认仅暴露健康端点，健康详情隐藏，API 文档关闭。

Dockerfile 使用 Java 21 多阶段构建，以非 root 用户运行，容器默认启用 `prod`：

```powershell
docker build -t foodtime-neo-backend:local .
# 创建仅供容器使用、指向可从容器访问的数据库 / Redis 的环境文件
.\scripts\place-seed-images.ps1
$uploadsPath = (Resolve-Path -LiteralPath .\uploads).Path
docker run --rm --env-file .env.container --mount "type=bind,source=$uploadsPath,target=/app/uploads" -p 8080:8080 foodtime-neo-backend:local
```

容器中的 `localhost` 指向容器本身，因此 `.env.container` 中必须填写实际可达的服务地址，并使用 `SPRING_PROFILES_ACTIVE=prod`。若连接开发机服务，可根据 Docker Desktop 配置使用 `host.docker.internal`。

上述PowerShell命令从Backend根目录执行；Linux或远程部署需将准备好的图片和元数据放到宿主机持久化目录，再挂载到 `/app/uploads`。非root的 `foodtime` 用户必须能读写该目录。Dockerfile只复制应用JAR，不复制 `seed-images/`；V5/V6数据库迁移成功并不代表图片已经可读。详见 [种子数据与图片部署](seed-data.md)。生产Web应由HTTPS入口同时转发页面与 `/api/v1`，并保留媒体请求；本地Vite代理不承担生产部署。

## 反向代理与客户端IP（上线前必做）

所有写接口限流同时按用户与按IP计数，IP取自连接地址。直连部署不用改；一旦经过Nginx等反向代理，
所有用户在后端看来都是同一个代理IP，整校会共用每IP配额（登录每15分钟1000次等），饭点必被误伤。
代理部署必须同时做这两件事：

1. 代理层覆写转发头，只传真实客户端IP（示例：`proxy_set_header X-Forwarded-For $remote_addr;`，
   同时丢弃客户端自带的 `X-Forwarded-For`，防止伪造）。
2. 后端设置 `FORWARD_HEADERS_STRATEGY=framework`，让Spring从 `X-Forwarded-For` 还原客户端IP；
   该值仅在可信代理之后才能启用，直连或代理未清洗请求头时保持 `none`。

校园网多为统一出口，单个出口IP背后可能有数百学生：每IP配额默认值已按此放大10倍
（登录1000、投稿2000、上传1000，窗口不变），真正的单人滥用仍由每用户/每账号配额卡住。
若出口IP更集中，继续调大 `*_IP_LIMIT` 即可，不动用户级配额。

`.gitignore` 已覆盖 Maven 构建目录、IDE 状态、本地环境配置、日志、运行数据和私钥；保留 `.env.example`、Maven Wrapper 与版本化数据库迁移。`.dockerignore` 将本地凭据、Git 与构建缓存排除在镜像构建上下文之外。

## 2核4G Debian 单机调配

`compose.prod.yml` 是按 2C4G 单机（应用+PG+Redis 同机）调好的生产编排，
内存预算合计约 3.1G，留 ~900M 给系统与峰值波动：

| 组件 | 配置 | 理由 |
| --- | --- | --- |
| app | `JAVA_OPTS=-Xms768m -Xmx1280m`，`mem_limit` 1600m | 本地压测 150 并发下堆压力不大，1280M 上限足够；非堆（线程/元空间/直接内存）约 450M 打包进 limit |
| app | `TOMCAT_MAX_THREADS=50` | 读链路单请求约 70ms 且多为序列化 CPU 活；2 核下 50 线程已能打出 ~600 rps，200 线程只会增加切换与栈内存 |
| postgres | `shared_buffers=256MB`、`max_connections=60`、`mem_limit` 1g | 压测中连接池仅用 1–4 连接；60 上限防慢连接堆积，256M shared_buffers 够吃千级菜品+万级评论 |
| redis | `maxmemory 256mb` + `volatile-lru` | 会话与限流键都有 TTL，只淘汰带过期时间的键；会话永不因内存被提前踢掉 |
| cpu | app 1.5 / pg 0.5 / redis 0.25 | 读链路是 CPU 密集的 JSON 序列化，优先保应用 |

Nginx 装本机即可（`worker_processes 2`），公网 443 同时转发页面与 `/api/v1`，
并按上一节覆写 `X-Forwarded-For`。AI 预检是调远端模型接口，不占本机 GPU（本来也没有）；
但生产默认关闭，上线前按 `ai-moderation.md` 走用户协议披露与审批再开。
磁盘重点看 `/app/uploads` 与日志卷，`STORAGE_DISK_WARN_BYTES` 默认 1G 告警；
首周盯 `monitoring.md` 的 p95、429 比例与 `hikaricp_connections_pending`（持续大于 0 再考虑加池）。
