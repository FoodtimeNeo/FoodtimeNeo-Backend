package com.FoodtimeNeo.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** This operation cannot edit another user's identity or password. */
public record UserAccessRequest(
        @NotNull @Pattern(regexp = "user|admin|superadmin") String role,
        @NotNull @Pattern(regexp = "active|disabled") String status) { }
