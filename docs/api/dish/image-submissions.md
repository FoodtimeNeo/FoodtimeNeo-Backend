# 图片投稿与审核

文件上传、投稿、审核是三步：先 `POST /uploads/images` 得到本人私有图片引用，再提交到菜品，管理员通过后才进入公共图册。业务前缀均为 `/api/v1`，需要登录Cookie，写操作带CSRF。

- `POST /dishes/{dishId}/image-submissions`：JSON `{ "imageUrl":"/api/v1/media/{id}" }`，返回201及本次投稿。本人图片、启用菜品和启用父级才可提交；上传者、状态、审核人等由服务端确定。
- `GET /users/me/image-submissions?page=1&size=20`：只返回自己的投稿，也保留停用菜品的历史。字段为 `id,dishId,dishName,stallId,stallName,diningHallId,diningHallName,imageUrl,status,reviewedAt,reviewReason,createdAt`，所属名称通过真实关系查询。
- `POST /admin/moderation/photo/{id}`：管理员用统一审核请求 `{ "action":"approve|reject", "reason":"说明" }`。驳回须填写原因；仅pending可审核，通过前重新检查父级启用状态。成功200，重复审核409。

2026-10-14 经项目负责人确认新增 V4 迁移，补充 `dish_images` 审核人/时间/原因与 `content_moderation_logs.dish_image_id`。记录直接更新图片表，不制造另一份图片或通用投稿表。状态、审核信息、日志在同一事务写入，四类日志外键只能填一个。旧图片缺少审核信息时保留null；不伪造过去的审核人、时间或原因。

待审核及驳回图片只有作者/管理员可以读取，其他用户不能通过图片ID访问文件；通过后仍受菜品及食堂、档口的启停状态限制。公开图册不暴露投稿者、审核人和审核原因。
