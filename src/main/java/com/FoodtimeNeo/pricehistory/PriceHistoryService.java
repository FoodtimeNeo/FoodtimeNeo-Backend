package com.FoodtimeNeo.pricehistory;

import com.FoodtimeNeo.authz.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static com.FoodtimeNeo.pricehistory.PriceHistoryDtos.*;

/** Owns timeline reads/corrections; catalog writers only depend on the database capture invariant. */
@Service
public class PriceHistoryService {
    private final JdbcTemplate jdbc;
    private final CapabilityService capabilities;
    private record Dish(UUID id,String name,BigDecimal price,String unit,long revision) {}
    public PriceHistoryService(JdbcTemplate jdbc,CapabilityService capabilities){this.jdbc=jdbc;this.capabilities=capabilities;}
    private AuthorizationContext context(UUID dish){return AuthorizationContext.resource("dish",dish);}
    private Dish visible(UUID id,boolean lock){
        return jdbc.query("""
            SELECT d.id,d.name,d.price,d.price_unit,d.revision FROM foodtime.dishes d
            JOIN foodtime.stalls s ON s.id=d.stall_id AND s.status='active'
            JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id AND h.status='active'
            WHERE d.id=? AND d.status='active'
            """+(lock?" FOR UPDATE OF d":""),(r,n)->new Dish(id,r.getString("name"),r.getBigDecimal("price"),r.getString("price_unit"),r.getLong("revision")),id)
            .stream().findFirst().orElseThrow(()->error("DISH_NOT_FOUND","菜品不存在或暂不可用",HttpStatus.NOT_FOUND));
    }
    private List<Entry> entries(UUID id){return jdbc.query("""
        SELECT id,price,price_unit,recorded_at,origin,revision FROM foodtime.dish_price_history
        WHERE dish_id=? AND hidden_at IS NULL ORDER BY id
        """,(r,n)->new Entry(r.getString("id"),r.getBigDecimal("price"),r.getString("price_unit"),r.getTimestamp("recorded_at").toInstant(),r.getString("origin"),r.getLong("revision"),false),id);}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public History read(UserProfile actor,UUID id,int days){
        if(days!=0&&days!=30&&days!=90)throw error("VALIDATION_ERROR","时间范围只能为全部、近30天或近90天",HttpStatus.BAD_REQUEST);
        Dish dish=visible(id,false);
        Instant end=jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class).toInstant();
        List<Entry> all=entries(id);
        if(!all.isEmpty()&&end.isBefore(all.getLast().recordedAt()))end=all.getLast().recordedAt();
        Instant start=days==0?null:end.atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().minusDays(days-1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        List<Entry> rows=new ArrayList<>();Entry anchor=null;
        for(Entry row:all){if(start!=null&&row.recordedAt().isBefore(start))anchor=row;else rows.add(row);}
        if(anchor!=null)rows.addFirst(new Entry(anchor.id(),anchor.price(),anchor.priceUnit(),anchor.recordedAt(),anchor.origin(),anchor.revision(),true));
        return new History(id,dish.name(),dish.price(),dish.unit(),dish.revision(),start,end,
            capabilities.allows(actor,Capability.DISH_PRICE_HISTORY_MANAGE,context(id)),all.size(),rows);
    }

    @Transactional
    public void edit(UserProfile actor,UUID id,long historyId,Edit body){
        capabilities.require(actor,Capability.DISH_PRICE_HISTORY_MANAGE,context(id));
        PriceUnit.require(body.priceUnit());
        mutate(actor,id,historyId,body.expectedRevision(),body,false);
    }
    @Transactional
    public void hide(UserProfile actor,UUID id,long historyId,long expectedRevision){
        capabilities.require(actor,Capability.DISH_PRICE_HISTORY_MANAGE,context(id));
        mutate(actor,id,historyId,expectedRevision,null,true);
    }
    private void mutate(UserProfile actor,UUID id,long historyId,long expected,Edit body,boolean hide){
        Dish dish=visible(id,true);
        if(dish.revision()!=expected)throw error("PRICE_HISTORY_CONFLICT","价格资料已更新，请刷新后再操作",HttpStatus.CONFLICT);
        List<Entry> rows=entries(id);
        Entry original=rows.stream().filter(v->v.id().equals(Long.toString(historyId))).findFirst()
            .orElseThrow(()->error("PRICE_HISTORY_NOT_FOUND","价格记录不存在或已移除",HttpStatus.NOT_FOUND));
        if(hide&&rows.size()==1)throw error("PRICE_HISTORY_LAST_RECORD","唯一记录不能删除，可以更正金额或单位",HttpStatus.CONFLICT);
        if(!hide&&Objects.equals(original.priceUnit(),body.priceUnit())&&same(original.price(),body.price()))return;
        jdbc.update("""
            INSERT INTO foodtime.dish_price_history_audit(id,dish_id,history_id,actor_id,action,before_values,after_values)
            SELECT ?,dish_id,id,?,?,to_jsonb(p),CASE WHEN ? THEN NULL ELSE jsonb_build_object('price',?::numeric,'priceUnit',?::text) END
            FROM foodtime.dish_price_history p WHERE id=? AND dish_id=?
            """,UUID.randomUUID(),actor.id(),hide?"hidden":"corrected",hide,hide?null:body.price(),hide?null:body.priceUnit(),historyId,id);
        if(hide)jdbc.update("UPDATE foodtime.dish_price_history SET hidden_at=clock_timestamp(),revision=revision+1 WHERE id=? AND dish_id=?",historyId,id);
        else jdbc.update("UPDATE foodtime.dish_price_history SET price=?,price_unit=?,revision=revision+1 WHERE id=? AND dish_id=?",body.price(),body.priceUnit(),historyId,id);
        Entry latest=entries(id).getLast();
        // The corrected visible tail is authoritative; do not manufacture a second price event.
        jdbc.queryForObject("SELECT set_config('foodtime.correcting_price_history','on',true)",String.class);
        jdbc.update("UPDATE foodtime.dishes SET price=?,price_unit=?,revision=revision+1 WHERE id=?",latest.price(),latest.priceUnit(),id);
        jdbc.queryForObject("SELECT set_config('foodtime.correcting_price_history','off',true)",String.class);
    }
    private static boolean same(BigDecimal a,BigDecimal b){return a==null?b==null:b!=null&&a.compareTo(b)==0;}
    private static BusinessException error(String code,String message,HttpStatus status){return new BusinessException(code,message,status);}
}
