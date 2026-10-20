package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.admin.dto.RemoveReviewRequest;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.review.service.ReviewReplyService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/review-replies")
public class AdminReviewReplyController {
    private final ReviewReplyService replies;
    public AdminReviewReplyController(ReviewReplyService replies) {this.replies=replies;}
    @DeleteMapping("/{replyId}") @Operation(summary="管理员移除违规回复",description="必须填写原因，软删除并保留审核记录")
    public ApiResponse<Void> delete(@PathVariable UUID replyId,@Valid @RequestBody RemoveReviewRequest body,HttpServletRequest request) {
        UserProfile actor=(UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        replies.delete(replyId,actor.id(),true);
        LoggerFactory.getLogger(AdminReviewReplyController.class).info("Reply removal actor={} target={} reason={}",actor.id(),replyId,body.reason().replace('\n',' ').replace('\r',' '));
        return ApiResponse.success(null);
    }
}
