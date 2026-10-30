package com.FoodtimeNeo.notice;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

/**
 * 公告服务：用户端读与已读标记，管理端增删改查；鉴权在首行 guard()。
 */
@Service
@Validated
public class NoticeService {
    private final NoticeStore store;
    public NoticeService(NoticeStore store) { this.store = store; }

    @Transactional(readOnly = true)
    public NoticeDtos.UnreadCountResponse unreadCount(UUID userId) {
        return new NoticeDtos.UnreadCountResponse(store.unreadCount(userId));
    }

    @Transactional(readOnly = true)
    public NoticeDtos.CurrentNoticeResponse current(UUID userId) {
        return store.current(userId);
    }

    @Transactional
    public NoticeDtos.ConfirmNoticeResponse confirm(UUID userId, UUID noticeId,
                                                    @Valid NoticeDtos.ConfirmNoticeRequest body) {
        if (!store.exists(noticeId))
            throw new BusinessException("NOTICE_NOT_FOUND", "公告不存在", HttpStatus.NOT_FOUND);
        return new NoticeDtos.ConfirmNoticeResponse(true, store.confirm(noticeId, userId, body.dontShowAgain()));
    }

    @Transactional(readOnly = true)
    public NoticeDtos.NoticeFeedResponse feed(UUID userId, long sinceSeq, int limit) {
        if (sinceSeq < 0) throw new BusinessException("VALIDATION_ERROR", "since_seq不能为负数", HttpStatus.BAD_REQUEST);
        if (limit < 1 || limit > 100) throw new BusinessException("VALIDATION_ERROR", "limit必须在1到100之间", HttpStatus.BAD_REQUEST);
        return new NoticeDtos.NoticeFeedResponse(store.feed(sinceSeq, limit), store.unreadCount(userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeDtos.NoticeResponse> list(UUID userId, PageRequest page) {
        return PageResponse.of(store.page(page.size(), page.offset()), store.count(), page);
    }

    @Transactional
    public void markRead(UUID userId, UUID noticeId) {
        if (!store.exists(noticeId)) throw new BusinessException("NOTICE_NOT_FOUND", "公告不存在", HttpStatus.NOT_FOUND);
        store.markRead(noticeId, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeDtos.NoticeResponse> adminList(UserProfile actor, PageRequest page) {
        guard(actor);
        return PageResponse.of(store.page(page.size(), page.offset()), store.count(), page);
    }

    @Transactional(readOnly = true)
    public NoticeDtos.NoticeResponse adminGet(UserProfile actor, UUID id) {
        guard(actor);
        var item = store.find(id);
        if (item == null) throw new BusinessException("NOTICE_NOT_FOUND", "公告不存在", HttpStatus.NOT_FOUND);
        return item;
    }

    @Transactional
    public NoticeDtos.NoticeResponse create(UserProfile actor, @Valid NoticeDtos.CreateNoticeRequest body) {
        guard(actor);
        UUID id = store.create(actor.id(), body.title(), body.body());
        return store.find(id);
    }

    @Transactional
    public NoticeDtos.NoticeResponse update(UserProfile actor, UUID id, @Valid NoticeDtos.UpdateNoticeRequest body) {
        guard(actor);
        if (store.update(id, body.title(), body.body()) == 0)
            throw new BusinessException("NOTICE_NOT_FOUND", "公告不存在", HttpStatus.NOT_FOUND);
        return store.find(id);
    }

    @Transactional
    public void delete(UserProfile actor, UUID id) {
        guard(actor);
        if (store.delete(id) == 0) throw new BusinessException("NOTICE_NOT_FOUND", "公告不存在", HttpStatus.NOT_FOUND);
    }

    private void guard(UserProfile actor) { if (!actor.status().equals("active") || !actor.role().equals("superadmin")) throw new BusinessException("ACCESS_DENIED","需要平台管理员权限",HttpStatus.FORBIDDEN); }
}
