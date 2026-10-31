package com.FoodtimeNeo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 数据看板传输对象。字段名即对外 JSON 名（camelCase），与前后端契约保持一致。 */
public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record TrendDay(
            @Schema(description = "业务日期（Asia/Shanghai），格式 yyyy-MM-dd") String day,
            @Schema(description = "当日新注册用户数") long newUsers,
            @Schema(description = "当日新增点评提交数") long newReviews,
            @Schema(description = "当日新增回复数") long newReplies,
            @Schema(description = "当日新增社区动态数") long newPosts,
            @Schema(description = "当日活跃用户数（有任一行为的去重用户，见 docs/admin-dashboard.md）") long onlinePeak) {
    }

    public record DashboardResponse(
            @Schema(description = "累计注册用户数（status=active）") long registeredUsers,
            @Schema(description = "当前有效会话数") long onlineUsers,
            @Schema(description = "累计正式点评数") long reviews,
            @Schema(description = "累计点评回复数") long replies,
            @Schema(description = "累计已发布社区动态数") long posts,
            @Schema(description = "累计启用食堂数") long halls,
            @Schema(description = "累计启用档口数") long stalls,
            @Schema(description = "累计启用菜品数") long dishes,
            @Schema(description = "快照生成时间") Instant generatedAt,
            @Schema(description = "趋势序列（按日正序，最早一天在前，无数据日期补零）") List<TrendDay> trend) {
    }
}
