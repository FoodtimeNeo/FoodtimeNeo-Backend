# 目录详情快捷管理

平台管理员可在食堂、档口和菜品详情中编辑目录，并直接维护图片。原有管理员目录权限不变；新图片管理接口仅允许 active 的 superadmin。

- `GET /api/v1/admin/catalog/{type}/{id}/images?page=1&size=20` 返回分页图片，type 为 dining-halls、stalls 或 dishes。
- `POST` 同一路径，正文 `{ "imageUrl": "/api/v1/media/<uuid>" }`。必须是本人上传的图片，菜品直接审核通过，档口加入图集，食堂替换封面。同一图片重复添加不产生重复记录。
- `DELETE /api/v1/admin/catalog/{type}/{id}/images/{imageId}` 移除指定目录的图片。菜品保留拒绝记录；档口删除对应图集项并在必要时从剩余图片选封面；食堂清除封面。文件可能被其他内容引用，因此不删除共享文件。

添加和删除都验证权限、父级和图片归属；对父级加锁使同一目录的图片修改串行。目录删除仍使用原有接口与外键保护，有关联内容时返回 409，可选择停用。
