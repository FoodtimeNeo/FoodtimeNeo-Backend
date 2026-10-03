package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.dish.service.DishRatingSummaryRefresher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "app.rating-summary.refresh-enabled=false")
class DatabaseSchemaIT {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void allDesignedTablesAndMaterializedViewExist() {
        assertThat(jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'foodtime' and table_name <> 'flyway_schema_history'
                """, String.class)).containsExactlyInAnyOrder("users", "dining_halls", "stalls", "dishes",
                "dish_images", "dish_review_submissions", "dish_reviews", "stall_submissions",
                "dish_submissions", "content_moderation_logs");
        assertThat(jdbc.queryForObject("""
                select ispopulated from pg_matviews
                where schemaname = 'foodtime' and matviewname = 'dish_rating_summary'
                """, Boolean.class)).isTrue();
    }

    @Test
    void emailMustBeNormalizedAndUnique() {
        rollback(status -> {
            Fixture fixture = seed();
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.users set email = 'UPPER@example.test' where id = ?", fixture.user()));
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.users set email = ' space@example.test ' where id = ?", fixture.user()));
            rejected(status, "23505", () -> jdbc.update(
                    "update foodtime.users set email = (select email from foodtime.users where id = ?) where id = ?",
                    fixture.user(), fixture.otherUser()));
        });
    }

    @Test
    void rolesStatusesAndPairedCoordinatesAreEnforced() {
        rollback(status -> {
            Fixture fixture = seed();
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.users set role = 'owner' where id = ?", fixture.user()));
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.users set status = 'deleted' where id = ?", fixture.user()));
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.dining_halls set latitude = 30 where id = ?", fixture.hall()));
            jdbc.update("update foodtime.dining_halls set latitude = 30, longitude = 120 where id = ?", fixture.hall());
        });
    }

    @Test
    void priceAndRatingBoundariesAreEnforced() {
        rollback(status -> {
            Fixture fixture = seed();
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.dishes set price = -0.01 where id = ?", fixture.dish()));
            jdbc.update("update foodtime.dishes set price = 0 where id = ?", fixture.dish());
            rejected(status, "23514", () -> review(fixture, fixture.user(), 0));
            rejected(status, "23514", () -> jdbc.update("""
                    insert into foodtime.dish_review_submissions (id, dish_id, user_id, rating, content)
                    values (?, ?, ?, 6, 'invalid rating')
                    """, UUID.randomUUID(), fixture.dish(), fixture.user()));
            review(fixture, fixture.user(), 1);
            review(fixture, fixture.otherUser(), 5);
        });
    }

    @Test
    void pendingReviewsAreUniqueWhileHistoryIsPreserved() {
        rollback(status -> {
            Fixture fixture = seed();
            UUID pending = reviewSubmission(fixture);
            rejected(status, "23505", () -> reviewSubmission(fixture));
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.dish_review_submissions set status = 'rejected' where id = ?", pending));
            jdbc.update("""
                    update foodtime.dish_review_submissions
                    set status = 'rejected', reviewed_by_user_id = ?, reviewed_at = now() where id = ?
                    """, fixture.moderator(), pending);
            UUID next = reviewSubmission(fixture);
            assertThat(next).isNotEqualTo(pending);
            assertThat(jdbc.queryForObject("""
                    select count(*) from foodtime.dish_review_submissions where dish_id = ? and user_id = ?
                    """, Integer.class, fixture.dish(), fixture.user())).isEqualTo(2);
        });
    }

    @Test
    void onlyOneFormalReviewPerDishAndUserIsAllowed() {
        rollback(status -> {
            Fixture fixture = seed();
            review(fixture, fixture.user(), 4);
            rejected(status, "23505", () -> review(fixture, fixture.user(), 5));
        });
    }

    @Test
    void approvedDishesRequirePriceReviewerAndFormalRecord() {
        rollback(status -> {
            Fixture fixture = seed();
            UUID submission = dishSubmission(fixture);
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.dish_submissions set approved_dish_id = ? where id = ?", fixture.dish(), submission));
            rejected(status, "23514", () -> jdbc.update("""
                    update foodtime.dish_submissions set status = 'approved', approved_dish_id = ?,
                    reviewed_by_user_id = ?, reviewed_at = now() where id = ?
                    """, fixture.dish(), fixture.moderator(), submission));
            jdbc.update("""
                    update foodtime.dish_submissions set status = 'approved', approved_dish_id = ?, price = 10,
                    reviewed_by_user_id = ?, reviewed_at = now() where id = ?
                    """, fixture.dish(), fixture.moderator(), submission);
        });
    }

    @Test
    void approvedStallsRequireReviewMetadataAndConsistentLinks() {
        rollback(status -> {
            Fixture fixture = seed();
            UUID submission = UUID.randomUUID();
            jdbc.update("""
                    insert into foodtime.stall_submissions
                    (id, dining_hall_id, name, cover_image_url, submitted_by_user_id)
                    values (?, ?, 'submitted stall', '/test/stall.png', ?)
                    """, submission, fixture.hall(), fixture.user());
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.stall_submissions set status = 'approved' where id = ?", submission));
            rejected(status, "23514", () -> jdbc.update("""
                    update foodtime.stall_submissions set status = 'approved', approved_stall_id = ? where id = ?
                    """, fixture.stall(), submission));
            jdbc.update("""
                    update foodtime.stall_submissions set status = 'approved', approved_stall_id = ?,
                    reviewed_by_user_id = ?, reviewed_at = now() where id = ?
                    """, fixture.stall(), fixture.moderator(), submission);
            rejected(status, "23514", () -> jdbc.update(
                    "update foodtime.stall_submissions set status = 'rejected' where id = ?", submission));
        });
    }

    @Test
    void moderationLogsReferenceExactlyOneSubmission() {
        rollback(status -> {
            Fixture fixture = seed();
            UUID reviewSubmission = reviewSubmission(fixture);
            UUID dishSubmission = dishSubmission(fixture);
            rejected(status, "23514", () -> jdbc.update("""
                    insert into foodtime.content_moderation_logs (id, moderator_user_id, action)
                    values (?, ?, 'approve')
                    """, UUID.randomUUID(), fixture.moderator()));
            rejected(status, "23514", () -> jdbc.update("""
                    insert into foodtime.content_moderation_logs
                    (id, moderator_user_id, action, review_submission_id, dish_submission_id)
                    values (?, ?, 'approve', ?, ?)
                    """, UUID.randomUUID(), fixture.moderator(), reviewSubmission, dishSubmission));
            jdbc.update("""
                    insert into foodtime.content_moderation_logs
                    (id, moderator_user_id, action, review_submission_id) values (?, ?, 'reject', ?)
                    """, UUID.randomUUID(), fixture.moderator(), reviewSubmission);
        });
    }

    @Test
    void foreignKeysRejectOrphansAndRestrictParentDeletion() {
        rollback(status -> {
            Fixture fixture = seed();
            // PostgreSQL 18 reports RESTRICT as 23001; earlier versions can report 23503.
            rejected(status, "23503,23001", () -> jdbc.update("delete from foodtime.dining_halls where id = ?", fixture.hall()));
            rejected(status, "23503", () -> jdbc.update(
                    "update foodtime.dishes set stall_id = ? where id = ?", UUID.randomUUID(), fixture.dish()));
        });
    }

    @Test
    void updatesMaintainTheLastModifiedTimestamp() {
        rollback(status -> {
            // An old timestamp on INSERT provides a deterministic baseline without sleeps.
            UUID user = UUID.randomUUID();
            jdbc.update("""
                    insert into foodtime.users (id, email, password_hash, display_name, updated_at)
                    values (?, ?, 'test-hash', 'old name', '2000-01-01T00:00:00Z')
                    """, user, user + "@example.test");
            jdbc.update("update foodtime.users set display_name = 'new name' where id = ?", user);
            assertThat(jdbc.queryForObject("select updated_at from foodtime.users where id = ?", OffsetDateTime.class, user))
                    .isAfter(OffsetDateTime.parse("2000-01-01T00:00:00Z"));
            assertThat(jdbc.queryForObject("""
                    select count(*) from pg_trigger t
                    join pg_class c on c.oid = t.tgrelid join pg_namespace n on n.oid = c.relnamespace
                    where n.nspname = 'foodtime' and not t.tgisinternal
                    """, Integer.class)).isEqualTo(8);
        });
    }

    @Test
    void summaryCountsOnlyFormalReviewsAndIncludesUnratedDishes() {
        rollback(status -> {
            Fixture fixture = seed();
            review(fixture, fixture.user(), 3);
            review(fixture, fixture.otherUser(), 5);
            reviewSubmission(fixture);
            new DishRatingSummaryRefresher(jdbc, new TransactionTemplate(transactionManager)).refresh();
            assertThat(jdbc.queryForObject("select average_score from foodtime.dish_rating_summary where dish_id = ?",
                    BigDecimal.class, fixture.dish())).isEqualByComparingTo("4.00");
            assertThat(jdbc.queryForObject("select review_count from foodtime.dish_rating_summary where dish_id = ?",
                    Long.class, fixture.dish())).isEqualTo(2L);
            assertThat(jdbc.queryForObject("select average_score from foodtime.dish_rating_summary where dish_id = ?",
                    BigDecimal.class, fixture.unratedDish())).isNull();
            assertThat(jdbc.queryForObject("select review_count from foodtime.dish_rating_summary where dish_id = ?",
                    Long.class, fixture.unratedDish())).isZero();
        });
    }

    private void rollback(Consumer<TransactionStatus> scenario) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            try {
                scenario.accept(status);
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    private void rejected(TransactionStatus status, String sqlState, Runnable statement) {
        Object savepoint = status.createSavepoint();
        try {
            Throwable exception = catchThrowable(statement::run);
            assertThat(exception).isInstanceOf(DataAccessException.class);
            Throwable cause = ((DataAccessException) exception).getMostSpecificCause();
            assertThat(cause).isInstanceOf(SQLException.class);
            assertThat(sqlState.split(",")).contains(((SQLException) cause).getSQLState());
        } finally {
            status.rollbackToSavepoint(savepoint);
            status.releaseSavepoint(savepoint);
        }
    }

    private Fixture seed() {
        Fixture fixture = new Fixture(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        for (UUID user : List.of(fixture.user(), fixture.otherUser(), fixture.moderator())) {
            jdbc.update("""
                    insert into foodtime.users (id, email, password_hash, display_name, role)
                    values (?, ?, 'test-hash', 'test user', ?)
                    """, user, user + "@example.test", user.equals(fixture.moderator()) ? "admin" : "user");
        }
        jdbc.update("insert into foodtime.dining_halls (id, name) values (?, 'test hall')", fixture.hall());
        jdbc.update("insert into foodtime.stalls (id, dining_hall_id, name) values (?, ?, 'test stall')", fixture.stall(), fixture.hall());
        for (UUID dish : List.of(fixture.dish(), fixture.unratedDish())) {
            jdbc.update("insert into foodtime.dishes (id, stall_id, name, price) values (?, ?, 'test dish', 10)", dish, fixture.stall());
        }
        return fixture;
    }

    private void review(Fixture fixture, UUID user, int rating) {
        jdbc.update("insert into foodtime.dish_reviews (id, dish_id, user_id, rating, content) values (?, ?, ?, ?, 'test review')",
                UUID.randomUUID(), fixture.dish(), user, rating);
    }

    private UUID reviewSubmission(Fixture fixture) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into foodtime.dish_review_submissions (id, dish_id, user_id, rating, content)
                values (?, ?, ?, 1, 'pending review')
                """, id, fixture.dish(), fixture.user());
        return id;
    }

    private UUID dishSubmission(Fixture fixture) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into foodtime.dish_submissions (id, stall_id, name, image_url, submitted_by_user_id)
                values (?, ?, 'submitted dish', '/test/dish.png', ?)
                """, id, fixture.stall(), fixture.user());
        return id;
    }

    private record Fixture(UUID user, UUID otherUser, UUID moderator, UUID hall, UUID stall, UUID dish, UUID unratedDish) { }
}
