# 获取档口下菜品列表

`GET /api/v1/stalls/{stallId}/dishes`

需要有效的 `FOODTIME_SESSION` 登录Cookie。此GET接口不需要CSRF令牌；登录方式见 [登录文档](../auth/login.md)。档口ID可从 [食堂下档口列表](../dining/stalls.md) 获取。

## 请求与展示规则

- 路径参数 `stallId` 为档口UUID；无请求体，返回该档口的完整启用菜品列表。
- 档口不存在、已停用或所属食堂已停用时，返回404 `STALL_NOT_FOUND`，不会返回其菜品。
- 食堂和档口均启用，但没有菜品或所有菜品均停用时，返回200，`data` 为 `[]`。
- 只返回路径指定档口下 `status = active` 的菜品，按 `created_at ASC, id ASC` 排列；创建时间相同的菜品使用ID保持顺序稳定。
- 所有登录角色使用相同展示规则；查询参数不能覆盖所属档口或状态过滤。第一版不提供分页、搜索或客户端排序。
- 食堂、档口状态和菜品数据由单条SQL在同一数据库快照读取，每次请求重新查询，后续请求体现名称、价格和启用/停用变更。
- 列表只提供名称、价格及定位所需的ID，不加载图片、评分或评论。

## 成功响应

返回 **200 OK**，采用统一响应，`data` 为数组：

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": "cc77444e-4ca7-42b6-89ba-e322e87667dc",
      "stallId": "72e6eeb7-e3a7-4d64-90af-8c7f67411012",
      "name": "红烧肉",
      "price": 12.30
    }
  ],
  "timestamp": "2026-10-09T08:00:00Z",
  "requestId": "dish-list-example"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID字符串 | 菜品唯一标识 |
| `stallId` | UUID字符串 | 所属档口ID，与路径一致 |
| `name` | 字符串 | 菜品名称 |
| `price` | JSON数字 | 非负价格，最多两位小数，允许0；数据库使用 `NUMERIC(10,2)`，后端使用 `BigDecimal` |

响应包含 `X-Request-Id`。前端显示价格时可按两位小数格式化；不要依赖JSON数字的尾随零。

## 失败响应

| HTTP | code | 说明 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | 档口ID无法解析为UUID |
| 401 | `UNAUTHENTICATED` | 未登录、会话失效、账号已禁用或密码已修改 |
| 404 | `STALL_NOT_FOUND` | 档口不存在、已停用或所属食堂已停用 |
| 503 | `DISHES_UNAVAILABLE` | 菜品查询暂时不可用，稍后重试 |
| 503 | `AUTH_UNAVAILABLE` | 查询前，数据库或Redis无法完成身份/会话验证 |
| 500 | `INTERNAL_ERROR` | 未预期服务端错误，不返回内部详情 |

实现位于 `api/dish/StallDishController`、`dish/service/DishService`、`dish/mapper/DishMapper`；公开响应为 `dish/dto/DishListItemResponse`。使用现有食堂、档口和菜品表，无新增依赖或数据库迁移。联调顺序见 [dining.http](../dining.http)。
