package com.FoodtimeNeo.auth.captcha;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * One-digit arithmetic with optional two-digit division dividends. Subtraction is intentionally
 * absent. Characters are individually rotated and the answer is never written into metadata.
 */
@Component
public class JavaArithmeticChallengeProvider implements ChallengeProvider {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 60;
    private final SecureRandom random = new SecureRandom();

    @Override
    public GeneratedChallenge generate() {
        int operator = random.nextInt(3);
        int left;
        int right;
        int answer;
        String symbol;
        if (operator == 0) {
            left = digit();
            right = digit();
            answer = left + right;
            symbol = "+";
        } else if (operator == 1) {
            left = digit();
            right = digit();
            answer = left * right;
            symbol = "×";
        } else {
            right = digit();
            answer = digit();
            left = right * answer;
            symbol = "÷";
        }
        String expression = left + " " + symbol + " " + right + " = ?";
        return new GeneratedChallenge(expression, answer, render(expression));
    }

    private int digit() {
        return 2 + random.nextInt(8);
    }

    private byte[] render(String expression) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(0xf8f4ec));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            drawNoise(graphics);
            graphics.setFont(new Font(Font.SERIF, Font.BOLD, 27));
            FontMetrics metrics = graphics.getFontMetrics();
            int x = Math.max(8, (WIDTH - metrics.stringWidth(expression)) / 2);
            int baseline = 40;
            for (char character : expression.toCharArray()) {
                int width = metrics.charWidth(character);
                AffineTransform original = graphics.getTransform();
                double angle = Math.toRadians(random.nextInt(51) - 25);
                graphics.rotate(angle, x + width / 2.0, baseline - 9);
                graphics.setColor(new Color(38 + random.nextInt(55), 30 + random.nextInt(42), 22 + random.nextInt(35)));
                graphics.drawString(String.valueOf(character), x, baseline + random.nextInt(5) - 2);
                graphics.setTransform(original);
                x += width;
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("CAPTCHA image generation failed", failure);
        } finally {
            graphics.dispose();
        }
    }

    private void drawNoise(Graphics2D graphics) {
        for (int line = 0; line < 3; line++) {
            graphics.setColor(new Color(170 + random.nextInt(45), 160 + random.nextInt(45), 145 + random.nextInt(45)));
            graphics.setStroke(new BasicStroke(1f));
            graphics.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        for (int dot = 0; dot < 45; dot++) {
            graphics.setColor(new Color(190 + random.nextInt(50), 180 + random.nextInt(50), 165 + random.nextInt(50)));
            graphics.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }
    }
}
