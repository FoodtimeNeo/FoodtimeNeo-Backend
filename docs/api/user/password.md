# 修改当前用户密码

`PUT /api/v1/users/me/password`

需要有效的登录Cookie和当前会话的CSRF令牌；只修改服务端识别的当前用户。此接口验证旧密码，不属于忘记密码或邮箱找回流程。

## 请求

登录成功后重新调用 `GET /api/v1/auth/csrf`，携带Cookie、`Content-Type: application/json` 和返回的 `X-CSRF-TOKEN`：

```json
{
  "oldPassword": "Old12345!",
  "newPassword": "New12345!"
}
```

| 字段 | 规则 |
| --- | --- |
| `oldPassword` | 必填，8至128位英文字母、数字或ASCII半角符号；须与当前密码匹配 |
| `newPassword` | 必填，8至128位英文字母、数字或ASCII半角符号；至少含数字和英文字母，且与旧密码不同 |

密码保持原样，不裁剪、不修改大小写；空白、中文、emoji、全角字符和控制字符均不允许。额外提交 `userId`、`email`、`role` 等属性不会用于选择账号或修改权限。

## 成功与会话处理

成功返回 **200 OK**，采用统一响应，`code` 为 `OK`、`data` 为null，不返回新密码、哈希或会话凭据。

- 新密码通过现有Argon2id策略生成随机盐哈希；数据库一次条件更新同时写入哈希和 `password_changed_at`。
- 数据库比较旧哈希、旧密码版本与active状态，防止并发请求覆盖已修改的密码。密码版本由数据库生成，并严格晚于上次版本，避免时间回拨使旧会话继续有效。
- 当前会话主动注销，`FOODTIME_SESSION` Cookie设置 `Max-Age=0`。其他设备的旧会话在下一次请求时被密码版本校验拒绝，并清理会话；不扫描或批量删除其他设备的Redis键。
- 数据库已成功更新后，若当前会话Redis删除失败，仍返回成功并尝试清除Cookie；旧会话仍无法通过密码版本校验。日志只记录异常类型。
- 客户端收到成功后清空页面内登录状态，重新获取匿名CSRF令牌，再用新密码登录；旧密码、旧Cookie和旧CSRF令牌不能继续使用。

## 失败响应

| HTTP | code | 说明 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 缺少字段、长度或字符不符合规则；data返回字段名与原因，不回显密码 |
| 400 | `BAD_REQUEST` | JSON无法解析或请求体缺失 |
| 400 | `OLD_PASSWORD_INCORRECT` | 旧密码错误，密码和当前会话保持不变 |
| 400 | `PASSWORD_UNCHANGED` | 新旧密码相同，密码和当前会话保持不变 |
| 401 | `UNAUTHENTICATED` | 未登录、会话失效、账号已禁用或密码版本已变化 |
| 403 | `CSRF_INVALID` | CSRF令牌缺失或无效；未登录的写请求也可能先返回此错误 |
| 409 | `PASSWORD_CHANGE_CONFLICT` | 验证过程中账号或密码已变化；重新登录后重试 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 请求必须使用JSON |
| 429 | `PASSWORD_CHANGE_RATE_LIMITED` | 修改尝试达到配额；响应头 `Retry-After` 为至少等待的秒数 |
| 503 | `PASSWORD_CHANGE_UNAVAILABLE` | 数据库或修改密码限流Redis无法完成操作 |
| 503 | `AUTH_UNAVAILABLE` | 请求进入接口前，会话或账号身份校验的基础设施不可用 |
| 500 | `INTERNAL_ERROR` | 未预期服务端错误，不返回内部详情 |

并发修改只有一个请求可以更新对应旧密码版本；其他请求可能在安全过滤阶段返回401/403，或在数据库条件更新时返回409。发生网络中断或503时不能仅凭响应断定数据库状态，客户端应通过重新登录确认，不重复自动提交敏感操作。

## 限流与配置

默认每个用户15分钟最多5次尝试、每个来源IP15分钟最多50次尝试，成功和失败都计数。通过格式、登录及CSRF校验后才计数；错误格式不进入密码哈希或消耗配额。限流绑定用户ID，不随浏览器或会话变化重置，且与登录限流使用独立命名空间。

| 环境变量 | 默认值 |
| --- | --- |
| `PASSWORD_CHANGE_WINDOW` | `PT15M`，正整数秒，最多30天 |
| `PASSWORD_CHANGE_USER_LIMIT` | `5`，正整数 |
| `PASSWORD_CHANGE_IP_LIMIT` | `50`，正整数 |
| `PASSWORD_CHANGE_REDIS_NAMESPACE` | `foodtime:auth:password-change` |

默认从连接的真实来源地址限流，不能通过客户端 `X-Forwarded-For` 绕过。Redis键只含用户ID/IP的摘要；Redis不可用时拒绝修改，不绕过限流。写接口继续使用 [Spring Security CSRF保护](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)。

HTTP入口位于 `api/user/PasswordChangeController`，业务和独立限流位于 `auth/service`，公共原子计数位于 `auth/security/RedisAttemptLimiter`，数据库条件更新位于 `user/mapper/UserMapper`。使用现有依赖和数据库字段，无新增数据库迁移。请求示例见 [password.http](../password.http)。
