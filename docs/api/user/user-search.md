# 搜索用户

`GET /api/v1/users/search?q=昵称&page=1&size=20`，需要有效会话。Web 与 Android 从信箱、关注／粉丝页进入搜索。

用户名对应现有 `users.display_name`（昵称），不搜索登录邮箱。去除关键词首尾空白，最多 50 个字符；不区分大小写，支持包含匹配。排序依次为完整匹配、前缀匹配、其他包含匹配，同组按昵称和 UUID 排序。`%`、`_`、`!` 按字面搜索，不作为 SQL 通配符。空关键词返回空页，不列举所有用户。

只返回其他活跃用户，不返回自己或停用用户；角色不影响被搜索资格。默认每页 20 条，沿用分页约束：`page >= 1`，`1 <= size <= 100`。非法关键词或分页返回 400，未登录返回 401。没有新增表、迁移或第三方搜索依赖。

统一响应的 `data`：

```json
{
  "items": [
    {
      "id": "8f9fe093-e51f-4f00-9439-998f76aab74b",
      "displayName": "干饭同学",
      "avatarUrl": null,
      "followedByMe": false
    }
  ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

行字段仅以上四项，不返回邮箱、角色、账号状态、认证或私信数据。关注标志在同一列表 SQL 中查询，列表和总数在只读一致性事务中读取，没有每行追加关注请求。

行操作复用 `PUT/DELETE /api/v1/users/{id}/follow` 和 `POST /api/v1/users/me/conversations`（`{"userId":"UUID"}`）；沿用 Cookie、CSRF、限流和访客写入限制。访客可搜索，绑定学校邮箱后才能关注、私信。打开私信只取得或创建会话，不代替用户发送消息。

回归入口：`UserSearchServiceTest`（空关键词、长度校验）与 `UserSearchIT`（真实 PostgreSQL／Redis、排序、分页、字面匹配、权限、公开字段及行操作衔接）。两端输入停顿 300ms 后搜索，空输入不请求，旧查询结果不能覆盖新查询；关注成功仅更新对应行。
