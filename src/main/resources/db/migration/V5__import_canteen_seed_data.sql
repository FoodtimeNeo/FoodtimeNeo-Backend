-- 四食堂菜单种子数据：留园/明湖/学活/学四（源自 test/*.pdf 文字与版式图片）。
-- 档口命名：有品牌用品牌名，无品牌用窗口编号；窗口/楼层记入档口简介。
-- 无图菜品只建菜品行；有图菜品另建 approved 图片行并写审核日志。
-- 图片文件随本分支 seed-images/v5/ 提交；各环境需执行
-- scripts/place-seed-images.ps1 将其铺入应用 uploads/ 目录后图片方可读取。
-- 种子用户仅满足图片上传人/审核人外键：已禁用，口令不可登录。

INSERT INTO foodtime.users(id,email,password_hash,display_name,role,status)
VALUES ('39d46f72-3ea4-57b9-a25f-8d3a6779e84d','seed-import@foodtime.local','LOCKED-NOT-LOGINABLE','菜单导入种子','user','disabled')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dining_halls(id,name,description,status,sort_order)
VALUES ('68166b66-14d1-58c5-960d-572228254c20','留园食堂',NULL,'active',0)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('a7dd62f7-868e-5afe-a249-a23e65a2fa5d','68166b66-14d1-58c5-960d-572228254c20','窗口一',NULL,NULL,'active',0)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c62b5d1e-ed4a-551c-8613-edb7a64c33a7','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','清炒小白菜',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('03f83f68-07d6-5090-baa3-3127acbb30a5','c62b5d1e-ed4a-551c-8613-edb7a64c33a7','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/03f83f68-07d6-5090-baa3-3127acbb30a5','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('1ccbd09c-b208-56df-b8ec-1d76b284248e','03f83f68-07d6-5090-baa3-3127acbb30a5','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('95239957-ef0c-50a1-b9c8-0dabf6fd91b1','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','红烧茄子',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('9b2a50d1-3c3e-502c-b778-37a64fd9063a','95239957-ef0c-50a1-b9c8-0dabf6fd91b1','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/9b2a50d1-3c3e-502c-b778-37a64fd9063a','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('ae72e29d-d5b5-5002-b0ce-69912010f887','9b2a50d1-3c3e-502c-b778-37a64fd9063a','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73b52f0b-6201-5659-b503-75b00d118c88','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','番茄炒鸡蛋',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('bc1d13f9-5e83-5446-bfef-76553d8c13e1','73b52f0b-6201-5659-b503-75b00d118c88','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/bc1d13f9-5e83-5446-bfef-76553d8c13e1','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('5d6164cd-163a-5703-94ed-008e192e8d5c','bc1d13f9-5e83-5446-bfef-76553d8c13e1','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ca8f7c29-aa75-544c-ad9a-2dc4876b4c7d','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','土豆丝炒豆角',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('c7507a18-bc48-5502-982c-bf7198882c58','ca8f7c29-aa75-544c-ad9a-2dc4876b4c7d','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/c7507a18-bc48-5502-982c-bf7198882c58','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('536ad701-705e-5eba-8bfb-078edc51e232','c7507a18-bc48-5502-982c-bf7198882c58','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('53581bc8-d270-5745-9d87-bcd88c8611dc','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','炸小酥肉',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('b8ad2b2d-b18a-5319-bb80-349276df1e6d','53581bc8-d270-5745-9d87-bcd88c8611dc','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/b8ad2b2d-b18a-5319-bb80-349276df1e6d','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('88489552-3b88-5761-9a6f-40ef7993bb81','b8ad2b2d-b18a-5319-bb80-349276df1e6d','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('787b711f-b262-51e7-9255-2ceedc765e25','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','炸鸡块',NULL,3.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('c2453259-5690-527c-9216-7df2cff2dd13','787b711f-b262-51e7-9255-2ceedc765e25','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/c2453259-5690-527c-9216-7df2cff2dd13','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('6a81fa42-0701-50b6-8bb0-ef05eb4633d8','c2453259-5690-527c-9216-7df2cff2dd13','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2548b59c-63e8-5414-8085-5289f22ada80','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','青椒炒肉',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('737e0f80-193f-5300-a166-62e1529672cc','2548b59c-63e8-5414-8085-5289f22ada80','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/737e0f80-193f-5300-a166-62e1529672cc','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('31d0a987-9e93-5b9b-b875-a528c95296a9','737e0f80-193f-5300-a166-62e1529672cc','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5d11aaa5-814e-55f8-a389-26e2d16e5e63','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','青椒炒肚丝',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('e7600eaf-7eb1-57ef-9a8a-6711e0944a65','5d11aaa5-814e-55f8-a389-26e2d16e5e63','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/e7600eaf-7eb1-57ef-9a8a-6711e0944a65','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('8e8adeb7-5840-5a55-8d20-935ba41dea6e','e7600eaf-7eb1-57ef-9a8a-6711e0944a65','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c4e9d787-3241-5f35-ac55-d23e05f12f1c','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','凉拌黄瓜虾仁',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('bb831627-3754-5bc1-ac31-b9cd15828380','c4e9d787-3241-5f35-ac55-d23e05f12f1c','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/bb831627-3754-5bc1-ac31-b9cd15828380','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('852103cd-5efb-576a-b540-3f00d9ff5e85','bb831627-3754-5bc1-ac31-b9cd15828380','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('646c677e-cc79-5389-ba23-174410b9e7d1','a7dd62f7-868e-5afe-a249-a23e65a2fa5d','卤猪肉',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('5d1d9233-780d-5dcb-b6b7-107ca63cb703','646c677e-cc79-5389-ba23-174410b9e7d1','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/5d1d9233-780d-5dcb-b6b7-107ca63cb703','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('381e3a09-29ce-55c4-a729-b01d4873f09e','5d1d9233-780d-5dcb-b6b7-107ca63cb703','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dining_halls(id,name,description,status,sort_order)
VALUES ('74ed9519-f3e3-5ce8-a89c-a353e2892edd','明湖食堂',NULL,'active',1)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','74ed9519-f3e3-5ce8-a89c-a353e2892edd','窗口一',NULL,'/api/v1/media/a7a219c9-63f4-5915-82f4-24f5cfbd4ec5','active',0)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a0fc2f12-cd68-5f9a-ba50-2e03bad16dce','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','麻婆豆腐',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b030fe2d-2175-5af5-88d1-badc2ef2ed25','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','清炒小白菜',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1cccbcd3-ab5f-5d88-8ee7-767234061b1a','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','清炒包菜',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f591fc22-4774-53c8-89e5-e8b30d3b3f4b','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','干煸四季豆',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b76fc0fc-7546-59e2-b0c1-b1bc8f2f703d','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','炸素鸡',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('65040f8e-4eb4-5b0e-8418-a0e3bd3b52d2','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','青椒炒肉',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aab7c605-2c35-57f6-8b32-2fe0378e122b','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','宫保鸡丁',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('465563e7-fd85-561e-85b2-3ecf60346ef9','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','红烧鸡块',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bff3ef4b-f740-5a15-bbb2-f596aab53a29','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','回锅肉',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('219e7238-2a02-5f37-9fa9-1e7b556ce73d','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','土豆炖鸡块',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('04aab291-f80b-5b28-b115-605b85f0e8db','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','红烧狮子头',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a9df6c21-3453-5b77-ad4c-008bfa51820a','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','鸡排',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ea673a98-3af3-556f-8f6a-f6ca12902a5f','e84728c6-2c35-5eeb-8db7-cb4aaa13cdf5','鸭腿',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('8050e9be-a332-54f2-8175-924a145bd5de','74ed9519-f3e3-5ce8-a89c-a353e2892edd','窗口二',NULL,'/api/v1/media/ce881071-6bc4-552c-a8b9-31bdf56f90ac','active',1)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c67a25e7-39df-503c-a515-e58c8bcc9164','8050e9be-a332-54f2-8175-924a145bd5de','奥尔良鸡腿肉拌饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4d0cab16-3989-5137-a81f-d36c46c0c7a6','8050e9be-a332-54f2-8175-924a145bd5de','咖喱鸡腿肉拌饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10bff144-2f2a-5559-b0a9-2e5daaee7706','8050e9be-a332-54f2-8175-924a145bd5de','蜜汁照烧鸡拌饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bdf0704a-0b82-5597-be8f-73dedd26a6b6','8050e9be-a332-54f2-8175-924a145bd5de','香菇小炖肉拌饭',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d532827d-e749-5730-b168-b2728f1e321c','8050e9be-a332-54f2-8175-924a145bd5de','香辣鸡胗拌饭',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f2f91c44-04f7-5e8e-aa8e-682c5bd862de','8050e9be-a332-54f2-8175-924a145bd5de','小炒肉拌饭',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2d3f22f1-7ed2-50fa-8d52-ff72a5a3e7fe','8050e9be-a332-54f2-8175-924a145bd5de','黑椒牛肉拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b7dd6bf8-520f-5a57-9c7d-994511a2e48f','8050e9be-a332-54f2-8175-924a145bd5de','孜然羊肉拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('2a34dab2-2794-5930-9c6c-aa219510f21a','74ed9519-f3e3-5ce8-a89c-a353e2892edd','窗口三',NULL,'/api/v1/media/ce28e50b-bc45-5aef-83da-b8447e50af90','active',2)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('715c944c-dbaf-5dc3-bf27-45f339e36533','2a34dab2-2794-5930-9c6c-aa219510f21a','西红柿鸡蛋面',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e32c3947-bbd5-5ed9-bbbc-dd7183b59b5f','2a34dab2-2794-5930-9c6c-aa219510f21a','辣椒卤肉面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f6d30ef0-d939-5549-a52e-03008cac23e0','2a34dab2-2794-5930-9c6c-aa219510f21a','肉酱面',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('3b3ccc37-b471-5cf6-9965-57d67c96dc2c','74ed9519-f3e3-5ce8-a89c-a353e2892edd','川渝麻辣香锅·麻辣烫·麻辣拌',NULL,'/api/v1/media/c65bf743-8793-54fd-a5e2-cfbf60b7f51c','active',3)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1848a70a-d7c4-5a9e-b95e-5043cb7458f5','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','麻辣香锅/麻辣烫/麻辣拌','计价单位：50',2.18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('18fe231f-f9d3-5eb4-9b04-7b2274c4fd59','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴肥牛',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8855a1b7-d8d3-5e34-97d3-903b3844a91e','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴方便面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a8f6c3f2-46d3-5d88-bf5a-818918887d34','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴小酥肉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('27744ae1-1344-5144-a062-0ab45fecf0bf','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴年糕',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b330dabc-efcf-5ca6-b354-cb84565186c1','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴鱼',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('044ac704-1117-5a81-a394-a98a7cae4784','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴培根',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('468474d6-f757-5a91-86c1-c05cd424ffef','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴鸡排',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f48c3e2c-69c5-5840-b6c0-aa7795648ce5','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴脆骨肠',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe587edf-278d-556e-bca2-c051eb38bb6a','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','馋嘴鱼豆腐',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9dd17caf-3100-5778-804d-6e5feb3dde2c','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','奥尔良鸡肉炒方便面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dff854b5-fd18-5062-9e35-8e6d3ea6f900','3b3ccc37-b471-5cf6-9965-57d67c96dc2c','火腿炒方便面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','74ed9519-f3e3-5ce8-a89c-a353e2892edd','轻食燃脂·简餐',NULL,'/api/v1/media/bd38915f-caf7-5990-a61e-fdf249c84230','active',4)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('35caa202-d72e-5960-992a-9846dcbd241a','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','炙烤牛排',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2bc55e2d-9044-5281-b4e4-f0c035f75d30','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','香烤鸡胸肉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f92ebdf-bf46-5e9b-ac25-78e0a859a108','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','炙烤奥尔良鸡腿肉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3a394098-c62a-5e06-918f-9ce18228a7e8','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','藤椒鸡胸肉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f2fd543b-2f2b-5114-9053-9a1b3f6c3c68','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','香煎鲷鱼排',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8ffcfbb3-e75b-559a-b279-19aac13ae7bb','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','黑椒猪排',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d8a565c-1ab9-552e-9e88-dc5eb0fc38d0','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','法式烤排',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1702b1fd-e15e-528b-bf6d-d5227f3bfd44','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','盐田大虾',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fc578fda-609a-57d3-8eb9-d3df496948ac','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','土豆泥沙拉',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('37acf5c8-b5d1-565b-a09e-9b7712f6f633','5f1e0639-c8aa-5f9d-9313-6e81902f3a2d','素食主义',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('ecd90879-9ae8-56fa-8d11-b38b77ffdf80','74ed9519-f3e3-5ce8-a89c-a353e2892edd','炉知府炙烤五花肉',NULL,'/api/v1/media/05bb6d44-dd81-5fbe-8fab-1d231f21bc5f','active',5)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4cac2461-594f-52f7-9fa6-3ba4d4208d3f','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','招牌原味炙烤五花肉饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('8fc64ed1-460f-5d78-9275-4d339bbd359c','4cac2461-594f-52f7-9fa6-3ba4d4208d3f','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/8fc64ed1-460f-5d78-9275-4d339bbd359c','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('5ec5bc9c-a44f-54fc-8938-b7002189775b','8fc64ed1-460f-5d78-9275-4d339bbd359c','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e31329da-9e78-55c5-b494-1f5aecf4d513','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','五花肉煎酸菜',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('bafc364d-cedd-5dce-bb42-ac3bfe96d8fd','e31329da-9e78-55c5-b494-1f5aecf4d513','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/bafc364d-cedd-5dce-bb42-ac3bfe96d8fd','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('8551f497-bd5c-50ff-999e-1e202f8f007d','bafc364d-cedd-5dce-bb42-ac3bfe96d8fd','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bafc861a-74a1-57dc-9f45-967bc98256b3','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','五花肉香辣包菜',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('21eed176-5f1a-5979-a9a4-82cc4f99e716','bafc861a-74a1-57dc-9f45-967bc98256b3','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/21eed176-5f1a-5979-a9a4-82cc4f99e716','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('c528bf20-a15a-5e16-acb7-69ecd8b37947','21eed176-5f1a-5979-a9a4-82cc4f99e716','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bcf87574-684e-54e2-9855-ae725d9f7249','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','五花肉娃娃菜',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('bf5a8267-1f7f-5d2b-83fe-0d741905a7e7','bcf87574-684e-54e2-9855-ae725d9f7249','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/bf5a8267-1f7f-5d2b-83fe-0d741905a7e7','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('6ed5092a-d1c2-5441-ac84-757a1e5b2e17','bf5a8267-1f7f-5d2b-83fe-0d741905a7e7','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('79798df5-5452-57ab-bc51-e2f3ea4d20d2','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','五花肉金针菇',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('13fb3e0e-99a0-594e-8e27-d545be7545da','79798df5-5452-57ab-bc51-e2f3ea4d20d2','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/13fb3e0e-99a0-594e-8e27-d545be7545da','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('af638418-4ea2-53dc-97b9-c3da3a323620','13fb3e0e-99a0-594e-8e27-d545be7545da','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a227ff4a-af50-5398-955e-73d1b8b854c0','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','韩式烤排饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason)
VALUES ('3638cd3e-7a16-50b3-a471-9346f80bc200','a227ff4a-af50-5398-955e-73d1b8b854c0','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','/api/v1/media/3638cd3e-7a16-50b3-a471-9346f80bc200','approved','39d46f72-3ea4-57b9-a25f-8d3a6779e84d',now(),'种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.content_moderation_logs(id,dish_image_id,moderator_user_id,action,reason)
VALUES ('02ae6674-374e-5cfa-9951-ca359f14ff5b','3638cd3e-7a16-50b3-a471-9346f80bc200','39d46f72-3ea4-57b9-a25f-8d3a6779e84d','approve','种子菜单导入')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('759eb706-b311-518a-af1b-00ffee5309b8','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','炙烤肥牛饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7d4f4f85-94ba-54ed-af63-6091f4d9918a','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','炙烤牛肉饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d5da8fc-f925-53e1-acfc-42f51b50b81a','ecd90879-9ae8-56fa-8d11-b38b77ffdf80','炙烤梅花肉饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('79463bc9-0ac7-5e8e-a3b3-ca89e1763543','74ed9519-f3e3-5ce8-a89c-a353e2892edd','西北风味拉面刀削面',NULL,'/api/v1/media/c6a94010-41fc-5b40-a2e3-2b4217c282b9','active',6)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('afde6837-55c5-50cd-bbca-06e3deb70616','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','酸菜肉片小面','鱼豆腐小面',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('06c1e68f-41c3-5da7-a4ee-e10715aa7357','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','花生肉片小面','鱼豆腐小面',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f980bef2-6cdb-50f8-90e6-9dc2c64f612c','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','麻辣鸡块小面','鱼豆腐小面',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dcf1ba98-b4fb-5c27-b1e8-70072647e313','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','麻辣肥牛小面','鱼豆腐小面',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7cfa1200-3a5c-5d96-a411-0997c8e6c1c5','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','红烧牛肉小面','鱼豆腐小面',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5ae789a-38a8-51f2-a931-d66036f7ed11','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','芽菜肉片小面','鱼豆腐小面',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0a9922a8-fabe-5dab-b85e-cd474267860e','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','兰州拉面','鱼豆腐小面',10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('52149ed4-a43e-5c2e-8571-2db4a14a7045','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','兰州拉面加肉','鱼豆腐小面',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a09812a8-811e-5ef7-ab75-ef5eb407cecc','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','传统猪肉刀削面','鱼豆腐小面',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a68c9796-6cc6-5c76-b6b3-d0c1fdd399d0','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','红烧牛肉刀削面','鱼豆腐小面',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('48e89a2b-c85e-5f8a-bca3-5b09aea146c1','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','酸菜牛肉刀削面','鱼豆腐小面',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2c78ad02-29fe-563a-827e-5b6f5ce1f947','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','肉片茄丁刀削面','鱼豆腐小面',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9fd4526-5b6c-5e74-818e-f0fa7ee01d6a','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','西红柿鸡蛋刀削面','鱼豆腐小面',10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d9ef7a6-c7f4-584e-9821-42999173a00a','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','油泼面','鱼豆腐小面',10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ebd42e7e-0726-5951-8af0-8c17a5235985','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','炸酱面','鱼豆腐小面',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c2b5d11f-47e4-5070-821e-5111630208f3','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','秘制干拌面（麻辣味）','鱼豆腐小面',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cd569839-9de5-5997-82d8-da6559f80b90','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','肉臊子干拌面','鱼豆腐小面',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cd781a92-0e68-5360-9972-3111c3dc33d5','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','二合一干拌面（西红柿+肉臊子）','鱼豆腐小面',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8ab8421a-2054-504f-90bb-b07f65f13a01','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','西红柿鸡蛋拌面','鱼豆腐小面',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5b20d8c-cda3-5392-a9ff-a59e6a94b4cf','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','大盘鸡拌面','鱼豆腐小面',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('69813095-1df3-54f8-8af5-45f27b163509','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','西红柿鸡蛋炒面','鱼豆腐小面',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6ece12ed-c739-536f-a87f-04d3b9262436','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','肉炒面','鱼豆腐小面',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b6ed4aee-8efb-5f46-9204-db7ca37f74f3','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','牛肉炒面','鱼豆腐小面',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2564139c-1335-5192-b000-e101d8782e82','79463bc9-0ac7-5e8e-a3b3-ca89e1763543','麻酱拌面','鱼豆腐小面',10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('35e29836-a3aa-521f-840a-472fcdbcc6eb','74ed9519-f3e3-5ce8-a89c-a353e2892edd','分米鸡·费大叔辣椒炒肉',NULL,'/api/v1/media/5b108ce1-ccbd-5460-bbce-99d27c3b53fa','active',7)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e5a2bbe-3806-5a42-9bea-5b205ba6b805','35e29836-a3aa-521f-840a-472fcdbcc6eb','香肉拌饭系列香辣烤肉拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b43aff59-bc49-5644-a273-fed15e88b145','35e29836-a3aa-521f-840a-472fcdbcc6eb','孜然烤肉拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26e8633e-a024-5431-b5f4-a433c982dadf','35e29836-a3aa-521f-840a-472fcdbcc6eb','奥尔良鸡丁拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fea48d2e-9043-5ceb-975c-27b001b9ff76','35e29836-a3aa-521f-840a-472fcdbcc6eb','香辣双拼拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b6d15160-ee52-5e54-82f1-8d1ca0b966a3','35e29836-a3aa-521f-840a-472fcdbcc6eb','黑椒五花肉拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c3343b93-8cda-57dd-bfa1-db9ff3ea1b7f','35e29836-a3aa-521f-840a-472fcdbcc6eb','费大叔辣椒炒肉系列招牌小炒肉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('27277b29-83a9-5cf2-8522-ddbccfe9ba68','35e29836-a3aa-521f-840a-472fcdbcc6eb','麻辣回锅肉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7cae8a4b-474c-5ff9-b2c0-247a8911d4b2','35e29836-a3aa-521f-840a-472fcdbcc6eb','孜然肉片（鸭胸肉）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f1403020-8315-5f7e-8278-25ca38ee6a8b','35e29836-a3aa-521f-840a-472fcdbcc6eb','辣椒炒肉（鸭胸肉）',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8767c435-f261-5543-b2e5-c9b52facb954','35e29836-a3aa-521f-840a-472fcdbcc6eb','咖喱肉片',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80d97a71-6ca9-5546-a9a5-de90305555c3','35e29836-a3aa-521f-840a-472fcdbcc6eb','铁板系列铁板黑椒牛柳',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f5cc8399-ff3d-591d-ab35-da23f5779c98','35e29836-a3aa-521f-840a-472fcdbcc6eb','铁板豆角牛柳',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e1f083c7-8a36-5409-a609-545579f9adc8','35e29836-a3aa-521f-840a-472fcdbcc6eb','铁板香辣鸡',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f39d5f45-db15-5f87-bca7-ae88cb564c1f','35e29836-a3aa-521f-840a-472fcdbcc6eb','铁板咖喱鸡',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('60e56222-b449-5b79-a773-c8d34aa43ae9','35e29836-a3aa-521f-840a-472fcdbcc6eb','铁板红烧鸡块',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7fb2e43-9f8a-547d-b301-0dc5f77e80a1','35e29836-a3aa-521f-840a-472fcdbcc6eb','分米鸡系列香辣分米鸡',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5ed6b06f-3c8b-50cd-bd79-0ca1db248f33','35e29836-a3aa-521f-840a-472fcdbcc6eb','酱香分米鸡',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('23a42631-cf6c-5f01-84d5-7865250d7c71','74ed9519-f3e3-5ce8-a89c-a353e2892edd','乡厨炒鸡米饭',NULL,'/api/v1/media/f5994309-adcf-544b-b55d-e5711df09dbb','active',8)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('71eb42d4-3e15-5d88-bafe-d15721a395e2','23a42631-cf6c-5f01-84d5-7865250d7c71','炒鸡米饭系列鲜香炒鸡',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ecf0502a-6527-5d71-bdd1-ac746ae2f5e7','23a42631-cf6c-5f01-84d5-7865250d7c71','椒麻炒鸡',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26866a22-4cec-5ab5-906a-2190786a4261','23a42631-cf6c-5f01-84d5-7865250d7c71','香辣炒鸡',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5e32d8fa-9a81-533f-9192-b9531d958f7d','23a42631-cf6c-5f01-84d5-7865250d7c71','菌菇炒鸡',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a29aea17-0503-5dd2-8391-766ae3c1a6d5','23a42631-cf6c-5f01-84d5-7865250d7c71','酸菜炒鸡',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d452d301-d72c-5958-9620-bcd1a9d56e85','23a42631-cf6c-5f01-84d5-7865250d7c71','番茄炒鸡',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('616756ea-1cbf-51f8-bd04-f7d884e4b820','23a42631-cf6c-5f01-84d5-7865250d7c71','青花椒炒鸡',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('76c6c865-899f-51ce-a218-dd5d3019e6d2','23a42631-cf6c-5f01-84d5-7865250d7c71','滑蛋饭&特色推荐系列火腿滑蛋饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('12520d96-8127-5745-9a7a-d4466f8e5848','23a42631-cf6c-5f01-84d5-7865250d7c71','培根滑蛋饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('62353c8e-2ece-5645-b169-62da65db5c8d','23a42631-cf6c-5f01-84d5-7865250d7c71','五花肉滑蛋饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b9744919-eced-5a28-a2f9-2b77536b9491','23a42631-cf6c-5f01-84d5-7865250d7c71','全家福滑蛋饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f0b5ecf-22db-57cd-a543-3bbba3f62810','23a42631-cf6c-5f01-84d5-7865250d7c71','阳光肥牛滑蛋饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a160c2e-2ec5-506b-8036-4264c76d910e','23a42631-cf6c-5f01-84d5-7865250d7c71','招牌鸡肉滑蛋饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('048d1284-88b2-5a2d-b2f1-218a5205e231','23a42631-cf6c-5f01-84d5-7865250d7c71','奥尔良鸡丁滑蛋饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('60a4d085-c482-571d-ba4a-41c5e8328fbf','23a42631-cf6c-5f01-84d5-7865250d7c71','鸭头+鸭翅+卤素菜+米饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('278c5064-fafe-53b3-8954-9ce8ce924e8f','23a42631-cf6c-5f01-84d5-7865250d7c71','鸭翅+鸭锁骨+卤素菜+米饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('08e9d12d-fdad-534d-8912-b1eb64cb52eb','23a42631-cf6c-5f01-84d5-7865250d7c71','鸭头+鸭脖+卤素菜+米饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c92c6f6e-6ec5-52a9-9afe-8281ba54cde2','23a42631-cf6c-5f01-84d5-7865250d7c71','鸭锁骨+鸭翅+鸭脖+卤素菜+米饭',NULL,19,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('68f46eaf-992f-5b56-82d5-57369f3db92e','74ed9519-f3e3-5ce8-a89c-a353e2892edd','云南菌菇炒饭·烤鱼',NULL,'/api/v1/media/f17a9274-db8d-5628-ac9d-d896ad190764','active',9)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5afd12de-99af-5e12-9b06-a7124b3a4534','68f46eaf-992f-5b56-82d5-57369f3db92e','铁板炒饭炒粉系列火腿鸡蛋炒饭',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('baf0f396-6509-5dce-a565-bd4551ffa72e','68f46eaf-992f-5b56-82d5-57369f3db92e','广式腊肠炒饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('455d1554-1997-579c-b92e-912d4e0c7911','68f46eaf-992f-5b56-82d5-57369f3db92e','辣白菜五花肉炒饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5a9c6769-83b8-5673-9bc1-d5d4fd790aa5','68f46eaf-992f-5b56-82d5-57369f3db92e','辣白菜牛肉炒饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dc0fc627-b31e-5656-8f07-ceb6ea5d7973','68f46eaf-992f-5b56-82d5-57369f3db92e','橄榄菜火腿炒饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('60493ec2-e86e-5cd3-a684-bbf27e1377f2','68f46eaf-992f-5b56-82d5-57369f3db92e','老干妈五花肉炒饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f6d4e8bd-244a-54c9-8f65-519c72d82f3c','68f46eaf-992f-5b56-82d5-57369f3db92e','奥尔良鸡肉炒饭',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('40208d5e-344e-55e4-b703-154827ab837c','68f46eaf-992f-5b56-82d5-57369f3db92e','酸辣河粉',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ed7151a0-5ad2-5cfb-ae22-76f47d6a4bbe','68f46eaf-992f-5b56-82d5-57369f3db92e','干炒牛河',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('78adfcf5-fd9b-50a9-b460-4607df454d29','68f46eaf-992f-5b56-82d5-57369f3db92e','鸡蛋火腿炒河粉',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('85297275-dce9-5ee1-bf63-b97e9931fbe8','68f46eaf-992f-5b56-82d5-57369f3db92e','菌菇炒饭系列香辣五花肉菌菇炒饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5bacd1a0-78b4-50f6-a6c6-169076bcf2eb','68f46eaf-992f-5b56-82d5-57369f3db92e','五花肉菌菇炒饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b66ad08-a983-5ab8-896a-cc0da09af2f3','68f46eaf-992f-5b56-82d5-57369f3db92e','牛肉菌菇炒饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a3552a77-6c46-565d-ba7e-0dd8ba100cfc','68f46eaf-992f-5b56-82d5-57369f3db92e','香辣牛肉菌菇炒饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('de685865-bdca-5d0d-817a-92c21f9a48b7','68f46eaf-992f-5b56-82d5-57369f3db92e','虾仁菠萝炒饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('86e3cb66-b951-5b0c-870f-95e042f8bd3b','68f46eaf-992f-5b56-82d5-57369f3db92e','鸡蛋火腿炒米线',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f998a1e9-b958-5146-8d44-26835171c706','68f46eaf-992f-5b56-82d5-57369f3db92e','五花肉炒米线',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6522f328-18bb-5af7-9ea4-f8da28c16d89','68f46eaf-992f-5b56-82d5-57369f3db92e','扬州炒米线',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fa82e9a2-72eb-50dc-8cab-36e1352a7e13','68f46eaf-992f-5b56-82d5-57369f3db92e','扬州炒饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('471ce6b3-1aa5-5e0c-bb71-d0d9bab54f76','68f46eaf-992f-5b56-82d5-57369f3db92e','烤鱼饭系列豆花烤鱼饭',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b96d9a74-04d4-5441-849c-96e8a369a50e','68f46eaf-992f-5b56-82d5-57369f3db92e','煎蛋烤鱼饭',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fabec0d5-ee80-5d75-898b-e97940422d11','68f46eaf-992f-5b56-82d5-57369f3db92e','年糕烤鱼饭',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8e484e9b-2cce-5dc9-bc12-974250b4b446','68f46eaf-992f-5b56-82d5-57369f3db92e','土豆粉烤鱼饭',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('54cd77ef-f210-5c16-8876-1164f9d04635','68f46eaf-992f-5b56-82d5-57369f3db92e','泡面烤鱼饭',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10391b61-f525-5e67-896f-b8bf4042ac88','68f46eaf-992f-5b56-82d5-57369f3db92e','炸蛋烤鱼饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f16c0d4-0c7e-5be4-937e-b8ac461e1c8a','68f46eaf-992f-5b56-82d5-57369f3db92e','酥肉烤鱼饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('63fbd23e-0059-51ed-90b0-f9b187fadbe4','68f46eaf-992f-5b56-82d5-57369f3db92e','鱼豆腐烤鱼饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f23b3509-3be1-5e99-966d-28a29ee83bc6','68f46eaf-992f-5b56-82d5-57369f3db92e','酥肉小锅饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('be75ef88-cb92-55c1-afac-289779fea891','74ed9519-f3e3-5ce8-a89c-a353e2892edd','烫三爷云南鲜烫吊龙米线',NULL,'/api/v1/media/a94cad17-0ae5-5074-a04e-3c55cdb79aeb','active',10)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9860a418-7fe2-5781-9272-ef336744cac5','be75ef88-cb92-55c1-afac-289779fea891','吊龙米线系列招牌鲜烫吊龙米线',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('49109002-e3d7-59f8-ab4f-300a3eb9426b','be75ef88-cb92-55c1-afac-289779fea891','云南炸酱米线',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d680076d-f686-584c-802d-4a106a653f87','be75ef88-cb92-55c1-afac-289779fea891','鲜烫梅花肉米线',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('159712f0-4dc6-5e2d-98cc-0208dc4aff2e','be75ef88-cb92-55c1-afac-289779fea891','经典双拼米线（牛肉+炸酱）',NULL,21,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b1f0d64-75aa-5584-9125-351229ba6fa8','be75ef88-cb92-55c1-afac-289779fea891','牛肉丸米线',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4ccd4ff8-3c77-5ecd-a698-45ce7ab38392','be75ef88-cb92-55c1-afac-289779fea891','泡椒鲜烫吊龙米线',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('728d703d-bfa1-5be4-8339-bd68e6559f70','be75ef88-cb92-55c1-afac-289779fea891','香辣卤牛肉米线',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('072adb2a-21a5-50e5-943b-1a364b21a842','be75ef88-cb92-55c1-afac-289779fea891','港式卤肉饭系列港式腊肠',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5761155b-917e-5af9-aff6-4dfef6557ded','be75ef88-cb92-55c1-afac-289779fea891','港式小酥肉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6501b146-a497-5800-9318-6d57edde59b4','be75ef88-cb92-55c1-afac-289779fea891','港式半只鸡',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fdbe0ca1-3727-50f0-b123-a0fac8688568','be75ef88-cb92-55c1-afac-289779fea891','港式把子肉',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('523db0f8-c271-5101-8b63-a34f8527e0e3','be75ef88-cb92-55c1-afac-289779fea891','梅菜烧肉饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('93809ada-2feb-5aef-8d7d-1f5d13d67f7f','be75ef88-cb92-55c1-afac-289779fea891','红酒麻辣鸭腿',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('3d7e9c06-c56b-545c-9169-a1da56faf49b','74ed9519-f3e3-5ce8-a89c-a353e2892edd','玖灵猫土豆泥拌饭',NULL,'/api/v1/media/be718dc0-8805-5df4-868f-ceea5ea2ca73','active',11)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('467b7499-3093-5ed1-94f7-f2d75ade2ec1','3d7e9c06-c56b-545c-9169-a1da56faf49b','店长推荐招牌绝味鸡',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('61972f83-e5c2-57df-b859-6100de9d2da8','3d7e9c06-c56b-545c-9169-a1da56faf49b','港式卤肉饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f899522a-8015-5cd8-80c3-935472e70a63','3d7e9c06-c56b-545c-9169-a1da56faf49b','霸王烤鸭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5456c854-f879-57a5-a1fd-c29d758f091e','3d7e9c06-c56b-545c-9169-a1da56faf49b','香辣鸭腿',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1903f38b-96f6-5cb9-8772-a605087c2245','3d7e9c06-c56b-545c-9169-a1da56faf49b','新品上市椒麻口水鸡',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('97e5cd45-7ed4-5e3f-91f5-29d3b847f9fa','3d7e9c06-c56b-545c-9169-a1da56faf49b','鲍汁鸡',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5656b063-313f-5c4d-bede-e1c6ed934ba5','3d7e9c06-c56b-545c-9169-a1da56faf49b','秘制系列可乐鸡翅根',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6db53ceb-4e99-541f-bcce-db23bc129b38','3d7e9c06-c56b-545c-9169-a1da56faf49b','泰式咖喱鸡',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5b480d02-a56b-578a-80a1-6e4224630b5d','3d7e9c06-c56b-545c-9169-a1da56faf49b','双椒鸡',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d6232b3-5063-5115-9fbd-baae0ea6fc87','3d7e9c06-c56b-545c-9169-a1da56faf49b','卤肉系列武汉黑鸭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d07e3aa3-d27e-5996-9792-5eba4855a21a','3d7e9c06-c56b-545c-9169-a1da56faf49b','蟹柳茄子',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b6eb63d1-d402-5ca4-83cf-fa0a13130ddd','3d7e9c06-c56b-545c-9169-a1da56faf49b','手撕鸡',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d7e01b0b-a0fa-5844-8f3b-d11a66e96613','3d7e9c06-c56b-545c-9169-a1da56faf49b','香汁肥牛',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('46f65289-1ecc-510c-947e-2348ea06cbf9','3d7e9c06-c56b-545c-9169-a1da56faf49b','任意双拼',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('52a74752-c680-5c39-8964-5c1daa1fa4a8','74ed9519-f3e3-5ce8-a89c-a353e2892edd','烤鸭米粉烧腊',NULL,'/api/v1/media/b32d014a-1244-52c4-af37-a92396d8e3c2','active',12)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('06d3b2b6-da9d-5b84-99a7-981968efc418','52a74752-c680-5c39-8964-5c1daa1fa4a8','臻味冰丝面鱼系列招牌炙烤鸭腿面鱼',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('862272b5-4b0a-5169-85e7-e06ff90077ff','52a74752-c680-5c39-8964-5c1daa1fa4a8','红油鸡丝面鱼',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ae06bab5-f6dc-5757-9b67-be6272aa6b52','52a74752-c680-5c39-8964-5c1daa1fa4a8','酸辣笋尖面鱼',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6053e489-1162-513a-a0b2-07c8b3a12bff','52a74752-c680-5c39-8964-5c1daa1fa4a8','蔬香素玉面鱼',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fac88763-581d-510c-b032-74758868845d','52a74752-c680-5c39-8964-5c1daa1fa4a8','经典双拼系列炙烤鸭腿火鸡面鱼',NULL,19,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c99b617b-ad66-56e6-829c-4cd354ec38c4','52a74752-c680-5c39-8964-5c1daa1fa4a8','红油鸡丝火鸡面鱼',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ed2b991b-d85d-555b-92e8-2865326623ed','52a74752-c680-5c39-8964-5c1daa1fa4a8','酸辣笋尖火鸡面鱼',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('926a4822-52be-5cdb-86a7-8e1446eace11','52a74752-c680-5c39-8964-5c1daa1fa4a8','蔬香素玉火鸡面鱼',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('885e4214-99df-5115-8bdc-107e21c8f63c','52a74752-c680-5c39-8964-5c1daa1fa4a8','芝香火鸡面系列椒麻芝香火鸡面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5fbf615a-2614-5f99-b2c2-a597188d4f7f','52a74752-c680-5c39-8964-5c1daa1fa4a8','绯香韩式火鸡面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a8ac80bc-c1ad-50c7-bcde-11d7e3906ed5','52a74752-c680-5c39-8964-5c1daa1fa4a8','麻酱油芝火鸡面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('554ec50e-b5c0-5216-9eb9-a537a7c3a6d5','52a74752-c680-5c39-8964-5c1daa1fa4a8','鲜味蟹黄火鸡面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('634b00e3-6b15-5cae-962d-57aec142a727','52a74752-c680-5c39-8964-5c1daa1fa4a8','湖南米粉/河粉系列麻辣牛肉拌粉/汤粉',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('583b2601-fefc-5bc3-bb5d-64ff36fcd088','52a74752-c680-5c39-8964-5c1daa1fa4a8','红烧牛肉拌粉/汤粉',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('706e499b-3428-5158-a496-fc8beeeea135','52a74752-c680-5c39-8964-5c1daa1fa4a8','香烤鸭腿拌粉/汤粉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('702c84cf-564f-5772-8e10-f5e24d65cb48','52a74752-c680-5c39-8964-5c1daa1fa4a8','笋尖拌粉/汤粉',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('99521366-dc00-5502-b826-64d3c2ee8fd2','52a74752-c680-5c39-8964-5c1daa1fa4a8','椒麻鸡拌粉/汤粉',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3520f217-2ae4-5b97-8d29-1f07b1b8b522','52a74752-c680-5c39-8964-5c1daa1fa4a8','时蔬拌粉/汤粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9beda2d8-bf96-599c-81e5-32bbe194bcb0','52a74752-c680-5c39-8964-5c1daa1fa4a8','明湖烤鸭（半只）',NULL,23.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('80fb46c5-3327-5df0-98c3-51982f44b4ad','74ed9519-f3e3-5ce8-a89c-a353e2892edd','牛肉粉丝汤·老母鸡粉丝汤',NULL,'/api/v1/media/3fb5004a-1d6c-531b-8be3-da8116527f6f','active',13)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('91a46cf3-7486-5901-b842-b64ae7a94e83','80fb46c5-3327-5df0-98c3-51982f44b4ad','素炒（米粉/河粉）','新疆炒米粉',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26a29775-0d7f-569c-9b4e-f1d24896a078','80fb46c5-3327-5df0-98c3-51982f44b4ad','鱼豆腐炒（米粉/河粉）','新疆炒米粉',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3263e017-6d2f-5580-9663-bd0f39994bc0','80fb46c5-3327-5df0-98c3-51982f44b4ad','火腿炒（米粉/河粉）','新疆炒米粉',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a22dd694-8fad-589a-9f6c-bfc95b7d8183','80fb46c5-3327-5df0-98c3-51982f44b4ad','鸡肉炒（米粉/河粉）','新疆炒米粉',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('38c1c392-12f2-5814-86ad-70551740da33','80fb46c5-3327-5df0-98c3-51982f44b4ad','牛肉炒（米粉/河粉）','新疆炒米粉',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b05c276-2828-578a-88e2-d6abc315577b','80fb46c5-3327-5df0-98c3-51982f44b4ad','牛肉汤','牛肉汤',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('af8764ef-0003-587a-a41a-f1011686d777','80fb46c5-3327-5df0-98c3-51982f44b4ad','剔骨牛肉汤','牛肉汤',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ca10fa02-4fd0-514f-9eff-cf8c5f527fb5','80fb46c5-3327-5df0-98c3-51982f44b4ad','母鸡汤','牛肉汤',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2a36b13d-c08a-5434-85b6-44bfb1c681f3','80fb46c5-3327-5df0-98c3-51982f44b4ad','牛肉汤河粉','牛肉汤',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c4c3040d-da5b-5ff7-8aaf-cec830b0bec9','80fb46c5-3327-5df0-98c3-51982f44b4ad','剔骨牛肉河粉','牛肉汤',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e549b024-5aa5-58c3-9559-e08b344a51f2','80fb46c5-3327-5df0-98c3-51982f44b4ad','母鸡汤河粉','牛肉汤',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ff3103ed-15cc-5ad3-9834-4a7953b587c2','80fb46c5-3327-5df0-98c3-51982f44b4ad','单加熟牛肉片','牛肉汤；计价单位：30',6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bdb35758-a039-54e0-b75f-1789918f5cae','80fb46c5-3327-5df0-98c3-51982f44b4ad','鹌鹑蛋','牛肉汤；计价单位：3',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e46bbb8-1ffe-522c-a884-30b48490af62','80fb46c5-3327-5df0-98c3-51982f44b4ad','特色葱油饼','牛肉汤',1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('226ad953-3d2c-55a9-ad69-3ce8cef850cc','74ed9519-f3e3-5ce8-a89c-a353e2892edd','炙香板烧饭',NULL,NULL,'active',14)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fc86e0ee-5db8-5080-882a-9554353e0bb9','226ad953-3d2c-55a9-ad69-3ce8cef850cc','麻香焖鸭板烧饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('18f844f3-64fd-5251-b7ca-a95810db78c4','226ad953-3d2c-55a9-ad69-3ce8cef850cc','咸香焖五花板烧饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('da923a3f-b597-5ef3-9aec-e5f842bc02b1','226ad953-3d2c-55a9-ad69-3ce8cef850cc','鲜肉铁板烧',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3ca5bbb9-ae1e-5a6c-92c8-d69846466b6a','226ad953-3d2c-55a9-ad69-3ce8cef850cc','黑椒肉柳铁板烧',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('33492c4a-2354-58a7-9108-c204c3403300','226ad953-3d2c-55a9-ad69-3ce8cef850cc','蟹香焖鸭板烧饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4ab6c77a-5bac-52ab-ac2c-a6aa7e29e337','226ad953-3d2c-55a9-ad69-3ce8cef850cc','香酥鸡板烧饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a56722d3-5385-5068-9c28-9a61196f0787','226ad953-3d2c-55a9-ad69-3ce8cef850cc','虾香鸡板烧饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6fccfd01-87d0-55a2-a243-61f2236b9892','226ad953-3d2c-55a9-ad69-3ce8cef850cc','酱焖精肉板烧饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('d7eec598-888f-5c72-b64e-9a8d3316f275','74ed9519-f3e3-5ce8-a89c-a353e2892edd','M小饿餐·猪排饭',NULL,'/api/v1/media/bd46f2e4-fafa-50a7-b8ea-bf69e8b4b435','active',15)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8a4b8766-7351-5cb3-b074-4b2002079736','d7eec598-888f-5c72-b64e-9a8d3316f275','咖喱厚切猪排饭','招牌/主食',16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6749ca90-5e3c-57ca-a6d0-52d4f1eda78b','d7eec598-888f-5c72-b64e-9a8d3316f275','经典番茄烩意大利面','招牌/主食',13.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5ab98010-8461-52b0-8b9e-b50c6c6d17fc','d7eec598-888f-5c72-b64e-9a8d3316f275','百年传承龙脊牛肉饭','招牌/主食',17.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fdafea26-5b25-5bf7-9114-fbc55fc9ab3f','d7eec598-888f-5c72-b64e-9a8d3316f275','红焖肘子饭','招牌/主食',9.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b6317110-89d0-5836-a3c0-999ceea59090','d7eec598-888f-5c72-b64e-9a8d3316f275','咖喱鸡肉饭','招牌/主食',9.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c89ce1bf-3d17-5bfb-90ab-94724c858e27','d7eec598-888f-5c72-b64e-9a8d3316f275','厚切猪排饭','招牌/主食',16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d1613f5-5f66-5796-81da-d77e20916a04','d7eec598-888f-5c72-b64e-9a8d3316f275','厚蛋茄汁猪排饭','招牌/主食',17.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('57b29ead-2521-5603-8c22-04d935efc3b1','d7eec598-888f-5c72-b64e-9a8d3316f275','红烩黄金大鸡排饭','招牌/主食',15.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('98f936d3-6920-5bf0-b0eb-90c6de520b54','d7eec598-888f-5c72-b64e-9a8d3316f275','红烩厚切猪排饭','招牌/主食',16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e37cc29d-0cc9-570f-a43e-65d7bc733eee','d7eec598-888f-5c72-b64e-9a8d3316f275','厚蛋茄汁奥尔良鸡扒饭','招牌/主食',16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('67591d8c-5cc9-535f-b660-f8fbe36c75e3','d7eec598-888f-5c72-b64e-9a8d3316f275','多利亚肉酱芝士焗饭','招牌/主食',16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ea3f035b-1e13-55c8-96b8-eacec268b6a6','d7eec598-888f-5c72-b64e-9a8d3316f275','虾仁煎饼','特色小食',8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7f5f1d94-bd8c-54e0-9442-e4666206f86a','d7eec598-888f-5c72-b64e-9a8d3316f275','韩式泡菜','特色小食',8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2b3fb448-f514-5da4-b29a-53cd6ab815bc','d7eec598-888f-5c72-b64e-9a8d3316f275','黄金薯饼','特色小食',10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d26ed96-02dd-57d5-8b59-d06dbcf4d811','d7eec598-888f-5c72-b64e-9a8d3316f275','海苔味噌汤','特色小食',6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','74ed9519-f3e3-5ce8-a89c-a353e2892edd','东北麻辣烫·友客来',NULL,'/api/v1/media/92fe1670-2315-526c-bfab-954ee0c063b4','active',16)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bc9ae142-f183-50e3-aa9e-3561f41112c0','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','麻辣拌','东北麻辣烫；计价单位：斤',19.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6eb111e1-2c0d-5446-8732-fa4d53089d28','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','麻辣烫','东北麻辣烫；计价单位：斤',19.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5683ccd8-8e5f-5810-9b7b-f217bfe5aa7d','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','沈阳拌鸡架','东北麻辣烫',11,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('658ac028-2dab-56aa-abec-bb7698446849','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','鸡架拌面','东北麻辣烫',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c6c20219-73b3-5a85-90cf-f677579e2689','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','另加面','东北麻辣烫',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('322b5bfd-153b-57dd-93c7-5c9f350f254f','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','黄焖茄子','友客来黄焖鸡系列',10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('36d5cd93-caeb-55c5-be3b-16cc905b0db2','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','土豆鸡块','友客来黄焖鸡系列',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a36f2306-e527-5f96-b89d-244ecc3bafd0','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','黄焖鸡','友客来黄焖鸡系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('81f5792f-4d7f-5653-9037-5afb3a78aa2a','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','排骨鸡','友客来黄焖鸡系列',23,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1ea88a9d-75a3-5fcc-a351-e664e1de8a0e','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','黄焖茄子（肥牛、五花肉）','友客来黄焖鸡系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('761800fa-b91c-5b9b-b1bf-97c41be79378','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','娃娃菜（肥牛、五花肉）','友客来黄焖鸡系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('38f7e6e5-4ace-50b9-b105-aac9d0391f8a','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','金针菇五花肉','友客来黄焖鸡系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4bf9b6d1-6d13-5e9f-9111-e1a7bfca1200','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','鱼豆腐五花肉','友客来黄焖鸡系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5581b598-3ad9-5a17-b65e-49a46acadd38','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','藤椒鸡公煲','藤椒鸡煲系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73f74892-733c-5341-adc8-cfde143319cb','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','金针菇肥牛','藤椒鸡煲系列',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('96017873-81cb-5c71-a187-a491eb6e9ac1','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','鱼豆腐肥牛','藤椒鸡煲系列',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0f2919b5-c0d9-5e0f-93fd-4ad4617758d2','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','油麦菜','单加系列',1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('59b7c8f6-63c7-5f77-b065-dab664e23bfd','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','金针菇','单加系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a729ac3e-11a7-5f2c-81e5-5ac372ebf44c','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','鱼豆腐','单加系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dd376806-c923-5c07-8f0f-947b4a1893b8','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','煎蛋','单加系列；计价单位：个',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5b46232b-5b2d-53d1-bc3c-7aad7de7840f','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','茄子','单加系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0882bef4-d1bb-5a24-88e4-88d1254500a2','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','豆皮','单加系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('70c25e4c-6c95-54bf-ba65-3aa998376cec','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','娃娃菜','单加系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fd24ef9a-1d0a-543f-9bc2-43f5993bb904','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','方便面','单加系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cb38c639-bc63-5303-8d0d-3dbe6d90f9dc','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','五花肉','单加系列',5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('860a3fc6-5864-512f-bc46-47bc8ecfb719','d8f7aaf4-dd0d-5289-9037-f1fa7aff095f','肥牛','单加系列',5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('9126068c-af4f-5535-b1f2-07f6398e361a','74ed9519-f3e3-5ce8-a89c-a353e2892edd','小麦兜海南鸡饭',NULL,'/api/v1/media/a1125c6d-010f-5e32-84c8-2d3c7ba92685','active',17)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('583eb88f-f64c-5aa9-9e2a-bce9f9092817','9126068c-af4f-5535-b1f2-07f6398e361a','口水鸡饭','人气TOP',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f2d4087a-c223-5a17-9d7f-f641c676e35b','9126068c-af4f-5535-b1f2-07f6398e361a','海南鸡饭','人气TOP',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e0ce3b5c-fc34-54da-965a-275e4729c87b','9126068c-af4f-5535-b1f2-07f6398e361a','金牌鸡肉饭','人气TOP',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('04f8018b-d2dd-5ba3-ac37-239e3d2e057f','9126068c-af4f-5535-b1f2-07f6398e361a','海南鸡饭','招牌推荐',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f827231-0eec-56fa-b0a4-2b0628e5e1e6','9126068c-af4f-5535-b1f2-07f6398e361a','鸡排豆腐饭','招牌推荐',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1e465974-2322-5860-892e-a357be66931f','9126068c-af4f-5535-b1f2-07f6398e361a','泰式脆皮鸡饭','招牌推荐',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('340b3ade-ded1-500e-af2a-3ef40bc2940d','9126068c-af4f-5535-b1f2-07f6398e361a','任意双拼加项','特色推荐',3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','74ed9519-f3e3-5ce8-a89c-a353e2892edd','重庆面庄',NULL,'/api/v1/media/6a42d95c-f741-5020-b553-4fffeb1473bd','active',18)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('76be2c49-d06c-500a-9b93-e539d6eb779c','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','炸酱面/米线','面条/米线系列',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('997a6bfc-63fb-55b1-9847-df695ee111ca','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','外婆菜/烤肠炒鸡蛋面/米线','面条/米线系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4079af00-1ffd-5353-ba05-0c62e59b0cef','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','豌杂面/米线','面条/米线系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4fb1fb74-1a50-5017-a058-b0d1f3ab36a3','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','泡椒鸡胗面/米线','面条/米线系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c8b3e96-0d4f-51d9-8b7b-4ea5bbfe727e','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','榨菜肉丝面/米线','面条/米线系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cbe32216-d561-5bca-960d-992c9a9bbc53','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','肉沫豇豆角面/米线','面条/米线系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('93e158e7-6652-5f43-b51c-6f413d5cf5c9','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','孜然肉片面/米线','面条/米线系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9031d82a-e34e-5163-9360-dc99e5d5b4d7','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','农家小炒肉面/米线','面条/米线系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('22c2a70f-3c25-5018-9947-f6f21dd83943','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','双椒鸡面/米线','面条/米线系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2a342151-c716-5edd-a021-36964c0b606d','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','回锅肉面/米线','面条/米线系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('09e8253e-5291-507e-b5cb-e0f5cac35124','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','红烧牛肉面/米线','面条/米线系列',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c0356b95-63f8-524d-b09d-4efba29d52f4','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','素面条/米线','其他主食',9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('074b040b-a2ce-55be-912a-d5eb351d663c','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','土豆泥拌面/米线','其他主食',9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('22dd24ae-a526-5428-afeb-a6c5bf155e8c','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','酸辣粉','其他主食',9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a941addc-8c58-520d-8740-cc707ee615e1','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','外婆菜炒鸡蛋','土豆泥拌饭',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dec58d82-9de7-5959-a3b3-b17dfc114fdf','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','泡椒鸡胗','土豆泥拌饭',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7fdb6539-7002-58c5-9235-03b59f8ccd0a','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','肉沫豆角','土豆泥拌饭',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3a208e16-d473-5366-8dc9-d693ff6ebd0f','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','农家小炒肉','土豆泥拌饭',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f4126b1c-3712-5b88-921f-444163b58a14','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','孜然肉片','土豆泥拌饭',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('550ef9db-03af-5bdd-aea1-827a1541652e','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','双椒鸡','土豆泥拌饭',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a05ad5b3-ea1b-5430-8c29-872d02ed32ea','b5bbcfb2-a39e-5d6a-95aa-072cb1b0879c','辣椒炒五花肉','土豆泥拌饭',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('ee66a03e-a879-5bfc-b58e-ebc4b280f340','74ed9519-f3e3-5ce8-a89c-a353e2892edd','小锅焖面·小锅焖饭',NULL,'/api/v1/media/197ebb22-7df5-5100-9a01-fee9df1173bb','active',19)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b381cef6-2f81-5021-a132-6b0194c06e16','ee66a03e-a879-5bfc-b58e-ebc4b280f340','青条豆角焖面','焖面系列',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('25f07f75-1d46-569d-b114-3eb43274add5','ee66a03e-a879-5bfc-b58e-ebc4b280f340','培根焖面','焖面系列',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9978bc01-6085-531e-82fc-447ca5b56039','ee66a03e-a879-5bfc-b58e-ebc4b280f340','肉沫薯条焖面','焖面系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('21968fc2-b2e6-52cc-a5c8-93dbdd7666cb','ee66a03e-a879-5bfc-b58e-ebc4b280f340','肉沫茄条焖面','焖面系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('af0cde5c-19b0-5cf7-b2e1-b52ee0dd5676','ee66a03e-a879-5bfc-b58e-ebc4b280f340','肉沫豆角焖面','焖面系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2fd44eae-ca8e-5619-932e-cccbd618b383','ee66a03e-a879-5bfc-b58e-ebc4b280f340','五花肉焖面','焖面系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('16aee3df-1484-5929-8c4b-d6add18df6bb','ee66a03e-a879-5bfc-b58e-ebc4b280f340','广式腊肠焖面','焖面系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9f513cfd-849a-5046-acc6-9b851226cf10','ee66a03e-a879-5bfc-b58e-ebc4b280f340','奥尔良鸡丁焖面','焖面系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ea10ca69-d7ec-5935-ab33-619324da81e0','ee66a03e-a879-5bfc-b58e-ebc4b280f340','腊肉豆角焖面','焖面系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ad7c5573-16c0-5e70-a3cb-40f4c0524887','ee66a03e-a879-5bfc-b58e-ebc4b280f340','孜然肉片焖面','焖面系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8ac8f620-52d0-5660-b849-05c0edc35f73','ee66a03e-a879-5bfc-b58e-ebc4b280f340','鸡腿肉焖面','焖面系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ea670b25-38cd-59ca-9e1f-9b8b6fc643de','ee66a03e-a879-5bfc-b58e-ebc4b280f340','黑椒牛肉焖面','焖面系列',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b933c785-0155-59e3-a570-d67dd0c509e6','ee66a03e-a879-5bfc-b58e-ebc4b280f340','外婆菜炒鸡蛋焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('48b1e9c4-14bb-52d4-8292-d6f7620caa18','ee66a03e-a879-5bfc-b58e-ebc4b280f340','烤肠炒鸡蛋焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('babed1d0-7b36-5e32-8f17-2ac4ae480564','ee66a03e-a879-5bfc-b58e-ebc4b280f340','泡椒鸡胗焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d4a675c0-a765-5a15-bfdb-eba7f1234e4d','ee66a03e-a879-5bfc-b58e-ebc4b280f340','肉沫豆角焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e4a82b5e-ef22-504e-abab-93eaf8582549','ee66a03e-a879-5bfc-b58e-ebc4b280f340','肉沫酸豆角焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0dd128ab-b39a-5706-87e4-5c7b099b16d5','ee66a03e-a879-5bfc-b58e-ebc4b280f340','农家小炒肉焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('400d7d7d-ef9a-51be-80a3-500dd757b23b','ee66a03e-a879-5bfc-b58e-ebc4b280f340','孜然肉片焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('118601f6-a7b5-520a-84fd-10b97506dc8d','ee66a03e-a879-5bfc-b58e-ebc4b280f340','辣椒炒五花肉焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fd3f7401-1f23-56b2-be6c-2f686f7d26f3','ee66a03e-a879-5bfc-b58e-ebc4b280f340','双椒鸡焖饭','口味：麻辣、秘制、原味、番茄焖饭系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1615bee6-c657-517f-aecc-143bb3f1ccfd','ee66a03e-a879-5bfc-b58e-ebc4b280f340','任意双拼','口味：麻辣、秘制、原味、番茄焖饭系列',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('665e8d5e-6591-5371-8bbe-3ca9d37422fa','ee66a03e-a879-5bfc-b58e-ebc4b280f340','卤蛋','口味：麻辣、秘制、原味、番茄焖饭系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5e9b1a24-e2e0-5b9a-a070-0b0565cd39ce','ee66a03e-a879-5bfc-b58e-ebc4b280f340','炸蛋','口味：麻辣、秘制、原味、番茄焖饭系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('835e6bf9-42d4-571b-8697-d8cb5df17e70','ee66a03e-a879-5bfc-b58e-ebc4b280f340','豆皮','口味：麻辣、秘制、原味、番茄焖饭系列',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c5513bca-91b2-55bb-bac4-d1796ec82dc6','ee66a03e-a879-5bfc-b58e-ebc4b280f340','肉肠','口味：麻辣、秘制、原味、番茄焖饭系列',3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e2c69167-7b3f-51e6-a4d8-354068f36976','ee66a03e-a879-5bfc-b58e-ebc4b280f340','鸡腿','口味：麻辣、秘制、原味、番茄焖饭系列',5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('7ec799d3-d05c-5a34-8e72-b049dd61009e','74ed9519-f3e3-5ce8-a89c-a353e2892edd','鲍汁捞饭·炙香板烧饭',NULL,'/api/v1/media/f0edc46f-944c-5cc9-86d6-2137efa8a020','active',20)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e93aef88-b1df-596d-9123-b6d0288f3849','7ec799d3-d05c-5a34-8e72-b049dd61009e','鲍汁大鸡排捞饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e073051a-1091-51e8-8252-c198dcda4ca7','7ec799d3-d05c-5a34-8e72-b049dd61009e','匠心鲍汁排骨捞饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a7586a33-e494-56b4-855c-c714248e158f','7ec799d3-d05c-5a34-8e72-b049dd61009e','元气番茄牛肉捞饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bdf35e92-6134-5ce2-b34e-a3de81549dc5','7ec799d3-d05c-5a34-8e72-b049dd61009e','焦糖可乐鸡翅捞饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('33c7bde1-960b-56da-b30f-1b7c2369bae8','7ec799d3-d05c-5a34-8e72-b049dd61009e','销魂蜜香猪肘捞饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a8f8a92f-09e0-52e3-9117-349a25abed36','7ec799d3-d05c-5a34-8e72-b049dd61009e','鲍汁手撕鸡捞饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('83f9322e-3979-5568-b087-0b17d851f1ff','74ed9519-f3e3-5ce8-a89c-a353e2892edd','手工鲜水饺',NULL,'/api/v1/media/a87832b6-1c51-551a-9e2b-14dc8db50c35','active',21)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c5f7dcdf-affe-5ef3-b4bf-37e618bd17b7','83f9322e-3979-5568-b087-0b17d851f1ff','猪肉白菜','招牌饺子系列',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5b606660-de84-508b-8abe-5d9976840b94','83f9322e-3979-5568-b087-0b17d851f1ff','韭菜鸡蛋','招牌饺子系列',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eb63eeed-eb31-5492-a8d5-12af01e1f75a','83f9322e-3979-5568-b087-0b17d851f1ff','韭菜猪肉','招牌饺子系列',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d1ff5af-4957-5b19-a63c-3cd4e862edeb','83f9322e-3979-5568-b087-0b17d851f1ff','猪肉酸菜','招牌饺子系列',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('050aeaf6-85dc-5722-a70e-0f411d2a6c59','83f9322e-3979-5568-b087-0b17d851f1ff','猪肉芹菜','招牌饺子系列',13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5210be8-aab9-5332-ac24-262098703947','83f9322e-3979-5568-b087-0b17d851f1ff','猪肉菌菇','招牌饺子系列',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cdbe39ad-ec91-5bc3-b8d5-e362dab07d7e','83f9322e-3979-5568-b087-0b17d851f1ff','猪肉鲜虾','招牌饺子系列',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('efc8ffb3-2df5-5b96-a29d-7d3f8902594e','83f9322e-3979-5568-b087-0b17d851f1ff','猪肉大葱','招牌饺子系列',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7b94412-6e4b-5aa9-b7a1-3cbeee8bce48','83f9322e-3979-5568-b087-0b17d851f1ff','猪肉香菇','招牌饺子系列',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c8975d3f-cc2d-5e4f-a573-35d22892de31','83f9322e-3979-5568-b087-0b17d851f1ff','牛肉水饺','招牌饺子系列',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3931abfd-7cb0-5c1c-a102-d2075c5cea54','83f9322e-3979-5568-b087-0b17d851f1ff','猪肉三鲜','招牌饺子系列',18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('05d58e8f-6ee8-5600-9dc4-29759ad8d6d6','83f9322e-3979-5568-b087-0b17d851f1ff','鲜肉锅贴','煎饺',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4cf4a222-bc9e-5c4a-b0fa-ab53f417ba90','83f9322e-3979-5568-b087-0b17d851f1ff','玉米猪肉锅贴','煎饺',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2a879df2-964b-533b-b6d2-ddf0dc2fd7c5','83f9322e-3979-5568-b087-0b17d851f1ff','鲜肉馄饨','小馄饨系列；计价单位：碗',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e6c28b8-fe6d-5de9-9779-9da2eb07cd81','83f9322e-3979-5568-b087-0b17d851f1ff','芹菜猪肉馄饨','小馄饨系列；计价单位：碗',16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('824599f4-3d73-55d8-91fa-23ea32b2d2c3','83f9322e-3979-5568-b087-0b17d851f1ff','虾仁韭菜馄饨','小馄饨系列；计价单位：碗',17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('57a352fd-8dbd-5f3e-8f14-40ffa6c1699b','83f9322e-3979-5568-b087-0b17d851f1ff','老味炒手','小馄饨系列；计价单位：碗',14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dining_halls(id,name,description,status,sort_order)
VALUES ('315086d7-f339-5d26-a030-b3d7ffe7cde1','学活食堂',NULL,'active',2)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('482a9fb6-ec4a-52ad-b79c-560a07e127b5','315086d7-f339-5d26-a030-b3d7ffe7cde1','广式味道',NULL,'/api/v1/media/1322f45a-da43-5c08-a916-de498b9a544c','active',0)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d7c4f6c6-c10f-5dd0-b224-62e7b91ca63b','482a9fb6-ec4a-52ad-b79c-560a07e127b5','招牌烧鸭饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e86059c6-1e63-5e5f-bb72-6dff619707cc','482a9fb6-ec4a-52ad-b79c-560a07e127b5','老娘叉烧饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('df3f3d5d-d83c-5a24-a6b7-5fc929b0375d','482a9fb6-ec4a-52ad-b79c-560a07e127b5','香煎鸡排饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8e7301d3-d55d-5f39-9b76-000aace2e353','482a9fb6-ec4a-52ad-b79c-560a07e127b5','黑椒猪排饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ab5a1fc7-8250-54c2-9d21-52f4fcb9b90e','482a9fb6-ec4a-52ad-b79c-560a07e127b5','黑椒土豆肉片饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('270fed48-48df-519d-90b4-a5cf3648abd4','482a9fb6-ec4a-52ad-b79c-560a07e127b5','蜜汁手撸鸡饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('94277bc2-22fc-5701-9bb2-6f3fcfa50cd9','482a9fb6-ec4a-52ad-b79c-560a07e127b5','迷你白切鸡饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3b5f2ea0-4db8-5506-bbfd-7e02b9f82871','482a9fb6-ec4a-52ad-b79c-560a07e127b5','姜葱口水鸡饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1a81a2b4-fa3b-5259-9c74-7a2b79500617','482a9fb6-ec4a-52ad-b79c-560a07e127b5','香辣鸡腿饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f8017aa4-c804-522a-8a9b-f3ea7dd46ed4','482a9fb6-ec4a-52ad-b79c-560a07e127b5','盐焗鸡腿饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('59b516aa-aa9c-5a18-b2e1-a146234db932','482a9fb6-ec4a-52ad-b79c-560a07e127b5','秘制鸡腿饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('86c23145-f78d-5635-816a-28146cac08c9','482a9fb6-ec4a-52ad-b79c-560a07e127b5','香汁牛腩排骨饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e364e111-c5dc-5a7f-8c69-887467074fd5','482a9fb6-ec4a-52ad-b79c-560a07e127b5','广式猪脚饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10e63796-46c9-5f79-84f1-f8e298fbe238','482a9fb6-ec4a-52ad-b79c-560a07e127b5','隆江猪脚饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c4baf87e-96c6-5a2d-82de-2a9405fdaf8e','482a9fb6-ec4a-52ad-b79c-560a07e127b5','网红脆皮五花肉饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ec780cc1-3e59-5db7-a9d9-9176dfad7487','482a9fb6-ec4a-52ad-b79c-560a07e127b5','任选双拼饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ef4b5ed4-ee51-5b20-b7ea-ff11741a73d8','482a9fb6-ec4a-52ad-b79c-560a07e127b5','小炒五花肉笼仔饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6647bb9a-3ca1-5c8d-ac8d-c00414854539','482a9fb6-ec4a-52ad-b79c-560a07e127b5','广式腊肠笼仔饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f0db37e2-7c05-5a47-99a2-33801deb722c','482a9fb6-ec4a-52ad-b79c-560a07e127b5','香菇鸡味笼仔饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('17efca59-425d-5e83-b758-4faf8f905a04','482a9fb6-ec4a-52ad-b79c-560a07e127b5','招牌烧鸭笼仔饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f0f6ecc-6d20-5e3c-b7ab-53555966f3f3','482a9fb6-ec4a-52ad-b79c-560a07e127b5','广式鸡腿笼仔饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c7439fe6-4282-5146-927c-e8fc3367c273','482a9fb6-ec4a-52ad-b79c-560a07e127b5','鸡翅笼仔饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('18e37780-87b8-5754-b024-5e02e58f177b','482a9fb6-ec4a-52ad-b79c-560a07e127b5','手撕鸡笼仔饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5763a3ca-597e-5809-b250-e54d4f9f4a68','482a9fb6-ec4a-52ad-b79c-560a07e127b5','口水鸡笼仔饭',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('708da1f4-0e29-5004-8929-01bd613ed274','482a9fb6-ec4a-52ad-b79c-560a07e127b5','脆皮五花肉笼仔饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('95fa080e-2def-5ff3-b34b-bddbd2375319','482a9fb6-ec4a-52ad-b79c-560a07e127b5','隆江猪脚笼仔饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('95ccd2d2-dc2e-51d8-b036-bf73e52ce8d5','482a9fb6-ec4a-52ad-b79c-560a07e127b5','腊肠鸡排笼仔饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('936ebbf9-2c7b-5bc7-a0ed-a1ca3f80ed6e','482a9fb6-ec4a-52ad-b79c-560a07e127b5','腊肠烧鸡笼仔饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('21e89a0d-98b5-521d-af3b-1099c76bb753','482a9fb6-ec4a-52ad-b79c-560a07e127b5','腊肠叉烧笼仔饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c2f58285-d6e3-54f2-a122-cb3a41dbb161','482a9fb6-ec4a-52ad-b79c-560a07e127b5','烧鸭叉烧笼仔饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e33205e1-8d2f-5362-b0c2-a34608835522','482a9fb6-ec4a-52ad-b79c-560a07e127b5','烧鸭鸡排笼仔饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d87458c-b97c-5b59-85b2-521c09bacaef','482a9fb6-ec4a-52ad-b79c-560a07e127b5','鸡排叉烧笼仔饭',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5dac1816-b834-50d3-aaff-9c12f28538fb','482a9fb6-ec4a-52ad-b79c-560a07e127b5','鸡蛋肉沫炒米粉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bfe586a4-f851-5e3f-be93-1aa7ff83d5ea','482a9fb6-ec4a-52ad-b79c-560a07e127b5','微辣鸡蛋肉沫炒河粉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1432c026-0a1b-535a-bf1a-ccbf7ffce6cf','482a9fb6-ec4a-52ad-b79c-560a07e127b5','素烩意粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe92bed8-fb89-537c-a21e-76087918528f','482a9fb6-ec4a-52ad-b79c-560a07e127b5','肉烩意粉',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3fc3ec50-347b-5bee-b5a6-96f6614e11b7','482a9fb6-ec4a-52ad-b79c-560a07e127b5','煎鸡蛋','计价单位：个',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7c6e83fc-774f-5925-9b4e-cf4bffebb117','482a9fb6-ec4a-52ad-b79c-560a07e127b5','卤鸡腿','计价单位：个',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c439d066-3e11-5a8c-878d-7bdad90f7452','482a9fb6-ec4a-52ad-b79c-560a07e127b5','香肠','计价单位：2个',5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6cbbc456-077f-5125-a37f-fb9f24cf62f2','482a9fb6-ec4a-52ad-b79c-560a07e127b5','大肠','计价单位：个',5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f6970455-216a-58ca-bb66-a1048335bf33','482a9fb6-ec4a-52ad-b79c-560a07e127b5','鸡腿','计价单位：个',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bd3739f2-62c8-5622-8915-a979f78ba5ba','482a9fb6-ec4a-52ad-b79c-560a07e127b5','鸡翅','计价单位：个',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7b031347-f3e6-5d84-abcb-38c06911105b','482a9fb6-ec4a-52ad-b79c-560a07e127b5','烧鸭','计价单位：只',90,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('210d8792-bdb9-5630-9d37-9448c55a433d','315086d7-f339-5d26-a030-b3d7ffe7cde1','重庆味道',NULL,'/api/v1/media/1d3c0299-4398-5fb6-b906-ea30bdd1fd25','active',1)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1e37ac6e-0d13-51b6-90e3-0080c7c89b79','210d8792-bdb9-5630-9d37-9448c55a433d','五花肉酸菜饭',NULL,15.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b115197e-9d44-5a66-8bf5-76ac34c6a5b5','210d8792-bdb9-5630-9d37-9448c55a433d','五花肉包菜饭',NULL,15.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f8c8b4a-7144-5c47-b905-daf54c25a149','210d8792-bdb9-5630-9d37-9448c55a433d','五花肉圆葱饭',NULL,15.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1088342b-f4fe-5502-8ed6-dfb51734c4b0','210d8792-bdb9-5630-9d37-9448c55a433d','五花肉辣白菜饭',NULL,15.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f4ea4d95-2e8c-529d-9ed2-ca7ee2914010','210d8792-bdb9-5630-9d37-9448c55a433d','法式烤排饭',NULL,15.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('53e7f513-f1f4-59ef-931d-fc8317baa34f','210d8792-bdb9-5630-9d37-9448c55a433d','五花肉金针菇饭',NULL,15.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f546d4a-e988-5da2-a0d1-5430d299d31a','210d8792-bdb9-5630-9d37-9448c55a433d','韩式咖喱鸡拌饭',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1feba549-4563-54d9-9d35-ea9110484be8','210d8792-bdb9-5630-9d37-9448c55a433d','韩式招牌炸鸡拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9279a4a0-07ba-531f-a679-f2e69ed15d19','210d8792-bdb9-5630-9d37-9448c55a433d','番茄肉末茄条拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7051fe39-e932-5c37-8bcf-2eef43635f46','210d8792-bdb9-5630-9d37-9448c55a433d','黑椒猪排拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bf01c892-68e5-5e62-bada-86819294f239','210d8792-bdb9-5630-9d37-9448c55a433d','韩式小鸡腿拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5c8183a7-cf13-54d1-bd88-f0ac071e0d26','210d8792-bdb9-5630-9d37-9448c55a433d','韩式金枪鱼拌饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('88444b51-3b55-5c60-ba69-2201ee7733f1','210d8792-bdb9-5630-9d37-9448c55a433d','韩式香辣培根拌饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5a321085-e50c-5f35-aa6e-320395210b77','210d8792-bdb9-5630-9d37-9448c55a433d','藤椒鸡排拌饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('de83bca8-0f19-510d-9c76-3d125e9fdfe3','210d8792-bdb9-5630-9d37-9448c55a433d','韩式肥牛拌饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1ac066a0-7335-5f0a-9ef2-b42d4b33681e','210d8792-bdb9-5630-9d37-9448c55a433d','韩式番茄牛腩拌饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0b05a970-bad8-509a-855c-585fd61cdae5','315086d7-f339-5d26-a030-b3d7ffe7cde1','西北味道',NULL,'/api/v1/media/2f3f6338-a843-51ea-bc83-7034881ac8f6','active',2)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('58460639-8e99-5179-aaf3-78c6235d1783','0b05a970-bad8-509a-855c-585fd61cdae5','校园牛肉面',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6a8b1af9-6573-5c61-8031-d15830ef5fbf','0b05a970-bad8-509a-855c-585fd61cdae5','素臊子面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bdadfc72-1142-5bee-944d-7ae5478a20bc','0b05a970-bad8-509a-855c-585fd61cdae5','臊子面',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3dc28c67-c870-5178-83d3-f86e41621535','0b05a970-bad8-509a-855c-585fd61cdae5','麻辣鸡丝面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9f4689e2-2ddc-513b-b3f3-3f74e6ee4305','0b05a970-bad8-509a-855c-585fd61cdae5','麻辣鸡蛋酱面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c77f4a90-754f-5bab-92cd-945f761e28e3','0b05a970-bad8-509a-855c-585fd61cdae5','西红柿肉酱面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f29cd9c-f528-5d54-bf36-8fc0fb278f28','0b05a970-bad8-509a-855c-585fd61cdae5','红烧鸡块面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('50d1dded-34bf-5930-9345-c064cb1e4541','0b05a970-bad8-509a-855c-585fd61cdae5','小炖肉面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1bd8360d-9cc1-52e2-9f9f-8f8caaeeb9d2','0b05a970-bad8-509a-855c-585fd61cdae5','酸汤肥牛面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3b63af9b-20a5-5cf5-a762-f5268fcd8d1e','0b05a970-bad8-509a-855c-585fd61cdae5','小炒肉面',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c78bb3c1-0c27-5029-9210-aabfae9d523e','0b05a970-bad8-509a-855c-585fd61cdae5','红烧牛肉面',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aa4cfdb1-2f88-5dcc-aed0-b4c13743fa3b','0b05a970-bad8-509a-855c-585fd61cdae5','油泼面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cc80382c-984f-506c-90ef-9f49dec2400a','0b05a970-bad8-509a-855c-585fd61cdae5','炸酱面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a4bd518c-3152-512a-bd85-181222280b74','0b05a970-bad8-509a-855c-585fd61cdae5','西红柿鸡蛋面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('518db5c6-7a5b-54a8-be10-3c8c80aa695e','0b05a970-bad8-509a-855c-585fd61cdae5','香菇肉酱面',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8b370627-b022-59b8-8590-b7e74117af4b','0b05a970-bad8-509a-855c-585fd61cdae5','尖椒鸡蛋酱面',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('20c64139-94cd-5f26-ae82-d8a5a396bc22','0b05a970-bad8-509a-855c-585fd61cdae5','茄子肉丁面',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7a25b31f-9c84-59cb-9baa-fd9f2b490b2a','0b05a970-bad8-509a-855c-585fd61cdae5','剁椒肉酱面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6daeb2d0-24e0-5b7a-89cd-781777a2d28d','0b05a970-bad8-509a-855c-585fd61cdae5','大盘鸡拌面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f80d43f4-4e0d-58ff-9b22-df166d040679','0b05a970-bad8-509a-855c-585fd61cdae5','素肉臊子面',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1008946a-6855-5d9c-86b6-92233d925857','0b05a970-bad8-509a-855c-585fd61cdae5','麻辣鸡丝面',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1f9f4bcd-9f16-529a-b51f-49c3a3d2f83a','0b05a970-bad8-509a-855c-585fd61cdae5','麻辣西红柿面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('be382e20-eed6-5bb5-a506-c9038ab69bdc','0b05a970-bad8-509a-855c-585fd61cdae5','西红柿鸡块面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('590562bb-fc00-5157-a907-a425e0d1cc64','0b05a970-bad8-509a-855c-585fd61cdae5','小炒肉面',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5c32633c-b23b-521d-8d87-056e2a19ac5d','0b05a970-bad8-509a-855c-585fd61cdae5','火腿炒面',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f1c88c23-791b-520d-ac1c-868344f2cd4c','0b05a970-bad8-509a-855c-585fd61cdae5','鸡蛋炒面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('626aab19-c4ee-5825-b52a-059f212896fa','0b05a970-bad8-509a-855c-585fd61cdae5','肉丝炒面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9a4b6654-c535-5218-8858-9551d688e9cb','0b05a970-bad8-509a-855c-585fd61cdae5','鸡肉炒面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73388948-ef49-5e3f-a2bf-ecc1ce6f4253','0b05a970-bad8-509a-855c-585fd61cdae5','卤鸡蛋','计价单位：个',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ae2fe4cd-e09f-5a00-9671-f8f20a197774','0b05a970-bad8-509a-855c-585fd61cdae5','卤豆腐',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c14b19f6-6659-50f5-86a6-20dabf402fd1','0b05a970-bad8-509a-855c-585fd61cdae5','小鸡蘑菇面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('23be2427-0c97-5e79-a740-64132db923f9','0b05a970-bad8-509a-855c-585fd61cdae5','羊肉泡馍',NULL,18.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('d2a8a116-7e8e-5d71-b753-8bbae5a1a725','315086d7-f339-5d26-a030-b3d7ffe7cde1','四川味道',NULL,'/api/v1/media/52327444-ba59-5932-bbfd-f00239949d15','active',3)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a19ca905-8c8c-57b0-a230-813b81c817b0','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','麻辣香锅招牌香辣鸡腿冒菜',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ceb4eb03-9293-5de0-a70a-232eb984f4a6','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','半只鸡冒菜',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0d081c3b-5502-56d7-9bbb-8e2161d22807','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','麻辣鸭蹼冒菜',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7d9b9ae4-a253-5cbc-a433-e85b102633a5','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','午餐肉冒菜',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('147ecea9-6002-52c2-b69a-8bacf0d4d289','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','培根冒菜',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d3ce6e08-180a-5c1e-8861-b0b861e730fb','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','肉卷冒菜',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b36c1c61-2ff8-53ac-ba33-52b459ac4753','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','水煮肉片',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d418d57b-f196-5ec5-a9f5-41691cdba17f','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','什锦丸子',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2921def7-2206-5823-829a-f9d0a651b4b5','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','什锦酥肉',NULL,16.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d9b9f87-f032-57b1-b473-afc7abd6d421','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','咖喱鸡肉盖浇饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('741a4721-c640-5a77-a832-da7ebc165d1d','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','辣椒炒肉盖浇饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('69d118d7-bdeb-5b05-b10c-b5c8286807a9','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','麻辣鸭腿盖浇饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('77703696-5b6a-5f54-9f38-c9e092afe283','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','尖椒豆芽肉丝盖浇饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c2256252-6c95-59bb-9164-fc4f50ba884b','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','梅菜扣肉盖浇饭',NULL,19.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('15b937b4-67de-5d75-9b9b-c5739f341434','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','土豆炖牛肉盖浇饭',NULL,19.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ddb7e2e8-d260-5713-a18e-507df5c2f1e0','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','酸辣土豆丝盖浇饭',NULL,9.9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('476b2351-6772-5fe5-97e9-e7079afffcf7','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','西红柿鸡蛋盖浇饭',NULL,9.9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('20d04e19-3fd9-5176-a768-380b2997b655','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','鱼香茄子盖浇饭',NULL,12.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('acbdbed5-f20d-5855-9575-8ea18119e037','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','麻婆豆腐盖浇饭',NULL,12.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c5b8a0e1-656a-56bb-a075-216a6cff1115','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','鱼香肉丝盖浇饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('631eee41-face-5aae-ba27-3b65bab8adeb','d2a8a116-7e8e-5d71-b753-8bbae5a1a725','黑椒肉片盖浇饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('bac0b38c-5847-53a9-9ffe-e9444adc6fd3','315086d7-f339-5d26-a030-b3d7ffe7cde1','南北味道',NULL,'/api/v1/media/a356c78f-a54b-56fb-bb51-364a0c0dc610','active',4)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bf267ecb-d9ae-59eb-9aae-623a1752e03a','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','酥肉小锅',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bc477972-91a1-55bf-92d4-0bcc6d9a7ca4','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','培根小锅',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dce407ac-469f-5ea0-a525-5123142a7bd3','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','瘦肉小锅',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bd195275-a6af-5f68-9b77-87693c22804e','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','肥牛小锅',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b58fcd9-0978-5e3b-90c1-171b45017f93','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','鱼豆腐小锅',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ebbbe995-3892-5159-8248-efdd68812f5c','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','牛肉丸小锅',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5006fdbb-cc6d-5e87-95c9-524ebc40fa88','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','招牌鸡公煲',NULL,15.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('04af727c-6883-5fb2-b1a2-ef46f51c3924','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','肉末酱香茄子煲',NULL,13.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5f49af18-729d-508e-80da-634b55aefe14','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','肉末嫩豆腐煲',NULL,13.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('40ad23e4-e0d7-5238-a002-fcbfc65717f8','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','招牌鸡公煲（大份）',NULL,15.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('715497d8-891f-5419-9aa1-7c85e9559fc2','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心卤汁素拼饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('91e54e0e-4bf1-59b0-82e6-b9292931343a','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','卤蛋','计价单位：个',1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f7bb3689-35b6-57ce-8c95-c1091448acb6','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','卤海带结','计价单位：3个',1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('21f12ab1-e2d9-5b06-a3d9-5082a206d3c4','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','卤萝卜','计价单位：个',0.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d157c9c6-bc91-5515-8528-215ab1d6cb96','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','卤豆腐干','计价单位：个',1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6830558e-2f01-5fa4-8d68-95297d205821','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','酱汁茄子饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10de2c04-5cf8-56af-a57f-3094b0755717','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心肉肠饭',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('55d3bc02-a56a-55de-8950-8bb85d99ba6a','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','卤汁鸭腿拌饭',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7adbc9ab-9d36-5b38-af24-f1d3ac184881','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','奥尔良鸡排饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f770c982-736f-54ac-93a4-2fe6be651adc','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','卤汁大鸡饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('026a1e0f-9b23-57e4-9c6e-279312806c3d','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','卤汁鸡腿拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b2934132-7f8a-5fd8-82de-7dc47e337a66','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心脆皮鸡肉饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fa208d9c-5911-5985-819e-e465b8e9cdc4','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心椒麻鸡饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('30cabe92-4575-5a3a-82d6-f7781dbca81f','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心排骨饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f3bfd5a3-eae3-59f4-84af-92596eaed04d','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心卤肉饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('171f7d89-1c36-56e7-99c1-51bac03f2c57','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心猪肘肘子饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9db42f9e-faf0-555d-a4de-87c46f7e9957','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','脱脂牌饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1316c4e2-cc50-52b9-a645-e0b65297d4b7','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','金牌肘子拌饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5b4a6766-8017-5c47-beaa-72900d4eb5a2','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心鸭腿拌饭',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('13737e73-51d9-5ec2-8117-f279b3633f53','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','匠心椒麻鸡拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('78ba3b10-91a7-59f7-9b7d-304255f2197e','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','招牌生烫吊龙米线（清汤）',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('251d8af3-d791-5841-a097-53c9745f93b7','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','招牌肉末炸酱米线（酱香）',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('951d0356-e3c4-5d01-8828-8bd59066385e','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','经典双拼米线（吊龙+炸酱）',NULL,21,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9c35fb68-f02e-5aa7-aee0-465dfc23525e','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','生烫梅花肉米线（清汤）',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c93c14d4-0f4f-505d-a3d0-0dde81d9727c','bac0b38c-5847-53a9-9ffe-e9444adc6fd3','牛肉丸生烫米线（清汤）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('007c891a-a6af-5a53-9de4-66ddd299bef3','315086d7-f339-5d26-a030-b3d7ffe7cde1','土耳其味道',NULL,'/api/v1/media/d6d8eb4d-f7a6-500d-8e4f-8ccd4e54abfb','active',5)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f515c48c-76f4-5d5a-8045-4490da6c6f0e','007c891a-a6af-5a53-9de4-66ddd299bef3','牛肉滑蛋饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('15dc344a-4dbf-5009-841b-cd5e06b0a351','007c891a-a6af-5a53-9de4-66ddd299bef3','鲜鱿滑蛋饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('657f512b-7d51-5de6-bd5b-bf93dea795e8','007c891a-a6af-5a53-9de4-66ddd299bef3','爽嫩虾滑滑蛋饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('699a0cc5-3b19-5d0e-9347-0ec982c4213d','007c891a-a6af-5a53-9de4-66ddd299bef3','精品五花肉滑蛋饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe22bfa2-1911-509e-8413-40d07270602b','007c891a-a6af-5a53-9de4-66ddd299bef3','奥尔良烤肉滑蛋饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9a64fc5a-0172-5927-b9e0-42a14f222d42','007c891a-a6af-5a53-9de4-66ddd299bef3','滑嫩鸡丁滑蛋饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('33be94ae-160d-514a-8340-ef0697aca536','007c891a-a6af-5a53-9de4-66ddd299bef3','肉松滑蛋饭',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b53b75d6-a8d7-5275-b253-b9ea65dd546a','007c891a-a6af-5a53-9de4-66ddd299bef3','迷你火腿滑蛋饭',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('95301da0-c320-56fd-a44e-a48d36c210e2','007c891a-a6af-5a53-9de4-66ddd299bef3','广式腊肠滑蛋饭',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4253483b-703f-5586-a565-7aad853091a7','007c891a-a6af-5a53-9de4-66ddd299bef3','排骨鸡米饭',NULL,23,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('02d7e7e9-48cc-5ef1-a6e5-e1e55a0d9833','007c891a-a6af-5a53-9de4-66ddd299bef3','黄焖排骨饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f6978f52-a0bb-50b1-9fcb-550b0ec41a68','007c891a-a6af-5a53-9de4-66ddd299bef3','黄焖五花肉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d38a0fc0-4c3d-5850-8039-16825418e694','007c891a-a6af-5a53-9de4-66ddd299bef3','黄焖小酥肉',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a065ecf0-afe1-5c60-8720-d81338ebae88','007c891a-a6af-5a53-9de4-66ddd299bef3','黄焖鱼豆腐',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a9008f1d-4bf3-549f-815a-e04838507c58','007c891a-a6af-5a53-9de4-66ddd299bef3','黄焖茄子饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a43a7e13-91b3-5f1b-98ee-f96e71881908','007c891a-a6af-5a53-9de4-66ddd299bef3','黄焖鸡加金针菇',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7684a2a0-24b7-5c77-bda0-f35548821c6d','007c891a-a6af-5a53-9de4-66ddd299bef3','番茄烤肉拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7bda1ad0-6b3f-53cd-ad42-6f66858da901','007c891a-a6af-5a53-9de4-66ddd299bef3','香辣烤肉拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('19ddf4f4-bcd3-55a3-b98d-fb7c7caa260c','007c891a-a6af-5a53-9de4-66ddd299bef3','沙拉烤肉拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('998296e4-002d-5925-9e6e-2574ef20bd6a','007c891a-a6af-5a53-9de4-66ddd299bef3','千岛烤肉拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('72faf7a8-7419-5f9c-958f-b742052feac0','007c891a-a6af-5a53-9de4-66ddd299bef3','黑椒烤肉拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a5b10f15-7e40-5145-8b9c-693d07dbcc5c','007c891a-a6af-5a53-9de4-66ddd299bef3','甜辣烤肉拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('200116b5-1ec7-5856-ace3-4c64a3f137ec','007c891a-a6af-5a53-9de4-66ddd299bef3','红油耳丝拌饭',NULL,21,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8b7335f8-659f-5853-a9f9-aa37bb17bf37','007c891a-a6af-5a53-9de4-66ddd299bef3','五香猪脸拌饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('db5d345f-7469-5933-9fe5-f5ca1f91f977','007c891a-a6af-5a53-9de4-66ddd299bef3','传统酱肉拌饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1bdb0fb3-b519-5c8e-88a4-0f8398abdf37','007c891a-a6af-5a53-9de4-66ddd299bef3','馋嘴鸭拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dde72057-366f-5eb2-b5ac-6ce40c9bf6cd','007c891a-a6af-5a53-9de4-66ddd299bef3','川湘小炒肉拌饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c77586ce-f1c9-54eb-a7d1-1e94a6de002f','007c891a-a6af-5a53-9de4-66ddd299bef3','霸王炒鸡拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('35e0abf8-ba7c-5341-be3d-785eaa0f1104','007c891a-a6af-5a53-9de4-66ddd299bef3','可乐鸡翅根拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ca9954a0-30de-5854-b900-5583540700d0','007c891a-a6af-5a53-9de4-66ddd299bef3','口水鸡拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('72a23709-3faf-553a-9110-c18af4a30e23','007c891a-a6af-5a53-9de4-66ddd299bef3','照烧鸡肉拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6762f6d9-e658-56de-bf0d-01c4fad506d5','007c891a-a6af-5a53-9de4-66ddd299bef3','鱼香肉丝拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fa4321e5-a691-57c2-8590-e576cd72810c','007c891a-a6af-5a53-9de4-66ddd299bef3','咖喱鸡肉拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e0fee668-8180-5235-9ab2-31558ccf1b8b','007c891a-a6af-5a53-9de4-66ddd299bef3','川香小炒肉拌饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('96fe9847-ed76-5506-bb55-3d8a9cd790cf','007c891a-a6af-5a53-9de4-66ddd299bef3','鳕鱼排拌饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('da403000-8bf6-59b1-81f4-4ac91ab72c30','007c891a-a6af-5a53-9de4-66ddd299bef3','日式照烧鸡肉拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('afe6c135-a202-5152-bdf9-dd102b71086c','007c891a-a6af-5a53-9de4-66ddd299bef3','泰式咖喱鸡肉拌饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('87f1f10c-c8ee-5491-a46b-e0103487c60a','007c891a-a6af-5a53-9de4-66ddd299bef3','五花肉土豆粉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('245519db-c25d-5e04-bb9f-8a37bef1a258','007c891a-a6af-5a53-9de4-66ddd299bef3','鸡块土豆粉',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('74f2ade6-b982-55f0-9375-a43f06430eea','007c891a-a6af-5a53-9de4-66ddd299bef3','肥牛土豆粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2aee4eef-b893-589c-9120-214d3159fb9a','007c891a-a6af-5a53-9de4-66ddd299bef3','鱼丸土豆粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a4b990db-491a-584a-86c5-2fe5bc006b79','007c891a-a6af-5a53-9de4-66ddd299bef3','鱼豆腐土豆粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ec75fcb2-5ff9-50e6-93a4-5ce413a3529b','007c891a-a6af-5a53-9de4-66ddd299bef3','烤肠土豆粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('a10059a7-c147-51a5-9a86-8d615e1d0779','315086d7-f339-5d26-a030-b3d7ffe7cde1','江南味道',NULL,'/api/v1/media/a7818309-7f97-5f9f-9f75-9ed3b6d71242','active',6)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('01fc34e2-15a1-5077-b790-fbe8b66c65ff','a10059a7-c147-51a5-9a86-8d615e1d0779','上海生煎包','计价单位：个',2.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b574e96d-6387-5527-aac2-8d130e0c060b','a10059a7-c147-51a5-9a86-8d615e1d0779','猪肉芹菜水饺小份15个/',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('846c6a9c-f1aa-56da-9541-54fe759649ea','a10059a7-c147-51a5-9a86-8d615e1d0779','大份18个/',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5cb70738-f512-5f36-894d-3f493364ec56','a10059a7-c147-51a5-9a86-8d615e1d0779','猪肉大葱水饺小份15个/',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f914d1aa-459d-5bd5-9174-a8cd2f777bb2','a10059a7-c147-51a5-9a86-8d615e1d0779','猪肉韭菜水饺小份15个/',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2fd4ac8c-d523-5282-8551-9384ffd3676b','a10059a7-c147-51a5-9a86-8d615e1d0779','猪肉白菜水饺小份15个/',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6dc75753-e18d-5b27-a261-8392d48bfa6e','a10059a7-c147-51a5-9a86-8d615e1d0779','猪肉茴香水饺小份15个/',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7fa9b7ea-2b51-51fb-823f-9905949821b6','a10059a7-c147-51a5-9a86-8d615e1d0779','猪肉莲藕水饺小份15个/',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('888c3f56-0dbf-530b-bbf5-763d0a2bcfc7','a10059a7-c147-51a5-9a86-8d615e1d0779','韭菜鸡蛋水饺小份15个/',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6d481176-3d59-592b-baea-2886ea628240','a10059a7-c147-51a5-9a86-8d615e1d0779','大份20个/',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d7abc68f-c146-5d4a-b7e5-28e0fcea87f1','a10059a7-c147-51a5-9a86-8d615e1d0779','西葫芦鸡蛋水饺小份15个/',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f78234ea-2316-56c6-b2e8-2c4965886295','a10059a7-c147-51a5-9a86-8d615e1d0779','酸汤水饺基础价+',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8148a53e-8643-5957-9f9e-83d898b741a4','a10059a7-c147-51a5-9a86-8d615e1d0779','猪肉三鲜',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('41f3a937-092b-5171-aaff-0c7cb145c012','a10059a7-c147-51a5-9a86-8d615e1d0779','满口香水饺',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('993640bc-68fb-5163-8cd7-ea5136fa7229','a10059a7-c147-51a5-9a86-8d615e1d0779','猪肉玉米粒',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e5692964-158d-538f-aea6-74727889e494','a10059a7-c147-51a5-9a86-8d615e1d0779','烧饼加菜',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8acb00e1-7b94-5ccf-a425-39daf56243c4','a10059a7-c147-51a5-9a86-8d615e1d0779','烧饼加鸡蛋',NULL,3.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a539fde9-5737-50dc-93d7-0838986350d8','a10059a7-c147-51a5-9a86-8d615e1d0779','烧饼加肠',NULL,4.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('db7f158a-2c64-54fa-9f66-3d19ee8a030a','a10059a7-c147-51a5-9a86-8d615e1d0779','蛋肠双夹',NULL,5.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d8c5ca6f-6d17-522a-8856-d034d5545408','a10059a7-c147-51a5-9a86-8d615e1d0779','烧饼加鸡柳',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4dde127d-0b51-5aaf-8ed7-a5e1eec3c1fb','a10059a7-c147-51a5-9a86-8d615e1d0779','烧饼加卤肉',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bf0d7e35-7f3c-5581-abcb-636be0c3ef31','a10059a7-c147-51a5-9a86-8d615e1d0779','烧饼加鸡排',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('475b29ea-4236-5da9-adb4-efdd35bd7753','a10059a7-c147-51a5-9a86-8d615e1d0779','蔬菜卷饼',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('278bcf84-9834-5736-9cb2-424260bac311','a10059a7-c147-51a5-9a86-8d615e1d0779','鸡蛋卷饼',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('361f522b-3f15-500d-8190-cc2403dc270d','a10059a7-c147-51a5-9a86-8d615e1d0779','香肠卷饼',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('507bafbc-b4eb-5230-8924-72948cf38cc0','a10059a7-c147-51a5-9a86-8d615e1d0779','鸭肉卷饼',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2155ea5e-1f68-5303-a4e3-eff61c700a29','a10059a7-c147-51a5-9a86-8d615e1d0779','鸡柳卷饼',NULL,9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f94a44e-acbb-564e-830e-a380959edc6c','a10059a7-c147-51a5-9a86-8d615e1d0779','卤肉卷饼',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('99420f7e-f3a0-5f98-ae45-2b719963d023','a10059a7-c147-51a5-9a86-8d615e1d0779','浓香螺蛳粉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('99f0d707-0618-51d2-8846-19c79f2aef2e','a10059a7-c147-51a5-9a86-8d615e1d0779','干捞螺蛳粉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6c474ae9-a800-545c-b11a-d6fb5f1f45a3','a10059a7-c147-51a5-9a86-8d615e1d0779','豆泡/卤蛋',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('adc1a69d-ee18-56ad-843a-59f4894082f6','a10059a7-c147-51a5-9a86-8d615e1d0779','兰花干',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('408cf7a8-7cee-5b38-a0c8-a932fd40ff4c','a10059a7-c147-51a5-9a86-8d615e1d0779','响铃卷',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b61f963-b7f9-53d0-bc71-d30bd242de25','a10059a7-c147-51a5-9a86-8d615e1d0779','台湾烤肠',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e08ebbea-ae18-5364-98b7-bafef66bbd0f','a10059a7-c147-51a5-9a86-8d615e1d0779','炸蛋',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dbd0a44c-982e-5e6d-8625-db0c7a8f3ef4','a10059a7-c147-51a5-9a86-8d615e1d0779','黄金猪皮',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('799cbcf6-db02-5441-9b38-e13341f4cbf6','a10059a7-c147-51a5-9a86-8d615e1d0779','原味米粉',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c0ba0b8e-6b2c-5bd6-ac93-ebcc14c1d8e5','a10059a7-c147-51a5-9a86-8d615e1d0779','酸辣米粉',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b029ea83-2e7c-5ff4-89d9-95e9b3d834f1','a10059a7-c147-51a5-9a86-8d615e1d0779','酸菜米粉',NULL,11,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e8404135-1240-5708-a079-4cdbc7d3ac21','a10059a7-c147-51a5-9a86-8d615e1d0779','金针菇米粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('63eaf3b1-126f-53cd-9185-9ea03ae160c1','a10059a7-c147-51a5-9a86-8d615e1d0779','鱼豆腐米粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('335f75d3-49b5-5f1f-bd1c-a179166598b2','a10059a7-c147-51a5-9a86-8d615e1d0779','笋尖米粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('13b0c18a-64e4-5b85-9b0e-48bfd22c6c38','a10059a7-c147-51a5-9a86-8d615e1d0779','招牌肉末米粉',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bd7060d5-b667-5e2f-aa86-013e7b7bd884','a10059a7-c147-51a5-9a86-8d615e1d0779','牛肉米粉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('775dd159-27ff-5576-a5e3-047a5b5da012','a10059a7-c147-51a5-9a86-8d615e1d0779','鸡丝米粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cc76fe5c-640f-5c30-8382-607cc5e3fb29','a10059a7-c147-51a5-9a86-8d615e1d0779','鸡肉米粉',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('961e8762-c8b0-535d-be29-67ffa8415a7e','a10059a7-c147-51a5-9a86-8d615e1d0779','原味拌粉',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b1c0bbf-e573-54e4-87bc-89ec42edd8d4','a10059a7-c147-51a5-9a86-8d615e1d0779','菌菇拌粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9b06a9a0-739f-5e6d-8fb0-d1e1f48fa36e','a10059a7-c147-51a5-9a86-8d615e1d0779','笋尖拌粉',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('137785ce-34e6-5a71-9e6c-60ac9b742257','a10059a7-c147-51a5-9a86-8d615e1d0779','鸡肉拌粉',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4c5efaa7-befd-5fa5-9caf-c93a795855d1','a10059a7-c147-51a5-9a86-8d615e1d0779','肉沫拌粉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f4299b2-5f43-50fc-869e-e77bf6905904','a10059a7-c147-51a5-9a86-8d615e1d0779','牛肉拌粉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','315086d7-f339-5d26-a030-b3d7ffe7cde1','川湘味道',NULL,'/api/v1/media/48b9feae-354f-5f36-a3a6-f6fa9847fb08','active',7)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d552fcf-07e1-5e85-80a2-0a92633adc2e','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','缤纷水果比萨',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4d13d6ac-34f6-5ce7-b3ec-55f99e3832f3','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','招牌牛肉比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5569fcc3-a519-5f85-8675-84d8e86e3890','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','BBQ鸡肉比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8f99662f-1135-55a4-a00e-44030b5af560','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','美乃滋土豆培根比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8fd0c374-7609-5239-8e5f-cd86ac730491','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','金枪鱼比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9ce11a5-cb2c-5e16-904d-466f4c160b18','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','夏威夷比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('44b9c144-85fe-5ffd-8e36-5050ccc58333','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','超至尊比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f4790ec5-f395-56b4-8db3-8ef78ba1fc21','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','金丝肉松比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c0428abc-8fff-5a6a-b456-a5370b5bfcf4','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','意式红肠比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8fb975c9-0f32-5b20-beff-199d4d9d536f','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','眉州香辣比萨',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('919c2335-0d4e-52fb-ae3d-0492588e26cc','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','榴莲多多比萨',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2734705e-52f3-5d96-a8d4-0a1c40e78320','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','地道肠',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7f102e54-ed24-5716-bc65-d1a32abf189c','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','蛋挞2个',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c4e3fa30-bb94-5a38-8009-351f75d52eff','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','奥尔良翅根2个',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('46b8c0bc-99bf-5779-aecf-62570435a71d','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','美味薯条',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9dffe5c0-d82a-5c97-9b3c-374512f536a3','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','香芋地瓜丸',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eaafaaeb-120b-5045-9c14-500182e0dcb9','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','香辣孜然鸡米花',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7d81da72-267c-54a6-ba6d-7efa3ba2633e','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','香甜芝士玉米',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cafd4e45-896c-5725-9fb6-a8fecf03c689','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','蛋香培根烤薯条',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('545a64ee-4fa8-5e97-8c84-277c67065dfb','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','鸡扒配薯条',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4953e3c3-073a-53dd-8f02-062db7592ea6','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','烤肠配芝士鸡扒',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e3163a0-6f34-50eb-8fbe-c9978a6b1464','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','2块比萨+1份小吃',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0bfcad18-c276-5362-9acb-066b7c93d80a','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','2块比萨+1份前菜',NULL,23,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d5e6777d-9702-5c36-8a72-195ef134e6bc','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','羊肉烩面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d903fae5-32bc-532a-b67d-04d6e8ac3218','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','羊肉汤',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d95250e7-8719-51a8-b0ef-20d59dfdcb67','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','芝麻烧饼','计价单位：个',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a9d61392-08ea-55bd-9dbf-dfac532ce41a','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','粉条',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4430330c-29e6-5819-ae78-4a78969ac60b','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','粉丝',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bd0cc78a-4a95-5dd9-80ce-15c5405a27a0','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','千张',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('35157bbb-12af-5144-8436-8a338f6adb4c','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','羊肉',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b3629933-418d-5e08-8f86-ad3bd21cf649','e9e15268-0a7e-5650-9e3f-8ff2d9e3ee4c','三杯鸡饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','315086d7-f339-5d26-a030-b3d7ffe7cde1','铁板味道',NULL,'/api/v1/media/3dcbd63f-2bf4-5346-9ced-805bfc8368d7','active',8)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d93ab1e3-4638-5cf6-a5b5-f9aa8888b9ac','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板芝士鸡丁',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fcaabb1b-a6da-5310-adb9-06a07e5a848d','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板咖喱鸡丁',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('04698c04-10b4-5818-b5b6-1ecba9998840','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板黑椒鸡丁',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('52f3e4e1-5da9-5e92-b80d-122dff003aaf','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板照烧鸡排',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fed46ca6-2fa2-54c5-bdff-11b80ed39274','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板照烧牛排',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('489fdef9-afb7-577a-81e4-fd19144fef25','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板咖喱牛肉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fdef8525-2703-5b21-a827-8cf1c8627c76','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板黑椒烧肉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a41eae7c-ad10-589c-bd91-cf84ee5a00d3','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板肥牛饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10219839-2fde-5523-b5fa-9e827d71583b','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板黑椒五花肉',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3572f9ce-e490-5016-ace7-8318026e94a6','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板黑椒牛肉',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('46485213-e903-5444-abdb-4ab0dd719a2d','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','酸辣土豆丝',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('36c5c5bd-3edd-5cb8-9c63-03f79ff5abee','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','香芹苗炒香干',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6646a514-37a3-59b9-bea8-2473b1b4847e','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','茄子豆角炒五花肉',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('017db08b-65b5-5d09-8131-eeb1754104c1','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','螺丝椒炒五花肉',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d732f27-5ebd-56a3-bb4b-e5f7cb61f336','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','豆角炒肉片',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a71913aa-425f-56d0-b1bd-f002bd2b14d0','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','苦瓜炒肉片',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('760b33ad-42e1-55d0-a1f5-1e0452b19ae1','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','木耳炒肉片',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4d1ba3f3-f9dd-5573-a29f-3531fba980fe','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','香菇炒肉片',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('89f7d1bc-d9d0-512f-b4f1-d77afb50da76','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','小炒黄牛肉',NULL,26,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ab703eca-0765-56e6-98b1-46e92e2c4e18','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','冬笋炒牛肉',NULL,26,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('56862670-a007-5b8e-a961-28bff099c62e','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','酸豆角炒牛肉',NULL,26,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('813ecb20-5cf2-59ca-ade0-dcf41795a1a8','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','土豆片炒肉片',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3c2c1077-1142-5da8-9205-6afad67c8f96','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','有机菜花炒肉片',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c9d8cb60-f5c5-5c25-a610-c38fc3bf8b38','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','酸豆角炒肉片',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('403e0f40-369d-5180-8a35-6126dfcd8cd5','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','螺丝椒茄子炒肉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c8ceea3c-747b-5227-8136-fe2a65aa8a84','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','黑木耳炒肉片',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bafc1d7a-2c3e-5b2d-bac1-5f66b11497e1','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','蒜苗炒肉片',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('990554ea-bc00-5746-8aeb-df8e9d72028e','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','腐竹炒肉片',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0141c609-a00f-50c4-8eeb-805fbeaed2a7','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','酸豆角炒五花肉',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('84608713-43cd-5911-9213-34683a7371c5','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','香芹炒五花肉',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b631688f-0f47-5170-bc09-8d980ea3ccb4','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','冬笋炒肉片',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9a475a19-6bc0-5b55-a847-3ffd8bc1bb2a','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板牛肉意面',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('278fe4cf-d01a-5786-a44e-80dfc33601f8','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板意面',NULL,11,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('48774fe3-8b00-5603-bd46-906948c3e9f7','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板鸡排意面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f5517645-07f2-504d-95f5-1f7f077d446b','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板牛排意面',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9b3b29b1-3831-57e3-95bf-dd27ff49ffca','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板里脊意面',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8343148c-3d55-59e3-80f8-75a654865d17','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板烧肉意面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1db88507-1793-53b3-a11e-fcfcc3c596eb','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板肥牛意面',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('68c4469f-741b-5af4-ba02-01762124ac3d','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板五花肉意面',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('56e0dfe3-d396-5649-9dc0-e68f757f0ce1','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板乌冬面',NULL,11,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e755e6d-c306-5a12-8ef9-ad4c22b2c03e','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板鸡肉乌冬面',NULL,13,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e16f8b9-5935-55b1-a1a9-4279a4f54165','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板鸡排乌冬面',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7dcd861b-9a43-54e9-bf80-c83055ffb1e7','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板里脊鸡肉乌冬面',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6615bd71-e44d-5e12-8c6d-130a31f88200','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板牛排乌冬面',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7a08d156-2ae8-5ed1-85fa-b16ca4843591','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板肥牛乌冬面',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f746b056-0cfe-541a-89d2-bce50a291b2e','7ec81f4f-f8df-5d51-8ba9-fdc85d3fe5cc','铁板牛肉乌冬面',NULL,20,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('dc558644-66d7-5e3e-ba3d-774904df5c0f','315086d7-f339-5d26-a030-b3d7ffe7cde1','亲爱的面',NULL,'/api/v1/media/32860cdf-fc7c-59a7-a290-438d46f8f141','active',9)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c3168bd9-dc79-526c-b18b-ff8de046d3db','dc558644-66d7-5e3e-ba3d-774904df5c0f','宜宾燃面（干拌）',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fdd11943-4c08-5e2a-a916-352c4da6855d','dc558644-66d7-5e3e-ba3d-774904df5c0f','生椒牛肉面（干拌）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bfd2bd1e-1da4-5e08-8932-192072383a9f','dc558644-66d7-5e3e-ba3d-774904df5c0f','重庆豌杂面（可干拌/可带汤）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('794fe475-c745-58fc-a89a-f5ac9832f70c','dc558644-66d7-5e3e-ba3d-774904df5c0f','天府牛肉面（带汤）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8a7123ce-c342-5fb2-9c36-974c79e8db56','dc558644-66d7-5e3e-ba3d-774904df5c0f','泡椒牛肉丝面（可干拌/可带汤）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8b75377d-6396-5ff3-bb7b-d5e112972b66','dc558644-66d7-5e3e-ba3d-774904df5c0f','老成都杂酱面（干拌）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26b6300b-fa10-5ca8-8093-6ad1bdfe06f5','dc558644-66d7-5e3e-ba3d-774904df5c0f','酸菜肉丝面（带汤）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1896454-e9ab-5de6-a2f7-9b93f7ec2159','dc558644-66d7-5e3e-ba3d-774904df5c0f','渝香小面（带汤）',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5afde2f6-950b-5ce6-ac06-db2e43e00345','dc558644-66d7-5e3e-ba3d-774904df5c0f','鸡丝拌面（干拌）',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9aec5a7e-ebb1-55e8-b805-ed5e98136f5d','dc558644-66d7-5e3e-ba3d-774904df5c0f','豌杂米线/粉条',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7cb12782-7c5a-55e5-933a-2558c502c687','dc558644-66d7-5e3e-ba3d-774904df5c0f','泡椒牛肉丝米线/粉条',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('12067556-6746-5549-9dbf-d2a2b142007a','dc558644-66d7-5e3e-ba3d-774904df5c0f','酸菜肉丝米线/粉条',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bb120000-7c92-5f67-aff7-e412277d2554','dc558644-66d7-5e3e-ba3d-774904df5c0f','天府牛肉米线/粉条',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a6b2cb5-3e85-5d23-9156-040d96a4e418','dc558644-66d7-5e3e-ba3d-774904df5c0f','酸辣米线/粉条',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c128c9f3-1a1d-5a84-bb36-5aeea49d7236','dc558644-66d7-5e3e-ba3d-774904df5c0f','红油抄手（招牌）',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e524f7c-94cb-58b7-b971-8cb293ba2607','dc558644-66d7-5e3e-ba3d-774904df5c0f','酸菜抄手',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d69c50b9-7be6-56f6-86cb-deb5924b4f0d','dc558644-66d7-5e3e-ba3d-774904df5c0f','清汤抄手',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f80ea2b5-9886-5d4e-815d-aaa0955321fd','dc558644-66d7-5e3e-ba3d-774904df5c0f','干拌抄手',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fb566938-9cf4-51a1-aa4a-09b80f3ce37e','dc558644-66d7-5e3e-ba3d-774904df5c0f','蒜泥抄手',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe24e1f4-08ae-59bb-955a-86bed064e6f0','dc558644-66d7-5e3e-ba3d-774904df5c0f','现炸小酥肉',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1afd479a-d81b-5464-9219-55a3849fdcec','dc558644-66d7-5e3e-ba3d-774904df5c0f','红糖糍粑',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('792dcc65-8a11-5721-89df-4cce94179760','dc558644-66d7-5e3e-ba3d-774904df5c0f','锅巴土豆',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('81a2cc01-423b-593c-ad9d-6af7f92b2028','dc558644-66d7-5e3e-ba3d-774904df5c0f','卤蛋',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('11003f07-72f0-5efc-bd1c-974c3c11e349','dc558644-66d7-5e3e-ba3d-774904df5c0f','淀粉肠',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('27ebf261-f923-58f9-8f79-60d2f1cac8e5','dc558644-66d7-5e3e-ba3d-774904df5c0f','醪糟汤圆',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('98e06f81-cff5-5837-8e23-82cc783f74ed','dc558644-66d7-5e3e-ba3d-774904df5c0f','醪糟荷包蛋',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2fa648f1-db24-5856-ab26-8baec1b1eb50','dc558644-66d7-5e3e-ba3d-774904df5c0f','醪糟蛋花',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('f5317e32-b154-5a8e-9acf-a0d8d155f138','315086d7-f339-5d26-a030-b3d7ffe7cde1','知味·清养膳食',NULL,'/api/v1/media/96be9e0f-7274-5837-9ac2-b6b4f1ec13f6','active',10)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('14fb4a9e-b7ab-5e73-a01f-47aef1ac7225','f5317e32-b154-5a8e-9acf-a0d8d155f138','盐田虾','计价单位：只',1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1ad21c97-51be-50e1-a1d2-08343882c86b','f5317e32-b154-5a8e-9acf-a0d8d155f138','木耳鸡蛋',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('16f709aa-7221-52a1-a8c7-6dc17b969461','f5317e32-b154-5a8e-9acf-a0d8d155f138','香菜丸子','计价单位：个',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6c3d7cd1-2829-5a78-9749-c7060ddab053','f5317e32-b154-5a8e-9acf-a0d8d155f138','芹菜花生米',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bdbfa024-198a-5858-ab2a-3582c1350bb2','f5317e32-b154-5a8e-9acf-a0d8d155f138','清蒸鸡腿','计价单位：个',4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f6d9f46c-4649-57df-91dc-2cdcdfbaef52','f5317e32-b154-5a8e-9acf-a0d8d155f138','冬瓜鸡块',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ad187b06-9006-5dec-ad9d-f6a028671fe7','f5317e32-b154-5a8e-9acf-a0d8d155f138','黑椒鸡腿肉',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fef9db94-163c-5b17-97c8-68915b9c0a8c','f5317e32-b154-5a8e-9acf-a0d8d155f138','山药南瓜',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7fa2946b-d5fc-5362-9036-8eb323e0bdfd','f5317e32-b154-5a8e-9acf-a0d8d155f138','荠菜豆腐',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5d4d7a5a-5f08-51e7-8bb4-990f4c527088','f5317e32-b154-5a8e-9acf-a0d8d155f138','毛豆仁鸡丁',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('464a72b2-febf-5238-9dc9-d48a3bcce827','f5317e32-b154-5a8e-9acf-a0d8d155f138','素什锦菜',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('46d58206-a21e-5b3b-bb63-94260c0dbddf','f5317e32-b154-5a8e-9acf-a0d8d155f138','油菜鸡蛋',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5414cae9-f783-5c5a-9ba9-22e1c03b705b','f5317e32-b154-5a8e-9acf-a0d8d155f138','粉蒸木耳',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a5150c90-636d-5158-913f-b7dde474b374','f5317e32-b154-5a8e-9acf-a0d8d155f138','杂粮饭',NULL,0.3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8a863848-9f54-5790-88a5-8433ed92027e','f5317e32-b154-5a8e-9acf-a0d8d155f138','蒜泥茄子',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9355198e-a6b4-52e0-bde8-57e64ec4475d','f5317e32-b154-5a8e-9acf-a0d8d155f138','红薯','计价单位：个',0.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('70043b6a-2b93-544e-84b4-42fe39962f7a','f5317e32-b154-5a8e-9acf-a0d8d155f138','双椒风味鱼',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8286122d-d3a3-5dc3-b44f-a3abe73431e6','f5317e32-b154-5a8e-9acf-a0d8d155f138','玉米','计价单位：个',1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('cef27f36-a372-5add-8353-5a9b06f541e5','315086d7-f339-5d26-a030-b3d7ffe7cde1','知味·沁香卤味',NULL,'/api/v1/media/9d5debdc-2d6e-5bca-9923-7d960d7157b5','active',11)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e17fb224-a850-5ace-8de8-001ecfa2d0c9','cef27f36-a372-5add-8353-5a9b06f541e5','卤鸭翅','计价单位：个',3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8eda2903-e425-5111-9ca3-03433831ed6d','cef27f36-a372-5add-8353-5a9b06f541e5','猪口条','计价单位：两',4.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('46f07605-f2f0-5122-84f8-7c5c3880eb67','cef27f36-a372-5add-8353-5a9b06f541e5','卤鸭脖','计价单位：个',6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('88ffa2fa-5311-5ca3-b807-dfa1d1698d89','cef27f36-a372-5add-8353-5a9b06f541e5','卤鸡胗','计价单位：两',4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aab56961-0540-55c7-b9a8-70794a649129','cef27f36-a372-5add-8353-5a9b06f541e5','卤鸭头','计价单位：个',6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7524831e-f66d-50d0-b536-db4dc0f89ab5','cef27f36-a372-5add-8353-5a9b06f541e5','猪头肉','计价单位：两',3.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b53a6512-6806-5710-bdef-44e28ecc181d','cef27f36-a372-5add-8353-5a9b06f541e5','酥鱼','计价单位：条',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('38af0ae8-b91b-5efd-96c5-40112071bde1','cef27f36-a372-5add-8353-5a9b06f541e5','猪耳朵','计价单位：两',5.2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b8c330d-ef25-5e4c-a711-24f034d8236c','cef27f36-a372-5add-8353-5a9b06f541e5','鸭锁骨','计价单位：个',4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f4c48599-b00c-5c19-8b71-8252492e2d2c','cef27f36-a372-5add-8353-5a9b06f541e5','素卤类','计价单位：两',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('45db9c80-e1bc-549f-ba09-845c284e9283','315086d7-f339-5d26-a030-b3d7ffe7cde1','知味·主食厨房',NULL,'/api/v1/media/03f965bc-464e-5d57-b9cb-e49616f98aa6','active',12)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4970c9cf-20f0-532b-9321-8ac12f330898','45db9c80-e1bc-549f-ba09-845c284e9283','烧饼夹鸡蛋',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('35532996-2117-59cd-8bcb-b4ae3b3dee7e','45db9c80-e1bc-549f-ba09-845c284e9283','驴打滚',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('788b5ba8-717a-57c4-a1e1-f3982773260a','45db9c80-e1bc-549f-ba09-845c284e9283','紫米团子',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4c62324f-cfd5-59b6-8dd0-078ffc208edf','45db9c80-e1bc-549f-ba09-845c284e9283','素三鲜馅饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('63edb017-2560-5300-b45e-8dad0f1a0700','45db9c80-e1bc-549f-ba09-845c284e9283','青椒火腿饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ece781f2-de9f-5423-aecb-c18855685ba9','45db9c80-e1bc-549f-ba09-845c284e9283','酱香饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('41245f59-177f-54c6-962f-408e2a7ed093','45db9c80-e1bc-549f-ba09-845c284e9283','豆沙饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a171bf3a-0e3e-5292-860d-5dace931490c','45db9c80-e1bc-549f-ba09-845c284e9283','葱花饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('115b93ee-5c2a-5b70-8db7-acf65d5c813f','45db9c80-e1bc-549f-ba09-845c284e9283','枣饼',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a9e71a7b-5c24-51c0-a68e-2c76710f5057','45db9c80-e1bc-549f-ba09-845c284e9283','紫米馅饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a90bccdb-42b3-5409-93cc-e81d4e66f2ef','45db9c80-e1bc-549f-ba09-845c284e9283','豆沙酥',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4d284851-94a5-5e65-936f-d36ba6b53a3f','45db9c80-e1bc-549f-ba09-845c284e9283','麻酱花卷',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('82cac00d-c0a7-5367-97dc-00f585f22423','45db9c80-e1bc-549f-ba09-845c284e9283','紫米馒头',NULL,0.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('627f6350-307f-5c90-98c8-8dbe4a02c31d','45db9c80-e1bc-549f-ba09-845c284e9283','南瓜饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c1655e2b-81b5-540a-8b11-3309e8924559','45db9c80-e1bc-549f-ba09-845c284e9283','花卷',NULL,0.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6a17f029-abfd-5bef-8ad3-62e67c79c3d6','45db9c80-e1bc-549f-ba09-845c284e9283','紫薯馒头',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('33a8d6b7-1095-5391-a074-678d0f17f3a9','45db9c80-e1bc-549f-ba09-845c284e9283','紫薯饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b08f7775-14cd-5309-bc7f-85ca7df04cd4','45db9c80-e1bc-549f-ba09-845c284e9283','三角饼',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3c04e84e-5b3b-5222-b2ad-5a6b717f2835','45db9c80-e1bc-549f-ba09-845c284e9283','紫薯花卷',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9469fd4c-9905-5bad-9cbb-afce3aee4f1b','45db9c80-e1bc-549f-ba09-845c284e9283','肉卷',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c9e0cfcf-8eac-5f20-b32b-74c987527127','45db9c80-e1bc-549f-ba09-845c284e9283','豆包',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('679201ad-dca1-5ec8-9ad4-87bc0ff5eb79','45db9c80-e1bc-549f-ba09-845c284e9283','玉米面发',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ba5f1070-f68f-5a73-b391-e862a72cd174','45db9c80-e1bc-549f-ba09-845c284e9283','馒头',NULL,0.4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('84051ecc-c5b5-58a0-89be-602cf6f0f7dc','45db9c80-e1bc-549f-ba09-845c284e9283','糖饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('c7a5b9a2-230f-5626-921d-8213008a9196','315086d7-f339-5d26-a030-b3d7ffe7cde1','知味·饺香满溢',NULL,'/api/v1/media/e73d0019-97b2-57e2-9a5d-793b25d15304','active',13)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e4bcadf9-3e1b-53ae-bf9b-69a8053161e1','c7a5b9a2-230f-5626-921d-8213008a9196','猪肉白菜','计价单位：个',0.3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('816a65f9-99b6-509f-a9d6-6c86832eb609','c7a5b9a2-230f-5626-921d-8213008a9196','猪肉大葱','计价单位：个',0.3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7055ca4-31b2-5f39-ac7f-5d36195bd2d1','c7a5b9a2-230f-5626-921d-8213008a9196','韭菜鸡蛋','计价单位：个',0.3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('f1b826c6-94a0-5c75-b38e-ab526d013bc9','315086d7-f339-5d26-a030-b3d7ffe7cde1','知味·一碗好面',NULL,NULL,'active',14)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ec51d97a-e322-5ff8-b3cb-613a36ee7c58','f1b826c6-94a0-5c75-b38e-ab526d013bc9','大盘鸡面',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cde23bed-1310-5726-bd35-77eb700389bf','f1b826c6-94a0-5c75-b38e-ab526d013bc9','西红柿鸡蛋面',NULL,4.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('61425b34-86d8-5b8a-aebc-009b5f49d132','f1b826c6-94a0-5c75-b38e-ab526d013bc9','肉沫面',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d4b3ffc-7ff7-5ded-85d7-17b3d05eeacd','f1b826c6-94a0-5c75-b38e-ab526d013bc9','酸菜肉丝榨菜面',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cd66b819-cd2a-553d-b843-79e16327c65c','f1b826c6-94a0-5c75-b38e-ab526d013bc9','肉沫茄丁面',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('752698bc-876c-58aa-b014-bc2fce254830','f1b826c6-94a0-5c75-b38e-ab526d013bc9','木耳鸡蛋面',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dining_halls(id,name,description,status,sort_order)
VALUES ('94f125bc-e647-5e4e-a946-cde15f54cbdc','学四食堂',NULL,'active',3)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('74b2c1d9-df56-5b8d-b382-75c33a71378e','94f125bc-e647-5e4e-a946-cde15f54cbdc','鲁豫鲜炒鸡炒鸡饭/炒鸡面鲜炒排骨煲',NULL,'/api/v1/media/e83f3dc6-db1a-5af2-bab9-2c504439d84c','active',0)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7fbb99fa-1402-5381-95de-eeead0ffd3f9','74b2c1d9-df56-5b8d-b382-75c33a71378e','鲜炒鸡小锅',NULL,38,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5361712f-871c-538f-a6ff-fb0290bfcaca','74b2c1d9-df56-5b8d-b382-75c33a71378e','鲜炒鸡中锅',NULL,54,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('161c01c2-109e-5553-b838-55dfe80ba58c','74b2c1d9-df56-5b8d-b382-75c33a71378e','单人炒鸡饭',NULL,14.9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f71dee4-0e50-5c3e-8bb3-69ee7cd03274','74b2c1d9-df56-5b8d-b382-75c33a71378e','单人炒鸡面',NULL,14.9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c00559f-0d69-507f-86af-f715c25f4572','74b2c1d9-df56-5b8d-b382-75c33a71378e','单人大虾炒鸡饭',NULL,17.9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f807023d-6918-5dfa-a756-252b89fa47f5','74b2c1d9-df56-5b8d-b382-75c33a71378e','单人大虾炒鸡面',NULL,17.9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('767793c2-b056-5ad7-8b44-3684cec4a5bb','74b2c1d9-df56-5b8d-b382-75c33a71378e','单人排骨煲',NULL,17.9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c7f29e5d-02a8-5c38-8ee6-1cd868a4d4c4','74b2c1d9-df56-5b8d-b382-75c33a71378e','单人大虾排骨煲',NULL,22,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('865a8454-26c4-5dfe-a6b3-b62a5afc637b','74b2c1d9-df56-5b8d-b382-75c33a71378e','鲜炒排骨小锅',NULL,48,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('68acda1a-80b6-57ab-b59a-39d8f4ab87b0','74b2c1d9-df56-5b8d-b382-75c33a71378e','鲜炒排骨中锅',NULL,60,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('b279a543-2451-5906-81ff-3567b64e61ca','94f125bc-e647-5e4e-a946-cde15f54cbdc','广式风味秘制私房菜',NULL,'/api/v1/media/b2e0977e-8ff7-55a2-9d69-1fa5bed5e185','active',1)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('af2e2400-0b22-5ea9-8f2c-6a08775a36d3','b279a543-2451-5906-81ff-3567b64e61ca','奥尔良鸡肉饭',NULL,13.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('921c1d5d-6e28-52a5-898c-7a39dc44efda','b279a543-2451-5906-81ff-3567b64e61ca','红油鸡腿饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4bd10dfa-624a-5c0c-b51c-322e848da419','b279a543-2451-5906-81ff-3567b64e61ca','黑椒鸡排饭',NULL,13.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('72f3356e-84cc-52d3-8329-57d6083cb322','b279a543-2451-5906-81ff-3567b64e61ca','手撕鸡饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dab84aec-1dc4-5cab-8298-093852427e19','b279a543-2451-5906-81ff-3567b64e61ca','麻辣鸡排饭',NULL,13.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3596a886-2e00-5ba7-9f3f-b22f77088a8d','b279a543-2451-5906-81ff-3567b64e61ca','脆皮烧鸭饭',NULL,15.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3fc95b3e-d0ff-53ec-ad0d-420c8ceb46a3','b279a543-2451-5906-81ff-3567b64e61ca','脆皮鸡排饭',NULL,13.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2fab95be-16fd-5f37-b1ea-23a6d69c2a3e','b279a543-2451-5906-81ff-3567b64e61ca','隆江猪脚饭',NULL,17.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ea6a7890-b26c-5be7-8536-540ee8805d00','b279a543-2451-5906-81ff-3567b64e61ca','叉烧鸡腿饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1118f8c1-bbfb-53b4-ad86-58a6c14439d7','b279a543-2451-5906-81ff-3567b64e61ca','蜜汁叉烧肉饭',NULL,17.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('418509d2-248b-55f3-a8e4-9f0d5f274676','b279a543-2451-5906-81ff-3567b64e61ca','蜜汁鸡腿饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73eaf9f7-03f9-53e8-b0a4-1cc550a2f4a4','b279a543-2451-5906-81ff-3567b64e61ca','东坡肉饭',NULL,17.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b62046ad-030c-5119-b9fc-f5b77d5c6ab9','b279a543-2451-5906-81ff-3567b64e61ca','脆皮烤鸭腿饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('48db7f6b-0b67-5eba-a7c0-248a293b1afb','b279a543-2451-5906-81ff-3567b64e61ca','脆皮五花肉饭',NULL,18.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('188629e8-7c40-546f-8f94-bb2578f62f2f','b279a543-2451-5906-81ff-3567b64e61ca','湛江白切鸡饭',NULL,14.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7ae12c49-d990-51cc-9be8-f746369fc7d8','b279a543-2451-5906-81ff-3567b64e61ca','双拼饭',NULL,18.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7e27665a-1b95-54c2-8b0b-e98005093243','b279a543-2451-5906-81ff-3567b64e61ca','爽口土豆丝',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aed01353-f092-56ff-bb51-7b2e529234a9','b279a543-2451-5906-81ff-3567b64e61ca','青椒豆芽',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8f96d8c7-8e96-5bdb-97a1-451ce7116dbf','b279a543-2451-5906-81ff-3567b64e61ca','香菇油菜',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('49404986-624a-58f9-84fa-6a510ff06dab','b279a543-2451-5906-81ff-3567b64e61ca','手撕包菜',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7fdce2d7-4b74-5905-a732-6611a14ebf80','b279a543-2451-5906-81ff-3567b64e61ca','西红柿鸡蛋',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a77df6d2-e388-5d1f-b40b-c8830097e8a6','b279a543-2451-5906-81ff-3567b64e61ca','清炒西兰花',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d798159d-b296-5f9e-8284-6c85d0dfe7aa','b279a543-2451-5906-81ff-3567b64e61ca','肉沫麻辣豆腐',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b341f6f2-b16b-51d3-bbbc-216d715ec5f8','b279a543-2451-5906-81ff-3567b64e61ca','肉沫茄子烧豆角',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c9985d9a-428b-5c23-a204-839233ce6b82','b279a543-2451-5906-81ff-3567b64e61ca','鱼香肉丝',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('07406ffa-4620-551c-af34-e26cd8469ea6','b279a543-2451-5906-81ff-3567b64e61ca','京酱肉丝',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cd53feca-ce10-577d-bf24-053c16a64bfe','b279a543-2451-5906-81ff-3567b64e61ca','港式烧鸭',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('010439eb-df85-57be-8699-744d4ac22a0a','b279a543-2451-5906-81ff-3567b64e61ca','韩式年糕鸡',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b2854969-4f7a-5fde-bc0a-143315f2d645','b279a543-2451-5906-81ff-3567b64e61ca','麻辣干锅鸭',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('39fd338a-401e-58ba-be29-37b8a980425e','b279a543-2451-5906-81ff-3567b64e61ca','新疆炒鸡',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d75b736-abf3-5ec7-857f-d34026cb3eb5','b279a543-2451-5906-81ff-3567b64e61ca','粉蒸肉',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('efe7206c-8288-53c3-93b8-833ade381dc1','b279a543-2451-5906-81ff-3567b64e61ca','粉蒸鸡',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('461949e1-4edc-5e6b-9a2c-7ae4fa6d989b','b279a543-2451-5906-81ff-3567b64e61ca','秘制红烧肉',NULL,9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e6c809d3-212a-5dd4-8e8f-25cd109a32fa','b279a543-2451-5906-81ff-3567b64e61ca','老式锅包肉',NULL,9,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('827d95ee-4e25-593d-91c5-e24cfa3c5cfe','b279a543-2451-5906-81ff-3567b64e61ca','香辣排骨',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('944995d8-932a-5dd4-8c1f-b88df4ba0ed8','b279a543-2451-5906-81ff-3567b64e61ca','黄焖肘子',NULL,12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','94f125bc-e647-5e4e-a946-cde15f54cbdc','热卤拼饭',NULL,'/api/v1/media/24fde2a0-7464-50e5-9eaf-0373e3e441d5','active',2)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('beb20806-f783-5f64-90fd-e94e0f46b136','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','小炒干页豆腐',NULL,6.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e79b5d12-74fe-5e84-9246-4edfa3b2344f','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','外婆菜炒鸡蛋',NULL,6.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('576f458c-d2e3-576e-a185-2bb123976fd6','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','香芹小炒肉',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a9feed53-4ba0-51eb-bfa1-11e5fcb2844a','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','农家一碗香',NULL,7,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('52ac00bf-0f9b-517c-a5c3-f7df187acf50','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','湘味花菜炒肉',NULL,7.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e2881148-839e-5050-a84b-d410d26b0255','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','黑椒肉片',NULL,7.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('744c9d3c-64dd-5714-8c29-98fd92b3853e','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','鸡杂酸豆角',NULL,7.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('38f79b5a-8f2b-5cf6-939c-3a71d1b61c9e','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','土豆片炒肉',NULL,7.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('06f9c633-7cc3-56d8-a2cf-a80503e73360','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','豆花肉片',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('79126823-c6fa-5436-a0ac-e64b901d999d','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','麻辣水煮肉',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('24667ad4-8c82-5e8a-b70f-6dc087f8873e','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','孜然脆骨',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f0e0fdaf-90b8-5b53-9181-6103d560ead5','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','椒麻辣子鸡',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4ab48d08-9c49-52ec-912e-bbc9f4bdf4f5','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','湖南小炒肉',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('089ae510-a754-5899-91b7-a7a19521ccea','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','川湘回锅肉',NULL,8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ff280629-dbb8-50dc-b626-cb421902c858','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','椒麻莲藕饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1583705-35d0-5b07-9912-bd5fa609df86','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','椒麻鸡肉饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('19ed3b80-a257-53e6-8ea7-e6c22fd0ad65','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','素双拼饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4911d286-882f-58f5-b890-abbcab99ad8f','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','椒麻鸭肉饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('16403540-f245-5a71-b956-5f3ea5b688ff','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','原味土豆片饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e6aa56e-7e01-5c96-a5d8-e860ab8c4018','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','椒麻香肉饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4470ff57-4939-5430-a602-dbc14410f471','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','原味豆干饭',NULL,10,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ee56e090-c54d-5026-b154-a6639a7ca1c7','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','原味鸡腿饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7e312917-f1a5-528f-90df-d303820ea4ac','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','椒麻肉肠饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0669d88d-e804-5b71-8fd8-417a84f543ca','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','原味鸭腿饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('91dc67f2-c7b5-5746-af80-ae77910a4f37','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','原味鳕鱼饭',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ef2177cb-6ccd-5850-be5a-d73e6324e960','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','原味鸡排饭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('282297e3-e7ac-5bdf-98e1-8a6caaae6a17','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','孜然香酥鸭',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('66247fec-8224-5f9b-8a3e-f8c346cbd298','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','椒麻脆骨饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8b892e58-14ea-59aa-9273-49e3734d1df9','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','红烧肉饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fc355c59-c8de-5484-b4a0-325f6ff36f6e','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','肉双拼饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1a39fbbb-982c-5e8f-84e5-359b586d3625','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','浇汁羊肉饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3a0a8b3a-1b93-57ac-af51-23496a40a1a7','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','招牌卤肉饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('541178e8-8bc4-5762-bf53-4ad174841f21','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','猪肉拼猪肉',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7a790013-6b15-5bd7-86a8-8da5a0965433','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','原味鸭胗饭',NULL,16,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a724e852-17b2-5d6b-9d9b-ab04ea6d5a55','2afd7806-9d5a-5a1f-a76e-5ad4a2bc1d6f','原味肘子饭',NULL,18,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','94f125bc-e647-5e4e-a946-cde15f54cbdc','抄手系列云南鲜汤生烫米线',NULL,'/api/v1/media/5118882c-bcb3-58a8-a612-6af71fce4bdd','active',3)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('de255eb9-0636-5cdd-bf1d-dba392643c62','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','清汤抄手15元12元（大份）','大小份',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e2ea972-fed5-5c7f-88e3-046086c26175','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','清汤抄手15元12元（小份）','大小份',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f0fb509-551a-591a-b60d-4c337fc8025a','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','麻辣抄手15元12元（大份）','大小份',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('88d7d1bb-1711-5fc1-a939-50fe5a071814','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','麻辣抄手15元12元（小份）','大小份',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('62785681-4e4d-5a23-8d9d-1c2da1b80a69','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','干拌抄手15元12元（大份）','大小份',15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('554ede6c-f759-5390-b875-5ca9c59947fb','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','干拌抄手15元12元（小份）','大小份',12,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f1cc44e1-cbce-5ba0-90ff-a2b27e6e35f2','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','招牌吊龙米线',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('378c916f-e24d-5c03-8a95-ed8e93e57f7c','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','生烫羊肉片米线',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ff858d2c-5cf7-534e-8f59-5624dd62bfd7','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','红烧牛肉米线',NULL,17,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7aa67d38-caff-5d81-bd8a-215b0e0871de','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','生烫梅花肉米线',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d58e91e7-f7e6-5941-918e-490802869804','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','泡椒鸭胗米线',NULL,15,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a52d6543-995c-535c-843c-1d0abdf881e6','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','云南炸酱米线',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7cf9c85f-69b6-5a9b-b32c-2264799d3dbd','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','牛肉丸米线',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f7521c65-277e-542a-9039-a3fa8d03bd03','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','生烫鱿鱼花米线',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f53aee9-2cc5-5c2b-bc48-eb5074fb4425','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','泡椒鱿鱼花米线',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('da5f707a-7750-507e-a196-b92d6f27dd47','cb619a12-ec6c-5ac2-9bad-c6e9cf38178b','泡椒炸酱米线',NULL,14,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('e364decd-3653-55e8-a31c-242e72e83e60','94f125bc-e647-5e4e-a946-cde15f54cbdc','减脂餐',NULL,'/api/v1/media/025e27d1-b6f7-5d53-8b4d-13ff97f6cb82','active',4)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d2bd84e-4789-5dd1-ac39-47bc0254c0d5','e364decd-3653-55e8-a31c-242e72e83e60','盐田虾','计价单位：只',1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f6548ac8-ca48-5d24-ae4d-78824f54ab83','e364decd-3653-55e8-a31c-242e72e83e60','蒸芹菜叶',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fa481e20-fe3a-5837-b711-8357819c9fb8','e364decd-3653-55e8-a31c-242e72e83e60','蒸木耳',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('559eabb4-a610-558e-81e3-b028a0454bca','e364decd-3653-55e8-a31c-242e72e83e60','蒸土豆丝',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5165d5f3-b802-51f2-8e33-db51aed28af1','e364decd-3653-55e8-a31c-242e72e83e60','蒸胡萝卜丝',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('851d9be9-1da8-556d-90c0-96e4d716662d','e364decd-3653-55e8-a31c-242e72e83e60','蒸豆角',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('308b9f4d-fe4e-5aa0-b53e-fbb46b1d4462','e364decd-3653-55e8-a31c-242e72e83e60','蒸海带丝',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('39587f5b-c07e-5840-91ad-27b4fce6692f','e364decd-3653-55e8-a31c-242e72e83e60','玉米粒',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ff8b3378-faa1-5eac-9912-78e101c3be30','e364decd-3653-55e8-a31c-242e72e83e60','西蓝花',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('72f5eadc-6a65-53dc-a7e2-e12eabf7d608','e364decd-3653-55e8-a31c-242e72e83e60','蒸蒿子秆',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1bc5199f-d21f-50b3-b84a-3ebc0a5c9642','e364decd-3653-55e8-a31c-242e72e83e60','鸡丝',NULL,6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a0b550a-acf4-50ef-86b5-a1e57a6e0e9e','e364decd-3653-55e8-a31c-242e72e83e60','酱牛肉','计价单位：两',8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('51ba753d-7383-5928-882a-bc026d5ea190','e364decd-3653-55e8-a31c-242e72e83e60','卤豆干','计价单位：块',2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a990506f-bad9-5451-8aa3-9e83a528cee6','e364decd-3653-55e8-a31c-242e72e83e60','煎鸡蛋','计价单位：个',1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ccc85057-8d44-5a93-9abf-0b84bef2a755','e364decd-3653-55e8-a31c-242e72e83e60','玉米/南瓜','计价单位：根',0.8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d8c5b872-ce8c-5562-a179-be78fc06e2ef','e364decd-3653-55e8-a31c-242e72e83e60','莜面蒸饺','计价单位：个',1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe597489-747c-57ec-97ff-abe02bca84d4','e364decd-3653-55e8-a31c-242e72e83e60','白灼虾','计价单位：只',1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('50a3c661-fa32-5d40-b331-0ea8c7e64d08','e364decd-3653-55e8-a31c-242e72e83e60','大份',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f6f81b9-21ce-5a87-af2b-ff2cda604041','e364decd-3653-55e8-a31c-242e72e83e60','小份',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('68b863c3-e453-5c06-974d-5ed5fa510349','e364decd-3653-55e8-a31c-242e72e83e60','酱牛腱','计价单位：两',8,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0e06ea0b-a557-520b-9ae6-99468d2561a8','94f125bc-e647-5e4e-a946-cde15f54cbdc','主食早餐',NULL,'/api/v1/media/11fd2178-e16d-5dda-ad81-12388a91ec60','active',5)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0f718d72-8395-5fe3-87e0-375f6cf3f9fa','0e06ea0b-a557-520b-9ae6-99468d2561a8','思源水',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a296ca9b-b291-5f9a-a833-7c2665c34268','0e06ea0b-a557-520b-9ae6-99468d2561a8','东方树叶',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0dbbb16c-d9ae-59f3-9723-a6e231cf7f7e','0e06ea0b-a557-520b-9ae6-99468d2561a8','可口可乐',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f408327e-51f5-513f-8f3c-a95ff14c57fb','0e06ea0b-a557-520b-9ae6-99468d2561a8','果粒橙',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6cc51963-06d9-59ad-be9c-fbf46af48fda','0e06ea0b-a557-520b-9ae6-99468d2561a8','茶π',NULL,5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('797f900e-26fa-5d00-939d-51fd02007fff','0e06ea0b-a557-520b-9ae6-99468d2561a8','百岁山',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b8643838-5545-5ebc-9300-a83852875d11','0e06ea0b-a557-520b-9ae6-99468d2561a8','红茶',NULL,3.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d9601f6-a680-540c-be05-e31d108c753b','0e06ea0b-a557-520b-9ae6-99468d2561a8','绿茶',NULL,3.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5f0b1acf-e08a-5968-9bf2-244358952993','0e06ea0b-a557-520b-9ae6-99468d2561a8','白菜鸡蛋',NULL,0.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('07b607a4-8bfb-5e7c-9c65-49955d4603e6','0e06ea0b-a557-520b-9ae6-99468d2561a8','玉米',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ce6886cd-10a4-56aa-bd52-35683cc24e59','0e06ea0b-a557-520b-9ae6-99468d2561a8','胡萝卜鸡蛋',NULL,0.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b8a0e70-b6d9-5058-8986-99bf5ffacfdc','0e06ea0b-a557-520b-9ae6-99468d2561a8','驴打滚',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c27a86f1-7d45-59b0-8302-d05290fd4b84','0e06ea0b-a557-520b-9ae6-99468d2561a8','荠菜鸡蛋',NULL,0.6,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('19ef1dc0-155b-5599-9c02-052b2454c717','0e06ea0b-a557-520b-9ae6-99468d2561a8','麻酱饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c140d72d-4f51-5511-abdb-b98b41e15cd3','0e06ea0b-a557-520b-9ae6-99468d2561a8','白菜肉包',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fa93cce6-9236-5046-b719-efb507b836e5','0e06ea0b-a557-520b-9ae6-99468d2561a8','糖火烧',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5d3d9736-990e-5e53-9b94-e4a85d2e0db1','0e06ea0b-a557-520b-9ae6-99468d2561a8','豆角肉包',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c514d826-5c77-5636-bfbb-284e68ad1183','0e06ea0b-a557-520b-9ae6-99468d2561a8','煎饺',NULL,0.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5fc65964-7feb-53e6-9f6f-563e2848aeb4','0e06ea0b-a557-520b-9ae6-99468d2561a8','茄子肉包',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('25b031c6-f241-5ee2-9929-3e160597f72e','0e06ea0b-a557-520b-9ae6-99468d2561a8','手抓饼',NULL,2.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1def916c-c879-5ab1-b803-2916ac991367','0e06ea0b-a557-520b-9ae6-99468d2561a8','酱肉包',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6f4ca9d7-e500-56ef-94cd-0554b4ba4f85','0e06ea0b-a557-520b-9ae6-99468d2561a8','火腿花卷',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('59d66e73-5f78-5dbd-aa12-080bbdd33d8b','0e06ea0b-a557-520b-9ae6-99468d2561a8','油条',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3bcf57c1-bf7b-5459-8f0d-9a284ea8df3e','0e06ea0b-a557-520b-9ae6-99468d2561a8','火腿煎蛋',NULL,2.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c87fe026-45ea-5127-be71-ee530e5da8f7','0e06ea0b-a557-520b-9ae6-99468d2561a8','葱花饼',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('66d7c125-aacd-5848-a968-5f7cba5a5d4c','0e06ea0b-a557-520b-9ae6-99468d2561a8','披萨',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a1123fc0-8eea-5631-bcd7-48b6a473f2cb','0e06ea0b-a557-520b-9ae6-99468d2561a8','馒头',NULL,0.4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e6875ab7-3d57-50fc-be5c-b102e61446dc','0e06ea0b-a557-520b-9ae6-99468d2561a8','抹茶卷',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0816d297-6a7b-57dc-948e-1a88e6580a66','0e06ea0b-a557-520b-9ae6-99468d2561a8','花卷',NULL,0.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f2bab703-cff6-5a99-9f01-fb46f9376fa9','0e06ea0b-a557-520b-9ae6-99468d2561a8','麻团',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe9356d6-2b5c-5d5c-986b-b861adb727c8','0e06ea0b-a557-520b-9ae6-99468d2561a8','烧饼夹腿排',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('99777926-96a7-5648-be04-301d18c68d05','0e06ea0b-a557-520b-9ae6-99468d2561a8','大烤肠',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('01ff884f-c31f-52f0-bad3-20ae882a7b51','0e06ea0b-a557-520b-9ae6-99468d2561a8','酱香饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c5ff921f-68b2-50ef-b5b8-7f3a8ed2eb27','0e06ea0b-a557-520b-9ae6-99468d2561a8','紫薯包',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d60459cc-5c76-5b6e-b485-54a32ab1ac7c','0e06ea0b-a557-520b-9ae6-99468d2561a8','生煎包',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0928585d-9a90-5382-b374-df570674fca0','0e06ea0b-a557-520b-9ae6-99468d2561a8','双夹饼',NULL,2.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b385c192-0556-5f4d-8968-a58b8d9fe6ed','0e06ea0b-a557-520b-9ae6-99468d2561a8','玉米饼',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('270c13b2-afd8-59c8-ad8b-325469a343b3','0e06ea0b-a557-520b-9ae6-99468d2561a8','土豆丝卷饼',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e1745922-0f30-598f-8036-fa479afc6b1b','0e06ea0b-a557-520b-9ae6-99468d2561a8','紫米发糕',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('394b4c10-77fe-5880-a5a6-235c8561d706','0e06ea0b-a557-520b-9ae6-99468d2561a8','蛋挞',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e58882a9-836b-5bf7-9c27-adb31496e962','0e06ea0b-a557-520b-9ae6-99468d2561a8','牛肉饼',NULL,4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3d2f6f58-d872-50b9-88a9-18287934f1b6','0e06ea0b-a557-520b-9ae6-99468d2561a8','蛋糕卷',NULL,3.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a688504e-a40e-55ca-9b26-7165ef3b0ffe','0e06ea0b-a557-520b-9ae6-99468d2561a8','糖饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('08597600-3cf7-5f80-9cba-f64b00b47a4c','0e06ea0b-a557-520b-9ae6-99468d2561a8','荠菜团子',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('27f45c0a-a365-5b2b-93ee-fc50496a0906','0e06ea0b-a557-520b-9ae6-99468d2561a8','肉饼',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('64395a07-5f4b-50ae-bef9-53d1cbf37359','0e06ea0b-a557-520b-9ae6-99468d2561a8','面包片',NULL,1.4,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fed778d6-a2aa-56b5-84a6-2d5c286b3203','0e06ea0b-a557-520b-9ae6-99468d2561a8','韭菜盒子',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4931696d-532f-5afb-845c-a4a9ad276921','0e06ea0b-a557-520b-9ae6-99468d2561a8','果酱面包',NULL,2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4bd715ba-b64e-544c-82bd-e6f03862fba6','0e06ea0b-a557-520b-9ae6-99468d2561a8','菜角',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e0e3af8-7b49-5057-b7e4-2e9e6bede20c','0e06ea0b-a557-520b-9ae6-99468d2561a8','烧麦',NULL,1.2,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5563074-bab9-5bb7-99f7-ca6b34d56280','0e06ea0b-a557-520b-9ae6-99468d2561a8','红丝绒蛋糕',NULL,3.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d68dd3cf-da82-5055-82c7-b6cf229057c0','0e06ea0b-a557-520b-9ae6-99468d2561a8','双薯饼',NULL,1.5,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('be3710ab-f003-5098-8bf8-bbcc7f480d02','0e06ea0b-a557-520b-9ae6-99468d2561a8','糖三角',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5f1ffb6f-3457-50ca-9ebf-1fff63b37729','0e06ea0b-a557-520b-9ae6-99468d2561a8','豆沙包',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d9a939c-90d2-5eec-bb72-9ecaf8c254a2','0e06ea0b-a557-520b-9ae6-99468d2561a8','椒盐饼',NULL,1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f6618a44-40ba-502c-95d1-1b65fe70b9bb','0e06ea0b-a557-520b-9ae6-99468d2561a8','早餐奶',NULL,2.1,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4b7d5b32-bc12-5446-94b8-a3fd02f1957d','0e06ea0b-a557-520b-9ae6-99468d2561a8','纯牛奶',NULL,3,'active')
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9c7dab40-0405-5cd1-9984-a869225ab26f','0e06ea0b-a557-520b-9ae6-99468d2561a8','豆浆(杯)',NULL,2.5,'active')
ON CONFLICT DO NOTHING;
REFRESH MATERIALIZED VIEW foodtime.dish_rating_summary;

