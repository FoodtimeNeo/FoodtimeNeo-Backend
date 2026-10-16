# Android 本机联调

本页描述专用本机联调，Android当前默认连接指定服务器；运行本地验收时需要显式构建为 `http://127.0.0.1:8080/api/v1/`。设备连接主机后执行 `adb reverse tcp:8080 tcp:8080`；设备上的127.0.0.1会转发到主机Backend。会话Cookie与CSRF沿用正式认证流程，不添加客户端角色或免认证接口。

普通启动不会创建这些可登录联调账号，但仍会通过Flyway执行未应用的V5/V6菜单及封面种子迁移。需要专用登录样例时，显式启用 `dev,local-integration` profiles、设置 `app.local-integration.fixtures-enabled=true` 和 `app.local-integration.password`，数据库须在本机。请使用单独联调数据库；不要向业务数据库启用样例。

```powershell
$env:SPRING_PROFILES_ACTIVE='dev,local-integration'
$env:SERVER_ADDRESS='127.0.0.1'
$env:SERVER_PORT='8080'
$env:APP_LOCAL_INTEGRATION_FIXTURES_ENABLED='true'
$env:APP_LOCAL_INTEGRATION_PASSWORD='Foodtime2026!'
# 配置DB_URL、DB_USERNAME、DB_PASSWORD与Redis连接，再启动：
java -jar target/foodtime-neo-backend.jar
```

样例账号为 `20260001@bjtu.edu.cn`（user）、`20260002@bjtu.edu.cn`（admin）、`20260003@bjtu.edu.cn`（superadmin），密码来自上述专用配置。UUID使用 `00000000-0000-4000-8000-` 加12位十进制编号：用户1/2/3、食堂101、档口201、菜品301/302。名称明确标为联调样例，不代表校园真实菜单。

初始化只插入缺失记录，不覆盖已有账号密码或审核结果；在本地通过改密测试后，应使用改过的密码登录。正式邮箱投递按用户要求暂不验收，未配置时注册验证码请求按原契约返回503。

上述503指发送邮件请求；Backend的dev测试通道启用时，可以不发送邮件而手工使用 `000000` 注册，详见[测试验证码说明](testing-verification.md)。既有样例登录不需要重新注册。种子图片文件仍需按[图片部署说明](seed-data.md)准备，数据库样例或迁移成功不能代替媒体文件落盘。
