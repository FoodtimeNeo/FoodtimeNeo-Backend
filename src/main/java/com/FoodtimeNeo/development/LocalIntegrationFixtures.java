package com.FoodtimeNeo.development;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** Explicit local integration data only; ordinary dev/prod startup never creates accounts. */
@Component
@Profile("local-integration")
@ConditionalOnProperty(name="app.local-integration.fixtures-enabled",havingValue="true")
public class LocalIntegrationFixtures implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String password;
    public LocalIntegrationFixtures(JdbcTemplate jdbc,PasswordEncoder encoder,
            @Value("${app.local-integration.password}") String password) {
        this.jdbc=jdbc;this.encoder=encoder;this.password=password;
    }
    @Override @Transactional
    public void run(String... args) {
        String address=jdbc.queryForObject("SELECT host(inet_server_addr())",String.class);
        if(!"127.0.0.1".equals(address) && !"::1".equals(address)) throw new IllegalStateException("联调样例只允许写入本机数据库");
        if(password.length()<8) throw new IllegalStateException("请设置联调专用密码");
        String hash=encoder.encode(password);
        String[] roles={"user","admin","superadmin"};
        for(int i=0;i<roles.length;i++) {
            jdbc.update("""
                    INSERT INTO foodtime.users(id,email,password_hash,display_name,role,email_verified_at)
                    VALUES (?, ?, ?, ?, ?, now()) ON CONFLICT DO NOTHING
                    """,id(i+1),"2026000"+(i+1)+"@bjtu.edu.cn",hash,"联调"+(i==0?"用户":i==1?"管理员":"平台管理员"),roles[i]);
        }
        jdbc.update("INSERT INTO foodtime.dining_halls(id,name,description) VALUES (?, '联调示例食堂', '仅用于开发验收的样例数据') ON CONFLICT DO NOTHING",id(101));
        jdbc.update("INSERT INTO foodtime.stalls(id,dining_hall_id,name) VALUES (?, ?, '联调示例档口') ON CONFLICT DO NOTHING",id(201),id(101));
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,description,price) VALUES (?, ?, '联调示例菜品', '示例介绍', 12.30) ON CONFLICT DO NOTHING",id(301),id(201));
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,price) VALUES (?, ?, '无图示例菜品', 8.00) ON CONFLICT DO NOTHING",id(302),id(201));
        jdbc.execute("REFRESH MATERIALIZED VIEW foodtime.dish_rating_summary");
    }
    private static UUID id(int value) { return UUID.fromString(String.format("00000000-0000-4000-8000-%012d",value)); }
}
