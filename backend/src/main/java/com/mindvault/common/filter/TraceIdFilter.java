package com.mindvault.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 注入过滤器
 *
 * 在每个 HTTP 请求开始时生成一个唯一的 traceId，
 * 注入到 SLF4J 的 MDC (Mapped Diagnostic Context) 中，
 * 使得同一请求的所有日志都可以通过 traceId 关联追溯。
 *
 * MDC 本质上是 ThreadLocal，请求结束后必须清理，
 * 否则会导致虚拟线程池中的线程泄漏 traceId。
 *
 * @Order(1) 确保此过滤器最先执行
 */
@Component
@Order(1)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 优先使用请求头中的 traceId（可用于分布式链路追踪）
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // 将 traceId 放入响应头，方便前端调试
            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后必须清理 MDC，防止 ThreadLocal 泄漏
            MDC.clear();
        }
    }
}