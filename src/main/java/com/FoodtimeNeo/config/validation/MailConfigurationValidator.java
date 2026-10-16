package com.FoodtimeNeo.config.validation;

import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import org.springframework.core.env.Environment;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class MailConfigurationValidator {
    public MailConfigurationValidator(EmailVerificationProperties properties, Environment environment) {
        if (!properties.enabled()) { return; }
        MailProperties mail = Binder.get(environment).bind("spring.mail", MailProperties.class).orElseGet(MailProperties::new);
        String host = mail.getHost();
        if (host == null || host.isBlank() || host.equals("smtp.example.com")) {
            throw new IllegalStateException("Enabled email verification requires a real spring.mail.host");
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            boolean ssl = Boolean.parseBoolean(mail.getProperties().get("mail.smtp.ssl.enable"));
            boolean tls = Boolean.parseBoolean(mail.getProperties().get("mail.smtp.starttls.enable"))
                    && Boolean.parseBoolean(mail.getProperties().get("mail.smtp.starttls.required"));
            if (!ssl && !tls) {
                throw new IllegalStateException("Production SMTP requires TLS");
            }
        }
        if (Boolean.parseBoolean(mail.getProperties().get("mail.smtp.auth"))
                && (mail.getUsername() == null || mail.getUsername().isBlank()
                || mail.getPassword() == null || mail.getPassword().isBlank())) {
            throw new IllegalStateException("Enabled SMTP authentication requires username and password");
        }
    }
}
