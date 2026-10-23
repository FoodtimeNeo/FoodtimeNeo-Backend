CREATE TABLE foodtime.community_post_likes (
 user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
 post_id UUID NOT NULL REFERENCES foodtime.community_posts(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY(user_id,post_id)
);
CREATE TABLE foodtime.community_comment_likes (
 user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
 comment_id UUID NOT NULL REFERENCES foodtime.community_comments(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY(user_id,comment_id)
);
INSERT INTO foodtime.community_post_likes SELECT l.user_id,l.target_id,l.created_at FROM foodtime.community_likes l
 JOIN foodtime.community_posts p ON p.id=l.target_id WHERE l.target_type='post';
INSERT INTO foodtime.community_comment_likes SELECT l.user_id,l.target_id,l.created_at FROM foodtime.community_likes l
 JOIN foodtime.community_comments c ON c.id=l.target_id WHERE l.target_type='comment';
DROP TABLE foodtime.community_likes;
CREATE INDEX ix_community_post_likes_target ON foodtime.community_post_likes(post_id);
CREATE INDEX ix_community_comment_likes_target ON foodtime.community_comment_likes(comment_id);
CREATE INDEX ix_community_favorites_owner ON foodtime.community_favorites(user_id,created_at DESC,post_id DESC);
ALTER TABLE foodtime.community_posts ADD CONSTRAINT ck_community_favorite_count CHECK(favorite_count>=0);
UPDATE foodtime.community_posts p SET like_count=(SELECT count(*) FROM foodtime.community_post_likes l WHERE l.post_id=p.id),
 favorite_count=(SELECT count(*) FROM foodtime.community_favorites f WHERE f.post_id=p.id);
UPDATE foodtime.community_comments c SET like_count=(SELECT count(*) FROM foodtime.community_comment_likes l WHERE l.comment_id=c.id);

CREATE FUNCTION foodtime.guard_community_interaction() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE post UUID;
BEGIN
 IF TG_TABLE_NAME='community_comment_likes' THEN
  SELECT post_id INTO post FROM foodtime.community_comments WHERE id=NEW.comment_id;
 ELSE post=NEW.post_id; END IF;
 PERFORM id FROM foodtime.community_posts WHERE id=post AND status='approved' AND deleted_at IS NULL FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'Interaction post unavailable' USING ERRCODE='23514'; END IF;
 IF TG_TABLE_NAME='community_comment_likes' THEN
  PERFORM id FROM foodtime.community_comments WHERE id=NEW.comment_id AND status='approved' AND deleted_at IS NULL FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Interaction comment unavailable' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END;
$$;
CREATE FUNCTION foodtime.count_community_interaction() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE delta INTEGER; target UUID;
BEGIN
 delta=CASE WHEN TG_OP='INSERT' THEN 1 ELSE -1 END;
 IF TG_TABLE_NAME='community_comment_likes' THEN
  target=CASE WHEN TG_OP='INSERT' THEN NEW.comment_id ELSE OLD.comment_id END;
  UPDATE foodtime.community_comments SET like_count=like_count+delta WHERE id=target;
 ELSIF TG_TABLE_NAME='community_post_likes' THEN
  target=CASE WHEN TG_OP='INSERT' THEN NEW.post_id ELSE OLD.post_id END;
  UPDATE foodtime.community_posts SET like_count=like_count+delta WHERE id=target;
 ELSE
  target=CASE WHEN TG_OP='INSERT' THEN NEW.post_id ELSE OLD.post_id END;
  UPDATE foodtime.community_posts SET favorite_count=favorite_count+delta WHERE id=target;
 END IF;
 RETURN NULL;
END;
$$;
DO $$ DECLARE tab TEXT; BEGIN
 FOREACH tab IN ARRAY ARRAY['community_post_likes','community_comment_likes','community_favorites'] LOOP
  EXECUTE format('CREATE TRIGGER trg_community_interaction_guard BEFORE INSERT ON foodtime.%I FOR EACH ROW EXECUTE FUNCTION foodtime.guard_community_interaction()',tab);
  EXECUTE format('CREATE TRIGGER trg_community_interaction_count AFTER INSERT OR DELETE ON foodtime.%I FOR EACH ROW EXECUTE FUNCTION foodtime.count_community_interaction()',tab);
 END LOOP;
END; $$;
