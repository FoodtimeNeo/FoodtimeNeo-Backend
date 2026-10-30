-- 合并执行 batch1+batch2 后的最终对账（V24–V28 全部应用、铺图完成后执行）。
-- 用法：docker exec -i foodtime-neo-postgres-1 psql -U foodtime -d foodtime_neo \
--         -v ON_ERROR_STOP=1 -f - < verify-canteen-all.sql
-- 任一项 FAIL 即应停止上线；runner 需 grep FAIL 并返回非零。
-- 两批合并后：9 厅 / 70 档口 / 1421 菜 / 75 张档口图集。

SELECT 'hall_total', CASE WHEN count(*)=9 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dining_halls;

-- 第一批：明湖 3 厅、30 档口、523 菜、32 图
SELECT 'b1_hall_minghu',
  CASE WHEN count(*)=3 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dining_halls WHERE name IN ('明湖餐厅1楼','明湖餐厅2楼','明湖餐厅3楼');
SELECT 'b1_old_minghu_gone', CASE WHEN count(*)=0 THEN 'PASS' ELSE 'FAIL' END
FROM foodtime.dining_halls WHERE id='74ed9519-f3e3-5ce8-a89c-a353e2892edd';
SELECT 'b1_stalls', CASE WHEN count(*)=30 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name IN ('明湖餐厅1楼','明湖餐厅2楼','明湖餐厅3楼') AND s.status='active';
SELECT 'b1_dishes', CASE WHEN count(*)=523 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id=d.stall_id
JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name IN ('明湖餐厅1楼','明湖餐厅2楼','明湖餐厅3楼');
SELECT 'b1_gallery', CASE WHEN count(*)=32 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stall_images i JOIN foodtime.stalls s ON s.id=i.stall_id
JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name IN ('明湖餐厅1楼','明湖餐厅2楼','明湖餐厅3楼');
SELECT 'b1_covers', CASE WHEN count(*)=0 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name IN ('明湖餐厅1楼','明湖餐厅2楼','明湖餐厅3楼')
  AND (s.cover_image_url IS NULL OR s.cover_image_url='');

-- 第二批：学活 2 厅、39 档口、888 菜、43 图
SELECT 'b2_hall_xuehuo',
  CASE WHEN count(*)=2 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dining_halls WHERE name IN ('学活一楼','学活二楼');
SELECT 'b2_old_xuehuo_gone', CASE WHEN count(*)=0 THEN 'PASS' ELSE 'FAIL' END
FROM foodtime.dining_halls WHERE id='315086d7-f339-5d26-a030-b3d7ffe7cde1';
SELECT 'b2_stalls', CASE WHEN count(*)=39 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name IN ('学活一楼','学活二楼','学四食堂','东区学生餐厅') AND s.status='active';
SELECT 'b2_dishes', CASE WHEN count(*)=888 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id=d.stall_id
JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name IN ('学活一楼','学活二楼','学四食堂','东区学生餐厅');
SELECT 'b2_gallery', CASE WHEN count(*)=43 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stall_images i JOIN foodtime.stalls s ON s.id=i.stall_id
JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name IN ('学活一楼','学活二楼','学四食堂','东区学生餐厅');
SELECT 'b2_covers', CASE WHEN count(*)=0 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name IN ('学活一楼','学活二楼','学四食堂','东区学生餐厅')
  AND (s.cover_image_url IS NULL OR s.cover_image_url='');

-- 最终排序
SELECT 'hall_sort_final',
  CASE WHEN string_agg(name||'='||sort_order, ',' ORDER BY sort_order)
    = '留园食堂=0,明湖餐厅1楼=1,明湖餐厅2楼=2,明湖餐厅3楼=3,学活一楼=4,学活二楼=5,学四食堂=6,东区学生餐厅=7,学苑餐厅=8'
    THEN 'PASS' ELSE 'FAIL' END
FROM foodtime.dining_halls;

-- 全局完整性与总量
SELECT 'stall_total', CASE WHEN count(*)=70 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stalls;
SELECT 'dish_total', CASE WHEN count(*)=1421 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dishes;
SELECT 'gallery_total', CASE WHEN count(*)=75 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stall_images;
SELECT 'stall_dup_name', CASE WHEN count(*)=0 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM (SELECT dining_hall_id,name,count(*) FROM foodtime.stalls
      GROUP BY 1,2 HAVING count(*)>1) t;
SELECT 'dish_null_price', CASE WHEN count(*)=0 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dishes WHERE price IS NULL;
SELECT 'dish_zero_price_unexpected', CASE WHEN count(*)=0 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dishes
WHERE price=0
  AND description NOT IN ('按称重计价','免费供应','对应水饺加酸汤需+2元，水饺基础价待核')
  AND name NOT IN ('麻辣香锅（待核价）','酸汤水饺（+2元）');

-- 未受影响的食堂
SELECT 'liuyuan_stalls', CASE WHEN count(*)=1 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name='留园食堂';
SELECT 'liuyuan_dishes', CASE WHEN count(*)=10 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id=d.stall_id
JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name='留园食堂';
SELECT 'xueyuan_stalls', CASE WHEN count(*)=0 THEN 'PASS' ELSE 'FAIL:'||count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
WHERE h.name='学苑餐厅';
