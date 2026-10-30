-- 删除旧目录：明湖旧厅名下全部档口及级联依赖（备份后执行，见 backup-canteen.sh）。
-- 东区本轮不上，不动（厅与旧档口原样保留）。
-- 用法：psql "$DB_URL" -v ON_ERROR_STOP=1 -f delete-old-catalog.sql
-- 原则：逐层删，删不干净就报错回滚（单事务），绝不静默跳过。
-- 目标厅：明湖旧厅 '74ed9519-f3e3-5ce8-a89c-a353e2892edd'
BEGIN;

-- 0) 目标集合
CREATE TEMP TABLE target_stalls AS
SELECT id FROM foodtime.stalls
WHERE dining_hall_id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd';
CREATE TEMP TABLE target_dishes AS
SELECT d.id FROM foodtime.dishes d JOIN target_stalls s ON s.id = d.stall_id;
SELECT '待删档口', count(*) FROM target_stalls;
SELECT '待删菜品', count(*) FROM target_dishes;

-- 1) 审核日志（引用投稿/图片，RESTRICT；先清）
DELETE FROM foodtime.content_moderation_logs l
USING foodtime.dish_review_submissions r JOIN target_dishes d ON d.id = r.dish_id
WHERE l.review_submission_id = r.id;
DELETE FROM foodtime.content_moderation_logs l
USING foodtime.dish_submissions s
WHERE (s.stall_id IN (SELECT id FROM target_stalls)
    OR s.approved_dish_id IN (SELECT id FROM target_dishes))
  AND l.dish_submission_id = s.id;
DELETE FROM foodtime.content_moderation_logs l
USING foodtime.stall_submissions s
WHERE (s.dining_hall_id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd'
    OR s.approved_stall_id IN (SELECT id FROM target_stalls))
  AND l.stall_submission_id = s.id;
DELETE FROM foodtime.content_moderation_logs l
USING foodtime.dish_images i JOIN target_dishes d ON d.id = i.dish_id
WHERE l.dish_image_id = i.id;

-- 2) 点评互动（点赞/回复/回复赞，RESTRICT；先清）
DELETE FROM foodtime.dish_review_likes l
USING foodtime.dish_reviews r JOIN target_dishes d ON d.id = r.dish_id
WHERE l.review_id = r.id;
DELETE FROM foodtime.dish_review_reply_likes l
USING foodtime.dish_review_replies r
JOIN foodtime.dish_reviews v ON v.id = r.review_id
JOIN target_dishes d ON d.id = v.dish_id
WHERE l.reply_id = r.id;
DELETE FROM foodtime.dish_review_replies r
USING foodtime.dish_reviews v JOIN target_dishes d ON d.id = v.dish_id
WHERE r.review_id = v.id;
DELETE FROM foodtime.dish_reviews v USING target_dishes d WHERE v.dish_id = d.id;

-- 3) 点评投稿
DELETE FROM foodtime.dish_review_submissions r USING target_dishes d WHERE r.dish_id = d.id;

-- 4) 菜品图片
DELETE FROM foodtime.dish_images i USING target_dishes d WHERE i.dish_id = d.id;

-- 5) 投稿（档口投稿按厅+已批准档口双条件；菜品投稿按档口+已批准菜品双条件）
DELETE FROM foodtime.stall_submissions s
WHERE s.dining_hall_id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd'
   OR s.approved_stall_id IN (SELECT id FROM target_stalls);
DELETE FROM foodtime.dish_submissions s
WHERE s.stall_id IN (SELECT id FROM target_stalls)
   OR s.approved_dish_id IN (SELECT id FROM target_dishes);

-- 6) 菜品（实时评分表是 CASCADE，会自动跟随，无需手动删）
DELETE FROM foodtime.dishes d USING target_stalls s WHERE d.stall_id = s.id;

-- 7) 图集（V24，RESTRICT；V23 旧库尚未建表时跳过）
DO $$
BEGIN
  IF to_regclass('foodtime.stall_images') IS NOT NULL THEN
    DELETE FROM foodtime.stall_images i USING target_stalls s WHERE i.stall_id = s.id;
  END IF;
END $$;

-- 8) 档口
DELETE FROM foodtime.stalls s USING target_stalls t WHERE s.id = t.id;

-- 9) 明湖旧厅行
DELETE FROM foodtime.dining_halls
WHERE id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd';

-- 10) 零残留断言：任一失败即整个事务回滚
DO $$
DECLARE
  leftovers TEXT;
BEGIN
  SELECT string_agg(detail, '; ') INTO leftovers FROM (
    SELECT 'stalls=' || count(*) AS detail FROM foodtime.stalls
      WHERE dining_hall_id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd'
    UNION ALL SELECT 'dishes_of_target=' || count(*) FROM foodtime.dishes d
      WHERE EXISTS (SELECT 1 FROM target_stalls s WHERE s.id = d.stall_id)
    UNION ALL SELECT 'hall_minghu_old=' || count(*) FROM foodtime.dining_halls
      WHERE id = '74ed9519-f3e3-5ce8-a89c-a353e2892edd'
  ) t WHERE split_part(detail, '=', 2) <> '0';
  IF leftovers IS NOT NULL THEN
    RAISE EXCEPTION '旧目录残留，拒绝提交: %', leftovers;
  END IF;
END $$;

COMMIT;
SELECT '旧目录删除完成' AS result;
