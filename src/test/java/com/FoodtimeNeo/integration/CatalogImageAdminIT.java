package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogImageAdminIT extends IntegrationApiSupport {
    private String path(String type,UUID id) { return "/api/v1/admin/catalog/"+type+"/"+id+"/images"; }
    private String upload(Browser browser) throws Exception {
        var image=new java.awt.image.BufferedImage(3,5,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image,"png",bytes);
        return body(browser.upload(bytes.toByteArray())).path("data").path("imageUrl").asString();
    }
    @Test void onlySuperadminCanManageImagesAndOnlyTheirOwnUploadsCanBeAdded() throws Exception {
        var root=login(account("superadmin")); var student=login(owner); var admin=login(account("admin"));
        String url=upload(root), foreign=upload(student), path=path("dishes",dish);
        for(var browser:java.util.List.of(student,admin)) {
            assertThat(browser.call("GET",path,null).statusCode()).isEqualTo(403);
            assertThat(browser.call("POST",path,Map.of("imageUrl",url)).statusCode()).isEqualTo(403);
            assertThat(browser.call("DELETE",path+"/"+UUID.randomUUID(),null).statusCode()).isEqualTo(403);
        }
        assertThat(root.call("POST",path,Map.of("imageUrl",foreign)).statusCode()).isEqualTo(400);
        assertThat(root.call("POST",path,Map.of("imageUrl","https://example.com/photo.png")).statusCode()).isEqualTo(400);
        assertThat(root.call("POST",path,Map.of("imageUrl",url)).statusCode()).isEqualTo(201);
        assertThat(root.call("POST",path,Map.of("imageUrl",url)).statusCode()).isEqualTo(201);
        var listed=body(root.call("GET",path,null)).path("data");
        assertThat(listed.path("total").asLong()).isEqualTo(1);
        String id=listed.path("items").get(0).path("id").asString();
        assertThat(student.image(url).statusCode()).isEqualTo(200);
        assertThat(root.call("DELETE",path("dishes",UUID.randomUUID())+"/"+id,null).statusCode()).isEqualTo(404);
        assertThat(root.call("DELETE",path+"/"+id,null).statusCode()).isEqualTo(200);
        assertThat(body(student.call("GET",dishPath("/images"),null)).path("data").path("total").asLong()).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_images WHERE id=?",String.class,UUID.fromString(id))).isEqualTo("rejected");
        assertThat(student.image(url).statusCode()).isEqualTo(404);
        assertThat(root.image(url).statusCode()).isEqualTo(200);
    }
    @Test void hallCoverAndStallGalleryAreUpdatedTogetherAndDoNotDeleteSharedFiles() throws Exception {
        var root=login(account("superadmin")); var viewer=login(owner);
        String first=upload(root), second=upload(root);
        assertThat(root.call("POST",path("dining-halls",hall),Map.of("imageUrl",first)).statusCode()).isEqualTo(201);
        assertThat(root.call("POST",path("stalls",stall),Map.of("imageUrl",first)).statusCode()).isEqualTo(201);
        assertThat(root.call("POST",path("stalls",stall),Map.of("imageUrl",second)).statusCode()).isEqualTo(201);
        var list=body(root.call("GET",path("stalls",stall)+"?size=1",null)).path("data");
        assertThat(list.path("total").asLong()).isEqualTo(2);
        assertThat(list.path("items").size()).isEqualTo(1);
        String firstId=list.path("items").get(0).path("id").asString();
        assertThat(root.call("DELETE",path("stalls",stall)+"/"+firstId,null).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT cover_image_url FROM foodtime.stalls WHERE id=?",String.class,stall)).isEqualTo(second);
        assertThat(viewer.image(first).statusCode()).isEqualTo(200);
        assertThat(root.call("DELETE",path("dining-halls",hall)+"/"+hall,null).statusCode()).isEqualTo(200);
        assertThat(viewer.image(first).statusCode()).isEqualTo(404);
        // Legacy covers may not have a matching stall_images row.
        jdbc.update("UPDATE foodtime.stalls SET cover_image_url=? WHERE id=?",first,stall);
        assertThat(root.call("DELETE",path("stalls",stall)+"/"+stall,null).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT cover_image_url FROM foodtime.stalls WHERE id=?",String.class,stall)).isEqualTo(second);
    }
}
