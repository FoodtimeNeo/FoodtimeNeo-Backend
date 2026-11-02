package com.FoodtimeNeo.integration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AvatarIT extends IntegrationApiSupport {
    byte[] png(int width,int height,boolean noise)throws Exception{
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var random=new Random(1);
        if(noise)for(int y=0;y<height;y++)for(int x=0;x<width;x++)image.setRGB(x,y,random.nextInt(0x1000000));
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }
    Map<String,Object> profile(String image){return Map.of("displayName","饭","avatarUrl",image);}
    @Test void dedicatedUploadEnforcesSquareAndInclusiveTenMBLimit()throws Exception{
        var b=login(owner);byte[] small=png(512,512,false);
        assertThat(b.upload(Arrays.copyOf(small,10485761),"/uploads/avatars").statusCode()).isEqualTo(413);
        assertThat(b.upload(png(512,256,false),"/uploads/avatars").statusCode()).isEqualTo(400);
        var accepted=b.upload(Arrays.copyOf(png(1024,1024,false),10485760),"/uploads/avatars");assertThat(accepted.statusCode()).isEqualTo(201);
        String ref=body(accepted).path("data").path("imageUrl").asString();
        assertThat(body(accepted).path("data").path("width").asInt()).isEqualTo(512);
        assertThat(body(accepted).path("data").path("height").asInt()).isEqualTo(512);
        assertThat(b.call("PUT","/api/v1/users/me/profile",profile(ref)).statusCode()).isEqualTo(200);
        assertThat(b.image(ref).body().length).isLessThanOrEqualTo(1048576);
    }
    @Test void usingGeneralUploadCannotBypassAvatarShapeOrSize()throws Exception{
        var b=login(owner);var rectangle=b.upload(png(60,30,false));assertThat(rectangle.statusCode()).isEqualTo(201);
        String ref=body(rectangle).path("data").path("imageUrl").asString();
        assertThat(b.call("PUT","/api/v1/users/me/profile",profile(ref)).statusCode()).isEqualTo(400);
        byte[] large=png(1024,1024,true);assertThat(large.length).isGreaterThan(1048576);
        var normal=b.upload(large);assertThat(normal.statusCode()).isEqualTo(201);
        String tooLarge=body(normal).path("data").path("imageUrl").asString();
        assertThat(b.call("PUT","/api/v1/users/me/profile",profile(tooLarge)).statusCode()).isEqualTo(400);
        assertThat(body(b.call("GET","/api/v1/users/me/profile",null)).path("data").path("avatarUrl").isNull()).isTrue();
    }
    @Test void foreignFilesAreRejectedAndUnchangedHistoricalAvatarsRemainUsable()throws Exception{
        var b=login(owner);var another=login(other);
        String foreign=body(another.upload(png(64,64,false),"/uploads/avatars")).path("data").path("imageUrl").asString();
        assertThat(b.call("PUT","/api/v1/users/me/profile",profile(foreign)).statusCode()).isEqualTo(400);
        String historic=body(b.upload(png(64,32,false))).path("data").path("imageUrl").asString();
        jdbc.update("UPDATE foodtime.users SET avatar_url=? WHERE id=?",historic,owner.id());
        assertThat(b.call("PUT","/api/v1/users/me/profile",profile(historic)).statusCode()).isEqualTo(200);
        String replacement=body(b.upload(png(80,40,false))).path("data").path("imageUrl").asString();
        assertThat(b.call("PUT","/api/v1/users/me/profile",profile(replacement)).statusCode()).isEqualTo(400);
    }
    @Test void avatarUploadRejectsNonImagesAndRequiresSessionAndCsrf()throws Exception{
        var b=login(owner);
        assertThat(b.upload("<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(),"/uploads/avatars").statusCode()).isEqualTo(400);
        assertThat(b.upload(png(64,64,false),"/uploads/avatars",false).statusCode()).isEqualTo(403);
    }
}
