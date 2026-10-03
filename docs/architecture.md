# 项目目录与职责

Backend是独立Maven项目，在本目录运行构建和启动命令。Java根包为 `com.FoodtimeNeo`。

```text
FoodtimeNeo-Backend/
├── config/                      外部邮件配置模板及被Git忽略的正式配置
├── docs/                        架构、配置、数据库与部署说明
│   └── api/                     接口说明与HTTP联调请求
├── src/main/java/com/FoodtimeNeo/
│   ├── FoodTimeNeoApplication.java
│   ├── api/
│   │   ├── auth/                认证Controller与认证HTTP异常处理
│   │   └── system/              HTTP运行检查Controller
│   ├── auth/
│   │   ├── dto/                 认证请求、响应类型
│   │   ├── service/             登录、注册编排及登录限流
│   │   ├── security/            会话、身份校验与安全过滤器
│   │   └── verification/        邮箱验证码发送、Redis校验与占用
│   ├── user/
│   │   ├── entity/              用户持久化输入及查询结果
│   │   ├── mapper/              参数化数据库访问
│   │   └── service/             用户默认昵称生成
│   ├── dish/service/            菜品评分汇总刷新
│   ├── common/
│   │   ├── api/                 统一响应类型
│   │   ├── exception/           共享业务异常与全局HTTP异常处理
│   │   ├── web/                 请求ID等通用Web支持
│   │   └── mybatis/             PostgreSQL UUID类型映射
│   └── config/
│       ├── *.java               Spring Bean、安全链及基础设施装配
│       ├── properties/          类型化配置参数与参数约束
│       └── validation/          启动时环境与邮件配置检查
├── src/main/resources/
│   ├── application*.yml         共享、开发和生产环境配置
│   ├── db/migration/            Flyway版本化数据库迁移
│   └── redis/email/             验证码原子操作Lua脚本
├── src/test/java/com/FoodtimeNeo/
│   ├── api/auth/                认证HTTP协议与参数校验测试
│   ├── auth/service/            登录与注册业务测试
│   ├── auth/security/           会话和基础设施故障测试
│   ├── auth/verification/       邮件验证码业务测试
│   ├── common/web/              通用Web测试
│   ├── config/                  配置、CORS与密码存储测试
│   └── integration/             真实HTTP、数据库、Redis及回环SMTP测试
├── .mvn/、mvnw、mvnw.cmd          Maven Wrapper
├── pom.xml                      依赖与构建
├── compose.yml                  本地PostgreSQL与Redis
└── Dockerfile                   应用镜像构建
```

## 分层约定

- `api` 处理URL、HTTP状态、参数校验与响应；业务编排调用领域Service。
- `auth`、`user`、`dish` 按业务领域维护功能。DTO定义明确的输入输出，敏感持久化类型与公开响应分开，避免返回密码哈希或把客户端角色写入数据库。
- Mapper负责数据库读写；验证码Store负责Redis操作。服务层负责密码验证、验证码生命周期和业务失败信息。
- `config` 配置运行时组件；`properties` 绑定配置值，`validation` 检查跨配置项约束。二者由启动类所在根包扫描。
- `common` 仅放跨领域共享内容；新增功能优先归属业务领域，避免堆入通用工具类。
- 新增HTTP入口放在 `api/<领域>`，测试目录对应被测包；需外部依赖的测试使用 `*IT` 命名，集中放在 `integration`。

新增领域时按实际代码建立目录。已有迁移文件不因目录整理而修改；数据库演进规则见 [数据库说明](database.md)。接口文档维护在 [docs/api](api/README.md)。

## 本地文件与版本控制

`target/` 是可重新生成的构建产物，`logs/` 是运行日志，`tmp/` 是临时联调数据，`.m2/` 是本地依赖缓存；均已被Git和Docker忽略，不属于源代码结构。正式邮件配置 `config/mail.yml` 与 `.env` 同样不提交；保留模板供其他开发者配置。

构建和启动设置见 [配置说明](configuration.md)，生产镜像与部署见 [部署说明](deployment.md)。
