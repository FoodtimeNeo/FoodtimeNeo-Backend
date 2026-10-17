# 个人资料

`GET /api/v1/users/me/profile`：需登录，返回 `id/email/displayName/role/avatarUrl`，头像允许null。字段全部来自 `users`；身份由已验证会话取得，查询参数不能指定其他用户。没有画像统计、虚构头像或密码字段。

401表示会话或账号无效；503表示基础设施不可用。角色由服务器读取，Android不能依靠本地预设角色授权。昵称与头像修改见下节PUT接口；修改不添加新的业务规则或数据库字段。

## 修改个人资料

`PUT /api/v1/users/me/profile`：需登录Cookie和CSRF令牌，JSON请求，整体替换两项可编辑字段，返回更新后的个人资料（与GET同一结构，不含敏感认证字段）。

```json
{ "displayName": "干饭达人", "avatarUrl": "/api/v1/media/3fa85f64-5717-4562-b3fc-2c963f66afa6" }
```

- `displayName` 必填，去除首尾空白后须保留至少1个字符且不超过50个字符（对应 `users.display_name VARCHAR(50) NOT NULL`）；默认昵称格式（`干饭人+6位数字`）仅用于注册时系统生成，修改时不强制该格式。
- `avatarUrl` 可空：传 `null` 清除头像；非空时必须是本人上传图片返回的同站引用（`/api/v1/media/{id}`），与图片/菜品/档口投稿的“本人引用”规则一致，他人引用或站外地址返回400。`updated_at` 由数据库触发器维护。
- 身份只取校验后的会话，不接受客户端指定的用户ID；账号被停用后调用返回401。本次不新增数据库字段或迁移。
