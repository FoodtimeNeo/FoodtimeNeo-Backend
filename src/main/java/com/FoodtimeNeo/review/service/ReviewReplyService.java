package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.review.dto.*;
import com.FoodtimeNeo.review.mapper.ReviewReplyStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class ReviewReplyService {
    private final ReviewReplyStore store;
    private final ReviewAccessService access;
    private final ApplicationEventPublisher events;
    public ReviewReplyService(ReviewReplyStore store,ReviewAccessService access,ApplicationEventPublisher events) {this.store=store;this.access=access;this.events=events;}
    @Transactional
    public ReviewReplyCreatedResponse submit(UUID review,UUID user,CreateReviewReplyRequest body) {
        var prior=store.byRequest(user,body.clientRequestId());
        if(prior!=null) return replay(prior,review,body);
        access.lock(review,true,false);
        if(body.replyToId()!=null&&!store.visibleTarget(review,body.replyToId())) throw missing();
        UUID id=UUID.randomUUID();
        if(!store.insert(id,review,user,body)) return replay(store.byRequest(user,body.clientRequestId()),review,body);
        events.publishEvent(new SubmissionDiagnoseEvent("dish-review-reply",id));
        return ack(store.find(id,false));
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,readOnly=true)
    public CursorPageResponse<ReviewReplyResponse> list(UUID review,UUID viewer,int size,String cursor) {
        new PageRequest(1,size);
        access.readable(review);
        Instant time=null;UUID id=null;
        if(cursor!=null&&!cursor.isBlank()) {
            try {
                if(cursor.length()>256) throw new IllegalArgumentException();
                String[] parts=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8).split("\\|",-1);
                if(parts.length!=3||!UUID.fromString(parts[0]).equals(review)) throw new IllegalArgumentException();
                time=Instant.parse(parts[1]); id=UUID.fromString(parts[2]);
                if(time.isBefore(Instant.parse("0001-01-01T00:00:00Z"))||time.isAfter(Instant.parse("9999-12-31T23:59:59Z"))) throw new IllegalArgumentException();
            } catch(RuntimeException e) {throw new BusinessException("VALIDATION_ERROR","回复游标无效",HttpStatus.BAD_REQUEST);}
        }
        var rows=store.page(review,viewer,size+1,time,id);
        boolean more=rows.size()>size;
        var visible=List.copyOf(rows.subList(0,Math.min(size,rows.size())));
        String next=null;
        if(more) {
            var last=visible.get(visible.size()-1);
            next=Base64.getUrlEncoder().withoutPadding().encodeToString((review+"|"+last.createdAt()+"|"+last.id()).getBytes(StandardCharsets.UTF_8));
        }
        return new CursorPageResponse<>(visible,store.count(review),more,next);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,readOnly=true)
    public PageResponse<ReviewReplyResponse> mine(UUID user,PageRequest page) {
        return PageResponse.of(store.mine(user,page.size(),page.offset()),store.countMine(user),page);
    }
    @Transactional
    public void delete(UUID id,UUID user,boolean admin) {
        var before=store.find(id,false);
        if(before==null||!admin&&!before.userId().equals(user)) throw missing();
        access.lock(before.reviewId(),false,false);
        var locked=store.find(id,true);
        if(locked==null) throw missing();
        store.delete(id);
    }
    private ReviewReplyCreatedResponse replay(ReviewReplyStore.Stored row,UUID review,CreateReviewReplyRequest body) {
        if(!row.reviewId().equals(review)||!Objects.equals(row.replyToId(),body.replyToId())||!row.content().equals(body.content().strip()))
            throw new BusinessException("REPLY_REQUEST_CONFLICT","同一操作ID不能用于不同回复",HttpStatus.CONFLICT);
        return ack(row);
    }
    private ReviewReplyCreatedResponse ack(ReviewReplyStore.Stored row) {return new ReviewReplyCreatedResponse(row.id(),row.status(),row.createdAt());}
    private BusinessException missing() {return new BusinessException("REPLY_NOT_FOUND","回复不存在或暂不可用",HttpStatus.NOT_FOUND);}
}
