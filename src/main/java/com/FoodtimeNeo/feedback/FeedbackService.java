package com.FoodtimeNeo.feedback;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.notification.NotificationCategory;
import com.FoodtimeNeo.notification.NotificationPublisher;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class FeedbackService {
    private final FeedbackStore store;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final NotificationPublisher notifications;
    public FeedbackService(FeedbackStore store,Clock clock,PlatformTransactionManager manager,
            NotificationPublisher notifications) {
        this.store=store;this.clock=clock;this.transactions=new TransactionTemplate(manager);
        this.notifications=notifications;
    }
    @Transactional
    public FeedbackDtos.Row create(UserProfile actor,FeedbackDtos.Create input,String device) {
        String content=text(input.content());
        if(content.isBlank())throw invalid("反馈内容不能为空");
        store.lockSubmit(actor.id(),device);
        var row=store.requested(actor.id(),input.clientRequestId());
        if(row==null) {
            var now=clock.instant();boolean visitor="visitor".equals(actor.role());
            var day=now.atOffset(ZoneOffset.ofHours(8)).toLocalDate();
            var start=day.atStartOfDay().toInstant(ZoneOffset.ofHours(8));
            var end=day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.ofHours(8));
            int limit=visitor?2:5;
            if(store.ownerCount(actor.id(),start,end)>=limit)
                throw new BusinessException("FEEDBACK_DAILY_LIMIT",(visitor?"访客":"认证用户")+"每天最多提交 "+limit+" 条反馈，请明天再试",HttpStatus.TOO_MANY_REQUESTS);
            if(store.deviceCount(device,start,end)>=10)
                throw new BusinessException("FEEDBACK_DEVICE_DAILY_LIMIT","本设备今天已提交 10 条反馈，请明天再试",HttpStatus.TOO_MANY_REQUESTS);
            row=store.create(actor.id(),input,content,device,visitor,now);
        }
        if(!row.type().equals(input.type())||!row.content().equals(content))
            throw new BusinessException("FEEDBACK_REQUEST_CONFLICT","提交编号已用于另一条反馈，请重新提交",HttpStatus.CONFLICT);
        return row;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResponse<FeedbackDtos.Row> list(UserProfile actor,boolean admin,boolean archived,PageRequest page) {
        if(admin)guard(actor);
        UUID owner=admin?null:actor.id();
        return PageResponse.of(store.page(owner,archived,page),store.count(owner,archived),page);
    }
    @Transactional(readOnly=true)
    public FeedbackDtos.Row detail(UserProfile actor,boolean admin,UUID id) {
        if(admin)guard(actor);
        var row=store.find(id,admin?null:actor.id());
        if(row==null)throw missing();return row;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResponse<FeedbackDtos.Reply> replies(UserProfile actor,boolean admin,UUID id,PageRequest page) {
        detail(actor,admin,id);
        return PageResponse.of(store.replies(id,page),store.replyCount(id),page);
    }
    @Transactional
    public FeedbackDtos.Row update(UserProfile actor,UUID id,FeedbackDtos.Update input) {
        guard(actor);if(!store.lock(id))throw missing();
        var row=store.find(id,null);
        if(store.hasAction(id,"manual:"+input.clientRequestId()))return row;
        if(row.status().equals("completed"))throw new BusinessException("FEEDBACK_COMPLETED","反馈已完成，请查看历史记录",HttpStatus.CONFLICT);
        String reply=text(input.reply()),status=status(row,input.status());
        if(reply.isBlank()&&status.equals(row.status()))throw invalid("请填写回复或将反馈标为已完成");
        boolean actionCreated=store.action(id,"manual:"+input.clientRequestId());
        if(actionCreated)store.update(id,actor.id(),reply,status);
        if(actionCreated&&!reply.isBlank()) {
            UUID owner=store.owner(id);
            if(owner!=null)notifications.publishEventOnly(NotificationCategory.FEEDBACK,owner,UUID.randomUUID(),
                    "反馈有新回复","你的建议反馈收到新的回复，请到反馈记录查看。","feedback",id,null,null);
        }
        return store.find(id,null);
    }
    @Transactional
    public FeedbackDtos.Export export(UserProfile actor) {
        guard(actor);var rows=store.exportRows();
        String csv=FeedbackCsv.export(rows);
        for(var row:rows)store.markExported(row.id());
        String date=clock.instant().atOffset(ZoneOffset.ofHours(8)).toLocalDate().toString();
        return new FeedbackDtos.Export("反馈建议-"+date+".csv",csv,rows.size());
    }
    public FeedbackDtos.ImportResult importCsv(UserProfile actor,FeedbackDtos.Import input) {
        guard(actor);
        List<List<String>> rows;
        try { rows=FeedbackCsv.parse(input.csv()); }
        catch(IllegalArgumentException error){throw invalid(error.getMessage());}
        if(rows.isEmpty())throw invalid("CSV 为空");
        var headers=rows.getFirst().stream().map(String::strip).toList();
        Set<String> expected=Set.of("反馈编号","反馈类型","反馈内容","回复","处理状态");
        if(headers.size()!=5||!new HashSet<>(headers).equals(expected))
            throw invalid("请保留反馈编号、反馈类型、反馈内容，并添加回复、处理状态两列；列名不能重复");
        int updated=0,skipped=0;var issues=new ArrayList<FeedbackDtos.ImportIssue>();var seen=new HashSet<UUID>();
        for(int index=1;index<rows.size();index++) {
            var values=rows.get(index);int record=index+1;
            if(values.stream().allMatch(String::isBlank))continue;
            try {
                if(values.size()!=headers.size())throw invalid("列数与表头不一致");
                String rawId=values.get(headers.indexOf("反馈编号")).strip();
                UUID id;
                try {id=UUID.fromString(rawId);if(!id.toString().equalsIgnoreCase(rawId))throw new IllegalArgumentException();}
                catch(IllegalArgumentException malformed){throw invalid("反馈编号格式不正确");}
                if(!seen.add(id))throw invalid("反馈编号重复，未重复处理");
                String type=values.get(headers.indexOf("反馈类型")).strip();
                String content=values.get(headers.indexOf("反馈内容")).replace("\r\n","\n").replace('\r','\n');
                String reply=text(values.get(headers.indexOf("回复")));
                String rawStatus=values.get(headers.indexOf("处理状态")).strip();
                String status=rawStatus.isBlank()?null:FeedbackCsv.STATUSES.entrySet().stream()
                        .filter(entry->entry.getValue().equals(rawStatus)).map(Map.Entry::getKey).findFirst()
                        .orElseThrow(()->invalid("处理状态须为待处理、处理中或已完成"));
                String outcome=transactions.execute(ignored->importRow(actor,id,type,content,reply,status));
                if(outcome==null)updated++;
                else {skipped++;issues.add(new FeedbackDtos.ImportIssue(record,outcome));}
            } catch(BusinessException|IllegalArgumentException error) {
                skipped++;issues.add(new FeedbackDtos.ImportIssue(record,error.getMessage()));
            } catch(org.springframework.dao.DataAccessException error) {
                skipped++;issues.add(new FeedbackDtos.ImportIssue(record,"保存失败，该条未修改，可重试导入"));
            }
        }
        return new FeedbackDtos.ImportResult(updated,skipped,List.copyOf(issues));
    }
    private String importRow(UserProfile actor,UUID id,String type,String content,String reply,String inputStatus) {
        if(!store.lock(id))throw missing();
        var row=store.find(id,null);
        if(row.status().equals("completed"))return "已完成反馈，已跳过旧表格中的记录";
        if(!FeedbackCsv.TYPES.get(row.type()).equals(type)||
                !(row.content().equals(content)||FeedbackCsv.cell(row.content()).equals(content)))
            throw invalid("反馈类型或原内容已被改动，请保留导出内容");
        String status=status(row,inputStatus);
        if(reply.isBlank()&&status.equals(row.status()))return "回复及状态没有变化";
        String key="csv:"+digest(reply+"\u0000"+(inputStatus==null?"":inputStatus));
        if(!store.action(id,key))return "该回复和状态已导入，未重复追加";
        store.update(id,actor.id(),reply,status);return null;
    }
    private static String digest(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    static String text(String text) {
        String value=text==null?"":text.replace("\r\n","\n").replace('\r','\n').strip();
        if(value.length()>5000||value.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\t'))
            throw invalid("内容限 5000 字，不能包含控制字符");
        return value;
    }
    static String status(FeedbackDtos.Row row,String value) {
        if(value==null||value.isBlank())return row.status();
        if(!FeedbackCsv.STATUSES.containsKey(value))throw invalid("处理状态不正确");
        if(!value.equals("completed")&&!value.equals(row.status()))
            throw invalid("待处理和处理中由是否导出决定，可保留当前状态或设为已完成");
        return value;
    }
    static BusinessException invalid(String message) { return new BusinessException("VALIDATION_ERROR",message,HttpStatus.BAD_REQUEST); }
    private static BusinessException missing() { return new BusinessException("FEEDBACK_NOT_FOUND","反馈不存在或不可访问",HttpStatus.NOT_FOUND); }
    private static void guard(UserProfile actor) {
        if(actor==null||!"active".equals(actor.status())||!"superadmin".equals(actor.role()))
            throw new BusinessException("ACCESS_DENIED","仅超级管理员可管理反馈",HttpStatus.FORBIDDEN);
    }
}
