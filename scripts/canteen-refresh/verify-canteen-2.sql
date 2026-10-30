-- 第二批对账：学活一楼/二楼、学四新档口、东区新档口（迁移 V28 应用 + 铺图后执行）。
-- 用法：psql "$DB_URL" -v ON_ERROR_STOP=1 -f verify-canteen-2.sql
-- 任何一项 FAIL 即停；全 PASS 才算上线完成。第一批（明湖）的 verify-canteen.sql 结论保持有效，不重复校验。

-- 1) 新厅 2 个（学活一楼/二楼），老学活厅已删
SELECT 'hall_xuehuo_new',
  CASE WHEN count(*) = 2 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.dining_halls WHERE name IN ('学活一楼', '学活二楼');
SELECT 'hall_xuehuo_old_gone',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM foodtime.dining_halls WHERE id = '315086d7-f339-5d26-a030-b3d7ffe7cde1';

-- 2) 厅排序：留园0/明湖1-3/学活一楼4/学活二楼5/学四6/东区7/学苑8
SELECT 'hall_sort',
  CASE WHEN string_agg(name || '=' || sort_order, ',' ORDER BY sort_order)
    = '留园食堂=0,明湖餐厅1楼=1,明湖餐厅2楼=2,明湖餐厅3楼=3,学活一楼=4,学活二楼=5,学四食堂=6,东区学生餐厅=7,学苑餐厅=8'
    THEN 'PASS' ELSE 'FAIL' END
FROM foodtime.dining_halls;

-- 3) 档口 39 个（学活一楼11/二楼10/学四6/东区12），厅内无重名
SELECT 'stall_count',
  CASE WHEN count(*) = 39 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
WHERE h.name IN ('学活一楼', '学活二楼', '学四食堂', '东区学生餐厅')
  AND s.status = 'active';
SELECT 'stall_dup_name',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM (SELECT dining_hall_id, name, count(*)
      FROM foodtime.stalls GROUP BY 1, 2 HAVING count(*) > 1) t;

-- 4) 菜品 889 个，价格无 NULL；0 元只允许以下 4 个（其余一律 FAIL）
SELECT 'dish_count',
  CASE WHEN count(*) = 889 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id = d.stall_id
JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
WHERE h.name IN ('学活一楼', '学活二楼', '学四食堂', '东区学生餐厅');
SELECT 'dish_null_price',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.dishes WHERE price IS NULL;
SELECT 'dish_zero_price_todo', name, description FROM foodtime.dishes WHERE price = 0;
SELECT 'dish_zero_price_unexpected',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.dishes
WHERE price = 0 AND description NOT IN ('免费供应')
  AND name NOT IN ('牛肉面（汤拌）', '麻辣香锅', '酸汤水饺');

-- 5) 封面全有（新档口），图集 43 张
SELECT 'stall_cover',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
WHERE h.name IN ('学活一楼', '学活二楼', '学四食堂', '东区学生餐厅')
  AND (s.cover_image_url IS NULL OR s.cover_image_url = '');
SELECT 'gallery_count',
  CASE WHEN count(*) = 43 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.stall_images i JOIN foodtime.stalls s ON s.id = i.stall_id
JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
WHERE h.name IN ('学活一楼', '学活二楼', '学四食堂', '东区学生餐厅');

-- 6) 零残留（本批范围）+ 第一批明湖数据仍在
SELECT 'no_old_refs',
  CASE WHEN (SELECT count(*) FROM foodtime.stalls
             WHERE dining_hall_id = '315086d7-f339-5d26-a030-b3d7ffe7cde1') = 0
        AND NOT EXISTS (SELECT 1 FROM foodtime.dining_halls
                        WHERE id = '315086d7-f339-5d26-a030-b3d7ffe7cde1')
        AND (SELECT count(*) FROM foodtime.dining_halls WHERE name = '明湖餐厅1楼') = 1
    THEN 'PASS' ELSE 'FAIL' END;
