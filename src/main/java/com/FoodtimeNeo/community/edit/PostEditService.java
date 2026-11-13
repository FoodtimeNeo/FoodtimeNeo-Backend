package com.FoodtimeNeo.community.edit;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.submission.service.SubmissionRateLimiter;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

@Service
public class PostEditService {
    public record Result(UUID id,long revision,boolean changed) { }
    private final PostEditStore store;private final CommunityStore posts;private final ImageStorageService images;
    private final SubmissionRateLimiter limiter;private final ApplicationEventPublisher events;private final boolean ai;
    public PostEditService(PostEditStore store,CommunityStore posts,ImageStorageService images,
            SubmissionRateLimiter limiter,ApplicationEventPublisher events,
            @org.springframework.beans.factory.annotation.Value("${app.ai.triage.enabled:false}") boolean ai) {
        this.store=store;this.posts=posts;this.images=images;this.limiter=limiter;this.events=events;this.ai=ai;
    }
    @Transactional
    public Result edit(UserProfile actor,UUID id,EditPostRequest input,String address) {
        var target=store.lock(id);
        if(target==null||!target.author().equals(actor.id()))throw new BusinessException("POST_NOT_FOUND","帖子不存在或不属于你",HttpStatus.NOT_FOUND);
        var payload=new LinkedHashMap<String,Object>();payload.put("title",input.title());payload.put("content",input.content());payload.put("imageUrls",input.imageUrls());payload.put("dishId",input.dishId());
        String json=tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(payload);
        String hash=hash(input.expectedRevision()+"\n"+json);var receipt=store.requested(id,input.clientRequestId());
        if(receipt!=null) {
            if(!receipt.hash().equals(hash))throw new BusinessException("REQUEST_CONFLICT","修改请求编号已用于另一份内容",HttpStatus.CONFLICT);
            return new Result(id,receipt.revision(),receipt.changed());
        }
        if(target.revision()!=input.expectedRevision())throw new BusinessException("POST_EDIT_CONFLICT","帖子已修改，请刷新后核对内容",HttpStatus.CONFLICT);
        if(input.imageUrls().isEmpty()&&target.repost()==null)throw new BusinessException("POST_IMAGES_REQUIRED","请保留至少1张图片",HttpStatus.BAD_REQUEST);
        if(new HashSet<>(input.imageUrls()).size()!=input.imageUrls().size())throw new BusinessException("IMAGE_DUPLICATED","请勿重复添加同一张图片",HttpStatus.BAD_REQUEST);
        if(store.same(id,json)){store.receipt(id,input.clientRequestId(),hash,target.revision(),false);return new Result(id,target.revision(),false);}
        limiter.check(actor.id(),address);
        var verified=input.imageUrls().stream().map(url->images.ownedImage(url,actor.id())).toList();
        if(input.dishId()!=null&&!posts.lockVisibleDish(input.dishId()))throw new BusinessException("DISH_NOT_FOUND","关联菜品不存在或暂不可用",HttpStatus.NOT_FOUND);
        store.snapshot(id,target.revision());store.replace(id,input,ai);
        for(int i=0;i<verified.size();i++)store.image(id,i,verified.get(i));
        store.receipt(id,input.clientRequestId(),hash,target.revision()+1,true);
        if(ai)events.publishEvent(new com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent("community-post",id));
        return new Result(id,target.revision()+1,true);
    }
    private static String hash(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
