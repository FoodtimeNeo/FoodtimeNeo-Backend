package com.FoodtimeNeo.invitation.entity;

import java.time.Instant;
import java.util.UUID;

/** Locked invitation row used to validate and consume a code inside one transaction. */
public record InvitationCode(UUID id, String status, Integer maxUses, int usedCount, Instant expiresAt) { }
