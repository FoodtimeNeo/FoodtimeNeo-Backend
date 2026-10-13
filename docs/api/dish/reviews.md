# 评论与评分

需有效登录；所有写请求携带当前Cookie和CSRF令牌。用户ID由服务端取得，客户端提交额外角色/用户ID不会改变授权。

| 方法与路径 | 行为 |
| --- | --- |
| `GET /api/v1/dishes/{dishId}/reviews?page=1&size=20` | 启用食堂/档口/菜品下的正式评论；返回统一分页 `items/total/page/size` |
| `POST /api/v1/dishes/{dishId}/reviews` | `{rating:1..5,content:"正文"}`；202返回待审核提交的ID、菜品ID、评分、正文、状态和提交时间 |
| `GET /api/v1/dishes/{dishId}/rating` | 返回 `dishId/averageScore/reviewCount`；平均分允许null，来自24小时物化视图 |
| `GET /api/v1/users/me/reviews?page=1&size=20` | 本人正式评论，包含菜品/档口/食堂的关联ID与名称；停用内容的历史保留 |
| `DELETE /api/v1/users/me/reviews/{reviewId}` | 删除自己的正式评论，200、data为null；他人或不存在的记录返回404 |

评论项返回 `id/dishId/userId/rating/content/createdAt/updatedAt/dishName/stallId/stallName/diningHallId/diningHallName`；不返回头像或密码。按 `created_at DESC,id ASC` 稳定排序。分页1起，size范围1到100，越界页为空但保留总数。

写入仅使用 `dish_review_submissions`，默认pending，审核通过后才进入 `dish_reviews`。同一用户/菜品最多一条pending，由数据库部分唯一索引裁决并发冲突，重复返回409 `REVIEW_PENDING`。评分范围和唯一正式评论规则来自数据库设计，正文必须非空是评论接口约定；没有增设数据库字段。隐藏或不存在菜品返回404；已发布评论删除不删除审核历史，也不立即刷新评分。

审核发布将在管理审核功能分支中实现。失败沿用统一401/403/400响应，数据库异常不返回SQL和凭据。
