# v1.4.0 发布说明

后端版本更新为 `1.4.0`。通过 `dev → main` 发布 PR 保留合并历史。

## 更新范围

- **社区互动消息**：点赞按食记/评论聚合（`actorCount`），评论审核通过通知作者，回复通知被回复者；消息带 `refCommentId` / `refRootCommentId` 供客户端定位楼层；原帖删除清理整帖互动消息，评论删除只清理指向该评论的消息。
- **一对一私信**：新增会话列表、摘要、消息分页、发送、已读、隐藏与未读计数；发送使用 `clientRequestId` 幂等并按用户/IP 限流；访客可读可已读但不能发送。
- **私有 API 缓存加固**：`/api/**` 默认 `Cache-Control: private, no-store`，媒体接口按既有策略覆盖。
- 社区审核与每日推荐等已有能力沿用上一版本。

## 数据库迁移

- `V39__community_notifications.sql`：互动消息定位/聚合字段与索引。
- `V40__direct_messages.sql`：`conversations` 与 `direct_messages`。

Flyway 自动执行；不手工修改版本表。部署过含旧 V35 AI 迁移构建的数据库按既有部署检查清单处理历史记录。

## 部署注意

- 新增私信限流配置（可选）：`DM_WINDOW`（默认 `PT1M`）、`DM_USER_LIMIT`（30）、`DM_IP_LIMIT`（300）、`DM_REDIS_NAMESPACE`。
- Android 新版本 `1.4.0`（versionCode 15）；更新 `APP_RELEASE_*` 后客户端可应用内升级。
- 私信为纯文本、仅会话双方可读；管理员接口不暴露私信内容。
