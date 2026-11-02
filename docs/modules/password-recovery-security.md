# 注册与找回密码安全约定

注册仍要求真实邮箱验证码。`RegistrationService`创建账号前调用`EmailVerificationService.claim`，Redis原子验证邮箱绑定的HMAC证明、有效期、错误次数和占用状态；不存在固定测试码或跳过开关。客户端的verified/emailVerified/role等字段不授予验证或管理权限。缺码、错码、换邮箱、过期和重复使用由接口回归覆盖。

找回密码复用既有SMTP配置与`EMAIL_VERIFICATION_ENABLED`/`EMAIL_VERIFICATION_SECRET`，不受关闭注册开关影响。无需新的明文验证码配置或数据库迁移。新增精确的三个匿名POST路径，全部保留CSRF校验：

| 路径（/api/v1前缀） | 请求 | 响应 |
| --- | --- | --- |
| /auth/password-reset/email-code | email | 202，expiresAt/resendAfterSeconds；对不存在/禁用账号同样受理 |
| /auth/password-reset/verify | email、verificationCode | 短时resetToken、expiresAt；Cache-Control:no-store |
| /auth/password-reset | resetToken、newPassword | 成功后重新登录；不能以email/userId/verified替代凭证 |

- 六位码使用SecureRandom，默认10分钟、60秒重发、每码最多5次错误。注册和重置采用独立Redis命名空间、HMAC用途和邮件模板，互不通用。
- 发信先预占邮箱/设备/IP配额，再进入2个工作线程、100个排队任务的有界队列。账号查询和SMTP在异步任务内完成，未知或禁用账号不发信；HTTP不会宣称邮件已投递。队列拒绝、Redis故障或缺少基本配置返回503。
- 新码成功投递后替换旧码。验证码在签发时绑定账号ID和密码版本，验证时还需匹配数据库当前版本，旧码不能在改密后换取新授权。
- 验证成功由Redis原子消费验证码、创建256位随机Token的摘要记录；有效期默认5分钟，绑定账号和密码版本。Token仅保留在两端流程内存，刷新或退出需重新验证，不进入URL、持久存储、DTO日志。
- 最终接口先原子消费Token，然后按其账号ID读取账号，以旧哈希和旧密码版本进行单条条件UPDATE。单Token并发至多一次使用；其它旧版本授权均无法覆盖新密码。请求体额外身份字段不决定修改对象。
- 为避免Redis与数据库之间不确定提交引发重放，Token在数据库写入前消费且不释放：数据库异常或未知提交结果需要尝试登录确认，或重新获取验证码；不会以无凭证方式重试。客户端不自动重放写请求。
- 成功后更新password_changed_at，所有旧会话在下一次认证检查时失效；不自动登录，异步发送改密通知。
- 同样的服务端密码规则适用于注册、登录态改密与找回密码；两次密码确认用于防止输错，不能证明邮箱所有权。

限流默认值：邮箱发信5次/小时、设备10次/小时、IP200次/小时；验证邮箱10次/15分钟、IP300次/15分钟；最终重置按Token5次/15分钟和IP300次/15分钟。`PASSWORD_RESET_*`配置见application.yml，不能仅关闭前端倒计时绕过。

测试：`mvnw -B -ntp verify`；`mvnw -B -ntp -Pintegration verify`使用独立PostgreSQL、Redis和测试内127.0.0.1 SMTP，不向真实邮箱发信。PasswordResetIT直接调用HTTP覆盖CSRF、伪造标记、Token重放/过期/并发、用途与邮箱绑定、旧验证码/旧凭证/旧会话失效。
