# Android 提醒迁移与投递边界

站内消息与 Android 提醒分开。客户端根据稳定事件编号和序号消费事件，不能以未读总量是否增加推断新消息。

`POST /api/v1/users/me/notification-preferences/legacy-message` 接收 `{ "enabled": true/false }`，仅迁移当前登录账号的旧私信开关。服务器在同一事务锁定偏好：只有仍为初始 revision 1 时写入旧选择并建立当前事件基线；已有明确设置时原样返回。重复调用、并发调用都不会覆盖较新的选择。客户端不需要自行猜测默认版本或执行“先关闭再开启”的两步操作。

七类设置仍用 `GET /users/me/notification-preferences` 和带 `expectedRevision` 的 `PUT /users/me/notification-preferences/{category}`。初次明确设置也建立基线，防止后端初始私信默认开启导致补发旧 pending。后续值不变的保存保留原基线；开关变化则按当前事件序号重新建立边界。

`GET /users/me/notification-events` 保留事件和游标，不改站内已读或历史。尚未投递、但当前已关闭或在新启用基线之前的投递，向客户端投影为 skipped；已 delivered 的真实历史保留。公告在读取时已到截止时间或已停用时，即使以前创建过 pending，也不再提醒，普通公告列表留档规则不变。公告模块返回本次有效编号集合，通知模块不直接依赖公告表结构。

接口沿用普通会话与 CSRF 校验，不加入受限申诉白名单，不接受由客户端指定账号；没有改写 Flyway 迁移或新增数据库版本。新版客户端的迁移接口依赖本次后端更新，发布时后端先升级。

`LegacyNotificationMigrationIT` 覆盖默认开启的历史边界、旧开关关闭、较新设置优先、并发迁移、重开后旧 pending、真实 delivered 历史和账号隔离；`NoticeNotificationIT` 覆盖 pending 公告到期后跳过而列表仍留档。
