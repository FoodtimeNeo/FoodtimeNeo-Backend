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
3. 后端只监听 `127.0.0.1:8080`（同机反代），或在防火墙/安全组中只允许代理访问 8080；
   否则外部可以绕过 Nginx 直连后端并伪造 `X-Forwarded-For`，使每 IP 限流失效。
   容器部署应在容器内监听 `0.0.0.0`，但宿主机发布端口限制为 `127.0.0.1:8080:8080`。

Nginx 边缘再加一层全局 API 速率上限，与应用层的 `PROTECTION_*` 并发上限配合：

```nginx
# http 块
limit_req_zone $server_name zone=api_global:10m rate=100r/s;
limit_req_status 429;
limit_req_log_level warn;

# location /api/v1/ 块
limit_req zone=api_global burst=200 nodelay;
```

2核/1GB堆的当前部署使用应用层全局100并发、发码30、注册/登录各12；
Nginx全局100 req/s、burst 200。超限直接429，不排队等待，避免线程、SMTP和Argon2被瞬间打满。
流量规模变化时只调这些上限，不要盲目提高 Tomcat 线程数。

校园网多为统一出口，单个出口IP背后可能有数百学生：每IP配额默认值已按此放大10倍
（登录1000、投稿2000、上传1000，窗口不变），真正的单人滥用仍由每用户/每账号配额卡住。
若出口IP更集中，继续调大 `*_IP_LIMIT` 即可，不动用户级配额。

`.gitignore` 已覆盖 Maven 构建目录、IDE 状态、本地环境配置、日志、运行数据和私钥；保留 `.env.example`、Maven Wrapper 与版本化数据库迁移。`.dockerignore` 将本地凭据、Git 与构建缓存排除在镜像构建上下文之外。

## 域名接入与 IP 访问

站点需要同时接受域名（`bjtufoodtime.top`）与 IP 访问时，Nginx 必须显式声明两个入口。只写一个
`server_name` 时，用 IP 或其他主机名访问会落到默认站点，页面通常返回 404，而 `/api/` 仍可能正常，
容易误判为"站点挂了、接口还在"。

### 证书

| 入口 | 证书要求 |
| --- | --- |
| 域名 | SAN 同时包含 `bjtufoodtime.top` 与 `www.bjtufoodtime.top` |
| IP | SAN 包含 `IP Address:<服务器IP>` |

浏览器与 App 都会做主机名校验，**证书 SAN 不匹配时 HTTPS 直接失败**，不是"只是有个警告"。
两个入口可以共用一份同时包含域名与 IP 的证书；否则需要分别为两个 `server` 块配置证书路径。
无论哪种方式，都要确认自动续期同时覆盖域名与 IP 证书。DNS 侧需同时提供
`bjtufoodtime.top` 与 `www.bjtufoodtime.top` 指向本机的 A/AAAA 记录（或 www CNAME 到主域名），
否则 `www` 的 HTTP-01 验证和 HTTPS 主机名校验都会失败。

### Nginx 结构

```nginx
# 域名入口：页面 + API
server {
    listen 443 ssl;
    server_name bjtufoodtime.top www.bjtufoodtime.top;

    ssl_certificate     /etc/letsencrypt/live/bjtufoodtime.top/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/bjtufoodtime.top/privkey.pem;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header X-Forwarded-For $remote_addr;   # 覆写，丢弃客户端自带值
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;
    }
    location / {
        root /var/www/foodtime;
        try_files $uri $uri/ /index.html;
    }
}

# IP 入口：作为默认站点兜底，同样提供页面与 API
server {
    listen 443 ssl default_server;
    server_name _;

    ssl_certificate     /etc/letsencrypt/live/<ip-cert>/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/<ip-cert>/privkey.pem;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;
    }
    location / {
        root /var/www/foodtime;
        try_files $uri $uri/ /index.html;
    }
}
```

要点：

- 必须显式写 `default_server`，否则 IP 访问命中哪个 server 块取决于配置顺序。
- 两个块都要覆写 `X-Forwarded-For`，否则每 IP 限流全部落在代理地址上。
- 启用 `FORWARD_HEADERS_STRATEGY=framework` 后，后端才按真实客户端 IP 计数。

### 后端配置

```bash
# 同源部署（页面与 API 同一主机）不需要 CORS；该值用于前端分域或本地预览
CORS_ALLOWED_ORIGINS=https://bjtufoodtime.top,https://www.bjtufoodtime.top
```

证书续期必须让 Nginx 重新加载证书。当前部署使用 certbot 的
`renewal-hooks/deploy/reload-nginx` 钩子；IP 证书使用独立的
`/etc/letsencrypt-ip` 配置目录，已有对应的 reload 钩子。新增证书目录时不要只验证续期成功，
还要确认续期后会执行 `nginx -s reload`。

