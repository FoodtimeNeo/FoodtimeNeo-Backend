# 社区管理接口与权限

已有的管理员社区删除接口与两端帖子详情对接；删除为软删除，记录删除时间、操作者和原因。

| 方法与路径 | 权限 | 请求体 |
| --- | --- | --- |
| DELETE `/api/v1/admin/community/posts/{id}` | active admin / superadmin | `{ "reason": "删除原因" }` |
| DELETE `/api/v1/admin/community/comments/{id}` | active admin / superadmin | `{ "reason": "删除原因" }` |
| DELETE `/api/v1/community/posts/{id}` | 内容作者 | 无 |
| DELETE `/api/v1/community/comments/{id}` | 内容作者 | 无 |

管理员删除原因必填，最多 500 字。缺少权限返回 403，非法原因返回 400，内容不存在或已删除返回 404。删除帖子后公开详情不可读；删除一级评论时保留已通过的楼内回复，回复数与帖子评论数由数据库触发器维护。评论删除与所属帖子加锁，避免与并发互动破坏计数。

`CommunityRemovalTest` 验证服务层权限、原因及锁顺序；`CommunityAdminIT` 通过真实 HTTP、Cookie、CSRF 验证两种管理员的帖子和楼中楼删除、普通用户拒绝访问、重复删除、删除审计与计数。

相关既有功能：公告创建、编辑和设置由 `/api/v1/admin/notices/**` 提供，仅限 `superadmin`；个人昵称和头像通过 `PUT /api/v1/users/me/profile` 修改，仅操作当前会话用户，昵称最多 50 字，头像须为本人上传的同站图片引用。公告和资料的集成回归为 `LoginNoticeIT`、`NoticeInboxIT`、`ProfileIT`。
