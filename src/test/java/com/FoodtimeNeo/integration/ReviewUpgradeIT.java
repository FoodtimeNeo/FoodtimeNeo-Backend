package com.FoodtimeNeo.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.net.URI;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** Upgrade real populated old schemas in disposable databases; never clean the configured database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewUpgradeIT extends IntegrationApiSupport {
    @Autowired Environment environment;

    @ParameterizedTest
    @ValueSource(strings = {"6", "14"})
    void oldReleaseAndPreInteractionSchemasUpgradeWithoutLosingReviews(String baseline) throws Exception {
        String database="review_upgrade_"+UUID.randomUUID().toString().replace("-","");
        if(!database.matches("review_upgrade_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe test database name");
        String originalUrl;
        try(var connection=jdbc.getDataSource().getConnection()) {originalUrl=connection.getMetaData().getURL();}
        URI address=URI.create(originalUrl.substring("jdbc:".length()));
        String url="jdbc:postgresql://"+address.getRawAuthority()+"/"+database+"?currentSchema=foodtime";
        String username=environment.getRequiredProperty("spring.datasource.username");
        String password=environment.getProperty("spring.datasource.password","");
        jdbc.execute("CREATE DATABASE "+database);
        try {
            Flyway.configure().dataSource(url,username,password).schemas("foodtime").defaultSchema("foodtime")
                    .target(baseline).cleanDisabled(true).load().migrate();
            JdbcTemplate old=new JdbcTemplate(new DriverManagerDataSource(url,username,password));
            UUID hall=UUID.randomUUID(),stall=UUID.randomUUID(),dish=UUID.randomUUID(),empty=UUID.randomUUID();
            UUID firstUser=UUID.randomUUID(),secondUser=UUID.randomUUID(),firstReview=UUID.randomUUID(),secondReview=UUID.randomUUID();
            old.update("INSERT INTO foodtime.users(id,email,password_hash,display_name,email_verified_at) VALUES (?,?,'test-only','upgrade user',now()),(?,?,'test-only','upgrade user',now())",
                    firstUser,firstUser+"@bjtu.edu.cn",secondUser,secondUser+"@bjtu.edu.cn");
            old.update("INSERT INTO foodtime.dining_halls(id,name) VALUES (?,'upgrade hall')",hall);
            old.update("INSERT INTO foodtime.stalls(id,dining_hall_id,name) VALUES (?,?,'upgrade stall')",stall,hall);
            old.update("INSERT INTO foodtime.dishes(id,stall_id,name,price) VALUES (?,?,'rated dish',10),(?,?,'empty dish',12)",dish,stall,empty,stall);
            old.update("INSERT INTO foodtime.dish_reviews(id,dish_id,user_id,rating,content) VALUES (?,?,?,2,'historical first'),(?,?,?,5,'historical second')",
                    firstReview,dish,firstUser,secondReview,dish,secondUser);
            String created=old.queryForObject("SELECT created_at::text FROM foodtime.dish_reviews WHERE id=?",String.class,firstReview);
            Flyway upgraded=Flyway.configure().dataSource(url,username,password).schemas("foodtime").defaultSchema("foodtime").cleanDisabled(true).load();
            upgraded.migrate();upgraded.validate();
            assertThat(upgraded.info().pending()).isEmpty();
            assertThat(old.queryForObject("SELECT content FROM foodtime.dish_reviews WHERE id=?",String.class,firstReview)).isEqualTo("historical first");
            assertThat(old.queryForObject("SELECT created_at::text FROM foodtime.dish_reviews WHERE id=?",String.class,firstReview)).isEqualTo(created);
            assertThat(old.queryForObject("SELECT review_count FROM foodtime.dish_stats WHERE dish_id=?",Long.class,dish)).isEqualTo(2);
            assertThat(old.queryForObject("SELECT average_score::text FROM foodtime.dish_rating_summary WHERE dish_id=?",String.class,dish)).isEqualTo("3.50");
            assertThat(old.queryForObject("SELECT average_score FROM foodtime.dish_rating_summary WHERE dish_id=?",java.math.BigDecimal.class,empty)).isNull();
            assertThat(old.queryForObject("SELECT relkind::text FROM pg_class WHERE oid='foodtime.dish_rating_summary'::regclass",String.class)).isEqualTo("v");
            assertThat(old.queryForObject("SELECT like_count+reply_count FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,firstReview)).isZero();
            old.update("INSERT INTO foodtime.dish_review_likes(review_id,user_id) VALUES (?,?)",firstReview,secondUser);
            UUID reply=UUID.randomUUID();
            old.update("INSERT INTO foodtime.dish_review_replies(id,review_id,user_id,client_request_id,content) VALUES (?,?,?,?, 'after upgrade')",reply,firstReview,firstUser,UUID.randomUUID());
            old.update("UPDATE foodtime.dish_review_replies SET status='approved',reviewed_by_user_id=?,reviewed_at=now() WHERE id=?",firstUser,reply);
            old.update("INSERT INTO foodtime.dish_review_reply_likes(reply_id,user_id) VALUES (?,?)",reply,secondUser);
            assertThat(old.queryForObject("SELECT like_count FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,firstReview)).isEqualTo(1);
            assertThat(old.queryForObject("SELECT reply_count FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,firstReview)).isEqualTo(1);
            assertThat(old.queryForObject("SELECT like_count FROM foodtime.dish_review_reply_stats WHERE reply_id=?",Long.class,reply)).isEqualTo(1);
            old.update("UPDATE foodtime.dish_review_replies SET deleted_at=now() WHERE id=?",reply);
            assertThat(old.queryForObject("SELECT reply_count FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,firstReview)).isZero();
            old.update("UPDATE foodtime.dish_reviews SET deleted_at=now() WHERE id=?",firstReview);
            assertThat(old.queryForObject("SELECT review_count FROM foodtime.dish_stats WHERE dish_id=?",Long.class,dish)).isEqualTo(1);
            assertThat(old.queryForObject("SELECT average_score::text FROM foodtime.dish_rating_summary WHERE dish_id=?",String.class,dish)).isEqualTo("5.00");
        } finally {
            jdbc.queryForList("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname=? AND pid<>pg_backend_pid()",database);
            jdbc.execute("DROP DATABASE "+database);
        }
    }
}
