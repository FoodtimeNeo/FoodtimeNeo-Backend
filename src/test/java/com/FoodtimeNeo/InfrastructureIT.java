package com.FoodtimeNeo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Run explicitly with mvn -Pintegration verify against a local disposable PostgreSQL / Redis. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InfrastructureIT {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private StringRedisTemplate redis;

    @Test
    void postgresUsesTheMigratedApplicationSchema() {
        assertThat(jdbc.queryForObject("select current_schema()", String.class)).isEqualTo("foodtime");
        assertThat(jdbc.queryForObject(
                "select count(*) from foodtime.flyway_schema_history where version = '1' and success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select count(*) from foodtime.flyway_schema_history where version = '2' and success", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void redisCanReadWriteAndExpireAStringKey() {
        String key = "foodtime:integration:" + UUID.randomUUID();
        try {
            redis.opsForValue().set(key, "ready", Duration.ofSeconds(30));
            assertThat(redis.opsForValue().get(key)).isEqualTo("ready");
            assertThat(redis.getExpire(key)).isPositive().isLessThanOrEqualTo(30L);
        } finally {
            redis.delete(key);
        }
    }
}
