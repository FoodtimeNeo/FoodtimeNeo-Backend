package com.FoodtimeNeo.api.dish;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.pricehistory.*;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name="sessionCookie")
public class DishPriceHistoryController {
    private final PriceHistoryService prices;
    public DishPriceHistoryController(PriceHistoryService prices){this.prices=prices;}
    @GetMapping("/dishes/{id}/price-history")
    @Operation(summary="菜品价格变化",description="启用目录可见；days为0、30或90，记录时间按北京时间展示")
    public ApiResponse<PriceHistoryDtos.History> history(@PathVariable UUID id,@RequestParam(defaultValue="0") int days,HttpServletRequest request){return ApiResponse.success(prices.read(actor(request),id,days));}
    @PutMapping("/admin/dishes/{id}/price-history/{historyId}")
    @Operation(summary="更正价格历史",description="独立管理能力；保留原时间，最新值同步当前价，使用目录版本防止覆盖")
    public ApiResponse<Void> edit(@PathVariable UUID id,@PathVariable long historyId,@Valid @RequestBody PriceHistoryDtos.Edit body,HttpServletRequest request){prices.edit(actor(request),id,historyId,body);return ApiResponse.success(null);}
    @DeleteMapping("/admin/dishes/{id}/price-history/{historyId}")
    @Operation(summary="移除错误价格记录",description="用户不再看到该记录，内部保留审计；唯一有效记录不可删除")
    public ApiResponse<Void> hide(@PathVariable UUID id,@PathVariable long historyId,@RequestParam long expectedRevision,HttpServletRequest request){prices.hide(actor(request),id,historyId,expectedRevision);return ApiResponse.success(null);}
    private static UserProfile actor(HttpServletRequest request){return (UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);}
}
