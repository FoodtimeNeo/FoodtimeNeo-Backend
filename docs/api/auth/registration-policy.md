# 注册策略

`GET /api/v1/auth/registration-policy`

公开接口，无需登录、无需 CSRF。用于 Web 与 Android 在注册页渲染当前部署真正支持的注册能力。

## 设计边界

- 只暴露稳定能力：`mode`、`canSendEmailCode`、`invitationRequired`、`revision`。
- 不暴露限流数值、邮箱域名白名单、邀请码列表、账号是否存在或部署凭据。
- 服务端仍是最终裁决者；客户端不得缓存 `mode` 后绕过服务端校验。
- 响应受 `/api/**` 的 `Cache-Control: private, no-store` 约束；客户端应在注册失败或长时间停留后重新读取。

## 响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "mode": "open",
    "canSendEmailCode": true,
    "invitationRequired": false,
    "revision": "open:email"
  },
  "timestamp": "2026-11-12T00:00:00Z",
  "requestId": "policy-example-1"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `mode` | string | `open`、`invitation` 或 `closed` |
| `canSendEmailCode` | boolean | 当前是否允许调用 `POST /api/v1/auth/register/email-code`；`closed` 或邮件验证未启用时为 `false` |
| `invitationRequired` | boolean | 是否必须填写邀请码；仅 `invitation` 模式为 `true` |
| `revision` | string | 策略版本；当前由 `mode` 与邮件能力组成，例如 `invitation:email`。变化时客户端应刷新页面状态 |

## 模式行为

| `mode` | 客户端 | 服务端 |
| --- | --- | --- |
| `open` | 正常注册，邀请码输入可选 | 无邀请码可注册；填写时按现有有效性校验并记录归因 |
| `invitation` | 显示并校验必填邀请码，随注册请求透传 | 必须提供有效、未过期、未用尽的邀请码 |
| `closed` | 停止提交与发码，说明暂未开放 | `/register` 与 `/register/email-code` 均返回 403 `REGISTRATION_CLOSED` |

## 错误与兼容

- `GET` 正常返回 200；服务异常返回 500 `INTERNAL_ERROR`。
- 策略读取失败不等于开放：客户端应显示重试与明确错误，不得默认切到 `open`。
- 注册请求仍可能返回 `REGISTRATION_CLOSED`（403）、`INVITATION_REQUIRED`（400）或 `INVITATION_CODE_INVALID`（400）；客户端应处理这些错误并重新读取策略。
- 旧客户端不读取该接口时，服务端原有校验不变；该接口是新增能力，不改变既有注册请求语义。
