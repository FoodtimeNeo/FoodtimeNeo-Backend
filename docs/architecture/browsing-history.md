# 浏览历史与旧任务防回写（Q14 技术方案）

## 目标

服务器按账号保存帖子、菜品两类浏览历史，各最多 50 条；同一对象合并为最近一次访问。关闭记录只停止新增，保留已有；删除单项或清空一类后，删除前已排队的离线任务不能把记录补回。

## 代次方案

`browsing_history_settings` 保存账号开关、设置 revision 和分类型 `dish_epoch`／`post_epoch`。客户端打开详情前读取设置，记录请求必须携带当前类型的 epoch。

| 操作 | 服务端行为 | 旧任务结果 |
|---|---|---|
| 记录 | 校验开关、epoch 与目标可见性后 upsert，并按类型裁剪到 50 条 | epoch 过期返回 409，不写入 |
| 删除单项 | 删除记录并递增该类型 epoch | 删除前事件 epoch 过期，被拒绝 |
| 清空一类 | 删除该类型全部记录并递增该类型 epoch | 同上；另一类 epoch 不变 |
| 关闭记录 | `enabled=false`，递增两类 epoch | 关闭期间与关闭前任务均被拒绝 |
| 重新开启 | 只改回 `enabled=true`，不把 epoch 重置到 0 | 关闭期间事件仍带旧 epoch，不会补发 |

epoch 单调递增且不重用。客户端遇到 409 `HISTORY_EPOCH_STALE` 时重新读取设置，只允许用户新的真实详情访问创建新事件；不得自动把旧事件换上新 epoch 重放。

这是“拒绝旧任务”的方案，不实现允许旧任务恢复已清空数据的路径。单次删除采用粗粒度类型代次，代价是同一类型中其他在途访问可能被拒绝一次；客户端刷新后，用户新的真实访问可正常记录。该代价换取不保留逐目标 tombstone 的存储与清理复杂度，符合当前“最多 50 条、无时间过期”的规模。

## 记录时机

- 只有详情成功返回且目标当前可见时才记录；列表曝光、预加载、失败响应、作者待审／拒绝预览都不记录。
- 服务端不信任设备时间，访问时刻与稳定顺序由服务器生成。
- `visitEventId` 用于重试去重：同一事件重复提交只更新一次最近顺序；不同事件更新最近访问。
- 帖子只接受 `approved` 且未删除；菜品只接受菜品、档口、食堂均 `active`。
- 浏览历史与帖子浏览量分离，记录历史不增加 `view_count`。

## 读取

列表只返回本人记录。目标已失效时保留 `targetId`、`visitedAt` 和 `available=false` 占位，不返回旧标题、正文或图片；用户仍可删除该记录。重新使用目录／帖子当前可见性投影，不能因为历史曾保存过摘要而越权显示。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/users/me/browsing-history/settings` | 开关、revision、两类 epoch |
| PUT | `/api/v1/users/me/browsing-history/settings` | `enabled` + `expectedRevision` 条件更新 |
| GET | `/api/v1/users/me/browsing-history?kind=dish&page=1&size=50` | 分类型最近列表 |
| PUT | `/api/v1/users/me/browsing-history/{kind}/{targetId}` | `{epoch, visitEventId}` 记录访问 |
| DELETE | `/api/v1/users/me/browsing-history/{kind}/{targetId}` | 删除单项并递增该类型 epoch |
| DELETE | `/api/v1/users/me/browsing-history?kind=dish` | 清空指定类型并递增 epoch；不省略 kind |

visitor 仅开放上述本人历史路径，不开放其他 `/users/me/**` 写入。

## 验证

- 两类各 50／51 条、并发 upsert 不超 50；
- 关闭后拒绝新增、保留旧记录；重新开启不补发关闭期间事件；
- 删除单项／清空一类后旧 epoch 写入被拒，另一类不受影响；
- 目标下架／删除后列表只返回占位，不泄露旧内容；
- 换号／注销后不继承旧历史；同邮箱重注册 UUID 不共享。
