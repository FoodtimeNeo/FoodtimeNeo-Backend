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
    public ProfileService(ProfileMapper profiles, ImageStorageService images) {
        this.profiles = profiles; this.images = images;
    }
    public ProfileResponse current(UUID userId) {
        ProfileResponse profile = profiles.current(userId);
        if (profile == null) throw new BusinessException("UNAUTHENTICATED", "请重新登录", HttpStatus.UNAUTHORIZED);
        return profile;
    }
    public ProfileResponse update(UUID userId, UpdateProfileRequest body) {
        String displayName = body.displayName().trim();
        if (body.avatarUrl() != null) images.requireOwnedReference(body.avatarUrl(), userId);
        int updated = profiles.update(userId, displayName, body.avatarUrl());
        if (updated == 0) throw new BusinessException("UNAUTHENTICATED", "请重新登录", HttpStatus.UNAUTHORIZED);
        return current(userId);
    }
}
