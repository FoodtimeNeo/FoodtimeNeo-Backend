-- 点赞/收藏以增删表达意图，禁止 UPDATE 绕过计数触发器。
CREATE FUNCTION foodtime.reject_community_interaction_update() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Community interactions are immutable; use INSERT or DELETE' USING ERRCODE='23514';
END; $$;
CREATE TRIGGER trg_community_post_likes_immutable BEFORE UPDATE ON foodtime.community_post_likes
 FOR EACH ROW EXECUTE FUNCTION foodtime.reject_community_interaction_update();
CREATE TRIGGER trg_community_comment_likes_immutable BEFORE UPDATE ON foodtime.community_comment_likes
 FOR EACH ROW EXECUTE FUNCTION foodtime.reject_community_interaction_update();
CREATE TRIGGER trg_community_favorites_immutable BEFORE UPDATE ON foodtime.community_favorites
 FOR EACH ROW EXECUTE FUNCTION foodtime.reject_community_interaction_update();
