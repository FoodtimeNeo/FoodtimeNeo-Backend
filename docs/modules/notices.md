# 登录公告与个人消息

## 本轮范围

公告在主动登录、冷启动、页面刷新恢复会话和返回前台时检查；不使用定时轮询。V45 已增加独立排期与草稿管理，详见 [公告管理与排期](notice-management.md)。每次会话同一公告只显示一次；更新与普通公告各取最新有效一条，更新优先。

## V27 数据结构

- foodtime.notices：notice_id UUID 主键、created_time TIMESTAMPTZ、content TEXT、created_by UUID。正文只有一份 Markdown content，不再拆分 title/body。seq 仅为旧客户端 feed 接口兼容保留，新客户端不使用它。
- foodtime.config：严格只有 id SMALLINT、notice_id UUID 两列。id 是主键且 CHECK(id=1)，初始化唯一行 (1,NULL)。notice_id 外键指向公告，删除当前公告时 SET NULL。没有更新时间或修改人字段。
- foodtime.notice_reads：联合主键 (notice_id,user_id)，保留 read_at，新增 dont_show_again BOOLEAN NOT NULL DEFAULT FALSE。
- foodtime.messages 不变。

V27 将旧标题和正文合并为 Markdown，保留公告 ID、时间、发布人及阅读记录。旧阅读记录的免弹标记默认 false；不把“读过”误认为“选择免弹”。已执行的 V26 不修改。

以下为 V27 旧配置兼容方式；V45 新管理使用每条公告独立 enabled 和时间区间，升级只启用原来的当前公告。旧客户端配置仍保留：

    UPDATE foodtime.config SET notice_id = '<公告 UUID>' WHERE id = 1;

## 用户接口（Session Cookie 鉴权，写操作同时需要 CSRF）

| 方法和路径（前缀 /api/v1） | 数据和行为 |
|---|---|
| GET /users/me/notices/current-set | 按服务器时间返回有效的更新、普通公告数组，最多两条，包含 publishedAt/displayEndsAt/isUpdate，不写阅读状态 |
| GET /users/me/notices/current | 兼容旧客户端，返回 current-set 第一条或 null |
| POST /users/me/notices/{id}/confirm | JSON 必须含 dontShowAgain 布尔值；记录阅读并按选择保存免弹，返回 isRead/dontShowAgain |
| GET /users/me/notices/unread-count | 返回 unreadCount；读操作后按需刷新，无定时任务 |
| GET /users/me/notices?page=1&size=20 | 仅已发布且到发布时间的历史列表，按发布时间倒序；到期与停用公告仍留档，草稿和未来公告不可见 |
| POST /users/me/notices/{id}/read | 历史阅读接口；幂等，不修改免弹字段 |

确认始终作用于 URL 中实际展示的公告 ID，不改写为 config 当前值。重复提交不重复建行；并发的普通确认通过 OR 合并，不会清除另一端已保存的 true。免弹为账号级、跨端有效。

未勾选确认后：已读但下次登录仍可展示，本次登录不重复。勾选确认后：同一公告以后不弹。关闭不写数据；请求失败可重试，客户端保留勾选状态。退出和切换账号会丢弃旧请求及弹窗状态。

GET /users/me/notices/feed 仅兼容旧安装包，响应仍为正序 items 和未读计数。新两端不依赖 feed，也不使用本地水位判断新公告。

## 管理接口兼容

现有 /admin/notices CRUD 接口仅允许 active superadmin，普通 admin 返回 403。旧请求 title/body 会转换为一份 content；新版独立启停和排期见管理文档。先升级数据库与后端，再更新客户端。

## Markdown 与前端行为

两端支持标题、段落、粗体/斜体、列表、引用、代码、删除线、表格、普通网页链接和网络图片。Web 禁用原始 HTML 并清洗生成 HTML；Android 使用原生 spans，不执行 HTML，不打开任意 intent/file 链接或加载本地文件图片。图片建议使用可访问的绝对 HTTP(S) 地址。

Web 刷新页面视为恢复会话；Android Activity 旋转重建复用本次登录状态，进程冷启动和返回前台重新检查。普通路由切换不重查。检查失败不阻止登录，提供明确重试入口。两类公告之间重新校验有效期。已读计数与是否免弹独立。

## 消息兼容

个人消息继续通过 /users/me/messages 分页、/unread-count 查询计数、/{id}/read 或 /read-all 标记已读。仅允许操作当前用户自己的消息。

社区互动会写入以下分类，`refType` 固定为 `community-post`：

| category | 触发 | 收件人 | 聚合 |
| --- | --- | --- | --- |
| `LIKE_POST` | 食记被点赞/取消 | 食记作者 | 同一食记只保留一条，`actorCount` 为点赞人数；全部取消后删除 |
| `LIKE_COMMENT` | 评论被点赞/取消 | 评论作者 | 同一评论只保留一条，`actorCount` 为点赞人数；全部取消后删除 |
| `COMMENT_POST` | 根评论审核通过 | 食记作者 | 每条评论一条 |
| `REPLY_COMMENT` | 回复审核通过 | 被回复者 | 每条回复一条 |

字段约定：`refId` 为食记 ID，`refCommentId` 为目标评论，`refRootCommentId` 为所在楼层根评论（帖子级为空）。客户端据此跳转原帖并定位楼层/回复；自己的点赞和评论不产生消息。原帖被作者或管理员删除时，该帖下全部互动消息一并删除；评论被删除时只清理指向该评论本身的消息，其回复若仍以墓碑/平铺形式可见则保留通知。

## 一对一私信

私信与系统消息分开存储：`conversations` 保存双方与各自的已读/隐藏位置，`direct_messages` 保存纯文本正文（最多 1000 字）。

- 入口：`/api/v1/users/me/conversations`；开始会话按 UUID 排序保证双方唯一，重复调用幂等
- 列表/未读/摘要/消息/发送/已读/隐藏接口均在上述前缀下；发送使用 `clientRequestId` 幂等
- 隐藏只影响当前用户，对方发送新消息会恢复；未读按各自 `last_read_at` 计算并计入信箱角标
- 访客只读：可查看、标已读、隐藏，但不能新建会话或发送
- 发送限流 `app.message.dm`（默认每用户 30 次/分钟、每 IP 300 次/分钟）
