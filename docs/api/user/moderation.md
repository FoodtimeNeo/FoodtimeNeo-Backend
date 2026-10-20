# 管理审核

管理员及平台管理员可使用；普通用户即使伪造请求中的角色/审核人也返回403。写请求需当前Cookie及CSRF。

| 路径 | 行为 |
| --- | --- |
| `GET /api/v1/admin/moderation?status=pending&page=1&size=20` | 评论、菜品、档口、图片及点评回复队列；status允许pending/approved/rejected |
| `POST /api/v1/admin/moderation/{type}/{id}` | type为review/dish/stall/photo/dish-review-reply；body为 `{action:"approve或reject",reason:"审核说明",price:可选补齐价格}` |
| `GET /api/v1/admin/moderation/logs?page=1&size=20` | 审核日志，只有管理员可见 |

队列项 `id/type/targetId/userId/userDisplayName/name/content/price/imageUrl/rating/status/reason/createdAt` 来自已有提交表或关联对象。`userDisplayName` 取自投稿人当前昵称，供管理端直接展示，不再显示原始用户ID。`aiRiskLevel/aiConfidence/aiLabels/aiReason/aiCheckedAt` 为AI预检结论（未检出为null）；评论结论够硬时AI直接自动通过/驳回（阈值见配置），拿不准的才留给人工，详见[AI预检](../../ai-moderation.md)。`type`是表来源协议标识，不增加通用提交表。图片沿用dish_images，V4经用户确认补充审核字段与独立图片日志外键；文件上传不等于投稿。

仅pending可审核，行锁保证并发只有一方成功，之后返回409。驳回须说明原因（沿用当前产品交互约定），不创建正式记录。通过时目标及父级须启用；菜品价格缺失须补齐，提交价格与正式价格一致。档口同一食堂名称冲突返回409，事务完全回滚。

评论通过后按用户/菜品唯一创建或更新正式评论，新建时created_at复制提交时间，重新点评保留首次时间；菜品通过后创建正式菜品和approved图片；档口通过后复制封面创建正式档口。审核人/时间/说明、approved ID、正式记录及新日志在同一数据库事务中完成。每条日志仅关联一种现有提交外键，记录approve/reject，不增加设计之外动作。AI自动决定的日志记 `actor='ai'` + 模型名，与人工（`actor='human'` + 管理员ID）分开存放，核验时在审核日志页区分展示。评分在正式点评变化的事务内同步更新。

API返回200，data为 `id/status/approvedId`；图片通过时approvedId是原图片ID，拒绝时为null。400为参数/补价错误，401为会话失效，403为权限不足，404为提交不存在，409为已审核或目录冲突，503为基础设施不可用。

点评回复的targetId是原一级点评ID，name来自菜品；待审且未删除回复可人工/AI决策，已通过及未删除回复计入reply_count。原点评软删除或目录停用后不能批准新回复；删除独立使用DELETE /admin/review-replies/{id}及必填reason，保留审核历史，操作原因记录应用日志。详见[互动契约](../dish/review-interactions.md)。
