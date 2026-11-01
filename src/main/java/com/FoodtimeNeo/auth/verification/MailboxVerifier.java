package com.FoodtimeNeo.auth.verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * 校园邮箱存在性预检：连接学校 SMTP 的 465（隐式 TLS），只做 RCPT 探测，
 * 不发送 DATA、不投递邮件。550/551/553 视为不存在；250/251/252 视为存在；
 * 超时、证书、网络等异常一律返回 UNKNOWN，由调用方 fail-open 继续发码。
 */
@Service
public class MailboxVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(MailboxVerifier.class);

    public enum Result { EXISTS, NOT_FOUND, UNKNOWN }

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final String host;
    private final int port;
    private final int timeoutMillis;
    private final Duration cacheTtl;
    private final String cacheNamespace;

    public MailboxVerifier(StringRedisTemplate redis,
            @Value("${app.email-verification.mailbox.enabled:false}") boolean enabled,
            @Value("${app.email-verification.mailbox.host:smg.bjtu.edu.cn}") String host,
            @Value("${app.email-verification.mailbox.port:465}") int port,
            @Value("${app.email-verification.mailbox.timeout:PT3S}") Duration timeout,
            @Value("${app.email-verification.mailbox.cache-ttl:PT10M}") Duration cacheTtl,
            @Value("${app.email-verification.mailbox.cache-namespace:foodtime:auth:mailbox}") String cacheNamespace) {
        this.redis = redis;
        this.enabled = enabled;
        this.host = host == null ? "" : host.strip();
        this.port = port >= 1 && port <= 65535 ? port : 465;
        long millis = timeout == null ? 3000 : timeout.toMillis();
        this.timeoutMillis = (int) Math.max(500, Math.min(10000, millis));
        this.cacheTtl = cacheTtl == null || cacheTtl.isZero() || cacheTtl.isNegative()
                ? Duration.ofMinutes(10) : cacheTtl;
        this.cacheNamespace = cacheNamespace == null || cacheNamespace.isBlank()
                ? "foodtime:auth:mailbox" : cacheNamespace;
    }

    public Result verify(String email) {
        if (!enabled || host.isBlank() || email == null || email.isBlank()) {
            return Result.UNKNOWN;
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        Result cached = cached(normalized);
        if (cached != null) {
            return cached;
        }
        Result result = probe(normalized);
        if (result != Result.UNKNOWN) {
            cache(normalized, result);
        }
        return result;
    }

    private Result probe(String email) {
        SSLSocket socket = null;
        try {
            SSLContext context = SSLContext.getDefault();
            SSLSocketFactory factory = context.getSocketFactory();
            socket = (SSLSocket) factory.createSocket();
            socket.setSoTimeout(timeoutMillis);
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            parameters.setServerNames(List.of(new SNIHostName(host)));
            socket.setSSLParameters(parameters);
            socket.startHandshake();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));

            if (readResponse(reader) / 100 != 2) return Result.UNKNOWN;
            writeCommand(writer, "EHLO foodtime-neo.local");
            if (readResponse(reader) / 100 != 2) return Result.UNKNOWN;
            writeCommand(writer, "MAIL FROM:<>");
            if (readResponse(reader) / 100 != 2) return Result.UNKNOWN;
            writeCommand(writer, "RCPT TO:<" + email + ">");
            int rcpt = readResponse(reader);
            writeCommand(writer, "QUIT");

            if (rcpt == 250 || rcpt == 251 || rcpt == 252) return Result.EXISTS;
            if (rcpt == 550 || rcpt == 551 || rcpt == 553) return Result.NOT_FOUND;
            return Result.UNKNOWN;
        } catch (Exception exception) {
            LOG.warn("Mailbox verification probe failed: {}", exception.getClass().getSimpleName());
            return Result.UNKNOWN;
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) { }
            }
        }
    }

    private static int readResponse(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null || line.length() < 3) {
            throw new IOException("Malformed SMTP response");
        }
        int code;
        try {
            code = Integer.parseInt(line.substring(0, 3));
        } catch (NumberFormatException exception) {
            throw new IOException("Malformed SMTP response code", exception);
        }
        if (line.length() > 3 && line.charAt(3) == '-') {
            while ((line = reader.readLine()) != null) {
                if (line.length() > 3 && line.charAt(3) == ' ') {
                    break;
                }
            }
        }
        return code;
    }

    private static void writeCommand(BufferedWriter writer, String command) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
    }

    private Result cached(String email) {
        try {
            String value = redis.opsForValue().get(cacheKey(email));
            if ("1".equals(value)) return Result.EXISTS;
            if ("0".equals(value)) return Result.NOT_FOUND;
        } catch (DataAccessException exception) {
            LOG.debug("Mailbox verification cache read failed: {}", exception.getClass().getSimpleName());
        }
        return null;
    }

    private void cache(String email, Result result) {
        try {
            redis.opsForValue().set(cacheKey(email), result == Result.EXISTS ? "1" : "0", cacheTtl);
        } catch (DataAccessException exception) {
            LOG.debug("Mailbox verification cache write failed: {}", exception.getClass().getSimpleName());
        }
    }

    private String cacheKey(String email) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8)));
            return cacheNamespace + ":" + digest;
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
