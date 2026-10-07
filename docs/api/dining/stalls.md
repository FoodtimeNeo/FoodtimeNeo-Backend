# 获取食堂下档口列表

`GET /api/v1/dining-halls/{diningHallId}/stalls`

需要有效的 `FOODTIME_SESSION` 登录Cookie。此GET接口不需要CSRF令牌；登录方式见 [登录文档](../auth/login.md)。食堂ID可从 [食堂列表](list.md) 获取。

## 请求与展示规则

- 路径参数 `diningHallId` 为食堂UUID；无请求体，返回该食堂的完整启用档口列表。
- 食堂不存在或已停用时，返回404 `DINING_HALL_NOT_FOUND`，不会返回其档口。
- 食堂启用，但没有档口或所有档口均停用时，返回200，`data` 为 `[]`。
- 只返回路径指定食堂下 `status = active` 的档口，按 `sort_order ASC, id ASC` 排列；相同排序值使用ID保持顺序稳定。
- 所有登录角色使用相同展示规则；查询参数不能覆盖所属食堂或状态过滤。第一版不提供分页、搜索或客户端排序。
- 父食堂状态和档口数据由单条SQL在同一数据库快照读取，每次请求重新查询，后续请求体现启用/停用变更。

## 成功响应

返回 **200 OK**，采用统一响应，`data` 为数组：

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": "72e6eeb7-e3a7-4d64-90af-8c7f67411012",
      "diningHallId": "3e96f06d-9f88-4b24-a9f5-0830359b629b",
      "name": "面食档口",
      "coverImageUrl": null,
      "description": "提供面食",
      "floor": "一层"
    }
  ],
  "timestamp": "2026-10-07T15:00:00Z",
  "requestId": "stall-list-example"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID字符串 | 档口唯一标识 |
| `diningHallId` | UUID字符串 | 所属食堂ID，与路径一致 |
| `name` | 字符串 | 档口名称 |
| `coverImageUrl` | 字符串或null | 封面图片地址或存储引用 |
| `description` | 字符串或null | 档口简介 |
| `floor` | 字符串或null | 楼层或区域 |

未设置的可空字段返回null；不公开内部状态、排序值和数据库创建/更新时间。响应包含 `X-Request-Id`，不加载菜品列表或统计菜品数量。

## 失败响应

| HTTP | code | 说明 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | 食堂ID无法解析为UUID |
| 401 | `UNAUTHENTICATED` | 未登录、会话失效、账号已禁用或密码已修改 |
| 404 | `DINING_HALL_NOT_FOUND` | 食堂不存在或已停用 |
| 503 | `STALLS_UNAVAILABLE` | 档口查询暂时不可用，稍后重试 |
| 503 | `AUTH_UNAVAILABLE` | 查询前，数据库或Redis无法完成身份/会话验证 |
| 500 | `INTERNAL_ERROR` | 未预期服务端错误，不返回内部详情 |

实现位于 `api/dining/DiningHallStallController`、`dining/service/StallService`、`dining/mapper/StallMapper`；公开响应为 `dining/dto/StallResponse`。使用现有食堂和档口表，无新增依赖或数据库迁移。联调顺序见 [dining.http](../dining.http)。
