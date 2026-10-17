# 菜品与档口投稿

写请求需Cookie与CSRF，上传图片后使用本人收到的 `imageUrl` 引用，不能引用其他人的暂存文件。身份、状态及正式记录ID由服务端控制。

| 路径 | 请求 / 响应 |
| --- | --- |
| `POST /api/v1/stalls/{stallId}/submissions` | `name`必填最多120，`description`可空，`price`可空且非负、最多两位小数，`imageUrl`必填 |
| `POST /api/v1/dining-halls/{hallId}/submissions` | `name`必填最多100，`description`可空，`coverImageUrl`必填；无楼层字段 |
| `GET /api/v1/users/me/stall-submissions?page=1&size=20` | 返回本人的档口投稿，包含提交内容、食堂名、状态、审核时间/原因及正式档口ID；停用父级的历史保留 |

POST返回202，data为 `id/status/createdAt`。投稿只写 `dish_submissions` / `stall_submissions`，初始pending，不直接创建正式菜品、图片或档口。菜品参考价格可空来自数据库设计；审核通过前必须补齐。名称上限来自VARCHAR类型，图片内容边界属于上传协议。客户端额外 `userId/status/approvedDishId/approvedStallId` 不参与写入。

父级不存在或隐藏返回404；输入错误或使用他人图片返回400；401/403沿用认证与CSRF规则，基础设施不可用返回503。两类投稿与点评、图片投稿共用一套提交限流（默认每人每15分钟20次），超限返回429 `SUBMISSION_RATE_LIMITED` 并带 `Retry-After`。分页范围沿用1起、每页1到100；本人记录不向其他账号泄漏。
