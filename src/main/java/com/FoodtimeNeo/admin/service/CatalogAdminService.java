package com.FoodtimeNeo.admin.service;

import com.FoodtimeNeo.admin.dto.*;
import com.FoodtimeNeo.admin.mapper.CatalogAdminStore;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.admin.dto.AdminReviewResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.*;

@Service
@Validated
public class CatalogAdminService {
    private final CatalogAdminStore catalog;
    private final ImageStorageService images;
    public CatalogAdminService(CatalogAdminStore catalog, ImageStorageService images) { this.catalog = catalog; this.images = images; }
    @Transactional(isolation = Isolation.REPEATABLE_READ,readOnly = true)
    public PageResponse<AdminCatalogResponse> list(UserProfile actor,String type,UUID parent,PageRequest page) {
        guard(actor); type(type); return PageResponse.of(catalog.page(type,parent,page.size(),page.offset()),catalog.count(type,parent),page);
    }
    public AdminCatalogResponse get(UserProfile actor,String type,UUID id) { guard(actor); type(type); return existing(type,id,false); }
    @Transactional
    public AdminCatalogResponse save(UserProfile actor,String type,UUID id,@Valid CatalogEditRequest body) {
        guard(actor); type(type);
        if (!type.equals("dishes") && body.name().length() > 100) throw bad("名称不能超过100字");
        boolean create = id == null;
        AdminCatalogResponse old = create ? null : existing(type,id,true);
        UUID parent = create ? body.parentId() : old.parentId();
        if (!create && body.parentId() != null && !Objects.equals(body.parentId(),parent)) throw bad("修改已有记录时不能改变所属关系");
        if (!type.equals("dining-halls") && (parent == null || !catalog.parentExists(type,parent))) throw bad("上级记录不存在");
        if (type.equals("dishes") && body.price() == null) throw bad("菜品价格必须填写");
        if (!type.equals("dishes") && body.coverImageUrl() != null && !body.coverImageUrl().isBlank()
                && (create || !Objects.equals(old.coverImageUrl(),body.coverImageUrl()))) images.requireOwnedReference(body.coverImageUrl(),actor.id());
        String status = body.status() == null ? create ? "active" : old.status() : body.status();
        int sort = body.sortOrder() == null ? create || old.sortOrder() == null ? 0 : old.sortOrder() : body.sortOrder();
        if (create) id = UUID.randomUUID();
        try { catalog.save(type,id,parent,body,status,sort,create); }
        catch (DuplicateKeyException conflict) { throw new BusinessException("CATALOG_CONFLICT","同一食堂已有同名档口",HttpStatus.CONFLICT); }
        return existing(type,id,false);
    }
    @Transactional(isolation = Isolation.REPEATABLE_READ,readOnly = true)
    public PageResponse<AdminReviewResponse> reviews(UserProfile actor,String query,PageRequest page) {
        guard(actor);
        String q = query == null ? "" : query.strip();
        if (q.length() > 254) throw bad("搜索内容不能超过254字");
        return PageResponse.of(catalog.reviews(q,page.size(),page.offset()),catalog.reviewCount(q),page);
    }
    @Transactional
    public void removeReview(UserProfile actor,UUID id) {
        guard(actor); if (catalog.removeReview(id) == 0) throw new BusinessException("REVIEW_NOT_FOUND","评论不存在",HttpStatus.NOT_FOUND);
    }
    @Transactional
    public void remove(UserProfile actor,String type,UUID id) {
        guard(actor); type(type);
        try {
            if (catalog.remove(type,id) == 0) throw new BusinessException("CATALOG_NOT_FOUND","记录不存在",HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException referenced) {
            throw new BusinessException("CATALOG_IN_USE","仍有关联记录，请先停用",HttpStatus.CONFLICT);
        }
    }
    private void guard(UserProfile actor) { if (!actor.status().equals("active") || !(actor.role().equals("admin") || actor.role().equals("superadmin"))) throw new BusinessException("ACCESS_DENIED","需要管理员权限",HttpStatus.FORBIDDEN); }
    private void type(String type) { if (!Set.of("dining-halls","stalls","dishes").contains(type)) throw bad("目录类型不合法"); }
    private AdminCatalogResponse existing(String type,UUID id,boolean lock) { var item = catalog.get(type,id,lock); if (item == null) throw new BusinessException("CATALOG_NOT_FOUND","记录不存在",HttpStatus.NOT_FOUND); return item; }
    private BusinessException bad(String message) { return new BusinessException("VALIDATION_ERROR",message,HttpStatus.BAD_REQUEST); }
}
