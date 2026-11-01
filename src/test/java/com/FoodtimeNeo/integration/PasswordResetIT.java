package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.auth.reset.PasswordResetStore;
import com.FoodtimeNeo.auth.verification.EmailVerificationStore;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Real HTTP, CSRF/session cookies, SMTP, Redis Lua and PostgreSQL conditional updates. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "app.ai.single.enabled=false","app.ai.triage.enabled=false","app.auth.cookie-secure=false",
    "app.email-verification.mailbox.enabled=false"})
class PasswordResetIT {
    static final CapturingSmtpServer SMTP=new CapturingSmtpServer();
    static final String PREFIX="foodtime:integration:reset:"+UUID.randomUUID();
    static final String OLD="Old2026!",NEW="New2026!";
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;@Autowired StringRedisTemplate redis;@Autowired PasswordEncoder passwords;
    @Autowired UserMapper users;@Autowired PasswordResetStore store;@Autowired EmailVerificationStore registration;
    final List<String> emails=new ArrayList<>();final JsonMapper json=JsonMapper.builder().build();
    @DynamicPropertySource static void config(DynamicPropertyRegistry p){
        p.add("app.email-verification.enabled",()->true);p.add("app.email-verification.from",()->"sender@example.test");
        p.add("app.email-verification.secret",()->"test-only-secret-at-least-32-characters");
        p.add("app.email-verification.redis-namespace",()->PREFIX+":registration");
        p.add("app.auth.password-reset.redis-namespace",()->PREFIX+":reset");
        p.add("app.auth.redis-namespace",()->PREFIX+":login");p.add("spring.session.redis.namespace",()->PREFIX+":session");
        p.add("spring.mail.host",()->"127.0.0.1");p.add("spring.mail.port",SMTP::port);
        p.add("spring.mail.properties[mail.smtp.localhost]",()->"localhost");p.add("spring.mail.properties[mail.from]",()->"sender@example.test");
        p.add("spring.mail.properties[mail.smtp.auth]",()->false);p.add("spring.mail.properties[mail.smtp.starttls.enable]",()->false);
        p.add("spring.mail.properties[mail.smtp.starttls.required]",()->false);p.add("spring.mail.properties[mail.smtp.ssl.enable]",()->false);
    }
    @AfterAll static void close()throws Exception{SMTP.close();}
    @AfterEach void clean(){
        for(String email:emails)jdbc.update("delete from foodtime.users where email=?",email);
        Set<String> keys=redis.keys(PREFIX+":*");if(keys!=null&&!keys.isEmpty())redis.delete(keys);SMTP.clear();
    }
    @Test void directlyCallingResetCannotSkipVerificationAndRequiresCsrf()throws Exception{
        String email=account();var before=users.findLoginAccount(email);
        Browser browser=new Browser();browser.csrf();
        assertThat(browser.post("",Map.of("email",email,"newPassword",NEW,"verified",true)).statusCode()).isEqualTo(400);
        assertThat(browser.post("",Map.of("resetToken","A".repeat(43),"newPassword",NEW)).statusCode()).isEqualTo(400);
        assertThat(browser.send("POST","/api/v1/auth/password-reset",json.writeValueAsString(Map.of("resetToken","A".repeat(43),"newPassword",NEW)),false).statusCode()).isEqualTo(403);
        assertThat(users.findLoginAccount(email).passwordHash()).isEqualTo(before.passwordHash());
    }
    @Test void successfulResetInvalidatesOldSessionsAndTokenCannotBeReplayed()throws Exception{
        String email=account();Browser logged=new Browser();logged.csrf();
        assertThat(logged.login(email,OLD).statusCode()).isEqualTo(200);logged.csrf();
        Browser guest=new Browser();guest.csrf();String token=grant(guest,email);
        assertThat(guest.post("",Map.of("resetToken",token,"newPassword",NEW,"email","another@example.test")).statusCode()).isEqualTo(200);
        assertThat(passwords.matches(NEW,users.findLoginAccount(email).passwordHash())).isTrue();
        assertThat(logged.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(guest.post("",Map.of("resetToken",token,"newPassword","Other2026!")).statusCode()).isEqualTo(400);
        Browser fresh=new Browser();fresh.csrf();assertThat(fresh.login(email,OLD).statusCode()).isEqualTo(401);
        assertThat(fresh.login(email,NEW).statusCode()).isEqualTo(200);
    }
    @Test void registrationAndResetCodesAreNotInterchangeable()throws Exception{
        String email=account(),reservation=UUID.randomUUID().toString();Browser b=new Browser();b.csrf();
        assertThat(registration.reserveSend(email,"127.0.0.1","fixture",reservation)).isZero();assertThat(registration.publish(email,reservation,"012345")).isTrue();
        assertThat(b.post("/verify",Map.of("email",email,"verificationCode","012345")).statusCode()).isEqualTo(400);
        redis.delete(registration.key("code",email));
        String code=sendCode(b,email);
        var response=b.send("POST","/api/v1/auth/register",json.writeValueAsString(Map.of("email",email,"password",NEW,"verificationCode",code,"verified",true)),true);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("EMAIL_CODE_INVALID");
    }
    @Test void codeIsEmailBoundAndFiveBadGuessesInvalidateIt()throws Exception{
        String email=account(),other=account();Browser b=new Browser();b.csrf();String code=sendCode(b,email);
        assertThat(b.post("/verify",Map.of("email",other,"verificationCode",code)).statusCode()).isEqualTo(400);
        String wrong=code.equals("000000")?"999999":"000000";
        for(int i=0;i<5;i++)assertThat(b.post("/verify",Map.of("email",email,"verificationCode",wrong)).statusCode()).isEqualTo(400);
        assertThat(b.post("/verify",Map.of("email",email,"verificationCode",code)).statusCode()).isEqualTo(400);
        assertThat(redis.hasKey(store.key("code",email))).isFalse();
    }
    @Test void expiredCapabilityCannotChangePassword()throws Exception{
        String email=account();Browser b=new Browser();b.csrf();String token=grant(b,email);
        redis.expire(store.key("grant",token),Duration.ZERO);
        assertThat(b.post("",Map.of("resetToken",token,"newPassword",NEW)).statusCode()).isEqualTo(400);
        assertThat(passwords.matches(OLD,users.findLoginAccount(email).passwordHash())).isTrue();
    }
    @Test void simultaneousUseOfOneCapabilityHasExactlyOneWinner()throws Exception{
        String email=account();Browser b=new Browser();b.csrf();String token=grant(b,email);
        Browser first=new Browser(),second=new Browser();first.csrf();second.csrf();
        CountDownLatch start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->{start.await();return first.post("",Map.of("resetToken",token,"newPassword",NEW)).statusCode();});
            var c=pool.submit(()->{start.await();return second.post("",Map.of("resetToken",token,"newPassword","Other2026!")).statusCode();});
            start.countDown();assertThat(List.of(a.get(20,TimeUnit.SECONDS),c.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,400);
        }
    }
    @Test void oldCodeAndOtherGrantsBecomeInvalidAfterPasswordVersionChanges()throws Exception{
        String email=account();Browser b=new Browser();b.csrf();String first=grant(b,email);
        redis.delete(store.key("cooldown",email));String second=grant(b,email);
        redis.delete(store.key("cooldown",email));String outstanding=sendCode(b,email);
        assertThat(b.post("",Map.of("resetToken",first,"newPassword",NEW)).statusCode()).isEqualTo(200);
        assertThat(b.post("",Map.of("resetToken",second,"newPassword","Other2026!")).statusCode()).isEqualTo(400);
        assertThat(b.post("/verify",Map.of("email",email,"verificationCode",outstanding)).statusCode()).isEqualTo(400);
    }
    @Test void unknownAndExistingEmailsShareResponseAndCooldownIsServerEnforced()throws Exception{
        String email=account();Browser b=new Browser();b.csrf();
        var known=b.post("/email-code",Map.of("email",email));
        var unknown=b.post("/email-code",Map.of("email",UUID.randomUUID()+"@example.test"));
        assertThat(known.statusCode()).isEqualTo(202);assertThat(unknown.statusCode()).isEqualTo(202);
        assertThat(json.readTree(known.body()).path("data").path("resendAfterSeconds").asInt()).isEqualTo(json.readTree(unknown.body()).path("data").path("resendAfterSeconds").asInt());
        var limited=b.post("/email-code",Map.of("email",email));assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(Long.parseLong(limited.headers().firstValue("Retry-After").orElseThrow())).isPositive();
        awaitCode(email);
    }
    String account(){String email=Long.toUnsignedString(ThreadLocalRandom.current().nextLong())+"@bjtu.edu.cn";emails.add(email);
        jdbc.update("insert into foodtime.users(id,email,password_hash,display_name,email_verified_at) values(?,?,?,'测试账号',statement_timestamp())",UUID.randomUUID(),email,passwords.encode(OLD));return email;}
    String sendCode(Browser b,String email)throws Exception{assertThat(b.post("/email-code",Map.of("email",email)).statusCode()).isEqualTo(202);return awaitCode(email);}
    String awaitCode(String email)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<end){if(SMTP.message(email)!=null&&Boolean.TRUE.equals(redis.hasKey(store.key("code",email))))return SMTP.code(email);Thread.sleep(10);}
        throw new AssertionError("Loopback SMTP code not published");
    }
    String grant(Browser b,String email)throws Exception{
        String code=sendCode(b,email);var response=b.post("/verify",Map.of("email",email,"verificationCode",code));
        assertThat(response.statusCode()).isEqualTo(200);assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        return json.readTree(response.body()).path("data").path("resetToken").asString();
    }
    class Browser{
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();String csrf;
        void csrf()throws Exception{csrf=json.readTree(get("/api/v1/auth/csrf").body()).path("data").path("token").asString();}
        HttpResponse<String> get(String path)throws Exception{return send("GET",path,null,false);}
        HttpResponse<String> post(String suffix,Map<String,?> body)throws Exception{return send("POST","/api/v1/auth/password-reset"+suffix,json.writeValueAsString(body),true);}
        HttpResponse<String> login(String email,String password)throws Exception{return send("POST","/api/v1/auth/login",json.writeValueAsString(Map.of("account",email,"password",password)),true);}
        HttpResponse<String> send(String method,String path,String body,boolean withCsrf)throws Exception{
            var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15)).header("X-Foodtime-Client","web");
            if(withCsrf)request.header("X-CSRF-TOKEN",csrf);if(body!=null)request.header("Content-Type","application/json");
            return client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        }
    }
}
