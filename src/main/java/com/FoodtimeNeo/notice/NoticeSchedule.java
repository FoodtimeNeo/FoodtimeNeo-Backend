package com.FoodtimeNeo.notice;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import java.time.*;

/** The display interval is [start,end); date-only deadlines include that UTC+8 calendar day. */
public final class NoticeSchedule {
    public static final ZoneOffset ZONE=ZoneOffset.ofHours(8);
    public record Resolved(Instant publishedAt,Instant startsAt,Instant endsAt,boolean dateOnly,
                           boolean isUpdate,boolean published,boolean enabled) {}
    public static Resolved resolve(NoticeDtos.Schedule input,Instant now) {
        if(input==null)return new Resolved(now,now,null,false,false,true,false);
        Instant publishedAt=input.publishedAt()==null?now:input.publishedAt();
        Instant starts=input.displayStartsAt()==null?publishedAt:input.displayStartsAt();
        Instant ends=input.displayEndsAt();boolean dateOnly=input.expiresOn()!=null;
        if(dateOnly&&ends!=null)throw invalid("截止日期与截止时分只能选择一种");
        try {
            if(dateOnly)ends=input.expiresOn().plusDays(1).atStartOfDay().toInstant(ZONE);
            for(Instant time:new Instant[]{publishedAt,starts,ends}) {
                if(time!=null&&(time.atOffset(ZONE).getYear()<1||time.atOffset(ZONE).getYear()>9999))
                    throw invalid("请填写有效的年月日和时间");
            }
        }catch(DateTimeException error){throw invalid("请填写有效的年月日和时间");}
        if(starts.isBefore(publishedAt))throw invalid("开始显示时间不能早于发布时间");
        if(ends!=null&&!ends.isAfter(starts))throw invalid("截止时间必须晚于开始显示时间");
        if(input.enabled()&&!input.published())throw invalid("草稿不能启用弹窗，请先发布公告");
        return new Resolved(publishedAt,starts,ends,dateOnly,input.isUpdate(),input.published(),input.enabled());
    }
    public static String status(Resolved value,Instant now) {
        if(!value.published())return "draft";
        if(now.isBefore(value.publishedAt()))return "scheduled";
        if(!value.enabled())return "paused";
        if(now.isBefore(value.startsAt()))return "waiting";
        if(value.endsAt()!=null&&!now.isBefore(value.endsAt()))return "expired";
        return "active";
    }
    private static BusinessException invalid(String message){return new BusinessException("VALIDATION_ERROR",message,HttpStatus.BAD_REQUEST);}
    private NoticeSchedule(){}
}
