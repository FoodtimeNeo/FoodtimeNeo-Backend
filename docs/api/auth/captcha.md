# 登录算术图片验证码

`GET /api/v1/auth/captcha`

公开接口，无需登录；需要浏览器设备 Cookie（不存在时由服务端签发 `foodtime_device`）。响应禁止缓存。

## 触发规则

- 同一设备最近滚动 15 分钟内的登录请求计数（包含失败、成功和缺少验证码的请求）。
- 第二次及之后的登录请求要求验证码；正常会话恢复、页面刷新、读取未读不计数。
- 计数、题目有效期和设备绑定均由服务端判定，客户端不能通过自报版本或隐藏页面绕过。

## 响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "challengeId": "0a5f4d7e-9d4a-4f4a-8c5e-2d3b4a5c6d7e",
    "imageBase64": "iVBORw0KGgo...",
    "expiresAt": "2026-11-12T00:02:00Z"
  },
  "timestamp": "2026-11-12T00:00:00Z",
  "requestId": "captcha-example-1"
}
```

| 字段 | 说明 |
| --- | --- |
| `challengeId` | 一次性挑战标识；登录时原样带回 |
| `imageBase64` | PNG 图片的 Base64；不包含答案、表达式或可逆提示 |
| `expiresAt` | 服务端计算的到期时刻；固定 2 分钟有效 |

题目为两个一位整数的加、乘、整除（不含减法）；除法允许两位数被除数，除数非 0 且必须整除。

## 登录时提交

在 `POST /api/v1/auth/login` 的 JSON 中增加：

```json
{
  "account": "20260001@bjtu.edu.cn",
  "password": "Foodtime2026!",
  "challengeId": "0a5f4d7e-9d4a-4f4a-8c5e-2d3b4a5c6d7e",
  "challengeAnswer": "7"
}
```

- 服务端在密码校验之前验证并**一次性消费**挑战；错误、过期、已消费或设备不匹配都返回 `CAPTCHA_INVALID`，不泄露密码正确性。
- 同一挑战不能重放；失败后需重新取题。
- 换题通过再次调用 `GET /auth/captcha`，旧挑战不保证继续有效。

## 错误

| HTTP | `code` | 说明 |
| --- | --- | --- |
| 404 | `CAPTCHA_DISABLED` | 当前部署未启用登录验证码 |
| 429 | `CAPTCHA_RATE_LIMITED` | 取题过于频繁；响应含 `Retry-After` |
| 503 | `CAPTCHA_UNAVAILABLE` | Redis 或图片生成暂时不可用；fail-closed |
| 400 | `CAPTCHA_REQUIRED` | 设备已达到触发条件但登录请求未带挑战 |
| 400 | `CAPTCHA_INVALID` | 挑战无效、过期、已消费或设备不匹配 |

## 配置

| 环境变量 | 默认 | 说明 |
| --- | --- | --- |
| `LOGIN_CAPTCHA_ENABLED` | `false` | 发布新版时由部署方显式启用；不按客户端版本豁免 |
| `LOGIN_CAPTCHA_WINDOW` | `PT15M` | 设备登录请求滚动窗口 |
| `LOGIN_CAPTCHA_TRIGGER_AFTER` | `1` | 窗口内第几次请求起要求挑战 |
| `LOGIN_CAPTCHA_TTL` | `PT2M` | 挑战有效期 |
| `LOGIN_CAPTCHA_ISSUE_WINDOW` | `PT15M` | 取题限流窗口 |
| `LOGIN_CAPTCHA_ISSUE_DEVICE_LIMIT` | `30` | 每设备每窗口取题上限（Q18 待确认） |
| `LOGIN_CAPTCHA_ISSUE_IP_LIMIT` | `300` | 每 IP 每窗口取题上限（Q18 待确认） |
| `LOGIN_CAPTCHA_REDIS_NAMESPACE` | `foodtime:auth:captcha` | Redis 命名空间 |
| `LOGIN_CAPTCHA_SECRET` | 设备/邮箱验证密钥 | 挑战答案 HMAC 密钥；至少 32 字符 |

当前未实现语音读题：语音生成依赖与降级策略属于 `DECISIONS.md` 的 Q18，待确认后接入；图片生成器、题目和存储均为可替换适配层。答案只以 HMAC 摘要存入 Redis，不写入日志、异常、响应或图片元数据。
