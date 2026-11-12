# 收藏分组

菜品与帖子分别维护分组，不能跨类型混放。收藏事实仍保存在 `dish_favorites` / `community_favorites`；分组只是附加组织关系。

## 接口

| 方法与路径 | 说明 |
| --- | --- |
| `GET /api/v1/users/me/favorite-groups?kind=dish\|post` | 当前账号该类型的分组，按手动顺序返回；每类最多 20 个 |
| `POST /api/v1/users/me/favorite-groups` | 创建分组，body `{kind,name,sortOrder?}`；成功 201 |
| `PUT /api/v1/users/me/favorite-groups/{id}` | 改名／排序，body `{name?,sortOrder?,expectedRevision?}` |
| `DELETE /api/v1/users/me/favorite-groups/{id}` | 删除分组，仅删除分组关系，收藏仍保留；成功 204 |
| `GET /api/v1/users/me/favorite-groups/memberships?kind=&targetId=` | 某个收藏对象当前加入的分组 ID |
| `PUT /api/v1/users/me/favorite-groups/{id}/members/{targetId}` | 加入分组；对象必须已收藏 |
| `DELETE /api/v1/users/me/favorite-groups/{id}/members/{targetId}` | 移出分组，幂等 |

收藏列表增加可选筛选：

- `GET /api/v1/users/me/dish-favorites?groupId=...` 或 `?unfiled=true`
- `GET /api/v1/users/me/community-favorites?groupId=...` 或 `?unfiled=true`

`groupId` 只接受本人、同类型分组；`unfiled=true` 表示未加入任何该类型分组。两者不能同时使用。

## 规则

- 名称去首尾空白并做 NFKC 规范化，按 Unicode 码点计数 1–20；同账号同类型不能重名。
- 每账号每类型最多 20 个分组，服务端事务内并发校验；删除分组不删除收藏。
- 同一收藏可加入同类型多个分组；取消收藏会级联删除该对象的全部分组关系，重新收藏默认未分组。
- 分组只组织已收藏对象；加入分组不会自动创建收藏。
- 名称、分组归属和展示顺序都由服务端校验；客户端传入的 ownerId/kind 不能作为授权依据。

## 错误

| HTTP | `code` | 说明 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 类型、名称长度或控制字符不合法 |
| 404 | `GROUP_NOT_FOUND` | 分组不存在或不属于当前账号 |
| 404 | `FAVORITE_NOT_FOUND` | 目标尚未收藏，不能加入分组 |
| 409 | `GROUP_NAME_TAKEN` | 同类型下已有同名分组 |
| 409 | `GROUP_LIMIT_REACHED` | 已达到每类 20 个分组 |
| 409 | `GROUP_REVISION_CONFLICT` | 期望 revision 已过期，需刷新 |

## 迁移

V48 新增 `favorite_groups`、`dish_favorite_group_members`、`community_favorite_group_members`；成员表通过复合外键要求“分组属于本人且对象已收藏”，取消收藏时级联清理。
