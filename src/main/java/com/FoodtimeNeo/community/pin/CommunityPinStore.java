package com.FoodtimeNeo.community.pin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/** Pin storage is separate from post content and does not alter publication chronology. */
@Repository
public class CommunityPinStore {
    private final JdbcTemplate jdbc;
    public CommunityPinStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public long revision(boolean lock) {
        return jdbc.queryForObject("SELECT revision FROM foodtime.community_pin_state WHERE singleton"+(lock?" FOR UPDATE":""),Long.class);
    }
    public List<PinDtos.Entry> entries(boolean includeHidden) {
        return jdbc.query("""
                SELECT p.id,p.title,u.display_name,p.status,pin.position
                FROM foodtime.community_post_pins pin JOIN foodtime.community_posts p ON p.id=pin.post_id
                JOIN foodtime.users u ON u.id=p.user_id
                WHERE p.deleted_at IS NULL
                """+(includeHidden?"":" AND p.status='approved'")+" ORDER BY pin.position",
                (row,index)->new PinDtos.Entry(row.getObject(1,UUID.class),row.getString(2),row.getString(3),row.getString(4),row.getInt(5)));
    }
    public boolean lockPublished(UUID id) {
        return !jdbc.query("SELECT id FROM foodtime.community_posts WHERE id=? AND status='approved' AND deleted_at IS NULL FOR UPDATE",
                (row,index)->row.getObject(1,UUID.class),id).isEmpty();
    }
    public void lockPost(UUID id) {
        jdbc.query("SELECT id FROM foodtime.community_posts WHERE id=? FOR UPDATE",(row,index)->row.getObject(1,UUID.class),id);
    }
    public void insert(UUID id,int position,UUID actor) {
        jdbc.update("INSERT INTO foodtime.community_post_pins(post_id,position,pinned_by) VALUES(?,?,?)",id,position,actor);
    }
    public void remove(UUID id) { jdbc.update("DELETE FROM foodtime.community_post_pins WHERE post_id=?",id); }
    public void order(List<UUID> ids) {
        for(int index=0;index<ids.size();index++)jdbc.update("UPDATE foodtime.community_post_pins SET position=? WHERE post_id=?",index+1,ids.get(index));
    }
    public void changed() { jdbc.update("UPDATE foodtime.community_pin_state SET revision=revision+1 WHERE singleton"); }
}
