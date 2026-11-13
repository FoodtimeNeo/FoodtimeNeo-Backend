package com.FoodtimeNeo.community.edit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/** Internal version evidence is never projected into public post responses. */
@Repository
public class PostEditStore {
    public record Target(UUID author,UUID repost,long revision) { }
    public record Receipt(String hash,long revision,boolean changed) { }
    private final JdbcTemplate jdbc;
    public PostEditStore(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public Target lock(UUID id) {
        return jdbc.query("SELECT user_id,repost_of_id,content_revision FROM foodtime.community_posts WHERE id=? AND deleted_at IS NULL FOR UPDATE",
                (r,n)->new Target(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3)),id).stream().findFirst().orElse(null);
    }
    public Receipt requested(UUID id,UUID request) {
        return jdbc.query("SELECT payload_hash,resulting_revision,changed FROM foodtime.community_post_edit_requests WHERE post_id=? AND client_request_id=?",
                (r,n)->new Receipt(r.getString(1),r.getLong(2),r.getBoolean(3)),id,request).stream().findFirst().orElse(null);
    }
    public void snapshot(UUID id,long revision) {
        jdbc.update("INSERT INTO foodtime.community_post_versions(post_id,revision,payload) SELECT id,content_revision,foodtime.community_post_snapshot(id) FROM foodtime.community_posts WHERE id=? AND content_revision=? ON CONFLICT DO NOTHING",id,revision);
    }
    public void replace(UUID id,EditPostRequest input,boolean aiEnabled) {
        jdbc.update("DELETE FROM foodtime.ai_moderation_results WHERE (submission_type='community-post' AND submission_id=?) OR (submission_type='community-image' AND submission_id IN(SELECT id FROM foodtime.community_post_images WHERE post_id=?))",id,id);
        jdbc.update("DELETE FROM foodtime.community_post_images WHERE post_id=?",id);
        jdbc.update("""
                UPDATE foodtime.community_posts SET title=?,content=?,dish_id=?,content_revision=content_revision+1,
                edited_at=clock_timestamp(),status='pending',review_stage=?,manual_review_reason=?,
                reviewed_by_user_id=NULL,reviewed_by_ai_model=NULL,reviewed_at=NULL,review_reason=NULL WHERE id=?
                """,input.title(),input.content(),input.dishId(),aiEnabled?"ai_pending":"manual_pending",aiEnabled?null:"ai_disabled",id);
    }
    public void image(UUID post,int position,com.FoodtimeNeo.media.service.ImageStorageService.UploadedImage image) {
        jdbc.update("INSERT INTO foodtime.community_post_images(id,post_id,image_url,sort_order,width,height) VALUES(?,?,?,?,?,?)",UUID.randomUUID(),post,image.imageUrl(),position,image.width(),image.height());
    }
    public void receipt(UUID id,UUID request,String hash,long revision,boolean changed) {
        jdbc.update("INSERT INTO foodtime.community_post_edit_requests(post_id,client_request_id,payload_hash,resulting_revision,changed) VALUES(?,?,?,?,?)",id,request,hash,revision,changed);
    }
    public boolean same(UUID id,String json) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT title=(?::jsonb->>'title') AND content=(?::jsonb->>'content') AND dish_id IS NOT DISTINCT FROM NULLIF(?::jsonb->>'dishId','')::uuid AND COALESCE((SELECT jsonb_agg(image_url ORDER BY sort_order,id) FROM foodtime.community_post_images WHERE post_id=p.id),'[]')=(?::jsonb->'imageUrls') FROM foodtime.community_posts p WHERE p.id=?",Boolean.class,json,json,json,json,id));
    }
}
