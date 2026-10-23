package com.FoodtimeNeo.community.dto;
public record CommunityModerationDetail(CommunityPostResponse post,CommunityCommentResponse comment,
    CommunityCommentResponse rootComment,CommunityCommentResponse replyToComment) { }
