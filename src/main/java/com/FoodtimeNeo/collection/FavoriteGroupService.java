package com.FoodtimeNeo.collection;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
public class FavoriteGroupService {
    private static final int MAX_GROUPS_PER_KIND = 20;
    private final FavoriteGroupStore groups;

    public FavoriteGroupService(FavoriteGroupStore groups) {
        this.groups = groups;
    }

    @Transactional(readOnly = true)
    public List<FavoriteGroupDtos.Response> list(UUID owner, String kind) {
        return groups.list(owner, kind(kind));
    }

    @Transactional
    public FavoriteGroupDtos.Response create(UUID owner, FavoriteGroupDtos.CreateRequest request) {
        FavoriteKind kind = kind(request.kind());
        String name = name(request.name());
        groups.lockOwnerKind(owner, kind);
        if (groups.count(owner, kind) >= MAX_GROUPS_PER_KIND) {
            throw new BusinessException("GROUP_LIMIT_REACHED", "每类最多创建20个收藏分组", HttpStatus.CONFLICT);
        }
        UUID id = UUID.randomUUID();
        int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        try {
            groups.insert(id, owner, kind, name, sortOrder);
        } catch (DuplicateKeyException conflict) {
            throw new BusinessException("GROUP_NAME_TAKEN", "同类型下已存在同名分组", HttpStatus.CONFLICT);
        }
        return groups.find(owner, id);
    }

    @Transactional
    public FavoriteGroupDtos.Response update(UUID owner, UUID id, FavoriteGroupDtos.UpdateRequest request) {
        FavoriteGroupDtos.Response existing = group(owner, id);
        String name = request.name() == null ? null : name(request.name());
        try {
            if (!groups.update(owner, id, name, request.sortOrder(), request.expectedRevision())) {
                throw new BusinessException("GROUP_REVISION_CONFLICT", "分组已被其他操作修改，请刷新后重试",
                        HttpStatus.CONFLICT);
            }
        } catch (DuplicateKeyException conflict) {
            throw new BusinessException("GROUP_NAME_TAKEN", "同类型下已存在同名分组", HttpStatus.CONFLICT);
        }
        return groups.find(owner, id);
    }

    @Transactional
    public void delete(UUID owner, UUID id) {
        group(owner, id);
        if (!groups.delete(owner, id)) {
            throw new BusinessException("GROUP_NOT_FOUND", "分组不存在", HttpStatus.NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public FavoriteGroupDtos.MembershipResponse membership(UUID owner, String kind, UUID targetId) {
        FavoriteKind parsed = kind(kind);
        return new FavoriteGroupDtos.MembershipResponse(groups.membershipIds(owner, parsed, targetId));
    }

    @Transactional
    public FavoriteGroupDtos.MembershipResponse addMember(UUID owner, UUID groupId, UUID targetId) {
        FavoriteGroupDtos.Response group = group(owner, groupId);
        FavoriteKind kind = kind(group.kind());
        if (!groups.favoriteExists(owner, kind, targetId)) {
            throw new BusinessException("FAVORITE_NOT_FOUND", "该对象尚未收藏，不能加入分组", HttpStatus.NOT_FOUND);
        }
        groups.addMember(groupId, owner, kind, targetId);
        return new FavoriteGroupDtos.MembershipResponse(groups.membershipIds(owner, kind, targetId));
    }

    @Transactional
    public FavoriteGroupDtos.MembershipResponse removeMember(UUID owner, UUID groupId, UUID targetId) {
        FavoriteGroupDtos.Response group = group(owner, groupId);
        FavoriteKind kind = kind(group.kind());
        groups.removeMember(groupId, owner, kind, targetId);
        return new FavoriteGroupDtos.MembershipResponse(groups.membershipIds(owner, kind, targetId));
    }

    private FavoriteGroupDtos.Response group(UUID owner, UUID id) {
        FavoriteGroupDtos.Response group = groups.find(owner, id);
        if (group == null || groups.kindOf(owner, id).isBlank()) {
            throw new BusinessException("GROUP_NOT_FOUND", "分组不存在", HttpStatus.NOT_FOUND);
        }
        return group;
    }

    private static FavoriteKind kind(String value) {
        return FavoriteKind.fromWire(value).orElseThrow(() ->
                new BusinessException("VALIDATION_ERROR", "分组类型只能为dish或post", HttpStatus.BAD_REQUEST));
    }

    static String name(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value.strip(), Normalizer.Form.NFKC);
        int length = normalized.codePointCount(0, normalized.length());
        if (length < 1 || length > 20 || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new BusinessException("VALIDATION_ERROR", "分组名称须为1至20个字符", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }
}
