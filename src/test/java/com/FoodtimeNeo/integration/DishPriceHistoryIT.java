package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import tools.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class DishPriceHistoryIT extends IntegrationApiSupport {
    @Autowired PlatformTransactionManager transactions;
    private String history(){return "/api/v1/dishes/"+dish+"/price-history";}
    private String manage(String entry){return "/api/v1/admin/dishes/"+dish+"/price-history/"+entry;}
    private JsonNode read(Browser b)throws Exception{var response=b.call("GET",history(),null);assertThat(response.statusCode()).as(response.body()).isEqualTo(200);return body(response).path("data");}
    private Map<String,Object> catalog(String amount,String unit){var body=new HashMap<String,Object>();body.put("name","integration dish");body.put("description","price history");body.put("price",new BigDecimal(amount));if(unit!=null)body.put("priceUnit",unit);return body;}
    private void save(Browser b,String amount,String unit)throws Exception{var response=b.call("PUT","/api/v1/admin/catalog/dishes/"+dish,catalog(amount,unit));assertThat(response.statusCode()).as(response.body()).isEqualTo(200);}
    private long count(){return jdbc.queryForObject("SELECT count(*) FROM foodtime.dish_price_history WHERE dish_id=? AND hidden_at IS NULL",Long.class,dish);}
    private JsonNode tail(JsonNode history){var rows=history.path("records");return rows.get(rows.size()-1);}
    private Map<String,Object> correction(long revision,List<String> fields){var body=new HashMap<String,Object>();body.put("targetType","dish");body.put("targetId",dish);body.put("baseRevision",revision);body.put("changedFields",fields);body.put("name","new name");body.put("description","new description");body.put("price",new BigDecimal("15.00"));body.put("priceUnit","serving");body.put("reason","核对菜单");body.put("idempotencyKey",UUID.randomUUID().toString());return body;}
    private Map<String,Object> edit(JsonNode data,String amount,String unit){var body=new HashMap<String,Object>();body.put("price",amount==null?null:new BigDecimal(amount));body.put("priceUnit",unit);body.put("expectedRevision",data.path("revision").asLong());return body;}

    @Test void initialObservationAndEveryCatalogWriterAreCapturedWithoutNoopEvents()throws Exception{
        var admin=login(account("admin"));var user=login(owner);var first=read(user);
        assertThat(first.path("priceUnit").asString()).isEqualTo("unspecified");assertThat(count()).isEqualTo(1);
        assertThat(tail(first).path("origin").asString()).isEqualTo("created");
        save(admin,"12.30","serving");save(admin,"12.3",null); // omitted unit preserves current unit
        assertThat(count()).isEqualTo(2);assertThat(read(user).path("priceUnit").asString()).isEqualTo("serving");
        save(admin,"13.00",null);assertThat(count()).isEqualTo(3);
        var invalid=admin.call("PUT","/api/v1/admin/catalog/dishes/"+dish,catalog("14.00","custom"));
        assertThat(invalid.statusCode()).isEqualTo(400);assertThat(count()).isEqualTo(3);
        assertThat(read(user).path("currentPrice").decimalValue()).isEqualByComparingTo("13");
    }
    @Test void onlyApprovedCorrectionsApplyUnitsAndHistoryAndRecordBothAuditValues()throws Exception{
        var user=login(owner);var superadmin=login(account("superadmin"));var original=read(user);
        var proposed=user.call("POST","/api/v1/users/me/catalog-corrections",correction(original.path("revision").asLong(),List.of("name","description","price","priceUnit")));
        assertThat(proposed.statusCode()).as(proposed.body()).isEqualTo(201);assertThat(count()).isEqualTo(1);
        String id=body(proposed).path("data").path("id").asString();
        var decision=superadmin.call("POST","/api/v1/admin/catalog-corrections/"+id+"/decision",Map.of("decision","approved","expectedRevision",1));
        assertThat(decision.statusCode()).as(decision.body()).isEqualTo(200);var result=read(user);
        assertThat(count()).isEqualTo(2);assertThat(result.path("currentPrice").decimalValue()).isEqualByComparingTo("15");assertThat(result.path("priceUnit").asString()).isEqualTo("serving");
        assertThat(jdbc.queryForObject("SELECT after_values->>'priceUnit' FROM foodtime.catalog_correction_events WHERE correction_id=? AND action='approved'",String.class,UUID.fromString(id))).isEqualTo("serving");
        var next=correction(result.path("revision").asLong(),List.of("priceUnit"));next.put("priceUnit","bowl");
        var rejected=user.call("POST","/api/v1/users/me/catalog-corrections",next);String rejectedId=body(rejected).path("data").path("id").asString();
        assertThat(superadmin.call("POST","/api/v1/admin/catalog-corrections/"+rejectedId+"/decision",Map.of("decision","rejected","expectedRevision",1)).statusCode()).isEqualTo(200);assertThat(count()).isEqualTo(2);
    }
    @Test void latestCorrectionAndDeletionSynchronizeCurrentStateWithoutExtraEvents()throws Exception{
        var superadmin=login(account("superadmin"));save(superadmin,"15.00","serving");var before=read(superadmin);String last=tail(before).path("id").asString(),time=tail(before).path("recordedAt").asString();
        assertThat(superadmin.call("PUT",manage(last),edit(before,"0","kg")).statusCode()).isEqualTo(200);var updated=read(superadmin);
        assertThat(updated.path("currentPrice").decimalValue()).isEqualByComparingTo("0");assertThat(updated.path("priceUnit").asString()).isEqualTo("kg");assertThat(count()).isEqualTo(2);
        assertThat(tail(updated).path("recordedAt").asString()).isEqualTo(time);
        assertThat(superadmin.call("DELETE",manage(last)+"?expectedRevision="+updated.path("revision").asLong(),null).statusCode()).isEqualTo(200);var remaining=read(superadmin);
        assertThat(remaining.path("currentPrice").decimalValue()).isEqualByComparingTo("12.30");assertThat(remaining.path("priceUnit").asString()).isEqualTo("unspecified");assertThat(count()).isEqualTo(1);
        assertThat(remaining.toString()).doesNotContain("hidden_at","before_values","actor_id");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.dish_price_history_audit WHERE dish_id=?",Long.class,dish)).isEqualTo(2);
        assertThat(superadmin.call("DELETE",manage(tail(remaining).path("id").asString())+"?expectedRevision="+remaining.path("revision").asLong(),null).statusCode()).isEqualTo(409);
        assertThat(count()).isEqualTo(1);
    }
    @Test void olderCorrectionsPreserveCurrentAndStaleRevisionsCannotOverwrite()throws Exception{
        var admin=login(account("superadmin"));save(admin,"20","bowl");var before=read(admin);String oldest=before.path("records").get(0).path("id").asString();
        assertThat(admin.call("PUT",manage(oldest),edit(before,"10","serving")).statusCode()).isEqualTo(200);var after=read(admin);
        assertThat(after.path("currentPrice").decimalValue()).isEqualByComparingTo("20");assertThat(after.path("priceUnit").asString()).isEqualTo("bowl");
        assertThat(admin.call("PUT",manage(oldest),edit(before,"9","kg")).statusCode()).isEqualTo(409);
        assertThat(read(admin).path("records").get(0).path("price").decimalValue()).isEqualByComparingTo("10");
        assertThat(admin.call("PUT",manage(oldest),edit(after,"10.00","serving")).statusCode()).isEqualTo(200);
        assertThat(read(admin).path("revision").asLong()).isEqualTo(after.path("revision").asLong());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.dish_price_history_audit WHERE dish_id=?",Long.class,dish)).isEqualTo(1);
    }
    @Test void readVisibilityAndIndependentManagementCapabilityAreEnforced()throws Exception{
        var user=login(owner);var admin=login(account("admin"));var superadmin=login(account("superadmin"));var visitor=login(account("visitor"));var data=read(superadmin);String id=tail(data).path("id").asString();
        assertThat(read(user).path("canManage").asBoolean()).isFalse();assertThat(read(admin).path("canManage").asBoolean()).isFalse();assertThat(read(visitor).path("canManage").asBoolean()).isFalse();assertThat(data.path("canManage").asBoolean()).isTrue();
        for(var b:List.of(user,admin,visitor))assertThat(b.call("PUT",manage(id),edit(data,"99","kg")).statusCode()).isEqualTo(403);
        var anonymous=new Browser();assertThat(anonymous.call("GET",history(),null).statusCode()).isEqualTo(401);
        jdbc.update("UPDATE foodtime.dining_halls SET status='inactive' WHERE id=?",hall);assertThat(user.call("GET",history(),null).statusCode()).isEqualTo(404);
        assertThat(superadmin.call("PUT",manage(id),edit(data,"99","kg")).statusCode()).isEqualTo(404);
    }
    @Test void rangeUsesBeijingDaysAndCarriesTheLastEarlierObservation()throws Exception{
        var admin=login(account("superadmin"));save(admin,"14","serving");var data=read(admin);var rows=data.path("records");Instant now=jdbc.queryForObject("SELECT clock_timestamp()",Timestamp.class).toInstant();
        jdbc.update("UPDATE foodtime.dish_price_history SET recorded_at=? WHERE id=?",Timestamp.from(now.minusSeconds(60L*86400)),Long.valueOf(rows.get(0).path("id").asString()));
        jdbc.update("UPDATE foodtime.dish_price_history SET recorded_at=? WHERE id=?",Timestamp.from(now.minusSeconds(40L*86400)),Long.valueOf(rows.get(1).path("id").asString()));
        var recent=body(admin.call("GET",history()+"?days=30",null)).path("data");assertThat(recent.path("records").size()).isEqualTo(1);assertThat(recent.path("totalRecords").asLong()).isEqualTo(2);assertThat(recent.path("records").get(0).path("carriedForward").asBoolean()).isTrue();
        assertThat(Instant.parse(recent.path("windowStart").asString()).atOffset(java.time.ZoneOffset.ofHours(8)).toLocalTime()).isEqualTo(java.time.LocalTime.MIDNIGHT);
        assertThat(body(admin.call("GET",history()+"?days=90",null)).path("data").path("records").size()).isEqualTo(2);
        assertThat(admin.call("GET",history()+"?days=7",null).statusCode()).isEqualTo(400);
    }
    @Test void unknownPriceAndUnitOnlyChangesRemainDistinctAndRollbackIsAtomic()throws Exception{
        var admin=login(account("superadmin"));save(admin,"12.30","serving");save(admin,"12.30","jin");assertThat(count()).isEqualTo(3);
        var data=read(admin);assertThat(admin.call("PUT",manage(tail(data).path("id").asString()),edit(data,null,"jin")).statusCode()).isEqualTo(200);assertThat(read(admin).path("currentPrice").isNull()).isTrue();
        long count=count();var tx=new TransactionTemplate(transactions);
        assertThatThrownBy(()->tx.execute(status->{jdbc.update("UPDATE foodtime.dishes SET price=88 WHERE id=?",dish);throw new IllegalStateException("rollback");})).isInstanceOf(IllegalStateException.class);
        assertThat(count()).isEqualTo(count);assertThat(read(admin).path("currentPrice").isNull()).isTrue();
    }
    @Test void concurrentCatalogChangesKeepTheTimelineTailConsistent()throws Exception{
        var first=login(account("admin"));var second=login(account("admin"));
        try(var workers=Executors.newFixedThreadPool(2)){var a=workers.submit(()->{save(first,"15","serving");return true;});var b=workers.submit(()->{save(second,"18","bowl");return true;});assertThat(a.get(10,TimeUnit.SECONDS)).isTrue();assertThat(b.get(10,TimeUnit.SECONDS)).isTrue();}
        var after=read(first);assertThat(count()).isEqualTo(3);assertThat(tail(after).path("price").decimalValue()).isEqualByComparingTo(after.path("currentPrice").decimalValue());assertThat(tail(after).path("priceUnit").asString()).isEqualTo(after.path("priceUnit").asString());
    }
}
