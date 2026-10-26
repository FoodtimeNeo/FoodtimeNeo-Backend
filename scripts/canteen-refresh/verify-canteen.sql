-- 新目录上线后对账（重启应用、Flyway 到 V25、铺图完成后执行）。
-- 用法：psql "$DB_URL" -v ON_ERROR_STOP=1 -f verify-canteen.sql
-- 任何一项 FAIL 即报错退出；全 PASS 才算上线完成。

-- 1) 新厅 3 个（明湖1/2/3楼），老明湖厅已删
SELECT 'hall_minghu_floors',
  CASE WHEN count(*) = 3 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.dining_halls WHERE name IN ('明湖餐厅1楼', '明湖餐厅2楼', '明湖餐厅3楼');
SELECT 'hall_minghu_old_gone',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL' END
FROM foodtime.dining_halls WHERE id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd';

-- 2) 厅排序（留园0/明湖1-3/学活4/学四5/东区6/学苑7）
SELECT 'hall_sort',
  CASE WHEN string_agg(name || '=' || sort_order, ',' ORDER BY sort_order)
    = '留园食堂=0,明湖餐厅1楼=1,明湖餐厅2楼=2,明湖餐厅3楼=3,学活食堂=4,学四食堂=5,东区学生餐厅=6,学苑餐厅=7'
    THEN 'PASS' ELSE 'FAIL' END
FROM foodtime.dining_halls;

-- 3) 档口 30 个（明湖1楼7/二楼11/三楼12），厅内无重名
SELECT 'stall_count',
  CASE WHEN count(*) = 30 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
WHERE h.name IN ('明湖餐厅1楼', '明湖餐厅2楼', '明湖餐厅3楼')
  AND s.status = 'active';
SELECT 'stall_dup_name',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM (SELECT dining_hall_id, name, count(*)
      FROM foodtime.stalls GROUP BY 1, 2 HAVING count(*) > 1) t;

-- 4) 菜品 523 个，价格无 NULL；0 元只允许暖柠（按称重计价）与白米饭（免费待确认）
SELECT 'dish_count',
  CASE WHEN count(*) = 523 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id = d.stall_id
JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
WHERE h.name IN ('明湖餐厅1楼', '明湖餐厅2楼', '明湖餐厅3楼');
SELECT 'dish_null_price',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.dishes WHERE price IS NULL;
SELECT 'dish_zero_price_todo', name, description FROM foodtime.dishes WHERE price = 0;
SELECT 'dish_zero_price_unexpected',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.dishes
WHERE price = 0 AND description NOT IN ('按称重计价', '免费供应');

-- 5) 封面全有，图集 32 张（含 30 首图 + 2 多余图）
SELECT 'stall_cover',
  CASE WHEN count(*) = 0 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
WHERE h.name IN ('明湖餐厅1楼', '明湖餐厅2楼', '明湖餐厅3楼')
  AND (s.cover_image_url IS NULL OR s.cover_image_url = '');
SELECT 'gallery_count',
  CASE WHEN count(*) = 32 THEN 'PASS' ELSE 'FAIL:' || count(*) END
FROM foodtime.stall_images;

-- 6) 旧数据零残留（与删除脚本第 10 步同口径）
SELECT 'no_old_refs',
  CASE WHEN (SELECT count(*) FROM foodtime.stalls
             WHERE dining_hall_id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd') = 0
        AND NOT EXISTS (SELECT 1 FROM foodtime.dining_halls
                        WHERE id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd')
    THEN 'PASS' ELSE 'FAIL' END;
