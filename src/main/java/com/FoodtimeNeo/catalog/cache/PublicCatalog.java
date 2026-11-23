package com.FoodtimeNeo.catalog.cache;

import java.lang.annotation.*;

/** HTTP revalidation for public catalog DTOs; never annotate personal or administrative responses. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicCatalog { }
