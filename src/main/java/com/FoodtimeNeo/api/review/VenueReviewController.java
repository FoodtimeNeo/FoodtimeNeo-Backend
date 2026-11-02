package com.FoodtimeNeo.api.review;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.review.dto.ReviewLikeResponse;
import com.FoodtimeNeo.review.service.ReviewActionRateLimiter;
import com.FoodtimeNeo.submission.service.SubmissionRateLimiter;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.venue.*;
import com.FoodtimeNeo.venue.VenueModels.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class VenueReviewController {
  private final VenueReviewService service;
  private final SubmissionRateLimiter submissions;
  private final ReviewActionRateLimiter actions;

  public VenueReviewController(
      VenueReviewService service,
      SubmissionRateLimiter submissions,
      ReviewActionRateLimiter actions) {
    this.service = service;
    this.submissions = submissions;
    this.actions = actions;
  }

  private UserProfile user(HttpServletRequest request) {
    return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
  }

  private String type(HttpServletRequest request) {
    return request.getRequestURI().contains("/dining-halls/") ? "hall" : "stall";
  }

  private void privateResponse(HttpServletResponse response) {
    response.setHeader("Cache-Control", "private, no-store");
    response.setHeader("Vary", "Cookie");
  }

  @GetMapping({"/dining-halls/{id}/rating", "/stalls/{id}/rating"})
  public ApiResponse<Target> target(
      @PathVariable UUID id, HttpServletRequest request, HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(service.target(type(request), id));
  }

  @GetMapping({"/dining-halls/{id}/reviews", "/stalls/{id}/reviews"})
  public ApiResponse<CursorPageResponse<Row>> list(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "all") String rating,
      HttpServletRequest request,
      HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(
        service.list(type(request), id, user(request).id(), size, cursor, rating));
  }

  @GetMapping({"/dining-halls/{id}/reviews/me", "/stalls/{id}/reviews/me"})
  public ApiResponse<Mine> mine(
      @PathVariable UUID id, HttpServletRequest request, HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(service.mine(type(request), id, user(request).id()));
  }

  @PostMapping({"/dining-halls/{id}/reviews", "/stalls/{id}/reviews"})
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<Row> submit(
      @PathVariable UUID id, @Valid @RequestBody ReviewInput body, HttpServletRequest request) {
    var user = user(request);
    submissions.check(user.id(), request.getRemoteAddr());
    return ApiResponse.success(service.submit(type(request), id, user.id(), body));
  }

  @GetMapping("/users/me/venue-reviews")
  public ApiResponse<PageResponse<Row>> personal(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String type,
      HttpServletRequest request,
      HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(
        service.personal(user(request).id(), type, false, new PageRequest(page, size)));
  }

  @GetMapping("/users/me/venue-review-replies")
  public ApiResponse<PageResponse<Row>> personalReplies(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String type,
      HttpServletRequest request,
      HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(
        service.personal(user(request).id(), type, true, new PageRequest(page, size)));
  }

  @GetMapping("/users/me/venue-review-submissions/{id}/status")
  public ApiResponse<Row> status(
      @PathVariable UUID id, HttpServletRequest request, HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(service.status(id, user(request).id(), false));
  }

  @GetMapping("/users/me/venue-review-replies/{id}/status")
  public ApiResponse<Row> replyStatus(
      @PathVariable UUID id, HttpServletRequest request, HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(service.status(id, user(request).id(), true));
  }

  @DeleteMapping("/users/me/venue-reviews/{id}")
  public ApiResponse<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
    service.deleteReview(id, user(request).id(), false, null);
    return ApiResponse.success(null);
  }

  @DeleteMapping("/admin/venue-reviews/{id}")
  public ApiResponse<Void> deleteAdmin(
      @PathVariable UUID id, @Valid @RequestBody Reason body, HttpServletRequest request) {
    service.deleteReview(id, user(request).id(), true, body.reason());
    return ApiResponse.success(null);
  }

  @GetMapping("/admin/venue-moderation/{type}/{id}")
  public ApiResponse<AuditDetail> auditDetail(
      @PathVariable String type,
      @PathVariable UUID id,
      HttpServletRequest request,
      HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(service.auditDetail(user(request), type, id));
  }

  @GetMapping("/venue-reviews/{id}/replies")
  public ApiResponse<CursorPageResponse<Row>> replies(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request,
      HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(service.replies(id, user(request).id(), size, cursor, false));
  }

  @GetMapping("/venue-reviews/{id}/replies/me")
  public ApiResponse<CursorPageResponse<Row>> ownReplies(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request,
      HttpServletResponse response) {
    privateResponse(response);
    return ApiResponse.success(service.replies(id, user(request).id(), size, cursor, true));
  }

  @PostMapping("/venue-reviews/{id}/replies")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<Row> reply(
      @PathVariable UUID id, @Valid @RequestBody ReplyInput body, HttpServletRequest request) {
    submissions.check(user(request).id(), request.getRemoteAddr());
    return ApiResponse.success(service.submitReply(id, user(request).id(), body));
  }

  @DeleteMapping("/venue-review-replies/{id}")
  public ApiResponse<Void> deleteReply(@PathVariable UUID id, HttpServletRequest request) {
    service.deleteReply(id, user(request).id(), false, null);
    return ApiResponse.success(null);
  }

  @DeleteMapping("/admin/venue-review-replies/{id}")
  public ApiResponse<Void> deleteReplyAdmin(
      @PathVariable UUID id, @Valid @RequestBody Reason body, HttpServletRequest request) {
    service.deleteReply(id, user(request).id(), true, body.reason());
    return ApiResponse.success(null);
  }

  @PutMapping("/venue-reviews/{id}/like")
  public ApiResponse<ReviewLikeResponse> like(@PathVariable UUID id, HttpServletRequest request) {
    return like(id, request, true, false);
  }

  @DeleteMapping("/venue-reviews/{id}/like")
  public ApiResponse<ReviewLikeResponse> unlike(@PathVariable UUID id, HttpServletRequest request) {
    return like(id, request, false, false);
  }

  @PutMapping("/venue-review-replies/{id}/like")
  public ApiResponse<ReviewLikeResponse> likeReply(
      @PathVariable UUID id, HttpServletRequest request) {
    return like(id, request, true, true);
  }

  @DeleteMapping("/venue-review-replies/{id}/like")
  public ApiResponse<ReviewLikeResponse> unlikeReply(
      @PathVariable UUID id, HttpServletRequest request) {
    return like(id, request, false, true);
  }

  private ApiResponse<ReviewLikeResponse> like(
      UUID id, HttpServletRequest request, boolean liked, boolean reply) {
    actions.check(user(request).id(), request.getRemoteAddr());
    return ApiResponse.success(service.like(id, user(request).id(), liked, reply));
  }
}
