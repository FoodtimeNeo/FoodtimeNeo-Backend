# 菜品基础详情

`GET /api/v1/dishes/{dishId}`

需要登录 Cookie，不需要 CSRF；`dishId` 为 UUID，可从档口下菜品列表取得。

## 设计依据

[数据库设计](../../database-design.md#dishes菜品表) 规定菜品名称、简介、价格及所属档口。所属食堂通过 `dishes.stall_id → stalls.dining_hall_id` 关联取得，不在菜品表新增食堂列。价格使用 `NUMERIC(10,2)` / `BigDecimal`，允许0；简介允许为null。

接口返回 `id`、`stallId`、`stallName`、`diningHallId`、`diningHallName`、`name`、`description`、`price`。档口名和食堂名来自关联表。图片、评分、评论属于独立的数据结构，本基础详情接口不查询它们。

必须登录、公开字段选择及不可见资源返回404属于沿用现有接口的约定，不是数据库新增字段或约束。所有登录角色使用相同展示规则；单条SQL同时检查食堂、档口、菜品均为 `active`，查询参数无法覆盖状态过滤。每次请求重新查询。

## 成功响应

统一响应的 `data` 示例：

```json
{
  "id": "cc77444e-4ca7-42b6-89ba-e322e87667dc",
  "stallId": "72e6eeb7-e3a7-4d64-90af-8c7f67411012",
  "stallName": "面食档口",
  "diningHallId": "3e96f06d-9f88-4b24-a9f5-0830359b629b",
  "diningHallName": "第一食堂",
  "name": "红烧肉",
  "description": null,
  "price": 12.30
}
```

## 失败响应

| HTTP | code | 含义 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | 菜品ID无法解析为UUID |
| 401 | `UNAUTHENTICATED` | 未登录或会话失效 |
| 404 | `DISH_NOT_FOUND` | 菜品不存在，或食堂、档口、菜品已停用 |
| 503 | `DISH_DETAIL_UNAVAILABLE` | 查询不可用 |
| 503 | `AUTH_UNAVAILABLE` | 身份或会话校验不可用 |

实现为 `api/dish/DishDetailController`、`dish/service/DishDetailService`、`dish/mapper/DishDetailMapper`。没有数据库迁移、新增业务字段或写入行为。
