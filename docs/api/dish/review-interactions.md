# 菜品点评互动

全部路径使用 `/api/v1` 前缀，需要有效会话；写请求携带CSRF。用户ID来自服务端会话。以下为已实现接口，后续回复切片完成后在本页补充。

## 一级点评点赞（V16）

| 方法及路径 | 行为 |
| --- | --- |
| PUT /reviews/{reviewId}/like | 幂等点赞，重复调用仍为已赞 |
| DELETE /reviews/{reviewId}/like | 幂等取消，仅删除当前用户的点赞；允许清理不可见历史 |

200返回 `likedByMe:boolean / likeCount:long / counterVersion:long`，无需请求正文。目标不存在、已删除或所属菜品/档口/食堂停用时，新增点赞返回404。取消不存在目标返回404；已无本人点赞返回未赞及当前计数。401/403沿用会话和CSRF规则。

`GET /dishes/{id}/reviews` 的每条评论新增 likeCount、replyCount、likedByMe、counterVersion，保留原有字段的扁平结构。计数按本页评论ID批量查询，不逐条COUNT；用户状态通过联合主键关联。个人和管理员历史列表继续使用原有评论字段。

`REVIEW_ACTION_WINDOW / REVIEW_ACTION_USER_LIMIT / REVIEW_ACTION_IP_LIMIT / REVIEW_ACTION_REDIS_NAMESPACE` 默认为 PT1M / 100 / 1000 / foodtime:review:actions；与内容投稿和社区动作独立。超限429 `REVIEW_ACTION_RATE_LIMITED` 和 Retry-After，Redis不可用503。

数据库V16新增 dish_review_stats 和 dish_review_likes；触发器是计数唯一更新入口，重复插入或取消不加减。点赞不修改点评正文时间或菜品评分。客户端禁用进行中的同目标操作，失败保留状态，不自动重试写请求。

## 点评回复（V17）

| 方法及路径 | 行为 |
| --- | --- |
| GET /reviews/{reviewId}/replies?size=20&cursor=… | 按首次时间及ID升序游标分页，只返回未删除且审核通过的回复 |
| POST /reviews/{reviewId}/replies | 202，提交审核；正文为 content（1–500字）、replyToId（可空同楼回复ID）、clientRequestId（UUID） |
| GET /users/me/review-replies?page=1&size=20 | 本人未删除回复及审核状态、原因，含原点评删除提示 |
| DELETE /review-replies/{id} | 本人软删除；重复删除幂等 |
| DELETE /admin/review-replies/{id} | 管理员软删除，正文 reason 必填 |

公开分页返回 items、total、hasMore、nextCursor；total直接读取主点评统计，不执行回复COUNT。游标绑定楼层，不接受跨楼使用。回复对象必须同楼、已通过且未删除；任意深度回复仍归属原一级点评。replyToDeleted为true时客户端显示已删除占位，不引用原正文。replyToUserId/replyToUserDisplayName只作结构化对象展示。

同一用户的clientRequestId全局唯一；相同规范化内容/楼层/对象重试返回原ID，不再次送审；更改载荷返回409 REPLY_REQUEST_CONFLICT。客户端失败保留该操作ID，不能自动生成新ID重发。回复进入现有内容投稿限流、AI文字分诊和人工审核；审核类型为dish-review-reply，只有pending可决策。原点评或目录已不可见时不能批准新回复。待审/驳回只对本人及管理员可见。

只有approved且未删除回复计入reply_count，批准和删除在事务中同步更新；回复不影响菜品评分。删除一级点评隐藏整楼并保留历史，不逐条删回复。公开列表附带ownedByMe；审核原因仅返回给本人。V17的likeCount/likedByMe/counterVersion为0/false/0，下一迁移接入回复点赞。
