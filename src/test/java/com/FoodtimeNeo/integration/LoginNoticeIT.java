package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real PostgreSQL migration, HTTP/session/CSRF and cross-client preference semantics. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginNoticeIT extends IntegrationApiSupport {
    @AfterEach
    void cleanup() {
        jdbc.update("UPDATE foodtime.config SET notice_id = NULL WHERE id=1");
        for (UUID user : users) {
            jdbc.update("DELETE FROM foodtime.notice_reads WHERE user_id=?", user);
            jdbc.update("DELETE FROM foodtime.notices WHERE created_by=?", user);
        }
    }
    private UUID notice(String content) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.notices(notice_id,content,created_by) VALUES (?,?,?)",id,content,owner.id());
        return id;
    }
    private void activate(UUID id) { jdbc.update("UPDATE foodtime.config SET notice_id=? WHERE id=1",id); }

    @Test
    void configHasExactlyTwoColumnsAndOneFixedRow() {
        assertThat(jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema='foodtime' AND table_name='config' ORDER BY ordinal_position",String.class))
                .containsExactly("id","notice_id");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.config",Long.class)).isEqualTo(1);
        assertThatThrownBy(()->jdbc.update("INSERT INTO foodtime.config(id) VALUES(2)"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema='foodtime' AND table_name='notices'",String.class))
                .contains("notice_id","created_time","content").doesNotContain("title","body");
    }
    @Test
    void confirmationSeparatesReadFromSuppressionAndIsSharedPerUser() throws Exception {
        Browser user=login(owner), secondClient=login(owner), stranger=login(other);
        assertThat(body(user.call("GET","/api/v1/users/me/notices/current",null)).path("data").isNull()).isTrue();
        UUID id=notice("# 公告\n\n**重要**\n\n| A | B |\n|---|---|\n| 1 | 2 |");
        activate(id);
        var current=body(user.call("GET","/api/v1/users/me/notices/current",null)).path("data");
        assertThat(current.path("id").asString()).isEqualTo(id.toString());
        assertThat(current.path("content").asString()).startsWith("# 公告");
        assertThat(current.has("title")).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.notice_reads WHERE user_id=?",Long.class,owner.id())).isZero();
        String path="/api/v1/users/me/notices/"+id+"/confirm";
        assertThat(user.call("POST",path,Map.of()).statusCode()).isEqualTo(400);
        assertThat(user.call("POST",path,Map.of("dontShowAgain",false)).statusCode()).isEqualTo(200);
        assertThat(body(secondClient.call("GET","/api/v1/users/me/notices/current",null)).path("data").path("isRead").asBoolean()).isTrue();
        assertThat(user.call("POST",path,Map.of("dontShowAgain",true)).statusCode()).isEqualTo(200);
        // A late unchecked confirmation or the legacy read endpoint cannot clear suppression.
        secondClient.call("POST",path,Map.of("dontShowAgain",false));
        secondClient.call("POST","/api/v1/users/me/notices/"+id+"/read",Map.of());
        assertThat(body(secondClient.call("GET","/api/v1/users/me/notices/current",null)).path("data").isNull()).isTrue();
        assertThat(body(stranger.call("GET","/api/v1/users/me/notices/current",null)).path("data").path("id").asString()).isEqualTo(id.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.notice_reads WHERE notice_id=? AND user_id=?",Long.class,id,owner.id())).isEqualTo(1);
        UUID next=notice("# 下一条");
        activate(next);
        user.call("POST",path,Map.of("dontShowAgain",true));
        assertThat(body(user.call("GET","/api/v1/users/me/notices/current",null)).path("data").path("id").asString()).isEqualTo(next.toString());
        jdbc.update("DELETE FROM foodtime.notices WHERE notice_id=?",next);
        assertThat(body(user.call("GET","/api/v1/users/me/notices/current",null)).path("data").isNull()).isTrue();
    }
    @Test
    void ordinaryAdminCannotPublishNotices() throws Exception {
        Browser admin=login(account("admin"));
        assertThat(admin.call("POST","/api/v1/admin/notices",Map.of("title","t","body","b")).statusCode()).isEqualTo(403);
        assertThat(admin.call("GET","/api/v1/admin/notices",null).statusCode()).isEqualTo(403);
        Browser superadmin=login(account("superadmin"));
        assertThat(superadmin.call("POST","/api/v1/admin/notices",Map.of("title","t","body","b")).statusCode()).isEqualTo(201);
    }
}
