-- 删除同档口、同名、同价、同简介的重复菜品，每组保留一行。
-- 14 组重名菜中有 3 组价格不同（东区酸菜肉丝面、学活二楼小炒肉面/麻辣鸡丝面），
-- 需人工核价，本迁移保留不删；在已手工去重的库上为 no-op。
WITH identical AS (
    SELECT stall_id, name
    FROM foodtime.dishes
    GROUP BY stall_id, name
    HAVING count(*) > 1
       AND count(DISTINCT price) = 1
       AND count(DISTINCT coalesce(description, '')) = 1
), ranked AS (
    SELECT d.id,
           row_number() OVER (PARTITION BY d.stall_id, d.name ORDER BY d.id) AS rn
    FROM foodtime.dishes d
    JOIN identical i ON i.stall_id = d.stall_id AND i.name = d.name
)
DELETE FROM foodtime.dishes d
USING ranked r
WHERE d.id = r.id AND r.rn > 1;
