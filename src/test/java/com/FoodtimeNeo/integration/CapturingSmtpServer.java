package com.FoodtimeNeo.integration;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/** Loopback-only SMTP fixture: exercises the real Jakarta Mail transport without external email. */
final class CapturingSmtpServer implements AutoCloseable {
    private final ServerSocket socket;
    private final ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, MimeMessage> messages = new ConcurrentHashMap<>();
    final AtomicBoolean rejectNext = new AtomicBoolean();

    CapturingSmtpServer() {
        try {
            socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            threads.submit(() -> {
                while (!socket.isClosed()) {
                    try { Socket connection = socket.accept(); threads.submit(() -> handle(connection)); }
                    catch (IOException failure) { if (!socket.isClosed()) throw new UncheckedIOException(failure); }
                }
            });
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    int port() { return socket.getLocalPort(); }
    MimeMessage message(String email) { return messages.get(email.toLowerCase(Locale.ROOT)); }
    void clear() { messages.clear(); rejectNext.set(false); }

    String code(String email) throws Exception {
        MimeMessage mail = message(email);
        if (mail == null) throw new IllegalStateException("Test SMTP did not receive a message");
        var matcher = Pattern.compile("验证码：([0-9]{6})").matcher((String) mail.getContent());
        if (!matcher.find()) throw new IllegalStateException("Missing six-digit code in test message");
        return matcher.group(1);
    }

    private void handle(Socket connection) {
        try (connection;
             var in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
             var out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
            connection.setSoTimeout(5000);
            reply(out, "220 localhost test SMTP");
            String command;
            while ((command = in.readLine()) != null) {
                String upper = command.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    reply(out, "250-localhost\r\n250 8BITMIME");
                } else if (upper.equals("DATA")) {
                    reply(out, "354 Send message");
                    StringBuilder data = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null && !line.equals(".")) {
                        data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                    }
                    if (rejectNext.getAndSet(false)) {
                        reply(out, "554 Test delivery rejected");
                    } else {
                        var message = new MimeMessage(Session.getInstance(new Properties()),
                                new ByteArrayInputStream(data.toString().getBytes(StandardCharsets.UTF_8)));
                        for (var recipient : message.getAllRecipients()) {
                            messages.put(recipient.toString().toLowerCase(Locale.ROOT), message);
                        }
                        reply(out, "250 Accepted");
                    }
                } else if (upper.equals("QUIT")) { reply(out, "221 Bye"); return; }
                else { reply(out, "250 OK"); }
            }
        } catch (Exception exception) { throw new RuntimeException("Test SMTP connection failed", exception); }
    }

    private static void reply(BufferedWriter out, String text) throws IOException {
        out.write(text + "\r\n"); out.flush();
    }

    @Override
    public void close() throws IOException { socket.close(); threads.close(); }
}
