package com.sxj.cache.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sxj.cache.manager.CacheLevel;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cached
{
    String name() default "methodCache";
    
    int timeToLive() default 5;
    
    CacheLevel level() default CacheLevel.NONE;
}