### 迁移后的验证

```bash
curl -sI https://bjtufoodtime.top/api/v1/system/ping | head -1      # 200
curl -sI https://www.bjtufoodtime.top/api/v1/system/ping | head -1  # 200
curl -sI https://<服务器IP>/api/v1/system/ping        | head -1      # 200
curl -s  https://bjtufoodtime.top/ | head -3                        # 页面
curl -s  https://www.bjtufoodtime.top/ | head -3                    # 页面
curl -s  https://<服务器IP>/ | head -3                              # 页面（IP 访问已恢复）
echo | openssl s_client -connect bjtufoodtime.top:443 -servername bjtufoodtime.top 2>/dev/null   | openssl x509 -noout -ext subjectAltName                          # 应包含域名
echo | openssl s_client -connect <服务器IP>:443 2>/dev/null   | openssl x509 -noout -enddate                                     # 确认未临近过期
```

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

## 图片上传与 Android 更新（部署检查清单）

以下两项是 1.2.0 之后新增的行为，升级后端或发布新 APK 时按需核对。

### 1. 图片上传上限提升到 50MB

后端默认值已改为：单文件 50MB、整个 multipart 请求 60MB、字节级上限 60MB。
如果服务器或容器显式设置了旧值，必须同步调大，否则上传仍会被 413 拒绝：

```dotenv
UPLOAD_MAX_FILE_SIZE=50MB
UPLOAD_MAX_REQUEST_SIZE=60MB
REQUEST_MAX_MULTIPART_BYTES=62914560
```

经过 Nginx 时还要放宽 `client_max_body_size`（默认 1MB），建议放在 `server` 或 `location` 块：

```nginx
client_max_body_size 60m;
```

行为说明：原图超过 10MB 时服务端会按比例降采样并重编码到 5MB 左右，
其余图片处理后硬上限 10MB；解码时超过 2000 万像素也会降采样。
缩略图仍是 480px、KB 级，前端展示不受影响。存储配额（`STORAGE_USER_BYTES` 等）按处理后的实际大小计算。

### 2. Android 应用内更新与增量补丁

`/api/v1/system/app-release` 仍由 `APP_RELEASE_*` 驱动，未配置则不提示：

```dotenv
APP_RELEASE_VERSION_CODE=10
APP_RELEASE_VERSION_NAME=1.3.0
APP_RELEASE_APK_URL=https://github.com/FoodtimeNeo/FoodtimeNeo-Android/releases/download/v1.3.0/app-release.apk
APP_RELEASE_NOTES=本次更新说明……
APP_RELEASE_MIN_VERSION_CODE=0
```

- **增量下载的前提**：`APP_RELEASE_APK_URL` 指向 GitHub Release 的 APK 资产，且与 `manifest.json` 位于同一目录。
  客户端会请求 `manifest.json`，匹配当前安装版本号的 bsdiff 补丁并只下载补丁（通常 1–3MB）；
  拿不到 manifest、没有对应补丁或补丁校验失败时，自动回退下载完整 APK，不需要人工干预。
- Android CD 在 `main` 发布时会为**最近三个正式版本**生成补丁，并上传
  `app-release.apk`、`app-release.apk.sha256`、`manifest.json` 与 `app-release-<旧>-<新>.patch`。
  使用其他下载地址（自建 CDN、网盘）时不会命中增量，只会走完整包下载。
- `APP_RELEASE_MIN_VERSION_CODE` 大于当前安装版本时，更新弹窗不可跳过；更新弹窗会先于公告显示。
- 客户端下载完成后会校验完整 APK 的 SHA-256 并直接调起系统安装器，
  用户需要允许本应用「安装未知来源应用」。

### 3. 迁移版本号变更（V35 → V36）

`dev` 上曾同时存在两个 `V35` 迁移（访客账号与 AI 驳回）。为避免 Flyway 启动失败，
较晚合入的 AI 迁移已改名为 `V36__catalog_ai_rejection_only.sql`。

只有在 **2026-11-02 14:57 之后、该修复合并之前**部署过包含 AI 功能构建的数据库，
才可能出现历史记录不匹配。上线前可检查：

```sql
SELECT version, description, success FROM foodtime.flyway_schema_history ORDER BY installed_rank;
```

若存在 `version='35' AND description='catalog ai rejection only'`，说明旧构建已执行过该迁移，
需要先把历史记录改成 V36，再启动新版本（迁移内容不变，checksum 不变）：

```sql
UPDATE foodtime.flyway_schema_history
SET version = '36'
WHERE version = '35' AND description = 'catalog ai rejection only';
```

全新数据库或一直部署的是合并后的版本则无需处理。
