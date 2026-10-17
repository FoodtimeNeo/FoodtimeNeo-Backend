# 图片文件

`POST /api/v1/uploads/images`，需Cookie和CSRF；multipart字段 `file`，内容必须是JPEG或PNG，最多10MB/2000万像素。返回201，data为 `{id,imageUrl,width,height}`。尺寸和文件ID是存储协议数据，不增加菜品表字段。

文件存于 `app.storage.directory`（默认工作目录下的 `uploads`），服务端生成UUID文件名并记录上传者。上传仅创建私有暂存文件，不直接发布或创建菜品图片记录；投稿使用返回的相对引用 `/api/v1/media/{id}`，服务端会检查引用归属。

`GET /api/v1/media/{id}[?size=thumb]` 返回实际图片字节。`size=thumb` 返回最长边480px的同格式预览图（首次请求时生成并缓存），列表与卡片请用它；点开展示才用原图。其他 `size` 取值返回400。上传者及管理员可预览暂存图片；其他已登录用户仅能读取已批准的菜品图片，或启用食堂/档口的公开封面。所有父级启停状态实时校验。无权读取与不存在均为404。公开图片内容按UUID寻址、永不变化，返回 `Cache-Control: public, max-age=31536000, immutable` 与 `ETag`，浏览器长期缓存、刷新不重复加载，`If-None-Match` 命中返回304；暂存预览仍 `no-store`，避免登出后复用私有缓存。

未经认证不能上传；无CSRF返回403，错误内容返回400，超出请求上限返回413，存储故障返回503。上传按用户与来源IP限流（默认每人每小时10次），超限返回429 `UPLOAD_RATE_LIMITED` 并带 `Retry-After`。服务器不接受原始文件名作为路径，不把图片内容或Cookie写入日志。部署需挂载持久化上传目录；正式对象存储接入不影响业务表中的图片引用。

暂存文件若长期未被任何投稿、封面或头像引用，由定时任务清理（默认保留7天，`STORAGE_CLEANUP_MAX_AGE` 可配）；被引用文件不受影响。
