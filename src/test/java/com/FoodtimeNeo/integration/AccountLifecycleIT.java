package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import com.FoodtimeNeo.user.lifecycle.EmailCooldowns;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Real SMTP, PostgreSQL, Redis, HTTP cookies/CSRF and server-side clock boundaries. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.auth.cookie-secure=false","app.email-verification.mailbox.enabled=false","app.ai.single.enabled=false"})
class AccountLifecycleIT extends IntegrationApiSupport {
    private static final String ROOT="/api/v1/users/me/account-lifecycle";
    private static final CapturingSmtpServer SMTP=new CapturingSmtpServer();
    @MockitoBean Clock clock;
    @Autowired EmailCooldowns cooldowns;
    private final Instant initial=Instant.parse("2026-11-11T15:58:00Z");
    private final Set<String> addresses=new HashSet<>();
    @DynamicPropertySource static void smtp(DynamicPropertyRegistry p){
        p.add("app.email-verification.enabled",()->true);p.add("app.email-verification.from",()->"sender@example.test");
        p.add("app.email-verification.secret",()->"account-lifecycle-test-only-secret-32-characters");
        p.add("app.email-verification.redis-namespace",()->"foodtime:it:account-mail:"+SMTP.port());
        p.add("app.auth.registration.mode",()->"open");
        p.add("spring.mail.host",()->"127.0.0.1");p.add("spring.mail.port",SMTP::port);
        p.add("spring.mail.properties[mail.smtp.localhost]",()->"localhost");
        p.add("spring.mail.properties[mail.smtp.auth]",()->false);p.add("spring.mail.properties[mail.smtp.ssl.enable]",()->false);
        p.add("spring.mail.properties[mail.smtp.starttls.enable]",()->false);p.add("spring.mail.properties[mail.smtp.starttls.required]",()->false);
    }
    @BeforeEach void time(){when(clock.instant()).thenReturn(initial);when(clock.getZone()).thenReturn(ZoneOffset.UTC);addresses.add(owner.email());addresses.add(other.email());}
    @AfterAll static void stop()throws Exception{SMTP.close();}
    @AfterEach void cleanup(){for(UUID user:users)jdbc.update("DELETE FROM foodtime.user_feedback WHERE user_id=?",user);for(String email:addresses)jdbc.update("DELETE FROM foodtime.email_cooldowns WHERE email_hash=?",EmailCooldowns.key(email));SMTP.clear();}
    private String school(){String email=Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits())+"@bjtu.edu.cn";addresses.add(email);return email;}
    private void bind(Account user,String school){jdbc.update("UPDATE foodtime.users SET school_email=?,school_email_verified_at=now() WHERE id=?",school,user.id());}
    private String send(Browser browser,String purpose,String email)throws Exception{var response=browser.call("POST",ROOT+"/"+purpose+"-code",Map.of());assertThat(response.statusCode()).as(response.body()).isEqualTo(202);assertThat(body(response).path("data").path("verificationEmail").asString()).isEqualTo(email);assertThat(response.body()).doesNotContain("code_hash","password_version");return SMTP.code(email);}
    private Map<String,String> code(String value){return Map.of("verificationCode",value);}
    private void setTime(Instant value){when(clock.instant()).thenReturn(value);}
    @Test void administratorsCannotSendConfirmOrBindEvenUsingDirectRequests()throws Exception{
        for(String role:List.of("admin","superadmin")){
            Browser browser=login(account(role));var summary=body(browser.call("GET",ROOT,null)).path("data");assertThat(summary.path("canDelete").asBoolean()).isFalse();
            for(String action:List.of("deletion-code","unbind-code","delete","unbind"))assertThat(browser.call("POST",ROOT+"/"+action,code("012345")).statusCode()).as(action).isEqualTo(403);
            assertThat(browser.call("POST","/api/v1/auth/bind-email/email-code",Map.of("email",school())).statusCode()).isEqualTo(403);
            assertThat(browser.call("POST","/api/v1/users/me/bind-email",Map.of("email",school(),"verificationCode","012345")).statusCode()).isEqualTo(403);
        }
    }
    @Test void sendsToBoundSchoolAndAnSmtpFailureDoesNotConsumeTheDailyAllowance()throws Exception{
        String school=school();bind(owner,school);Browser browser=login(owner);SMTP.rejectNext.set(true);
        assertThat(browser.call("POST",ROOT+"/deletion-code",Map.of()).statusCode()).isEqualTo(503);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.account_email_actions WHERE user_id=?",Long.class,owner.id())).isZero();
        send(browser,"deletion",school);assertThat(SMTP.message(owner.email())).isNull();
        var second=browser.call("POST",ROOT+"/deletion-code",Map.of());assertThat(second.statusCode()).isEqualTo(429);assertThat(body(second).path("message").asString()).contains("明天再试");
        assertThat(body(browser.call("GET",ROOT,null)).path("data").path("deletionCodeAvailableAt").asString()).isEqualTo("2026-11-11T16:00:00Z");
    }
    @Test void unbindingKeepsTheAccountDowngradesEverySessionAndHoldsTheSchoolForExactlyOneHour()throws Exception{
        String school=school();bind(owner,school);Browser first=login(owner),second=login(owner);String proof=send(first,"unbind",school);
        var response=first.call("POST",ROOT+"/unbind",code(proof));assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(body(response).path("data").path("profile").path("role").asString()).isEqualTo("visitor");
        assertThat(body(second.call("GET","/api/v1/auth/me",null)).path("data").path("role").asString()).isEqualTo("visitor");
        assertThat(second.call("POST","/api/v1/community/posts",Map.of()).statusCode()).isEqualTo(403);
        assertThat(first.call("POST","/api/v1/auth/bind-email/email-code",Map.of("email",school)).statusCode()).isEqualTo(409);
        assertThat(first.call("POST","/api/v1/users/me/bind-email",Map.of("email",school,"verificationCode",proof)).statusCode()).isEqualTo(409);
        assertThat(login(owner).call("GET",ROOT,null).statusCode()).isEqualTo(200);
        setTime(initial.plusSeconds(3599));assertThatThrownBy(()->cooldowns.requireAvailable(school)).extracting(e->((com.FoodtimeNeo.common.exception.BusinessException)e).getCode()).isEqualTo("EMAIL_COOLING_DOWN");
        setTime(initial.plusSeconds(3600));cooldowns.requireAvailable(school);
        assertThat(first.call("POST","/api/v1/auth/bind-email/email-code",Map.of("email",school)).statusCode()).isEqualTo(202);
        var rebound=first.call("POST","/api/v1/users/me/bind-email",Map.of("email",school,"verificationCode",SMTP.code(school)));assertThat(rebound.statusCode()).as(rebound.body()).isEqualTo(200);
        assertThat(body(rebound).path("data").path("role").asString()).isEqualTo("user");
    }
    @Test void deletionInvalidatesAllSessionsAnonymizesContributionsAndDoesNotTransferDataToReregistration()throws Exception{
        String school=school();bind(owner,school);Browser first=login(owner),second=login(owner),reader=login(other);UUID post=UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.community_posts(id,user_id,title,content,status,review_stage,reviewed_by_user_id,reviewed_at) VALUES (?,?,'保留食记','注销后的历史内容','approved','completed',?,now())",post,owner.id(),other.id());
        first.call("POST","/api/v1/users/me/feedback",Map.of("type","function","content","保留反馈","clientRequestId",UUID.randomUUID()));
        String proof=send(first,"deletion",school);var deleted=first.call("POST",ROOT+"/delete",code(proof));assertThat(deleted.statusCode()).as(deleted.body()).isEqualTo(200);
        assertThat(first.call("GET","/api/v1/auth/me",null).statusCode()).isEqualTo(401);assertThat(second.call("GET","/api/v1/auth/me",null).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT display_name FROM foodtime.users WHERE id=?",String.class,owner.id())).isEqualTo("已注销用户");
        var retained=reader.call("GET","/api/v1/community/posts/"+post,null);assertThat(retained.statusCode()).as(retained.body()).isEqualTo(200);assertThat(retained.body()).contains("已注销用户","注销后的历史内容");
        for(String email:List.of(owner.email(),school))assertThatThrownBy(()->cooldowns.requireAvailable(email)).isInstanceOf(com.FoodtimeNeo.common.exception.BusinessException.class);
        Browser fresh=new Browser();fresh.csrf();assertThat(fresh.call("POST","/api/v1/auth/register/email-code",Map.of("email",owner.email())).statusCode()).isEqualTo(409);
        setTime(initial.plusSeconds(3600));
        assertThat(fresh.call("POST","/api/v1/auth/register/email-code",Map.of("email",owner.email())).statusCode()).isEqualTo(202);
        var registration=fresh.call("POST","/api/v1/auth/register",Map.of("email",owner.email(),"password",PASSWORD,"verificationCode",SMTP.code(owner.email())));assertThat(registration.statusCode()).as(registration.body()).isEqualTo(201);
        UUID newId=UUID.fromString(body(registration).path("data").path("id").asString());users.add(newId);assertThat(newId).isNotEqualTo(owner.id());
        Browser newAccount=login(new Account(newId,owner.email()));assertThat(body(newAccount.call("GET","/api/v1/users/me/feedback",null)).path("data").path("total").asInt()).isZero();
        assertThat(newAccount.call("POST",ROOT+"/deletion-code",Map.of()).statusCode()).as("new account has independent deletion allowance").isEqualTo(202);
    }
    @Test void onlyTheCurrentAccountCurrentEmailAndCurrentPasswordVersionCanUseAProof()throws Exception{
        String school=school();bind(owner,school);Browser first=login(owner),second=login(other);String proof=send(first,"deletion",school);
        assertThat(second.call("POST",ROOT+"/delete",code(proof)).statusCode()).isEqualTo(400);
        assertThat(first.call("POST",ROOT+"/unbind",code(proof)).statusCode()).isEqualTo(400);
        jdbc.update("UPDATE foodtime.users SET school_email=? WHERE id=?",school(),owner.id());assertThat(first.call("POST",ROOT+"/delete",code(proof)).statusCode()).isEqualTo(400);
        jdbc.update("UPDATE foodtime.users SET school_email=?,password_changed_at=password_changed_at+interval '1 microsecond' WHERE id=?",school,owner.id());
        Browser renewed=login(owner);assertThat(renewed.call("POST",ROOT+"/delete",code(proof)).statusCode()).isEqualTo(400);
    }
    @Test void fiveBadGuessesArePersistedAndInvalidateEvenTheCorrectCode()throws Exception{
        Browser first=login(owner);String proof=send(first,"deletion",owner.email()),wrong=proof.equals("000000")?"999999":"000000";
        for(int i=0;i<5;i++)assertThat(first.call("POST",ROOT+"/delete",code(wrong)).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT attempts FROM foodtime.account_email_actions WHERE user_id=? AND purpose='delete'",Integer.class,owner.id())).isEqualTo(5);
        assertThat(first.call("POST",ROOT+"/delete",code(proof)).statusCode()).isEqualTo(400);
    }
    @Test void expiryAndUtcPlusEightMidnightUseIndependentRules()throws Exception{
        Browser first=login(owner);send(first,"deletion",owner.email());setTime(Instant.parse("2026-11-11T15:59:59Z"));
        assertThat(first.call("POST",ROOT+"/deletion-code",Map.of()).statusCode()).isEqualTo(429);
        setTime(Instant.parse("2026-11-11T16:00:00Z"));String newer=send(first,"deletion",owner.email());
        setTime(Instant.parse("2026-11-11T16:15:00Z"));assertThat(first.call("POST",ROOT+"/delete",code(newer)).statusCode()).isEqualTo(400);
        assertThat(first.call("POST",ROOT+"/deletion-code",Map.of()).statusCode()).isEqualTo(429);
    }
    @Test void concurrentSendsAdmitOnlyOneAndStoreHashesInsteadOfPlaintext()throws Exception{
        Browser first=login(owner),second=login(owner);var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->{barrier.await();return first.call("POST",ROOT+"/deletion-code",Map.of()).statusCode();});
            var b=pool.submit(()->{barrier.await();return second.call("POST",ROOT+"/deletion-code",Map.of()).statusCode();});
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).contains(202).allMatch(s->s==202||s==409||s==429);
            assertThat(List.of(a.get(),b.get()).stream().filter(s->s==202).count()).isEqualTo(1);
        }
        String proof=SMTP.code(owner.email());assertThat(jdbc.queryForObject("SELECT code_hash FROM foodtime.account_email_actions WHERE user_id=? AND purpose='delete'",String.class,owner.id())).matches("[a-f0-9]{64}").isNotEqualTo(proof);
    }
    @Test void concurrentDeletionHasOneCommitAndDeletedIdentitiesCannotBeRestored()throws Exception{
        Browser first=login(owner),second=login(owner),admin=login(account("superadmin"));String proof=send(first,"deletion",owner.email());var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->{barrier.await();return first.call("POST",ROOT+"/delete",code(proof)).statusCode();});var b=pool.submit(()->{barrier.await();return second.call("POST",ROOT+"/delete",code(proof)).statusCode();});
            assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).contains(200).allMatch(s->s==200||s==401||s==400);
            assertThat(List.of(a.get(),b.get()).stream().filter(s->s==200).count()).isEqualTo(1);
        }
        assertThat(admin.call("PUT","/api/v1/admin/users/"+owner.id(),Map.of("role","user","status","active")).statusCode()).isEqualTo(409);
        assertThatThrownBy(()->jdbc.update("UPDATE foodtime.users SET status='active',deleted_at=NULL WHERE id=?",owner.id())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void promotionAfterSendingAProofPreventsTheAccountAction()throws Exception{
        Browser first=login(owner);String proof=send(first,"deletion",owner.email());jdbc.update("UPDATE foodtime.users SET role='admin' WHERE id=?",owner.id());
        assertThat(first.call("POST",ROOT+"/delete",code(proof)).statusCode()).isEqualTo(403);assertThat(jdbc.queryForObject("SELECT status FROM foodtime.users WHERE id=?",String.class,owner.id())).isEqualTo("active");
    }
}
