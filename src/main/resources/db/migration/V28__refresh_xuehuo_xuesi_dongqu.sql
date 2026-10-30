-- 学活按楼拆厅 + 学四/东区换新目录：只 INSERT 与食堂排序 UPDATE，绝不 DELETE。
-- 老学活厅行不碰，由后续删除脚本处理；本迁移幂等，可重放。
-- UUID 确定性复现：uuid v5(NAMESPACE_URL)，key 形如
--   foodtimeNeo:hall:<厅名> / foodtimeNeo:stall:<厅名>:<档口名> /
--   foodtimeNeo:dish:<厅名>:<档口名>:<菜品名>[#n] / foodtimeNeo:media:<厅名>:<档口名>:<0起序号>。
-- 同档口重名菜品第 2 次起 key 后缀 #n（共 11 组）。
-- 价格规则v2：冒号后取数；“个/num元”取num；“N 个 M 元”取M；段内首个“数字+元”；
-- 无数字的 3 条以 0 入库：米饭/汤按免费供应；麻辣香锅改名（待核价）；
-- 酸汤水饺改名（+2元，基础价待核）；其余原价待核价见简介。
-- 重庆小面/牛肉面（汤拌）疑似下架，已整行删除（见 docs/canteen-refresh-batch2.md）。
-- 档口 cover 取该档口第 0 张图（无图则 NULL）；图集表含全部 43 张图。
-- 图片字节：seed-images/v28/<media-uuid>.jpg。
-- Use schema-qualified names so this migration is independent of search_path.

-- 一、食堂排序：留园0 / 明湖1-3 / 学活一楼4 / 学活二楼5 / 学四6 / 东区7 / 学苑8。
UPDATE foodtime.dining_halls SET sort_order=6, updated_at=now() WHERE id='94f125bc-e647-5e4e-a946-cde15f54cbdc';
UPDATE foodtime.dining_halls SET sort_order=7, updated_at=now() WHERE id='955c1f92-3671-5d13-8566-81911b5a5a7b';
UPDATE foodtime.dining_halls SET sort_order=8, updated_at=now() WHERE id='78fa7f63-f52f-524f-9b28-f55fb441a1f0';

-- 二、新增 2 个楼层厅：封面复用原学活封面。
INSERT INTO foodtime.dining_halls(id,name,description,status,cover_image_url,sort_order)
VALUES ('af829558-1410-503a-a426-e1d7ba2a399d','学活一楼','原学活食堂按楼层拆分','active','/api/v1/media/bfb6d4cc-85e6-5687-a3f8-233d7ffe7cde1',4)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dining_halls(id,name,description,status,cover_image_url,sort_order)
VALUES ('9bc79377-bb58-54ff-8b51-4cfb67411d37','学活二楼','原学活食堂按楼层拆分','active','/api/v1/media/bfb6d4cc-85e6-5687-a3f8-233d7ffe7cde1',5)
ON CONFLICT (id) DO NOTHING;

-- 三、39 个档口：cover 为该档口第 0 张图，无图则 NULL。
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','955c1f92-3671-5d13-8566-81911b5a5a7b','云南鲜烫牛肉米线',NULL,'/api/v1/media/8e29a5d5-a7c2-5685-b0e0-8cc0a8c3b437','active',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('2e1baa15-bcce-56a4-9bac-ab1710703001','955c1f92-3671-5d13-8566-81911b5a5a7b','臻腊饱｜粤桂美食（广式烧腊·笼仔饭）',NULL,'/api/v1/media/f7c53ed9-6619-582f-bee4-1da580181ba3','active',2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('92f5e77d-ace0-5843-a9e7-f9a05aec4fba','955c1f92-3671-5d13-8566-81911b5a5a7b','滑蛋饭｜土豆拌饭',NULL,'/api/v1/media/5ba6183d-cb68-513a-aa3d-a298aac9d6aa','active',3)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('86c177b3-6573-5e79-a18b-24341725ef1a','955c1f92-3671-5d13-8566-81911b5a5a7b','卤肉饭',NULL,'/api/v1/media/5848513b-83fa-5a4a-b4aa-56bf6f28e7d3','active',4)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','955c1f92-3671-5d13-8566-81911b5a5a7b','大唐小二｜牛肉刀削面',NULL,'/api/v1/media/41d11bcd-d662-5bca-b8b7-a80c7e499e7e','active',5)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','955c1f92-3671-5d13-8566-81911b5a5a7b','金日升｜烤盘饭',NULL,'/api/v1/media/516794f4-99b9-55e0-ad88-d4bebb558a8d','active',6)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('8f31fd77-010e-5c63-8607-f99db544939d','955c1f92-3671-5d13-8566-81911b5a5a7b','肉朵香｜小坛焖肉',NULL,'/api/v1/media/844cfe48-58ed-5d6a-8f39-b8db99aaf5ac','active',7)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('385ec11a-5ffe-5022-ae2a-ed9a32c83e17','955c1f92-3671-5d13-8566-81911b5a5a7b','嗨族｜牛肉粉面',NULL,'/api/v1/media/d0ffcd14-eeec-53a6-be82-2fb8fb8d6463','active',8)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('c3f07056-e5a9-50f3-9a0f-7187b34afe7a','955c1f92-3671-5d13-8566-81911b5a5a7b','馋大八｜江西小炒',NULL,'/api/v1/media/70938423-7310-59c1-9345-1ffd6396a831','active',9)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('43643b96-1419-59dc-9dff-6e9605f6bb09','955c1f92-3671-5d13-8566-81911b5a5a7b','重庆小面',NULL,'/api/v1/media/dd04abb8-2512-52e2-bde2-4081a3cb1b96','active',10)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('21622451-9645-5fdf-a5d3-a52e6f8e45d4','955c1f92-3671-5d13-8566-81911b5a5a7b','三哥｜小炒盖饭',NULL,'/api/v1/media/ac8b97fa-9ab9-55f4-ae32-6eafc5fc1fb1','active',11)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('96535f3f-b7c9-5cf8-9bfd-67192c8370f7','955c1f92-3671-5d13-8566-81911b5a5a7b','交大小笼包｜手工水饺·云吞方便面',NULL,'/api/v1/media/35093422-3144-50ce-b12a-04f0a5674976','active',12)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('1683fa82-5238-5bfe-9f08-adb9a007ee29','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 饺香四溢窗口',NULL,'/api/v1/media/5e257677-29f3-5696-ad8b-4453f4c55f98','active',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('e605d74f-8c09-5571-b2c7-d64c4a281b2c','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 一碗好面窗口',NULL,'/api/v1/media/8b98b987-489e-5b16-8a36-a73d5e387906','active',2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('1e949237-63e6-5adb-b74b-413eb635d6e2','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 轻养膳食菜肴',NULL,'/api/v1/media/3ad8f7cc-40e4-53a3-b21b-de80b66bcc5a','active',3)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 鲜蒸佳肴',NULL,'/api/v1/media/699f584c-f765-528e-b298-425c3055b8f9','active',4)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('3af1365e-ce72-5e82-918b-514f771e997c','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 炒色炒香',NULL,'/api/v1/media/cb98e511-977b-5ca3-ad67-caf3a2a8dfcd','active',5)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('a4ab5dcf-4895-5f8e-a231-a3f9fd029049','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 砂锅窗口',NULL,'/api/v1/media/f2c9c3aa-1cf5-5469-8b0b-d8ee768f442d','active',6)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('f78e6f30-bdcd-5f67-a06a-631ae9a459fc','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 湖南菜',NULL,'/api/v1/media/3f602d2b-483c-54ab-8c56-192c9d69f6ce','active',7)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('3055b9d0-387e-57d7-9446-70d3c08a005a','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 私房小厨',NULL,'/api/v1/media/46f22db0-bf87-5734-8801-26fb270428a0','active',8)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('848292e4-ffde-5abb-8b73-59b7f8975320','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 暖锅炖菜',NULL,'/api/v1/media/d6ef0bca-c172-57e1-9373-d2732976db50','active',9)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('acda8dd1-08c3-5b9a-85c5-10339fca3927','af829558-1410-503a-a426-e1d7ba2a399d','特色风味｜知味 · 鲜汁盖饭',NULL,'/api/v1/media/fe6a8381-7d13-55f2-b211-73cec28798ae','active',10)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('6e5a215e-4fa6-5701-afaf-789f2c8197d5','af829558-1410-503a-a426-e1d7ba2a399d','基本伙｜知味 · 家常百味',NULL,'/api/v1/media/27b4e4d1-3b6d-55ff-befe-e44698592c3f','active',11)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('f989e7cc-b64e-54b6-bd22-1eadeffd02a0','9bc79377-bb58-54ff-8b51-4cfb67411d37','广式味道',NULL,'/api/v1/media/1801985d-6948-52ca-9647-62091eb4b929','active',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('57c1c534-f550-50f9-b599-5521b590f18d','9bc79377-bb58-54ff-8b51-4cfb67411d37','重庆味道',NULL,'/api/v1/media/60aa4589-49b4-5719-979e-6245c1daeb16','active',2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('872d34f7-3d76-5199-8618-cc17eb5370de','9bc79377-bb58-54ff-8b51-4cfb67411d37','西北味道',NULL,'/api/v1/media/5e4a3bb0-0b2f-5127-a77d-860460f02557','active',3)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('82b5557f-7d9b-5276-881a-9ddb8c4a12b8','9bc79377-bb58-54ff-8b51-4cfb67411d37','四川味道',NULL,'/api/v1/media/0c4dc48c-8fc4-5a26-8ae4-7f8ef2ea3fcc','active',4)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','9bc79377-bb58-54ff-8b51-4cfb67411d37','南北味道',NULL,'/api/v1/media/9d85ebf6-6b89-51cd-b15d-4da35d5ff2ae','active',5)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0d5288b0-b521-5b91-a419-8e019adc2b37','9bc79377-bb58-54ff-8b51-4cfb67411d37','土耳其味道',NULL,'/api/v1/media/b01fe15b-1766-5898-baa7-3e276bc091f6','active',6)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('cdf98f90-9fca-56e5-8a0b-393265d055c7','9bc79377-bb58-54ff-8b51-4cfb67411d37','江南味道',NULL,'/api/v1/media/b10bffbe-db6c-5c53-a3dd-9fee097e1249','active',7)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','9bc79377-bb58-54ff-8b51-4cfb67411d37','川湘味道',NULL,'/api/v1/media/5d0bc613-fdf1-5800-8daa-ece5da0e58c8','active',8)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('05bf06ac-11df-5e8f-852d-6e9fa7fa8072','9bc79377-bb58-54ff-8b51-4cfb67411d37','铁板味道',NULL,'/api/v1/media/0c669a3f-e0fb-5209-b1c4-195c1567ff82','active',9)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','9bc79377-bb58-54ff-8b51-4cfb67411d37','亲爱的面',NULL,'/api/v1/media/1509d461-2295-5d08-b3e7-cd979d0c151e','active',10)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('903d4bfb-d232-5394-b99f-052983a41abc','94f125bc-e647-5e4e-a946-cde15f54cbdc','鲁豫鲜炒鸡 炒鸡饭/炒鸡面 鲜炒排骨煲',NULL,'/api/v1/media/ec57ab52-6ada-5530-9b8e-0e352c8c2b46','active',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0cf77375-5b1a-51e5-bd34-12024315133d','94f125bc-e647-5e4e-a946-cde15f54cbdc','广式风味 秘制私房菜',NULL,'/api/v1/media/084c6517-7c4b-5d80-85e1-7c32368700fe','active',2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','94f125bc-e647-5e4e-a946-cde15f54cbdc','热卤拼饭',NULL,'/api/v1/media/1e42fb8a-67fa-5efb-a24f-2bb7a92fc3c6','active',3)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('468eae22-d558-5a7e-891a-a21ae01f6c42','94f125bc-e647-5e4e-a946-cde15f54cbdc','抄手系列 云南鲜汤生烫米线',NULL,'/api/v1/media/ec206ee4-3706-5274-84e1-d4e116e1d878','active',4)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('78eff9cc-c97d-539b-95b6-a7f113f3beed','94f125bc-e647-5e4e-a946-cde15f54cbdc','减脂餐',NULL,'/api/v1/media/6005360f-4e0c-5771-a42d-0df980a0acbb','active',5)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','94f125bc-e647-5e4e-a946-cde15f54cbdc','主食早餐',NULL,'/api/v1/media/6695ab94-ad2c-543d-a73e-0ed308ea3072','active',6)
ON CONFLICT (id) DO NOTHING;

