-- 食堂封面：留园/明湖/学活/学四挂封面；新建东区学生餐厅/学苑餐厅（仅封面，档口待导）。
-- 图片文件随本分支 seed-images/v6/ 提交；各环境执行
-- scripts/place-seed-images.ps1 将其铺入应用 uploads/ 目录后方可读取。

UPDATE foodtime.dining_halls
SET cover_image_url='/api/v1/media/0a3c0e9a-7405-585e-94ce-29d9275b1b6e', updated_at=now() WHERE id='68166b66-14d1-58c5-960d-572228254c20';
UPDATE foodtime.dining_halls
SET cover_image_url='/api/v1/media/fdfb8516-8db8-5ca7-bb08-6ac9b9fe6b18', updated_at=now() WHERE id='74ed9519-f3e3-5ce8-a89c-a353e2892edd';
UPDATE foodtime.dining_halls
SET cover_image_url='/api/v1/media/4e78a9a3-beb9-5035-aef0-c2ac97f9f522', updated_at=now() WHERE id='94f125bc-e647-5e4e-a946-cde15f54cbdc';
UPDATE foodtime.dining_halls
SET cover_image_url='/api/v1/media/bfb6d4cc-05ec-5868-a697-12d2e8334780', updated_at=now() WHERE id='315086d7-f339-5d26-a030-b3d7ffe7cde1';
INSERT INTO foodtime.dining_halls(id,name,description,status,cover_image_url,sort_order)
VALUES ('955c1f92-3671-5d13-8566-81911b5a5a7b','东区学生餐厅',NULL,'active','/api/v1/media/5f6c20b4-432f-5616-a199-506d40634a8a',4)
ON CONFLICT DO NOTHING;
INSERT INTO foodtime.dining_halls(id,name,description,status,cover_image_url,sort_order)
VALUES ('78fa7f63-f52f-524f-9b28-f55fb441a1f0','学苑餐厅',NULL,'active','/api/v1/media/8f138021-affb-5403-bdba-73739c4c1dae',5)
ON CONFLICT DO NOTHING;

