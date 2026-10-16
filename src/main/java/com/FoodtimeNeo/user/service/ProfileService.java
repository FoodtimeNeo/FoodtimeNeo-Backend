package com.FoodtimeNeo.user.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.dto.ProfileResponse;
import com.FoodtimeNeo.user.mapper.ProfileMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ProfileService {
    private final ProfileMapper profiles;
    public ProfileService(ProfileMapper profiles) { this.profiles = profiles; }
    public ProfileResponse current(UUID userId) {
        ProfileResponse profile = profiles.current(userId);
        if (profile == null) throw new BusinessException("UNAUTHENTICATED", "请重新登录", HttpStatus.UNAUTHORIZED);
        return profile;
    }
}
