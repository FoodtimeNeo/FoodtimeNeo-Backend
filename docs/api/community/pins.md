# 社区帖子置顶

新增 V56，仅添加独立置顶清单与版本状态，不改写旧迁移。最多保留 3 条，序号 1–3；操作使用全局版本进行并发校验，位置唯一约束在事务提交时检查。`community.pin.manage` 能力默认仅超管允许，权限可独立下放，不在各服务散落角色判断。

| 接口 | 用途 |
|---|---|
| `GET /api/v1/community/pins` | 当前可见置顶；需有效会话，普通用户不返回隐藏标题 |
| `GET /api/v1/admin/community/pins` | 管理清单，包含待审核／被拒的保留名额 |
| `PUT /api/v1/admin/community/pins/{id}` | 置顶已通过且未删除的帖子，JSON `{expectedRevision}` |
| `DELETE /api/v1/admin/community/pins/{id}?expectedRevision=...` | 取消置顶，剩余序号压紧 |
| `PUT /api/v1/admin/community/pins/order` | JSON `{expectedRevision,postIds}`，完整、不重复的当前清单顺序 |

快照 `{revision,items,canManage}`；条目包含 `id/title/userDisplayName/status/position`，不包含正文、图片或私密审核材料。最多 3 条包含隐藏名额；待审核或被拒不公开，审核通过后恢复原顺序。删除或下架在同一事务取消置顶并释放名额；不能通过恢复旧记录偷偷恢复置顶。该迁移只准备状态联动，作者编辑入口由后续独立功能交付。

发现游标流 `GET /api/v1/community/feed/cursor` 新增可空 `pins` 快照。只有无关键词、非关注的发现流提供快照，同时从普通 `items/total` 排除置顶。关注、关键词搜索、作者页、收藏及我的食记保持原规则；原偏移分页 `/community/feed` 保留原契约。

客户端加载下一页时提交首轮 `pinRevision`。置顶集合或可见性发生变化时返回 409 `PIN_SET_CHANGED`，客户端重新读取第一页和置顶快照，不把新的第一页追加到旧列表。同一读取事务保证置顶、普通条目、总数和版本来自同一快照。管理冲突为 409 `PIN_CONFLICT`，应让操作者刷新核对，不能自动重复写入。

保持原帖 ID、发布时间、互动关系和计数。置顶取消后回到按原发布时间排序的位置。数据库串行化名额变更，测试验证并发争抢最后一个名额只能一个成功；状态变动、删除和排序遵守一致锁序。
