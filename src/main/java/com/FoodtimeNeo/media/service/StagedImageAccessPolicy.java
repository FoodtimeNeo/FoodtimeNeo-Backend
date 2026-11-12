package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.user.entity.UserProfile;

import java.util.UUID;

/**
 * Optional restriction for staged images whose business purpose is narrower than the general
 * "any administrator may inspect staging" rule. Implementations return true only when this user
 * must not read the image through the generic media endpoint.
 */
public interface StagedImageAccessPolicy {
    default boolean restricted(UserProfile user, UUID imageId) {
        return false;
    }
}
