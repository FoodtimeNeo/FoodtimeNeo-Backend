package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.review.dto.ReviewLikeResponse;
import com.FoodtimeNeo.review.mapper.ReviewInteractionStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ReviewInteractionService {
    private final ReviewAccessService access;
    private final ReviewInteractionStore store;
    public ReviewInteractionService(ReviewAccessService access,ReviewInteractionStore store) {this.access=access;this.store=store;}
    @Transactional
    public ReviewLikeResponse setLike(UUID id,UUID user,boolean liked) {
        access.lock(id,liked,false);
        store.setLike(id,user,liked);
        return store.state(id,user);
    }
}
