# 基础数据与正式评论管理

仅admin/superadmin；写请求需Cookie与CSRF。普通用户无法通过指定角色或状态访问管理能力。

| 方法与路径 | 行为 |
| --- | --- |
| `GET /api/v1/admin/catalog/{type}?parentId=UUID&page=1&size=20` | type为dining-halls/stalls/dishes，包含active/inactive；子级可按父ID筛选 |
| `GET /api/v1/admin/catalog/{type}/{id}` | 管理详情 |
| `POST /api/v1/admin/catalog/{type}` | 创建正式记录，201，服务端生成ID |
| `PUT /api/v1/admin/catalog/{type}/{id}` | 编辑名称、简介、价格、封面、状态及适用的排序值，200 |
| `DELETE /api/v1/admin/catalog/{type}/{id}` | 仅删除没有关联引用的记录，有关联返回409，不级联删除历史 |
| `GET /api/v1/admin/reviews?page=1&size=20` | 正式评论管理列表，含停用父级历史 |
| `DELETE /api/v1/admin/reviews/{id}` | body须提供非空 `reason`；删除违规正式评论，保留原提交和审核日志 |

目录请求为 `parentId/name/description/price/coverImageUrl/status/sortOrder`。菜品须非负两位小数价格，名称最多120；食堂/档口名称最多100，同食堂档口名称唯一。parentId映射真实外键；创建子级时父级须存在，管理端允许父级已停用。编辑保持原归属（沿用当前管理交互），不允许夹带迁移所属关系。状态为active/inactive，未提供时创建默认active、编辑保留原值。

封面和排序值只用于食堂/档口；更换封面须使用本人上传的引用，原封面未改变时可保留原引用。菜品列表管理响应的封面来自approved的dish_images，不是新增菜品列；菜品图片走独立上传/审核流程。没有floor、分类或其他未定义业务字段。

响应为 `id/parentId/name/description/price/coverImageUrl/status/sortOrder`，不适用的字段为null。修改结果在后续查询中体现，停用父级阻断学生端展示。名称冲突409且事务回滚；无权限403、记录不存在404、参数400、基础设施503。目录删除由现有RESTRICT外键保证没有孤立数据，包括业务内容和审核历史的引用；仍有关联时返回409，可选择停用。管理员删除正式评论须填写处理原因，在服务事务提交后记录含时间/操作者/对象/原因的运行审计日志，不伪造数据库approve/reject以外动作，也不即时重算物化评分。
