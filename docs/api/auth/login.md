# 登录与会话

账号使用注册时的完整邮箱。登录通过服务端验证 Argon2id 密码哈希，使用 Spring Security 和 Spring Session Redis 保存登录状态。浏览器持有不透明的 `FOODTIME_SESSION` Cookie；JSON响应不返回会话ID或密码。禁止将会话凭据存入 localStorage。

## 接口

| 方法与路径 | 登录要求 | 用途 |
| --- | --- | --- |
| `GET /api/v1/auth/csrf` | 无 | 获取当前浏览器会话的CSRF令牌；必要时创建匿名会话 |
| `POST /api/v1/auth/login` | 无，但需要Cookie和CSRF令牌 | 校验邮箱账号与密码，创建新的登录会话 |
| `GET /api/v1/auth/me` | 是 | 返回当前用户，并检查账号和会话是否仍有效 |
| `POST /api/v1/auth/logout` | 是，且需要CSRF令牌 | 删除当前Redis会话并清除浏览器Cookie |

注册接口仍可匿名调用，但必须提交通过校验的邮箱验证码，不自动登录。注册发送验证码接口匿名可调用且需要CSRF令牌，详见 [邮箱验证码](email-verification.md)。其他业务接口默认需要登录；运行检查、健康检查和开发环境API文档允许匿名读取。

## 正确调用顺序

1. `GET /csrf`，保存响应中的 `data.headerName` 和 `data.token`；浏览器接收会话Cookie。
2. `POST /login`，携带Cookie、`Content-Type: application/json` 和 `X-CSRF-TOKEN: <token>`。
3. 登录成功后再次 `GET /csrf`：登录会替换整个会话，旧CSRF令牌已失效。
4. 之后通过Cookie访问 `/me` 或业务接口；所有有副作用的请求携带最新CSRF令牌。
5. `POST /logout` 后Cookie被清除，原凭据即使重放也不能继续访问。

前端使用 `fetch` 时，每次请求都设置 `credentials: 'include'`。CSRF令牌仅保留在页面内存中；页面刷新后重新获取。

```javascript
const base = 'http://localhost:8080/api/v1/auth';
async function getCsrf() {
  const response = await fetch(`${base}/csrf`, { credentials: 'include' });
  if (!response.ok) throw new Error('无法获取CSRF令牌');
  return (await response.json()).data;
}
let csrf = await getCsrf();
const response = await fetch(`${base}/login`, {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
  body: JSON.stringify({ account: '20260001@bjtu.edu.cn', password: 'Foodtime2026!' })
});
const result = await response.json();
if (!response.ok) throw new Error(result.message);
csrf = await getCsrf();
```

## 登录请求

```json
{ "account": "20260001@bjtu.edu.cn", "password": "Foodtime2026!" }
```

| 字段 | 规则 |
| --- | --- |
| `account` | 必填，完整邮箱，最多254位；去除首尾空白并转为小写。使用数据库现有邮箱查询，不支持昵称或仅输入学号 |
| `password` | 必填，8至128位，只允许英文字母、数字及ASCII半角符号，禁止空白与非ASCII字符；不裁剪、不修改大小写。登录校验原密码，不重新执行注册时的复杂度规则 |

客户端提交的其他属性（包括角色、账号状态）不会参与身份授权。登录角色来自数据库。未知账号、错误密码和禁用账号统一返回相同的401信息；未知账号仍执行一次哈希比较，以减少明显的耗时差异。

## 成功响应

