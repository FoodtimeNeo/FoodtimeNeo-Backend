package com.FoodtimeNeo.dining.entity;

import java.util.UUID;

/** 档口图集行：仅展示字段；可见性跟随所属档口与食堂，读取时按 sort_order ASC, id ASC 排序。 */
public record StallImage(UUID stallId, UUID id, String imageUrl, Integer sortOrder) { }
