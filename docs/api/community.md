# 社区动态（P1 动态 + P2 评论）

登录后可用；写请求需当前Cookie及CSRF。动态与评论走与点评相同的AI即审链
（文本用 `community-post` 类型按评论阈值，配图用 `community-image` 类型按图片链逐张诊断；
任一 HIGH 达线即驳回，文本 LOW 达线且配图全 LOW 达线才通过，其余留给人工），
人工队列与审核日志与点评共用语义（`actor` 区分 AI/人工），详见[AI预检](../../ai-moderation.md)。

| 路径 | 行为 |
| --- | --- |
| `POST /api/v1/community/posts` | 发布动态，body 为 `{content:"1到2000字",imageUrls:["/api/v1/media/{id}"，最多9张本人已上传],repostOfId:可选}`；202 受理，pending 待审 |
| `GET /api/v1/community/feed?page=1&size=20` | 仅已通过，最新发布在前；附点赞/评论/转发数、是否已赞/已藏、转发原帖摘要 |
| `GET /api/v1/community/posts/{id}` | 详情；pending 仅本人可见 |
| `DELETE /api/v1/community/posts/{id}` | 只能删除本人动态；连带配图、评论、赞、藏一起删除，审核记录保留 |
| `GET /api/v1/users/me/community-posts?page=1&size=20` | 我的动态，含 pending/approved/rejected，附 AI 结论与驳回原因 |
| `GET /api/v1/admin/community/posts?status=pending&page=1&size=20` | 社区审核队列，含 AI 分诊结论 |
| `POST /api/v1/admin/community/posts/{id}` | body 为 `{action:"approve或reject",reason:"驳回必填"}`；通过后转发数+1（转发的原帖） |
| `POST /api/v1/community/posts/{postId}/comments` | 发表评论，body 为 `{content:"1到500字"}`；202 受理，仅已通过动态可评论，同样走AI即审 |
| `GET /api/v1/community/posts/{postId}/comments?page=1&size=20` | 只返回已通过评论，最新在前 |
| `DELETE /api/v1/community/comments/{id}` | 只能删除本人的评论 |
| `GET /api/v1/admin/community/comments?status=pending&page=1&size=20` | 评论审核队列，附 AI 分诊结论 |
| `POST /api/v1/admin/community/comments/{id}` | body 为 `{action:"approve或reject",reason:"驳回必填"}`；通过后动态评论数+1 |

同一用户同一目标的点赞见 P3（`community_likes` 按（用户,类型,目标）唯一，`target_type` 预留 post/comment，表已建好）。收藏与转发见 P3（表与 `repostOfId` 机制已在 P1 落地）。
