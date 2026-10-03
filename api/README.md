# FoodtimeNeo Backend API

业务接口前缀：`/api/v1`。请求与响应使用 UTF-8 JSON；错误响应保留对应 HTTP 状态码。

| 功能 | 方法与路径 | 接口文档 |
| --- | --- | --- |
| 用户注册 | `POST /api/v1/auth/register` | [注册接口](auth/register.md) |
| HTTP 检查 | `GET /api/v1/system/ping` | 返回服务名称与状态；依赖状态见 `/actuator/health/readiness` |

统一响应字段：`code`（业务码）、`message`（提示）、`data`（业务数据）、`timestamp`（ISO 8601 UTC 时间）、`requestId`（请求 ID）。响应头也包含 `X-Request-Id`。

开发环境实时文档：`/swagger-ui.html`；OpenAPI JSON：`/v3/api-docs`。新增或修改接口时，同时维护对应 Markdown 文档和 Controller / DTO 的 OpenAPI 注解。
