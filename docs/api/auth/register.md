# 用户注册

`POST /api/v1/auth/register`

无需登录，`Content-Type: application/json`，响应状态码成功时为 **201 Created**。

先通过 `POST /api/v1/auth/register/email-code` 发送验证码，再提交邮箱、密码和验证码；校验通过才创建账号。发送接口及SMTP配置详见 [邮箱验证码](email-verification.md)。

## 请求

```json
{
  "email": "20260001@bjtu.edu.cn",
  "password": "Foodtime2026!",
  "verificationCode": "012345"
}
```

| 字段 | 必填 | 规则 |
| --- | --- | --- |
| `email` | 是 | 去除首尾空白、转为小写后，必须完全匹配 `[0-9]+@bjtu.edu.cn`；至少一位数字前缀，最多254位 |
| `password` | 是 | 8至128位，仅允许英文字母（A–Z / a–z）、数字（0–9）和ASCII半角符号；至少包含一个数字和一个英文字母；符号非必需；保持原样，不修改大小写 |
| `verificationCode` | 是 | 与该邮箱绑定的6位ASCII数字验证码；作为字符串提交以保留前导零；发送成功后15分钟内有效，最多5次错误尝试 |

允许的符号如下（ASCII可打印字符范围 `!` 至 `~` 中的非字母、非数字字符）：

~~~text
!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
~~~

不允许空格、制表符、换行、中文、全角字符、emoji及其他非ASCII字符；非法Unicode转义也返回400参数校验失败，不进入密码哈希或数据库写入。

示例合法邮箱：`20260001@bjtu.edu.cn`、`00123@bjtu.edu.cn`。`abc@bjtu.edu.cn`、`123@example.com`、`123@bjtu.edu.cn.example.com` 均不合法。

请求仅支持设置邮箱、密码和验证码。即使额外提交 `role`、`displayName`、`status`、`email_verified_at` 等属性，也不会写入这些客户端值。验证成功才创建账号，`email_verified_at` 由服务端填写；注册不会自动登录或签发Token。此前已经存在的未验证账号不会被自动标记为已验证。

## 成功响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": "9e55b67f-f051-4019-bafd-a4082c95f2a6",
    "email": "20260001@bjtu.edu.cn",
    "displayName": "干饭人012345",
    "role": "user"
  },
  "timestamp": "2026-10-03T15:00:00Z",
  "requestId": "register-example-1"
}
```

用户ID为服务端生成的 UUID；昵称为 `干饭人` 加随机6位数字（000000–999999，保留前导零，允许不同用户昵称相同）；角色固定为 `user`，账号状态为 `active`。

密码使用 Spring Security `PasswordEncoder` 的 Argon2id 加盐哈希：19 MiB 内存、2次迭代、并行度1、16字节随机盐、32字节哈希，存储格式为 `{argon2id}$argon2id$...`。同一密码的哈希不同，后续登录应使用 `PasswordEncoder.matches` 校验。参数依据 [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)，算法标识遵循 [Spring Security 密码存储格式](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)。接口响应不会返回密码、密码哈希或盐。

## 失败响应

| HTTP状态码 | `code` | `message` / 说明 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `请求参数校验失败`；`data` 返回字段名和具体失败原因 |
| 400 | `BAD_REQUEST` | `请求参数或请求格式错误`；例如请求体缺失或JSON无法解析 |
| 400 | `EMAIL_CODE_INVALID` | 验证码错误、过期、已用或达到5次错误上限，不创建账号 |
| 409 | `EMAIL_ALREADY_REGISTERED` | `该邮箱已注册`；包括并发提交同一邮箱 |
| 409 | `REGISTRATION_IN_PROGRESS` | 同一邮箱的有效验证码正在被另一注册请求使用，稍后重试 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `不支持的内容类型`；必须提交JSON |
| 503 | `REGISTRATION_UNAVAILABLE` | `注册服务暂时不可用，请稍后重试`；数据库读写失败 |
| 503 | `EMAIL_VERIFICATION_UNAVAILABLE` | 验证服务未配置/未启用，或Redis无法完成校验；不会退回无验证码注册 |
| 500 | `INTERNAL_ERROR` | `服务器内部错误`；其他未预期故障 |

参数校验失败示例（错误列表顺序不固定）：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数校验失败",
  "data": [
    { "field": "email", "message": "邮箱须为数字加@bjtu.edu.cn" },
    { "field": "password", "message": "密码长度须为8至128位" }
  ],
  "timestamp": "2026-10-03T15:00:00Z",
  "requestId": "register-example-2"
}
```

其他失败响应的 `data` 为 NULL。错误信息不会包含原密码、哈希、SQL或堆栈。校验失败和邮箱重复不会新增用户；重复请求不会覆盖已存在账号的密码、昵称或角色。

## 调用示例（PowerShell）

```powershell
$body = @{ email = '20260001@bjtu.edu.cn'; password = 'Foodtime2026!'; verificationCode = '012345' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/auth/register' `
  -ContentType 'application/json; charset=utf-8' -Body $body
```

## 实现位置

`api/auth` 接收请求并触发参数校验；`auth/service` 编排注册流程和失败业务码；`auth/dto` 定义请求与响应；`user/mapper` 负责参数化SQL；`user/service` 生成默认昵称；`config/PasswordConfig` 管理密码哈希策略；`common/mybatis/UuidTypeHandler` 统一处理 PostgreSQL 原生 UUID。

`auth/verification` 管理发送、HMAC摘要和Redis校验占用，Lua脚本位于 `src/main/resources/redis/email`。验证码校验占用不延长15分钟TTL：数据库写入失败会释放占用，成功后删除验证码；进程中断后占用最多60秒自动到期。数据库提交后即使Redis清理暂时失败，唯一索引仍阻止同邮箱再次创建账号，不把已成功创建的账号误报为注册失败。

数据库唯一索引 `ux_users_normalized_email` 配合 `ON CONFLICT ... DO NOTHING` 保证同一规范化邮箱最多注册一次。注册是一次原子INSERT，不修改既有数据库迁移文件。
