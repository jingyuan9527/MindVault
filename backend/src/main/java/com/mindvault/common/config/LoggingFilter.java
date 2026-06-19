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

@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private static final Logger accessLog = LoggerFactory.getLogger("ACCESS_LOG");

    private static final Set<String> SKIP_PATHS = new HashSet<>(Arrays.asList(
            "/actuator", "/api/v1/actuator", "/v3/api-docs", "/swagger-ui", "/webjars", "/doc.html"
    ));

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

    private String getRequestBody(ContentCachingRequestWrapper wrapper) {
        byte[] buf = wrapper.getContentAsByteArray();
        if (buf.length > 0) {
            String body = new String(buf, StandardCharsets.UTF_8);
            return maskSensitive(body);
        }
        return null;
    }

    private String maskSensitive(String body) {
        if (body == null) return null;
        return SENSITIVE_PATTERN.matcher(body).replaceAll(MASK);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}