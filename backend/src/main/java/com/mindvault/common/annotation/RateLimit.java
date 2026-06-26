package com.mindvault.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 *
 * 基于令牌桶算法（Caffeine 本地缓存实现），按客户端 IP + 接口维度限流。
 * 由 RateLimitAspect 切面拦截，超出容量时抛出异常。
 *
 * 字段说明：
 * - capacity: 时间窗口内的最大请求次数（默认 10 次）
 * - duration: 时间窗口长度（默认 60 秒）
 * - key:      自定义限流键前缀，为空则自动拼接 "clientIp:methodSignature"
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int capacity() default 10;
    int duration() default 60;
    String key() default "";
}
