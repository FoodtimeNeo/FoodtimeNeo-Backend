package com.FoodtimeNeo.authz;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Stable action names used by the central authorization policy. */
public enum Capability {
    MODERATION_REVIEW("moderation.review"),
    CATALOG_EDIT("catalog.edit"),
    FEEDBACK_MANAGE("feedback.manage"),
    NOTICE_MANAGE("notice.manage"),
    USER_MANAGE("user.manage"),
    INVITATION_MANAGE("invitation.manage"),
    AI_MODEL_MANAGE("ai.model.manage"),
    CORRECTION_SUBMIT("correction.submit"),
    CORRECTION_REVIEW("correction.review"),
    REPORT_REVIEW("report.review"),
    REPORT_REPLY("report.reply"),
    APPEAL_REVIEW("appeal.review");

    private final String wireName;

    Capability(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static Optional<Capability> fromWire(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(capability -> capability.wireName.equals(normalized))
                .findFirst();
    }
}
