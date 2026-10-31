package com.FoodtimeNeo.user.dto;

import com.FoodtimeNeo.common.api.MediaReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "修改当前用户昵称与头像；昵称必填，头像传null表示清除")
public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称不能超过50个字符")
        @Schema(description = "用户昵称；首尾空白会被去除，去除后须保留至少1个字符且不超过50个字符", minLength = 1, maxLength = 50)
        String displayName,
        @Size(max = 2000, message = "头像引用过长")
        @Pattern(regexp = MediaReference.PATTERN, message = MediaReference.MESSAGE)
        @Schema(description = "本人上传图片返回的同站引用，如 /api/v1/media/{id}；传null清除头像", nullable = true)
        String avatarUrl) { }
