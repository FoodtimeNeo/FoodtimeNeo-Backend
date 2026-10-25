# DDNS 快速上线（kfcvivo50.ddns.net，临时域名）

配服的人按这份照做即可；原理与生产编排见 [生产与容器部署](deployment.md)。

## 前置检查（缺一不可）

1. DDNS 已解析到服务器公网 IP：`nslookup kfcvivo50.ddns.net` 与服务器出口 IP 一致。
2. 路由器/光猫放行 **80 + 443** 到这台机器（`compose.prod.yml` 的 app 只绑了 `127.0.0.1:8080`，公网必须走 Nginx）。
3. 后端已按 `compose.prod.yml` 启动（`app` 健康，`SPRING_PROFILES_ACTIVE=prod`）。

## 步骤

1. 装 Nginx + certbot：`sudo apt install nginx certbot python3-certbot-nginx`
2. 放配置：
   `sudo cp deploy/ddns-nginx/kfcvivo50.ddns.net.conf /etc/nginx/sites-available/kfcvivo50.ddns.net.conf`
   `sudo ln -s /etc/nginx/sites-available/kfcvivo50.ddns.net.conf /etc/nginx/sites-enabled/`
3. 申请证书：`sudo certbot --nginx -d kfcvivo50.ddns.net`
4. 重载：`sudo nginx -t && sudo systemctl reload nginx`
5. 续期自检：`sudo certbot renew --dry-run`（Debian 自带 timer 自动续）

80 端口被运营商封锁时 HTTP-01 会失败，改用 DNS-01（需 DDNS 服务商的 API Token，
`certbot --preferred-challenges dns -d kfcvivo50.ddns.net`，按提示加一条 TXT 记录）。

## 验证（逐条过才算完）

```bash
# 80 只跳转不代理
curl -s -o /dev/null -w "%{http_code}\n" http://kfcvivo50.ddns.net/api/v1/auth/csrf   # 期望 301
# https 可达 + 安全头
curl -sI https://kfcvivo50.ddns.net/api/v1/auth/csrf | grep -iE "HTTP/|strict-transport|x-content-type|x-frame|referrer"
# 会话 Cookie 必须 Secure + HttpOnly（后端 prod 默认 cookie-secure=true）
curl -si https://kfcvivo50.ddns.net/api/v1/auth/csrf | grep -i "set-cookie"
# 真实客户端 IP 进后端：登录失败几次后看后端日志 IP，应为你的出口 IP 而不是 127.0.0.1
```

## 前后端同源说明

- Web dist 与后端同机（nginx `location /` 取消注释）：浏览器同源，**后端 CORS 不用动**。
- Web 另外部署：把它的 https 域名追加进生产 env 的 `CORS_ALLOWED_ORIGINS`（逗号分隔），不要写通配符。

## Android release 包

门禁要求 release 的 base URL 必须是 https（`feature/https-enforcement`，默认 http 会直接失败）。
证书就绪后这样打 release 包，域名不要写死进代码（DDNS 是临时的）：

```powershell
.\gradlew.bat assembleRelease -PFOODTIME_API_BASE_URL=https://kfcvivo50.ddns.net/api/v1/
```

## DDNS 变更须知

- 证书跟域名不跟 IP：IP 变了只要 DDNS 更新，https 不受影响，不用换证书。
- Android 门禁只校验 scheme（https），不校验具体域名，换正式域名时同样一条 `-P` 命令即可。
- DDNS 只是临时方案：拿到正式域名后，把本目录 conf 与本文档的域名替换掉重走一遍即可。
