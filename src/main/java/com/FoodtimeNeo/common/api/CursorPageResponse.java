package com.FoodtimeNeo.common.api;

import java.util.List;

public record CursorPageResponse<T>(List<T> items,long total,boolean hasMore,String nextCursor) { }
