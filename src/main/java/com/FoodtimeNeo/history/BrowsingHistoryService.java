package com.FoodtimeNeo.history;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.history.BrowsingHistoryDtos.Item;
import com.FoodtimeNeo.history.BrowsingHistoryDtos.RecordRequest;
import com.FoodtimeNeo.history.BrowsingHistoryDtos.SettingsRequest;
import com.FoodtimeNeo.history.BrowsingHistoryDtos.SettingsResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BrowsingHistoryService {
    private static final int MAX_PAGE_SIZE = 50;
    private final BrowsingHistoryStore history;

    public BrowsingHistoryService(BrowsingHistoryStore history) {
        this.history = history;
    }

    @Transactional
    public SettingsResponse settings(UserProfile actor) {
        return history.settings(actor.id());
    }

    @Transactional
    public SettingsResponse updateSettings(UserProfile actor, SettingsRequest request) {
        history.lockSettings(actor.id());
        if (!history.updateSettings(actor.id(), request.enabled(), request.expectedRevision())) {
            throw new BusinessException("HISTORY_SETTINGS_CONFLICT", "设置已被其他操作修改，请刷新后重试",
                    HttpStatus.CONFLICT);
        }
        return history.settings(actor.id());
    }

    @Transactional
    public void record(UserProfile actor, String kindValue, UUID targetId, RecordRequest request) {
        HistoryKind kind = kind(kindValue);
        boolean visible = kind == HistoryKind.DISH ? history.dishVisible(targetId) : history.postVisible(targetId);
        if (!visible) throw new BusinessException("HISTORY_TARGET_NOT_FOUND", "目标不存在或当前不可访问",
                HttpStatus.NOT_FOUND);

        history.lockSettings(actor.id());
        SettingsResponse settings = history.settings(actor.id());
        if (!settings.enabled()) throw new BusinessException("HISTORY_DISABLED", "浏览历史记录已关闭",
                HttpStatus.CONFLICT);
        long current = kind == HistoryKind.DISH ? settings.dishEpoch() : settings.postEpoch();
        if (request.epoch() < current) throw new BusinessException("HISTORY_EPOCH_STALE",
                "浏览历史代次已过期，请刷新后重试", HttpStatus.CONFLICT);
        if (request.epoch() > current) throw new BusinessException("VALIDATION_ERROR", "epoch不合法",
                HttpStatus.BAD_REQUEST);

        history.lockKind(actor.id(), kind);
        history.record(actor.id(), kind, targetId, current, request.visitEventId());
    }

    @Transactional
    public void deleteOne(UserProfile actor, String kindValue, UUID targetId) {
        HistoryKind kind = kind(kindValue);
        history.lockSettings(actor.id());
        if (history.deleteOne(actor.id(), kind, targetId)) history.bumpEpoch(actor.id(), kind);
    }

    @Transactional
    public void clear(UserProfile actor, String kindValue) {
        HistoryKind kind = kind(kindValue);
        history.lockSettings(actor.id());
        history.clear(actor.id(), kind);
        history.bumpEpoch(actor.id(), kind);
    }

    @Transactional(readOnly = true)
    public PageResponse<Item> list(UserProfile actor, String kindValue, PageRequest page) {
        HistoryKind kind = kind(kindValue);
        int size = Math.min(page.size(), MAX_PAGE_SIZE);
        return PageResponse.of(history.list(actor.id(), kind, size, page.offset()),
                history.count(actor.id(), kind), page);
    }

    private static HistoryKind kind(String value) {
        return HistoryKind.fromWire(value).orElseThrow(() ->
                new BusinessException("VALIDATION_ERROR", "kind只能为dish或post", HttpStatus.BAD_REQUEST));
    }
}
