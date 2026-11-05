-- Restaurant/stall reviews: immutable submissions, stable publication identity and five rating buckets.
CREATE TABLE foodtime.venue_review_targets (
 id UUID PRIMARY KEY, dining_hall_id UUID UNIQUE REFERENCES foodtime.dining_halls(id) ON DELETE CASCADE,
 stall_id UUID UNIQUE REFERENCES foodtime.stalls(id) ON DELETE CASCADE, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK(num_nonnulls(dining_hall_id,stall_id)=1)
);
CREATE TABLE foodtime.venue_reviews (
 id UUID PRIMARY KEY, target_id UUID NOT NULL REFERENCES foodtime.venue_review_targets(id),
 user_id UUID NOT NULL REFERENCES foodtime.users(id), current_submission_id UUID,
 publication_version BIGINT NOT NULL DEFAULT 0 CHECK(publication_version>=0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), first_published_at TIMESTAMPTZ,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_venue_reviews_owner ON foodtime.venue_reviews(target_id,user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_venue_reviews_public ON foodtime.venue_reviews(target_id,first_published_at DESC,id DESC)
 WHERE deleted_at IS NULL AND current_submission_id IS NOT NULL;
CREATE INDEX ix_venue_reviews_owner ON foodtime.venue_reviews(user_id,created_at DESC,id DESC);
CREATE TABLE foodtime.venue_review_submissions (
 id UUID PRIMARY KEY, review_id UUID NOT NULL REFERENCES foodtime.venue_reviews(id),
 client_request_id UUID NOT NULL UNIQUE, base_version BIGINT NOT NULL CHECK(base_version>=0),
 rating SMALLINT NOT NULL CHECK(rating BETWEEN 1 AND 5), content TEXT NOT NULL CHECK(char_length(btrim(content)) BETWEEN 1 AND 500),
 status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','approved','rejected')),
 submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(), reviewed_by_user_id UUID REFERENCES foodtime.users(id),
 reviewed_by_ai_model TEXT, reviewed_at TIMESTAMPTZ, review_reason TEXT CHECK(char_length(review_reason)<=500), invalidated_at TIMESTAMPTZ,
 UNIQUE(review_id,id), CHECK(invalidated_at IS NULL OR status='pending'),
 CHECK((status='pending' AND reviewed_at IS NULL AND num_nonnulls(reviewed_by_user_id,reviewed_by_ai_model)=0)
  OR (status<>'pending' AND reviewed_at IS NOT NULL AND num_nonnulls(reviewed_by_user_id,reviewed_by_ai_model)=1)),
 CHECK(status<>'rejected' OR (review_reason IS NOT NULL AND char_length(btrim(review_reason))>0))
);
ALTER TABLE foodtime.venue_reviews ADD CONSTRAINT fk_venue_review_current FOREIGN KEY(id,current_submission_id)
 REFERENCES foodtime.venue_review_submissions(review_id,id) DEFERRABLE INITIALLY DEFERRED;
CREATE UNIQUE INDEX ux_venue_submission_pending ON foodtime.venue_review_submissions(review_id) WHERE status='pending' AND invalidated_at IS NULL;
CREATE INDEX ix_venue_submission_history ON foodtime.venue_review_submissions(review_id,submitted_at DESC,id DESC);
CREATE INDEX ix_venue_submission_pending ON foodtime.venue_review_submissions(submitted_at,id) WHERE status='pending' AND invalidated_at IS NULL;
CREATE INDEX ix_venue_submission_reviewer ON foodtime.venue_review_submissions(reviewed_by_user_id) WHERE reviewed_by_user_id IS NOT NULL;
CREATE TABLE foodtime.venue_rating_stats (
 target_id UUID NOT NULL REFERENCES foodtime.venue_review_targets(id) ON DELETE CASCADE,rating SMALLINT NOT NULL CHECK(rating BETWEEN 1 AND 5),
 review_count BIGINT NOT NULL DEFAULT 0 CHECK(review_count>=0),updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),PRIMARY KEY(target_id,rating)
);
CREATE FUNCTION foodtime.initialize_venue_target() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE target UUID;
BEGIN
 target:=gen_random_uuid();
 IF TG_TABLE_NAME='dining_halls' THEN INSERT INTO foodtime.venue_review_targets(id,dining_hall_id) VALUES(target,NEW.id);
 ELSE INSERT INTO foodtime.venue_review_targets(id,stall_id) VALUES(target,NEW.id); END IF;
 INSERT INTO foodtime.venue_rating_stats(target_id,rating) SELECT target,generate_series(1,5);RETURN NEW;
END;$$;
INSERT INTO foodtime.venue_review_targets(id,dining_hall_id) SELECT gen_random_uuid(),id FROM foodtime.dining_halls;
INSERT INTO foodtime.venue_review_targets(id,stall_id) SELECT gen_random_uuid(),id FROM foodtime.stalls;
INSERT INTO foodtime.venue_rating_stats(target_id,rating) SELECT id,generate_series(1,5) FROM foodtime.venue_review_targets;
CREATE TRIGGER trg_hall_review_target AFTER INSERT ON foodtime.dining_halls FOR EACH ROW EXECUTE FUNCTION foodtime.initialize_venue_target();
CREATE TRIGGER trg_stall_review_target AFTER INSERT ON foodtime.stalls FOR EACH ROW EXECUTE FUNCTION foodtime.initialize_venue_target();
CREATE VIEW foodtime.venue_rating_summary AS
 SELECT target_id,SUM(review_count)::BIGINT AS review_count,
 ROUND(SUM(rating::NUMERIC*review_count)/NULLIF(SUM(review_count),0),2) AS average_score
 FROM foodtime.venue_rating_stats GROUP BY target_id;

CREATE TABLE foodtime.venue_review_stats (
 review_id UUID PRIMARY KEY REFERENCES foodtime.venue_reviews(id),like_count BIGINT NOT NULL DEFAULT 0 CHECK(like_count>=0),
 reply_count BIGINT NOT NULL DEFAULT 0 CHECK(reply_count>=0),counter_version BIGINT NOT NULL DEFAULT 0 CHECK(counter_version>=0),updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE foodtime.venue_review_likes (
 review_id UUID NOT NULL REFERENCES foodtime.venue_reviews(id),user_id UUID NOT NULL REFERENCES foodtime.users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),PRIMARY KEY(review_id,user_id)
);
CREATE INDEX ix_venue_review_likes_user ON foodtime.venue_review_likes(user_id);
CREATE TABLE foodtime.venue_review_replies (
 id UUID PRIMARY KEY,review_id UUID NOT NULL REFERENCES foodtime.venue_reviews(id),reply_to_id UUID,
 user_id UUID NOT NULL REFERENCES foodtime.users(id),client_request_id UUID NOT NULL,
 content TEXT NOT NULL CHECK(char_length(btrim(content)) BETWEEN 1 AND 500),
 status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','approved','rejected')),
 reviewed_by_user_id UUID REFERENCES foodtime.users(id),reviewed_by_ai_model TEXT,reviewed_at TIMESTAMPTZ,
 review_reason TEXT CHECK(char_length(review_reason)<=500),created_at TIMESTAMPTZ NOT NULL DEFAULT now(),updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),deleted_at TIMESTAMPTZ,
 UNIQUE(user_id,client_request_id),UNIQUE(review_id,id),
 FOREIGN KEY(review_id,reply_to_id) REFERENCES foodtime.venue_review_replies(review_id,id), CHECK(reply_to_id IS NULL OR reply_to_id<>id),
 CHECK((status='pending' AND reviewed_at IS NULL AND num_nonnulls(reviewed_by_user_id,reviewed_by_ai_model)=0)
 OR(status<>'pending' AND reviewed_at IS NOT NULL AND num_nonnulls(reviewed_by_user_id,reviewed_by_ai_model)=1)),
 CHECK(status<>'rejected' OR (review_reason IS NOT NULL AND char_length(btrim(review_reason))>0))
);
CREATE INDEX ix_venue_reply_public ON foodtime.venue_review_replies(review_id,created_at,id) WHERE status='approved' AND deleted_at IS NULL;
CREATE INDEX ix_venue_reply_private ON foodtime.venue_review_replies(review_id,user_id,created_at,id) WHERE deleted_at IS NULL;
CREATE INDEX ix_venue_reply_owner ON foodtime.venue_review_replies(user_id,created_at DESC,id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_venue_reply_pending ON foodtime.venue_review_replies(created_at,id) WHERE status='pending' AND deleted_at IS NULL;
CREATE INDEX ix_venue_reply_target ON foodtime.venue_review_replies(review_id,reply_to_id) WHERE reply_to_id IS NOT NULL;
CREATE INDEX ix_venue_reply_reviewer ON foodtime.venue_review_replies(reviewed_by_user_id) WHERE reviewed_by_user_id IS NOT NULL;
CREATE TABLE foodtime.venue_review_reply_stats (
 reply_id UUID PRIMARY KEY REFERENCES foodtime.venue_review_replies(id),like_count BIGINT NOT NULL DEFAULT 0 CHECK(like_count>=0),
 counter_version BIGINT NOT NULL DEFAULT 0 CHECK(counter_version>=0),updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE foodtime.venue_review_reply_likes (
 reply_id UUID NOT NULL REFERENCES foodtime.venue_review_replies(id),user_id UUID NOT NULL REFERENCES foodtime.users(id),created_at TIMESTAMPTZ NOT NULL DEFAULT now(),PRIMARY KEY(reply_id,user_id)
);
CREATE INDEX ix_venue_reply_likes_user ON foodtime.venue_review_reply_likes(user_id);

CREATE FUNCTION foodtime.guard_venue_submission() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(NEW.id,NEW.review_id,NEW.client_request_id,NEW.base_version,NEW.rating,NEW.content,NEW.submitted_at)
  IS DISTINCT FROM ROW(OLD.id,OLD.review_id,OLD.client_request_id,OLD.base_version,OLD.rating,OLD.content,OLD.submitted_at)
  OR (OLD.status<>'pending' AND NEW IS DISTINCT FROM OLD)
  OR (OLD.invalidated_at IS NOT NULL AND NEW IS DISTINCT FROM OLD)
 THEN RAISE EXCEPTION 'Immutable review submission' USING ERRCODE='23514';END IF;RETURN NEW;
END;$$;
CREATE TRIGGER trg_venue_submission_guard BEFORE UPDATE ON foodtime.venue_review_submissions FOR EACH ROW EXECUTE FUNCTION foodtime.guard_venue_submission();
CREATE FUNCTION foodtime.guard_venue_review() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(NEW.id,NEW.target_id,NEW.user_id,NEW.created_at) IS DISTINCT FROM ROW(OLD.id,OLD.target_id,OLD.user_id,OLD.created_at)
 OR (OLD.deleted_at IS NOT NULL AND NEW IS DISTINCT FROM OLD)
 OR (OLD.first_published_at IS NOT NULL AND NEW.first_published_at IS DISTINCT FROM OLD.first_published_at)
 THEN RAISE EXCEPTION 'Immutable review identity' USING ERRCODE='23514';END IF;
 IF NEW.current_submission_id IS DISTINCT FROM OLD.current_submission_id THEN
  IF NOT EXISTS(SELECT 1 FROM foodtime.venue_review_submissions s WHERE s.id=NEW.current_submission_id AND s.review_id=NEW.id
    AND s.status='approved' AND s.invalidated_at IS NULL AND s.base_version=OLD.publication_version)
  THEN RAISE EXCEPTION 'Invalid publication version' USING ERRCODE='23514';END IF;
  NEW.first_published_at:=COALESCE(OLD.first_published_at,statement_timestamp());
 END IF;
 IF NEW.current_submission_id IS DISTINCT FROM OLD.current_submission_id OR NEW.deleted_at IS DISTINCT FROM OLD.deleted_at THEN
  NEW.publication_version:=OLD.publication_version+1;NEW.updated_at:=statement_timestamp();
 END IF;RETURN NEW;
END;$$;
CREATE TRIGGER trg_venue_review_guard BEFORE UPDATE ON foodtime.venue_reviews FOR EACH ROW EXECUTE FUNCTION foodtime.guard_venue_review();
CREATE FUNCTION foodtime.adjust_venue_rating() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE old_rating INT;new_rating INT;bucket INT;delta INT;
BEGIN
 IF TG_OP='INSERT' THEN INSERT INTO foodtime.venue_review_stats(review_id) VALUES(NEW.id);RETURN NEW;END IF;
 IF OLD.deleted_at IS NULL THEN SELECT rating INTO old_rating FROM foodtime.venue_review_submissions WHERE id=OLD.current_submission_id;END IF;
 IF TG_OP<>'DELETE' AND NEW.deleted_at IS NULL THEN SELECT rating INTO new_rating FROM foodtime.venue_review_submissions WHERE id=NEW.current_submission_id;END IF;
 FOR bucket IN 1..5 LOOP
  delta:=(CASE WHEN new_rating=bucket THEN 1 ELSE 0 END)-(CASE WHEN old_rating=bucket THEN 1 ELSE 0 END);
  IF delta<>0 THEN UPDATE foodtime.venue_rating_stats SET review_count=review_count+delta,updated_at=statement_timestamp() WHERE target_id=OLD.target_id AND rating=bucket;
   IF NOT FOUND THEN RAISE EXCEPTION 'Missing venue bucket' USING ERRCODE='23503';END IF;
  END IF;
 END LOOP;IF TG_OP='DELETE' THEN RETURN OLD;END IF;RETURN NEW;
END;$$;
CREATE TRIGGER trg_venue_rating AFTER INSERT OR UPDATE OR DELETE ON foodtime.venue_reviews FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_venue_rating();
CREATE FUNCTION foodtime.guard_venue_reply() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(NEW.id,NEW.review_id,NEW.reply_to_id,NEW.user_id,NEW.client_request_id,NEW.content,NEW.created_at)
 IS DISTINCT FROM ROW(OLD.id,OLD.review_id,OLD.reply_to_id,OLD.user_id,OLD.client_request_id,OLD.content,OLD.created_at)
 OR (OLD.status<>'pending' AND NEW.status<>OLD.status) OR (OLD.deleted_at IS NOT NULL AND NEW IS DISTINCT FROM OLD)
 THEN RAISE EXCEPTION 'Immutable venue reply' USING ERRCODE='23514';END IF;NEW.updated_at:=statement_timestamp();RETURN NEW;
END;$$;
CREATE TRIGGER trg_venue_reply_guard BEFORE UPDATE ON foodtime.venue_review_replies FOR EACH ROW EXECUTE FUNCTION foodtime.guard_venue_reply();
CREATE FUNCTION foodtime.adjust_venue_reply_count() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE old_count INT:=0;new_count INT:=0;root UUID;
BEGIN
 IF TG_OP<>'INSERT' THEN root:=OLD.review_id;IF OLD.status='approved' AND OLD.deleted_at IS NULL THEN old_count:=1;END IF;END IF;
 IF TG_OP<>'DELETE' THEN root:=NEW.review_id;IF NEW.status='approved' AND NEW.deleted_at IS NULL THEN new_count:=1;END IF;END IF;
 IF TG_OP='INSERT' THEN INSERT INTO foodtime.venue_review_reply_stats(reply_id) VALUES(NEW.id);END IF;
 IF new_count<>old_count THEN UPDATE foodtime.venue_review_stats SET reply_count=reply_count+new_count-old_count,counter_version=counter_version+1,updated_at=statement_timestamp() WHERE review_id=root;END IF;
 IF TG_OP='DELETE' THEN RETURN OLD;END IF;RETURN NEW;
END;$$;
CREATE TRIGGER trg_venue_reply_count AFTER INSERT OR UPDATE OR DELETE ON foodtime.venue_review_replies FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_venue_reply_count();
CREATE FUNCTION foodtime.adjust_venue_likes() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE key UUID;delta INT;
BEGIN
 delta:=CASE WHEN TG_OP='INSERT' THEN 1 ELSE -1 END;
 IF TG_TABLE_NAME='venue_review_likes' THEN
  IF TG_OP='INSERT' THEN key:=NEW.review_id;ELSE key:=OLD.review_id;END IF;
  UPDATE foodtime.venue_review_stats SET like_count=like_count+delta,counter_version=counter_version+1,updated_at=statement_timestamp() WHERE review_id=key;
 ELSE
  IF TG_OP='INSERT' THEN key:=NEW.reply_id;ELSE key:=OLD.reply_id;END IF;
  UPDATE foodtime.venue_review_reply_stats SET like_count=like_count+delta,counter_version=counter_version+1,updated_at=statement_timestamp() WHERE reply_id=key;
 END IF;IF TG_OP='DELETE' THEN RETURN OLD;END IF;RETURN NEW;
END;$$;
CREATE TRIGGER trg_venue_review_likes AFTER INSERT OR DELETE ON foodtime.venue_review_likes FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_venue_likes();
CREATE TRIGGER trg_venue_reply_likes AFTER INSERT OR DELETE ON foodtime.venue_review_reply_likes FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_venue_likes();
ALTER TABLE foodtime.content_moderation_logs ADD COLUMN venue_review_submission_id UUID REFERENCES foodtime.venue_review_submissions(id),
 ADD COLUMN venue_review_reply_id UUID REFERENCES foodtime.venue_review_replies(id);
ALTER TABLE foodtime.content_moderation_logs DROP CONSTRAINT ck_moderation_logs_one_submission;
ALTER TABLE foodtime.content_moderation_logs ADD CONSTRAINT ck_moderation_logs_one_submission CHECK(num_nonnulls(
 review_submission_id,dish_submission_id,stall_submission_id,dish_image_id,community_post_id,community_comment_id,dish_review_reply_id,venue_review_submission_id,venue_review_reply_id)=1);
CREATE INDEX ix_logs_venue_submission ON foodtime.content_moderation_logs(venue_review_submission_id) WHERE venue_review_submission_id IS NOT NULL;
CREATE INDEX ix_logs_venue_reply ON foodtime.content_moderation_logs(venue_review_reply_id) WHERE venue_review_reply_id IS NOT NULL;
ALTER TABLE foodtime.ai_moderation_results DROP CONSTRAINT ai_moderation_results_submission_type_check;
ALTER TABLE foodtime.ai_moderation_results ADD CONSTRAINT ai_moderation_results_submission_type_check CHECK(submission_type IN (
 'review','photo','dish','community-post','community-image','community-comment','dish-review-reply',
 'hall-review','stall-review','hall-review-reply','stall-review-reply'));

ALTER TABLE foodtime.content_moderation_logs DROP CONSTRAINT ck_moderation_logs_action;
ALTER TABLE foodtime.content_moderation_logs ADD CONSTRAINT ck_moderation_logs_action CHECK(action IN ('approve','reject','remove'));

CREATE FUNCTION foodtime.guard_venue_target() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN IF NEW IS DISTINCT FROM OLD THEN RAISE EXCEPTION 'Immutable review target' USING ERRCODE='23514';END IF;RETURN NEW;END;$$;
CREATE TRIGGER trg_venue_target_guard BEFORE UPDATE ON foodtime.venue_review_targets FOR EACH ROW EXECUTE FUNCTION foodtime.guard_venue_target();
CREATE FUNCTION foodtime.guard_venue_like() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Like rows are immutable' USING ERRCODE='23514';END;$$;
CREATE TRIGGER trg_venue_like_guard BEFORE UPDATE ON foodtime.venue_review_likes FOR EACH ROW EXECUTE FUNCTION foodtime.guard_venue_like();
CREATE TRIGGER trg_venue_reply_like_guard BEFORE UPDATE ON foodtime.venue_review_reply_likes FOR EACH ROW EXECUTE FUNCTION foodtime.guard_venue_like();
