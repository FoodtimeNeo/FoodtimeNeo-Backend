package com.FoodtimeNeo.catalog.cache;

import java.lang.annotation.*;

/** Only immutable, viewer-independent projections may opt in. Authorization remains outside this cache. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CatalogQuery { }
