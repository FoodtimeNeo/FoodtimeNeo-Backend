# Android 本机联调

Android 使用 `http://127.0.0.1:8080/api/v1/`。设备连接主机后执行 `adb reverse tcp:8080 tcp:8080`；设备上的127.0.0.1会转发到主机Backend。会话Cookie与CSRF沿用正式认证流程，不添加客户端角色或免认证接口。

普通启动不会生成用户或样例。需要专用数据库样例时，显式启用 `dev,local-integration` profiles、设置 `app.local-integration.fixtures-enabled=true` 和 `app.local-integration.password`，数据库须在本机。请使用单独联调数据库；不要向业务数据库启用样例。

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
