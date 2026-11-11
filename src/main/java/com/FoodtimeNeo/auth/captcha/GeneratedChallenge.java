package com.FoodtimeNeo.auth.captcha;

/** Internal challenge payload; the expression and answer never leave the server. */
public record GeneratedChallenge(String expression, int answer, byte[] image) {
    public GeneratedChallenge {
        image = image.clone();
    }

    @Override
    public byte[] image() {
        return image.clone();
    }

    @Override
    public String toString() {
        return "GeneratedChallenge[expression=<redacted>, answer=<redacted>, imageBytes=" + image.length + "]";
    }
}
