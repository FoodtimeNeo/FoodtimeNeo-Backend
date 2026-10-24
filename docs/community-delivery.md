# 社区功能交付与验收

## 原型核查

开发起点：后端 `816d05a`、Web `1758547`、Android `6d3af35`。

两个前端已有页面原型。Web 社区默认使用 localStorage 模拟列表、发布和评论，点赞收藏在本地切换；Android 调试版默认启用原型数据源，发布只有正文和单图，评论平铺。后端已有基础接口，尚缺标题、图片尺寸、楼中楼关系、完整软删除和显式幂等互动。

本次已接通真实数据库和接口，完成确认的紧凑楼中楼样式。

## 子功能与 Git 流程

三个仓库均从最新开发分支建立功能分支，检查通过后提交并推送功能分支，以 `--no-ff` 合并并推送开发分支。后端和 Android 使用 `dev`，Web 使用原有的 `develop`。

| 功能分支 | 内容 |
|---|---|
| `feature/community-posts` | 标题、1–9 张图、真实图片尺寸、公开信息流、发布去重和审核阶段 |
| `feature/community-replies` | 多图详情、大图查看、一级评论与楼中楼、实际回复关系、删除占位 |
| `feature/community-interactions` | 帖子／评论／回复点赞、收藏、个人食记与收藏列表、软删除及一致计数 |
| `feature/community-moderation` | 完整帖子及全部配图、回复上下文、人工通过／驳回、带原因下架 |
| `feature/community-validation` | 真实联调验收、字段文档、窄屏布局、时间显示和媒体访问回归 |

## 已实现规则

- 标题最多 50 字，正文最多 2000 字，评论回复最多 500 字；原创帖配图 1–9 张。上传接受 JPEG/PNG，单张最多 10MB。
- 帖子、评论和回复不可编辑。一次发布的重试复用 `clientRequestId`，内容改变后使用新编号。
- 楼内所有回复归属同一个根评论，另存实际回复对象和被回复用户；界面只缩进一层。
- 审核通过才公开，作者可查看自己的待审和驳回内容。AI 不确定、不可用或关闭时转人工；转人工后不接受迟到的自动改判。
- 删除保留审核证据。删除根评论后，若仍有合法回复则显示占位；删除帖子后关闭其评论及互动入口。
- PUT/DELETE 明确设置点赞和收藏状态，数据库唯一键、外键、事务锁和触发器维护计数。
- 社区图片每次请求校验当前可见性并使用 `no-store`；公开目录图片保留原有缓存策略。

六张主表的真实字段和约束见 [community-schema.md](community-schema.md)，请求格式见 [community-api.md](community-api.md)。新增 V19–V23 迁移复用原有用户、AI 结论和审核日志。

## 验证方式

本次通过：后端 215 项单元测试与 34 项集成测试；Web 60 个测试文件、237 项测试；Android 16 项单元测试，以及下述真实设备接口、原生页面和图库验收。

后端使用 Java 21+，按项目 README 配置独立测试数据库和 Redis，然后运行：

```powershell
.\mvnw.cmd -Pintegration "-Dit.test=Community*IT,AiTriageIT,ImageStorageIT,StagedImageCleanupIT" verify
```

包含普通单元测试和选定的真实 HTTP、Cookie、CSRF、数据库与 Redis 集成验证，覆盖发布去重、图片权限和缓存、审核故障、跨楼拦截、游标、删除占位、并发互动、计数及管理权限。

继承 `IntegrationApiSupport` 的接口测试强制使用随机临时图片目录，覆盖本地 `.env` 的存储配置，避免暂存清理测试影响人工验收图片。回归前后已逐文件核对人工验收目录的文件名与 SHA256，结果完全一致。

Web：

```powershell
npm run check
```

包含 ESLint、Prettier、Vitest 和生产构建。浏览器已验证真实多图上传、发布、完整内容审核、楼内互相回复、评论审核和点赞收藏；同时检查 390px 与 1180px 两侧布局。

Android：

```powershell
.\gradlew.bat verifyArchitecture testDebugUnitTest lintDebug assembleDebug
```

设备验收程序需显式启用，并通过运行参数传入本地测试账号：

| Runner | 检查内容 | 所需参数 |
|---|---|---|
| `cn.edu.bjtu.foodtime.data.remote.CommunityRepositoryRunner` | 真实 Android HTTP、JSON、会话与社区闭环，21 项检查 | `userAccount`、`adminAccount`、`superadminAccount`、`testPassword` |
| `cn.edu.bjtu.foodtime.presentation.CommunityUiRunner` | 原生多图导入排序、发布、待审可见性、互相回复、缩进、点赞、删除占位及管理下架，12 项检查 | `userAccount`、`adminAccount`、`testPassword` |

构建时用 `FOODTIME_API_BASE_URL` 指定本机 API，`FOODTIME_DEBUG_SUFFIX` 指定独立联调应用标识，`FOODTIME_COMMUNITY_PROTOTYPE=false` 选择真实接口，`FOODTIME_TEST_RUNNER` 选择上述 Runner。通过 `adb shell am instrument` 的 `-e` 参数提供账号和密码。

UI Runner 的 `evidenceOnly=true` 模式只读已有“社区联调样例”帖子，检查图片翻页、全屏缩放和关闭，并更新应用私有目录 `files/community-ui-evidence/` 中的截图。完整写入验收创建明确标注的临时帖子并以软下架结束，保留审核记录；重复运行遵守正常上传和提交频率限制。

本次本机人工体验关闭 AI，内容进入人工审核。自动化测试通过显式指定的测试提供方验证高置信通过、不确定转人工和故障转人工；真实提供方沿用项目已有配置。
