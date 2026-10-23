# 社区接口与表结构

开发分支按 `feature/community-posts`、`feature/community-replies`、`feature/community-interactions` 顺序集成。

## 图文发布

- `POST /api/v1/community/posts`：`title`（1–50）、`content`（1–2000）、`imageUrls`（原创 1–9 张本人上传的 JPEG/PNG）、`clientRequestId`（UUID，同一草稿重试复用）。返回 202 和 `id`。同一编号内容不同返回 409。
- `GET /api/v1/community/feed/cursor?size=20&cursor=...`：已通过且未删除的帖子；返回 `items,total,hasMore,nextCursor`。按发布时间与 UUID 降序，游标不能跨列表使用。原有 `/feed?page=...` 保留兼容。
- `GET /api/v1/community/posts/{id}`：公开帖子或本人待审/驳回帖子。
- `GET /api/v1/users/me/community-posts`：本人的所有未删除帖子。
- `DELETE /api/v1/community/posts/{id}`：作者软删除；正文、图片引用、审核记录保留，公开入口立即隐藏。

先调用原有 `POST /api/v1/uploads/images` 上传图片。服务端保存真实宽高，不接受客户端伪造尺寸。图片顺序即首图顺序；同一帖子不能重复引用一张图片。迁移前旧图片宽高允许未知，界面使用布局回退，不能伪造宽高。

V19 扩充 `community_posts`：标题、发布去重编号、收藏计数、审核阶段/转人工原因、更新时间、删除时间/操作者/原因；扩充 `community_post_images`：宽高及图片顺序唯一约束。帖子、评论内容禁止更新。

`status=pending` 时，`reviewStage=ai_pending` 或 `manual_pending`。AI 无法判断、不可用、关闭时分别以 `uncertain`、`ai_unavailable`、`ai_disabled` 转人工；终审后为 `completed`。所有配图与标题正文都须完成审核才可自动通过。文件清理会保留仍被社区引用的图片；私有图片只允许作者及管理员访问，公开图片随帖子可见性变化。
