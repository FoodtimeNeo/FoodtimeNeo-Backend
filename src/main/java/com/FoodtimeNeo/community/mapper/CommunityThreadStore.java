package com.FoodtimeNeo.community.mapper;

import com.FoodtimeNeo.community.dto.*;
import com.FoodtimeNeo.community.service.CommunityCursor;
import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

@Repository
public class CommunityThreadStore {
    private final JdbcTemplate jdbc;
    public CommunityThreadStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private static final String SELECT="""
        SELECT c.*,u.display_name,u.avatar_url,t.display_name AS target_name,
          EXISTS(SELECT 1 FROM foodtime.community_likes l WHERE l.user_id=? AND l.target_type='comment' AND l.target_id=c.id) AS liked
        FROM foodtime.community_comments c JOIN foodtime.users u ON u.id=c.user_id
        LEFT JOIN foodtime.users t ON t.id=c.reply_to_user_id
        """;
    public boolean lockPost(UUID post){
        return !jdbc.query("SELECT id FROM foodtime.community_posts WHERE id=? AND status='approved' AND deleted_at IS NULL FOR UPDATE",(r,n)->r.getObject(1,UUID.class),post).isEmpty();
    }
    public UUID retry(UUID user,UUID post,CreateCommentRequest body){
        var rows=jdbc.query("SELECT id,post_id,content,root_comment_id,reply_to_comment_id FROM foodtime.community_comments WHERE user_id=? AND client_request_id=?",
            (r,n)->{
                if(!post.equals(r.getObject("post_id",UUID.class))||!body.content().equals(r.getString("content"))
                    ||!Objects.equals(body.rootCommentId(),r.getObject("root_comment_id",UUID.class))
                    ||!Objects.equals(body.replyToCommentId(),r.getObject("reply_to_comment_id",UUID.class)))
                    throw new BusinessException("REQUEST_CONFLICT","请求编号已用于另一条评论",HttpStatus.CONFLICT);
                return r.getObject("id",UUID.class);
            },user,body.clientRequestId());
        return rows.isEmpty()?null:rows.getFirst();
    }
    public UUID create(UUID user,UUID post,CreateCommentRequest body,boolean ai){
        UUID targetUser=null;
        if((body.rootCommentId()==null)!=(body.replyToCommentId()==null))throw invalid();
        if(body.rootCommentId()!=null){
            var rows=jdbc.query("""
                SELECT t.user_id FROM foodtime.community_comments t JOIN foodtime.community_comments root ON root.id=?
                WHERE t.id=? AND t.post_id=? AND root.post_id=t.post_id AND root.root_comment_id IS NULL
                  AND root.status='approved' AND root.deleted_at IS NULL AND t.status='approved' AND t.deleted_at IS NULL
                  AND COALESCE(t.root_comment_id,t.id)=root.id
                """,(r,n)->r.getObject(1,UUID.class),body.rootCommentId(),body.replyToCommentId(),post);
            if(rows.isEmpty())throw invalid();targetUser=rows.getFirst();
        }
        UUID id=UUID.randomUUID();
        int inserted=jdbc.update("""
            INSERT INTO foodtime.community_comments(id,post_id,user_id,content,client_request_id,root_comment_id,reply_to_comment_id,reply_to_user_id,review_stage,manual_review_reason)
            VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(user_id,client_request_id) DO NOTHING
            """,id,post,user,body.content(),body.clientRequestId(),body.rootCommentId(),body.replyToCommentId(),targetUser,ai?"ai_pending":"manual_pending",ai?null:"ai_disabled");
        return inserted==0?null:id;
    }
    private BusinessException invalid(){return new BusinessException("REPLY_TARGET_UNAVAILABLE","回复对象已不可见或不属于同一楼层",HttpStatus.CONFLICT);}
    public List<CommunityCommentResponse> page(UUID post,UUID root,UUID viewer,int limit,CommunityCursor cursor){
        var args=new ArrayList<Object>();args.add(viewer);args.add(post);
        String where=" WHERE c.post_id=? AND ";
        if(root==null)where+="c.root_comment_id IS NULL";else{where+="c.root_comment_id=?";args.add(root);}
        where+=" AND (c.status='approved' OR c.user_id=?)";args.add(viewer);
        if(root==null)where+=" AND (c.deleted_at IS NULL OR (c.status='approved' AND c.reply_count>0))";
        else where+=" AND c.deleted_at IS NULL";
        String order=root==null?"DESC":"ASC",op=root==null?"<":">";
        if(cursor!=null){where+=" AND (c.created_at,c.id)"+op+"(?,?)";args.add(Timestamp.from(cursor.time()));args.add(cursor.id());}
        args.add(limit);
        return jdbc.query(SELECT+where+" ORDER BY c.created_at "+order+",c.id "+order+" LIMIT ?",(r,n)->row(r),args.toArray());
    }
    public boolean rootVisible(UUID root,UUID post,UUID viewer){
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM foodtime.community_comments WHERE id=? AND post_id=? AND root_comment_id IS NULL AND (status='approved' OR user_id=?) AND (deleted_at IS NULL OR reply_count>0))",Boolean.class,root,post,viewer));
    }
    public UUID postOf(UUID id){return jdbc.query("SELECT post_id FROM foodtime.community_comments WHERE id=?",(r,n)->r.getObject(1,UUID.class),id).stream().findFirst().orElse(null);}
    public long total(UUID post,UUID root,UUID viewer){
        String sql="SELECT count(*) FROM foodtime.community_comments c WHERE post_id=? AND ";
        if(root==null)return jdbc.queryForObject(sql+"root_comment_id IS NULL AND (status='approved' OR user_id=?) AND (deleted_at IS NULL OR (status='approved' AND reply_count>0))",Long.class,post,viewer);
        return jdbc.queryForObject(sql+"root_comment_id=? AND (status='approved' OR user_id=?) AND deleted_at IS NULL",Long.class,post,root,viewer);
    }
    public int delete(UUID id,UUID user){return jdbc.update("UPDATE foodtime.community_comments SET deleted_at=now(),deleted_by_user_id=?,delete_reason='作者删除' WHERE id=? AND user_id=? AND deleted_at IS NULL",user,id,user);}
    public List<CommunityCommentResponse> legacy(UUID post,int size,long offset){
        return jdbc.query(SELECT+" WHERE c.post_id=? AND c.status='approved' AND c.deleted_at IS NULL ORDER BY c.created_at DESC,c.id DESC LIMIT ? OFFSET ?",(r,n)->row(r),new UUID(0,0),post,size,offset);
    }
    private CommunityCommentResponse row(ResultSet r)throws SQLException{
        boolean deleted=r.getTimestamp("deleted_at")!=null;
        return new CommunityCommentResponse(r.getObject("id",UUID.class),r.getObject("post_id",UUID.class),r.getObject("user_id",UUID.class),r.getString("display_name"),deleted?"":r.getString("content"),
            r.getLong("like_count"),r.getString("status"),r.getString("review_reason"),null,null,null,null,null,r.getTimestamp("created_at").toInstant(),
            r.getObject("root_comment_id",UUID.class),r.getObject("reply_to_comment_id",UUID.class),r.getObject("reply_to_user_id",UUID.class),r.getString("target_name"),r.getString("avatar_url"),r.getLong("reply_count"),r.getBoolean("liked"),deleted,r.getString("review_stage"));
    }
}
