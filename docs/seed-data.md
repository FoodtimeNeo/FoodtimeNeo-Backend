# 种子数据与图片部署

迁移脚本写入数据库中的目录和图片引用；图片字节位于仓库的 `seed-images/`，需要另外放入服务实际使用的存储目录。这两步缺一不可，应用 JAR 和当前 Dockerfile 都不会自动打包或铺放这些文件。

| 迁移 | 已有内容 |
| --- | --- |
| [V5](../src/main/resources/db/migration/V5__import_canteen_seed_data.sql) | 留园、明湖、学活、学四的菜单；有图菜品建立 approved 图片及审核日志；导入用户被禁用，仅承担数据关联 |
| [V6](../src/main/resources/db/migration/V6__import_hall_covers.sql) | 为上述四个食堂设置封面，新增东区学生餐厅和学苑餐厅；新增食堂尚无种子档口、菜单 |

当前图片目录为 `seed-images/v5/` 的57张JPEG和 `seed-images/v6/` 的6张JPEG。数量描述仓库文件，不代表任意部署环境已存在同样的数据和文件。不要启用导入用户或将其当作登录账号。

## 默认存储目录

从 Backend 根目录，在 PowerShell 中执行现有脚本：

```powershell
.\scripts\place-seed-images.ps1
```

[脚本](../scripts/place-seed-images.ps1) 遍历各版本目录，将每个 `<uuid>.jpg` 写为 `uploads/<uuid>.img`，同时生成 `<uuid>.properties`，其中包含文件归属和 `image/jpeg` 类型。只复制 JPG 而不生成元数据，媒体接口不能正常读取。重复执行会覆盖同 UUID 的种子文件及其元数据，不会清空其他上传文件。

随后按 [README](../README.md) 启动应用，Flyway只应用未执行的迁移。不要修改已执行迁移或手工重复执行整份SQL来“修复”图片文件。

脚本固定写入 Backend 根目录的 `uploads`。应用的 `app.storage.directory` 默认相对于进程工作目录；若使用自定义目录或从其他目录启动，需要将上述文件及元数据放到实际配置的位置。例如：

```sh
java -jar target/foodtime-neo-backend.jar --app.storage.directory=/var/lib/foodtime/uploads
```

Linux 环境可使用已安装的 PowerShell运行同一脚本；也可以从已铺放的目录完整复制 `.img` 和 `.properties` 到目标存储目录。现有脚本不会上传到远程服务器。

## 容器和备份

容器默认工作目录是 `/app`，因此默认图片目录是 `/app/uploads`。部署时挂载持久化目录，并确保非 root 的 `foodtime` 用户可读写；镜像重建不能代替文件迁移。Windows 的具体挂载示例见 [部署说明](deployment.md)。

数据库备份与上传目录应配套保留。只恢复数据库会留下失效的媒体引用，只恢复文件则不会创建目录、批准图片或审核日志。

## 图片不显示时

1. 确认迁移状态及数据库实际引用，使用已登录账号按原页面请求媒体。
2. 检查对应 UUID 的 `.img` 与 `.properties` 是否都在实际存储目录，进程是否有访问权限。
3. 检查食堂、档口、菜品及图片的实际状态。私有、不可见和不存在资源都可能返回404，不能仅凭404判定文件丢失。

新食堂菜单为空是 V6 当前的数据范围，不能由前端生成虚构档口或菜品。图片可读验证需要实际启动应用；源码和文件检查不等于已完成线上部署验收。
