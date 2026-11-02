# 审核发布者信息

管理员的内容审核、社区审核、正式评论管理及审核日志返回发布者 `userId`、`userDisplayName`、`userEmail`。名称和邮箱关联用户记录；普通用户不能访问这些接口。公开点评响应继续使用原有 DTO，不包含邮箱。

审核队列的 `createdAt` 为投稿时间。日志保留原有 `createdAt`（审核时间），新增 `submittedAt`（原投稿时间）、`name`、`content`、`imageUrl`，供两端同时显示发布信息与审核信息。日志关联原提交表读取，包含已软删除的社区内容和回复；这些字段反映关联原记录，不是新增的不可变快照。

社区完整审核详情的 `userEmail` 对应本次审核对象：审核评论/回复时为评论作者，审核食记时为食记作者。图片投稿通过 `imageUrl` 展示；无文字或缺失历史信息时前端明确显示缺省文案，不推测作者或时间。

无需数据库迁移。先部署本次 Backend，再更新 Web/Android；旧后端不返回邮箱时，新前端显示“未记录”。

验证：`ModerationIT`、`CatalogAdminIT`、`CommunityAdminIT` 共 16 项真实 PostgreSQL/Redis/HTTP 集成测试通过，包括用户权限、发布者信息、两种时间、软删除日志和公开接口邮箱隔离。
