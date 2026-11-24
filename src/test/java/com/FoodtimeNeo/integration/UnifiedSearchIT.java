package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.search.halls[10000000-0000-0000-0000-000000000001].latitude=39.95",
    "app.search.halls[10000000-0000-0000-0000-000000000001].longitude=116.34",
    "app.search.halls[10000000-0000-0000-0000-000000000001].source=isolated-test-fixture"})
class UnifiedSearchIT extends IntegrationApiSupport {
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate searchJdbc;
    @Test void publicSearchRevalidatesLocalReplicaAndSkipsUnchangedProjectionQueries() throws Exception {
        var browser=login(owner);addDish(stall,key,10);
        String path="/api/v1/search/stalls?q="+key+"&diningHallIds="+hall+"&previewSize=4";
        org.mockito.Mockito.clearInvocations(searchJdbc);
        var first=browser.call("GET",path,null);assertThat(first.statusCode()).isEqualTo(200);
        String etag=first.headers().firstValue("ETag").orElseThrow();
        assertThat(first.headers().firstValue("X-Foodtime-Public-Catalog")).contains("1");
        assertThat(browser.imageIfNoneMatch(path,etag).statusCode()).isEqualTo(304);
        org.mockito.Mockito.verify(searchJdbc,org.mockito.Mockito.times(1)).queryForObject(
                org.mockito.ArgumentMatchers.contains("count(DISTINCT stall_id)"),
                org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.eq(Long.class));
        jdbc.update("UPDATE foodtime.dishes SET price=20 WHERE stall_id=?",stall);
        var changed=browser.imageIfNoneMatch(path,etag);assertThat(changed.statusCode()).isEqualTo(200);
        assertThat(json.readTree(changed.body()).path("data").path("items").get(0).path("dishes").get(0).path("price").asInt()).isEqualTo(20);
        String current=changed.headers().firstValue("ETag").orElseThrow();
        jdbc.update("UPDATE foodtime.dining_halls SET status='inactive' WHERE id=?",hall);
        assertThat(json.readTree(browser.imageIfNoneMatch(path,current).body()).path("data").path("total").asInt()).isZero();
        assertThat(browser.call("GET",path+"&latitude=39&longitude=116",null).statusCode()).isEqualTo(400);
        assertThat(browser.call("POST","/api/v1/search/stalls/query",Map.of("q",key)).headers().firstValue("X-Foodtime-Public-Catalog")).isEmpty();
    }
    private final String key = "search-" + UUID.randomUUID().toString().substring(0,8);
    private UUID addDish(UUID target, String name, Integer price) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,price) VALUES(?,?,?,?)",id,target,name,price); return id;
    }
    private UUID anotherStall(UUID targetHall) {
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO foodtime.stalls(id,dining_hall_id,name) VALUES(?,?,?)",id,targetHall,key+"档口");return id;
    }
    private UUID locatedHall() {
        UUID id=UUID.fromString("10000000-0000-0000-0000-000000000001"); additionalHalls.add(id);
        jdbc.update("INSERT INTO foodtime.dining_halls(id,name) VALUES(?,?)",id,key+"食堂"); return id;
    }
    @Test void groupsBeforePaginationAndBoundsPreviewsWithApprovedDishImages() throws Exception {
        var browser=login(account("visitor"));
        UUID first=addDish(stall,key,10);for(int i=0;i<7;i++)addDish(stall,key+"-"+i,11+i);
        addDish(anotherStall(hall),key+"-另一组",15);
        jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,image_url,status,uploaded_by_user_id) VALUES(?,?,?,'approved',?)",UUID.randomUUID(),first,"/uploads/formal.jpg",owner.id());
        var response=browser.call("POST","/api/v1/search/stalls/query",Map.of("q",key,"size",1,"previewSize",4));
        assertThat(response.statusCode()).isEqualTo(200);
        var data=body(response).path("data");assertThat(data.path("total").asInt()).isEqualTo(2);
        var group=data.path("items").get(0);assertThat(group.path("id").asString()).isEqualTo(stall.toString());
        assertThat(group.path("matchCount").asInt()).isEqualTo(8);assertThat(group.path("dishes").size()).isEqualTo(4);
        assertThat(group.path("dishes").get(0).path("imageUrl").asString()).isEqualTo("/uploads/formal.jpg");
        var second=body(browser.call("POST","/api/v1/search/stalls/query",Map.of("q",key,"page",2,"size",1))).path("data");
        assertThat(second.path("items").get(0).path("id").asString()).isNotEqualTo(stall.toString());
        var all=body(browser.call("POST","/api/v1/search/dishes/query",Map.of("q",key,"stallId",stall,"size",3,"page",2))).path("data");
        assertThat(all.path("total").asInt()).isEqualTo(8);assertThat(all.path("items").size()).isEqualTo(3);
    }
    @Test void filtersRetainUnknownValuesAndDoNotReturnDisabledAncestors() throws Exception {
        UUID priced=addDish(stall,key+" %_",12);addDish(stall,key+" %_unknown",null);addDish(stall,key+" %_expensive",90);
        UUID hidden=anotherStall(hall);addDish(hidden,key+" %_hidden",11);
        jdbc.update("UPDATE foodtime.stalls SET status='inactive' WHERE id=?",hidden);
        var browser=login(owner);
        var result=body(browser.call("POST","/api/v1/search/stalls/query",Map.of("q",key+" %_","minPrice",10,"maxPrice",20,"diningHallIds",List.of(hall),"sort","price_asc"))).path("data");
        assertThat(result.path("total").asInt()).isEqualTo(1);
        var dishes=result.path("items").get(0).path("dishes");assertThat(dishes.size()).isEqualTo(2);
        assertThat(dishes.get(0).path("id").asString()).isEqualTo(priced.toString());assertThat(dishes.get(1).path("price").isNull()).isTrue();
    }
    @Test void distanceSortingPutsUnlocatedStallsLastAndNeverReturnsDevicePosition() throws Exception {
        UUID targetHall=locatedHall();UUID nearby=anotherStall(targetHall);addDish(nearby,key,12);addDish(stall,key+"unknown",10);
        var result=body(login(owner).call("POST","/api/v1/search/stalls/query",Map.of("q",key,"sort","distance","latitude",39.95,"longitude",116.34))).path("data");
        assertThat(result.path("items").get(0).path("id").asString()).isEqualTo(nearby.toString());
        assertThat(result.path("items").get(0).path("distanceMeters").asDouble()).isZero();
        assertThat(result.path("items").get(1).path("distanceMeters").isNull()).isTrue();
        assertThat(result.toString()).doesNotContain("latitude","longitude");
    }
    private UUID note(String status, UUID linkedDish, UUID source, int likes) {
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO foodtime.community_posts(id,user_id,title,content,status,dish_id,repost_of_id,like_count,client_request_id,review_stage,reviewed_by_user_id,reviewed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,now())",id,owner.id(),key,key+"正文",status,linkedDish,source,likes,UUID.randomUUID(),status.equals("approved")?"completed":"ai_pending",owner.id());return id;
    }
    @Test void notesSupportHallTypeLikesAndDistanceWithoutExposingHiddenVersions() throws Exception {
        UUID locatedDish=addDish(anotherStall(locatedHall()),key,10);
        UUID original=note("approved",locatedDish,null,1);note("pending",locatedDish,null,99);
        UUID repost=note("approved",locatedDish,original,8);note("approved",null,null,50);
        var browser=login(account("visitor"));
        var response=browser.call("POST","/api/v1/search/notes/query",Map.of("q",key,"type","repost","sort","likes","diningHallIds",List.of(UUID.fromString("10000000-0000-0000-0000-000000000001"))));
        assertThat(response.statusCode()).isEqualTo(200);var data=body(response).path("data");
        assertThat(data.path("total").asInt()).isEqualTo(1);assertThat(data.path("items").get(0).path("post").path("id").asString()).isEqualTo(repost.toString());
        data=body(browser.call("POST","/api/v1/search/notes/query",Map.of("q",key,"sort","distance","latitude",39.95,"longitude",116.34))).path("data");
        assertThat(data.path("total").asInt()).isEqualTo(3);
        assertThat(data.path("items").get(0).path("distanceMeters").asDouble()).isZero();
        assertThat(data.path("items").get(2).path("distanceMeters").isNull()).isTrue();
        jdbc.update("UPDATE foodtime.dishes SET status='inactive' WHERE id=?",locatedDish);
        assertThat(body(browser.call("POST","/api/v1/search/notes/query",Map.of("q",key,"diningHallIds",List.of(UUID.fromString("10000000-0000-0000-0000-000000000001"))))).path("data").path("total").asInt()).isZero();
    }
    @Test void rejectsUnboundedInvalidOrUnauthenticatedQueries() throws Exception {
        var browser=login(owner);
        assertThat(new Browser().call("POST","/api/v1/search/stalls/query",Map.of()).statusCode()).isEqualTo(403);
        for(var body:List.of(Map.of("q","字".repeat(51)),Map.of("previewSize",100),Map.of("sort","injected"),Map.of("sort","distance"),Map.of("latitude",999,"longitude",116),Map.of("minPrice",20,"maxPrice",10)))
            assertThat(browser.call("POST","/api/v1/search/stalls/query",body).statusCode()).isEqualTo(400);
        assertThat(browser.call("POST","/api/v1/search/notes/query",Map.of("sort","injected")).statusCode()).isEqualTo(400);
    }
}
