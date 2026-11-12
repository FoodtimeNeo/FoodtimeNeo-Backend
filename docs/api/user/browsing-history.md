# 浏览历史

服务器按账号保存帖子、菜品两栏最近浏览，各最多 50 条，不按时间过期。接口需要登录 Cookie；写请求需要 CSRF。visitor 仅可读写本人的浏览历史。

## 设置

`GET /api/v1/users/me/browsing-history/settings`

```json
{
  "enabled": true,
  "revision": 1,
  "dishEpoch": 0,
  "postEpoch": 0
}
```

`PUT /api/v1/users/me/browsing-history/settings`

```json
{ "enabled": false, "expectedRevision": 1 }
```

关闭只停止新增并保留已有记录。关闭时两类 epoch 各递增一次；重新开启不重置 epoch，因此关闭期间或关闭前的离线任务不会补发。`expectedRevision` 不一致返回 409。

## 记录访问

`PUT /api/v1/users/me/browsing-history/{kind}/{targetId}`

```json
{ "epoch": 0, "visitEventId": "6d6f..." }
```

- `kind`：`dish` 或 `post`。
- 只有详情成功返回且目标当前可见后才调用；列表曝光、预加载、失败响应和作者待审预览不得调用。
- `epoch` 来自设置中对应类型；过期返回 409 `HISTORY_EPOCH_STALE`，客户端应刷新设置，不能把旧事件换新 epoch 重放。
- `visitEventId` 用于重试去重；同一事件重复提交不重复改变最近顺序。
- 服务端使用服务器时间；记录历史不增加帖子浏览量。
- 目标不存在或不可见返回 404 `HISTORY_TARGET_NOT_FOUND`；关闭状态返回 409 `HISTORY_DISABLED`。

成功返回 204。

## 列表

`GET /api/v1/users/me/browsing-history?kind=dish&page=1&size=50`

按服务器访问时间倒序，同一对象只保留最近一次访问，按类型裁剪到 50 条。`size` 最大 50。返回统一分页协议：

```json
{
  "items": [
    {
      "kind": "dish",
      "targetId": "cc77444e-...",
      "visitedAt": "2026-11-12T08:00:00Z",
      "available": true,
      "title": "红烧肉",
      "subtitle": "第一食堂 · 面食档口",
      "imageUrl": "/api/v1/media/..."
    }
  ],
  "total": 1,
  "page": 1,
  "size": 50
}
```

目标下架、删除或帖子不可见时保留 `targetId`、`visitedAt`，返回 `available=false` 且 `title`／`subtitle`／`imageUrl` 为 null，不展示旧正文或旧图片；记录仍可删除。

## 删除与清空

| 方法 | 路径 | 语义 |
|---|---|---|
| DELETE | `/api/v1/users/me/browsing-history/{kind}/{targetId}` | 删除单项；若确实删除则递增该类型 epoch |
| DELETE | `/api/v1/users/me/browsing-history?kind=dish` | 清空指定类型并递增该类型 epoch；必须显式提供 kind |

两类 epoch 独立。删除／清空后，删除前已排队的事件带旧 epoch，服务端返回 409，不会把记录补回；用户重新打开详情属于新的真实访问，需先读取新 epoch。

## 实现

- `V50__browsing_history.sql`：设置、历史表与稳定顺序序列。
- `history/BrowsingHistoryService`、`BrowsingHistoryStore`、`api/user/BrowsingHistoryController`。
- 设计说明见 [浏览历史与旧任务防回写](../../architecture/browsing-history.md)。