登录成功返回200，并设置Cookie。示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "user": { "id": "9e55b67f-f051-4019-bafd-a4082c95f2a6", "email": "20260001@bjtu.edu.cn", "displayName": "干饭人012345", "role": "user" },
    "expiresAt": "2026-10-11T00:00:00Z"
  },
  "timestamp": "2026-10-04T00:00:00Z",
  "requestId": "login-example"
}
```

`GET /me` 返回200，`data` 为上述 `user` 对象。`POST /logout` 返回200，`data` 为null。`GET /csrf` 返回200，`data` 为 `{ "headerName": "X-CSRF-TOKEN", "token": "..." }`。认证响应采用 `Cache-Control: no-store`。

## 会话规则

- 默认从登录成功起固定7天到期；访问不会延长到期时间。Cookie设置 `HttpOnly`、`SameSite=Lax`、`Path=/`、`Max-Age=604800`，不设置Domain。
- 生产环境必须使用HTTPS和Secure Cookie；本地dev默认允许HTTP。生产启动拒绝 `AUTH_COOKIE_SECURE=false`。
- 每次成功登录都会删除该浏览器的旧会话并生成新会话，防止会话固定；不同设备可以分别登录，退出仅注销当前会话。
- Redis只保存最小身份信息、密码变更时间、到期时间和框架安全上下文，不保存密码或密码哈希。
- 每次已登录请求从数据库检查用户状态和密码变更时间；禁用账号、删除账号、密码变更或到期均使原会话失效。权限角色也使用数据库当前值。
- [修改密码接口](../user/password.md) 同时更新哈希和 `password_changed_at`，注销当前会话；其他旧会话在下一次请求时失效，必须重新登录。
- 更新 `last_login_at` 时同时检查账号仍处于active状态且密码哈希未变化，避免验证过程中发生密码修改仍授予会话。
- Redis丢失会话或Redis数据被清理时需重新登录。基础设施不可用时认证失败关闭，返回503，不退回本地会话或绕过限流。
- 新注册账号必须先验证邮箱验证码，再创建账号并填写 `email_verified_at`。此前已有的未验证账号不会被自动标记为已验证；本次未改变这些历史账号的登录策略。

## 限流与失败响应

Redis通过Lua原子计数，默认每个规范化账号15分钟最多10次登录尝试，每个来源IP15分钟最多100次；成功与失败都计数，计数不因换浏览器或重新获取CSRF令牌而重置。Redis键只包含账号/IP的SHA-256摘要。默认不信任客户端自行发送的 `X-Forwarded-For`；只有受信任反向代理确实清洗转发头后才启用转发头处理。

| HTTP | code | 说明 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 缺少字段或非法字段；data包含字段名与原因，非法Unicode不进入密码哈希 |
| 400 | `BAD_REQUEST` | JSON无法解析或请求体缺失 |
| 401 | `INVALID_CREDENTIALS` | 账号或密码错误（也用于不存在及禁用账号） |
| 401 | `UNAUTHENTICATED` | 未登录、Cookie被伪造、会话失效或到期 |
| 403 | `CSRF_INVALID` | CSRF令牌缺失或无效；重新获取后重试 |
| 403 | `ACCESS_DENIED` | 无权访问资源 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 登录请求须为JSON |
| 429 | `LOGIN_RATE_LIMITED` | 登录尝试过于频繁；响应 `Retry-After` 表示至少等待的秒数 |
| 503 | `AUTH_UNAVAILABLE` | PostgreSQL或Redis无法完成认证/会话读写，稍后重试 |
| 500 | `INTERNAL_ERROR` | 未预期服务端错误，不返回内部详情 |

没有有效会话时，直接POST退出可能先返回403（缺少CSRF）或401；客户端可以清除自己的页面登录状态，再获取新的CSRF令牌。失败响应不包含原密码、哈希、SQL、Cookie内容或堆栈。

## 配置与实现

| 配置项 | 默认值 | 作用 |
| --- | --- | --- |
| `AUTH_SESSION_TTL` | `P7D` | 登录会话固定期限，正整数秒，最多30天 |
| `AUTH_COOKIE_SECURE` | prod为true；dev为false | Cookie仅HTTPS传输；生产禁止false |
| `SESSION_REDIS_NAMESPACE` | `foodtime:session` | 会话Redis命名空间 |
| `AUTH_REDIS_NAMESPACE` | `foodtime:auth:login` | 登录限流Redis命名空间 |
| `AUTH_LOGIN_WINDOW` | `PT15M` | 限流窗口，正整数秒，最多30天 |
| `AUTH_LOGIN_ACCOUNT_LIMIT` | `10` | 每个账号在窗口内的登录次数 |
| `AUTH_LOGIN_IP_LIMIT` | `100` | 每个IP在窗口内的登录次数 |

匿名CSRF会话空闲15分钟到期；登录成功替换为具有绝对期限的认证会话。Redis写入使用immediate模式。前后端应使用同站部署（如同域名或同站子域名）；`SameSite=Lax`不适合完全跨站的Cookie部署。CORS只允许配置的明确来源，并允许凭据和 `X-CSRF-TOKEN` 请求头。

`api/auth` 管理HTTP协议；`auth/service` 校验凭据与登录限流；`auth/security` 管理会话、期限、账号状态和过滤器错误；`user/mapper` 读取用户和更新登录时间；`config/SecurityConfig` 统一配置安全链和Cookie。新增了 `spring-boot-starter-security`、`spring-boot-starter-session-data-redis`，版本交由Spring Boot管理；Maven自动解析，无需手动安装额外服务。

框架依据：[Spring Boot会话配置](https://docs.spring.io/spring-boot/reference/web/spring-session.html)、[Spring Security自定义认证持久化](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)、[Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)。
