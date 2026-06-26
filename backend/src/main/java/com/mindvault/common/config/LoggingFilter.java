package com.mindvault.common.config;

import com.mindvault.common.service.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 请求日志过滤器
 *
 * 基于 OncePerRequestFilter，对每次请求执行：
 * 1. 生成/传递 TraceId（优先取 X-Trace-Id 请求头，否则自动生成 16 位 Hex）
 * 2. 记录 ACCESS_LOG（方法 + 路径 + 状态码 + 耗时）
 * 3. 记录慢请求告警（>2s）和异常请求详情（>=400）
 * 4. 更新 MetricsService 的活跃连接数统计
 * 5. 脱敏敏感字段（密码、Token、API Key 等）
 *
 * 过滤白名单（SKIP_PATHS）：actuator 端点 / API 文档 / Swagger 等不记录
 */
@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private static final Logger accessLog = LoggerFactory.getLogger("ACCESS_LOG");

    private static final Set<String> SKIP_PATHS = new HashSet<>(Arrays.asList(
            "/actuator", "/api/v1/actuator", "/v3/api-docs", "/swagger-ui", "/webjars", "/doc.html"
    ));

    /** 敏感字段正则：匹配 JSON 中的 apiKey/password/token/secret 等字段 */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "\"(apiKey|password|oldPassword|newPassword|secret|token)\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final String MASK = "\"$1\":\"***\"";

    private final MetricsService metricsService;

    public LoggingFilter(ObjectProvider<MetricsService> metricsServiceProvider) {
        this.metricsService = metricsServiceProvider.getIfAvailable();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATHS.stream().anyMatch(path::contains);
    }

    /**
     * 核心过滤逻辑：
     * 1. 包装 Request/Response 以支持缓存读取（ContentCaching）
     * 2. 设置 TraceId（请求头 + MDC + 响应头）
     * 3. 执行过滤链
     * 4. 结束后记录访问日志、更新指标
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);

        long startTime = System.currentTimeMillis();
        if (metricsService != null) {
            metricsService.incrementActiveConnections();
        }

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String path = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
            String query = request.getQueryString();
            String url = query != null ? path + "?" + query : path;

            int status = responseWrapper.getStatus();
            String method = request.getMethod();

            accessLog.info("{} {} {} {}ms traceId={}", method, url, status, duration, traceId);

            if (metricsService != null) {
                metricsService.recordApiCall(method, path, status);

                if (status >= 400) {
                    metricsService.recordApiError(method, path, status);
                }
            }

            if (status >= 400) {
                String requestBody = getRequestBody(requestWrapper);
                if (requestBody != null && !requestBody.isEmpty()) {
                    log.warn("请求失败 {} {} status={} body={} traceId={}", method, url, status, truncate(requestBody, 2000), traceId);
                }
            }

            if (duration > 2000) {
                log.warn("慢请求: {} {} 耗时 {}ms traceId={}", method, url, duration, traceId);
            }

            if (metricsService != null) {
                metricsService.decrementActiveConnections();
            }
            MDC.clear();
            responseWrapper.copyBodyToResponse();
        }
    }

    /** 获取请求体内容（经内容缓存包装后读取） */
    private String getRequestBody(ContentCachingRequestWrapper wrapper) {
        byte[] buf = wrapper.getContentAsByteArray();
        if (buf.length > 0) {
            String body = new String(buf, StandardCharsets.UTF_8);
            return maskSensitive(body);
        }
        return null;
    }

    /** 脱敏请求体中的敏感字段（密码、Token、Key 替换为 ***） */
    private String maskSensitive(String body) {
        if (body == null) return null;
        return SENSITIVE_PATTERN.matcher(body).replaceAll(MASK);
    }

    /** 截断字符串到指定长度并追加 "..." */
    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}