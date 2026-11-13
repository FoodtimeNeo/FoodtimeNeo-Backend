package com.FoodtimeNeo.api.community;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.community.edit.*;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community/posts")
@SecurityRequirement(name="sessionCookie")
public class PostEditController {
    private final PostEditService edits;
    public PostEditController(PostEditService edits){this.edits=edits;}
    @PutMapping("/{id}")
    public ApiResponse<PostEditService.Result> edit(@PathVariable UUID id,@Valid @RequestBody EditPostRequest input,HttpServletRequest request) {
        return ApiResponse.success(edits.edit((UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER),id,input,request.getRemoteAddr()));
    }
}
