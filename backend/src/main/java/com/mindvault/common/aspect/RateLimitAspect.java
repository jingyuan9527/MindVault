package com.mindvault.common.aspect;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mindvault.common.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口限流切面
 *
 * 拦截所有标注 @RateLimit 的方法，基于 Caffeine 本地缓存实现令牌桶算法。
 * 限流维度：客户端 IP + 接口方法签名（或自定义 key），
 * 超出 capacity 阈值时直接抛出异常，由 GlobalExceptionHandler 统一处理。
 *
 * 注意：这是单机限流方案，多实例部署时需改为 Redis 等分布式方案。
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /** 计数器缓存，key = "clientIp:methodSignature"，每分钟自动过期 */
    private final Cache<String, AtomicInteger> counter;

    public RateLimitAspect() {
        this.counter = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();
    }

    /** 环绕增强：校验请求频率，超过容量时抛出限流异常 */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String clientIp = getClientIp(request);
        String key = rateLimit.key().isEmpty()
                ? clientIp + ":" + joinPoint.getSignature().toShortString()
                : rateLimit.key() + ":" + clientIp;

        AtomicInteger count = counter.get(key, k -> new AtomicInteger(0));
        int current = count.incrementAndGet();

        if (current > rateLimit.capacity()) {
            log.warn("触发限流: key={}, count={}, capacity={}", key, current, rateLimit.capacity());
            throw new RuntimeException("请求过于频繁，请稍后重试");
        }

        return joinPoint.proceed();
    }

    /**
     * 获取客户端真实 IP
     * 优先从 X-Forwarded-For 取（经过代理时），其次 X-Real-IP，最后 getRemoteAddr
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
