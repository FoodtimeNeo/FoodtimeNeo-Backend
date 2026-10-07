# 获取食堂列表

`GET /api/v1/dining-halls`

需要有效的 `FOODTIME_SESSION` 登录Cookie。此GET接口不需要CSRF令牌；登录时仍按 [登录文档](../auth/login.md) 获取CSRF并保留Cookie。

第一版返回全部启用食堂，适用于校园食堂选择和列表展示。没有请求体，不提供分页、搜索或客户端排序参数；额外查询参数不会改变服务端展示规则。

## 成功响应

返回 **200 OK**，采用统一响应，`data` 为数组：

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": "3e96f06d-9f88-4b24-a9f5-0830359b629b",
      "name": "第一食堂",
      "coverImageUrl": null,
      "description": "食堂简介",
      "latitude": 39.9520000,
      "longitude": 116.3500000
    }
  ],
  "timestamp": "2026-10-07T15:00:00Z",
  "requestId": "hall-list-example"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID字符串 | 食堂唯一标识 |
| `name` | 字符串 | 食堂名称 |
| `coverImageUrl` | 字符串或null | 图片地址或存储引用，按现有数据返回 |
| `description` | 字符串或null | 食堂简介 |
| `latitude` / `longitude` | JSON数值或null | 食堂经纬度；未配置时均为null，不以0代替缺失位置 |

- 查询固定过滤 `status = active`，所有角色均适用；管理员也不能通过此展示接口读取停用食堂。
- 按 `sort_order ASC, id ASC` 排列；相同排序值使用ID保持顺序稳定。
- 没有启用食堂时，`data` 为 `[]`，仍返回200。
- 不公开内部状态、排序值及数据库创建/更新时间。
- 每次从数据库读取，停用/启用变更在后续请求中体现。当前不缓存食堂列表，也不加载档口或计算距离。
- 响应包含 `X-Request-Id`；认证和响应格式沿用项目统一配置。

## 失败响应

| HTTP | code | 说明 |
| --- | --- | --- |
| 401 | `UNAUTHENTICATED` | 未登录、会话失效、账号已禁用或密码已修改 |
| 503 | `DINING_HALLS_UNAVAILABLE` | 食堂数据库查询暂时不可用，稍后重试 |
| 503 | `AUTH_UNAVAILABLE` | 接口调用前，数据库或Redis无法完成身份/会话验证 |
| 500 | `INTERNAL_ERROR` | 未预期服务端错误，不返回内部详情 |

实现位于 `api/dining/DiningHallController`、`dining/service`、`dining/mapper`，公开响应为 `dining/dto/DiningHallResponse`；使用现有 `foodtime.dining_halls` 表，无新增依赖或数据库迁移。联调顺序见 [dining.http](../dining.http)。
