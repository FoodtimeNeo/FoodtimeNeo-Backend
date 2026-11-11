package com.FoodtimeNeo.auth.captcha;

/** Replaceable challenge generator; implementations must not expose the answer in the image response. */
public interface ChallengeProvider {
    GeneratedChallenge generate();
}
