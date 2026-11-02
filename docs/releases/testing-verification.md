# 注册验证测试

固定测试验证码通道已移除。开发、联调及生产环境均必须先发送邮件，再使用该邮箱收到的有效6位随机验证码注册。旧的测试开关不再生效。

未启用邮件验证或服务不可用时，注册返回503；未发送验证码、错误或过期的验证码返回400。`000000` 只有恰好由随机生成器产生、发送给该邮箱且仍有效时才可能通过，与其它数字串遵循相同规则。

本地联调优先使用隔离数据库中的专用账号登录；需要测试完整注册时，按[邮箱配置](api/auth/email-verification.md#邮箱配置)配置邮件服务。

单元回归覆盖固定码不能绕过验证、验证服务停用时拒绝注册、有效验证码租约消费及密码哈希。`RegistrationIT` 和 `InvitationCodeIT` 使用仅监听回环地址的捕获式 SMTP 服务与真实 Redis，测试邮件不会发到外部邮箱。

```powershell
./mvnw.cmd verify
# 使用隔离 PostgreSQL / Redis 环境：
./mvnw.cmd -Pintegration '-Dit.test=RegistrationIT,InvitationCodeIT' verify
```
