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
import java.time.Clock;
import java.util.List;

/**
 * 公告服务：用户端读与已读标记，管理端增删改查；鉴权在首行 guard()。
 */
@Service
@Validated
public class NoticeService {
    private final NoticeStore store;
    private final Clock clock;
    public NoticeService(NoticeStore store,Clock clock) { this.store = store;this.clock=clock; }

    @Transactional
    public NoticeDtos.UnreadCountResponse unreadCount(UUID userId) {
        var now=clock.instant();store.announceDue(now);
        return new NoticeDtos.UnreadCountResponse(store.unreadCount(userId,now));
    }

    @Transactional
    public NoticeDtos.CurrentNoticeResponse current(UUID userId) {
        var rows=currentSet(userId);return rows.isEmpty()?null:rows.getFirst();
    }
    @Transactional
    public List<NoticeDtos.CurrentNoticeResponse> currentSet(UUID userId) {
        var now=clock.instant();store.announceDue(now);return store.currentSet(userId,now);
    }

    @Transactional
    public NoticeDtos.ConfirmNoticeResponse confirm(UUID userId, UUID noticeId,
                                                    @Valid NoticeDtos.ConfirmNoticeRequest body) {
        if (!store.visible(noticeId,clock.instant()))
            throw new BusinessException("NOTICE_NOT_FOUND", "公告不存在", HttpStatus.NOT_FOUND);
        return new NoticeDtos.ConfirmNoticeResponse(true, store.confirm(noticeId, userId, body.dontShowAgain()));
    }

    @Transactional
    public NoticeDtos.NoticeFeedResponse feed(UUID userId, long sinceSeq, int limit) {
        if (sinceSeq < 0) throw new BusinessException("VALIDATION_ERROR", "since_seq不能为负数", HttpStatus.BAD_REQUEST);
        if (limit < 1 || limit > 100) throw new BusinessException("VALIDATION_ERROR", "limit必须在1到100之间", HttpStatus.BAD_REQUEST);
        var now=clock.instant();store.announceDue(now);
        return new NoticeDtos.NoticeFeedResponse(store.feed(sinceSeq, limit,now), store.unreadCount(userId,now));
    }

    @Transactional
    public PageResponse<NoticeDtos.NoticeResponse> list(UUID userId, PageRequest page) {
        var now=clock.instant();store.announceDue(now);
        return PageResponse.of(store.visiblePage(page.size(), page.offset(),now), store.visibleCount(now), page);
    }

    @Transactional
    public void markRead(UUID userId, UUID noticeId) {
        if (!store.visible(noticeId,clock.instant())) throw new BusinessException("NOTICE_NOT_FOUND", "公告不存在", HttpStatus.NOT_FOUND);
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
        store.announceDue(clock.instant());
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

    @Transactional(readOnly=true)
    public NoticeDtos.SettingsResponse settings(UserProfile actor) { guard(actor);return new NoticeDtos.SettingsResponse(store.configured()); }
    @Transactional
    public NoticeDtos.SettingsResponse settings(UserProfile actor,NoticeDtos.SettingsRequest body) {
        guard(actor);
        if(body.noticeId()!=null&&!store.exists(body.noticeId())) throw new BusinessException("NOTICE_NOT_FOUND","公告不存在",HttpStatus.NOT_FOUND);
        store.configure(body.noticeId());return new NoticeDtos.SettingsResponse(body.noticeId());
    }
    @Transactional(readOnly=true)
    public PageResponse<NoticeDtos.NoticeResponse> search(UserProfile actor,PageRequest page,String query) {
        guard(actor);if(query.isBlank()) return adminList(actor,page);
        return store.search(query.strip(),page);
    }
    @Transactional
    public NoticeDtos.NoticeResponse saveMarkdown(UserProfile actor,UUID id,@Valid NoticeDtos.MarkdownRequest body) {
        guard(actor);
        if(NoticeHeading.title(body.content()).isBlank()) throw new BusinessException("VALIDATION_ERROR","请在 Markdown 中添加非空标题",HttpStatus.BAD_REQUEST);
        store.lockPublications();
        boolean creating=id==null;
        if(creating) id=store.createContent(actor.id(),body.content());
        else if(store.updateContent(id,body.content())==0) throw new BusinessException("NOTICE_NOT_FOUND","公告不存在",HttpStatus.NOT_FOUND);
        if(body.schedule()!=null||creating)store.schedule(id,NoticeSchedule.resolve(body.schedule(),clock.instant()));
        store.announceDue(clock.instant());
        return store.find(id);
    }
    @Transactional
    public NoticeDtos.NoticeResponse schedule(UserProfile actor,UUID id,NoticeDtos.Schedule input) {
        guard(actor);if(!store.exists(id))throw new BusinessException("NOTICE_NOT_FOUND","公告不存在",HttpStatus.NOT_FOUND);
        store.lockPublications();
        store.schedule(id,NoticeSchedule.resolve(input,clock.instant()));store.announceDue(clock.instant());return store.find(id);
    }
    private void guard(UserProfile actor) { if (!actor.status().equals("active") || !actor.role().equals("superadmin")) throw new BusinessException("ACCESS_DENIED","需要平台管理员权限",HttpStatus.FORBIDDEN); }
}
