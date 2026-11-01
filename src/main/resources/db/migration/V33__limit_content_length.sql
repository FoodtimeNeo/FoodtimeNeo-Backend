-- Align storage-level limits with validation so a bypassed application layer cannot persist oversized text.
--
-- Pre-flight (run before deploying; every count must be 0, otherwise this migration aborts below):
--   SELECT 'review_submissions' t, count(*) FROM foodtime.dish_review_submissions    WHERE char_length(content)     > 500
--   UNION ALL SELECT 'reviews',    count(*) FROM foodtime.dish_reviews               WHERE char_length(content)     > 500
--   UNION ALL SELECT 'dish_sub',   count(*) FROM foodtime.dish_submissions           WHERE char_length(description) > 500
--   UNION ALL SELECT 'stall_sub',  count(*) FROM foodtime.stall_submissions          WHERE char_length(description) > 500
--   UNION ALL SELECT 'halls',      count(*) FROM foodtime.dining_halls               WHERE char_length(description) > 500
--   UNION ALL SELECT 'stalls',     count(*) FROM foodtime.stalls                     WHERE char_length(description) > 500
--   UNION ALL SELECT 'dishes',     count(*) FROM foodtime.dishes                     WHERE char_length(description) > 500;
--
-- If any row exists, shorten or delete it first: ADD CONSTRAINT validates existing rows and a failure
-- leaves the schema partially migrated, which blocks every later startup until fixed by hand.

DO $$
DECLARE
    offending bigint;
BEGIN
    SELECT count(*) INTO offending FROM (
        SELECT id FROM foodtime.dish_review_submissions WHERE char_length(content)     > 500
        UNION ALL SELECT id FROM foodtime.dish_reviews            WHERE char_length(content)     > 500
        UNION ALL SELECT id FROM foodtime.dish_submissions        WHERE char_length(description) > 500
        UNION ALL SELECT id FROM foodtime.stall_submissions       WHERE char_length(description) > 500
        UNION ALL SELECT id FROM foodtime.dining_halls            WHERE char_length(description) > 500
        UNION ALL SELECT id FROM foodtime.stalls                  WHERE char_length(description) > 500
        UNION ALL SELECT id FROM foodtime.dishes                  WHERE char_length(description) > 500
    ) oversized;
    IF offending > 0 THEN
        RAISE EXCEPTION 'V33 aborted: % existing rows exceed 500 characters; shorten them before migrating (see the query above)', offending
            USING HINT = 'Run the pre-flight SELECT at the top of V33__limit_content_length.sql to list them.';
    END IF;
END
$$;

ALTER TABLE foodtime.dish_review_submissions
    ADD CONSTRAINT ck_review_submissions_content_length CHECK (char_length(content) <= 500);

ALTER TABLE foodtime.dish_reviews
    ADD CONSTRAINT ck_dish_reviews_content_length CHECK (char_length(content) <= 500);

ALTER TABLE foodtime.dish_submissions
    ADD CONSTRAINT ck_dish_submissions_description_length CHECK (char_length(description) <= 500);

ALTER TABLE foodtime.stall_submissions
    ADD CONSTRAINT ck_stall_submissions_description_length CHECK (char_length(description) <= 500);

ALTER TABLE foodtime.dining_halls
    ADD CONSTRAINT ck_dining_halls_description_length CHECK (char_length(description) <= 500);

ALTER TABLE foodtime.stalls
    ADD CONSTRAINT ck_stalls_description_length CHECK (char_length(description) <= 500);

ALTER TABLE foodtime.dishes
    ADD CONSTRAINT ck_dishes_description_length CHECK (char_length(description) <= 500);

COMMENT ON CONSTRAINT ck_review_submissions_content_length ON foodtime.dish_review_submissions
    IS '评论正文上限500字，与 SubmitReviewRequest 校验一致';
COMMENT ON CONSTRAINT ck_dish_reviews_content_length ON foodtime.dish_reviews
    IS '正式评论正文上限500字，与提交表一致';
