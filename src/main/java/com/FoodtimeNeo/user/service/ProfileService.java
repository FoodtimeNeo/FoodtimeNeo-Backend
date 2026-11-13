package com.FoodtimeNeo.user.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.user.dto.ProfileResponse;
import com.FoodtimeNeo.user.dto.UpdateProfileRequest;
import com.FoodtimeNeo.user.mapper.ProfileMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ProfileService {
    private final ProfileMapper profiles;
    private final ImageStorageService images;
    private final com.FoodtimeNeo.auth.security.ParticipationPolicy participation;
    public ProfileService(ProfileMapper profiles, ImageStorageService images,
                          com.FoodtimeNeo.auth.security.ParticipationPolicy participation) {
        this.profiles = profiles; this.images = images;
        this.participation = participation;
    }
    public ProfileResponse current(UUID userId) {
        ProfileResponse profile = profiles.current(userId);
        if (profile == null) throw new BusinessException("UNAUTHENTICATED", "请重新登录", HttpStatus.UNAUTHORIZED);
        String role = participation.effectiveRole(profile.role());
        return new ProfileResponse(profile.id(), profile.email(), profile.displayName(), role,
                profile.avatarUrl(), profile.followingCount(), profile.followerCount(),
                profile.schoolEmail(), profile.schoolEmailVerified());
    }
    public ProfileResponse update(UUID userId, UpdateProfileRequest body) {
        String displayName = body.displayName().trim();
        // Existing avatars remain usable when changing only the name; every new reference is checked independently.
        if (body.avatarUrl() != null && !body.avatarUrl().equals(current(userId).avatarUrl())) images.requireAvatarReference(body.avatarUrl(), userId);
        int updated = profiles.update(userId, displayName, body.avatarUrl());
        if (updated == 0) throw new BusinessException("UNAUTHENTICATED", "请重新登录", HttpStatus.UNAUTHORIZED);
        return current(userId);
    }
}
