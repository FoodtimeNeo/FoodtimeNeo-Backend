package com.FoodtimeNeo.community.pin;

import com.FoodtimeNeo.authz.AuthorizationContext;
import com.FoodtimeNeo.authz.Capability;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

@Service
public class CommunityPinService {
    private final CommunityPinStore store;
    private final CapabilityService capabilities;
    public CommunityPinService(CommunityPinStore store,CapabilityService capabilities) {
        this.store=store;this.capabilities=capabilities;
    }
    private void require(UserProfile actor) { capabilities.require(actor,Capability.COMMUNITY_PIN_MANAGE,AuthorizationContext.none()); }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PinDtos.Snapshot read(UserProfile actor,boolean management) {
        if(management)require(actor);
        return new PinDtos.Snapshot(store.revision(false),store.entries(management),
                capabilities.allows(actor,Capability.COMMUNITY_PIN_MANAGE,AuthorizationContext.none()));
    }
    private void revision(long expected) {
        if(store.revision(true)!=expected)throw new BusinessException("PIN_CONFLICT","置顶列表已变化，请刷新后重试",HttpStatus.CONFLICT);
    }
    @Transactional
    public PinDtos.Snapshot pin(UserProfile actor,UUID id,long expected) {
        require(actor);
        if(!store.lockPublished(id))throw new BusinessException("POST_NOT_FOUND","只能置顶已审核通过且未删除的帖子",HttpStatus.NOT_FOUND);
        revision(expected);var entries=store.entries(true);
        if(entries.stream().noneMatch(value->value.id().equals(id))) {
            if(entries.size()>=3)throw new BusinessException("PIN_LIMIT_REACHED","最多保留3条置顶，请先取消一条",HttpStatus.CONFLICT);
            var ids=new ArrayList<>(entries.stream().map(PinDtos.Entry::id).toList());
            store.order(ids);store.insert(id,ids.size()+1,actor.id());store.changed();
        }
        return read(actor,true);
    }
    @Transactional
    public PinDtos.Snapshot unpin(UserProfile actor,UUID id,long expected) {
        require(actor);store.lockPost(id);revision(expected);
        var entries=store.entries(true);
        if(entries.stream().anyMatch(value->value.id().equals(id))) {
            store.remove(id);store.order(entries.stream().map(PinDtos.Entry::id).filter(value->!value.equals(id)).toList());store.changed();
        }
        return read(actor,true);
    }
    @Transactional
    public PinDtos.Snapshot order(UserProfile actor,PinDtos.Order input) {
        require(actor);revision(input.expectedRevision());var ids=store.entries(true).stream().map(PinDtos.Entry::id).toList();
        if(input.postIds().size()!=ids.size()||new HashSet<>(input.postIds()).size()!=input.postIds().size()
                ||!new HashSet<>(input.postIds()).equals(new HashSet<>(ids)))
            throw new BusinessException("PIN_ORDER_INVALID","请提交完整且不重复的当前置顶列表",HttpStatus.BAD_REQUEST);
        if(!ids.equals(input.postIds())) {store.order(input.postIds());store.changed();}
        return read(actor,true);
    }
}
