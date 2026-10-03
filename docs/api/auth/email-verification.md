# 注册邮箱验证码

`POST /api/v1/auth/register/email-code`，JSON请求，匿名可调用，但必须带当前会话Cookie和CSRF令牌。先 `GET /api/v1/auth/csrf`，再携带 `X-CSRF-TOKEN` 发送请求；浏览器设置 `credentials: 'include'`。CSRF调用方式见 [登录文档](login.md)。

## 请求与响应

```json
{ "email": "20260001@bjtu.edu.cn" }
```

邮箱必填，去除首尾空白、转为小写后，须为数字前缀加 `@bjtu.edu.cn`，最多254位。已注册邮箱返回409，不发送邮件。

SMTP接受邮件且验证码写入Redis后，返回 **202 Accepted**：

```json
{
  "code": "OK",
  "message": "success",
  "data": { "expiresAt": "2026-10-04T01:15:00Z", "resendAfterSeconds": 60 },
  "timestamp": "2026-10-04T01:00:00Z",
  "requestId": "email-code-example"
}
```

邮件主题为“FoodtimeNeo 注册验证码”，正文包含6位数字验证码（包括可能的前导零）和15分钟有效期提示。202表示SMTP服务已接受，并不保证收件箱即时送达；客户端应提示检查收件箱及垃圾邮件。响应、日志和接口文档不返回实际验证码。

收到邮件后调用 [注册接口](register.md)，提交同一邮箱、密码和 `verificationCode` 字符串。此接口只发送验证码，不创建用户；注册成功后服务端设置邮箱验证时间。

## 有效期及安全规则

- 使用 `SecureRandom` 生成000000–999999之间的6位数字码，绑定规范化后的邮箱。
- Redis只保存以服务器密钥计算的HMAC-SHA256摘要、尝试次数及临时校验占用；不保存明文验证码。Redis键中的邮箱/IP也使用SHA-256摘要。
- 每个验证码从SMTP接受并成功写入Redis后固定 **15分钟** 到期；错误校验或重试不延长TTL，Redis TTL为最终期限依据。
- 同邮箱默认60秒内不能重发；15分钟内每个邮箱最多发送5次、每个来源IP最多发送20次。失败的SMTP尝试也消耗发送配额，但会释放本次重发间隔。
- 发送配额和重发控制使用Redis Lua原子操作，无法通过并发请求或换浏览器绕过。默认不信任客户端自填的转发IP头；代理策略沿用登录配置。
- 验证码最多允许5次错误校验，达到上限后失效，需重新发送。格式错误的请求返回400；没有正确验证码不能进行密码哈希或新增账号。
- 重发成功后替换旧验证码；重发失败保留此前尚未到期的有效验证码。极小概率下新旧随机码可能相同。
- 验证成功的请求临时占用验证码，其他并发注册请求返回409。注册成功后删除验证码；写入失败释放占用，可在原有效期内重试。不会覆盖更新的验证码或占用。
- SMTP或Redis异常返回503，不将验证码写入日志，也不会绕过验证创建账号；账号唯一索引继续防止成功注册后的重放。
- 未配置或未启用邮件功能时，发送接口和需要验证码的注册都返回503；关闭邮件功能不代表允许无验证码注册。

## 失败码

