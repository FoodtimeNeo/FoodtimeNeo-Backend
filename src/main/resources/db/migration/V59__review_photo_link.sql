-- Images remain independently moderated; the link identifies the review version.
ALTER TABLE foodtime.dish_images ADD COLUMN review_submission_id UUID
    REFERENCES foodtime.dish_review_submissions(id) ON DELETE RESTRICT;
CREATE UNIQUE INDEX uq_dish_images_review_submission
    ON foodtime.dish_images(review_submission_id) WHERE review_submission_id IS NOT NULL;
ALTER TABLE foodtime.dish_images DROP CONSTRAINT ck_dish_images_ai_rejection_only;
ALTER TABLE foodtime.dish_images ADD CONSTRAINT ck_dish_images_ai_decision CHECK (
    reviewed_by_ai_model IS NULL OR
    (reviewed_by_user_id IS NULL AND reviewed_at IS NOT NULL AND
       (status='rejected' OR (status='approved' AND review_submission_id IS NOT NULL)))
);
