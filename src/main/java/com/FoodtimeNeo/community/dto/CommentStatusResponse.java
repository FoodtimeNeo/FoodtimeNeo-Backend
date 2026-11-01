package com.FoodtimeNeo.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** 轻量审核状态查询，供评论提交后轮询，避免整页刷新。 */
@Schema(description = "评论审核状态；只有作者本人或已通过的评论可见")
public record CommentStatusResponse(UUID id, String status) { }
