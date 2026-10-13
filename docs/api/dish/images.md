# 菜品图册

`GET /api/v1/dishes/{dishId}/images?page=1&size=20`

需要登录 Cookie，不需要 CSRF。`dishId` 为 UUID；`page` 默认1且不得小于1，`size` 默认20且范围为1到100。非法参数返回400，不自动截断。页码和大小是接口协议，不是业务表字段。

## 数据库设计与展示规则

图片来自 [dish_images](../../database-design.md#dish_images菜品图片表)，只展示 `status = approved`，不返回待审核或被拒绝图片。食堂、档口和菜品必须均为 `active`；所有角色统一使用此公开展示规则。没有图片时返回200空分页结果；菜品不存在或任一级已停用时返回404。

按 `created_at DESC, id ASC` 稳定排序，分页结果为 `items`、`total`、`page`、`size`。`total` 仅统计本菜品已通过审核的图片。超出末页仍返回真实总数与空 `items`，不会回到第一页。父级状态、总数与分页项由一条SQL取得，使用同一数据库快照。

每项只返回已有字段 `id`、`dishId`、`imageUrl`、`createdAt`；上传人和审核状态属于内部信息，不在公开图册中返回。登录要求、分页、排序、字段选择和404规则属于接口约定。此接口只查询图片引用，不下载、上传或代理图片，也不生成虚构占位图；空图册由前端显示空状态。

## 响应

统一响应的 `data` 示例：

```json
{
  "items": [
    {
      "id": "97555e78-c941-43f3-b861-aa88434f4ae1",
      "dishId": "cc77444e-4ca7-42b6-89ba-e322e87667dc",
      "imageUrl": "https://example.test/dish.jpg",
      "createdAt": "2026-10-13T04:00:00Z"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

| HTTP | code | 含义 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | UUID或分页参数无法解析 |
| 400 | `VALIDATION_ERROR` | 分页参数超出允许范围 |
| 401 | `UNAUTHENTICATED` | 未登录或会话失效 |
| 404 | `DISH_NOT_FOUND` | 菜品不存在或三级状态不可见 |
| 503 | `DISH_IMAGES_UNAVAILABLE` | 图册查询不可用 |
| 503 | `AUTH_UNAVAILABLE` | 认证基础设施不可用 |

实现为 `api/dish/DishImageController`、`dish/service/DishImageService`、`dish/mapper/DishImageMapper`。沿用现有表、索引及状态取值，无数据库迁移。
