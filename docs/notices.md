# 登录公告与个人消息

## 本轮范围

公告仅在主动登录、冷启动或页面刷新恢复会话后检查一次。Web 和 Android 不再创建 60 秒公告轮询。创建/编辑管理流程后续实现；现有管理接口仅保留兼容，并限制启用状态的 superadmin。

## V27 数据结构

- foodtime.notices：notice_id UUID 主键、created_time TIMESTAMPTZ、content TEXT、created_by UUID。正文只有一份 Markdown content，不再拆分 title/body。seq 仅为旧客户端 feed 接口兼容保留，新客户端不使用它。
- foodtime.config：严格只有 id SMALLINT、notice_id UUID 两列。id 是主键且 CHECK(id=1)，初始化唯一行 (1,NULL)。notice_id 外键指向公告，删除当前公告时 SET NULL。没有更新时间或修改人字段。
- foodtime.notice_reads：联合主键 (notice_id,user_id)，保留 read_at，新增 dont_show_again BOOLEAN NOT NULL DEFAULT FALSE。
- foodtime.messages 不变。

V27 将旧标题和正文合并为 Markdown，保留公告 ID、时间、发布人及阅读记录。旧阅读记录的免弹标记默认 false；不把“读过”误认为“选择免弹”。已执行的 V26 不修改。

当前公告由 config.notice_id 指定；NULL 表示没有登录公告。管理配置入口本轮不新增。专用测试环境可通过 SQL 指定已存在的公告：

    UPDATE foodtime.config SET notice_id = '<公告 UUID>' WHERE id = 1;

## 用户接口（Session Cookie 鉴权，写操作同时需要 CSRF）

| 方法和路径（前缀 /api/v1） | 数据和行为 |
|---|---|
| GET /users/me/notices/current | 无配置或该用户 dont_show_again=true 时 data=null；否则返回 id/content/createdTime/isRead/dontShowAgain，不写阅读状态 |
| POST /users/me/notices/{id}/confirm | JSON 必须含 dontShowAgain 布尔值；记录阅读并按选择保存免弹，返回 isRead/dontShowAgain |
| GET /users/me/notices/unread-count | 返回 unreadCount；读操作后按需刷新，无定时任务 |
| GET /users/me/notices?page=1&size=20 | 历史列表，倒序分页；content 是完整 Markdown，旧 title/body/publishedAt 响应字段为兼容适配 |
| POST /users/me/notices/{id}/read | 历史阅读接口；幂等，不修改免弹字段 |

确认始终作用于 URL 中实际展示的公告 ID，不改写为 config 当前值。重复提交不重复建行；并发的普通确认通过 OR 合并，不会清除另一端已保存的 true。免弹为账号级、跨端有效。

未勾选确认后：已读但下次登录仍可展示，本次登录不重复。勾选确认后：同一公告以后不弹。关闭不写数据；请求失败可重试，客户端保留勾选状态。退出和切换账号会丢弃旧请求及弹窗状态。

GET /users/me/notices/feed 仅兼容旧安装包，响应仍为正序 items 和未读计数。新两端不依赖 feed，也不使用本地水位判断新公告。

## 管理接口兼容

现有 /admin/notices CRUD 接口仅允许 active superadmin，普通 admin 返回 403。旧请求 title/body 会转换为一份 content；新创建/编辑界面与配置管理不在本轮范围。不能原地运行旧 Backend 二进制访问 V27 数据库，需要配套更新后端应用。

## Markdown 与前端行为

两端支持标题、段落、粗体/斜体、列表、引用、代码、删除线、表格、普通网页链接和网络图片。Web 禁用原始 HTML 并清洗生成 HTML；Android 使用原生 spans，不执行 HTML，不打开任意 intent/file 链接或加载本地文件图片。图片建议使用可访问的绝对 HTTP(S) 地址。

Web 刷新页面视为恢复会话；Android Activity 旋转重建复用本次登录状态，进程冷启动重新检查。普通路由切换和返回前台不重查。检查失败不阻止登录，提供明确重试入口。已读计数与是否免弹独立。

## 消息兼容

个人消息继续通过 /users/me/messages 分页、/unread-count 查询计数、/{id}/read 或 /read-all 标记已读。仅允许操作当前用户自己的消息。消息自动产生的业务流程不在本轮范围。
