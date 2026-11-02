# 社区管理接口与权限

已有的管理员社区删除接口与两端帖子详情对接；删除为软删除，记录删除时间和操作者，无需填写原因。

| 方法与路径 | 权限 | 请求体 |
| --- | --- | --- |
| DELETE `/api/v1/admin/community/posts/{id}` | active admin / superadmin | 可省略；兼容旧客户端可选reason备注 |
| DELETE `/api/v1/admin/community/comments/{id}` | active admin / superadmin | 可省略；兼容旧客户端可选reason备注 |
| DELETE `/api/v1/community/posts/{id}` | 内容作者 | 无 |
| DELETE `/api/v1/community/comments/{id}` | 内容作者 | 无 |

管理员直接确认即可删除，空请求体、空对象或空白备注均可；仅旧客户端附带超过500字的备注时返回400。缺少权限返回403，内容不存在或已删除返回明确CONTENT_NOT_FOUND。删除帖子后公开详情不可读；删除一级评论时保留已通过的楼内回复，回复数与帖子评论数由数据库触发器维护。评论删除与所属帖子加锁，避免与并发互动破坏计数。

Web复用游标楼中楼组件并嵌入原详情分栏，独立显示加载失败和重试，保留已展开范围；计数重新读取服务端，不按pending/approved统一减一。确认对话框显示目标摘要，接口失败保留；超时先读取确认结果，不自动重放DELETE。Android沿用原生详情入口和AlertDialog，不显示原因输入框，提交中禁止取消与重复提交，失败可重试或刷新确认结果。审核驳回说明属于独立操作，仍遵守原有要求。

`CommunityRemovalTest` 验证服务层权限、原因及锁顺序；`CommunityAdminIT` 通过真实 HTTP、Cookie、CSRF 验证两种管理员的帖子和楼中楼删除、普通用户拒绝访问、重复删除、删除审计与计数。

相关既有功能：公告创建、编辑和设置由 `/api/v1/admin/notices/**` 提供，仅限 `superadmin`；个人昵称和头像通过 `PUT /api/v1/users/me/profile` 修改，仅操作当前会话用户，昵称最多 50 字，头像须为本人上传的同站图片引用。公告和资料的集成回归为 `LoginNoticeIT`、`NoticeInboxIT`、`ProfileIT`。
