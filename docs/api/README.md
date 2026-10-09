# FoodtimeNeo Backend API

业务接口前缀：`/api/v1`。请求与响应使用 UTF-8 JSON；错误响应保留对应 HTTP 状态码。

| 功能 | 方法与路径 | 接口文档 |
| --- | --- | --- |
| 用户注册 | `POST /api/v1/auth/register` | [注册接口](auth/register.md) |
| 注册邮箱验证码 | `POST /api/v1/auth/register/email-code` | [邮箱验证码](auth/email-verification.md) |
| 邮箱密码登录 | `POST /api/v1/auth/login` | [登录与会话](auth/login.md) |
| CSRF令牌 | `GET /api/v1/auth/csrf` | [登录与会话](auth/login.md) |
| 当前登录用户 | `GET /api/v1/auth/me` | [登录与会话](auth/login.md) |
| 退出登录 | `POST /api/v1/auth/logout` | [登录与会话](auth/login.md) |
| 修改当前用户密码 | `PUT /api/v1/users/me/password` | [修改密码](user/password.md) |
| 获取食堂列表 | `GET /api/v1/dining-halls` | [食堂列表](dining/list.md) |
| 获取食堂下档口列表 | `GET /api/v1/dining-halls/{diningHallId}/stalls` | [档口列表](dining/stalls.md) |
| 获取档口下菜品列表 | `GET /api/v1/stalls/{stallId}/dishes` | [菜品列表](dish/list.md) |
| HTTP 检查 | `GET /api/v1/system/ping` | 返回服务名称与状态；依赖状态见 `/actuator/health/readiness` |

统一响应字段：`code`（业务码）、`message`（提示）、`data`（业务数据）、`timestamp`（ISO 8601 UTC 时间）、`requestId`（请求 ID）。响应头也包含 `X-Request-Id`。

成功响应的 `code` 为 `OK`。失败响应保留对应HTTP状态码；参数校验失败的 `data` 返回字段名和原因，其余失败通常为null，不返回原密码、验证码、SQL或堆栈。Actuator和OpenAPI使用各自的标准响应格式。

客户端可传入由字母、数字、点、下划线、短横线组成且长度不超过64的 `X-Request-Id`；缺失或非法时由服务端生成。日志使用相同ID，便于排查请求。

开发环境实时文档：`/swagger-ui.html`；OpenAPI JSON：`/v3/api-docs`。新增或修改接口时，同时维护对应 Markdown 文档和 Controller / DTO 的 OpenAPI 注解。

## 登录与注册联调

可按 [auth.http](auth.http) 的顺序逐条发送请求。请求客户端须保留服务器设置的Cookie；填写自己的测试邮箱、密码、邮件验证码和CSRF令牌，勿将真实凭据提交到Git。

1. 获取CSRF令牌，再发送注册邮箱验证码。
2. 收到邮件后提交邮箱、密码和验证码完成注册。注册成功不会自动登录。
3. 携带Cookie和CSRF令牌提交邮箱账号与密码登录。
4. 登录会替换旧会话；重新获取CSRF令牌，再查询当前用户或退出登录。

从Backend目录启动服务，开发环境文档地址为 `http://localhost:8080/swagger-ui.html`，OpenAPI地址为 `http://localhost:8080/v3/api-docs`。生产环境默认关闭实时文档；文件接口文档仍保留在本目录。

Java认证HTTP入口位于 `src/main/java/com/FoodtimeNeo/api/auth`；本目录维护接口文档与联调请求。发送邮件前须按 [邮箱验证码配置说明](auth/email-verification.md#邮箱配置) 填写正式SMTP配置并启用。未配置邮件时发送验证码返回503；缺少验证码的注册请求返回400，提交验证码后验证服务未启用返回503。登录和注册依赖可用的PostgreSQL与Redis。
