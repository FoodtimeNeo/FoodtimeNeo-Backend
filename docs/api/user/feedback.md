# 建议与反馈

登录用户可提交并查看自己的反馈；普通管理员只有自己的记录权限。全部反馈管理、回复和 CSV 导入导出仅限 active superadmin，路由和服务均检查权限。

## 用户接口

| 方法与路径 | 行为 |
| --- | --- |
| POST `/api/v1/users/me/feedback` | `{type,content,clientRequestId}`，反馈类型为 function/content/usage/suggestion/other，内容 1–5000 字，请求编号为 UUID |
| GET `/api/v1/users/me/feedback?archived=false&page=1&size=20` | 本人未完成记录；archived=true 为已完成归档 |
| GET `/api/v1/users/me/feedback/{id}` | 本人详情，非本人或不存在均返回 404 |
| GET `/api/v1/users/me/feedback/{id}/replies?page=1&size=20` | 完整回复历史分页，最新回复在前 |

同一用户以相同请求编号重试不重复创建，也不重复占用额度；编号被用于不同内容时返回 409。每天按 UTC+8 00:00 重置：访客每账号 2 条，认证用户每账号 5 条，同设备合计 10 条。仅成功创建的记录占用额度；回复、完成或归档不释放当日额度。

额度在数据库事务中与创建一起判断和提交，设备与账号加锁防止并发超额。设备沿用服务器签名 Cookie，不信任客户端随意提交的设备编号，不采集硬件指纹。清除 Cookie 或重装应用可能获得新标识，不能视为不可绕过的物理设备识别。Android 退出登录保留 `foodtime_device`，清除认证 Cookie；设备 Cookie 不参与会话缓存身份。

## 超级管理员接口

| 方法与路径 | 行为 |
| --- | --- |
| GET `/api/v1/admin/feedback?archived=false&page=1&size=20` | 全部未完成记录，archived=true 查询完成归档 |
| GET `/api/v1/admin/feedback/{id}` | 任意反馈详情 |
| GET `/api/v1/admin/feedback/{id}/replies?page=1&size=20` | 回复历史分页 |
| PUT `/api/v1/admin/feedback/{id}` | `{reply,status,clientRequestId}`，追加回复或完成；空白回复保留历史，状态空白保留原状态 |
| POST `/api/v1/admin/feedback/export` | 返回 `{filename,csv,count}`；两端保存为 CSV 文件，导出包含所有未完成记录并将 pending 标为 processing |
| POST `/api/v1/admin/feedback/import` | `{csv}`，返回 `{updated,skipped,issues:[{row,message}]}`，逐条处理有效记录并列出失败或跳过原因 |

状态为 pending（待处理）、processing（处理中）、completed（已完成）。是否导出决定待处理或处理中，不能手动在这两个状态之间切换；可以完成归档。已完成反馈保留历史，不接受旧表格再次修改，也不进入下次默认导出。

导出表为 UTF-8（带 BOM），列为“反馈编号、反馈类型、反馈内容”。超管保留原三列，新增“回复、处理状态”，列顺序可变，列名不可重复。处理状态使用中文；回复、状态空白均表示不修改。文件应另存为 CSV UTF-8，不接受 XLSX 或非 UTF-8 字节。导入文本上限为 200 万字符。

CSV 引号、逗号、中文、emoji、多行内容均保留；疑似表格公式的原内容以文本形式导出，导入核对原内容时支持该安全前缀。编号准确匹配记录，并核对类型与原内容，原内容被改动的行不写入。重复编号只处理首次出现，后续行提示；重复导入相同回复及状态不重复追加。每行在独立事务中加锁、追加回复并更新状态，该行失败不会留下半条修改，也不会撤销其他有效行。表头缺失或 CSV 引号损坏时无法可靠解析，整份拒绝并说明原因。

## 迁移与验证

新增 Flyway V44，保存反馈、回复历史及去重动作。需要先升级后端再更新客户端。`FeedbackCsvTest` 覆盖 CSV 引号、换行和公式前缀，`FeedbackServiceTest` 覆盖 UTC+8 午夜边界，`FeedbackIT` 覆盖真实 HTTP/CSRF、权限、归档、重复内容、部分行处理、历史回复、并发去重和每日额度。
