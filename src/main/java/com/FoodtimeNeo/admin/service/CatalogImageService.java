package com.FoodtimeNeo.admin.service;

import com.FoodtimeNeo.admin.dto.*;
import com.FoodtimeNeo.admin.mapper.CatalogImageStore;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Set;
import java.util.UUID;

@Service
public class CatalogImageService {
    private final CatalogImageStore store;
    private final CatalogAdminService catalog;
    private final ImageStorageService images;
    public CatalogImageService(CatalogImageStore store,CatalogAdminService catalog,ImageStorageService images) {
        this.store=store; this.catalog=catalog; this.images=images;
    }
    private void guard(UserProfile actor,String type) {
        if (actor==null || !"active".equals(actor.status()) || !"superadmin".equals(actor.role()))
            throw new BusinessException("ACCESS_DENIED","需要平台管理员权限",HttpStatus.FORBIDDEN);
        if (!Set.of("dining-halls","stalls","dishes").contains(type))
            throw new BusinessException("VALIDATION_ERROR","目录类型不合法",HttpStatus.BAD_REQUEST);
    }
    private void lock(String type,UUID parent) {
        if (!store.lock(type,parent)) throw new BusinessException("CATALOG_NOT_FOUND","记录不存在",HttpStatus.NOT_FOUND);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResponse<CatalogImageResponse> list(UserProfile actor,String type,UUID parent,PageRequest page) {
        guard(actor,type); catalog.get(actor,type,parent);
        return PageResponse.of(store.page(type,parent,page.size(),page.offset()),store.count(type,parent),page);
    }
    @Transactional
    public CatalogImageResponse add(UserProfile actor,String type,UUID parent,String url) {
        guard(actor,type); lock(type,parent);
        images.requireOwnedReference(url,actor.id());
        return store.add(type,parent,actor.id(),url);
    }
    @Transactional
    public void remove(UserProfile actor,String type,UUID parent,UUID imageId) {
        guard(actor,type); lock(type,parent);
        var image=store.find(type,parent,imageId);
        if (image==null) throw new BusinessException("IMAGE_NOT_FOUND","图片不存在或已移除",HttpStatus.NOT_FOUND);
        // Remove the association only; the same uploaded file may be used elsewhere.
        store.remove(type,parent,actor.id(),image);
    }
}
