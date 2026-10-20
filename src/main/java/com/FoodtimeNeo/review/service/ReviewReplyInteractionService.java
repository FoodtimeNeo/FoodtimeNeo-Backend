package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.review.dto.ReviewLikeResponse;
import com.FoodtimeNeo.review.mapper.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ReviewReplyInteractionService {
    private final ReviewAccessService access;
    private final ReviewReplyStore replies;
    private final ReviewReplyInteractionStore likes;
    public ReviewReplyInteractionService(ReviewAccessService access,ReviewReplyStore replies,ReviewReplyInteractionStore likes) {
        this.access=access;this.replies=replies;this.likes=likes;
    }
    @Transactional
    public ReviewLikeResponse setLike(UUID id,UUID user,boolean liked) {
        var before=replies.find(id,false);
        if(before==null) throw missing();
        access.lock(before.reviewId(),liked,false);
        if(replies.lockForLike(id,liked)==null) throw missing();
        likes.setLike(id,user,liked);
        return likes.state(id,user);
    }
    private BusinessException missing() {return new BusinessException("REPLY_NOT_FOUND","回复不存在或暂不可用",HttpStatus.NOT_FOUND);}
}
