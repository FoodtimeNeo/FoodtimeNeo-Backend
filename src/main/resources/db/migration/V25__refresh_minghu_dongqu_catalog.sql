-- 明湖按楼层拆厅 + 东区换新目录：只 INSERT 与食堂排序 UPDATE，绝不 DELETE。
-- 老明湖食堂（单厅）行不碰，由后续删除脚本处理；本迁移幂等，可重放。
-- UUID 确定性复现：uuid v5(NAMESPACE_URL, key)，key 形如
--   foodtimeNeo:hall:<厅名> / foodtimeNeo:stall:<厅名>:<档口名> /
--   foodtimeNeo:dish:<厅名>:<档口名>:<菜品名>[#n] / foodtimeNeo:media:<厅名>:<档口名>:<0起序号>。
-- 同档口重名菜品第 2 次起 key 后缀 #2、#3（共 6 组）；
-- 价格取数字部分，无数字的 13 条以 0 入库并在简介标注原文待人工核价；
-- 多规格/称重价取首个数字，见 test/canteen-refresh/extract-report.txt 第四节复核。
-- 档口 cover 取该档口第 0 张图；图集表含全部 49 张图（含首图，首图 URL 与 cover 一致）。
-- 图片字节：seed-images/v25/<media-uuid>.jpg，部署按 docs/canteen-refresh-v25.md 铺入 uploads/。
-- Use schema-qualified names so this migration is independent of search_path.

-- 一、既有食堂排序：留园0 / 明湖1楼1 / 2楼2 / 3楼3 / 学活4 / 学四5 / 东区6 / 学苑7。
UPDATE foodtime.dining_halls SET sort_order=0, updated_at=now() WHERE id='68166b66-14d1-58c5-960d-572228254c20';
UPDATE foodtime.dining_halls SET sort_order=4, updated_at=now() WHERE id='315086d7-f339-5d26-a030-b3d7ffe7cde1';
UPDATE foodtime.dining_halls SET sort_order=5, updated_at=now() WHERE id='94f125bc-e647-5e4e-a946-cde15f54cbdc';
UPDATE foodtime.dining_halls SET sort_order=6, updated_at=now() WHERE id='955c1f92-3671-5d13-8566-81911b5a5a7b';
UPDATE foodtime.dining_halls SET sort_order=7, updated_at=now() WHERE id='78fa7f63-f52f-524f-9b28-f55fb441a1f0';

-- 二、新增 3 个楼层厅：封面复用原明湖封面。
INSERT INTO foodtime.dining_halls(id,name,description,status,cover_image_url,sort_order)
VALUES ('f158d27a-329c-57ec-978a-22ed9e6a447b','明湖餐厅1楼','原明湖食堂按楼层拆分（1楼档口）','active','/api/v1/media/fdfb8516-8db8-5ca7-bb08-6ac9b9fe6b18',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dining_halls(id,name,description,status,cover_image_url,sort_order)
VALUES ('bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','明湖餐厅2楼','原明湖食堂按楼层拆分（2楼档口）','active','/api/v1/media/fdfb8516-8db8-5ca7-bb08-6ac9b9fe6b18',2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dining_halls(id,name,description,status,cover_image_url,sort_order)
VALUES ('d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','明湖餐厅3楼','原明湖食堂按楼层拆分（3楼档口）','active','/api/v1/media/fdfb8516-8db8-5ca7-bb08-6ac9b9fe6b18',3)
ON CONFLICT (id) DO NOTHING;

-- 三、43 个档口：cover 为该档口第 0 张图。
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('a689caf7-27e9-5ead-90ac-421560ea3ef8','955c1f92-3671-5d13-8566-81911b5a5a7b','贾香嫂｜云南鲜烫牛肉米线',NULL,'/api/v1/media/ab80a264-7961-5f25-bbc8-da2db5a3bab6','active',1)
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
VALUES ('3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','955c1f92-3671-5d13-8566-81911b5a5a7b','嗨旋｜牛肉粉面',NULL,'/api/v1/media/199fb876-5bb1-5f33-b372-1cf0c0632fea','active',8)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('6aa161d4-a889-5e09-9d9d-cf67b32e3504','955c1f92-3671-5d13-8566-81911b5a5a7b','嗨族牛肉米粉',NULL,'/api/v1/media/e7d5d97c-2c84-59b9-99a0-53e548561f88','active',9)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('c3f07056-e5a9-50f3-9a0f-7187b34afe7a','955c1f92-3671-5d13-8566-81911b5a5a7b','馋大八｜江西小炒',NULL,'/api/v1/media/70938423-7310-59c1-9345-1ffd6396a831','active',10)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('43643b96-1419-59dc-9dff-6e9605f6bb09','955c1f92-3671-5d13-8566-81911b5a5a7b','重庆小面',NULL,'/api/v1/media/dd04abb8-2512-52e2-bde2-4081a3cb1b96','active',11)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('21622451-9645-5fdf-a5d3-a52e6f8e45d4','955c1f92-3671-5d13-8566-81911b5a5a7b','三哥｜小炒盖饭',NULL,'/api/v1/media/ac8b97fa-9ab9-55f4-ae32-6eafc5fc1fb1','active',12)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('96535f3f-b7c9-5cf8-9bfd-67192c8370f7','955c1f92-3671-5d13-8566-81911b5a5a7b','交大小笼包｜手工水饺·云吞方便面',NULL,'/api/v1/media/35093422-3144-50ce-b12a-04f0a5674976','active',13)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('feaff20c-3540-5e76-9d8c-28b402cae888','f158d27a-329c-57ec-978a-22ed9e6a447b','西北面食',NULL,'/api/v1/media/554332d7-74f2-57f1-9ad3-efd98caa7f47','active',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','f158d27a-329c-57ec-978a-22ed9e6a447b','风味米线',NULL,'/api/v1/media/6b999298-e1de-577c-b11b-db838a4df1eb','active',2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('b2a0daba-5068-5d4e-9359-d68eaba39d42','f158d27a-329c-57ec-978a-22ed9e6a447b','小份菜窗口',NULL,'/api/v1/media/478ad6f7-9470-5694-9208-351db45f8e6c','active',3)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('73858888-0fb5-5265-9411-5cf3c34512e3','f158d27a-329c-57ec-978a-22ed9e6a447b','知味拌饭',NULL,'/api/v1/media/09ad15d8-4bd4-567c-a95e-3967994eefd6','active',4)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('c1e2f896-fc05-5bbf-8115-ea322bab3c6a','f158d27a-329c-57ec-978a-22ed9e6a447b','江西小炒',NULL,'/api/v1/media/8d18bfb5-5f32-5be5-b6f2-d02682c08614','active',5)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','f158d27a-329c-57ec-978a-22ed9e6a447b','家常菜窗口',NULL,'/api/v1/media/730d6537-3e42-538a-9548-a43b41dfc5b7','active',6)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('9088c8ea-5385-541d-abf3-e73cbfb75be6','f158d27a-329c-57ec-978a-22ed9e6a447b','主食窗口',NULL,'/api/v1/media/708f2dce-4c5c-5475-bb25-826f854178bc','active',7)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('b02814c2-7d05-550e-9434-d38a0515fb62','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','拉面刀削面',NULL,'/api/v1/media/de75ba87-e21e-5479-ad71-1ec0ec096e74','active',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','蘸水菜｜蛋浇肥牛饭·牛油拌饭',NULL,'/api/v1/media/f79cb85a-023d-5f57-ba48-029a13dbbbed','active',2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('ced58e12-683c-5563-96a0-d7f86c137384','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','手拍粉｜芝士焗饭',NULL,'/api/v1/media/0ad9f63d-a48a-5cf1-8e31-6c126d2a2bc3','active',3)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0dda9895-1219-5507-b3ab-8b5729d25a45','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','云南菌菇炒饭·烤鱼',NULL,'/api/v1/media/04e615f9-07ba-57ab-b0c7-2b4af1945cc2','active',4)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('6b6ea528-3994-5ceb-979f-f612a4647206','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','烫三爷｜云南鲜烫吊龙米线',NULL,'/api/v1/media/ff91d488-ba84-546b-a282-ad72df4185eb','active',5)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','烤鸭·烧腊·米粉',NULL,'/api/v1/media/c8643c9f-4989-5427-a059-96dbf5061416','active',6)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','分米鸡·土豆泥拌饭',NULL,'/api/v1/media/ed47e608-296c-5560-a393-464db1c730a0','active',7)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('10f3a596-9e0d-519d-a02e-402c93f8bc59','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','炙知府｜炙烤五花肉',NULL,'/api/v1/media/0d1eaea3-90c6-5657-9698-dc116a413990','active',8)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('60a470c3-d39e-5029-9300-66f5f4d8e10e','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','明湖广式煲仔饭',NULL,'/api/v1/media/76cd885f-a767-5e4b-9643-27f1810240bb','active',9)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('6ade0f06-e790-547e-ac49-71f2a5e670d2','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','烧腊·肠粉·砂锅饭',NULL,'/api/v1/media/333a5e93-4732-5933-8f97-21cbbeb0afbc','active',10)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('b47b0628-073e-5483-9670-a9312f0e01a7','bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f','川渝麻辣香锅·小锅炒鸡',NULL,'/api/v1/media/befad71a-6c79-50de-8976-0970835e3f75','active',11)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('217f40fb-0a92-510f-b9d8-139999735cf2','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','牛肉粉丝汤·老母鸡粉丝汤',NULL,'/api/v1/media/4e20a987-9af9-578c-a911-698cf40d5c20','active',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0bafdf33-8eae-546d-ac3c-98239dd858c9','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','旋转小火锅',NULL,'/api/v1/media/596cd1b0-cf72-52aa-954f-675449eaa929','active',2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('b70bbd25-46c1-51b4-8a2d-8cbd6638f9b1','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','12号｜东北麻辣烫',NULL,'/api/v1/media/b21f7386-f221-5d92-a049-afd2aff73c2c','active',3)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','12号｜友客来·藤椒鸡煲·黄焖鸡',NULL,'/api/v1/media/febbb4af-ef10-5e79-974f-25b5bb5b118a','active',4)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','9号｜手工鲜水饺',NULL,'/api/v1/media/a06517c1-c488-595a-8871-319bec199c08','active',5)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('97722745-ce68-5b3b-8490-ef7a08cf2441','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','犇犇牛肉饭',NULL,'/api/v1/media/64887ecc-0b21-57e5-a8e6-3b79233193d8','active',6)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('cdc93861-b55a-5831-8c96-a180654c50e3','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','滑蛋饭',NULL,'/api/v1/media/55247970-fd64-504f-a272-bb0c014f50b4','active',7)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('b55c32db-c2bc-54bc-9565-b80e029beeb3','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','6号｜匠心卤·热卤拌饭',NULL,'/api/v1/media/7eacd4ce-d174-5450-bc76-19a69d0ce8b1','active',8)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('7cc04078-48a6-53fe-a42f-07a1912937cf','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','5号｜重庆面庄',NULL,'/api/v1/media/2af9c86f-88ee-5e5e-8fe5-a0985c610c0c','active',9)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('0dd40e8e-cef6-5f60-9075-aafd3a68243a','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','4号｜小锅焖面·小锅焖饭',NULL,'/api/v1/media/ebe97b8f-5309-5760-b19c-e60f97d1ff67','active',10)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('d9a2ae41-2799-540b-a50f-98088f32e108','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','3号｜广式烧腊饭',NULL,'/api/v1/media/1c81d3b7-358e-52d1-9f27-5ef48f08044a','active',11)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order)
VALUES ('4252061b-6c95-58e4-b6ed-2c76582d2e7a','d4fb48b3-a92d-5e93-a4f3-4ab3f210118d','暖柠健康餐',NULL,'/api/v1/media/86b91bd2-c31e-5eed-b4c9-034e8e44c95f','active',12)
ON CONFLICT (id) DO NOTHING;

-- 四、全部菜品：无数字价格以 0 入库，原文记入简介待核价。
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f463f709-b76e-5f1f-8d03-24d39cc06bf7','a689caf7-27e9-5ead-90ac-421560ea3ef8','牛骨汤米线/面',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3d946a56-3a4f-50f2-ba85-a00e10331973','a689caf7-27e9-5ead-90ac-421560ea3ef8','招牌肉酱米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d286739-c78c-5f73-a221-144f97b18bb0','a689caf7-27e9-5ead-90ac-421560ea3ef8','日光番茄米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('99c931a6-34d2-566e-9726-e454f18ea6d2','a689caf7-27e9-5ead-90ac-421560ea3ef8','山野泡椒米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f3b6ae46-2947-5111-92d0-434073b2753c','a689caf7-27e9-5ead-90ac-421560ea3ef8','香辣骨汤米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('42cd8f21-19bf-5cad-9d38-93b17b8bfbe4','a689caf7-27e9-5ead-90ac-421560ea3ef8','劲爽麻辣米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e1cfb55-3207-5e21-ad25-17040eb99a10','a689caf7-27e9-5ead-90ac-421560ea3ef8','醇香麻酱米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('461611f3-07d3-5468-87e0-887fafac54cd','a689caf7-27e9-5ead-90ac-421560ea3ef8','金汤酸辣米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ee5d1bb6-7edb-53da-a77f-e8efc9301b22','a689caf7-27e9-5ead-90ac-421560ea3ef8','酱卤牛肉米线/面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('051e3d62-f21a-5857-bf85-4719a1dfb140','a689caf7-27e9-5ead-90ac-421560ea3ef8','招牌肉酱干拌米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1272c41c-6009-5361-bec8-df015f9c0cfe','a689caf7-27e9-5ead-90ac-421560ea3ef8','云贵烧椒干拌米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('62bb1cf0-ef9b-5f38-924c-cf821e54f227','a689caf7-27e9-5ead-90ac-421560ea3ef8','剁椒番茄干拌米线/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f03e767d-93d3-5500-8916-434d59ea733e','a689caf7-27e9-5ead-90ac-421560ea3ef8','把子肉',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ebd14cdb-80fc-5969-a546-601aa297849b','a689caf7-27e9-5ead-90ac-421560ea3ef8','鸡腿',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9f30fa3-e095-5a23-835e-d7fa1a247316','a689caf7-27e9-5ead-90ac-421560ea3ef8','吊龙',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('486aae14-049f-5e8f-9181-ffca8a401d7e','a689caf7-27e9-5ead-90ac-421560ea3ef8','黑椒肠',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0da369c5-1793-5bc3-801a-b9a70914d07c','a689caf7-27e9-5ead-90ac-421560ea3ef8','肉丸子',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1f01c117-a9c8-51a9-847b-50faa421a340','a689caf7-27e9-5ead-90ac-421560ea3ef8','鸡柳',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a51f333b-2f77-56cb-b984-89092198c6b4','a689caf7-27e9-5ead-90ac-421560ea3ef8','豌豆尖',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e39f3f84-fe50-5fb3-912e-d58feb447298','a689caf7-27e9-5ead-90ac-421560ea3ef8','响铃卷',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6db29928-74a1-5bac-bc3c-95224c4215d9','a689caf7-27e9-5ead-90ac-421560ea3ef8','炸蛋',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c33a077-9385-5571-acc4-e3fa76603c0e','a689caf7-27e9-5ead-90ac-421560ea3ef8','煎蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('29a292f2-297f-5fe3-a949-1aed899f11c9','a689caf7-27e9-5ead-90ac-421560ea3ef8','卤蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f7f3bae0-c6c6-57df-9c4b-993fdaeb3477','a689caf7-27e9-5ead-90ac-421560ea3ef8','烤肠',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('464186be-e7ef-5c28-9e51-a8f68645b435','a689caf7-27e9-5ead-90ac-421560ea3ef8','豆干',NULL,2,'active')
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
VALUES ('b1db7516-5305-51c5-a55f-b5a84bd2712d','5b1b7f38-9fb7-5d57-aa4f-8753b527b06f','米饭/汤','原价标注：免费（菜单标注），待人工核价',0,'active')
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
VALUES ('bf6c9705-3469-5dae-87d5-20432cc93a0d','8f31fd77-010e-5c63-8607-f99db544939d','黄椒炒肉',NULL,18,'active')
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
VALUES ('517ad605-f8fc-51f8-9cc9-3e042abc2d84','8f31fd77-010e-5c63-8607-f99db544939d','临沂炒鸡※',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a417873c-052c-5e39-92cc-65b7ad9b13ad','8f31fd77-010e-5c63-8607-f99db544939d','黄焖鸡块※',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8679ae68-02de-562d-b0ce-3aa7a627e789','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','招牌黄牛肉面/粉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('14a4ae9d-935a-5baa-9420-f3d4eeb487f7','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','酸菜肉炒肉汤面/粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ecabba20-d698-5d5b-a60e-867f78609ea6','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','白切牛肉汤面/粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b50bc63e-5610-524b-a009-f7cd7dd77a00','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','湘味辣子鸡汤面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('277940b3-174c-5430-8998-bf15f6fd4e6a','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','番茄鸡蛋汤面/粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b87c7ae7-587f-59bc-ba2c-00a41e9fc58b','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','农家小炒肉汤面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eba23384-4560-55ec-80e4-29c4b540b721','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','柳州螺蛳粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ff975b95-9f30-5d1d-b768-846f8d4b06d6','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','黑椒鸡腿排汤面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f796f481-2c0d-5e3b-afe7-507b741da14e','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','招牌肉酱拌面/粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('81ffd780-36ec-55ea-86b7-cd836f44a085','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','蟹黄肉酱拌面/粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d569f76-4d6a-5cb0-bec9-25602d0d58c7','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','烤肉肉酱拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4c2475d0-4d89-58cd-8749-443f4fbcf8b0','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','农家小炒肉拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('55cb3ec8-3030-584c-8621-18355aa007a4','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','湘味辣子鸡拌面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ab5f9687-16b4-5560-9deb-bcf6e5161314','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','葱油拌面/粉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d659b0b0-ab57-5013-9f1c-4e9ae3296b02','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','南昌拌粉',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('49b06131-9f39-5049-88d5-d52e87614d2a','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','武汉热干面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26d5b9f9-de4d-5c43-9c5b-4ce939a95521','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','椒麻鸡丝拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8aa0630d-933b-5250-82c6-f49ae5cc1258','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','巴西烤肉拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('13a4d8f7-17df-5599-80bc-7eb4bc76200b','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','黑椒鸡腿排拌面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e98b335-625e-5b7e-9ece-2be63ad46d8e','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','陕西油泼面',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e183388-12d4-5f00-af29-a28566768690','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','招牌肉酱油泼面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cc10f878-6113-5eb4-a611-3f1c7fb4cb4e','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','番茄鸡蛋油泼面',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a23c9ce4-78b4-5e28-aabc-4f15d6ebda3b','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','巴西烤肉油泼面',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e9c4fde-10c4-509b-923a-048af96569e5','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','椒麻鸡丝油泼面',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4e89d811-26a7-5149-9a74-ed9e55740f1b','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','辣子鸡油泼面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ee9f2762-b59a-5ca9-9d7d-4914aef1dd37','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','剁椒肉酱油泼面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dde016f1-4687-5d05-aa90-e4c7ef78adc7','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','招牌黄牛肉油泼面',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6467c09f-43e3-5023-a951-e66d25a330fb','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','煎蛋',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f8cad15-29b6-58bb-b30e-aafddd3c246d','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','兰花串',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9bc091d-e820-5ba9-8d39-4d6a941d6e54','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','淀粉肠',NULL,3.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('14d7b7a5-6495-5b93-a6fe-850b10b25ac5','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','纯肉肠',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('12ea851b-b7e2-59d9-b2bd-e8d946ee9c52','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','鸡腿',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d4f07db8-f526-5378-a614-575f9942f92b','6aa161d4-a889-5e09-9d9d-cf67b32e3504','贵州羊肉粉',NULL,19,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e5d91eb-01ee-5023-b08c-5aec47672026','6aa161d4-a889-5e09-9d9d-cf67b32e3504','番茄鸡蛋汤面/粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4487d5c3-370f-5473-aa8f-dbcb3e51a547','6aa161d4-a889-5e09-9d9d-cf67b32e3504','农家小炒肉汤面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c2d1cd59-0b05-5f61-9034-f6bbce805b62','6aa161d4-a889-5e09-9d9d-cf67b32e3504','白切牛肉汤面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('58ea122a-e6ac-5065-8faf-2b4095c4c191','6aa161d4-a889-5e09-9d9d-cf67b32e3504','柳州螺狮粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4db5b3cc-3d7e-59d0-8534-39fdd83d7269','6aa161d4-a889-5e09-9d9d-cf67b32e3504','香菇肉炒汤面/粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9376c2d9-5c21-5514-8dbd-0194ed27c68f','6aa161d4-a889-5e09-9d9d-cf67b32e3504','黑椒鸡腿排汤面/粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f457b8a0-af15-5fc1-838a-2afdf3e47a4e','6aa161d4-a889-5e09-9d9d-cf67b32e3504','招牌肉酱伴面/粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('14a1f9bf-d299-5cfd-a8fe-7295e1b1b09b','6aa161d4-a889-5e09-9d9d-cf67b32e3504','蟹黄肉酱拌面/粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1cb41594-111c-5a84-a633-7387eef7c592','6aa161d4-a889-5e09-9d9d-cf67b32e3504','椒麻鸡拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2a692de8-d0d8-5040-8f24-751a0308e299','6aa161d4-a889-5e09-9d9d-cf67b32e3504','巴西烤肉拌面/粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0e777ad0-dc36-5d81-af68-e728b8eb7b56','6aa161d4-a889-5e09-9d9d-cf67b32e3504','黑椒鸡腿拌面/粉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f98b3728-e81e-5430-aebc-06d95159d12e','6aa161d4-a889-5e09-9d9d-cf67b32e3504','番茄鸡蛋油泼面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0c32ae2f-6c68-5fa6-8654-022bab8f251c','6aa161d4-a889-5e09-9d9d-cf67b32e3504','巴西烤肉油泼面',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('28c186e1-32fe-52b1-ad56-26e936df4de6','6aa161d4-a889-5e09-9d9d-cf67b32e3504','椒麻鸡丝油泼面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8dfce9bb-37b1-5af9-8a6c-1166176ec000','6aa161d4-a889-5e09-9d9d-cf67b32e3504','辣子鸡油泼面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('09c24dcd-2868-585d-b361-39fa7b233812','6aa161d4-a889-5e09-9d9d-cf67b32e3504','蟹黄肉酱油泼面',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a7606eb4-4385-54a2-a5ad-3ae3f00b8ba4','6aa161d4-a889-5e09-9d9d-cf67b32e3504','招牌黄牛肉油泼面',NULL,17,'active')
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
VALUES ('3a40ecbb-7c1a-521d-a1eb-74a214584f36','43643b96-1419-59dc-9dff-6e9605f6bb09','牛肉面（汤拌）','原价标注：价格遮挡，待人工核价',0,'active')
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
VALUES ('9f30e7df-afc5-5f5a-9ff0-c9d66d118778','feaff20c-3540-5e76-9d8c-28b402cae888','扯面',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('54b25c69-ac31-5f1b-a9d3-ccce6d7d6db6','feaff20c-3540-5e76-9d8c-28b402cae888','拉条子',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9717c268-d7b9-594a-b7bb-cebefc4c8a91','feaff20c-3540-5e76-9d8c-28b402cae888','烩面片',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1faf3abd-9689-5eb8-8ffc-826cff50327d','feaff20c-3540-5e76-9d8c-28b402cae888','油泼面',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c6fb0cfd-9112-5e6a-8761-beaf26b539c2','feaff20c-3540-5e76-9d8c-28b402cae888','裤带面',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f153de73-62db-5030-8149-6148ba7cee78','feaff20c-3540-5e76-9d8c-28b402cae888','热干面',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1890a774-5b54-56e4-bb4c-64eebdf07c56','feaff20c-3540-5e76-9d8c-28b402cae888','香菇炖鸡面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7c015ea5-544b-5918-9d2e-c6ffe2577da3','feaff20c-3540-5e76-9d8c-28b402cae888','担担面',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e941f79-21cd-55ac-87fc-46f652025895','feaff20c-3540-5e76-9d8c-28b402cae888','小烧肉面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9ec96511-1f7d-53a6-8a17-95dcbf5e2ec1','feaff20c-3540-5e76-9d8c-28b402cae888','三合一拌面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5ae65de-6a54-5773-99eb-b75eb9ed1936','feaff20c-3540-5e76-9d8c-28b402cae888','麻辣肉沫拌面',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8c8bd675-ca72-508b-a97e-e17b09c9400c','feaff20c-3540-5e76-9d8c-28b402cae888','臊子干拌刀削面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0b2abdc2-904f-5796-a9d7-f7dc159ace61','feaff20c-3540-5e76-9d8c-28b402cae888','双椒鸡腿肉拌面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7c067836-7741-5017-ac44-173d21866e9e','feaff20c-3540-5e76-9d8c-28b402cae888','尖椒木耳鸡蛋拌面',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ad2c0781-eb46-5543-a4cf-3cef5dd56e2d','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','面片',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('410036ea-4ff5-5eb6-9554-f9d818f0d3e2','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','米线',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0096245b-8d94-5862-995a-73e090eeab02','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','土豆粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('992e2d46-6399-5ee2-8e4a-5cde6d69e2df','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','酸菜米线、土豆粉',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('14a1c38a-e6db-5cbd-a785-1852aba700a6','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','鸡块米线、土豆粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d01cf055-4126-5eef-b7ba-16c0f58084c2','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','牛肉米线、土豆粉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5d46ee5a-52ed-5aa7-8619-01803c11f3c6','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','牛肉丸米线、土豆粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cb72ab41-c168-5966-b0ac-dd369dc6a9e9','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','小酥肉米线、土豆粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f5155a2-7e37-5ac4-9158-40db6a88a720','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','香菜丸子米线、土豆粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('83246bc7-d80a-5124-9e58-c594a7404460','b2a0daba-5068-5d4e-9359-d68eaba39d42','清炒快菜',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a0a95054-aeff-5bac-b410-7b8b7f65cd80','b2a0daba-5068-5d4e-9359-d68eaba39d42','清炒小白菜',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c13beb36-ffb6-5a80-aeab-e8039d2c332f','b2a0daba-5068-5d4e-9359-d68eaba39d42','芹菜炒鸡蛋',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('afeee76f-4849-5bd2-ac0e-cd634d83f6ae','b2a0daba-5068-5d4e-9359-d68eaba39d42','木须西葫芦',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f2c38e8-3e34-5f68-9521-26679281be01','b2a0daba-5068-5d4e-9359-d68eaba39d42','魔芋丝炖鸭块',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('947b8a81-1a2f-5620-8338-cf4aa7de4a41','b2a0daba-5068-5d4e-9359-d68eaba39d42','葱头炒鸡蛋',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a3599cfd-c6a9-5feb-b861-af13339c4081','b2a0daba-5068-5d4e-9359-d68eaba39d42','红烧豆腐',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('25f7a8ee-5d61-5359-ac2c-d60147617ec1','b2a0daba-5068-5d4e-9359-d68eaba39d42','糖醋里脊',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('49590045-3132-5121-bc3e-44dcca6261a6','b2a0daba-5068-5d4e-9359-d68eaba39d42','锅包肉',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f167965d-0497-52d0-9451-367ef34d17fa','b2a0daba-5068-5d4e-9359-d68eaba39d42','盐酥鸡',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('02c2627d-e82a-53b6-b4df-749a928d84e9','b2a0daba-5068-5d4e-9359-d68eaba39d42','孜然鸭块',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fecb1201-84c0-51c3-be30-a0da4457d683','b2a0daba-5068-5d4e-9359-d68eaba39d42','白菜丸子',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b4c9678d-6d84-587a-b5a4-540ab1bcc0b8','73858888-0fb5-5265-9411-5cf3c34512e3','咖喱鸡腿肉拌饭',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('133a11db-ed09-5b69-9700-9e4df3193d63','73858888-0fb5-5265-9411-5cf3c34512e3','尖椒肉丝鸡蛋拌饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b7904066-ff8a-52e2-946d-2b8d5d3f190f','73858888-0fb5-5265-9411-5cf3c34512e3','孜然肉丁拌饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b5c7faf-2652-5b4a-b936-000d45a42ebc','73858888-0fb5-5265-9411-5cf3c34512e3','香酥鸡排拌饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('21332e83-23c1-529f-8fe6-ff76ca78ee23','73858888-0fb5-5265-9411-5cf3c34512e3','香菇小炖肉拌饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c93a48db-b80e-5dd5-bbda-347deb1b623a','73858888-0fb5-5265-9411-5cf3c34512e3','爆炒羊杂拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bcc136e7-a240-58fb-828c-70b076d000e8','73858888-0fb5-5265-9411-5cf3c34512e3','杠子肉拌饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4b978900-a99d-5d44-b0ad-fb9c55c71300','73858888-0fb5-5265-9411-5cf3c34512e3','过油牛肉拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('af92ee5d-4063-53b3-b529-b41557cdf76c','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','辣椒炒肉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8753a19e-6e2c-574e-8dfa-aedce8562f07','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','外婆菜炒鸡蛋',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('931ed059-b752-5adf-8b3a-c5a578407231','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','江西小炒鸡',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b777225-f58b-54a7-a6e7-da25a4747a3e','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','赣南小炒鱼',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3bd5663f-d3a9-5bd2-a603-8fd1993c4adf','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','江西辣鸡爪',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8ef39317-7acc-50a1-a472-846a29dc6059','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','辣香翅根',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('71ebe1c3-6a16-5974-9b27-604a0c5b29ec','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','酸辣鸡杂',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a4f98aa0-2ae3-5c95-a1e7-ed51c967e5b5','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','鲜毛豆炒牛肉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a4344b6b-bb07-5135-8e9e-e7afa3c13563','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','清炒圆白菜',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a0f750ab-fadc-53b3-8348-e2c760b37c78','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','清炒快菜',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f87faaa7-b1e9-5f47-8fad-5ec0c6d269ba','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','香辣鸡翅根',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('016eff5a-0a2b-56ec-9f72-aa319bfa1c29','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','红烧狮子头',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('74e3fa64-a3a2-5543-9916-ea38a366cb31','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','剁椒娃娃菜',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3bab3cb7-82ad-5c3e-9129-ff97f6629e8d','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','木耳冬瓜',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5ac03f0d-b5c1-58c9-8886-ff7803911e3e','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','豆皮炒肉',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('43843825-e98d-5659-a66a-936c5011a523','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','豇豆肉丝',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9aec9003-893e-5aec-85a0-78aebeade453','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','剁椒粉丝龙利鱼',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6447afaf-97d0-531d-995d-58148cc1a532','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','干炸鸡米花',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('75f6e79e-b2c9-51a2-bee4-9dbcf5e143b2','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','炒豆芽',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e801f54e-5eb2-5ef2-a22e-f523aff08841','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','清炒小白菜',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9bb4147-cace-5ef4-b24a-5b650b4aceaa','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','红烧豆腐',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b596a6be-8b99-5641-8bcd-49d667bda109','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','芹菜腐竹',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45328076-9e86-5024-954d-ba6c63694e1d','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','红烧琵琶腿',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('66e204c8-56fa-5e2e-b695-5df1771b03f3','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','豆腐丝炒肉',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ef24249b-fbe1-5909-b991-afd0f0d22f53','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','木耳圆白菜',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('39d6acf0-3e97-5a04-a3bb-977932e05b1a','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','蒜薹炒肉',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('359f320a-e7ec-5556-9edb-eb96ff7292a9','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','魔芋丝烧鸭',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cb235fdb-c203-58f6-9d1b-bfa313b2058f','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','香辣片片肠',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ebaad00a-04bc-5082-bf1f-d50abfaf35c5','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','锅包肉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('97d3e358-9f7c-5a17-b849-dbb4670546e8','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','番茄龙利鱼',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f2144efe-c8ca-5ae0-be12-59a50b73cb79','9088c8ea-5385-541d-abf3-e73cbfb75be6','馒头',NULL,0.4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6340f0f9-861d-5b8f-b51d-891c563fc931','9088c8ea-5385-541d-abf3-e73cbfb75be6','白花卷',NULL,0.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d971d89d-9894-5dee-bc36-b23524dbff06','9088c8ea-5385-541d-abf3-e73cbfb75be6','荠菜菜团子',NULL,2.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7ab53f8b-3a54-5539-80f7-6ec6321c7c35','9088c8ea-5385-541d-abf3-e73cbfb75be6','酸奶包',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e8703b1-5022-5f86-992d-2fc68401e218','9088c8ea-5385-541d-abf3-e73cbfb75be6','酱肉包',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9d222f2-0fdb-5cb3-8834-286a3178aa4b','9088c8ea-5385-541d-abf3-e73cbfb75be6','蜜豆窝头',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b71fe9a9-d09d-50b5-ab34-301151db3259','9088c8ea-5385-541d-abf3-e73cbfb75be6','红糖发糕',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('595f8c0c-87e2-5835-bb9b-a46564b1eef9','9088c8ea-5385-541d-abf3-e73cbfb75be6','玉米面发糕',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ac2963e4-ac5c-55fe-ba9e-802be3543b0c','9088c8ea-5385-541d-abf3-e73cbfb75be6','椒盐烧饼',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('76e792bf-da61-5238-a632-9d6f44652071','9088c8ea-5385-541d-abf3-e73cbfb75be6','豆沙烧饼',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c0e47850-e8ef-5716-a88a-1f4d59c9dd76','9088c8ea-5385-541d-abf3-e73cbfb75be6','麻酱烧饼',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73bf3291-310d-5865-b0e4-b96991df82e4','9088c8ea-5385-541d-abf3-e73cbfb75be6','牛舌饼',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7388bb64-c52a-5d09-a93e-9efaaa11a3dc','9088c8ea-5385-541d-abf3-e73cbfb75be6','玉米',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1efc8d7b-3e1f-5a64-bb49-b334e6bcfadc','9088c8ea-5385-541d-abf3-e73cbfb75be6','发面饼',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f5886bf0-3c45-5339-968c-6e0ead508623','9088c8ea-5385-541d-abf3-e73cbfb75be6','大饼',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b574d9ad-d6c8-5f35-8320-9070900d1659','b02814c2-7d05-550e-9434-d38a0515fb62','酸菜肉片小面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8f3ac685-dfae-5fbe-a867-adb9722866d8','b02814c2-7d05-550e-9434-d38a0515fb62','花生肉片小面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5459ee0f-5e57-5362-a0b3-fc48ec75b836','b02814c2-7d05-550e-9434-d38a0515fb62','麻辣鸡块小面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('31516b96-05ca-5e8e-b6b4-8f5ec0f28897','b02814c2-7d05-550e-9434-d38a0515fb62','麻辣肥牛小面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('949d2fa8-e4a3-5bcc-accb-dc37bdcd5bdc','b02814c2-7d05-550e-9434-d38a0515fb62','芽菜肉片小面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe897612-8ded-5daa-95ed-fea467fdf379','b02814c2-7d05-550e-9434-d38a0515fb62','红烧牛肉小面',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1cef629e-8fa0-5488-aa17-a0e8eda30f74','b02814c2-7d05-550e-9434-d38a0515fb62','兰州拉面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('467163dd-4950-5a85-b939-da732543f197','b02814c2-7d05-550e-9434-d38a0515fb62','加肉拉面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('53d68e56-1236-5112-a4a7-f0dc9b6c42f6','b02814c2-7d05-550e-9434-d38a0515fb62','传统猪肉刀削面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('060d8457-61ee-5dc1-851d-61c697fa3606','b02814c2-7d05-550e-9434-d38a0515fb62','肉臊子汤刀削面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fa004fb8-26f8-57c5-a660-883a806bcb7b','b02814c2-7d05-550e-9434-d38a0515fb62','红烧牛肉刀削面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6ac1590c-e53c-5cba-b59c-11d9b8791f08','b02814c2-7d05-550e-9434-d38a0515fb62','酸菜牛肉刀削面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2d23be24-7004-550f-aea4-4e9c88967cb1','b02814c2-7d05-550e-9434-d38a0515fb62','西红柿鸡蛋刀削面',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f5d46aed-1619-5dce-a8e5-32a41d48a14f','b02814c2-7d05-550e-9434-d38a0515fb62','浇波面※',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('18c7eee5-58c0-56e9-b8e9-e3190880415b','b02814c2-7d05-550e-9434-d38a0515fb62','炸酱面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('aab3bf70-4964-50f2-92ed-2cddcae20e04','b02814c2-7d05-550e-9434-d38a0515fb62','肉臊子干拌面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ae9279eb-9490-5e9f-9f8e-337822727521','b02814c2-7d05-550e-9434-d38a0515fb62','大盘鸡拌面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ee4545ee-6ed9-5294-b0a5-eb23b3a0997f','b02814c2-7d05-550e-9434-d38a0515fb62','二合一拌面（西红柿+肉臊子）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('62556d0f-0eea-52b1-b598-fa1e4baae99b','b02814c2-7d05-550e-9434-d38a0515fb62','三合一拌面（西红柿+肉臊子+炸酱）',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e7d7d82-dc93-5f11-a0cf-e965ea0ac242','b02814c2-7d05-550e-9434-d38a0515fb62','鸡蛋炒面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9f9091a4-2041-5740-a0b6-1133eaa1dd94','b02814c2-7d05-550e-9434-d38a0515fb62','肉炒面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('208cae63-fc3d-5dd5-92d9-9aafeb549a46','b02814c2-7d05-550e-9434-d38a0515fb62','牛肉炒面',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a7a4a6f6-3c2d-5b21-8596-f1f760a9e0d2','b02814c2-7d05-550e-9434-d38a0515fb62','肉臊子（加菜）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('087b62cf-13d6-5844-b7d5-f4d1de6e0959','b02814c2-7d05-550e-9434-d38a0515fb62','牛肉片（加菜）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3975c430-64c0-54d8-aec5-3a0b12a00959','b02814c2-7d05-550e-9434-d38a0515fb62','牛肉块（加菜）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('80a7a0ea-91b6-5e0d-91d3-3702d862e17c','b02814c2-7d05-550e-9434-d38a0515fb62','卤鸡腿（加菜）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('50cba6e1-2f03-5a29-830d-45988c5f5cf5','b02814c2-7d05-550e-9434-d38a0515fb62','青菜（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e4e99e02-a686-5766-996e-ca7acf5901df','b02814c2-7d05-550e-9434-d38a0515fb62','卤鸡蛋（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8258dfc2-5e25-57fe-8e79-8cab295459ba','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','全蔬减脂（骨汤）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('beda14da-ccce-5e87-9fc9-a2830f19dc2a','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','减脂鸡胸肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('205834b2-e9d1-5bad-b696-d5a7110ec0b1','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','手撕鸡腿肉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d6d4a893-c563-577d-ba99-046aafcacb5e','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','牛肉丸拼虾滑',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3883eead-ebab-5619-b0f1-54e75da42c13','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','黑鱼片拼鸡胸肉',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2bc2987d-c78a-5139-b677-71047c031604','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','梅花肉拼鸡胸肉',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7608da62-bc4e-5736-91a6-35badba2f9eb','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','三文治午餐肉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5566bb62-d72d-5641-917d-7e4721b6704e','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','午餐肉拼肥牛',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('091d61a9-4f1f-57db-8a3b-3ea6669b703f','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','手切牛肉拼鸡胸肉',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('811cff83-f45c-5a15-9e7b-3f6d5c3aeb12','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','嫩滑黑鱼片',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c1859dd5-169f-5501-89ab-7d1b7f4cf09b','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','鲜嫩梅花肉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2148b9b0-c9e0-51f3-8842-06e6bba64602','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','鲜牛肉拼大虾',NULL,19.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d81f56d1-d418-550a-b8be-9b74fc298d69','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','土豆粉（加量）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c08e3eb8-3f87-58a8-a7be-a1574fbc815a','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','午餐肉（加量）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cc0637da-1538-5fd7-94f2-da766cdf7f84','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','梅花肉（加量）',NULL,7,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('71e32c04-14b5-5e09-bcfc-447e47817518','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','黑鱼片（加量）',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8bcfaa27-57f3-5778-a112-097183c453c5','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','大虾（加量）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a07deaff-a444-5f45-b8af-f3d0196e4e9c','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','鸡胸肉（加量）',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4013b28f-d893-53ed-8c74-e4137d7c3279','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','手切牛肉（加量）',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f248ef7-0f23-5edf-8cd8-5aaf9ffb3bdf','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','宽粉（加量）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('31c5d465-3ff0-51c2-b1c3-668fd6509993','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','加盐水鸡蛋（加量）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f3984222-2f04-5060-9ed2-bfe9a450143f','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','麻辣川湘豆腐拌饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f9497271-b113-5de0-9956-3a109bc94cc4','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','板烧鸡腿拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5df90c3b-27a6-5d64-9f93-8103c734d3da','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','香酥鸡排拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d8051ca-cabe-56c8-9167-22cfb5c12442','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','炭烤黑椒五花肉拌饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7b07158c-820f-5c96-bea2-8c102a20da28','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','安格斯肥牛拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('98c7d6a8-44df-526c-a27b-3d6a392cbe12','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','任意双拼拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('387ac19d-b950-590d-bad8-68d8942ec575','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','超级三拼拌饭（五花肉+肥牛+鸡腿）',NULL,19,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('86492d85-3794-5aaa-8043-e8c71c73dbd1','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','爆辣牛油拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8b058e94-15a7-5456-acb2-6d9b5e1281da','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','爆辣鸡腿肉拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('587c67f9-562c-5014-b40b-c206c5510bf2','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','蔬菜鸡腿肉拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a2bf292c-167c-5211-9a06-66f9e810c6ba','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','蔬菜牛油拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('236b02ec-c308-5ad6-90e9-13dd7bf9a9a6','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','蛋黄鸡腿肉拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eac23c29-f606-5b32-8eca-f4e5d19be9c1','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','菠萝鸡腿肉拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('da18daab-9145-58b9-90ad-2bd82a76c7a3','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','菠萝牛油拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c1cfd7b6-c7e7-501d-b4f2-920d22b8d800','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','杂粮饭',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('06465716-efbc-5577-bbe7-77aaf97d911b','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','白米饭','原价标注：免费，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b42b2145-79f0-583e-b277-12e81025b6a4','ced58e12-683c-5563-96a0-d7f86c137384','青菜肉酱手拍粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('73fe1bb8-9dec-5b2e-9cee-dde4f6d5226a','ced58e12-683c-5563-96a0-d7f86c137384','菌菇肉酱手拍粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fdfeb716-7744-5625-8108-7e6edabefb70','ced58e12-683c-5563-96a0-d7f86c137384','鱼豆腐手拍粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7454f9b3-d33e-5478-a541-f2f71adc2f05','ced58e12-683c-5563-96a0-d7f86c137384','培根手拍粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f827df6-66f8-50a6-995b-dca4fb41533e','ced58e12-683c-5563-96a0-d7f86c137384','蟹棒肉手拍粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b3bb0e99-a3b5-50b1-8b5a-d7cc1a7e2849','ced58e12-683c-5563-96a0-d7f86c137384','红烧肉手拍粉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0ad1f686-3e47-5f00-90e0-6cab3838280a','ced58e12-683c-5563-96a0-d7f86c137384','肥牛手拍粉',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c96202cb-a52b-502e-8392-0eee3a7a05ab','ced58e12-683c-5563-96a0-d7f86c137384','撒尿牛丸手拍粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1e574897-6ae3-54ea-b959-77e57c291dc3','ced58e12-683c-5563-96a0-d7f86c137384','牛腩块手拍粉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('15acf7cd-2800-5feb-9aff-324e22b7aef3','ced58e12-683c-5563-96a0-d7f86c137384','火腿芝士焗饭',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('68468e0c-92e0-5d85-8021-06e4a68029a5','ced58e12-683c-5563-96a0-d7f86c137384','番茄培根芝士焗饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f1e71601-ac1a-5009-b258-6783989eb609','ced58e12-683c-5563-96a0-d7f86c137384','照烧鸡肉芝士焗饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e672dc8a-a77f-5949-be2a-34803dc58c53','ced58e12-683c-5563-96a0-d7f86c137384','奥尔良鸡肉焗饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('02b58f93-3284-564f-85c2-d0ddc693c415','ced58e12-683c-5563-96a0-d7f86c137384','奥尔良菠萝鸡肉芝士焗饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ab54c32b-e318-5233-b4e1-36c3ace6114c','ced58e12-683c-5563-96a0-d7f86c137384','奥尔良咖喱鸡肉芝士双拼饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a5f4b18-e9c4-5338-8c2c-34ff2986057c','ced58e12-683c-5563-96a0-d7f86c137384','咖喱牛肉芝士焗饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4ab15ea0-afba-5334-bc23-a75cfe7b20d3','ced58e12-683c-5563-96a0-d7f86c137384','番茄菠萝牛肉粒芝士焗饭',NULL,19,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dba5ee41-b4a1-5afa-a106-9a72ce149c83','ced58e12-683c-5563-96a0-d7f86c137384','豆皮（加料）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4cc42d08-cb1d-554b-aed8-95558beeea62','ced58e12-683c-5563-96a0-d7f86c137384','金针菇（加料）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e90f9f18-c8b2-54b7-943f-13b391e46b7f','ced58e12-683c-5563-96a0-d7f86c137384','青菜（加料）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5f9da8c5-572f-5327-9d65-2de388a37b1e','ced58e12-683c-5563-96a0-d7f86c137384','粉（加料）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b25fa051-e55f-57f6-b2ba-b3aadb09039d','ced58e12-683c-5563-96a0-d7f86c137384','杂粮面（加料）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cd326e17-2f3c-5ff7-97c1-c70cd5467d1e','ced58e12-683c-5563-96a0-d7f86c137384','蟹棒肉（加料）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2936d684-7085-596b-8556-959acd528dae','ced58e12-683c-5563-96a0-d7f86c137384','鱼豆腐（加料）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a57db38d-299b-5cb7-b8fa-152d49220fcf','ced58e12-683c-5563-96a0-d7f86c137384','培根（加料）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('608f5da3-4829-5e36-81c7-eee1690986be','ced58e12-683c-5563-96a0-d7f86c137384','火腿肠（加料）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2cbe28c9-d304-5ed5-a520-72b6567d5192','ced58e12-683c-5563-96a0-d7f86c137384','菌菇（加料）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f9456117-e6bb-5488-8dc6-740ba8600655','ced58e12-683c-5563-96a0-d7f86c137384','肥牛（加料）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a1405668-f27c-52a2-b2eb-677a6531b6dc','ced58e12-683c-5563-96a0-d7f86c137384','撒尿牛丸（加料）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f676b9dd-657f-506b-bf2b-ce780b039dc2','ced58e12-683c-5563-96a0-d7f86c137384','红烧肉（加料）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bd1fd990-7555-55e4-8fbf-0f599123f737','ced58e12-683c-5563-96a0-d7f86c137384','鸡腿肉（加料）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c2f98c58-2b00-5649-aac4-ad4af85d9c1e','ced58e12-683c-5563-96a0-d7f86c137384','牛肉（加料）',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('100a13c4-a18d-55c0-8b91-600aa3ed4e29','0dda9895-1219-5507-b3ab-8b5729d25a45','火腿鸡蛋炒饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d636f838-362f-5544-acc9-0f2ff2924cb0','0dda9895-1219-5507-b3ab-8b5729d25a45','广式腊肠炒饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('65f78e8a-3f9b-5735-8897-fd4fe13a63ff','0dda9895-1219-5507-b3ab-8b5729d25a45','辣白菜五花肉炒饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e20ab4ab-b9e6-5852-8648-89f0ef239859','0dda9895-1219-5507-b3ab-8b5729d25a45','辣白菜牛肉炒饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f75b8920-21d1-5b02-881f-59fd4fdb43b3','0dda9895-1219-5507-b3ab-8b5729d25a45','橄榄菜火腿炒饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('29583c0e-8041-51a4-84c0-0931c823351d','0dda9895-1219-5507-b3ab-8b5729d25a45','老干妈五花肉炒饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1ff7e211-d30e-5b0c-ba87-9330e6652c57','0dda9895-1219-5507-b3ab-8b5729d25a45','奥尔良鸡肉炒饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b9087c13-662c-56e0-aeb8-6d9de81c5fcd','0dda9895-1219-5507-b3ab-8b5729d25a45','酸辣炒河粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8d989ae4-48f9-500e-88de-f5b76c054343','0dda9895-1219-5507-b3ab-8b5729d25a45','干炒牛河',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f5aa5b5-50f4-5cd3-9f41-871a3a4113ae','0dda9895-1219-5507-b3ab-8b5729d25a45','鸡蛋火腿炒河粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dfeb729b-a116-5db1-9921-dd7695bbfd2e','0dda9895-1219-5507-b3ab-8b5729d25a45','香辣五花肉菌菇炒饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('771560ef-a2be-5e66-aeb0-5348893b1773','0dda9895-1219-5507-b3ab-8b5729d25a45','五花肉菌菇炒饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1efcf207-d556-50e4-a5f9-6195ea708727','0dda9895-1219-5507-b3ab-8b5729d25a45','牛肉菌菇炒饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4f342295-e580-56f3-bf29-1584f1c0e09a','0dda9895-1219-5507-b3ab-8b5729d25a45','香辣牛肉菌菇炒饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9b336809-a242-53c9-baca-f0340450217d','0dda9895-1219-5507-b3ab-8b5729d25a45','虾仁菠萝炒饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3744ab50-8f37-52a7-beeb-e611793cf849','0dda9895-1219-5507-b3ab-8b5729d25a45','鸡蛋火腿炒米线',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8a11aef9-9422-515a-8158-6c11818b351d','0dda9895-1219-5507-b3ab-8b5729d25a45','五花肉炒米线',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('24203ec5-0bc3-52ee-b404-f0b4345e00a4','0dda9895-1219-5507-b3ab-8b5729d25a45','扬州炒米线',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c6dab1dd-548c-5fc7-b9f7-b9b863a239ff','0dda9895-1219-5507-b3ab-8b5729d25a45','橄榄菜火腿炒饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f7c18dee-5f24-561f-8ad7-eae3112982ed','0dda9895-1219-5507-b3ab-8b5729d25a45','扬州炒饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('955ff43e-4844-5702-a6c5-0e02a9fbc9ad','0dda9895-1219-5507-b3ab-8b5729d25a45','豆花烤鱼饭',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('03aa03c1-966b-54a3-8168-eec2008e79d6','0dda9895-1219-5507-b3ab-8b5729d25a45','煎蛋烤鱼饭',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('872f0fc7-97de-5e9f-abc3-95fafa6f9aa5','0dda9895-1219-5507-b3ab-8b5729d25a45','年糕烤鱼饭',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('901dc71c-a640-50f7-b57f-50f03deee5b2','0dda9895-1219-5507-b3ab-8b5729d25a45','土豆粉烤鱼饭',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1384e16f-347d-5657-8b59-aab1771f99b0','0dda9895-1219-5507-b3ab-8b5729d25a45','泡面烤鱼饭',NULL,16.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9245d191-153e-51a8-a57a-c4ddc8931fe5','0dda9895-1219-5507-b3ab-8b5729d25a45','炸蛋烤鱼饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4192ec53-ecc3-5284-811a-c356fa40cb8b','0dda9895-1219-5507-b3ab-8b5729d25a45','酥肉烤鱼饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4df2eaff-e8f7-50f2-919c-198f85fcc8b5','0dda9895-1219-5507-b3ab-8b5729d25a45','鱼豆腐烤鱼饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6af3caeb-778c-5221-b095-ecc0bfc847aa','0dda9895-1219-5507-b3ab-8b5729d25a45','酥肉小锅饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dc0050f7-8550-5ebb-b2a9-250b12ccccd1','6b6ea528-3994-5ceb-979f-f612a4647206','招牌鲜烫吊龙米线（清汤）',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('51a51743-2ceb-52ae-b6ab-170e3de09edb','6b6ea528-3994-5ceb-979f-f612a4647206','云南炸酱米线（酱香）',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('78aa2785-5de3-598e-85f0-624362d0a382','6b6ea528-3994-5ceb-979f-f612a4647206','香辣卤牛肉米线（香辣）',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('25d56e6d-cf3c-53c9-807d-f4e3c22ec3b3','6b6ea528-3994-5ceb-979f-f612a4647206','鲜烫梅花肉米线（清汤）',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f17cc460-4cec-559e-a45b-36acbc61991e','6b6ea528-3994-5ceb-979f-f612a4647206','经典双拼米线（牛肉+炸酱）',NULL,21,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('533f0250-bdb8-583f-bcef-2b77ce5062c0','6b6ea528-3994-5ceb-979f-f612a4647206','酸腌菜米线（清汤）',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a6ef92d6-3a83-5be9-ba05-8985d2050172','6b6ea528-3994-5ceb-979f-f612a4647206','牛肉丸米线（清汤）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b34055fd-6f5e-593d-81fd-dc1658b48bca','6b6ea528-3994-5ceb-979f-f612a4647206','泡椒鲜烫吊龙米线（香辣）',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('543c393f-007c-5f6f-8414-f9cc6eb1620f','6b6ea528-3994-5ceb-979f-f612a4647206','番茄吊龙米线',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('711bc13d-4584-540d-958b-15c5aa02399e','6b6ea528-3994-5ceb-979f-f612a4647206','麻辣吊龙米线',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('65989b37-2499-50c7-bcad-3b34bdf397fa','6b6ea528-3994-5ceb-979f-f612a4647206','金汤吊龙米线',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7edc1db5-0924-5067-a4e8-0b8a976201ea','6b6ea528-3994-5ceb-979f-f612a4647206','藤椒吊龙米线',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bc6dbda5-a902-5f0c-b529-70ad3ee018af','6b6ea528-3994-5ceb-979f-f612a4647206','吊龙牛肉（单加）',NULL,6.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('af5afec1-86ad-5b9f-b26c-190d32c73257','6b6ea528-3994-5ceb-979f-f612a4647206','云南炸酱（单加）',NULL,4,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('85227cd1-79b6-5feb-a04a-73d7e5c50bf6','6b6ea528-3994-5ceb-979f-f612a4647206','特色炸蛋（单加）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9f0471e-7660-53b9-af12-d5a44f1259a0','6b6ea528-3994-5ceb-979f-f612a4647206','煎蛋（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('92c7fa32-4cf5-5a96-8517-42c916e5d4ea','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','招牌红烧肥牛汤/拌粉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9cd3cd22-95db-540a-8f87-f69102118f58','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','特色麻辣肥牛汤/拌粉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a32f292b-db76-5862-a752-be1f5cee5ddf','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','香烤鸭腿汤/拌粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f42cfbd1-f470-5a36-9104-6da8c9f0ba08','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','椒麻鸡汤/拌粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('05501ba3-8012-508d-982a-e1ef3cea2853','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','酸辣笋尖汤/拌粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('587fee44-8384-5bf6-825b-07a4cb82ed49','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','低脂时蔬米汤/拌粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2fc373b5-e2f4-5ae3-9c97-3da2d27066c2','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','炙烤鸭腿火鸡面',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('67ab558e-7f2b-5023-8d03-faebfffa5459','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','红油鸡丝火鸡面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1ab3751e-3ad9-5f0c-8f2b-8220bb456f59','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','酸辣笋尖火鸡面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('10cd7ed3-abb2-55cd-a62c-12d3626ba3e3','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','冒烤鸭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f054cace-e7d3-5101-a0c5-5de43cc488d6','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','冒烤红汤肥牛',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7d3a2af9-c6d1-5740-a351-de8420e7552f','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','冒烤香卤鸭头',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4dea6f5b-236d-551d-b72d-9d3eb0a1a7b1','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','冒烤鸡',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fe955393-dfa9-5f45-a355-e9202bf8dafb','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','明湖烤鸭',NULL,23.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ead8378e-31ef-56d3-863b-764c3b16d034','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','方便面（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('69852926-19ac-57fd-a30c-3b91f716c913','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','金针菇（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b58226f5-20f1-5833-a5df-054fb5f526cc','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','娃娃菜（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('396cbc0c-13fc-50e7-b215-6c9fb88b5203','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','香辣分米鸡',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1f99f45-e6eb-5e55-89d6-181309276d5d','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','酱香分米鸡',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('df84df13-5037-5856-b17f-c7ed44e28ea7','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','招牌绝味鸡',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('64a85a22-6b16-522c-aee5-dd39dd8a8ef1','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','麻辣双椒鸡',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0f03a1b6-2f55-5d3b-a522-54d288de075b','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','港式卤肉饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bafc33ff-dec9-5c0d-9dee-3fe0f3bdb2bc','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','香辣鸭腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6ab0671a-558d-56b6-90a5-0a9c7e963edd','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','招牌小炒肉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cf3c06d6-1d80-5122-97cd-b10a97bc6a91','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','辣椒炒肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('44c9a9ae-57a9-546b-8729-8ff9ebf7c4e6','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','回锅肉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ccba1140-d7d4-5d74-bbc9-ccf6d183177b','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','孜然肉片',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5372e374-303d-5364-8a2e-9da403173dab','10f3a596-9e0d-519d-a02e-402c93f8bc59','招牌炙烤五花肉',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c4892a2c-fddf-5a4c-b5fe-6d037b5b4921','10f3a596-9e0d-519d-a02e-402c93f8bc59','法式大排饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4b21cc27-c9f1-569e-8821-7b0f2ffa1eac','10f3a596-9e0d-519d-a02e-402c93f8bc59','招牌玉豆肉酱饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('11fb553a-1c78-58de-93c3-84dddab5a09f','10f3a596-9e0d-519d-a02e-402c93f8bc59','炙烤螺丝椒五花肉',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0291b3d4-1911-5d67-a4c4-2a2edde7e0a0','10f3a596-9e0d-519d-a02e-402c93f8bc59','五花肉烤汁焖饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d2495e5c-74b6-5600-9625-e468fd21bd09','10f3a596-9e0d-519d-a02e-402c93f8bc59','五花肉香辣包菜',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5f5ee978-db38-5c67-a2ae-b5292b63a18e','10f3a596-9e0d-519d-a02e-402c93f8bc59','炙烤梅花肉饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e9cbb99a-d740-51ef-98d2-9621871cf15a','10f3a596-9e0d-519d-a02e-402c93f8bc59','炙烤黑椒肉柳',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8ac34d96-401c-524d-8eef-276abd175c4b','10f3a596-9e0d-519d-a02e-402c93f8bc59','炙烤牛肉饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c60938d4-8fd1-5cd8-9085-ba1122edf229','10f3a596-9e0d-519d-a02e-402c93f8bc59','家传蜜汁鸡腿饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('841a1325-72e9-5270-95b8-e0f1ecf783b7','10f3a596-9e0d-519d-a02e-402c93f8bc59','家传秘制鸡翅饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('47e9e602-5e3f-55c9-9d97-d28fca1e1533','60a470c3-d39e-5029-9300-66f5f4d8e10e','广式腊肠煲仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('567dcc0b-005c-5e0e-a7da-c34b52852853','60a470c3-d39e-5029-9300-66f5f4d8e10e','广式腊肉煲仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ae2158ba-cdef-5df1-95d6-48fa20d771a1','60a470c3-d39e-5029-9300-66f5f4d8e10e','滑鸡煲仔饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a94616d2-b7d4-53be-80eb-d132243ae960','60a470c3-d39e-5029-9300-66f5f4d8e10e','窝蛋牛肉煲仔饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('abe0f7ab-0d87-5397-844c-40ff30b12a9d','60a470c3-d39e-5029-9300-66f5f4d8e10e','豉汁排骨煲仔饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('753878bd-243d-53f6-8add-a42df25e8092','60a470c3-d39e-5029-9300-66f5f4d8e10e','黑金叉烧煲仔饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('210d5662-858c-54c4-8e1a-cdb021233f99','60a470c3-d39e-5029-9300-66f5f4d8e10e','姜汁豉油鸡腿煲仔饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('342e3440-a276-5723-a2e6-df3e36687124','60a470c3-d39e-5029-9300-66f5f4d8e10e','可乐鸡翅煲仔饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('57549353-2a70-534f-91ea-999e4f78d9b9','60a470c3-d39e-5029-9300-66f5f4d8e10e','小锅花雕鸡',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('60567472-0181-50db-afbe-a195f866ec30','60a470c3-d39e-5029-9300-66f5f4d8e10e','小锅啤酒花雕鸡',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ce75a7b6-2be7-5060-938c-7add497060fb','60a470c3-d39e-5029-9300-66f5f4d8e10e','小锅焖排骨',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('75f779b7-20fc-5450-baee-a8ceb805f92f','60a470c3-d39e-5029-9300-66f5f4d8e10e','小锅焖蹄花鸡',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5ab64cd9-eaae-5646-8f51-5418de347af5','60a470c3-d39e-5029-9300-66f5f4d8e10e','小锅焖蹄花',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d4b69167-d0bb-5f40-829c-bb6bffacddbc','60a470c3-d39e-5029-9300-66f5f4d8e10e','窝蛋（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7c75263-5f8a-5210-9223-f1c72a06e5b3','60a470c3-d39e-5029-9300-66f5f4d8e10e','炉饼（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ec704eb0-84cd-5010-a346-6e3c14201b81','60a470c3-d39e-5029-9300-66f5f4d8e10e','娃娃菜（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c00b26ab-122c-57e6-b78b-045f76dda65f','60a470c3-d39e-5029-9300-66f5f4d8e10e','方便面（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f46db9f9-3091-5ab7-a494-ceb4b5f8da5a','60a470c3-d39e-5029-9300-66f5f4d8e10e','金针菇（加菜）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('97c6f7b5-d919-51c3-bc70-bc8ae8a0f5eb','60a470c3-d39e-5029-9300-66f5f4d8e10e','土豆宽粉（加菜）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0d07e235-acc1-5376-b3c1-9046c3ce4fec','60a470c3-d39e-5029-9300-66f5f4d8e10e','罗马生菜（加菜）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a9e92e5f-690c-51b7-a93c-f7c93c1f9083','6ade0f06-e790-547e-ac49-71f2a5e670d2','素菜肠粉',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d7118b7e-7cf4-562f-8113-0d2b29a3f817','6ade0f06-e790-547e-ac49-71f2a5e670d2','鸡蛋肉沫肠粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('066ea278-e5b3-5364-a606-56c400fdb836','6ade0f06-e790-547e-ac49-71f2a5e670d2','鸡蛋叉烧肠粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a6524517-fcd6-57ec-8552-9e81084a5f44','6ade0f06-e790-547e-ac49-71f2a5e670d2','牛肉肠粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7f46b71f-95b7-5d1e-9168-60a8c90980bc','6ade0f06-e790-547e-ac49-71f2a5e670d2','鸡蛋虾仁肠粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5ef7d429-4017-55cd-b928-237c47b93d9c','6ade0f06-e790-547e-ac49-71f2a5e670d2','香肠肠粉',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('06381ae1-0b99-5ad7-8e37-70c722ca2aae','6ade0f06-e790-547e-ac49-71f2a5e670d2','烧鸭饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9a4fa1f8-ef45-556d-ab2c-ceac6d1878ba','6ade0f06-e790-547e-ac49-71f2a5e670d2','烧鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('92c182f7-67d9-532a-9cc0-344705bc63ce','6ade0f06-e790-547e-ac49-71f2a5e670d2','叉烧饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ca2dfa10-6431-5dbb-802c-4bf19517425b','6ade0f06-e790-547e-ac49-71f2a5e670d2','叉烧鸡腿饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dc4270e1-73f5-549b-90bb-bc7c1c032e23','6ade0f06-e790-547e-ac49-71f2a5e670d2','叉烧鸡饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d6604952-7dbc-53b0-96e2-353545ecde41','6ade0f06-e790-547e-ac49-71f2a5e670d2','叉烧烧鸭饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a1a1778e-ecb3-5ed7-9dc1-d38c790cc7db','6ade0f06-e790-547e-ac49-71f2a5e670d2','隆江猪脚饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b26703fb-3bc0-5080-b38f-9a555b2fe09f','6ade0f06-e790-547e-ac49-71f2a5e670d2','豉油鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b9e40c20-f812-53bf-90f3-4ea946fc27e8','6ade0f06-e790-547e-ac49-71f2a5e670d2','口水鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7571e9ff-1cfc-5c19-b94f-df199988a8f7','6ade0f06-e790-547e-ac49-71f2a5e670d2','蒸宝焖鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eb99e9fe-3195-5fbe-baea-b9205b851f11','6ade0f06-e790-547e-ac49-71f2a5e670d2','广式柠檬鸭饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2f9d0f8e-dcfa-55a9-a705-24f2d2b0b7fa','6ade0f06-e790-547e-ac49-71f2a5e670d2','黑椒鸡排饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('de823757-d8c7-5c70-90c1-4784e56c5cee','6ade0f06-e790-547e-ac49-71f2a5e670d2','烧鸭排骨饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('747d5b36-6d0c-5ddc-b8c1-b15d0bf77234','6ade0f06-e790-547e-ac49-71f2a5e670d2','窝蛋滑鸡砂锅饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e8607594-bbea-5e63-a11a-866ab240d6cb','6ade0f06-e790-547e-ac49-71f2a5e670d2','叉烧滑鸡饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('069f5817-4dba-5535-8920-4b2c13aebc1e','6ade0f06-e790-547e-ac49-71f2a5e670d2','叉烧鸡饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fcf00651-f900-5f87-a653-70620d1a2edf','6ade0f06-e790-547e-ac49-71f2a5e670d2','烧鸭鸡腿饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e7c997f2-9e86-52eb-b920-0764619566f0','6ade0f06-e790-547e-ac49-71f2a5e670d2','排骨五花饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('96f65735-ebf0-504b-b4bb-bc63a347c333','6ade0f06-e790-547e-ac49-71f2a5e670d2','叉烧鸭双拼饭',NULL,22,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('02a8e289-525c-5795-bcd6-562638df3883','6ade0f06-e790-547e-ac49-71f2a5e670d2','窝蛋三拼饭',NULL,22,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d5ed47f2-964c-5772-a826-f787936b4f0c','b47b0628-073e-5483-9670-a9312f0e01a7','麻辣香锅/麻辣烫/麻辣拌（自选称重）',NULL,2.18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4bb80b14-675b-5e14-a420-1a0979bb09ed','b47b0628-073e-5483-9670-a9312f0e01a7','招牌鲜香炒鸡',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('87dc499c-89f8-5ee6-8c36-6e5adaa44755','b47b0628-073e-5483-9670-a9312f0e01a7','青花椒炒鸡',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5e979e8b-d786-5b57-9417-45d0a90d0625','b47b0628-073e-5483-9670-a9312f0e01a7','回味菌菇炒鸡',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c65cd75a-8157-537a-b36a-da92fea08758','b47b0628-073e-5483-9670-a9312f0e01a7','秘制香辣炒鸡',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4a674a11-d761-5875-9cd1-e5c616f812d1','b47b0628-073e-5483-9670-a9312f0e01a7','香辣鸡公煲',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('edf44564-e7fb-52c7-ab2a-c8cc4214c175','b47b0628-073e-5483-9670-a9312f0e01a7','酱香鸡公煲',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dee4ea7d-7534-582c-ae2c-8361dd7cc697','b47b0628-073e-5483-9670-a9312f0e01a7','香辣肥牛煲',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e5f47cf2-2897-5dee-a0a0-f45cb1006ae4','b47b0628-073e-5483-9670-a9312f0e01a7','青花椒鸡公煲',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('334d4826-81ee-5835-bfe7-02fccf78e491','b47b0628-073e-5483-9670-a9312f0e01a7','鱼豆腐鸡公煲',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('42f20ba8-116e-5bf9-9e68-0f3d42e2614f','b47b0628-073e-5483-9670-a9312f0e01a7','酱香/香辣排骨煲',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9d3bc4c8-5636-5cac-b35f-4d0b28cd5aa4','b47b0628-073e-5483-9670-a9312f0e01a7','招牌酸辣粉/面',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d63fc1c8-631b-53ae-b108-495a2375fecb','b47b0628-073e-5483-9670-a9312f0e01a7','韩式泡菜酸辣粉/面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9c2a0fbc-d40d-5161-a39f-5f5cc2a3bd6c','217f40fb-0a92-510f-b9d8-139999735cf2','素炒米粉/河粉',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('503608ac-481d-58c9-a17c-219a1ed1868f','217f40fb-0a92-510f-b9d8-139999735cf2','鱼豆腐炒米粉/河粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ef94e33c-0b63-5e0d-a931-6b201cad8a47','217f40fb-0a92-510f-b9d8-139999735cf2','火腿炒米粉/河粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8f83eb6e-8ef6-55b3-b2c9-540ff1ed89f6','217f40fb-0a92-510f-b9d8-139999735cf2','鸡肉炒米粉/河粉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4201a525-7041-5b70-8b13-825b474f87bd','217f40fb-0a92-510f-b9d8-139999735cf2','牛肉炒米粉/河粉',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('de8cc0ec-e5c4-5de5-bba3-38b79a68b9f3','217f40fb-0a92-510f-b9d8-139999735cf2','牛肉汤',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('98a1ac76-b181-5f2f-8239-725064b37da6','217f40fb-0a92-510f-b9d8-139999735cf2','剔骨牛肉汤',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d6274048-d22a-5e07-9bba-1bec8f4c4401','217f40fb-0a92-510f-b9d8-139999735cf2','母鸡汤',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cc7e330e-1415-5651-a84b-dca99518304b','217f40fb-0a92-510f-b9d8-139999735cf2','牛肉汤河粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('01aaac37-96fd-5d41-a59d-a79990b3934e','217f40fb-0a92-510f-b9d8-139999735cf2','剔骨牛肉河粉',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8e5efb41-a835-56b5-855e-8424a401f760','217f40fb-0a92-510f-b9d8-139999735cf2','母鸡汤河粉',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6e9e88d2-dab8-55ba-9216-107e1b7eea09','217f40fb-0a92-510f-b9d8-139999735cf2','熟牛肉片（单加）',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3be6309e-4374-563b-b2f7-cecc7a4703a7','217f40fb-0a92-510f-b9d8-139999735cf2','鹌鹑蛋（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('86077de7-c44e-564d-88fa-5bb11aec8ab3','217f40fb-0a92-510f-b9d8-139999735cf2','葱油饼',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('59dc3266-fb31-5e97-89cb-90ed01107463','0bafdf33-8eae-546d-ac3c-98239dd858c9','牛油锅底',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3c89c2ec-e7d4-5c21-87d0-2187c76a1089','0bafdf33-8eae-546d-ac3c-98239dd858c9','过把瘾老火锅锅底',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('70c6f220-c5b1-5ea7-9113-ab59a1ce211e','0bafdf33-8eae-546d-ac3c-98239dd858c9','秘制三鲜锅底',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('41513ca7-f175-5568-8e08-544c5e62257c','0bafdf33-8eae-546d-ac3c-98239dd858c9','姬松茸菌汤锅底',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3a899748-f444-51d2-9e3a-524218c8d69a','0bafdf33-8eae-546d-ac3c-98239dd858c9','番茄浓汤锅底',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cae086fb-56af-5a8d-89f5-7b1a8eb8993f','0bafdf33-8eae-546d-ac3c-98239dd858c9','老坛酸菜锅底',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b4b1492f-ba5c-586f-876b-cc745532a747','0bafdf33-8eae-546d-ac3c-98239dd858c9','水果酸汤锅底',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d4877f30-841a-5eae-a522-d53bdc7b1fb5','0bafdf33-8eae-546d-ac3c-98239dd858c9','牛肉卷',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('66509695-a55c-5e94-baa5-1498e18d30b8','0bafdf33-8eae-546d-ac3c-98239dd858c9','羊肉卷',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e6841b5e-c395-50b8-b168-9b11c7e18b53','0bafdf33-8eae-546d-ac3c-98239dd858c9','鸭肉卷',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7e38c5c8-040b-52fc-abcf-aaa2872787d1','0bafdf33-8eae-546d-ac3c-98239dd858c9','五花肉卷',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ffedec8e-f520-5863-a2f0-ec0c99de8d13','0bafdf33-8eae-546d-ac3c-98239dd858c9','绿色签夹子',NULL,1,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('90a23b63-5d5a-5224-98b7-2172e60eb27d','0bafdf33-8eae-546d-ac3c-98239dd858c9','黄色签夹子',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('15ee6cc7-d4a9-5e21-abe8-e30ea05326e6','0bafdf33-8eae-546d-ac3c-98239dd858c9','橙色签夹子',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d197d404-f8b3-5849-8b92-3973805ed09d','0bafdf33-8eae-546d-ac3c-98239dd858c9','蓝色签夹子',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('81324765-2639-5af8-bafb-bd6ec80010aa','b70bbd25-46c1-51b4-8a2d-8cbd6638f9b1','麻辣拌（称重）',NULL,19.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a90e4e2f-5e93-5bd8-9e69-a9b5b014a3df','b70bbd25-46c1-51b4-8a2d-8cbd6638f9b1','麻辣烫（称重）',NULL,19.8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2ba3cef5-f45b-567e-ade7-33194142c7cb','b70bbd25-46c1-51b4-8a2d-8cbd6638f9b1','沈阳拌鸡架',NULL,11,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('36bdd01f-1420-5e2c-a4f2-3b386c5124b8','b70bbd25-46c1-51b4-8a2d-8cbd6638f9b1','鸡架拌面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cdd34df1-ceda-5a81-9123-a784ca5ee664','b70bbd25-46c1-51b4-8a2d-8cbd6638f9b1','另外加面',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('eedbcc19-c87b-5e3c-a3c5-b80dbe656579','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','黄焖茄子',NULL,10,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8ff9e311-5000-5b0c-87da-86e776b14f5a','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','土豆鸡块',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c63e2a67-773f-55ab-8831-304f57ff10aa','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','黄焖鸡',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('f452ccb7-9246-5af8-9963-09bd0634ecda','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','排骨鸡',NULL,23,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2e701b19-cec7-56c9-8b15-0febacb9117d','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','黄焖茄子（肥牛、五花肉）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c4850fd4-a836-5bbd-8880-108064ec0ea8','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','娃娃菜（肥牛、五花肉）',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('385557cb-c915-5aa1-a984-d606c928ce79','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','金针菇五花肉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1037a97a-2f16-5298-861c-f5b5b37983dd','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','鱼豆腐五花肉',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c883edf7-e87b-5889-8647-09501d0ac5d5','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','藤椒鸡公煲',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1f4a1776-b6e3-5c95-8e86-3d3b0571fce8','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','金针菇肥牛',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5efefd2f-64b1-541a-8d92-0220a846b412','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','鱼豆腐肥牛',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2ffd97ca-d248-52e2-9149-6b904eddf506','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','油麦菜（单加）',NULL,1.5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('dc2e3a8a-5a53-595c-87cc-5d7b7a78f95d','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','金针菇（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b136b6d3-f428-5dcc-8431-8c34769a2957','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','鱼豆腐（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('240b6783-e132-5bdc-bbb1-6ee6d4d0c702','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','煎蛋（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('0486ad06-0a40-56e6-ab6b-3778b5432a3b','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','茄子（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('95f4cc40-4ec8-5c24-96b5-5e5bd2da2dd7','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','豆皮（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3f4b9a9d-5518-5f6b-b7bc-328297e9e631','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','娃娃菜（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e2c67731-8274-5338-a8aa-bec3602faffc','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','方便面（单加）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6d54b8e3-47a8-59b2-83c1-f5ebe4d23a16','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','五花肉（单加）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('64e801eb-c7a7-548e-ac51-eeb78f7d847c','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','肥牛（单加）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26d7ee12-5099-535c-a048-cbce67f1cb32','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','猪肉白菜水饺',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fa3a5198-9230-5287-bbe4-c2d7c9bfb095','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','韭菜鸡蛋水饺',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('269c6491-ec95-5415-b78d-45458cd8bce4','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','韭菜猪肉水饺',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6d5cdc1a-0fa0-50fa-b161-26e78a5f5c5e','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','猪肉酸菜水饺',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2d587482-01a0-5427-8170-f48269d960c7','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','猪肉芹菜水饺',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('025c9a5d-59cd-528c-8d9c-fb5cfbaff515','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','猪肉茴香水饺',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('edcec840-ee16-52c9-b53d-1706774df563','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','猪肉香菜水饺',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1695f733-7c98-5c27-b90d-ccfde9da21b5','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','猪肉大葱水饺',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e46b31db-78b6-5d31-a59f-9d180f3b5e87','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','猪肉香菇水饺',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fbf94d16-b8ca-5028-88f4-cc20bcf45d59','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','牛肉洋葱水饺',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e6a9acf1-c0bf-5c6b-8a86-5f50c79e8528','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','猪肉三鲜水饺',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1dd30cc-49fb-52c9-a73b-fc60b6ca162a','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','鲜肉锅贴',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2ebd0f78-227c-51f9-8ce2-c161988c5a85','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','玉米猪肉锅贴',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1e98c1da-e781-540e-953d-c057e314abc2','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','鲜肉馄饨',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('26a67077-6850-559c-8607-f12565da565e','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','荠菜肉馄饨',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b3640a93-ecb2-5118-90fd-1fa923bdb904','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','虾仁肉馄饨',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('033d5a78-228b-53cd-8609-a0f4f7c815f3','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','老麻抄手',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('be5e3ada-7cf2-5b11-a225-f71fb3e4fa97','97722745-ce68-5b3b-8490-ef7a08cf2441','红烧牛肉饭',NULL,23,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('283325b8-6d81-589a-8e35-fe2642918c8f','97722745-ce68-5b3b-8490-ef7a08cf2441','香辣牛肉饭',NULL,24,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('72e11de0-8f44-527f-bdf2-dfbfa9f04cd4','97722745-ce68-5b3b-8490-ef7a08cf2441','番茄牛肉饭',NULL,23,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('02709c39-16d5-5b92-9acd-7ab73ec59e6b','cdc93861-b55a-5831-8c96-a180654c50e3','琥珀虾仁滑蛋饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a38c77af-f3a3-5e19-b065-15f284c4b4bf','cdc93861-b55a-5831-8c96-a180654c50e3','双椒火腿滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('49e610f8-0ec8-5078-9ddc-6da9f630d1a6','cdc93861-b55a-5831-8c96-a180654c50e3','秘制鸡丁滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6f1fbd05-7298-5612-bd8d-b8b43e9c0ad9','cdc93861-b55a-5831-8c96-a180654c50e3','小酥肉滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('71a8098f-7998-5575-a915-8858c5294fc0','cdc93861-b55a-5831-8c96-a180654c50e3','五花肉滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a30e4572-20c8-590a-b471-d2e61f53b438','cdc93861-b55a-5831-8c96-a180654c50e3','意式肥牛滑蛋饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('68b6ede7-8218-5227-9d95-8b45d39dfe1b','cdc93861-b55a-5831-8c96-a180654c50e3','港式滑蛋饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b8cdf5d-3fdb-5cf8-a9c3-bb252f1847e5','cdc93861-b55a-5831-8c96-a180654c50e3','港香肉沫滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e9e399b2-9343-5097-a524-86f913408512','cdc93861-b55a-5831-8c96-a180654c50e3','秘制鸡丁滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5a586f79-e464-54fe-8537-e954e03d2559','cdc93861-b55a-5831-8c96-a180654c50e3','铁板鸡排滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('efda60e9-b275-5307-bbb5-e8d1241904d4','cdc93861-b55a-5831-8c96-a180654c50e3','奥尔良鸡排滑蛋饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fa9d456c-3b69-5d24-9a47-c5231199f1c5','cdc93861-b55a-5831-8c96-a180654c50e3','黑椒牛肉滑蛋饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('82d3bcb9-97d4-5f19-8063-30354e4727c0','cdc93861-b55a-5831-8c96-a180654c50e3','黑椒孜然牛肉滑蛋饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9c169cae-0dac-585c-862b-02f9fa72457c','cdc93861-b55a-5831-8c96-a180654c50e3','鸭肉（单加）',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('51f47e5f-b6a2-56a5-b172-664210b62fc6','cdc93861-b55a-5831-8c96-a180654c50e3','小酥肉（单加）',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cfb0b3e9-f75d-5770-8efd-70824fc91fce','cdc93861-b55a-5831-8c96-a180654c50e3','培根（单加）',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e5573922-be29-5edb-99c2-ae759c68e7e6','cdc93861-b55a-5831-8c96-a180654c50e3','火腿（单加）',NULL,6,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('79c26982-4844-5737-b35a-066023fad6c9','cdc93861-b55a-5831-8c96-a180654c50e3','五花肉（单加）',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5b9e5698-7144-59d2-a709-05e8d2375591','cdc93861-b55a-5831-8c96-a180654c50e3','肥牛（单加）',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('8106ef3d-fc33-50fb-a77f-260ffd72c3e5','cdc93861-b55a-5831-8c96-a180654c50e3','牛肉（单加）',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5664f0cd-4af8-5e8d-8263-ebf4b603f2cc','cdc93861-b55a-5831-8c96-a180654c50e3','虾仁（单加）',NULL,8,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d2a1ab4c-40a0-5a75-bfb7-8be823029fa2','b55c32db-c2bc-54bc-9565-b80e029beeb3','武汉黑鸭饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cf7a9de1-0816-5c6f-83cf-e75d91641dd2','b55c32db-c2bc-54bc-9565-b80e029beeb3','超级卤肉饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a48ca5c0-ab84-5a02-9ae2-edc5fd1bef54','b55c32db-c2bc-54bc-9565-b80e029beeb3','椒麻鸡肉饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7e0e31ec-fa98-5cd7-a73f-96207e263bb9','b55c32db-c2bc-54bc-9565-b80e029beeb3','卤汁肉肠饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e8fe636-45a2-56e1-968c-68fa0fec1712','b55c32db-c2bc-54bc-9565-b80e029beeb3','金牌肘子饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('07f33dc8-19ae-5093-9965-595790b304f5','b55c32db-c2bc-54bc-9565-b80e029beeb3','卤汁茄子饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7f8c32c3-e12a-53bf-94df-c83bde662768','b55c32db-c2bc-54bc-9565-b80e029beeb3','武汉莲藕饭',NULL,12,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c74efee8-ace3-5e8c-9881-aa11ba357e01','b55c32db-c2bc-54bc-9565-b80e029beeb3','老武汉炸鸡',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6d06b7c0-1e79-562d-81b2-2305fac31374','b55c32db-c2bc-54bc-9565-b80e029beeb3','卤汁鸡排饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a54b7ce2-4841-5ebd-beaa-55fbce28e0cd','b55c32db-c2bc-54bc-9565-b80e029beeb3','孜然小酥肉',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('23f4086f-0cc3-52f4-8996-cb7e19133670','b55c32db-c2bc-54bc-9565-b80e029beeb3','甜辣鸭翅根',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c6878b78-f1f4-54ad-9f13-43f3297b609c','b55c32db-c2bc-54bc-9565-b80e029beeb3','卤香翅根饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('4296cdc6-fef2-53d6-ad21-ea39864354ac','7cc04078-48a6-53fe-a42f-07a1912937cf','炸酱面/米线',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('5b65532c-b99b-5198-97fb-bd5bada139ab','7cc04078-48a6-53fe-a42f-07a1912937cf','外婆菜/烤肠炒鸡蛋面/米线',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('88e6232a-c443-5e17-8ecb-f64e1fe8bf26','7cc04078-48a6-53fe-a42f-07a1912937cf','豌杂面/米线',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6f5b980d-6edf-5411-94ef-d9f70303bb20','7cc04078-48a6-53fe-a42f-07a1912937cf','泡椒鸡胗面/米线',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('32316dfe-fab1-581c-bae8-d4fe64a18c81','7cc04078-48a6-53fe-a42f-07a1912937cf','榨菜肉丝面/米线',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cf8d01ad-5e8e-5ef9-b636-b47604b2bd76','7cc04078-48a6-53fe-a42f-07a1912937cf','肉沫酸豆角面/米线',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c66a006f-dd1a-5b02-8b26-97954fbf2cb2','7cc04078-48a6-53fe-a42f-07a1912937cf','孜然肉片面/米线',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('55fc0c9f-efb9-5431-b142-d5982564c6c9','7cc04078-48a6-53fe-a42f-07a1912937cf','农家小炒肉面/米线',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e67c7695-a912-567a-a52f-e46a1c070388','7cc04078-48a6-53fe-a42f-07a1912937cf','双椒鸡面/米线',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a815d15d-62d4-517e-a744-b96f099675be','7cc04078-48a6-53fe-a42f-07a1912937cf','回锅肉面/米线',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('47a85664-ae23-5bf8-8dc0-45eed66f2912','7cc04078-48a6-53fe-a42f-07a1912937cf','红烧牛肉面/米线',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('571dd2d8-244d-53d1-a45e-4c643f6c3f24','7cc04078-48a6-53fe-a42f-07a1912937cf','素面条/米线',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('65231099-75c0-5d0d-9abd-8154ef450ae5','7cc04078-48a6-53fe-a42f-07a1912937cf','土豆泥拌面/米线',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2c6395b2-76b7-5ed3-bbec-0bd899fcc949','7cc04078-48a6-53fe-a42f-07a1912937cf','酸辣粉',NULL,9,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e8093f90-c3b6-5246-aaab-996dc7b5201a','7cc04078-48a6-53fe-a42f-07a1912937cf','外婆菜炒鸡蛋土豆泥拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('de9e2215-2d2f-5508-a8f5-6c80018b5868','7cc04078-48a6-53fe-a42f-07a1912937cf','泡椒鸡胗土豆泥拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('66426cf5-25b0-5ef8-9028-5ce29056ba12','7cc04078-48a6-53fe-a42f-07a1912937cf','肉沫豆角土豆泥拌饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3e15a620-99a2-5950-acc5-ae938ef90708','7cc04078-48a6-53fe-a42f-07a1912937cf','农家小炒肉土豆泥拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bbef3cea-3927-56ac-a557-360c0b9300b4','7cc04078-48a6-53fe-a42f-07a1912937cf','孜然肉片土豆泥拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('57d8a66c-6859-5f80-8a3e-0d6160170982','7cc04078-48a6-53fe-a42f-07a1912937cf','双椒鸡土豆泥拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c8f9f729-248b-5590-853d-86274d369027','7cc04078-48a6-53fe-a42f-07a1912937cf','辣椒炒五花肉土豆泥拌饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fac78434-bbd8-503b-b7ac-785f72722702','7cc04078-48a6-53fe-a42f-07a1912937cf','任意双拼土豆泥拌饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('04e6d7a7-6ab8-5a72-8b73-9bcb5c7ab858','7cc04078-48a6-53fe-a42f-07a1912937cf','卤蛋（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fffae144-aa7b-5297-8faf-305be04bacd5','7cc04078-48a6-53fe-a42f-07a1912937cf','炸蛋（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1b322b5d-5f92-5f1f-848d-28d763bce315','7cc04078-48a6-53fe-a42f-07a1912937cf','豆皮（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d1a41e17-d983-5efb-872c-6694752b5e1f','7cc04078-48a6-53fe-a42f-07a1912937cf','肉肠（加菜）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b5c62485-81c0-5718-94d6-9408a4a891f9','7cc04078-48a6-53fe-a42f-07a1912937cf','鸡腿（加菜）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('098ddc0e-6445-5430-9595-81393e4c8b61','0dd40e8e-cef6-5f60-9075-aafd3a68243a','薯条豆角焖面',NULL,13,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('51aaf321-b93d-53f6-98fc-596ae84f9d9b','0dd40e8e-cef6-5f60-9075-aafd3a68243a','培根焖面',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('48c20e48-b70b-5442-8e53-a0797c5035f0','0dd40e8e-cef6-5f60-9075-aafd3a68243a','肉沫薯条焖面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('182f24d3-e9fe-5796-b19b-f8dd17f930df','0dd40e8e-cef6-5f60-9075-aafd3a68243a','肉沫茄条焖面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('53f064e8-de54-59da-925a-3b7da5e4498b','0dd40e8e-cef6-5f60-9075-aafd3a68243a','肉沫豆角焖面',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e3fb5e1f-84c0-572d-a522-bf2a60d4f088','0dd40e8e-cef6-5f60-9075-aafd3a68243a','五花肉焖面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('540994a1-24cc-5b64-a246-9ae7e66c6ca7','0dd40e8e-cef6-5f60-9075-aafd3a68243a','广式腊肠焖面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('fd393390-21c1-5208-92d8-db03fa7c9b42','0dd40e8e-cef6-5f60-9075-aafd3a68243a','奥尔良鸡丁焖面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c13add13-01c1-5636-a7e0-452c2590a356','0dd40e8e-cef6-5f60-9075-aafd3a68243a','腊肉豆角焖面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7f21f1b7-1de5-5647-81cd-78016f3d53f8','0dd40e8e-cef6-5f60-9075-aafd3a68243a','孜然肉片焖面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b35edd2e-0432-5f47-94a8-60b3a5a49ded','0dd40e8e-cef6-5f60-9075-aafd3a68243a','鸡腿肉焖面',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d8a1ad45-2f30-5cd6-bfd8-8e72495555b1','0dd40e8e-cef6-5f60-9075-aafd3a68243a','黑椒牛肉焖面',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('7dd8b2aa-a7f8-524c-aa4c-9a7039de6e4e','0dd40e8e-cef6-5f60-9075-aafd3a68243a','外婆菜炒鸡蛋焖饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('a2cccfae-d2d8-5500-8524-d4d6d6d5c2f0','0dd40e8e-cef6-5f60-9075-aafd3a68243a','烤肠炒鸡蛋焖饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b455a103-fb54-540c-a30f-19e93b056969','0dd40e8e-cef6-5f60-9075-aafd3a68243a','泡椒鸡胗焖饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('179e1a4c-a981-544b-83cd-c6efc7032c19','0dd40e8e-cef6-5f60-9075-aafd3a68243a','肉沫豆角焖饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2037bf3a-5eb4-5491-957e-3648cfe73220','0dd40e8e-cef6-5f60-9075-aafd3a68243a','肉沫酸豆角焖饭',NULL,15,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6bfcb03d-a07e-5a2c-90a2-c3af1160bd71','0dd40e8e-cef6-5f60-9075-aafd3a68243a','农家小炒肉焖饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('230c3a20-e071-50c0-b39d-8933832611ae','0dd40e8e-cef6-5f60-9075-aafd3a68243a','孜然肉片焖饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bb86db15-8796-5815-8c99-4256f39286e3','0dd40e8e-cef6-5f60-9075-aafd3a68243a','辣椒炒五花肉焖饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cdaa08c6-2cc8-5327-acb2-44212c46b087','0dd40e8e-cef6-5f60-9075-aafd3a68243a','双椒鸡焖饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('b1f19f58-8337-58fa-ba71-0ec650b57631','0dd40e8e-cef6-5f60-9075-aafd3a68243a','任意双拼焖饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('36a4c77a-fb6f-59cd-8951-bd460bb54f8b','0dd40e8e-cef6-5f60-9075-aafd3a68243a','卤蛋（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('650c257c-a2e3-59e0-9df9-5609eade9263','0dd40e8e-cef6-5f60-9075-aafd3a68243a','炸蛋（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('926f829a-6df7-563a-b2e2-368174ef9b06','0dd40e8e-cef6-5f60-9075-aafd3a68243a','豆皮（加菜）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('c04772e7-4bcc-5ad6-a88e-765f940f212a','0dd40e8e-cef6-5f60-9075-aafd3a68243a','肉肠（加菜）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d863d96c-84ea-5f38-97eb-4fe4b63a1822','0dd40e8e-cef6-5f60-9075-aafd3a68243a','鸡腿（加菜）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('cea3dc57-89c6-5249-b5fb-131784cd3ce3','d9a2ae41-2799-540b-a50f-98088f32e108','咸香鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1ad155c9-b575-5537-b803-64969dfe0b50','d9a2ae41-2799-540b-a50f-98088f32e108','脆皮鸡排饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6a7d8815-3f62-5597-9e82-0224b5050eac','d9a2ae41-2799-540b-a50f-98088f32e108','奥尔良烤鸡腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('928f9d4b-1a1b-5591-9648-2b34e992f731','d9a2ae41-2799-540b-a50f-98088f32e108','手撕鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('6b141d23-a666-5a63-beb1-d68c180fc629','d9a2ae41-2799-540b-a50f-98088f32e108','口水鸡饭（辣）',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('939e98b5-1908-59cf-bbe3-c76f888c8c90','d9a2ae41-2799-540b-a50f-98088f32e108','葱油鸡饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('76c84e5b-df42-5c13-bbe5-58b2c3b1d658','d9a2ae41-2799-540b-a50f-98088f32e108','广式腊肠饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2494d8ea-bf0f-59ca-9c69-23e4564cfbf9','d9a2ae41-2799-540b-a50f-98088f32e108','香酥鸭腿饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ebec4eaf-fc56-59e6-9514-52cb66645741','d9a2ae41-2799-540b-a50f-98088f32e108','烧鸭饭',NULL,16,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('37f538aa-ffb6-5fd9-a510-c001ca937482','d9a2ae41-2799-540b-a50f-98088f32e108','烧鸭腿饭',NULL,17,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('77f5fa94-651c-5be4-9ed0-a067e4affbb9','d9a2ae41-2799-540b-a50f-98088f32e108','叉烧饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ceaded3f-0fa9-58ff-8959-78acec83727c','d9a2ae41-2799-540b-a50f-98088f32e108','脆皮五花肉饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('9e5c02dd-e90d-5a8f-986a-d5364eb2aaac','d9a2ae41-2799-540b-a50f-98088f32e108','隆江猪脚饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('2cd57aea-cdaa-556b-aa68-26d04c35fd65','d9a2ae41-2799-540b-a50f-98088f32e108','肘子饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('455d2f07-c673-5c40-9bb5-a35f2497c490','d9a2ae41-2799-540b-a50f-98088f32e108','卤肉饭（卤梅花肉）',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('3962aa98-040c-5ad9-8b91-886acacac3b9','d9a2ae41-2799-540b-a50f-98088f32e108','鸡蛋豆干肉肠饭',NULL,14,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1d5622d2-33c8-5f0a-aa2a-6bb6caa9e477','d9a2ae41-2799-540b-a50f-98088f32e108','秘制卤肉饭',NULL,18,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('31b0e87d-a475-57b9-8779-b749cb78df38','d9a2ae41-2799-540b-a50f-98088f32e108','双拼饭',NULL,20,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('793e007e-6ada-50d5-8eb3-2ddcbe2e1b96','d9a2ae41-2799-540b-a50f-98088f32e108','卤鸡蛋（加料）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d9a59e4d-7b65-5166-a270-83ea2e9fd4e2','d9a2ae41-2799-540b-a50f-98088f32e108','卤鸭头（加料）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('61cc9588-120e-50c2-817d-09ddbd201b4e','d9a2ae41-2799-540b-a50f-98088f32e108','卤豆干（加料）',NULL,2,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('d35c015e-5237-554b-a7be-49a718616830','d9a2ae41-2799-540b-a50f-98088f32e108','卤肉肠（加料）',NULL,3,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('30d17cb6-3fe3-5143-92b7-6951c4d567d5','d9a2ae41-2799-540b-a50f-98088f32e108','广式腊肠（加料）',NULL,5,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('39c16beb-50fc-5639-8f5e-ecb4f469b244','4252061b-6c95-58e4-b6ed-2c76582d2e7a','自选称重餐',NULL,1.88,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('86b0bd93-58e4-5002-8234-a307e8e64fa8','4252061b-6c95-58e4-b6ed-2c76582d2e7a','S级招牌鸡牛双拼健康餐','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('45633dce-2b36-5508-9664-bdba963e6a6f','4252061b-6c95-58e4-b6ed-2c76582d2e7a','基础碗（无蛋白质）','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('ef291f35-182a-52e2-a09e-e53d00a166b3','4252061b-6c95-58e4-b6ed-2c76582d2e7a','鲜卤原切真牛肉能量碗','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('67f889d0-349c-59b8-965d-8796e79fbafc','4252061b-6c95-58e4-b6ed-2c76582d2e7a','现烤黑椒鸡胸健康餐','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('e9b33a3b-0f80-5d62-8d94-90ebabc6a646','4252061b-6c95-58e4-b6ed-2c76582d2e7a','每日现烤中式鸡腿排健康餐','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('adb92a7a-96f6-5a7e-adb5-19a6f72f4467','4252061b-6c95-58e4-b6ed-2c76582d2e7a','霸王烤全腿能量碗','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('bc10f7e3-472c-5c3d-9443-bb9aa35ef0c7','4252061b-6c95-58e4-b6ed-2c76582d2e7a','香煎鳕鱼健康餐','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('1f3b8d31-89f1-5378-bced-07723791aeb3','4252061b-6c95-58e4-b6ed-2c76582d2e7a','黛味打抛肉拌饭※','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('835a2de3-119a-58f5-af52-f385875769d5','4252061b-6c95-58e4-b6ed-2c76582d2e7a','黑椒鸡胸咖喱饭','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status)
VALUES ('38499ca9-c827-5f84-b707-f3f173cbb0c0','4252061b-6c95-58e4-b6ed-2c76582d2e7a','鲜烤牛肉拌饭','原价标注：价格未标示，待人工核价',0,'active')
ON CONFLICT (id) DO NOTHING;

-- 五、档口图集（含首图，首图 URL 与档口 cover 一致）。
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('ab80a264-7961-5f25-bbc8-da2db5a3bab6','a689caf7-27e9-5ead-90ac-421560ea3ef8','/api/v1/media/ab80a264-7961-5f25-bbc8-da2db5a3bab6',0)
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
VALUES ('199fb876-5bb1-5f33-b372-1cf0c0632fea','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','/api/v1/media/199fb876-5bb1-5f33-b372-1cf0c0632fea',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('8e1fccf4-9734-5177-919a-188a06701917','3f891e2f-96ff-5ba4-9c8a-4f0bff003ca7','/api/v1/media/8e1fccf4-9734-5177-919a-188a06701917',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('e7d5d97c-2c84-59b9-99a0-53e548561f88','6aa161d4-a889-5e09-9d9d-cf67b32e3504','/api/v1/media/e7d5d97c-2c84-59b9-99a0-53e548561f88',0)
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
VALUES ('6b0b7ead-8b8e-570a-a63c-154664f0fea2','96535f3f-b7c9-5cf8-9bfd-67192c8370f7','/api/v1/media/6b0b7ead-8b8e-570a-a63c-154664f0fea2',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('554332d7-74f2-57f1-9ad3-efd98caa7f47','feaff20c-3540-5e76-9d8c-28b402cae888','/api/v1/media/554332d7-74f2-57f1-9ad3-efd98caa7f47',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('6b999298-e1de-577c-b11b-db838a4df1eb','e5cbe6e0-d62a-53bd-9f7f-514f5ab22364','/api/v1/media/6b999298-e1de-577c-b11b-db838a4df1eb',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('478ad6f7-9470-5694-9208-351db45f8e6c','b2a0daba-5068-5d4e-9359-d68eaba39d42','/api/v1/media/478ad6f7-9470-5694-9208-351db45f8e6c',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('09ad15d8-4bd4-567c-a95e-3967994eefd6','73858888-0fb5-5265-9411-5cf3c34512e3','/api/v1/media/09ad15d8-4bd4-567c-a95e-3967994eefd6',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('8d18bfb5-5f32-5be5-b6f2-d02682c08614','c1e2f896-fc05-5bbf-8115-ea322bab3c6a','/api/v1/media/8d18bfb5-5f32-5be5-b6f2-d02682c08614',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('730d6537-3e42-538a-9548-a43b41dfc5b7','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','/api/v1/media/730d6537-3e42-538a-9548-a43b41dfc5b7',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('d2d73597-c371-5512-a73f-1ad5a9cf1acf','6de8c4ec-e01b-54ed-86ee-e11b7be37ff8','/api/v1/media/d2d73597-c371-5512-a73f-1ad5a9cf1acf',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('708f2dce-4c5c-5475-bb25-826f854178bc','9088c8ea-5385-541d-abf3-e73cbfb75be6','/api/v1/media/708f2dce-4c5c-5475-bb25-826f854178bc',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('de75ba87-e21e-5479-ad71-1ec0ec096e74','b02814c2-7d05-550e-9434-d38a0515fb62','/api/v1/media/de75ba87-e21e-5479-ad71-1ec0ec096e74',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('f79cb85a-023d-5f57-ba48-029a13dbbbed','0ae16bf7-1a3f-5f54-b37b-ff89d5f24820','/api/v1/media/f79cb85a-023d-5f57-ba48-029a13dbbbed',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('0ad9f63d-a48a-5cf1-8e31-6c126d2a2bc3','ced58e12-683c-5563-96a0-d7f86c137384','/api/v1/media/0ad9f63d-a48a-5cf1-8e31-6c126d2a2bc3',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('04e615f9-07ba-57ab-b0c7-2b4af1945cc2','0dda9895-1219-5507-b3ab-8b5729d25a45','/api/v1/media/04e615f9-07ba-57ab-b0c7-2b4af1945cc2',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('ff91d488-ba84-546b-a282-ad72df4185eb','6b6ea528-3994-5ceb-979f-f612a4647206','/api/v1/media/ff91d488-ba84-546b-a282-ad72df4185eb',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('c8643c9f-4989-5427-a059-96dbf5061416','c48e7a4a-7144-50af-8ae8-f206bdc9c9ba','/api/v1/media/c8643c9f-4989-5427-a059-96dbf5061416',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('ed47e608-296c-5560-a393-464db1c730a0','ae6d3922-b42d-5e6c-9a53-9d6bd2b1205d','/api/v1/media/ed47e608-296c-5560-a393-464db1c730a0',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('0d1eaea3-90c6-5657-9698-dc116a413990','10f3a596-9e0d-519d-a02e-402c93f8bc59','/api/v1/media/0d1eaea3-90c6-5657-9698-dc116a413990',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('76cd885f-a767-5e4b-9643-27f1810240bb','60a470c3-d39e-5029-9300-66f5f4d8e10e','/api/v1/media/76cd885f-a767-5e4b-9643-27f1810240bb',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('333a5e93-4732-5933-8f97-21cbbeb0afbc','6ade0f06-e790-547e-ac49-71f2a5e670d2','/api/v1/media/333a5e93-4732-5933-8f97-21cbbeb0afbc',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('befad71a-6c79-50de-8976-0970835e3f75','b47b0628-073e-5483-9670-a9312f0e01a7','/api/v1/media/befad71a-6c79-50de-8976-0970835e3f75',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('4e20a987-9af9-578c-a911-698cf40d5c20','217f40fb-0a92-510f-b9d8-139999735cf2','/api/v1/media/4e20a987-9af9-578c-a911-698cf40d5c20',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('c03055f9-29e6-5128-b267-6abee904cc8d','217f40fb-0a92-510f-b9d8-139999735cf2','/api/v1/media/c03055f9-29e6-5128-b267-6abee904cc8d',1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('596cd1b0-cf72-52aa-954f-675449eaa929','0bafdf33-8eae-546d-ac3c-98239dd858c9','/api/v1/media/596cd1b0-cf72-52aa-954f-675449eaa929',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('b21f7386-f221-5d92-a049-afd2aff73c2c','b70bbd25-46c1-51b4-8a2d-8cbd6638f9b1','/api/v1/media/b21f7386-f221-5d92-a049-afd2aff73c2c',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('febbb4af-ef10-5e79-974f-25b5bb5b118a','2c656d66-1eaa-5d15-aaa5-de5e11a8cb2b','/api/v1/media/febbb4af-ef10-5e79-974f-25b5bb5b118a',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('a06517c1-c488-595a-8871-319bec199c08','fb2ce9d0-4f71-58b0-b9b4-0150d244b71f','/api/v1/media/a06517c1-c488-595a-8871-319bec199c08',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('64887ecc-0b21-57e5-a8e6-3b79233193d8','97722745-ce68-5b3b-8490-ef7a08cf2441','/api/v1/media/64887ecc-0b21-57e5-a8e6-3b79233193d8',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('55247970-fd64-504f-a272-bb0c014f50b4','cdc93861-b55a-5831-8c96-a180654c50e3','/api/v1/media/55247970-fd64-504f-a272-bb0c014f50b4',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('7eacd4ce-d174-5450-bc76-19a69d0ce8b1','b55c32db-c2bc-54bc-9565-b80e029beeb3','/api/v1/media/7eacd4ce-d174-5450-bc76-19a69d0ce8b1',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('2af9c86f-88ee-5e5e-8fe5-a0985c610c0c','7cc04078-48a6-53fe-a42f-07a1912937cf','/api/v1/media/2af9c86f-88ee-5e5e-8fe5-a0985c610c0c',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('ebe97b8f-5309-5760-b19c-e60f97d1ff67','0dd40e8e-cef6-5f60-9075-aafd3a68243a','/api/v1/media/ebe97b8f-5309-5760-b19c-e60f97d1ff67',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('1c81d3b7-358e-52d1-9f27-5ef48f08044a','d9a2ae41-2799-540b-a50f-98088f32e108','/api/v1/media/1c81d3b7-358e-52d1-9f27-5ef48f08044a',0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order)
VALUES ('86b91bd2-c31e-5eed-b4c9-034e8e44c95f','4252061b-6c95-58e4-b6ed-2c76582d2e7a','/api/v1/media/86b91bd2-c31e-5eed-b4c9-034e8e44c95f',0)
ON CONFLICT (id) DO NOTHING;
