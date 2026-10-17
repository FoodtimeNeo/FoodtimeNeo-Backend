# 管理审核

管理员及平台管理员可使用；普通用户即使伪造请求中的角色/审核人也返回403。写请求需当前Cookie及CSRF。

| 路径 | 行为 |
| --- | --- |
| `GET /api/v1/admin/moderation?status=pending&page=1&size=20` | 评论、菜品、档口、图片提交队列；status允许pending/approved/rejected |
| `POST /api/v1/admin/moderation/{type}/{id}` | type为review/dish/stall/photo；body为 `{action:"approve或reject",reason:"审核说明",price:可选补齐价格}` |
| `GET /api/v1/admin/moderation/logs?page=1&size=20` | 审核日志，只有管理员可见 |

队列项 `id/type/targetId/userId/userDisplayName/name/content/price/imageUrl/rating/status/reason/createdAt` 来自已有提交表或关联对象。`userDisplayName` 取自投稿人当前昵称，供管理端直接展示，不再显示原始用户ID。`type`是表来源协议标识，不增加通用提交表。图片沿用dish_images，V4经用户确认补充审核字段与独立图片日志外键；文件上传不等于投稿。

仅pending可审核，行锁保证并发只有一方成功，之后返回409。驳回须说明原因（沿用当前产品交互约定），不创建正式记录。通过时目标及父级须启用；菜品价格缺失须补齐，提交价格与正式价格一致。档口同一食堂名称冲突返回409，事务完全回滚。

评论通过后按用户/菜品唯一创建或更新正式评论，created_at复制提交时间；菜品通过后创建正式菜品和approved图片；档口通过后复制封面创建正式档口。审核人/时间/说明、approved ID、正式记录及新日志在同一数据库事务中完成。每条日志仅关联一种现有提交外键，记录approve/reject，不增加设计之外动作。评分快照继续等待24小时刷新。

API返回200，data为 `id/status/approvedId`；图片通过时approvedId是原图片ID，拒绝时为null。400为参数/补价错误，401为会话失效，403为权限不足，404为提交不存在，409为已审核或目录冲突，503为基础设施不可用。
