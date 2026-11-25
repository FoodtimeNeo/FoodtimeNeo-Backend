-- 为北交主校区及东校区 11 个启用食堂预置经 OpenStreetMap 核验的 WGS84 经纬度坐标。
-- 仅在当前经纬度均为空时回填，避免覆盖管理员后续在后台手动微调的坐标。

-- 留园食堂（主校区留园餐厅楼）
UPDATE foodtime.dining_halls
SET latitude = 39.9488990, longitude = 116.3374050, revision = revision + 1, updated_at = now()
WHERE id = '68166b66-14d1-58c5-960d-572228254c20' AND latitude IS NULL AND longitude IS NULL;

-- 明湖食堂 / 明湖餐厅 1–3 楼（主校区明湖餐厅 / 学生一食堂）
UPDATE foodtime.dining_halls
SET latitude = 39.9518280, longitude = 116.3371990, revision = revision + 1, updated_at = now()
WHERE id IN (
    '74ed9519-f3e3-5ce8-a89c-a353e2892edd',
    'f158d27a-329c-57ec-978a-22ed9e6a447b',
    'bb05ca8f-2544-5e2a-a7e1-151a8b3e0e8f',
    'd4fb48b3-a92d-5e93-a4f3-4ab3f210118d'
) AND latitude IS NULL AND longitude IS NULL;

-- 学活食堂 / 学活一楼 / 学活二楼（主校区学生活动中心）
UPDATE foodtime.dining_halls
SET latitude = 39.9493190, longitude = 116.3318620, revision = revision + 1, updated_at = now()
WHERE id IN (
    '315086d7-f339-5d26-a030-b3d7ffe7cde1',
    'af829558-1410-503a-a426-e1d7ba2a399d',
    '9bc79377-bb58-54ff-8b51-4cfb67411d37'
) AND latitude IS NULL AND longitude IS NULL;

-- 学四食堂（主校区学生第四食堂）
UPDATE foodtime.dining_halls
SET latitude = 39.9483300, longitude = 116.3382080, revision = revision + 1, updated_at = now()
WHERE id = '94f125bc-e647-5e4e-a946-cde15f54cbdc' AND latitude IS NULL AND longitude IS NULL;

-- 东区学生餐厅（东校区学生食堂）
UPDATE foodtime.dining_halls
SET latitude = 39.9471200, longitude = 116.3399810, revision = revision + 1, updated_at = now()
WHERE id = '955c1f92-3671-5d13-8566-81911b5a5a7b' AND latitude IS NULL AND longitude IS NULL;

-- 学苑餐厅（主校区东北角学苑公寓区）
UPDATE foodtime.dining_halls
SET latitude = 39.9531990, longitude = 116.3416620, revision = revision + 1, updated_at = now()
WHERE id = '78fa7f63-f52f-524f-9b28-f55fb441a1f0' AND latitude IS NULL AND longitude IS NULL;