| HTTP | code | 说明 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` / `BAD_REQUEST` | 邮箱不合法、缺少字段或JSON格式错误 |
| 403 | `CSRF_INVALID` | 缺少或无效CSRF令牌；重新获取后重试 |
| 409 | `EMAIL_ALREADY_REGISTERED` | 邮箱已注册 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 请求必须为JSON |
| 429 | `EMAIL_SEND_RATE_LIMITED` | 重发间隔、发送配额或正在注册的占用限制；`Retry-After` 为至少需等待的秒数 |
| 503 | `EMAIL_VERIFICATION_UNAVAILABLE` | SMTP未配置、未启用、发送失败，或Redis/数据库无法完成验证流程 |

错误响应不返回SMTP授权码、HMAC密钥、原验证码或服务端堆栈。

## 邮箱配置

模板：[config/mail.example.yml](../../../config/mail.example.yml)。实际配置路径为 `config/mail.yml`，启动时从Backend当前工作目录读取，已加入 `.gitignore` 和 `.dockerignore`。模板本身保留在Git中。

```powershell
# Backend目录运行。若实际配置已存在，先编辑现有文件，不覆盖邮箱授权码。
if (-not (Test-Path -LiteralPath config/mail.yml)) {
  Copy-Item -LiteralPath config/mail.example.yml -Destination config/mail.yml
}
```

填写实际配置中的以下字段，也可以用对应环境变量注入：

| 文件字段 | 环境变量 | 说明 |
| --- | --- | --- |
| `spring.mail.host` | `MAIL_HOST` | SMTP服务器域名，不能保留smtp.example.com占位符 |
| `spring.mail.port` | `MAIL_PORT` | 默认587（STARTTLS）；隐式TLS常用465，以邮箱服务商要求为准 |
| `spring.mail.username` | `MAIL_USERNAME` | SMTP登录账号 |
| `spring.mail.password` | `MAIL_PASSWORD` | SMTP授权码/App密码；不是网站用户的登录密码 |
| `app.email-verification.from` | `MAIL_FROM` | 发件邮箱，须为SMTP账号有权使用的地址 |
| `app.email-verification.secret` | `EMAIL_VERIFICATION_SECRET` | 至少32字符的随机密钥，多个应用实例必须一致 |
| `app.email-verification.enabled` | `EMAIL_VERIFICATION_ENABLED` | 填好配置后设为true，默认false |

生成随机密钥的PowerShell示例（仅在本机生成，粘贴到被忽略的实际配置或环境变量中）：

```powershell
$verificationBytes = New-Object byte[] 32
$verificationRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$verificationRng.GetBytes($verificationBytes)
[Convert]::ToBase64String($verificationBytes)
$verificationRng.Dispose()
```

默认启用SMTP认证、STARTTLS和强制STARTTLS。若服务商要求465隐式TLS，设置 `MAIL_SSL_ENABLED=true`、`MAIL_STARTTLS_ENABLED=false`、`MAIL_STARTTLS_REQUIRED=false`，并改端口；对应文件键在模板注释中给出。服务器身份校验默认开启，SMTP连接、读取和写入超时均为5秒。生产环境启动拒绝未启用TLS的邮件配置；SMTP认证启用时，账号和授权码不能为空。

容器部署时通过环境变量/部署平台密钥注入，或将实际配置只读挂载到 `/app/config/mail.yml`。正式配置不会复制进Docker镜像。切勿把SMTP授权码或随机密钥写入模板。修改HMAC密钥会使已有待用验证码失效，建议保持密钥稳定。

发送限流可配置 `EMAIL_RESEND_INTERVAL`（默认PT60S）、`EMAIL_SEND_WINDOW`（PT15M）、`EMAIL_SEND_ACCOUNT_LIMIT`（5）、`EMAIL_SEND_IP_LIMIT`（20）、`EMAIL_VERIFICATION_NAMESPACE`（foodtime:auth:email）；间隔和窗口须为60秒至1天的正整数秒。验证码有效期固定15分钟，错误上限固定5次。

新增项目依赖 `spring-boot-starter-mail`，由Maven自动下载，版本跟随Spring Boot；无需手动安装Java邮件库。真实邮件投递需要用户配置可用SMTP账号；测试使用仅监听本机的SMTP服务，不向外部邮箱投递。

框架配置依据：[Spring Boot邮件配置](https://docs.spring.io/spring-boot/reference/io/email.html)、[Spring Framework邮件发送](https://docs.spring.io/spring-framework/reference/integration/email.html)。
