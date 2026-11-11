package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.community.mapper.CommunityViewStore;
import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class CommunityViewService {
    private final CommunityViewStore store;
    public CommunityViewService(CommunityViewStore store) { this.store=store; }
    @Transactional
    public long record(UUID post,UUID viewer,UUID requestId) {
        var target=store.lock(post);
        if(target==null || (!"approved".equals(target.status()) && !viewer.equals(target.author())))
            throw new BusinessException("POST_NOT_FOUND","动态不存在或暂不可见",HttpStatus.NOT_FOUND);
        if(!"approved".equals(target.status())) return target.count();
        return store.record(post,viewer,requestId,target.count());
    }
}
