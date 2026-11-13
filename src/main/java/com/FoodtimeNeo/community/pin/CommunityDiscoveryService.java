package com.FoodtimeNeo.community.pin;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.dto.CommunityPostResponse;
import com.FoodtimeNeo.community.service.CommunityPostService;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** One read snapshot joins independent pin and chronological-feed contracts for discovery only. */
@Service
public class CommunityDiscoveryService {
    public record Page(List<CommunityPostResponse> items,long total,boolean hasMore,String nextCursor,PinDtos.Snapshot pins) { }
    private final CommunityPostService posts;private final CommunityPinService pins;
    public CommunityDiscoveryService(CommunityPostService posts,CommunityPinService pins) {this.posts=posts;this.pins=pins;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page read(UserProfile actor,int size,String cursor,boolean following,String query,Long expectedPins) {
        PinDtos.Snapshot snapshot=!following&&query.strip().isEmpty()?pins.read(actor,false):null;
        if(snapshot!=null&&cursor!=null&&expectedPins!=null&&snapshot.revision()!=expectedPins)
            throw new BusinessException("PIN_SET_CHANGED","置顶列表已变化，请刷新食记列表",HttpStatus.CONFLICT);
        var page=posts.feedCursor(actor.id(),size,cursor,null,following,query);
        return new Page(page.items(),page.total(),page.hasMore(),page.nextCursor(),snapshot);
    }
}
