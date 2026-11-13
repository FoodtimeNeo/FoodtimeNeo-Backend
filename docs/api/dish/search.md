# 跨食堂菜品搜索与筛选

`GET /api/v1/dishes/search`

需要有效会话 Cookie。只返回启用食堂、启用档口下的启用菜品。

## 参数

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `q` | 否 | 菜品名称关键词；去除首尾空白，最多 50 字符。为空时浏览全部启用菜品 |
| `diningHallIds` | 否 | 食堂 UUID 多选；重复参数或逗号分隔均可。空/缺省表示全部食堂 |
| `minPrice` / `maxPrice` | 否 | 十进制金额；`0` 是有效价格；`minPrice <= maxPrice` |
| `minRating` | 否 | 最低评分，0–5；UI 档位为不限、3.0、3.5、4.0、4.5 |
| `sort` | 否 | `relevance`（默认）、`price_asc`、`price_desc`、`rating_desc`；其他值返回 400 |
| `page` / `size` | 否 | 现有分页协议，`page>=1`、`1<=size<=100` |

## 缺失数据规则

- `dishes.price` 为 `NULL` 表示价格未知；`0` 是有效价格，二者不能混淆。
- 评分来自 `dish_rating_summary`：没有正式点评时 `averageScore` 为 `null`、`reviewCount` 为 `0`。
- 价格或评分未知的菜品**保留**，不因价格区间或最低评分被直接排除；已知值不符合门槛的菜品仍排除。
- 排序先分两层：价格和评分都完整在前，至少一项缺失在后。层内按所选排序；所选排序字段为 `null` 时排在有值项之后；最后以稳定 ID 决胜。
- 综合排序保留名称完全匹配、前缀匹配、包含匹配的优先级，再按 `lower(name)` 与 `id` 排序；空关键词按名称与 ID 稳定浏览。

## 响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "items": [
      {
        "id": "…",
        "name": "鸡腿饭",
        "price": 15.00,
        "stallId": "…",
        "stallName": "三号档口",
        "diningHallId": "…",
        "diningHallName": "学苑食堂",
        "averageScore": 4.32,
        "reviewCount": 18
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  },
  "timestamp": "2026-11-12T00:00:00Z",
  "requestId": "dish-search-example-1"
}
```

- `price` 可为 `null`，客户端显示“价格未知”或“待补充”，不能按 0 处理，也不能自动标成称重。
- `averageScore` 可为 `null`，此时 `reviewCount` 为 `0`；不把未知评分显示为 0 分。

## 错误

| HTTP | `code` | 说明 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 关键词超长、价格负数、最低价高于最高价、评分越界、排序不在白名单、分页越界 |
| 401 | `UNAUTHENTICATED` | 未登录或会话失效 |
| 503 | `DATA_UNAVAILABLE` | 数据库暂时不可用 |

## 迁移与实现

- **V47**：`dishes.price` 改为可空；补充状态/价格读路径索引。V1–V46 不改写。
- HTTP：`api/dish/DishSearchController`；校验与 QuerySpec：`dish/service/DishSearchService`；SQL：`dish/mapper/DishSearchMapper`；DTS：`dish/dto/DishSearchResponse`。
- `sort` 只映射为服务端固定 SQL 片段，不拼接客户端字符串；食堂/价格/评分全部参数化。