-- 四、全部菜品：免费供应/待核价见简介。
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fece0457-a544-558b-8756-1df5982f3ba0','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','牛骨汤米线/面',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('69746f4b-bee2-5b41-a740-a9e5c2514430','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','招牌肉酱米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('21e12541-0736-514a-86f4-a738691571cc','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','日光番茄米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eaf75a50-051d-5237-871e-8a80677d219d','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','山野泡椒米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('51bb58e1-7a62-51d3-a306-a858d1e2c6b8','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','香辣骨汤米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('89780e7d-18c4-5cf3-b9dd-d4e973dfcefd','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','劲爽麻辣米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2b395c79-5fce-5a7b-b0ef-41189e102344','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','醇香麻酱米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6755a838-ba4a-5fc7-985f-4c9e1c1173e1','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','金汤酸辣米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('38f0fd41-5c37-5243-8b17-caf49af22877','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','酱卤牛肉米线/面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0dc5ad45-e6a6-5f10-bf0a-e02ac3a69c1d','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','招牌肉酱干拌米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('71708625-7697-5004-b56d-c9a59f66139c','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','云贵烧椒干拌米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e662999-8e94-5873-bf93-f2c78466fd48','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','剁椒番茄干拌米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e515a9d8-816e-5f0f-8f93-eaaeb7213499','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','把子肉',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('25d7d33e-3f17-52c0-b69a-d4ec0116932f','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','鸡腿',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('351f588d-3048-5d07-98df-bdde330f716a','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','吊龙',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9faa9b6b-4d66-5de6-8f4a-d977aba476cc','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','黑椒肠',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f98b3353-a687-5a85-8199-d811aa8c4c23','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','肉丸子',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a06376a1-d528-572c-b0ee-2d86af7eb5ca','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','鸡柳',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c0accf0d-4054-5940-b820-c20cfe25eb26','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','豌豆尖',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('363874d0-249b-5b82-9bb6-41260750cb0c','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','响铃卷',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b893e56d-5b3c-509c-840e-b292789a5e1a','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','炸蛋',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d31fe969-cdf9-5ac0-bf83-fe618ef07712','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','煎蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fc1d59f8-643a-56fc-b88b-7c7ea09ca8d0','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','卤蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3a7dc3f0-38a6-5289-8757-c7ff9b0edf5f','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','烤肠',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c35d076f-e73e-5d12-8ad0-a4f571df5d18','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','豆干',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c61a0daf-63de-5fb8-a2d3-ca63298d3d78','2e1baa15-bcce-56a4-9bac-ab1710703001','豆豉排骨蒸饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3bc34c9f-3df6-5b7f-9694-42509211310f','2e1baa15-bcce-56a4-9bac-ab1710703001','原味牛腩蒸饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('184134e9-dbaa-5b2f-a1ad-cec2b9072042','2e1baa15-bcce-56a4-9bac-ab1710703001','鸡蛋肉饼蒸饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('24b2c7a9-dcd6-5303-a62e-57c1b382e035','2e1baa15-bcce-56a4-9bac-ab1710703001','广味腊肠笼仔饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('77f6d439-2886-59db-89f5-e99d2aabfaa5','2e1baa15-bcce-56a4-9bac-ab1710703001','鸡排笼仔饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b0adea1d-1dad-5f94-8e07-cfb982394873','2e1baa15-bcce-56a4-9bac-ab1710703001','鸡腿笼仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('92fa4a74-bd34-5bad-bbe6-1e6add8af5b2','2e1baa15-bcce-56a4-9bac-ab1710703001','烧鸭笼仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80489539-1b16-574c-97d3-ff64dd9d36fe','2e1baa15-bcce-56a4-9bac-ab1710703001','叉烧笼仔饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3da4dd78-6f73-5698-87c2-624a4bf9ccb2','2e1baa15-bcce-56a4-9bac-ab1710703001','煎鸡蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5dfc4f65-486b-58f4-b569-7043c1b4615d','2e1baa15-bcce-56a4-9bac-ab1710703001','广味腊肠',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4fdfd638-a3f9-536f-9b9b-466b0eff8a8b','2e1baa15-bcce-56a4-9bac-ab1710703001','肉肠',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9f825f0d-398e-5ac0-9021-148cce186e78','2e1baa15-bcce-56a4-9bac-ab1710703001','单加肉类',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('76e6336e-6c81-579d-9e5b-ea0983b4dbf6','2e1baa15-bcce-56a4-9bac-ab1710703001','鸡腿',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d8e0229b-389b-535c-af8b-256fbace7cfd','2e1baa15-bcce-56a4-9bac-ab1710703001','鸭腿',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7fefbd6e-c6ea-5525-bf5b-e33400610bb0','2e1baa15-bcce-56a4-9bac-ab1710703001','招牌烧鸭饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('68fd93a6-596a-5b07-a5a5-50f39f5507d2','2e1baa15-bcce-56a4-9bac-ab1710703001','脆皮鸭腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b850fb7-6cb8-5cfc-a859-0ca4ebff0728','2e1baa15-bcce-56a4-9bac-ab1710703001','老娘叉烧饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('05258c3a-8337-5f8d-9ebd-0abc96e53f77','2e1baa15-bcce-56a4-9bac-ab1710703001','迷你手撕鸡饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c6592b7-652a-5590-ac6a-76d4249291a7','2e1baa15-bcce-56a4-9bac-ab1710703001','香辣烤鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ff311ad5-5985-5c41-94df-bdc954af2c4e','2e1baa15-bcce-56a4-9bac-ab1710703001','麻辣口水鸡饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9c5bcb30-afb5-596f-a4c3-26c333e4f42f','2e1baa15-bcce-56a4-9bac-ab1710703001','蜜汁鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5905db63-e70d-50c4-81fb-2d3c70c42dd5','2e1baa15-bcce-56a4-9bac-ab1710703001','烧汁土豆肉卷饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b863308c-26c5-5f8e-ba6e-261dc4556c14','2e1baa15-bcce-56a4-9bac-ab1710703001','脆皮鸡排饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3ab9a4df-0515-5882-b90b-2021d34c8097','2e1baa15-bcce-56a4-9bac-ab1710703001','葱姜白切鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2bc670ac-d93f-5892-8d67-5a5b53f53ac0','2e1baa15-bcce-56a4-9bac-ab1710703001','照烧鸡排饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0d3a0ac6-df7d-579a-8c37-6261688c93a5','2e1baa15-bcce-56a4-9bac-ab1710703001','黑椒鸡排饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('51a9c5f4-ec6b-5eb4-9dad-ec230563571e','2e1baa15-bcce-56a4-9bac-ab1710703001','广味腊肠饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ba6dfd15-f724-587a-8aab-fd8a9ba28bae','2e1baa15-bcce-56a4-9bac-ab1710703001','脆皮五花肉饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e00edc5-170a-58f7-8eab-3cb81d81ddb1','2e1baa15-bcce-56a4-9bac-ab1710703001','隆江猪脚饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9de5e723-fee8-5707-8e12-e6c77c2f9dff','2e1baa15-bcce-56a4-9bac-ab1710703001','任意双拼饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ec393ed3-59e4-5107-88bf-d3676f57aa2f','2e1baa15-bcce-56a4-9bac-ab1710703001','香汁牛腩饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2681e663-437e-537f-9f53-248fa44afa7e','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','港式肥牛滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('34bb1462-9fee-5ac2-866f-d7e4e0f90c4e','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','至尊虾仁滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('777c0c5e-ac9d-56ad-9874-0228e8fe0080','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','广式腊肠滑蛋饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6f0ce31f-573a-5a16-8c9b-4b7616427e77','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','精品五花肉滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e04e8f5b-1c71-53a2-af0d-0185f4b2d276','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','一品烤肉滑蛋饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9fb6a434-65fe-5254-89e5-f867175a5408','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','招牌鸡丁滑蛋饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6f5ba9d7-093f-543f-a4d3-5c37862fdbb1','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','经典麻肉滑蛋饭※',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4223f9bf-9c71-5169-8687-a950599f31df','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','迷你火腿滑蛋饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('99da98f4-259c-5d86-a49a-5672fc88c5ea','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','招牌霸王炒鸡土豆泥拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1513a792-c262-50a2-b09e-8abe9bfc0ed8','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','韩式甜辣炒鸡土豆泥拌饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a027075a-1e8b-5551-85fb-51e2ba8eeef6','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','川香小炒肉土豆泥拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8c535f9f-5fbe-5553-aa67-f660db4c0f55','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','新疆红烧肉土豆泥拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('678c805e-45c4-51b8-9c8e-70486f7cb266','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','咕噜口水鸡土豆泥拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('55becf8f-bf5c-537c-820d-7b41f4c76b3c','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','黑椒雪花肉粒土豆泥拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4bc11e3e-f346-5d2e-b57e-86b08938e05f','86c177b3-6573-5e79-a18b-24341725ef1a','招牌卤肉饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('40daabec-4460-5f05-a7a2-626a0869c34a','86c177b3-6573-5e79-a18b-24341725ef1a','招牌猪肘饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e298829a-5b3b-56b3-9cc5-e61a14fbb263','86c177b3-6573-5e79-a18b-24341725ef1a','卤汁鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7ccce17f-75e2-5361-bdc0-6cd033338d7e','86c177b3-6573-5e79-a18b-24341725ef1a','口水鸡汁饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2d26c2c3-fdc6-51e6-b421-4e8e59167b60','86c177b3-6573-5e79-a18b-24341725ef1a','卤汁素拼饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2aeb27d2-07bc-571e-bf05-2f31708e9840','86c177b3-6573-5e79-a18b-24341725ef1a','酱香大棒骨饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cae7b079-6aca-5b2a-b9d1-6819e879a98e','86c177b3-6573-5e79-a18b-24341725ef1a','排骨焖土豆拌饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80f216f9-02cc-5773-bbce-47e9a5247601','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','酱香牛肉面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('58e4e474-8f82-5580-b366-930feea1b307','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','陕西油泼面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4ce49aec-69bf-5645-8dc6-32f5108719f7','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','西红柿鸡蛋面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cea5dcce-7715-563e-b423-c5bc305c04fb','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','猪肉刀削面',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d09582f6-32e8-536f-8dec-919e44b1a4ee','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','炸酱面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e4e8db6-a6e6-5c40-98e8-cdd1c92eea21','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','葱油肉酱拌面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('00aebd72-fb1a-5cc2-a0dc-4956f65a53fd','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','牛肉粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('469f6da6-3b6f-5067-91ce-02c23fc62b8c','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','大盘鸡拌面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f428efdd-4d22-5ff4-9f41-e7717de4f661','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','麻汁土豆粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bd54f950-fe4c-55e3-85bd-e5c81fa41221','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','葱油肉酱拌面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6f9acb05-7c13-5758-bfb4-b97dbba78209','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','西红柿鸡蛋面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dac370c9-2269-524c-be42-428b93f70e5b','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','方便面',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1fd77836-ef8c-5243-b337-02c3a72f175a','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','卤蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('122bed76-e8d6-5a92-9a49-7458ffb9df0f','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','肉肠',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('47d4e5da-d1e3-58ef-88a5-5e98bba570a0','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','纯肉丸子',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ba89b879-34a8-5d2f-bd0e-4967c975bd14','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','豆干',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1e72a7a8-76b0-5de8-b761-0eee4d6163b4','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','炸豆腐',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2967f822-071d-5be3-8456-b6dc4201de5a','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','豆皮',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('859fc9ef-ca28-5f6b-88c0-fc14cc8b9399','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','自选称重烤盘饭',NULL,9.9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9ddc8e70-7ad0-561f-a42f-c73c412c4157','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','土豆片烤盘',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c46865cd-41da-5d7e-a7e5-c52c5dbea711','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','鸡柳香菇烤盘',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9db6d218-bb7e-5dec-8c2f-ea398859f236','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','培根娃娃菜烤盘',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e2c3e18c-208c-547c-be62-ec303e429923','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','鸡柳土豆片',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('624ac6f6-38af-5b54-8252-ee1aef1d0dc2','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','烤肠花菜烤盘',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aac68d95-7595-57a8-bd73-02f747da58a8','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','面筋鱼豆腐烤盘',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b1db7516-5305-51c5-a55f-b5a84bd2712d','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','米饭/汤','免费供应',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cbb102bd-163a-5a98-8adb-48242c12c711','8f31fd77-010e-5c63-8607-f99db544939d','蜜汁土豆鸡',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d376ba42-812e-548d-9f11-6f5fba2813a2','8f31fd77-010e-5c63-8607-f99db544939d','金牌酱骨饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9000fa0-ce77-5106-aea2-fa586c03b5a8','8f31fd77-010e-5c63-8607-f99db544939d','梅菜烧肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c9ce0f33-99ad-5e56-b4be-398b682183de','8f31fd77-010e-5c63-8607-f99db544939d','趣吃香焖鸡',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('200b53cc-877d-5af8-9b6a-11a9cbddbc78','8f31fd77-010e-5c63-8607-f99db544939d','辣椒趣吃肉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('931ad049-436c-5cea-aca5-f223b70e6426','8f31fd77-010e-5c63-8607-f99db544939d','马叔红烧肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('72e233c3-e30d-5afa-8636-348c80cf7431','8f31fd77-010e-5c63-8607-f99db544939d','辣椒炒肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7a68d14e-3315-5641-b365-50bec1fbaf2b','8f31fd77-010e-5c63-8607-f99db544939d','孜然肉片',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('55a7bafa-f8f9-516e-86a5-16dcfafaa3c2','8f31fd77-010e-5c63-8607-f99db544939d','可乐鸡翅',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7fecb72a-67fd-504b-837e-724867291c03','8f31fd77-010e-5c63-8607-f99db544939d','肉沫茄子',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f0944186-590d-5d26-a159-1ecdf58a7ac1','8f31fd77-010e-5c63-8607-f99db544939d','临沂炒鸡',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('90197546-bb8f-5ad8-8154-3554945e2f5a','8f31fd77-010e-5c63-8607-f99db544939d','贵妃荔枝鸡',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7aec29b-5f72-55dc-ad74-504987dcd991','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','招牌黄牛肉面/粉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ce038ba1-6d87-5c9c-911e-a8de5cdad43a','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','酸菜肉炒肉汤面/粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10a63b7e-557a-5627-8e4c-2f8b06be13c3','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','白切牛肉汤面/粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('92c1247d-f2b5-529a-b296-90ea94411d2f','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','湘味辣子鸡汤面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0039a689-7d1d-50dd-aaa0-708d4044f20f','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','番茄鸡蛋汤面/粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('79d5bcfd-1bf8-50dc-9d96-ed9b8601ac94','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','农家小炒肉汤面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('395c6da6-8220-5578-a350-2c1ee7e485b4','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','柳州螺蛳粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e81b6a9e-75ab-58e4-bba6-a52539ccab6e','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','黑椒鸡腿排汤面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('91278576-66a1-522a-976a-5acc08eb4b63','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','招牌肉酱拌面/粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('345ba0f5-ed89-55e9-b9e0-2227959d5a34','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','蟹黄肉酱拌面/粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1b085d99-f0c7-593b-8116-64c61e55f294','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','烤肉肉酱拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('59ef6d45-bb27-5cab-84e7-fb967e2e1b87','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','农家小炒肉拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c6e2ad70-28a2-5791-a485-e5335d085730','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','湘味辣子鸡拌面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1e907444-3f6e-5278-a6fb-8318021785c1','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','葱油拌面/粉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('87303834-6b51-5c78-bd05-c135ef6f4c1b','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','南昌拌粉',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('57557e93-5604-5e0c-b0b0-0825085ab935','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','武汉热干面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6f702512-ef5d-5f5e-bd9f-849c91331e07','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','椒麻鸡丝拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('99ee3999-83e8-5ff6-a57a-58eb584ec189','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','巴西烤肉拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('28cb879b-8ce1-5c1d-905d-32e8dbbecfbd','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','黑椒鸡腿排拌面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7b869fb9-3a8b-5773-9aff-0ce68b90a930','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','陕西油泼面',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9f39bea8-5611-5200-9e57-bd98fea32447','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','招牌肉酱油泼面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('39489313-544e-59ad-81a6-b05ddbfb7e98','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','番茄鸡蛋油泼面',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('13f15448-beeb-525b-a99d-0942d8612651','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','巴西烤肉油泼面',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9b39a026-1099-5c3d-a8f8-228b90058508','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','椒麻鸡丝油泼面',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d6af982a-eaea-52ca-bcef-a6f0e4752db9','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','辣子鸡油泼面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0bf292da-02e6-57a9-a2a2-84fb9f46a8b0','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','剁椒肉酱油泼面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('645f8e65-fd13-5574-b490-c8492f3a06a8','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','招牌黄牛肉油泼面',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('435663ca-38af-5228-bc9c-195234155655','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','煎蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('98cb599a-a616-50cb-9998-5331992ca361','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','兰花串',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3affa9f1-1618-552d-b391-c9744b85097a','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','淀粉肠',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45aa5aae-f32c-51ee-b47f-525ebd4f7de9','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','纯肉肠',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f1a60092-88f0-5c02-944b-4df9b22b5073','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','鸡腿',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9b017b5f-5a17-5596-af22-8af8466825f0','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','辣椒炒肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6c6dabef-74ea-5674-9d7b-ddab1af3bf7c','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','鱼味小炒鸡※',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10a54b74-5093-5bdb-8096-6b1e58021d8f','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','农家一碗香',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b559dc28-3da5-5193-9f0f-abd4d00408c3','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','肉沫酸豆角',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('020886c0-8255-5b1f-a7c0-98f6bd4dc66d','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','乡乡小炒肉※',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('acc05fff-e532-54b8-b821-9bd6c1ec0ea9','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','炒土豆丝',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('16bf268b-6f4f-5b96-8d49-923455a011cd','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','西红柿炒鸡蛋',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('944ffcf3-a26f-5ec8-80a7-1c8df50c42ea','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','冬瓜排骨汤',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ebbc5fdb-85b6-501e-9b22-536dc1c146c9','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','萝卜排骨汤',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('23d866de-488d-5531-90af-f15d23f881cf','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','玉米排骨汤',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e2750a0b-09aa-5002-a577-3a78b1123988','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','鸡蛋肉饼汤',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3bea2e6f-f698-586a-856d-f67fd55a9797','43643b96-1419-59dc-9dff-6e9605f6bb09','麻辣小面（汤）',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c55118b3-b018-5412-be49-8f446fd7dbba','43643b96-1419-59dc-9dff-6e9605f6bb09','清汤小面（汤）',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fc7f4231-ae7d-5466-9110-19789e28e454','43643b96-1419-59dc-9dff-6e9605f6bb09','葱油拌面（拌）',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe840f0b-409c-5c72-89fb-82096a3c80aa','43643b96-1419-59dc-9dff-6e9605f6bb09','酸辣面（汤）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('191139bb-a128-5b4a-9801-5db6c184df38','43643b96-1419-59dc-9dff-6e9605f6bb09','豌豆面（汤）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ed8d3e63-bac7-535c-890c-b09eb4ceb30e','43643b96-1419-59dc-9dff-6e9605f6bb09','酸菜肉丝面（汤拌）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('32c24e14-799d-5a68-aae0-ce00b4aeb623','43643b96-1419-59dc-9dff-6e9605f6bb09','西红柿鸡蛋面（汤拌）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d5d21a1e-d9d4-5fbf-8629-6f1d2edbea9c','43643b96-1419-59dc-9dff-6e9605f6bb09','辣子油泼面（拌）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('01121c65-aa40-5a5a-8190-71e970618851','43643b96-1419-59dc-9dff-6e9605f6bb09','西红柿鸡蛋刀削面（汤拌）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a231c773-fb24-52ad-8c9a-6cae1b369e6d','43643b96-1419-59dc-9dff-6e9605f6bb09','麻酱拌面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b8ff0dca-7b13-5a9f-801b-4ce3b801839b','43643b96-1419-59dc-9dff-6e9605f6bb09','肉沫刀削面（汤拌）',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2524488d-98e9-5433-940b-96a821941697','43643b96-1419-59dc-9dff-6e9605f6bb09','肉沫面（汤拌）',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6ee2cde5-b3e5-5969-824c-2cbbfe2b9c76','43643b96-1419-59dc-9dff-6e9605f6bb09','酸菜肉丝面（汤拌）',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('56b491e7-32f1-5b46-ab01-01aba3e52aca','43643b96-1419-59dc-9dff-6e9605f6bb09','鸡肉刀削面（汤拌）',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cd696302-99a0-5ff6-939a-9e06ba733ff9','43643b96-1419-59dc-9dff-6e9605f6bb09','鸡块面（汤拌）',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('97b4e5cf-200f-561a-88aa-b30ea92fbee0','43643b96-1419-59dc-9dff-6e9605f6bb09','炸酱面（拌）',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1c6ba819-e769-5190-a139-1f164298a163','43643b96-1419-59dc-9dff-6e9605f6bb09','宜宾燃面（拌）',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('36373b1a-beb3-53e0-82dd-b67b187e7c85','43643b96-1419-59dc-9dff-6e9605f6bb09','飘香面（汤拌）',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0cf3b03e-52f5-516a-959a-024bbd096c28','43643b96-1419-59dc-9dff-6e9605f6bb09','牛肉刀削面（汤拌）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c60fd7c2-dbec-5c29-b9f2-bfe7dba09ec9','43643b96-1419-59dc-9dff-6e9605f6bb09','牛肉油泼面（拌）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45d77c2f-8c29-5325-842d-91c58c3f10ac','43643b96-1419-59dc-9dff-6e9605f6bb09','青菜鸡蛋米粉（汤）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c606fe90-d6e9-547e-a1a2-effa48c39bff','43643b96-1419-59dc-9dff-6e9605f6bb09','麻辣肉沫米粉（汤拌）',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c85daf12-3865-579b-909c-3e8fee5d38fb','43643b96-1419-59dc-9dff-6e9605f6bb09','酸辣肉沫米粉（汤拌）',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cac21894-cc74-53bc-8bf9-46deb0974f97','43643b96-1419-59dc-9dff-6e9605f6bb09','麻酱肉沫米粉（汤拌）',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('af5d464e-db38-5ae4-9b95-d75c68137454','43643b96-1419-59dc-9dff-6e9605f6bb09','肉丝米粉（汤拌）',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('60f53064-31a4-59e5-84c1-a877e619d01c','43643b96-1419-59dc-9dff-6e9605f6bb09','鸡肉米粉（汤拌）',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('396f3ff4-4b1c-5834-8642-ab813b42fd9c','43643b96-1419-59dc-9dff-6e9605f6bb09','牛肉米粉（汤拌）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('25baf438-66b8-5efc-8699-1c750177c3bc','21622451-9645-5fdf-a5d3-a52e6f8e45d4','地三鲜',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ffea4edc-f1cd-518d-a3cb-0a394b5e291a','21622451-9645-5fdf-a5d3-a52e6f8e45d4','宫保鸡丁',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d7f12351-6928-55fc-b86d-ecf122952c6e','21622451-9645-5fdf-a5d3-a52e6f8e45d4','小炒鸡',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2b866c79-16af-5782-bc6e-38c5dc9be054','21622451-9645-5fdf-a5d3-a52e6f8e45d4','土豆肉片',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4d6fb937-3b21-502f-b125-a606b1271f4d','21622451-9645-5fdf-a5d3-a52e6f8e45d4','滑蛋肉片',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('89c4000a-383f-51d1-857d-492b3f850a06','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黑椒肉片',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1076d85b-a9a8-5fe4-af0c-2ad78b64f870','21622451-9645-5fdf-a5d3-a52e6f8e45d4','农家小炒肉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b0047abb-1d35-53b8-9549-bd69592f8400','21622451-9645-5fdf-a5d3-a52e6f8e45d4','金针菇肉片',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bfd80b0a-489e-5b69-97bf-bb9ae9ddaf76','21622451-9645-5fdf-a5d3-a52e6f8e45d4','回锅肉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('35e2d1be-fb0f-591c-8eb3-94978d19c4a2','21622451-9645-5fdf-a5d3-a52e6f8e45d4','小炒鸡杂',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9085da51-b694-51f1-9daf-534bb921eeef','21622451-9645-5fdf-a5d3-a52e6f8e45d4','水煮肉片',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ecfed7f1-2250-5e19-b3c5-d017b75edc9f','21622451-9645-5fdf-a5d3-a52e6f8e45d4','干锅菜花',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('445da727-6cdb-5e9d-8002-487a69fa7bdc','21622451-9645-5fdf-a5d3-a52e6f8e45d4','干锅土豆片',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d6cfdc5-2ddf-515a-b827-ecb80f5802cc','21622451-9645-5fdf-a5d3-a52e6f8e45d4','干锅娃娃菜',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('765b4e6d-1c9c-5b1e-ae85-792c4240254a','21622451-9645-5fdf-a5d3-a52e6f8e45d4','干锅千叶豆腐',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('756f3d5b-4b88-5aa1-8249-97a5172e281c','21622451-9645-5fdf-a5d3-a52e6f8e45d4','干锅杏鲍菇',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e9268ea-a51a-533b-aa65-934b71a8a7cb','21622451-9645-5fdf-a5d3-a52e6f8e45d4','水煮肥牛',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5342f2f4-9b01-543e-8daf-9aea8d913bff','21622451-9645-5fdf-a5d3-a52e6f8e45d4','干锅仔鸡',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aa53a22e-95ab-5e47-8b90-b1bf70e72af3','21622451-9645-5fdf-a5d3-a52e6f8e45d4','干锅鸡杂',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('94b1224a-2dfe-52bc-8251-996b7135b5c4','21622451-9645-5fdf-a5d3-a52e6f8e45d4','金针菇肥牛',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fdd6640b-237c-5e33-a53d-825bc5be6e21','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黄焖茄子',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('768cc88b-9d00-5975-9978-9f5e3e60e666','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黄焖豆腐',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('44ecdf07-37ca-5ad3-81c7-25fcdc777b94','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黄焖鸡',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f4f7d7e7-8274-5fef-8b38-d969106ffc4c','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黄焖小酥肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f9f248f-a8e3-55a6-828c-b83968fcf54a','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黄焖五花肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b00cd1d1-913b-56cd-875e-41016b750a71','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黄焖肥牛',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('00f11217-3873-53cd-a7a3-ee41d878ad2f','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黄焖排骨',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0645d682-53db-5709-8e97-9936eb9e223c','21622451-9645-5fdf-a5d3-a52e6f8e45d4','黄焖牛肉',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f3c3cd54-8379-533e-9913-b5cfa7286ad1','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','猪肉大葱水饺',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('42335714-0f36-5a42-9558-76179c2ed551','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','猪肉白菜水饺',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eca2420d-ce37-5535-a023-55c5a38ea513','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','鸡蛋韭菜水饺',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('695271e2-f12c-51ee-85aa-30ede51d3ef9','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','蒸饺',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe929ca1-a6f9-53f2-8096-80c6ed6e591e','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','香芋地瓜丸',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('86c67594-7d05-52f2-aa8a-e7e7c0d4cd9b','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','煎鸡蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1f6362c-dad4-50aa-a27b-9843c434d852','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','煎饺',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('46268a64-cf37-505a-9d14-83d6e214ca95','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','煎包',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('651e5d62-a9a1-54fc-9d8c-6e08e3d42249','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','油条',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('41a18430-1a91-5011-9cdc-781671a7a780','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','鸟巢饼',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0db603fa-c6b7-5726-83f1-996f93aa78d6','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','菜盒子',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73cb0da2-a28b-5dec-8738-acdc0c53db5c','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','炸糖糕',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bc42f78d-0cda-59de-96ee-c678f7fcdb8d','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','小米南瓜粥',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aeef57e5-ff85-5857-9bf8-326d8313345c','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','豆腐脑',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b72d04f3-4d89-548b-9110-695a1ff79d09','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','碗豆浆',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fed51b3b-dc4e-54ee-aeb3-efe1ce365289','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','杯豆浆',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5e8f84fd-9c7e-5224-ab7a-ea27d9c88622','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','卤蛋',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('deab99af-03e6-50cc-8484-731b2e8ae864','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','鸡蛋汤',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5af0c099-6d82-5a04-95d6-fab417518d80','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','八宝粥',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bf8167c4-04f9-5be4-aa88-0c8101cd1849','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','小碗馄饨',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('29154d38-2fe4-50a8-8f7a-13ccc69a4c2e','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','中碗馄饨',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dee390df-7b87-56ed-8256-a4a05c2344ee','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','大碗馄饨',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ee2a067b-e75e-5793-9f1b-f8a808a1c9e9','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','江米糕',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('03ecc2d4-218b-50e8-aa0b-645f9841ccc2','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','大麻团',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dfaf0235-5b2e-57c5-99c2-1888cf2ee1cb','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','烧麦',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2700e89a-3b21-5f8c-a2d6-19398fbb1c2f','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','蛋黄烧麦',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8699f12d-4863-587b-af17-19dffaac7a6e','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','肉饼',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('498c212c-45e3-5cd1-85f4-a9bdd0a21b28','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','鸡肉卷',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e7f66f8-00ab-5d85-abdb-583b07fc1e5d','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','土豆丝夹饼',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('188fc82e-b5bf-5d4a-870c-99bbfc60e25e','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','肉丝炒面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('872ea6bb-5ca2-5e4d-b2e1-f8955555d66e','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','素炒面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cca3b1c6-1ff9-5bf6-b766-29946a4081d2','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','肉丝炒饼',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('25bb6a00-daaf-58eb-87c5-66a86b4d2559','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','素炒饼',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b02edd5c-5a50-5c39-ae50-875df6959094','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','双夹烧饼',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('36d93019-f3e1-5db1-950d-97bd86746620','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','烧饼夹鸡柳',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1879043b-3cc8-5ff7-9a50-a516120fe2fc','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','烧饼夹培根',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bd36e5f1-539a-5703-bebd-32b889311a22','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','鸡蛋韭菜馅饼',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('41886d13-f587-5324-a523-bb1eabee109d','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','土豆丝卷',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c1b37cd0-8269-5747-8b31-646806faacda','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','酸辣粉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fc5543f3-dffc-56e4-84bd-a0716c3c2a86','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','红烧牛肉云吞方便面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1cec3bf-d599-5c57-bc74-0a5cc5870567','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','酸菜云吞方便面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('58ca84d5-3e72-503e-a5a0-34c51178600a','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','香辣云吞方便面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('03456f55-76ab-5f69-b759-31bba8849a9f','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','香菇云吞方便面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('60ff977a-d54b-5768-9063-74ffafa0e9bc','1683fa82-5238-5bfe-9f08-adb9a007ee29','卤豆干',NULL,0.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('29794aca-aad9-5609-b4a5-c1dbcd692b1b','1683fa82-5238-5bfe-9f08-adb9a007ee29','卤鸡蛋',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f828f85f-6e90-586b-bf3b-388e788ba032','1683fa82-5238-5bfe-9f08-adb9a007ee29','猪肉白菜馅饺子',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('60bc2425-e96b-5d79-94e4-2b10ed87ab23','1683fa82-5238-5bfe-9f08-adb9a007ee29','猪肉大葱馅饺子',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7776fa3f-f476-528a-a488-314ca124c092','1683fa82-5238-5bfe-9f08-adb9a007ee29','韭菜鸡蛋馅饺子',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('766bf89b-9eaf-54e7-a657-73a9b5438b33','e605d74f-8c09-5571-b2c7-d64c4a281b2c','西红柿鸡蛋面',NULL,4.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0752f67e-bd67-5969-9fde-b4fa7897f6c0','e605d74f-8c09-5571-b2c7-d64c4a281b2c','木耳鸡蛋肉沫面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f03bba72-e6a7-5348-9841-898cae2615c6','e605d74f-8c09-5571-b2c7-d64c4a281b2c','豇豆肉沫面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('469641c3-595c-5b41-b8fb-b55aa1fd12a1','e605d74f-8c09-5571-b2c7-d64c4a281b2c','酸菜肉丝面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('caf8e924-e4d0-5880-9cca-8696dbf40ec8','e605d74f-8c09-5571-b2c7-d64c4a281b2c','香菇鸡块面',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0fe23d45-3f3f-562f-9734-cf53dfa4864c','e605d74f-8c09-5571-b2c7-d64c4a281b2c','家常臊子面',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('762e1649-829e-5908-beb8-2fe1eac37f06','e605d74f-8c09-5571-b2c7-d64c4a281b2c','牛肉板面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8b153099-b76a-56e3-8748-2fb8f2b79f85','e605d74f-8c09-5571-b2c7-d64c4a281b2c','素三丁面',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d3b1f4c-055d-5f32-b4be-584330d29ea1','e605d74f-8c09-5571-b2c7-d64c4a281b2c','烩菜面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('569f32c6-f667-5235-91f0-2d4a2bb868c5','e605d74f-8c09-5571-b2c7-d64c4a281b2c','肉沫茄丁面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9f6d18e7-cf16-5140-abad-0b5332f93614','e605d74f-8c09-5571-b2c7-d64c4a281b2c','尖椒肉丝面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('004f4fe2-3ad4-5905-8333-60d9f2f1afdf','e605d74f-8c09-5571-b2c7-d64c4a281b2c','炸酱面',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9908e09-c2bc-54d1-84b7-7a91a22e2077','e605d74f-8c09-5571-b2c7-d64c4a281b2c','麻酱面',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3bea87ec-1d27-5dd3-bcc3-69eac6cc3ee6','e605d74f-8c09-5571-b2c7-d64c4a281b2c','红烧牛肉面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d511f560-541c-5983-9ac1-e2b4f86acaf2','1e949237-63e6-5adb-b74b-413eb635d6e2','杂粮饭',NULL,0.3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('06f9962b-113e-5852-870e-0cd15e5f9cea','1e949237-63e6-5adb-b74b-413eb635d6e2','盐田虾',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('523ba349-0fe4-5a21-9db5-a8c77bbe2ba2','1e949237-63e6-5adb-b74b-413eb635d6e2','香菜丸子',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3c2d6c86-04b6-50f8-b20b-9f23c181182c','1e949237-63e6-5adb-b74b-413eb635d6e2','油菜鸡蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b77071a-d287-504e-b010-8b473d8935e1','1e949237-63e6-5adb-b74b-413eb635d6e2','粉蒸木耳',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ceef820d-154c-50f1-8260-255172aeb857','1e949237-63e6-5adb-b74b-413eb635d6e2','芹菜花生米',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('42b38bba-478b-583e-88be-a3e83de8a2f7','1e949237-63e6-5adb-b74b-413eb635d6e2','木耳鸡蛋',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2dcc7d26-2680-5566-9369-b07f82505fa5','1e949237-63e6-5adb-b74b-413eb635d6e2','黑椒鸡腿肉',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b8f79369-f344-5825-ba84-653cf98f6ace','1e949237-63e6-5adb-b74b-413eb635d6e2','芹菜豆腐',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b2d5567c-a4f0-5052-8362-9532e50924ea','1e949237-63e6-5adb-b74b-413eb635d6e2','红薯',NULL,0.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('809d94aa-dd20-5953-a08e-01d1782bad72','1e949237-63e6-5adb-b74b-413eb635d6e2','玉米',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('476cbacb-bd31-5ea9-949c-a582c8790dbd','1e949237-63e6-5adb-b74b-413eb635d6e2','素什锦菜',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a9ae90eb-0882-5049-b484-6853feccb828','1e949237-63e6-5adb-b74b-413eb635d6e2','蒜泥茄子',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3672d7b9-1905-5d09-a947-0a8653ab613a','1e949237-63e6-5adb-b74b-413eb635d6e2','清蒸鸡腿',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b47a2f6b-479a-5b5d-909a-fbaa75d3c776','1e949237-63e6-5adb-b74b-413eb635d6e2','山药南瓜',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('08c12d0e-06cb-5418-a929-ed480f619ece','1e949237-63e6-5adb-b74b-413eb635d6e2','毛豆仁鸭丁',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b6746f48-1da5-59e5-a047-a10e8c276af4','1e949237-63e6-5adb-b74b-413eb635d6e2','冬瓜鸡块',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('33dfac81-808f-50aa-9c29-e4b14b04bac3','1e949237-63e6-5adb-b74b-413eb635d6e2','双椒风味鱼',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('94910a78-88fd-591c-b5f8-0233b9e9dcf6','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','鸡毛菜',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ae194b24-2aff-543c-9d7f-6ec9b4e65c81','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','青笋叶',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f275ffc8-34ec-54e7-9261-abc2c8f9e889','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','蒿子秆',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('65883e04-7210-59ec-973d-77b4021d026a','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','土豆丝',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('736bfe16-06a2-5050-9d4a-01739c18508d','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','胡萝卜丝',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5af68d95-6827-5ad6-8633-fc8d5f2f95de','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','豇豆',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('90cf0a6b-4140-5ca9-918a-0e482a1089d7','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','木耳',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fed56a23-bbbf-5608-8802-49be3454537b','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','苦菊',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('47fab7f7-90b9-51fa-84d3-b381fdf1a427','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','芹菜叶',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('628e0ba0-7ecb-5bab-992b-17cdbc8df54f','3af1365e-ce72-5e82-918b-514f771e997c','老干妈炒饭',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('108e7ac7-31ba-5d5c-8a7a-3dd5ea17091b','3af1365e-ce72-5e82-918b-514f771e997c','鸡蛋炒饭',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1876e8a7-dd89-5e6a-8dd4-c3e4f4ed23e3','3af1365e-ce72-5e82-918b-514f771e997c','鸡蛋炒饼',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('88020766-4855-5879-b543-0058db184941','3af1365e-ce72-5e82-918b-514f771e997c','肉丝炒饼',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a4ec4a9c-e738-5619-b858-9fca7f3b2be3','3af1365e-ce72-5e82-918b-514f771e997c','炒方便面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('13d7ba01-324e-5691-a010-9895215a2812','3af1365e-ce72-5e82-918b-514f771e997c','炒刀削面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eff33e5b-7a99-527b-ace8-a8cd1747021c','3af1365e-ce72-5e82-918b-514f771e997c','炒牛筋面',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('caab08ab-f126-5b3c-9980-3727cdbecdc7','a4ab5dcf-4895-5f8e-a231-a3f9fd029049','砂锅豆腐',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0e1b0750-9033-57d0-ac51-9726de8e6c85','a4ab5dcf-4895-5f8e-a231-a3f9fd029049','砂锅方便面',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('79b7a860-f4a6-5229-924e-221db644a3db','a4ab5dcf-4895-5f8e-a231-a3f9fd029049','砂锅三鲜丸子',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a41190ed-69d8-57e6-9524-25da5362d985','a4ab5dcf-4895-5f8e-a231-a3f9fd029049','砂锅小酥肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('31de8729-1301-51d4-acaa-dafd3bc73d24','a4ab5dcf-4895-5f8e-a231-a3f9fd029049','砂锅肥牛',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5cf577a4-d610-5d47-84eb-9033788a2183','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','小炒藕丝',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e7452f4-75d7-5afc-8598-f25811c36f67','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','麻辣鸭脖',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10366b82-e960-51f0-acba-aba0cd229534','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','东安鸡',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('afa75c93-5113-5a04-9fc5-153c305acd0e','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','干煸鸡腿肉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80485a82-4e93-5af7-bf74-2afefce21181','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','爆炒羊杂',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('072360de-a995-59ab-9baa-ee43166bee26','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','小炒黄牛肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('de6d1d9a-6ca4-5b6d-8835-1cdf7d29390b','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','烟笋炒腊肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('35beb179-bcf3-5488-85e1-0b2a0ec1db67','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','香辣火箭鱿鱼',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('925bc24f-162b-5a8a-9ba1-8d2ef25c172c','3055b9d0-387e-57d7-9446-70d3c08a005a','酱寸骨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('be824f80-1638-5fcd-a63b-376bab1dd970','3055b9d0-387e-57d7-9446-70d3c08a005a','梅菜扣肉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cbe81bf1-d549-5a68-96d5-ad270fdbc727','3055b9d0-387e-57d7-9446-70d3c08a005a','蜜汁红烧肉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4e5412a5-b387-5b7d-aa80-de7b80c5e301','3055b9d0-387e-57d7-9446-70d3c08a005a','酱小碗肘子',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8224d6a5-c492-5220-8e5d-d2af30ea41c4','3055b9d0-387e-57d7-9446-70d3c08a005a','酱棒骨',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b0a821d1-b245-5afe-b716-89d300d0de4b','3055b9d0-387e-57d7-9446-70d3c08a005a','小碗酥肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('08a7481f-317b-560e-ab8c-e3dd6a9ad3b0','3055b9d0-387e-57d7-9446-70d3c08a005a','鱼头泡饼',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('99574fb3-41d7-5b50-a56b-2a0fe1a5bb8a','3055b9d0-387e-57d7-9446-70d3c08a005a','黄豆猪蹄',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('16facd48-7f5c-5717-9bad-54ccc300f55d','3055b9d0-387e-57d7-9446-70d3c08a005a','红酒牛肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aa885705-319d-54f1-86e6-252c42600fe4','3055b9d0-387e-57d7-9446-70d3c08a005a','酱扇骨',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('281d23d4-1b63-59bd-a0f1-06ea291baccc','848292e4-ffde-5abb-8b73-59b7f8975320','白菜豆腐汤',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7e7a2ef3-5636-52f5-b4c9-f4d69a8bc616','848292e4-ffde-5abb-8b73-59b7f8975320','银耳雪梨汤',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a731d07-6cfa-592c-b7c4-b8f3dc5052c8','848292e4-ffde-5abb-8b73-59b7f8975320','蘑菇鸡块汤',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0a43633c-b47b-5df7-afb3-e24d74bf598e','848292e4-ffde-5abb-8b73-59b7f8975320','菌菇虾仁汤',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('58335ece-4e77-5855-821e-4e9a1e1f0013','848292e4-ffde-5abb-8b73-59b7f8975320','海带红烧肉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a73ba078-95ce-5592-b85d-8a3149ec09eb','848292e4-ffde-5abb-8b73-59b7f8975320','玉米排骨汤',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c8ceb375-891d-583d-b948-7efc39fa834a','848292e4-ffde-5abb-8b73-59b7f8975320','西红柿牛腩',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('801aa66f-1414-5c2d-9e3e-5f36c650c9d9','848292e4-ffde-5abb-8b73-59b7f8975320','萝卜羊肉汤',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('39b9a683-0f56-5888-8da0-e1e78ccb9532','acda8dd1-08c3-5b9a-85c5-10339fca3927','土豆丝盖饭',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c8663d40-10a4-50b3-8289-b1fc8e03df3b','acda8dd1-08c3-5b9a-85c5-10339fca3927','麻婆豆腐盖饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0158a18e-ed5a-552f-9f4c-3312515ef76f','acda8dd1-08c3-5b9a-85c5-10339fca3927','西红柿炒鸡蛋盖饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('419f8e63-f080-5986-ae2b-b88d5335187c','acda8dd1-08c3-5b9a-85c5-10339fca3927','香菇炒肉盖饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6d388b21-508a-5c63-bc68-3d9534f8cb53','acda8dd1-08c3-5b9a-85c5-10339fca3927','青椒肉丝盖饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('050625f3-21a0-5dae-ab1c-838d4ec6534c','acda8dd1-08c3-5b9a-85c5-10339fca3927','京酱肉丝盖饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('906a98a8-d330-5010-8e85-3b7f9b772966','acda8dd1-08c3-5b9a-85c5-10339fca3927','鱼香肉丝盖饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a46e4d9b-ab24-5006-b137-05715fa5d3c9','acda8dd1-08c3-5b9a-85c5-10339fca3927','回锅肉盖饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('415b8320-b439-513f-9fff-8e027b82b146','6e5a215e-4fa6-5701-afaf-789f2c8197d5','土豆丝',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c0662c14-8eb1-5a2c-80d9-9cfd7fb2c3be','6e5a215e-4fa6-5701-afaf-789f2c8197d5','清炒快菜',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f0e42a84-80b6-53b7-95b7-ba803580338c','6e5a215e-4fa6-5701-afaf-789f2c8197d5','油菜木须肉',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c5ac799-7231-5873-94b6-052525299ab3','6e5a215e-4fa6-5701-afaf-789f2c8197d5','香酥鸭腿',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('009a89d7-03ef-5c07-8a51-10f66da76008','6e5a215e-4fa6-5701-afaf-789f2c8197d5','八宝鸡块',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('94b3f4e0-2787-5b10-be5e-cc4dc0ee1fd7','6e5a215e-4fa6-5701-afaf-789f2c8197d5','素炒西葫芦',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5cd9f008-9615-5a16-ab19-3cac5fc206b2','6e5a215e-4fa6-5701-afaf-789f2c8197d5','芹菜腐竹',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b1e924af-08c9-516e-bdaf-19890565669d','6e5a215e-4fa6-5701-afaf-789f2c8197d5','干锅土豆片',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1a017c3b-7492-5d17-99bd-92eaef1a6db1','6e5a215e-4fa6-5701-afaf-789f2c8197d5','千页豆腐炒豇豆',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3c03590f-a3c9-5af5-847f-8c5cdaa65d09','6e5a215e-4fa6-5701-afaf-789f2c8197d5','红烧海鲅鱼',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4d39144b-7b43-5f49-b4ab-74f83aef77c2','6e5a215e-4fa6-5701-afaf-789f2c8197d5','胡萝卜丝',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e651f93a-dab9-5ec4-aa34-2fe78d91aff5','6e5a215e-4fa6-5701-afaf-789f2c8197d5','清炒小白菜',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ce956e6e-4cc6-52b9-bcbc-19c9134654f3','6e5a215e-4fa6-5701-afaf-789f2c8197d5','杏鲍菇炒肉',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('84602817-88a2-5e0c-98fb-30fa5eaef6d7','6e5a215e-4fa6-5701-afaf-789f2c8197d5','糖醋里脊',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b38e5059-438e-5d02-b957-4c0201fdc4f8','6e5a215e-4fa6-5701-afaf-789f2c8197d5','香辣鸡腿肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('34d072be-ae26-5edd-8aa7-bdfa27b065ee','6e5a215e-4fa6-5701-afaf-789f2c8197d5','素炒黄豆芽',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3aadf3f1-b0f7-5a7a-bf5a-41fe0a5c0c33','6e5a215e-4fa6-5701-afaf-789f2c8197d5','卤鸡腿',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26b0c10a-1bf3-52e4-b89a-708cfb7b93c2','6e5a215e-4fa6-5701-afaf-789f2c8197d5','酸菜鱼',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4512687b-0976-5521-a365-b9ac1ba08051','6e5a215e-4fa6-5701-afaf-789f2c8197d5','黑椒鸭片',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('81d244de-b6fb-5730-b44a-dfe9af0ed749','6e5a215e-4fa6-5701-afaf-789f2c8197d5','麻辣鸡块',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('83452cc2-14ce-5390-940a-781b32355403','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','招牌烧鸭饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2bee94c8-304b-518f-9f82-31fb02d72805','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','老娘叉烧饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2456322b-62bb-5725-8340-d6603af527c2','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','香煎鸡排饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73c09ffc-fd1b-5b55-954c-baf4b8e0e35c','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','黑椒猪排饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('727efe68-6c34-5031-aa5b-de3e441e99e5','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','黑椒土豆肉片饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f1c355c7-1bbf-5f3a-8c47-2c08a26a3d48','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','蜜汁手撸鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('52131469-b756-5973-9d37-571712a73fe6','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','迷你白切鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('85eb0583-6730-5e20-9f31-c9909d4ea47c','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','姜葱口水鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('53af4ce5-cf94-5f4b-a8ac-dc122923a495','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','香辣鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('23240ddd-56a5-5623-b6f8-c2940f3bd62f','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','盐焗鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9cf7558c-5c60-5497-8095-3daa8e41250d','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','秘制鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('825f89e8-e4ce-5227-b24b-fa29b8270743','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','香汁牛腩排骨饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c0fec949-b484-57d9-80b1-1be39c066711','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','广式猪脚饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('657b8784-bfe7-5606-b065-8c2eee5a92c0','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','隆江猪脚饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eede6850-e4a4-58cd-b71d-35d096802d4c','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','网红脆皮五花肉饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fdafc534-0735-527b-a741-e1594a3bb8bd','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','任选双拼饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('33e3aa39-d010-52f3-8af6-f85c845221a6','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','小炒五花肉笼仔饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b1b70bd4-caad-55e1-91df-5b651efe902b','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','广式腊肠笼仔饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9dd5531c-7387-52b6-8526-5c35daf90722','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','香菇鸡味笼仔饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5b378e8-92c3-5f9b-874f-49535fa9e054','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','招牌烧鸭笼仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('815bf9d3-f53d-5f4b-bf14-44b8c90f5a53','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','广式鸡腿笼仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1bee071-810c-5473-911e-6f45418675f6','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','鸡翅笼仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('39b820a9-a02b-5579-a319-df912966467a','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','手撕鸡笼仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6ec5d31b-5dca-5c66-b2ea-2fb195b4f50f','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','口水鸡笼仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('58845bc7-59c0-5afc-89a1-85ab66cb4a61','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','脆皮五花肉笼仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5249fab5-579c-527a-bdbf-8c5de0ca7bd6','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','隆江猪脚笼仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('be1dc079-73fe-59a8-a401-a02062d72f20','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','腊肠鸡排笼仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('547f79e6-423e-5867-be38-4d0529800bcc','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','腊肠烧鸡笼仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8f232d83-171c-5c61-88ff-36f846063b78','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','腊肠叉烧笼仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b41a2398-b807-5c47-8137-2a8cb0b99fca','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','烧鸭叉烧笼仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('77b35f92-491d-547a-8061-ef1e14d7a4f5','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','烧鸭鸡排笼仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8852c69f-fc96-50cf-ba10-4872d265348e','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','鸡排叉烧笼仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8dec6646-90f8-534d-b79e-c889affd051f','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','鸡蛋肉沫炒米粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d22b0c0-371b-5f8b-aa39-37a659e64721','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','微辣鸡蛋肉沫炒河粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c63cdbbf-0fcd-55c6-99f3-a32241507041','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','素烩意粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b952e2ab-ca5f-539c-963b-4ce43d62ec78','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','肉烩意粉',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e4e75067-9d28-5158-8e32-4230d8dc3095','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','煎鸡蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('95d2d4e2-f3ae-5763-99e4-614632c7fb21','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','卤鸡腿',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80160695-ff08-5efe-b142-00ee8f3f699c','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','香肠',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('718de979-1a3b-5c96-8979-3222ef92ddd4','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','大肠',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5e98621d-46fb-5fcd-ba15-dcdf3b7a5352','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','鸡腿',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('52c2f5ae-c1bc-50a5-a155-ee8f08c6a394','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','鸡翅',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f9e2213c-0f1d-5557-9463-6a57c7c20fd4','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','烧鸭',NULL,90,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('507ff085-25a0-535e-b776-0e62bb14e04e','57c1c534-f550-50f9-b599-5521b590f18d','五花肉酸菜饭',NULL,15.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('82fc8b9f-e407-5330-9c8e-26b899e5f124','57c1c534-f550-50f9-b599-5521b590f18d','五花肉包菜饭',NULL,15.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a5a4ee82-5c3f-5ea4-9f59-f9bb2d755f90','57c1c534-f550-50f9-b599-5521b590f18d','五花肉圆葱饭',NULL,15.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('591e84b4-f35a-5437-adb2-ab3ee078e5aa','57c1c534-f550-50f9-b599-5521b590f18d','五花肉辣白菜饭',NULL,15.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2438ff03-01fa-57a3-abce-ae3d8d6f5c43','57c1c534-f550-50f9-b599-5521b590f18d','法式烤排饭',NULL,15.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b9546ab-f84d-55be-9be5-5ce1f48a9247','57c1c534-f550-50f9-b599-5521b590f18d','五花肉金针菇饭',NULL,15.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d06b0b1d-7287-5a35-acb6-99f96bffb8f2','57c1c534-f550-50f9-b599-5521b590f18d','韩式咖喱鸡拌饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('40338e31-419a-5a53-8bdc-d0172df06d07','57c1c534-f550-50f9-b599-5521b590f18d','韩式招牌炸鸡拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('70b971a9-08ee-5aa2-abba-745be76899a4','57c1c534-f550-50f9-b599-5521b590f18d','番茄肉末茄条拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('feab6c15-11eb-5333-ae1e-daa9ab8ecd24','57c1c534-f550-50f9-b599-5521b590f18d','黑椒猪排拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d9e8e8a-0bc2-5fd5-945b-c41b6d9417ce','57c1c534-f550-50f9-b599-5521b590f18d','韩式小鸡腿拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a26a9df4-98b4-5550-8701-757457ee8356','57c1c534-f550-50f9-b599-5521b590f18d','韩式金枪鱼拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ac806696-2f87-5e47-91fc-b53a0f0144de','57c1c534-f550-50f9-b599-5521b590f18d','韩式香辣培根拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cd2b4c89-ac48-5556-9a84-28739a6df928','57c1c534-f550-50f9-b599-5521b590f18d','藤椒鸡排拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('76ddca64-8f6a-593a-83d0-23aacb79f7d7','57c1c534-f550-50f9-b599-5521b590f18d','韩式肥牛拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c6cce0a1-8d62-5639-9278-da905be7f696','57c1c534-f550-50f9-b599-5521b590f18d','韩式番茄牛腩拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9f036ea1-5cb7-5831-bb51-a65bea2a27dc','872d34f7-3d76-5199-8618-cc17eb5370de','校园牛肉面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5424b4d8-d413-5e29-920f-a12a408d86e9','872d34f7-3d76-5199-8618-cc17eb5370de','素臊子面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3416eca8-1af3-53d0-8efb-fc8c2609df91','872d34f7-3d76-5199-8618-cc17eb5370de','臊子面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a4e5d979-10e1-53e0-a880-5752749c6cc2','872d34f7-3d76-5199-8618-cc17eb5370de','麻辣鸡丝面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ee4359aa-53ed-5847-8b13-7c16178753c0','872d34f7-3d76-5199-8618-cc17eb5370de','麻辣鸡蛋酱面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('725ce9a9-6be7-5255-8311-f3b6926d5c69','872d34f7-3d76-5199-8618-cc17eb5370de','西红柿肉酱面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('980a481e-18b1-518f-9093-7ad5e6731a34','872d34f7-3d76-5199-8618-cc17eb5370de','红烧鸡块面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1f50b084-bc38-572b-9730-d9ebc25c2ca0','872d34f7-3d76-5199-8618-cc17eb5370de','小炖肉面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('65899084-942f-5b8e-9f18-1920fa5be0ce','872d34f7-3d76-5199-8618-cc17eb5370de','酸汤肥牛面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eee2aa6a-6b88-50d2-943b-062084b76128','872d34f7-3d76-5199-8618-cc17eb5370de','小炒肉面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('466e50e9-4985-5081-9e9d-000e83e5b816','872d34f7-3d76-5199-8618-cc17eb5370de','红烧牛肉面',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2a47be6c-6784-50d8-8bbd-32ec6578f66d','872d34f7-3d76-5199-8618-cc17eb5370de','油泼面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('846dc1ff-97f2-57dd-b7f7-3551f8a4da5b','872d34f7-3d76-5199-8618-cc17eb5370de','炸酱面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('78ffe3a3-87e2-54d4-a7b1-65a923ffb694','872d34f7-3d76-5199-8618-cc17eb5370de','西红柿鸡蛋面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d67db35c-efde-5ff6-8040-52b9190857cb','872d34f7-3d76-5199-8618-cc17eb5370de','香菇肉酱面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a1175c4-3c11-5c1f-95c0-b131f61f3c63','872d34f7-3d76-5199-8618-cc17eb5370de','尖椒鸡蛋酱面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('db306676-e384-5eb1-a84e-4c289e91669b','872d34f7-3d76-5199-8618-cc17eb5370de','茄子肉丁面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d532fdf6-fd27-5684-8ed3-41b4032de95d','872d34f7-3d76-5199-8618-cc17eb5370de','剁椒肉酱面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('188dadcc-0cfc-50bd-824a-279d755ee7ac','872d34f7-3d76-5199-8618-cc17eb5370de','大盘鸡拌面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('90a018c1-e4cc-5d02-bc4c-dd6ca76bbec4','872d34f7-3d76-5199-8618-cc17eb5370de','素肉臊子面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('441cc481-66b2-5abd-82f0-1189280367fd','872d34f7-3d76-5199-8618-cc17eb5370de','麻辣鸡丝面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c5504e67-1a72-5172-ba95-f2d99056f3d4','872d34f7-3d76-5199-8618-cc17eb5370de','麻辣西红柿面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9cd9efa2-9a50-5111-bb9d-d94cead0be8f','872d34f7-3d76-5199-8618-cc17eb5370de','西红柿鸡块面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c5223ab6-dca0-5ccc-9b68-f386e8c295ae','872d34f7-3d76-5199-8618-cc17eb5370de','小炒肉面',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c9106ebb-da98-52c9-9277-fdae7989f007','872d34f7-3d76-5199-8618-cc17eb5370de','红烧牛肉面',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2ec9ee18-833b-5c67-9d77-94a6f28b717b','872d34f7-3d76-5199-8618-cc17eb5370de','火腿炒面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('767d0511-b92e-5376-b2aa-be7279a5181a','872d34f7-3d76-5199-8618-cc17eb5370de','鸡蛋炒面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9fb69038-a901-5427-a355-08180d09d27f','872d34f7-3d76-5199-8618-cc17eb5370de','肉丝炒面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('003571a7-6c76-51b1-956c-cf476c23a9ae','872d34f7-3d76-5199-8618-cc17eb5370de','鸡肉炒面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9a0d616c-6f26-5701-820c-395320ed367b','872d34f7-3d76-5199-8618-cc17eb5370de','卤鸡蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1ab1f396-ea72-579f-81ac-0f748987c86a','872d34f7-3d76-5199-8618-cc17eb5370de','卤豆腐',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45eb5e0f-024e-5282-9373-e74e58d688a0','872d34f7-3d76-5199-8618-cc17eb5370de','小鸡蘑菇面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7a50f316-ef52-5147-bc63-7133c4c1d5a0','872d34f7-3d76-5199-8618-cc17eb5370de','羊肉泡馍',NULL,18.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4c973818-4c94-5c14-895f-5b0752471f81','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','麻辣香锅（待核价）','原价标注：麻辣香锅，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('df0f510e-ebb1-5f2f-b3ae-ec8f79642a23','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','招牌香辣鸡腿冒菜',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b540666-9dce-54c1-81a6-ade48534901f','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','半只鸡冒菜',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('183e382d-4fc6-5fa0-b907-80c125682f4b','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','麻辣鸭蹼冒菜',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f744d0e0-3274-5635-91ca-f1cadcaed7a5','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','午餐肉冒菜',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('713a8254-24da-5ec5-a29f-bc5b3204ce87','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','培根冒菜',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ab74f5ed-a301-51ef-8de7-eb820c346742','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','肉卷冒菜',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4c0371c6-76e2-5afc-9205-e59bed8aae20','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','水煮肉片',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e0196416-e003-5452-88b0-a495bc860c91','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','什锦丸子',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('58e8e9dd-811f-5be4-8741-ac2679e96f79','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','什锦酥肉',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('40ff8260-33d6-5586-ad62-85f9c15a6049','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','咖喱鸡肉盖浇饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45094fa0-6fa3-5f31-9a30-34c1761392e7','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','辣椒炒肉盖浇饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('43e2459f-53d3-5dd1-967e-a686f3b6644f','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','麻辣鸭腿盖浇饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2c36f1e8-b1b4-503f-9a94-7050afe4fa63','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','尖椒豆芽肉丝盖浇饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('47d7d6f1-543c-5965-9802-1f921f6b0024','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','梅菜扣肉盖浇饭',NULL,19.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ccc4fab5-c549-5e97-a245-39383b2cc5c0','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','土豆炖牛肉盖浇饭',NULL,19.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5d682e1b-b2ba-59d1-baae-f2d5a59c8c04','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','酸辣土豆丝盖浇饭',NULL,9.9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('72cd9754-8a0a-5d81-ace7-cd5a35466086','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','西红柿鸡蛋盖浇饭',NULL,9.9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7b567b07-f6a4-5939-a0ec-d3bb8129a215','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','鱼香茄子盖浇饭',NULL,12.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('326dbc18-da95-57da-9244-fc7f71934f59','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','麻婆豆腐盖浇饭',NULL,12.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('31a7251c-efb3-5949-a18a-87a9346bd120','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','鱼香肉丝盖浇饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4636675f-ff34-5abb-b4ff-3e35c29ccbcb','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','黑椒肉片盖浇饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('35f301d7-4b12-505d-8f82-ff0ac225038f','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','酥肉小锅',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2d7bb254-2483-5bae-9745-069c630131b5','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','培根小锅',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('465f4cf5-11e8-5544-95f1-7cfde95ea10b','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','瘦肉小锅',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b3565ff-4585-54ea-859e-0435c7986348','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','肥牛小锅',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5529ef27-ca87-5069-9e19-b2d22ee5ea04','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','鱼豆腐小锅',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f78ae829-4bd0-5959-af9f-ffe9e28de47c','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','牛肉丸小锅',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('23e30926-bbc9-5040-b822-5bb44072b800','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','招牌鸡公煲',NULL,15.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2ca25857-10f9-5d86-b96d-74741a354978','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','肉末酱香茄子煲',NULL,13.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('75990b91-4ddd-5de6-9bd0-73bfcf5b265c','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','肉末嫩豆腐煲',NULL,13.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('430829ed-c5f0-5779-8db4-d48210e2ed8c','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','招牌鸡公煲（大份）',NULL,15.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3aa289db-abda-592c-a3de-201507628731','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','肉末酱香茄子煲',NULL,13.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5e822846-3d91-5720-976a-85f06b9f94a5','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','肉末嫩豆腐煲',NULL,13.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f938ebc0-292b-5931-8a55-716eb032c329','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心卤汁素拼饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7722a8e1-0f12-555e-8542-f05dde26996c','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','卤蛋',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a93e11e6-aaf9-581c-9c9d-724aa7e1a76a','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','卤海带结',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5ce3605b-e306-5ece-8466-dbd28ef2b4c8','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','卤萝卜',NULL,0.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c31faa82-6245-5075-8f7b-98ddf4e18ea1','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','卤豆腐干',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5f06ff10-837b-59e8-ad84-562eb3245e23','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','酱汁茄子饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('39584554-9d12-53b8-a485-0562d0f42635','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心肉肠饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('70680cd4-c5a2-5186-b455-b36fc9329939','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','卤汁鸭腿拌饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e2ef74c2-35cd-566a-b08d-958d63573f4a','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','奥尔良鸡排饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('42f93773-5dd8-50e3-9df7-9af9f438ab49','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','卤汁大鸡饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6388422a-2f6b-5070-a3b5-efd6211f7223','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','卤汁鸡腿拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5cde555e-c208-52e8-b990-1ea04966aa45','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心脆皮鸡肉饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e6a094b-cb38-591a-b8d6-c7dbb2023159','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心椒麻鸡饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bad10c97-9b99-5708-a91e-0197a150ecc9','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心排骨饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7e30a48b-5d11-508c-a5ad-5f7e9d7dc6a0','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心卤肉饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1061d616-a0ca-506f-8602-f07e877b1c6f','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心猪肘肘子饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4eb4dc82-0a2d-5568-bb7f-14e9f1597965','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','脱脂牌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3a3b0336-4746-5655-80e1-c84ceeed04d9','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','金牌肘子拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('68823566-8587-5104-ba14-5a849050f1cb','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心卤肉饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6d51cb0c-52ad-546a-a3cb-22fdd0b91d93','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心鸭腿拌饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d995a111-170a-5f0d-9c14-4df76a7cd76e','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','匠心椒麻鸡拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e200c207-2a85-582c-9e59-6ecbffc3391e','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','招牌生烫吊龙米线（清汤）',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b1cfabf-4cd4-5507-9511-399c17d9308c','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','招牌肉末炸酱米线（酱香）',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e006e6a-e657-51d9-b01c-dc111d3012cb','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','经典双拼米线（吊龙+炸酱）',NULL,21,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5e667e5-ef60-5f69-ad4f-314dab90baae','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','生烫梅花肉米线（清汤）',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('02142d9e-1364-5cd6-899b-f51e95d80b56','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','牛肉丸生烫米线（清汤）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('283c4dc0-aeef-5358-b11a-bc9aa5115c71','0d5288b0-b521-5b91-a419-8e019adc2b37','牛肉滑蛋饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5bf8ecb7-733e-56ef-ae00-d60098af28a8','0d5288b0-b521-5b91-a419-8e019adc2b37','鲜鱿滑蛋饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1ec99e91-2a76-5394-baa2-c27bbf132b1c','0d5288b0-b521-5b91-a419-8e019adc2b37','爽嫩虾滑滑蛋饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c6e43fca-244e-50af-8945-b1b585c77dee','0d5288b0-b521-5b91-a419-8e019adc2b37','精品五花肉滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bbbf5bc8-8527-563a-9c0f-2fcbe7d171e6','0d5288b0-b521-5b91-a419-8e019adc2b37','奥尔良烤肉滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6a1af756-2830-52b9-82e8-935e4a2d3dc4','0d5288b0-b521-5b91-a419-8e019adc2b37','滑嫩鸡丁滑蛋饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9046bcb4-1e31-5034-9dde-58552f7797ac','0d5288b0-b521-5b91-a419-8e019adc2b37','肉松滑蛋饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bb756a73-6629-5d97-be2a-b7719e3f3c3a','0d5288b0-b521-5b91-a419-8e019adc2b37','迷你火腿滑蛋饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('42a980d2-aa2a-510b-8d4f-a956b531fc73','0d5288b0-b521-5b91-a419-8e019adc2b37','广式腊肠滑蛋饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d71a9fc-a87d-5e2f-8ef8-9c7b092404cb','0d5288b0-b521-5b91-a419-8e019adc2b37','排骨鸡米饭',NULL,23,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8f4d9817-e42c-543b-9859-f420763c3e17','0d5288b0-b521-5b91-a419-8e019adc2b37','黄焖排骨饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b081e5f0-145b-5bb6-964c-648cc633633d','0d5288b0-b521-5b91-a419-8e019adc2b37','黄焖五花肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f1b1e70b-b3da-5a39-adf0-888b7dff0c1a','0d5288b0-b521-5b91-a419-8e019adc2b37','黄焖小酥肉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('67c55972-2d7f-5083-9d40-48a094f690eb','0d5288b0-b521-5b91-a419-8e019adc2b37','黄焖鱼豆腐',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3800f310-0cf7-525d-bdda-c0420d8013be','0d5288b0-b521-5b91-a419-8e019adc2b37','黄焖茄子饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5b8da016-1464-55cf-8d83-69ae71280e6c','0d5288b0-b521-5b91-a419-8e019adc2b37','黄焖鸡加金针菇',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('971e9e46-d1bc-57a0-8871-4c1a2c8368f3','0d5288b0-b521-5b91-a419-8e019adc2b37','番茄烤肉拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6cc797e3-b8cd-57d6-a23a-a06c910fd1bc','0d5288b0-b521-5b91-a419-8e019adc2b37','香辣烤肉拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5bcfc03f-aa6d-5777-9126-31671b72f297','0d5288b0-b521-5b91-a419-8e019adc2b37','沙拉烤肉拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a3ca493e-e935-51e8-8225-330298c82449','0d5288b0-b521-5b91-a419-8e019adc2b37','千岛烤肉拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b57844fc-4507-506b-9788-323c12f5719f','0d5288b0-b521-5b91-a419-8e019adc2b37','黑椒烤肉拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e0623999-ece3-5f73-b658-38fef443144d','0d5288b0-b521-5b91-a419-8e019adc2b37','甜辣烤肉拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f32d715c-c335-5bb7-8c7b-20ba9db716b3','0d5288b0-b521-5b91-a419-8e019adc2b37','红油耳丝拌饭',NULL,21,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7bb13f3-7610-5e4d-863b-ac3b21a19e76','0d5288b0-b521-5b91-a419-8e019adc2b37','五香猪脸拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3ccd6c34-132b-5c2a-9eaf-00ce6178ca0f','0d5288b0-b521-5b91-a419-8e019adc2b37','传统酱肉拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('134114f3-a3e4-5d94-9688-e0fd69534866','0d5288b0-b521-5b91-a419-8e019adc2b37','馋嘴鸭拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5d1e0246-aea5-545c-88ff-0d1c9f5ede78','0d5288b0-b521-5b91-a419-8e019adc2b37','川湘小炒肉拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1bf5eab9-590c-5b57-8c13-a077576bdafe','0d5288b0-b521-5b91-a419-8e019adc2b37','霸王炒鸡拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2ddea456-ad20-574f-8ff2-6aba8cb399d6','0d5288b0-b521-5b91-a419-8e019adc2b37','可乐鸡翅根拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e4314342-4085-50ce-847d-eb206ab4c307','0d5288b0-b521-5b91-a419-8e019adc2b37','口水鸡拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('979ac893-cbbf-5687-b3d5-37630bf8c6b4','0d5288b0-b521-5b91-a419-8e019adc2b37','照烧鸡肉拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3081097a-58b1-5133-bcc3-72eef7400a69','0d5288b0-b521-5b91-a419-8e019adc2b37','鱼香肉丝拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('13f1c83a-66ec-5ee8-a1f6-811a15d9557d','0d5288b0-b521-5b91-a419-8e019adc2b37','咖喱鸡肉拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f31b9b0-fa64-5ce8-9eed-22b8aad18da5','0d5288b0-b521-5b91-a419-8e019adc2b37','川香小炒肉拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1b4349dc-7999-53f0-bc6f-60ec620c91c8','0d5288b0-b521-5b91-a419-8e019adc2b37','可乐鸡翅根拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('88f8785c-13c4-5310-a3e0-00f8fc684875','0d5288b0-b521-5b91-a419-8e019adc2b37','鱼香肉丝拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bf68bdd3-fd5d-5914-b7a6-bf8c473702d7','0d5288b0-b521-5b91-a419-8e019adc2b37','鳕鱼排拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10f983f0-b800-524d-bbe9-5fec1ee5385a','0d5288b0-b521-5b91-a419-8e019adc2b37','日式照烧鸡肉拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8a2fdb40-0a2c-50ad-a798-906dfc9ab496','0d5288b0-b521-5b91-a419-8e019adc2b37','泰式咖喱鸡肉拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fb6cb6f3-a77f-5c8a-933e-39e74481f807','0d5288b0-b521-5b91-a419-8e019adc2b37','五花肉土豆粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fd67702c-e9c9-5f74-99c6-7070a4c4f3a5','0d5288b0-b521-5b91-a419-8e019adc2b37','鸡块土豆粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fcdfcc31-e649-5679-a575-930dcc984d0b','0d5288b0-b521-5b91-a419-8e019adc2b37','肥牛土豆粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73284c78-e863-5b86-8dcf-2f578314eeeb','0d5288b0-b521-5b91-a419-8e019adc2b37','鱼丸土豆粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('085d99f1-654a-5c43-a53f-8851fff97f7c','0d5288b0-b521-5b91-a419-8e019adc2b37','鱼豆腐土豆粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7807963e-3411-58f1-8ccf-d9bc8df83d61','0d5288b0-b521-5b91-a419-8e019adc2b37','烤肠土豆粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a3505ea-b9f3-5dc3-b67e-b388320dd143','cdf98f90-9fca-56e5-8a0b-393265d055c7','上海生煎包',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0775ac33-a7e8-5d03-ac91-bc8ddc010936','cdf98f90-9fca-56e5-8a0b-393265d055c7','猪肉芹菜水饺 小份15 个/',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('37900d17-a21a-58f2-9b3c-53c891de8404','cdf98f90-9fca-56e5-8a0b-393265d055c7','猪肉大葱水饺 小份15 个/',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c5118a64-b98c-583b-a52c-aaeb5dbdca39','cdf98f90-9fca-56e5-8a0b-393265d055c7','猪肉韭菜水饺 小份15 个/',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('227b0a4e-648d-535c-a475-e231dbe53454','cdf98f90-9fca-56e5-8a0b-393265d055c7','猪肉白菜水饺 小份15 个/',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f486f5f2-979e-54e6-8fe4-2f9559b6f0fe','cdf98f90-9fca-56e5-8a0b-393265d055c7','猪肉茴香水饺 小份15 个/',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4269c34d-2b94-5a3a-9ee2-feca2708b79d','cdf98f90-9fca-56e5-8a0b-393265d055c7','猪肉莲藕水饺 小份15 个/',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8864332b-c317-5104-bf20-966d2eba34b3','cdf98f90-9fca-56e5-8a0b-393265d055c7','韭菜鸡蛋水饺 小份15 个/',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('40b00c5d-42a5-561c-93f5-5b6b6d15611b','cdf98f90-9fca-56e5-8a0b-393265d055c7','西葫芦鸡蛋水饺 小份15 个/',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e21cdd3a-10c5-5ba1-afc6-a862842446ca','cdf98f90-9fca-56e5-8a0b-393265d055c7','酸汤水饺（+2元）','对应水饺加酸汤需+2元，水饺基础价待核',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e731968e-b5fd-5fdc-917e-c8a5c205e6b5','cdf98f90-9fca-56e5-8a0b-393265d055c7','猪肉三鲜',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e613ae4-95e9-5341-adfd-1bab0d5ee8dc','cdf98f90-9fca-56e5-8a0b-393265d055c7','满口香水饺',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4cae171a-1c92-58e8-92c3-c3144e1365c4','cdf98f90-9fca-56e5-8a0b-393265d055c7','猪肉玉米粒',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('69ce707f-3db5-5f9d-9f57-6798d9efc4c6','cdf98f90-9fca-56e5-8a0b-393265d055c7','烧饼加菜',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('58a0b4f9-c16f-5efd-b0a2-9a1a7be0f497','cdf98f90-9fca-56e5-8a0b-393265d055c7','烧饼加鸡蛋',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73b9f2d8-da63-5211-85c7-2cb1a05251c0','cdf98f90-9fca-56e5-8a0b-393265d055c7','烧饼加肠',NULL,4.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d625357-ffe1-5426-b224-1d569d6571da','cdf98f90-9fca-56e5-8a0b-393265d055c7','蛋肠双夹',NULL,5.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0f648d93-29aa-5ce5-a810-ab863857ca80','cdf98f90-9fca-56e5-8a0b-393265d055c7','烧饼加鸡柳',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9742de2a-5d8e-588e-b0b7-35ac0908f786','cdf98f90-9fca-56e5-8a0b-393265d055c7','烧饼加卤肉',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('92ae1896-dafc-5c8b-8bb7-fb3ab199c0bb','cdf98f90-9fca-56e5-8a0b-393265d055c7','烧饼加鸡排',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e20a625-4560-5f6c-91b4-8feb96c9b30f','cdf98f90-9fca-56e5-8a0b-393265d055c7','蔬菜卷饼',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b8cfe27c-95fc-5475-aa95-e21c0639214f','cdf98f90-9fca-56e5-8a0b-393265d055c7','鸡蛋卷饼',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c00d8186-64f3-5826-b049-4ac18b453b85','cdf98f90-9fca-56e5-8a0b-393265d055c7','香肠卷饼',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('66139fa7-426e-5c08-84a8-b06556a32e82','cdf98f90-9fca-56e5-8a0b-393265d055c7','鸭肉卷饼',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('14b1b6f4-fb70-51d3-b86e-97879d04a671','cdf98f90-9fca-56e5-8a0b-393265d055c7','鸡柳卷饼',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9459d385-92f2-5f0a-aa18-592953873026','cdf98f90-9fca-56e5-8a0b-393265d055c7','卤肉卷饼',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('efa4795a-21d9-57ad-9861-eda01a013fd5','cdf98f90-9fca-56e5-8a0b-393265d055c7','浓香螺蛳粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c8fac3aa-da85-5496-ba6a-f99ceb98ef50','cdf98f90-9fca-56e5-8a0b-393265d055c7','干捞螺蛳粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('391a8316-b9c8-5b9f-b7a5-f3aea11803c3','cdf98f90-9fca-56e5-8a0b-393265d055c7','豆泡/卤蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('684ca1f2-0a37-5a4a-baa5-bd04803b2fe6','cdf98f90-9fca-56e5-8a0b-393265d055c7','兰花干',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('458b1bbb-4714-583e-95c2-6c6ddad8fdd1','cdf98f90-9fca-56e5-8a0b-393265d055c7','响铃卷',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('28f220d6-17de-5347-af71-e32990bb92f0','cdf98f90-9fca-56e5-8a0b-393265d055c7','台湾烤肠',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9a38043e-5994-501c-a83c-62e228b1caf2','cdf98f90-9fca-56e5-8a0b-393265d055c7','炸蛋',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b2a7dd79-d0a8-5ded-9fe0-e7903f919b8b','cdf98f90-9fca-56e5-8a0b-393265d055c7','黄金猪皮',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bea34b96-8d8b-5ac0-8e86-caff17f2baf7','cdf98f90-9fca-56e5-8a0b-393265d055c7','原味米粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d28d3346-1cbd-56c5-b331-73a70e7300e9','cdf98f90-9fca-56e5-8a0b-393265d055c7','酸辣米粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26ab6b17-750b-5d42-9c01-78b011a5409c','cdf98f90-9fca-56e5-8a0b-393265d055c7','酸菜米粉',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b9a64e05-cf16-56ff-bcfa-119f8600d8e8','cdf98f90-9fca-56e5-8a0b-393265d055c7','金针菇米粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b14fb789-ef2f-5096-8906-c89e38fcf771','cdf98f90-9fca-56e5-8a0b-393265d055c7','鱼豆腐米粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cbf9f48c-5695-5dbf-8391-4aea707ed4bb','cdf98f90-9fca-56e5-8a0b-393265d055c7','笋尖米粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9c7eaab7-9b7c-5143-b321-22c258142e3a','cdf98f90-9fca-56e5-8a0b-393265d055c7','招牌肉末米粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cc6fc3ea-2c10-5d68-88d7-ad85069200fb','cdf98f90-9fca-56e5-8a0b-393265d055c7','牛肉米粉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b911a724-f9c8-5917-bb22-2a0df6534491','cdf98f90-9fca-56e5-8a0b-393265d055c7','鸡丝米粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('14984c89-1763-5a55-b9e0-0701f3010d0d','cdf98f90-9fca-56e5-8a0b-393265d055c7','鸡肉米粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('751a7f36-5e13-53a2-bf69-2a67da8a6564','cdf98f90-9fca-56e5-8a0b-393265d055c7','原味拌粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5da3bfc5-cbd6-5b1f-9d4b-1196736855e1','cdf98f90-9fca-56e5-8a0b-393265d055c7','菌菇拌粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8773ebf9-6a2e-5e19-b930-1d37afc57f44','cdf98f90-9fca-56e5-8a0b-393265d055c7','笋尖拌粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('944b5101-cb1b-59f0-949a-8439aefe1902','cdf98f90-9fca-56e5-8a0b-393265d055c7','鸡肉拌粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('be6e1010-e83d-5376-9624-2df6a9bcce8e','cdf98f90-9fca-56e5-8a0b-393265d055c7','肉沫拌粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7d1e022f-9153-5cad-88d6-8fc8b1417d25','cdf98f90-9fca-56e5-8a0b-393265d055c7','牛肉拌粉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b9e3d2e9-788a-5517-aa58-d5aebea15262','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','缤纷水果比萨',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b8514e5e-42ba-5b04-bf12-8ce20fb5b147','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','招牌牛肉比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('317d8b86-ee6c-5066-8056-2a4cdc05ee36','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','BBQ 鸡肉比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3cd412f6-4b1c-5934-bd4e-32c9d2b1a4e8','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','美乃滋土豆培根比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('588d325e-a561-5409-bdb2-f80481a10d6c','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','金枪鱼比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('841eac04-7e6e-5a85-8664-9bf6fb8e4cca','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','夏威夷比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0dc2fbe3-9e09-52cf-982b-462b81f3e2fb','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','超至尊比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d19ec601-01e3-561a-a77c-931a7080fe0a','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','金丝肉松比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b993250e-ed9d-5b98-ba32-5609123a8033','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','意式红肠比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('23330528-1733-5880-80d2-9893558909d5','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','眉州香辣比萨',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9fd114eb-0bb4-5ebd-9f71-d31255ba99ca','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','榴莲多多比萨',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7a3a95cf-57f1-59b5-ae05-58e854ec6469','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','地道肠',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('02588b81-0c60-5e80-a6b7-5bd33f3255a7','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','蛋挞2 个',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7a83eed-2f7f-517e-80b2-0dd515f706c3','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','奥尔良翅根2 个',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3af13dd0-de00-5d05-b685-4b9ca80b55ac','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','美味薯条',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6cc4e3a5-904c-5cb5-b631-c4a70377cb68','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','香芋地瓜丸',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e0e052b8-0952-5da5-a6f3-437d45d65a7a','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','香辣孜然鸡米花',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('15017039-6ed9-513c-9766-7f2041f956e8','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','香甜芝士玉米',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7b9f4b7d-8254-55ea-956a-1a59899ea0a1','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','蛋香培根烤薯条',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('36a76d7b-734d-5827-9e76-d054a33da32a','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','鸡扒配薯条',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ed3c2174-39c3-5d16-9061-4aa5bd283598','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','烤肠配芝士鸡扒',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('81c57017-ddd9-52a9-9652-3cd2e6eb00d1','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','2 块比萨+1 份小吃：',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a9c17a71-a1f7-58ba-b298-a2001857be6c','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','2 块比萨+1 份前菜：',NULL,23,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('13463b52-e6a9-55e8-a412-1e7cb0391780','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','羊肉烩面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('91f8892c-5d9f-5cd6-9df5-de0adf243b31','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','羊肉汤',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2016dc1d-68bc-5bfd-be5b-36185d4b96da','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','芝麻烧饼',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ce2bff99-bd10-5908-bb48-0b6ffd5c7831','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','粉条',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cbe39550-81bf-5d5c-aa0d-b1dcc0e00c9f','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','粉丝',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e1dcf46-46be-5c25-a954-2aa47f0c311a','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','千张',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b7dba6aa-f160-594d-91b0-fc2e78fb36b8','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','羊肉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('baef4679-fbe6-51fc-b725-cf51368559ff','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','三杯鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bd76c092-67f5-558d-9fd6-e863e6dfb6f0','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板芝士鸡丁',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1bca2114-ce63-5f2b-933a-8b85f0662204','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板咖喱鸡丁',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fcc90cca-49be-50fa-aba3-874e5bb8ac19','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板黑椒鸡丁',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4dda8bc1-8057-5e72-902f-a7522fd8b97b','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板照烧鸡排',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f5a38abc-c7c1-57ef-a4d1-74c265d7b225','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板照烧牛排',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f4f17869-8469-5fa1-9b70-a6cd553d12f9','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板咖喱牛肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2ee66fc1-5e17-55cd-a01a-31dac09fb4aa','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板黑椒烧肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80aa2195-8b18-5d94-9741-b34aa9c29317','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板肥牛饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('67fde4a2-f8c2-5f20-ab3b-3b460c67c10a','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板黑椒五花肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ca1fb5d4-4da3-5d70-95fe-8ee8d907a119','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板黑椒牛肉',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cc113264-fb3a-5b3d-bb21-23feabcdcc9c','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','酸辣土豆丝',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('054ed8e6-1f22-55ef-b4ee-352c7c3a6a6e','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','香芹苗炒香干',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80bb4d1b-9af9-5828-9c82-35d096c6c4af','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','茄子豆角炒五花肉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cd408dd2-a450-5f24-bdce-fdad97a651fd','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','螺丝椒炒五花肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c1c0f98-7776-5dfa-a862-adada6649d0c','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','豆角炒肉片',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8dbc2db1-012e-5949-a718-ee490a1c60d5','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','苦瓜炒肉片',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('30abbf72-2d47-5818-a022-62f9962e653d','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','木耳炒肉片',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9770a923-14e8-58b2-aaf9-bdfc947ba83e','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','香菇炒肉片',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1c657829-057b-51a2-bbf7-c4932d609db3','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','小炒黄牛肉',NULL,26,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('175e5b54-2e3c-5825-b2d8-346f4b8b7ce1','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','冬笋炒牛肉',NULL,26,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a6af6c3-a0b4-5305-aecb-fc4b7e18ea8d','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','酸豆角炒牛肉',NULL,26,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('87073e01-2722-55cc-9047-cb3e3ed16e38','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','土豆片炒肉片',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('50468b35-65fd-5e43-bab5-24c0dab5ca1a','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','有机菜花炒肉片',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5efd70b9-1296-525f-a50f-0b55e3c91966','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','酸豆角炒肉片',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f20b0226-2299-59fc-9ba4-c7ed78197319','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','螺丝椒茄子炒肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('652764a7-ef78-52b3-8ca7-70d1510a1308','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','黑木耳炒肉片',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fb672ebc-9eee-5ee8-be7f-a5f39a3d4fad','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','蒜苗炒肉片',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('65d57d6f-f178-53ec-aad2-91eec6e806e8','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','腐竹炒肉片',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d53635d8-bf3b-5abb-85e0-afa012294489','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','酸豆角炒五花肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b8a39ce-b6d8-521c-b691-7af48b9bbf49','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','香芹炒五花肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('47ebb85c-e6f2-5d9d-a0ed-c3275b95d32e','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','冬笋炒肉片',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dd106a4f-a3d3-5d7c-9563-e45ca256b24f','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板牛肉意面',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2279d632-4831-588b-830f-235305964db1','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板意面',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d052afc4-7306-5f01-8871-fbddf2c5544a','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板鸡排意面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f9ad64a3-f8b0-5c10-9101-dcb874e47db8','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板牛排意面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3b84e564-cc11-5261-a570-ef0c6d7acbd1','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板里脊意面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cc2e3541-4316-5b5a-8508-35f5cf21da15','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板烧肉意面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c1134ea-0544-5b72-8e1d-f3fd5bfe5b1b','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板肥牛意面',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ac70c25c-99d6-5228-938b-5f3c9ec0f7f3','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板五花肉意面',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b3bd836-d0d1-5a38-a470-dabd53f8c72c','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板乌冬面',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a5157e4-9d98-5420-9f54-394bb21441ee','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板鸡肉乌冬面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9207e8a5-a940-50b5-a154-d5f52af49745','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板鸡排乌冬面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a37c9fae-23a6-516a-b4ba-42a111c3ac01','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板里脊鸡肉乌冬面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('309731e8-f2dc-5a57-97f9-5d1d31598d3a','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板牛排乌冬面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('852ecd69-e79d-5d8b-a966-2a41d84d4d1e','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板肥牛乌冬面',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3507cdc8-996c-5577-90ed-418d2b60e5ef','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','铁板牛肉乌冬面',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5f30c70-8a6f-5894-b63d-efd3f4893b44','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','宜宾燃面（干拌）',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d06f9696-8c91-567b-82e8-793a7d80442e','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','生椒牛肉面（干拌）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cb5c20d4-c52e-5745-8323-2a146af7422e','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','重庆豌杂面（可干拌/可带汤）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('95592ce8-91e2-5a34-bf3d-0318fa8fcbc9','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','天府牛肉面（带汤）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('84ad4566-f9a6-5612-9724-ed764a389409','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','泡椒牛肉丝面（可干拌/可带汤）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c4df3a2-8500-5543-bbbc-a93319abc704','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','老成都杂酱面（干拌）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4dc04a2e-cb6d-57ee-b1cd-3e1b6c7ab7b4','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','酸菜肉丝面（带汤）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ead02566-5ed7-5c20-85c9-a2c8c20612d8','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','渝香小面（带汤）',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('04cf7b26-eca1-53c3-aea3-8d558c4a3615','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','鸡丝拌面（干拌）',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d356d3ce-4a5f-535a-a559-3b62d95cf43b','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','豌杂米线/粉条',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('29b69531-b01f-5047-964c-65e570e4e8a8','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','泡椒牛肉丝米线/粉条',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('25f05e5d-73c9-5507-bfb5-358958280fc3','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','酸菜肉丝米线/粉条',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('40c2b0fc-86c9-5626-9945-4c8abbc8c5e9','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','天府牛肉米线/粉条',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('544b0fb7-9657-56ab-a3c5-5021099d57d4','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','酸辣米线/粉条',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('469fc6bc-be77-51c1-bf98-d835a0a9a022','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','红油抄手（招牌）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('85428963-e2bf-5aaa-8387-74c4192e14a7','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','酸菜抄手',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0bd49ecd-04a5-543d-9e7d-e18e73b01d3c','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','清汤抄手',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a905310e-1a22-57e0-85dd-7f0c50912d6f','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','干拌抄手',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c2d3b818-9d65-58e9-beae-37f591cb8dbe','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','蒜泥抄手',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6c264be9-10fb-5fdf-8684-7df643f1c29e','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','现炸小酥肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3346c217-31a2-5897-9c0d-6a24b5498289','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','红糖糍粑',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ee2290ae-a432-53b6-be7d-ad3dcc119c48','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','锅巴土豆',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('11d13087-4f3e-5506-8c76-f6fa724c10a1','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','卤蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('03839413-389f-5903-9a42-fe8fa5c1b5f9','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','淀粉肠',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b7d4db9a-84b6-5632-8f0a-0830e7283b9f','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','醪糟汤圆',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('81ae33e3-1d1b-579e-aef2-0e1e573806b7','903d4bfb-d232-5394-b99f-052983a41abc','鲜炒鸡小锅',NULL,38,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('38fe093d-f2d2-503c-8a7d-5c863e953768','903d4bfb-d232-5394-b99f-052983a41abc','鲜炒鸡中锅',NULL,54,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('85e66efe-47a6-596f-ad91-05919871798a','903d4bfb-d232-5394-b99f-052983a41abc','单人炒鸡饭',NULL,14.9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8eb2bf07-9e13-53f3-97d1-a74db2277212','903d4bfb-d232-5394-b99f-052983a41abc','单人炒鸡面',NULL,14.9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('30360daf-ffe0-5820-bb55-6e5da1b7af21','903d4bfb-d232-5394-b99f-052983a41abc','单人大虾炒鸡饭',NULL,17.9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('651eaf4c-8147-5970-b616-275e8930ac92','903d4bfb-d232-5394-b99f-052983a41abc','单人大虾炒鸡面',NULL,17.9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('611e11cd-6154-5525-9734-f4f8464a5949','903d4bfb-d232-5394-b99f-052983a41abc','单人排骨煲',NULL,17.9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4cd1fc5e-e631-5bc6-a37a-05542efc122e','903d4bfb-d232-5394-b99f-052983a41abc','单人大虾排骨煲',NULL,22,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b399b02-9687-58ac-b7a2-cd6be0437e8d','903d4bfb-d232-5394-b99f-052983a41abc','鲜炒排骨小锅',NULL,48,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b86958dc-e9c5-5a65-b2bf-5b5866bd8270','903d4bfb-d232-5394-b99f-052983a41abc','鲜炒排骨中锅',NULL,60,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('303bfc11-1fcf-5c1c-9ad4-65e19779729f','0cf77375-5b1a-51e5-bd34-12024315133d','奥尔良鸡肉饭',NULL,13.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c2ddfe50-c3a7-5e13-9ad2-9da1e8ef1505','0cf77375-5b1a-51e5-bd34-12024315133d','红油鸡腿饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ba3648e5-1289-50b4-ae97-5d39a2894a44','0cf77375-5b1a-51e5-bd34-12024315133d','黑椒鸡排饭',NULL,13.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4ff4b431-8017-510c-818d-5b026090d89a','0cf77375-5b1a-51e5-bd34-12024315133d','手撕鸡饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9a9d0138-7c0a-5117-a637-2ae9bd7e6aba','0cf77375-5b1a-51e5-bd34-12024315133d','麻辣鸡排饭',NULL,13.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('88aed9b5-0fe4-542a-b99c-1c0a26017c00','0cf77375-5b1a-51e5-bd34-12024315133d','脆皮烧鸭饭',NULL,15.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('489a52c0-99bf-57ee-bd1c-f53937e583cd','0cf77375-5b1a-51e5-bd34-12024315133d','脆皮鸡排饭',NULL,13.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7c62cc6-4575-5552-9e90-8d31ff5ca5d8','0cf77375-5b1a-51e5-bd34-12024315133d','隆江猪脚饭',NULL,17.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('19bc69a1-9ff7-5379-a4ae-c9d6a4284d84','0cf77375-5b1a-51e5-bd34-12024315133d','叉烧鸡腿饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c038c27d-a3c0-59c3-b9eb-8abfdbadf764','0cf77375-5b1a-51e5-bd34-12024315133d','蜜汁叉烧肉饭',NULL,17.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('78e86baa-6b34-549a-a5c8-28ef80cb9a9a','0cf77375-5b1a-51e5-bd34-12024315133d','蜜汁鸡腿饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26dabba1-3672-53a9-9ea8-5f0b58537461','0cf77375-5b1a-51e5-bd34-12024315133d','东坡肉饭',NULL,17.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1b2d6627-b200-5180-995a-ca4a31fb6b6d','0cf77375-5b1a-51e5-bd34-12024315133d','脆皮烤鸭腿饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('07140bba-8c6c-5973-8217-fd01faad5fb3','0cf77375-5b1a-51e5-bd34-12024315133d','脆皮五花肉饭',NULL,18.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('015de8a6-0bbb-5c3b-b459-f55ded5b3fa7','0cf77375-5b1a-51e5-bd34-12024315133d','湛江白切鸡饭',NULL,14.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('86b166e1-39d2-5c45-9eb2-df9c0d61ab34','0cf77375-5b1a-51e5-bd34-12024315133d','双拼饭',NULL,18.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cf7f5ce9-e65b-5674-b4a1-f5a699286fbe','0cf77375-5b1a-51e5-bd34-12024315133d','爽口土豆丝',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e6ca2d43-e405-5eb2-b457-d8449d7faafe','0cf77375-5b1a-51e5-bd34-12024315133d','青椒豆芽',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fc6d476e-6b75-5bda-80f2-9815cfa93ff6','0cf77375-5b1a-51e5-bd34-12024315133d','香菇油菜',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8724a199-56a1-50fa-9ea6-a654368f1283','0cf77375-5b1a-51e5-bd34-12024315133d','手撕包菜',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('acb856f0-31ba-58a7-949e-a9cf85bb1404','0cf77375-5b1a-51e5-bd34-12024315133d','西红柿鸡蛋',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('df19c602-244c-50d0-a237-62817c6987ef','0cf77375-5b1a-51e5-bd34-12024315133d','清炒西兰花',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a2d4c1e5-17b3-5f41-924b-589944186184','0cf77375-5b1a-51e5-bd34-12024315133d','肉沫麻辣豆腐',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('62179966-c4a2-539b-9c62-e52dda6e5173','0cf77375-5b1a-51e5-bd34-12024315133d','肉沫茄子烧豆角',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('044ad645-13c8-59f5-9590-00bd70790fde','0cf77375-5b1a-51e5-bd34-12024315133d','鱼香肉丝',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9adb27e5-0c93-5a98-b0b8-737d7281adb8','0cf77375-5b1a-51e5-bd34-12024315133d','京酱肉丝',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45eeacc1-f1e4-591d-b25c-4c242aca7d41','0cf77375-5b1a-51e5-bd34-12024315133d','港式烧鸭',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1addf190-0020-5cbf-8524-f2f93401fad3','0cf77375-5b1a-51e5-bd34-12024315133d','韩式年糕鸡',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1b4b796e-1f40-570a-9ffd-415004f8a142','0cf77375-5b1a-51e5-bd34-12024315133d','麻辣干锅鸭',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b9a6bfa3-117a-5847-82db-96dfa943236c','0cf77375-5b1a-51e5-bd34-12024315133d','新疆炒鸡',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ccca1ca0-3085-54ce-a9be-bff69cdf3335','0cf77375-5b1a-51e5-bd34-12024315133d','粉蒸肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('31d13d92-3064-5589-9aee-ada191a223a3','0cf77375-5b1a-51e5-bd34-12024315133d','粉蒸鸡',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eb37d237-6fc5-58d3-8c5b-ae87d7560703','0cf77375-5b1a-51e5-bd34-12024315133d','秘制红烧肉',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ba167f6d-2586-5720-affa-d13257fc7799','0cf77375-5b1a-51e5-bd34-12024315133d','老式锅包肉',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1a3ece87-ebf4-5c38-baef-3103880f9409','0cf77375-5b1a-51e5-bd34-12024315133d','香辣排骨',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f9489e35-a77c-52b2-92c2-bb400fdfca5d','0cf77375-5b1a-51e5-bd34-12024315133d','黄焖肘子',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9eec5661-e8df-5833-b7c3-1a750803ba64','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','小炒干页豆腐',NULL,6.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8b1bc65a-2712-5b36-8ee2-a1e2e2b1efba','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','外婆菜炒鸡蛋',NULL,6.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aefa4962-2a28-568a-8942-7d34b2af272b','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','香芹小炒肉',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('83644806-6d97-5710-baa1-0814a4bb7626','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','农家一碗香',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d7e8d08f-6b82-5ba4-a8a5-c3666fbf2bec','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','湘味花菜炒肉',NULL,7.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a728d2e6-5cb2-5b07-9339-2777e7aabee2','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','黑椒肉片',NULL,7.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ec55fe5f-f771-573b-a1ed-63823b74cf51','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','鸡杂酸豆角',NULL,7.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a62bf2a1-66df-5036-ab5a-4bbb499e1e22','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','土豆片炒肉',NULL,7.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e18a5d28-7031-5d19-ad9e-2ce159505716','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','豆花肉片',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80034d04-e34f-50f6-82da-8ebb705e5252','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','麻辣水煮肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1cdedc1-9a56-5199-ab41-e28f3304d7b5','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','孜然脆骨',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('23494e30-4368-50af-a9e7-9e817058bd39','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','椒麻辣子鸡',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c1a5ea58-5903-5434-a243-e09a6eeca4f3','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','湖南小炒肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8e474ebb-51ea-55ac-847b-d86f4da8eb7b','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','川湘回锅肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('524026f6-bf44-5ef0-9b49-7939a28be029','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','椒麻莲藕饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9ea23175-398e-5597-a566-648cb6af8623','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','椒麻鸡肉饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0cf5f2f6-add8-586f-b387-7befdbe2ef14','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','素双拼饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a12c8e71-3f23-5ecc-bdaf-515c2368165d','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','椒麻鸭肉饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45425f9f-3cbb-584c-a62d-0c38a5d04aab','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','原味土豆片饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5b490212-7da7-52cd-8f86-d262bbca42be','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','椒麻香肉饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a61688c-7f92-5ffb-b509-f064819f6426','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','原味豆干饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10d8fd72-1c2d-5467-b393-f18e9c185df3','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','原味鸡腿饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('013a7b64-8c60-5a1a-b1cd-479aa29c4564','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','椒麻肉肠饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fb03287a-7401-549f-8e49-92c31e5d4c57','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','原味鸭腿饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e21c6299-0ce8-559c-9d9a-62633ba9f4d0','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','原味鳕鱼饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f362b9d-327d-50f7-99dc-194615f1f377','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','原味鸡排饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1f80fa87-e796-51db-90d7-63a2e2c7ed27','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','孜然香酥鸭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0ae373b0-69d4-5f17-bc0d-ecdbd89d382c','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','椒麻脆骨饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a7ba16d0-fa3e-5e41-b939-5fd6c8c643bd','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','红烧肉饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('61b2cb51-3cfd-50fd-a036-1ef7749bfcdd','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','肉双拼饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5522dee-5e1d-5a31-a7e8-1ab3ba4169ca','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','浇汁羊肉饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('db340a09-427a-51c4-987c-86d26a94699d','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','招牌卤肉饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1583c4c5-fd3d-5838-8f2d-98f9d891ca6c','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','猪肉拼猪肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e5c86c0-b6b7-5910-aba9-31b991f64ed8','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','原味鸭胗饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('680e6fed-2966-5895-be1b-bb286aafc5b8','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','原味肘子饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('529ec834-d2e4-5445-90d4-4f67480fcaa2','468eae22-d558-5a7e-891a-a21ae01f6c42','清汤抄手',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('09205971-9569-5a2c-bcfe-52d1afd4bad4','468eae22-d558-5a7e-891a-a21ae01f6c42','麻辣抄手',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('75e04df7-fbc6-5145-8388-877036890d04','468eae22-d558-5a7e-891a-a21ae01f6c42','干拌抄手',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8259085d-f96b-5609-a603-54b4a390e75d','468eae22-d558-5a7e-891a-a21ae01f6c42','招牌吊龙米线',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0eef38d0-c74a-5f07-a2cd-9bab042c6ca6','468eae22-d558-5a7e-891a-a21ae01f6c42','生烫羊肉片米线',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('48ae847a-0144-5599-8518-2fdcbaba7bb4','468eae22-d558-5a7e-891a-a21ae01f6c42','红烧牛肉米线',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c505e068-001e-581e-9f45-f619c27e2f38','468eae22-d558-5a7e-891a-a21ae01f6c42','生烫梅花肉米线',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bdaa7900-4aa5-52d4-976f-f70cc6562d04','468eae22-d558-5a7e-891a-a21ae01f6c42','泡椒鸭胗米线',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45d38194-6167-50a6-a77e-da74c781f6d1','468eae22-d558-5a7e-891a-a21ae01f6c42','云南炸酱米线',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('50db626e-9459-5be3-80d5-c75da1c6eea2','468eae22-d558-5a7e-891a-a21ae01f6c42','牛肉丸米线',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('74390b29-8e70-5c76-b499-b3856c78c2ad','468eae22-d558-5a7e-891a-a21ae01f6c42','生烫鱿鱼花米线',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('52a195f5-65d2-5f4e-9972-a6b2747c5d94','468eae22-d558-5a7e-891a-a21ae01f6c42','泡椒鱿鱼花米线',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('acf09e3e-53f1-51f4-b039-3d833a13e5cf','468eae22-d558-5a7e-891a-a21ae01f6c42','泡椒炸酱米线',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e3f45411-5360-5d58-8cda-d4f0e27fcecd','78eff9cc-c97d-539b-95b6-a7f113f3beed','盐田虾',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('35f30493-bc1d-5760-bb54-4e691482b053','78eff9cc-c97d-539b-95b6-a7f113f3beed','蒸芹菜叶',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6efb5f6c-fac6-56ba-be48-342457cb76c6','78eff9cc-c97d-539b-95b6-a7f113f3beed','蒸木耳',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('952490da-c15e-5075-a359-154d54ab1e8f','78eff9cc-c97d-539b-95b6-a7f113f3beed','蒸土豆丝',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c177421e-dc3b-54fc-94be-dd6956898021','78eff9cc-c97d-539b-95b6-a7f113f3beed','蒸胡萝卜丝',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b51bbe69-d9d5-5543-8b7c-27cd31fa5a10','78eff9cc-c97d-539b-95b6-a7f113f3beed','蒸豆角',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('283b7d9e-c8ce-5082-b388-bb819bac3d82','78eff9cc-c97d-539b-95b6-a7f113f3beed','蒸海带丝',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ee5195e2-fa1b-5877-a4eb-182bf41fdb93','78eff9cc-c97d-539b-95b6-a7f113f3beed','玉米粒',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('55b6a51e-fdf1-5ecd-99e8-a59982a3b2a0','78eff9cc-c97d-539b-95b6-a7f113f3beed','西蓝花',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7852a463-dc1a-518b-a08f-7286fef3be8e','78eff9cc-c97d-539b-95b6-a7f113f3beed','蒸蒿子秆',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bc5588d2-e905-599f-b54e-6cec91df3d81','78eff9cc-c97d-539b-95b6-a7f113f3beed','鸡丝',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e65aea7-6465-5246-b817-a55413a90099','78eff9cc-c97d-539b-95b6-a7f113f3beed','酱牛肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e88c5b3f-69fe-5a03-a225-aba71961f89f','78eff9cc-c97d-539b-95b6-a7f113f3beed','卤豆干',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('00c88cce-a918-5f21-b58e-21fb58375b6d','78eff9cc-c97d-539b-95b6-a7f113f3beed','煎鸡蛋',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('745f3dc2-df92-5d91-8bf2-c315bb952cb9','78eff9cc-c97d-539b-95b6-a7f113f3beed','玉米/南瓜',NULL,0.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e1e6fef-1eb5-5fb3-9b72-8c90c51f1aff','78eff9cc-c97d-539b-95b6-a7f113f3beed','莜面蒸饺',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f90dec99-55f8-5261-aa18-3f548897d9ef','78eff9cc-c97d-539b-95b6-a7f113f3beed','白灼虾：',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('af90d9fa-cd5a-5a12-9eac-7d34292ae95a','78eff9cc-c97d-539b-95b6-a7f113f3beed','其余蒸菜类（蒸蒿子秆/土豆丝/胡萝卜丝/茼蒿/木耳/海带丝/芹菜叶/玉米粒/西兰花/鸡丝）：大份',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f3d23d0-25d2-597f-b7f5-1577c20c5bef','78eff9cc-c97d-539b-95b6-a7f113f3beed','酱牛腱',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d35010a4-a40c-50ac-ab24-59e4f1c3153f','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','思源水',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c7e86ec0-2a29-5bd5-acee-4b8f765819b9','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','东方树叶',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('86db49eb-3abc-5b12-9aa1-dade2412bfa3','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','可口可乐',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('419454b6-e40c-5dfb-8c23-1b6f51052fc7','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','果粒橙',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7c34a834-5d98-5bd1-89aa-bbffd7193593','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','茶π',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('33b71bd6-439b-510a-a854-839f51b12a7c','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','百岁山',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9abf9ec7-da38-5a7f-b513-e00ca587feea','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','红茶',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f695a57-9a6f-5043-9346-6eff56b1f884','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','绿茶',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('936df881-9392-5f75-b7dd-7983a5a3868f','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','白菜鸡蛋',NULL,0.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a4dfea48-15d0-5e84-b49d-551ae5f54803','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','玉米',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d3ea21dc-c42d-5993-b0b1-b3d138064ddb','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','胡萝卜鸡蛋',NULL,0.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('42c31c8d-d5e9-5519-9ba1-c00ad10e1540','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','驴打滚',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8f3af8c7-b620-54c2-a6cc-e9b79d379ea8','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','荠菜鸡蛋',NULL,0.6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('23509311-441e-54c9-aa82-033011054c47','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','麻酱饼',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('44e8d3d2-f388-5a72-92c2-a58550cc6a9e','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','白菜肉包',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('44538adb-5052-5d59-8b4e-48a80a87bdd9','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','糖火烧',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4720276c-8b3c-56a8-bb52-1f09fc014c78','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','豆角肉包',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4d1fddfd-3e06-51a6-9fa3-765a08f0aa23','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','煎饺',NULL,0.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4515cba4-070f-5c6d-82e7-475405014269','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','茄子肉包',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('03eaaef6-d939-5c29-a721-c5919606cc2a','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','手抓饼',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c6bc69cc-1fba-5759-895b-1ff1daeecf8a','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','酱肉包',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e730f9d-c817-5f0a-bd78-324cb04c2bae','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','火腿花卷',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1beb9707-4494-5f77-a2ba-1973a366c69a','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','油条',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('47f70aa4-ece2-5140-98ce-4a6dfca306a7','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','火腿煎蛋',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4cf2bf08-1593-58d3-8650-462aa0b90332','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','葱花饼',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3db98ca0-99b3-5355-b719-dd362f4ad603','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','披萨',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f901153e-d363-5249-9442-80e2fd1001a5','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','馒头',NULL,0.4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c9096e7e-c0c3-51fc-9ab3-81019b63f6e1','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','抹茶卷',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('350d41a7-6d69-5fcb-8c0d-01864d862598','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','花卷',NULL,0.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('79a252b7-b91d-551b-826d-a6abffdde94f','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','麻团',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('96f7553d-4601-5db2-b4aa-c28e956d93a9','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','烧饼夹腿排',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('87ac5668-5783-5256-bb84-32a49c55cfab','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','大烤肠',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aab26ea4-31b0-5cc4-a940-36eaa685e686','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','酱香饼',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d20b846-f810-5203-a726-07afea05cd6d','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','紫薯包',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f5d9015e-1266-509a-862d-d66305cc71f7','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','生煎包',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5233250b-c2d3-5ba1-8f47-3ad2354b7170','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','双夹饼',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6412498f-cd36-5f37-b4d8-7dbc7bf44f4c','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','玉米饼',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('521b4e7b-0eac-5261-b339-cdba36b8db6e','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','土豆丝卷饼',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('405155e3-cf30-59a3-a78e-3496b3a0928c','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','紫米发糕',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('78849f12-bd90-56db-bc6e-d226e43e4304','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','蛋挞',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c90887f0-078f-5549-ae73-e9d706001a89','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','牛肉饼',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e8d73d99-0070-5eea-9a9e-22443f8b485a','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','蛋糕卷',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7a3d4a20-475f-5095-9047-a0715803a27e','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','糖饼',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aab09c70-330f-58e8-a999-b49a0455e544','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','荠菜团子',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ba08f665-9d01-5b0e-bcaf-6ac8881f21a9','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','肉饼',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6917b02b-3e3d-5031-ade7-fa5bd0e6d935','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','面包片',NULL,1.4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5fb88c4-5cb8-59a6-a7a0-569648afaaa0','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','韭菜盒子',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('349baf1b-b48a-5249-bacb-d28147120542','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','果酱面包',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7e1cbbd9-5d7f-54a4-86d0-6c82785d3f00','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','菜角',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('240a7fea-4e3b-53cb-a6a1-1403796d33b1','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','烧麦',NULL,1.2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('588177a4-616f-5e60-a29d-d369c702bfe4','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','红丝绒蛋糕',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9ac667ac-8952-5a8c-ba77-45bf996e2f56','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','双薯饼',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('621e2967-cf64-5225-a7ef-bdcb6a3c5265','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','糖三角',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('847c0832-e2cf-52e1-bb1f-c75ac6fa5043','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','豆沙包',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b1f374b-1bea-5d0c-9119-52fab05a1ecd','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','椒盐饼',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f08e94f5-7667-5299-95db-22a2df96001d','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','早餐奶',NULL,2.1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('722835b5-ab9a-539b-bf76-47067deb5b74','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','纯牛奶',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e9ce420-b47e-52cb-a167-5f73e8da9dda','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','豆浆(杯)',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;

-- 五、档口图集（含首图，首图 URL 与档口 cover 一致）。
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('8e29a5d5-a7c2-5685-b0e0-8cc0a8c3b437','debd2a0e-3cdf-5826-ad05-d5a36ecb01f9','/api/v1/media/8e29a5d5-a7c2-5685-b0e0-8cc0a8c3b437',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('f7c53ed9-6619-582f-bee4-1da580181ba3','2e1baa15-bcce-56a4-9bac-ab1710703001','/api/v1/media/f7c53ed9-6619-582f-bee4-1da580181ba3',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('5ba6183d-cb68-513a-aa3d-a298aac9d6aa','92f5e77d-ace0-5843-a9e7-f9a05aec4fba','/api/v1/media/5ba6183d-cb68-513a-aa3d-a298aac9d6aa',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('5848513b-83fa-5a4a-b4aa-56bf6f28e7d3','86c177b3-6573-5e79-a18b-24341725ef1a','/api/v1/media/5848513b-83fa-5a4a-b4aa-56bf6f28e7d3',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('41d11bcd-d662-5bca-b8b7-a80c7e499e7e','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','/api/v1/media/41d11bcd-d662-5bca-b8b7-a80c7e499e7e',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('648edf20-b770-5da3-973f-4e166f6362c3','3e525ca3-ba7a-55c1-ba19-b2ba8d886bad','/api/v1/media/648edf20-b770-5da3-973f-4e166f6362c3',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('516794f4-99b9-55e0-ad88-d4bebb558a8d','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','/api/v1/media/516794f4-99b9-55e0-ad88-d4bebb558a8d',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('844cfe48-58ed-5d6a-8f39-b8db99aaf5ac','8f31fd77-010e-5c63-8607-f99db544939d','/api/v1/media/844cfe48-58ed-5d6a-8f39-b8db99aaf5ac',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('9fd19f29-418b-5d13-b6a8-d047582cde59','8f31fd77-010e-5c63-8607-f99db544939d','/api/v1/media/9fd19f29-418b-5d13-b6a8-d047582cde59',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('d0ffcd14-eeec-53a6-be82-2fb8fb8d6463','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','/api/v1/media/d0ffcd14-eeec-53a6-be82-2fb8fb8d6463',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('03cfdaae-e7ef-5cab-aab8-69591b0ca32c','385ec11a-5ffe-5022-ae2a-ed9a32c83e17','/api/v1/media/03cfdaae-e7ef-5cab-aab8-69591b0ca32c',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('70938423-7310-59c1-9345-1ffd6396a831','c3f07056-e5a9-50f3-9a0f-7187b34afe7a','/api/v1/media/70938423-7310-59c1-9345-1ffd6396a831',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('dd04abb8-2512-52e2-bde2-4081a3cb1b96','43643b96-1419-59dc-9dff-6e9605f6bb09','/api/v1/media/dd04abb8-2512-52e2-bde2-4081a3cb1b96',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('ac8b97fa-9ab9-55f4-ae32-6eafc5fc1fb1','21622451-9645-5fdf-a5d3-a52e6f8e45d4','/api/v1/media/ac8b97fa-9ab9-55f4-ae32-6eafc5fc1fb1',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('35093422-3144-50ce-b12a-04f0a5674976','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','/api/v1/media/35093422-3144-50ce-b12a-04f0a5674976',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('5e257677-29f3-5696-ad8b-4453f4c55f98','1683fa82-5238-5bfe-9f08-adb9a007ee29','/api/v1/media/5e257677-29f3-5696-ad8b-4453f4c55f98',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('8b98b987-489e-5b16-8a36-a73d5e387906','e605d74f-8c09-5571-b2c7-d64c4a281b2c','/api/v1/media/8b98b987-489e-5b16-8a36-a73d5e387906',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('3ad8f7cc-40e4-53a3-b21b-de80b66bcc5a','1e949237-63e6-5adb-b74b-413eb635d6e2','/api/v1/media/3ad8f7cc-40e4-53a3-b21b-de80b66bcc5a',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('699f584c-f765-528e-b298-425c3055b8f9','3c2ff9d8-cec7-5651-a0ef-713e2ee79eb8','/api/v1/media/699f584c-f765-528e-b298-425c3055b8f9',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('cb98e511-977b-5ca3-ad67-caf3a2a8dfcd','3af1365e-ce72-5e82-918b-514f771e997c','/api/v1/media/cb98e511-977b-5ca3-ad67-caf3a2a8dfcd',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('f2c9c3aa-1cf5-5469-8b0b-d8ee768f442d','a4ab5dcf-4895-5f8e-a231-a3f9fd029049','/api/v1/media/f2c9c3aa-1cf5-5469-8b0b-d8ee768f442d',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('3f602d2b-483c-54ab-8c56-192c9d69f6ce','f78e6f30-bdcd-5f67-a06a-631ae9a459fc','/api/v1/media/3f602d2b-483c-54ab-8c56-192c9d69f6ce',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('46f22db0-bf87-5734-8801-26fb270428a0','3055b9d0-387e-57d7-9446-70d3c08a005a','/api/v1/media/46f22db0-bf87-5734-8801-26fb270428a0',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('d6ef0bca-c172-57e1-9373-d2732976db50','848292e4-ffde-5abb-8b73-59b7f8975320','/api/v1/media/d6ef0bca-c172-57e1-9373-d2732976db50',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('fe6a8381-7d13-55f2-b211-73cec28798ae','acda8dd1-08c3-5b9a-85c5-10339fca3927','/api/v1/media/fe6a8381-7d13-55f2-b211-73cec28798ae',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('27b4e4d1-3b6d-55ff-befe-e44698592c3f','6e5a215e-4fa6-5701-afaf-789f2c8197d5','/api/v1/media/27b4e4d1-3b6d-55ff-befe-e44698592c3f',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('931aa831-962d-514d-89cd-1707e6ccd7fd','6e5a215e-4fa6-5701-afaf-789f2c8197d5','/api/v1/media/931aa831-962d-514d-89cd-1707e6ccd7fd',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('1801985d-6948-52ca-9647-62091eb4b929','f989e7cc-b64e-54b6-bd22-1eadeffd02a0','/api/v1/media/1801985d-6948-52ca-9647-62091eb4b929',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('60aa4589-49b4-5719-979e-6245c1daeb16','57c1c534-f550-50f9-b599-5521b590f18d','/api/v1/media/60aa4589-49b4-5719-979e-6245c1daeb16',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('5e4a3bb0-0b2f-5127-a77d-860460f02557','872d34f7-3d76-5199-8618-cc17eb5370de','/api/v1/media/5e4a3bb0-0b2f-5127-a77d-860460f02557',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('0c4dc48c-8fc4-5a26-8ae4-7f8ef2ea3fcc','82b5557f-7d9b-5276-881a-9ddb8c4a12b8','/api/v1/media/0c4dc48c-8fc4-5a26-8ae4-7f8ef2ea3fcc',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('9d85ebf6-6b89-51cd-b15d-4da35d5ff2ae','e1a4d821-e3d5-5210-a199-b6f4dc6ec78e','/api/v1/media/9d85ebf6-6b89-51cd-b15d-4da35d5ff2ae',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('b01fe15b-1766-5898-baa7-3e276bc091f6','0d5288b0-b521-5b91-a419-8e019adc2b37','/api/v1/media/b01fe15b-1766-5898-baa7-3e276bc091f6',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('b10bffbe-db6c-5c53-a3dd-9fee097e1249','cdf98f90-9fca-56e5-8a0b-393265d055c7','/api/v1/media/b10bffbe-db6c-5c53-a3dd-9fee097e1249',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('5d0bc613-fdf1-5800-8daa-ece5da0e58c8','fc9bb359-6b2a-5b23-9344-65e2ebf89cb4','/api/v1/media/5d0bc613-fdf1-5800-8daa-ece5da0e58c8',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('0c669a3f-e0fb-5209-b1c4-195c1567ff82','05bf06ac-11df-5e8f-852d-6e9fa7fa8072','/api/v1/media/0c669a3f-e0fb-5209-b1c4-195c1567ff82',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('1509d461-2295-5d08-b3e7-cd979d0c151e','2b6e78c5-9df5-5c88-8bc3-fd20a7d31383','/api/v1/media/1509d461-2295-5d08-b3e7-cd979d0c151e',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('ec57ab52-6ada-5530-9b8e-0e352c8c2b46','903d4bfb-d232-5394-b99f-052983a41abc','/api/v1/media/ec57ab52-6ada-5530-9b8e-0e352c8c2b46',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('084c6517-7c4b-5d80-85e1-7c32368700fe','0cf77375-5b1a-51e5-bd34-12024315133d','/api/v1/media/084c6517-7c4b-5d80-85e1-7c32368700fe',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('1e42fb8a-67fa-5efb-a24f-2bb7a92fc3c6','03d1e4fa-aece-5f27-b1b3-87ca6d7d5c39','/api/v1/media/1e42fb8a-67fa-5efb-a24f-2bb7a92fc3c6',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('ec206ee4-3706-5274-84e1-d4e116e1d878','468eae22-d558-5a7e-891a-a21ae01f6c42','/api/v1/media/ec206ee4-3706-5274-84e1-d4e116e1d878',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('6005360f-4e0c-5771-a42d-0df980a0acbb','78eff9cc-c97d-539b-95b6-a7f113f3beed','/api/v1/media/6005360f-4e0c-5771-a42d-0df980a0acbb',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('6695ab94-ad2c-543d-a73e-0ed308ea3072','0d3aae1e-8bf8-5ccc-95f0-391ac99c8e09','/api/v1/media/6695ab94-ad2c-543d-a73e-0ed308ea3072',0)
ON CONFLICT (id) DO NOTHING;
