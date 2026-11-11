package com.FoodtimeNeo.safety;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class SafetyService {
    private final SafetyStore store;

    public SafetyService(SafetyStore store) {
        this.store = store;
    }

    public record BlockState(boolean blocked) {}
    public record BlockedUser(UUID id, String displayName, String avatarUrl, Instant blockedAt) {}
    public record ReportRow(UUID id, String targetType, UUID targetId, UUID targetUserId, UUID postId,
                            String reason, String excerpt, Instant createdAt) {}

    public BlockState blockState(UUID actor, UUID target) {
        return new BlockState(store.blockedBy(actor, target));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<BlockedUser> blockedUsers(UUID actor, PageRequest page) {
        return PageResponse.of(store.blockedUsers(actor, page.size(), page.offset()),
                store.blockedCount(actor), page);
    }

    @Transactional
    public BlockState setBlock(UUID actor, UUID target, boolean blocked) {
        if (actor.equals(target)) {
            throw new BusinessException("VALIDATION_ERROR", "不能拉黑自己", HttpStatus.BAD_REQUEST);
        }
        store.lockPair(actor, target);
        if (blocked) {
            store.block(actor, target);
            if (!store.blockedBy(actor, target)) throw unavailable();
        } else {
            store.unblock(actor, target);
        }
        return new BlockState(blocked);
    }

    @Transactional
    public void report(UUID reporter, String type, UUID target, String reason) {
        store.lockReporter(reporter);
        if (store.hasReport(reporter, type, target)) return;
        if (store.recentReports(reporter) >= 20) {
            throw new BusinessException("REPORT_RATE_LIMITED", "今日举报次数已达上限", HttpStatus.TOO_MANY_REQUESTS);
        }
        if (!store.report(reporter, type, target, reason.strip())) throw unavailable();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<ReportRow> pending(PageRequest page) {
        return PageResponse.of(store.pending(page.size(), page.offset()), store.pendingCount(), page);
    }

    public void review(UUID id, UUID reviewer) {
        if (!store.review(id, reviewer)) throw unavailable();
    }

    private static BusinessException unavailable() {
        return new BusinessException("SAFETY_TARGET_NOT_FOUND", "目标不存在或不可访问", HttpStatus.NOT_FOUND);
    }
}
