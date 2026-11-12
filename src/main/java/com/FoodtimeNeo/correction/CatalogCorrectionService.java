package com.FoodtimeNeo.correction;

import com.FoodtimeNeo.authz.AuthorizationContext;
import com.FoodtimeNeo.authz.Capability;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.correction.CatalogCorrectionDtos.AdminResponse;
import com.FoodtimeNeo.correction.CatalogCorrectionDtos.CreateRequest;
import com.FoodtimeNeo.correction.CatalogCorrectionDtos.DecisionRequest;
import com.FoodtimeNeo.correction.CatalogCorrectionDtos.Response;
import com.FoodtimeNeo.correction.CatalogCorrectionStore.AdminRow;
import com.FoodtimeNeo.correction.CatalogCorrectionStore.CorrectionRow;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CatalogCorrectionService {
    private static final int DAILY_LIMIT = 5;
    private static final int MAX_EVIDENCE = 3;
    private static final java.time.ZoneId BUSINESS_ZONE = java.time.ZoneId.of("Asia/Shanghai");
    private final CatalogCorrectionStore corrections;
    private final ImageStorageService images;
    private final CapabilityService capabilities;
    private final Clock clock;

    public CatalogCorrectionService(CatalogCorrectionStore corrections, ImageStorageService images,
            CapabilityService capabilities, Clock clock) {
        this.corrections = corrections;
        this.images = images;
        this.capabilities = capabilities;
        this.clock = clock;
    }

    @Transactional
    public Response submit(UserProfile actor, CreateRequest request) {
        capabilities.require(actor, Capability.CORRECTION_SUBMIT, AuthorizationContext.none());
        CorrectionTargetType type = targetType(request.targetType());
        List<String> fields = fields(type, request.changedFields());
        String name = fields.contains("name") ? name(request.name(), type) : null;
        String description = fields.contains("description") ? description(request.description()) : null;
        BigDecimal price = fields.contains("price") ? price(request.price()) : null;
        String reason = text(request.reason(), 500, "理由");
        List<String> evidence = evidence(actor, request.evidenceUrls());
        if (request.baseRevision() == null || request.baseRevision() < 1) throw bad("baseRevision必须为正数");
        String hash = hash(type, request.targetId(), request.baseRevision(), fields, name, description, price,
                reason, evidence);
        LocalDate day = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        String idempotencyKey = request.idempotencyKey().strip();
        corrections.lockDaily(actor.id(), day);

        var existing = corrections.findByIdempotency(actor.id(), idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(hash)) throw conflict("IDEMPOTENCY_KEY_REUSED",
                    "幂等键已用于不同内容");
            return response(existing.get(), corrections.evidence(existing.get().id()));
        }
        if (corrections.countOnDay(actor.id(), day) >= DAILY_LIMIT) {
            long retryAfter = Math.max(1, java.time.Duration.between(clock.instant(),
                    day.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()).toSeconds());
            throw new RateLimitException("CORRECTION_DAILY_LIMIT", "今天最多提交5个纠错提案", retryAfter);
        }
        var target = corrections.target(type, request.targetId(), false);
        if (target == null || !target.visible()) throw notFound();
        if (target.revision() != request.baseRevision()) throw conflict("CATALOG_REVISION_CONFLICT",
                "目录资料已更新，请刷新后重新核对");

        UUID id = UUID.randomUUID();
        String summary = target.summary() == null ? "" : target.summary();
        if (summary.codePointCount(0, summary.length()) > 200)
            summary = summary.substring(0, summary.offsetByCodePoints(0, 200));
        CorrectionRow row = new CorrectionRow(id, actor.id(), type, target.id(), summary,
                target.revision(), target.name(), target.description(), target.price(),
                name, description, price, fields, reason, "pending", null, null, null, null, 1, hash);
        try {
            corrections.insert(row, idempotencyKey);
        } catch (DuplicateKeyException duplicate) {
            var replay = corrections.findByIdempotency(actor.id(), idempotencyKey);
            if (replay.isPresent() && replay.get().requestHash().equals(hash)) {
                return response(replay.get(), corrections.evidence(replay.get().id()));
            }
            throw conflict("IDEMPOTENCY_KEY_REUSED", "幂等键已用于不同内容");
        }
        corrections.insertEvidence(id, actor.id(), evidence);
        corrections.insertEvent(UUID.randomUUID(), id, actor.id(), "submitted", null, null,
                target.name(), target.description(), target.price(), name, description, price);
        return response(corrections.findAny(id).orElseThrow(), evidence);
    }

    @Transactional(readOnly = true)
    public PageResponse<Response> mine(UserProfile actor, PageRequest page) {
        return PageResponse.of(corrections.listByOwner(actor.id(), page.size(), page.offset()).stream()
                        .map(row -> response(row, List.of())).toList(),
                corrections.countByOwner(actor.id()), page);
    }

    @Transactional(readOnly = true)
    public Response mine(UserProfile actor, UUID id) {
        CorrectionRow row = corrections.find(actor.id(), id).orElseThrow(this::notFound);
        return response(row, corrections.evidence(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminResponse> adminList(UserProfile actor, String status, String targetType,
            PageRequest page) {
        capabilities.require(actor, Capability.CORRECTION_REVIEW, AuthorizationContext.none());
        String normalizedStatus = status == null || status.isBlank() ? null : status.strip();
        if (normalizedStatus != null && !Set.of("pending", "approved", "rejected").contains(normalizedStatus))
            throw bad("status不合法");
        String normalizedType = null;
        if (targetType != null && !targetType.isBlank()) {
            normalizedType = targetType(targetType).wireName();
        }
        List<AdminResponse> items = corrections.listAdmin(normalizedStatus, normalizedType, page.size(), page.offset())
                .stream().map(row -> adminResponse(row, false)).toList();
        return PageResponse.of(items, corrections.countAdmin(normalizedStatus, normalizedType), page);
    }

    @Transactional(readOnly = true)
    public AdminResponse adminDetail(UserProfile actor, UUID id) {
        capabilities.require(actor, Capability.CORRECTION_REVIEW, AuthorizationContext.none());
        return adminResponse(corrections.findAdmin(id).orElseThrow(this::notFound), true);
    }

    @Transactional
    public AdminResponse decide(UserProfile actor, UUID id, DecisionRequest request) {
        capabilities.require(actor, Capability.CORRECTION_REVIEW, AuthorizationContext.none());
        if (!Set.of("approved", "rejected").contains(request.decision())) throw bad("decision只能为approved或rejected");
        String note = request.reviewNote() == null ? null : text(request.reviewNote(), 500, "审核说明");
        CorrectionRow row = corrections.findForUpdate(id).orElseThrow(this::notFound);
        if (!"pending".equals(row.status())) throw conflict("CORRECTION_ALREADY_DECIDED", "该提案已处理");
        if (row.revision() != request.expectedRevision()) throw conflict("CORRECTION_REVISION_CONFLICT",
                "提案已被其他操作修改，请刷新后重试");

        if ("approved".equals(request.decision())) {
            var target = corrections.target(row.targetType(), row.targetId(), true);
            if (target == null) throw conflict("CATALOG_TARGET_GONE", "目标资料已删除，无法应用");
            if (!target.visible()) throw conflict("CATALOG_TARGET_UNAVAILABLE", "目标资料已停用，无法应用");
            if (target.revision() != row.baseRevision()) throw conflict("CATALOG_REVISION_CONFLICT",
                    "目录资料已更新，请刷新后重新核对");
            if (!corrections.apply(row.targetType(), row.targetId(), row.baseRevision(), row.changedFields(),
                    row.proposedName(), row.proposedDescription(), row.proposedPrice()))
                throw conflict("CATALOG_REVISION_CONFLICT", "目录资料已更新，请刷新后重新核对");
        }
        if (!corrections.decide(id, request.expectedRevision(), request.decision(), note, actor.id()))
            throw conflict("CORRECTION_REVISION_CONFLICT", "提案已被其他操作修改，请刷新后重试");
        corrections.insertEvent(UUID.randomUUID(), id, actor.id(), request.decision(), note, null,
                row.originalName(), row.originalDescription(), row.originalPrice(),
                row.proposedName(), row.proposedDescription(), row.proposedPrice());
        return adminDetail(actor, id);
    }

    private AdminResponse adminResponse(AdminRow row, boolean includeEvidence) {
        CorrectionRow correction = row.correction();
        return new AdminResponse(
                response(correction, includeEvidence ? corrections.evidence(correction.id()) : List.of()),
                correction.submitterId(), row.submitterName(), row.submitterEmail());
    }

    private Response response(CorrectionRow row, List<String> evidence) {
        return new Response(row.id(), row.targetType().wireName(), row.targetId(), row.targetSummary(),
                row.baseRevision(), row.originalName(), row.originalDescription(), row.originalPrice(),
                row.proposedName(), row.proposedDescription(), row.proposedPrice(), row.changedFields(),
                row.reason(), row.status(), row.reviewNote(), row.createdAt(), row.reviewedAt(),
                row.revision(), evidence);
    }

    private static CorrectionTargetType targetType(String value) {
        return CorrectionTargetType.fromWire(value).orElseThrow(() ->
                new BusinessException("VALIDATION_ERROR", "targetType只能为dining_hall、stall或dish",
                        HttpStatus.BAD_REQUEST));
    }

    private static List<String> fields(CorrectionTargetType type, List<String> values) {
        if (values == null || values.isEmpty()) throw bad("changedFields不能为空");
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String value : values) {
            String field = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
            if (!type.supports(field)) throw bad("该目录类型不支持字段：" + field);
            if (!seen.add(field)) throw bad("changedFields不能重复");
            result.add(field);
        }
        if (result.size() > 3) throw bad("changedFields最多3个");
        return result;
    }

    private static String name(String value, CorrectionTargetType type) {
        String normalized = java.text.Normalizer.normalize(value == null ? "" : value.strip(),
                java.text.Normalizer.Form.NFKC);
        if (normalized.isBlank() || normalized.codePointCount(0, normalized.length()) > type.nameLimit())
            throw bad("名称不能为空且不能超过" + type.nameLimit() + "个字符");
        if (normalized.codePoints().anyMatch(Character::isISOControl)) throw bad("名称包含非法字符");
        return normalized;
    }

    private static String description(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = java.text.Normalizer.normalize(value.strip(), java.text.Normalizer.Form.NFKC);
        if (normalized.codePointCount(0, normalized.length()) > 500) throw bad("介绍不能超过500个字符");
        if (normalized.codePoints().anyMatch(Character::isISOControl)) throw bad("介绍包含非法字符");
        return normalized;
    }

    private static BigDecimal price(BigDecimal value) {
        if (value == null) return null;
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.signum() < 0 || normalized.scale() > 2 || normalized.precision() - normalized.scale() > 8)
            throw bad("价格必须为非负数、最多两位小数且不超过8位整数");
        return value;
    }

    private static String text(String value, int max, String label) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw bad(label + "不能为空");
        if (normalized.codePointCount(0, normalized.length()) > max) throw bad(label + "不能超过" + max + "个字符");
        return normalized;
    }

    private List<String> evidence(UserProfile actor, List<String> urls) {
        List<String> result = new ArrayList<>();
        if (urls == null) return result;
        if (urls.size() > MAX_EVIDENCE) throw bad("证据图片最多3张");
        for (String value : urls) {
            if (value == null || value.isBlank()) throw bad("证据图片引用不能为空");
            String url = value.strip();
            if (result.contains(url)) throw bad("证据图片不能重复");
            images.requireOwnedReference(url, actor.id());
            result.add(url);
        }
        return result;
    }

    private static String hash(CorrectionTargetType type, UUID targetId, long baseRevision, List<String> fields,
            String name, String description, BigDecimal price, String reason, List<String> evidence) {
        String canonical = String.join("\u0000", type.wireName(), targetId.toString(),
                Long.toString(baseRevision), String.join(",", fields.stream().sorted().toList()),
                nullToEmpty(name), nullToEmpty(description),
                price == null ? "" : price.toPlainString(), reason,
                String.join("\n", evidence.stream().sorted().toList()));
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private BusinessException notFound() {
        return new BusinessException("CORRECTION_NOT_FOUND", "纠错提案不存在", HttpStatus.NOT_FOUND);
    }

    private static BusinessException bad(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }

    private static final class RateLimitException extends com.FoodtimeNeo.common.exception.RateLimitedException {
        private RateLimitException(String code, String message, long retryAfterSeconds) {
            super(code, message, retryAfterSeconds);
        }
    }
}
