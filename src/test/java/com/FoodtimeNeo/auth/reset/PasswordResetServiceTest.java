package com.FoodtimeNeo.auth.reset;

import com.FoodtimeNeo.auth.dto.*;
import com.FoodtimeNeo.config.properties.*;
import com.FoodtimeNeo.user.entity.LoginAccount;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    final PasswordResetStore store=mock(PasswordResetStore.class);
    final UserMapper users=mock(UserMapper.class);
    final PasswordEncoder passwords=mock(PasswordEncoder.class);
    final StringRedisTemplate redis=mock(StringRedisTemplate.class);
    final ObjectProvider<JavaMailSender> mail=mock(ObjectProvider.class);
    PasswordResetServiceTest(){when(redis.execute(any(),anyList(),anyString(),anyString(),anyString())).thenReturn(0L);}
    final LoginAccount account=new LoginAccount(UUID.randomUUID(),"admin@example.test","old-hash","管理员","admin","active",Instant.parse("2026-01-01T00:00:00Z"));
    PasswordResetService service(){return new PasswordResetService(store,new PasswordResetProperties("test:reset",Duration.ofMinutes(10),Duration.ofMinutes(5),Duration.ofMinutes(1),5,10,200,10,300),
        new EmailVerificationProperties(true,"sender@example.test","test-only-secret-at-least-32-characters","test:registration",Duration.ofMinutes(1),Duration.ofHours(1),5,10,200),
        users,passwords,redis,mail,Runnable::run,Clock.systemUTC());}
    @Test void badCodeNeverIssuesCapabilityOrWritesPassword(){
        when(users.findLoginAccount(account.email())).thenReturn(account);
        assertThatThrownBy(()->service().verify(new PasswordResetVerifyRequest(account.email(),"000000"),"127.0.0.1"))
            .hasMessageContaining("验证码错误");
        verify(users,never()).changePassword(any(),any(),any(),any());
        verifyNoInteractions(passwords);
    }
    @Test void forgedTokenCannotReachPasswordWrite(){
        when(store.consume(anyString())).thenThrow(PasswordResetService.invalidGrant());
        assertThatThrownBy(()->service().reset(new PasswordResetRequest("A".repeat(43),"New12345!"),"127.0.0.1"))
            .hasMessageContaining("验证已失效");
        verifyNoInteractions(users,passwords);
    }
    @Test void oldPasswordVersionCannotResetAgain(){
        when(store.consume(anyString())).thenReturn(new PasswordResetStore.Grant(account.id(),Instant.EPOCH.toString()));
        when(users.findLoginAccountById(account.id())).thenReturn(account);
        assertThatThrownBy(()->service().reset(new PasswordResetRequest("A".repeat(43),"New12345!"),"127.0.0.1"))
            .hasMessageContaining("验证已失效");
        verify(users,never()).changePassword(any(),any(),any(),any());verifyNoInteractions(passwords);
    }
    @Test void successfulResetUsesOnlyGrantOwnerAndConditionalPasswordVersion(){
        when(store.consume(anyString())).thenReturn(new PasswordResetStore.Grant(account.id(),account.passwordChangedAt().toString()));
        when(users.findLoginAccountById(account.id())).thenReturn(account);when(passwords.encode("New12345!")).thenReturn("new-hash");
        when(users.changePassword(account.id(),account.passwordHash(),account.passwordChangedAt(),"new-hash")).thenReturn(1);
        service().reset(new PasswordResetRequest("A".repeat(43),"New12345!"),"127.0.0.1");
        verify(users).changePassword(account.id(),"old-hash",account.passwordChangedAt(),"new-hash");
        verify(users,never()).findLoginAccount(anyString());
    }
}
