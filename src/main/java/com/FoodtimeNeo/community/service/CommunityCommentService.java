package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.dto.CommunityCommentResponse;
import com.FoodtimeNeo.community.dto.CreateCommentRequest;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import com.FoodtimeNeo.submission.service.SubmissionRateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.Map;
import java.util.UUID;

/** Community comments: same pending → AI instant → auto/human pipeline as posts. */
@Service
@Validated
public class CommunityCommentService {
    private final CommunityStore comments;
    private final SubmissionRateLimiter limiter;
    private final ApplicationEventPublisher events;
    public CommunityCommentService(CommunityStore comments, SubmissionRateLimiter limiter,
                                   ApplicationEventPublisher events) {
        this.comments = comments; this.limiter = limiter; this.events = events;
    }

    @Transactional
    public Map<String, UUID> submit(@NotNull UUID userId, @NotNull UUID postId,
                                    @Valid CreateCommentRequest body, String remoteAddress) {
        limiter.check(userId, remoteAddress);
        if (!comments.postVisibleForComment(postId)) {
            throw new BusinessException("POST_NOT_FOUND", "动态不存在或暂不可见", HttpStatus.NOT_FOUND);
        }
        UUID id = comments.createComment(postId, userId, body.content());
        events.publishEvent(new SubmissionDiagnoseEvent("community-comment", id));
        return Map.of("id", id);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityCommentResponse> list(@NotNull UUID postId, @NotNull PageRequest page) {
        return PageResponse.of(comments.comments(postId, page.size(), page.offset()),
                comments.commentCount(postId), page);
    }

    @Transactional
    public void delete(@NotNull UUID id, @NotNull UUID userId) {
        if (comments.deleteCommentOwned(id, userId) == 0) {
            throw new BusinessException("COMMENT_NOT_FOUND", "评论不存在或不属于你", HttpStatus.NOT_FOUND);
        }
    }
}
