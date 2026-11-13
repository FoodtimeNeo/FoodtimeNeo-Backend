package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.user.lifecycle.EmailCooldowns;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Open-mode HTTP/CSRF/session and real SMTP checks; existing suites retain closed-mode coverage. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"app.auth.cookie-secure=false", "app.email-verification.mailbox.enabled=false", "app.ai.single.enabled=false"})
@TestPropertySource(properties="app.participation.school-email-required=false")
class OpenParticipationIT extends IntegrationApiSupport {
    private static final CapturingSmtpServer SMTP = new CapturingSmtpServer();
    private final Set<String> mailboxes = new HashSet<>();
    @DynamicPropertySource static void mail(DynamicPropertyRegistry p) {
        p.add("app.email-verification.enabled", () -> true);
        p.add("app.email-verification.from", () -> "sender@example.test");
        p.add("app.email-verification.secret", () -> "open-participation-test-secret-at-least-32-characters");
        p.add("app.email-verification.redis-namespace", () -> "foodtime:it:open-mail:"+SMTP.port());
        p.add("app.auth.registration.mode", () -> "open");
        p.add("spring.mail.host", () -> "127.0.0.1");p.add("spring.mail.port", SMTP::port);
        p.add("spring.mail.properties[mail.smtp.localhost]", () -> "localhost");
        p.add("spring.mail.properties[mail.smtp.auth]", () -> false);
        p.add("spring.mail.properties[mail.smtp.ssl.enable]", () -> false);
        p.add("spring.mail.properties[mail.smtp.starttls.enable]", () -> false);
        p.add("spring.mail.properties[mail.smtp.starttls.required]", () -> false);
    }
    @AfterAll static void closeSmtp() throws Exception { SMTP.close(); }
    @AfterEach void cleanupOpen() {
        for(var id:users) jdbc.update("DELETE FROM foodtime.user_feedback WHERE user_id=?",id);
        for(var email:mailboxes) jdbc.update("DELETE FROM foodtime.email_cooldowns WHERE email_hash=?",EmailCooldowns.key(email));
        SMTP.clear();
    }
    private Account nonSchool() {
        var account = account("visitor");
        String email = account.id()+"@gmail.com";
        jdbc.update("UPDATE foodtime.users SET email=? WHERE id=?", email, account.id());
        mailboxes.add(email);return new Account(account.id(),email);
    }
    private byte[] png() throws Exception {
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);
        return output.toByteArray();
    }
    @Test void existingNonSchoolAccountsReceiveOrdinaryAccessAndCannotManageOtherUsers() throws Exception {
        var account=nonSchool();var browser=login(account);
        for(String path:List.of("/api/v1/auth/me","/api/v1/users/me/profile")) {
            var result=body(browser.call("GET",path,null)).path("data");
            assertThat(result.path("role").asString()).isEqualTo("user");
        }
        var profile=body(browser.call("GET","/api/v1/users/me/profile",null)).path("data");
        assertThat(profile.path("schoolEmail").isNull()).isTrue();
        assertThat(profile.path("schoolEmailVerified").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("SELECT role FROM foodtime.users WHERE id=?",String.class,account.id())).isEqualTo("visitor");
        assertThat(browser.call("PUT","/api/v1/users/"+other.id()+"/follow",null).statusCode()).isEqualTo(200);
        var conversation=browser.call("POST","/api/v1/users/me/conversations",Map.of("userId",other.id()));
        assertThat(conversation.statusCode()).as(conversation.body()).isEqualTo(200);
        String id=body(conversation).path("data").path("id").asString();
        assertThat(browser.call("POST","/api/v1/users/me/conversations/"+id+"/messages",Map.of("content","你好","clientRequestId",UUID.randomUUID())).statusCode()).isEqualTo(200);
        assertThat(browser.call("PUT","/api/v1/users/me/dish-favorites/"+dish,null).statusCode()).isEqualTo(200);
        var capabilities=body(browser.call("GET","/api/v1/users/me/capabilities",null)).path("data").path("capabilities");
        assertThat(capabilities.toString()).contains("correction.submit").doesNotContain("catalog.edit","report.review","dish-price-history.manage");
        assertThat(browser.call("GET","/api/v1/admin/feedback",null).statusCode()).isEqualTo(403);
        assertThat(browser.call("GET","/api/v1/admin/users",null).statusCode()).isEqualTo(403);
    }
    @Test void postingUploadingAndCorrectionsUseTheExistingModerationRules() throws Exception {
        var browser=login(nonSchool());
        var post=browser.call("POST","/api/v1/community/posts",communityPost(browser,"普通邮箱的食记"));
        assertThat(post.statusCode()).as(post.body()).isEqualTo(202);
        UUID postId=UUID.fromString(body(post).path("data").path("id").asString());
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.community_posts WHERE id=?",String.class,postId)).isEqualTo("pending");
        assertThat(browser.upload(png()).statusCode()).isEqualTo(201);
        long revision=body(browser.call("GET","/api/v1/dishes/"+dish,null)).path("data").path("revision").asLong();
        var correction=browser.call("POST","/api/v1/users/me/catalog-corrections",Map.of(
                "targetType","dish","targetId",dish,"baseRevision",revision,"changedFields",List.of("name"),
                "name","更正菜名","reason","菜单已变更","evidenceUrls",List.of(),"idempotencyKey",UUID.randomUUID().toString()));
        assertThat(correction.statusCode()).as(correction.body()).isEqualTo(201);
        assertThat(body(correction).path("data").path("status").asString()).isEqualTo("pending");
    }
    @Test void nonSchoolFeedbackUsesTheSameFivePerDayLimitAndRetriesRemainIdempotent() throws Exception {
        var browser=login(nonSchool());var first=Map.of("type","usage","content","反馈一","clientRequestId",UUID.randomUUID());
        assertThat(browser.call("POST","/api/v1/users/me/feedback",first).statusCode()).isEqualTo(200);
        assertThat(browser.call("POST","/api/v1/users/me/feedback",first).statusCode()).isEqualTo(200);
        for(int i=2;i<=5;i++)assertThat(browser.call("POST","/api/v1/users/me/feedback",Map.of("type","usage","content","反馈"+i,"clientRequestId",UUID.randomUUID())).statusCode()).isEqualTo(200);
        var sixth=browser.call("POST","/api/v1/users/me/feedback",Map.of("type","usage","content","反馈六","clientRequestId",UUID.randomUUID()));
        assertThat(sixth.statusCode()).isEqualTo(429);assertThat(body(sixth).path("code").asString()).isEqualTo("FEEDBACK_DAILY_LIMIT");
    }
    @Test void bindingAndUnbindingKeepOrdinaryAccessInBothExistingSessions() throws Exception {
        var account=nonSchool();var first=login(account);var second=login(account);
        String school=Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits())+"@bjtu.edu.cn";mailboxes.add(school);
        assertThat(first.call("POST","/api/v1/auth/bind-email/email-code",Map.of("email",school)).statusCode()).isEqualTo(202);
        var bound=first.call("POST","/api/v1/users/me/bind-email",Map.of("email",school,"verificationCode",SMTP.code(school)));
        assertThat(bound.statusCode()).as(bound.body()).isEqualTo(200);
        assertThat(body(bound).path("data").path("schoolEmailVerified").asBoolean()).isTrue();
        String base="/api/v1/users/me/account-lifecycle";
        assertThat(first.call("POST",base+"/unbind-code",null).statusCode()).isEqualTo(202);
        var unbound=first.call("POST",base+"/unbind",Map.of("verificationCode",SMTP.code(school)));
        assertThat(unbound.statusCode()).as(unbound.body()).isEqualTo(200);
        var profile=body(unbound).path("data").path("profile");
        assertThat(profile.path("role").asString()).isEqualTo("user");
        assertThat(profile.path("schoolEmailVerified").asBoolean()).isFalse();assertThat(profile.path("schoolEmail").isNull()).isTrue();
        assertThat(body(second.call("GET","/api/v1/auth/me",null)).path("data").path("role").asString()).isEqualTo("user");
        assertThat(second.call("PUT","/api/v1/users/me/dish-favorites/"+dish,null).statusCode()).isEqualTo(200);
        assertThat(first.call("POST","/api/v1/auth/bind-email/email-code",Map.of("email",school)).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT role FROM foodtime.users WHERE id=?",String.class,account.id())).isEqualTo("visitor");
    }
    @Test void newNonSchoolRegistrationReportsEffectiveAccessAndPreservesUnverifiedSchoolState() throws Exception {
        String email=UUID.randomUUID()+"@gmail.com";mailboxes.add(email);var browser=new Browser();browser.csrf();
        assertThat(browser.call("POST","/api/v1/auth/register/email-code",Map.of("email",email)).statusCode()).isEqualTo(202);
        var registered=browser.call("POST","/api/v1/auth/register",Map.of("email",email,"password",PASSWORD,"verificationCode",SMTP.code(email)));
        assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
        var data=body(registered).path("data");UUID id=UUID.fromString(data.path("id").asString());users.add(id);
        assertThat(data.path("role").asString()).isEqualTo("user");
        assertThat(jdbc.queryForObject("SELECT role FROM foodtime.users WHERE id=?",String.class,id)).isEqualTo("visitor");
        assertThat(body(login(new Account(id,email)).call("GET","/api/v1/auth/me",null)).path("data").path("role").asString()).isEqualTo("user");
    }
    @Test void disabledAndAnonymousAccountsAreNotGrantedOrdinaryWriteAccess() throws Exception {
        var account=nonSchool();jdbc.update("UPDATE foodtime.users SET status='disabled' WHERE id=?",account.id());
        var restricted=login(account);
        assertThat(body(restricted.call("GET","/api/v1/auth/me",null)).path("data").path("sessionMode").asString()).isEqualTo("appeal_only");
        assertThat(restricted.call("POST","/api/v1/community/posts",Map.of()).statusCode()).isEqualTo(403);
        assertThat(restricted.call("GET","/api/v1/users/me/profile",null).statusCode()).isEqualTo(403);
        var anonymous=new Browser();anonymous.csrf();
        assertThat(anonymous.call("PUT","/api/v1/users/me/dish-favorites/"+dish,null).statusCode()).isIn(401,403);
        for(String role:List.of("admin","superadmin")) {
            var manager=login(account(role));
            assertThat(body(manager.call("GET","/api/v1/auth/me",null)).path("data").path("role").asString()).isEqualTo(role);
            assertThat(manager.call("POST","/api/v1/auth/bind-email/email-code",Map.of("email","20260001@bjtu.edu.cn")).statusCode()).isEqualTo(403);
        }
    }
}
