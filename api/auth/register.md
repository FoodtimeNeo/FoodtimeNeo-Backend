# 用户注册

`POST /api/v1/auth/register`

无需登录，`Content-Type: application/json`，响应状态码成功时为 **201 Created**。

## 请求

```json
{
  "email": "20260001@bjtu.edu.cn",
  "password": "Foodtime2026!"
}
```

| 字段 | 必填 | 规则 |
| --- | --- | --- |
| `email` | 是 | 去除首尾空白、转为小写后，必须完全匹配 `[0-9]+@bjtu.edu.cn`；至少一位数字前缀，最多254位 |
| `password` | 是 | 8至128位，仅允许英文字母（A–Z / a–z）、数字（0–9）和ASCII半角符号；至少包含一个数字和一个英文字母；符号非必需；保持原样，不修改大小写 |

允许的符号如下（ASCII可打印字符范围 `!` 至 `~` 中的非字母、非数字字符）：

~~~text
!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
~~~

不允许空格、制表符、换行、中文、全角字符、emoji及其他非ASCII字符；非法Unicode转义也返回400参数校验失败，不进入密码哈希或数据库写入。

示例合法邮箱：`20260001@bjtu.edu.cn`、`00123@bjtu.edu.cn`。`abc@bjtu.edu.cn`、`123@example.com`、`123@bjtu.edu.cn.example.com` 均不合法。

请求仅支持设置邮箱与密码。即使额外提交 `role`、`displayName`、`status` 等属性，也不会写入这些客户端值。注册不会自动登录或签发 Token，也不执行邮箱所有权验证；`email_verified_at` 保持 NULL。

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
| 409 | `EMAIL_ALREADY_REGISTERED` | `该邮箱已注册`；包括并发提交同一邮箱 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `不支持的内容类型`；必须提交JSON |
| 503 | `REGISTRATION_UNAVAILABLE` | `注册服务暂时不可用，请稍后重试`；数据库读写失败 |
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
$body = @{ email = '20260001@bjtu.edu.cn'; password = 'Foodtime2026!' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/auth/register' `
  -ContentType 'application/json; charset=utf-8' -Body $body
```

## 实现位置

`auth/controller` 接收请求并触发参数校验；`auth/service` 编排注册流程和失败业务码；`auth/dto` 定义请求与响应；`user/mapper` 负责参数化SQL；`user/service` 生成默认昵称；`config/PasswordConfig` 管理密码哈希策略；`common/mybatis/UuidTypeHandler` 统一处理 PostgreSQL 原生 UUID。

数据库唯一索引 `ux_users_normalized_email` 配合 `ON CONFLICT ... DO NOTHING` 保证同一规范化邮箱最多注册一次。注册是一次原子INSERT，不修改既有数据库迁移文件。
