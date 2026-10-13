package com.FoodtimeNeo.admin.service;

import com.FoodtimeNeo.admin.dto.*;
import com.FoodtimeNeo.admin.mapper.UserAdminStore;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

@Service
@Validated
public class UserAdminService {
    private final UserAdminStore users;
    public UserAdminService(UserAdminStore users) { this.users = users; }
    @Transactional(isolation=Isolation.REPEATABLE_READ,readOnly=true)
    public PageResponse<AdminUserResponse> list(UserProfile actor,String query,PageRequest page) {
        guard(actor.role(),actor.status());
        String q = query == null ? "" : query.strip();
        if(q.length()>254) throw new BusinessException("VALIDATION_ERROR","搜索内容过长",HttpStatus.BAD_REQUEST);
        return PageResponse.of(users.page(q,page.size(),page.offset()),users.count(q),page);
    }
    @Transactional
    public void update(UserProfile actor,UUID id,@Valid UserAccessRequest body) {
        guard(actor.role(),actor.status());
        if(actor.id().equals(id)) throw new BusinessException("SELF_ACCESS_CHANGE","不能修改自己的角色或状态",HttpStatus.CONFLICT);
        var locked = users.lock(actor.id(),id);
        var current = locked.stream().filter(u -> u.id().equals(actor.id())).findFirst()
                .orElseThrow(() -> new BusinessException("ACCESS_DENIED","需要平台管理员权限",HttpStatus.FORBIDDEN));
        guard(current.role(),current.status());
        if(locked.stream().noneMatch(u -> u.id().equals(id))) throw new BusinessException("USER_NOT_FOUND","用户不存在",HttpStatus.NOT_FOUND);
        users.update(id,body.role(),body.status());
    }
    private void guard(String role,String status) {
        if(!role.equals("superadmin") || !status.equals("active")) throw new BusinessException("ACCESS_DENIED","需要平台管理员权限",HttpStatus.FORBIDDEN);
    }
}
