package com.FoodtimeNeo.authz;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single entry point for capability checks and capability snapshots. Role-level results are cached
 * for a short period keyed by actor identity, status and policy revision; resource-level decisions
 * still consult the policy directly so a future context-aware policy is never bypassed by the cache.
 */
@Service
public class CapabilityService {
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private final AuthorizationPolicy policy;
    private final Clock clock;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Set<Capability> capabilities, Instant expiresAt) { }

    public CapabilityService(AuthorizationPolicy policy, Clock clock) {
        this.policy = policy;
        this.clock = clock;
    }

    public Set<Capability> capabilities(UserProfile actor) {
        if (actor == null) return Set.of();
        String key = actor.id() + "|" + actor.role() + "|" + actor.status() + "|" + policy.revision();
        Instant now = clock.instant();
        Cached cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) return cached.capabilities();
        Set<Capability> resolved = Set.copyOf(policy.capabilities(actor));
        cache.put(key, new Cached(resolved, now.plus(CACHE_TTL)));
        if (cache.size() > 256) {
            cache.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        }
        return resolved;
    }

    public boolean allows(UserProfile actor, Capability capability, AuthorizationContext context) {
        if (actor == null || !"active".equals(actor.status())) return false;
        return policy.allows(actor, capability, context == null ? AuthorizationContext.none() : context);
    }

    public void require(UserProfile actor, Capability capability, AuthorizationContext context) {
        if (!allows(actor, capability, context)) {
            throw new BusinessException("ACCESS_DENIED", "无权执行该操作", HttpStatus.FORBIDDEN);
        }
    }

    public CapabilitySnapshot snapshot(UserProfile actor) {
        List<String> names = capabilities(actor).stream()
                .map(Capability::wireName)
                .sorted()
                .toList();
        return new CapabilitySnapshot(names, policy.revision());
    }

    /** Used by tests and future policy-change events; production policy changes also change revision(). */
    public void invalidate() {
        cache.clear();
    }
}
