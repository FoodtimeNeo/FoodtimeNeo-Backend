# 社区接口与表结构

未单独写出完整前缀的路径均相对于 `/api/v1`。

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

## 紧凑楼中楼

- `POST /community/posts/{postId}/comments`：`content`（1–500）、`clientRequestId`；一级评论不传关系字段，回复同时传 `rootCommentId` 和 `replyToCommentId`。被回复用户由服务端推导。返回 202，审核通过前仅作者可见。
- `GET /community/posts/{postId}/comments/cursor`：一级评论按时间降序，返回游标分页。含本人的待审/驳回评论及删除后仍有合法回复的根评论占位。
- `GET /community/posts/{postId}/comments/{rootId}/replies`：回复按时间升序，同一楼内可互相回复，始终只缩进一层；支持 `size` 和 `cursor`。
- `DELETE /community/comments/{id}`：作者软删除。已删除根评论只返回空正文、`deleted=true`，合法回复保留；禁止新增回复。

V20 在同一评论表增加根评论、实际回复对象、被回复用户、发布去重编号和回复计数。数据库校验同帖同楼关系，评论通过/删除时同步帖子总评论数及根评论的回复数。所有评论和回复均不可编辑。

## 点赞与收藏

- `PUT/DELETE /community/posts/{id}/like`：设置帖子已赞／取消赞，返回 `liked,likeCount`。
- `PUT/DELETE /community/comments/{id}/like`：设置一级评论或回复已赞／取消赞，返回 `liked,likeCount`。
- `PUT/DELETE /community/posts/{id}/favorite`：设置收藏／取消收藏，返回 `favorited,favoriteCount`。
- `GET /users/me/community-favorites`：按收藏时间降序，过滤已删除及不可见帖子，列表与总数使用同一条件。

V21 将原通用点赞迁移为有外键的 `community_post_likes`、`community_comment_likes`，清除历史孤立目标明细。与 `community_posts`、`community_post_images`、`community_comments`、`community_favorites` 共六张主表。唯一键防止重复互动，数据库触发器按真实增删维护计数，事务锁串行化同目标操作；重复 PUT/DELETE 不反转状态。待审核、驳回或已删除内容不能互动。旧 POST 切换接口保留给旧客户端，新客户端使用显式幂等接口。

## 完整人工审核与管理

- `GET /admin/community/posts`、`GET /admin/community/comments`：按 `status,page,size` 查看队列。父帖子已删除的评论不再进入可处理队列。
- `GET /admin/community/posts/{id}`、`GET /admin/community/comments/{id}`：仅管理员可读取完整帖子、全部配图及评论的根楼层／实际回复对象上下文。普通用户无法读取他人待审内容。
- `POST /admin/community/posts/{id}`、`POST /admin/community/comments/{id}`：`action=approve/reject`，驳回必须说明原因；与 AI 同一终审事务，先完成的决定生效，重复处理返回 409。
- `DELETE /admin/community/posts/{id}`、`DELETE /admin/community/comments/{id}`：`reason` 必填，1–500 字。软下架并保留操作者、原因、内容及既有审核日志。

V22 禁止直接 UPDATE 点赞、收藏行，所有改变通过 INSERT/DELETE 维护计数。AI 即使在写入正文结论后中断，也可根据持久化的 `review_stage` 恢复处理；一旦转人工，AI 不再改判。评论审核、删除和互动统一按帖子→评论的顺序加锁。

V23 为图片引用反查与启用用户头像添加索引。社区图片及头像返回 `Cache-Control: no-store`，每次读取重新检查可见性；公开目录图片保留原有缓存策略。条件请求的 304 响应也携带相同缓存策略。
