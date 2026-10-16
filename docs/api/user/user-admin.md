# 用户管理

仅 `superadmin` 可以访问。角色和状态沿用 `users` 的现有取值；不增加运营员、档口归属或新的角色模型。

- `GET /api/v1/admin/users?q=&page=1&size=20`：分页及邮箱/昵称不区分大小写的字面子串搜索。返回 `id,email,displayName,avatarUrl,role,status,createdAt,reviewCount`；评论数由正式 `dish_reviews` 计数，不包含待审核提交。搜索长度最多254属于接口约定。
- `PUT /api/v1/admin/users/{id}`：JSON `{ "role":"user|admin|superadmin", "status":"active|disabled" }`。成功200。未知取值400、无权限403、用户不存在404、自身权限修改409。不修改邮箱、昵称或密码，额外JSON字段不参与写入。

用户管理划分给平台管理员、禁止自身角色和状态修改属于现有产品约定，数据库只规定角色/状态枚举。账号禁用会使已有会话在下次认证请求失效；角色调整在下次请求重读，不信任旧会话内的权限。账号恢复后需重新登录。

修改时按UUID固定顺序锁定操作者和目标，重查操作者仍为启用的平台管理员，再提交。成功操作的操作者、目标、角色、状态和时间写入应用操作日志，并含请求ID；没有伪造 `content_moderation_logs` 的审核动作或新增用户审计表。日志留存由部署配置管理。
