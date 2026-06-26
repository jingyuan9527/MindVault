package com.mindvault.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auth.entity.ApiToken;
import com.mindvault.auth.service.ApiTokenService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证过滤器，处理所有 API 请求的鉴权逻辑。
 *
 * <p>通过 Spring Boot @ConditionalOnProperty 控制启用/禁用——测试环境下
 * 设置 mindvault.auth.enabled=false 可直接跳过认证，方便集成测试。</p>
 *
 * <p>认证流程（两阶段验证）：
 * <ol>
 *   <li><b>Session 验证</b>：先从 Authorization 头提取 Bearer token，交给 SessionManager
 *       验证是否为有效会话（存在于内存且未过期）</li>
 *   <li><b>API Token 退路</b>：Session 验证失败后，尝试用 ApiTokenService 验证是否为
 *       持久化 API 令牌</li>
 * </ol>
 * 任意一种验证通过后，将用户信息写入 UserContext，放行请求；均失败则返回 401。</p>
 *
 * <p>放行路径（无需认证）：
 * <ul>
 *   <li>/api/v1/auth/login — 登录接口</li>
 *   <li>/api/v1/system/health — 健康检查</li>
 *   <li>/api/v1/system/info — 系统信息</li>
 *   <li>/api/v1/actuator/** — Actuator 端点</li>
 *   <li>/v3/**, /swagger-ui/**, /doc.html — API 文档</li>
 *   <li>/favicon.ico, /error — 浏览器默认请求和错误路径</li>
 * </ul>
 * OPTIONS 请求（CORS 预检）也直接放行。</p>
 *
 * @see SessionManager
 * @see ApiTokenService
 * @see UserContext
 */
@Component
@ConditionalOnProperty(name = "mindvault.auth.enabled", havingValue = "true", matchIfMissing = true)
@Order(1)
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final SessionManager sessionManager;
    private final ApiTokenService apiTokenService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> SKIP_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/system/health",
            "/api/v1/system/info",
            "/api/v1/actuator/**",
            "/v3/**",
            "/swagger-ui/**",
            "/doc.html",
            "/favicon.ico",
            "/error"
    );

    public AuthFilter(SessionManager sessionManager, ApiTokenService apiTokenService) {
        this.sessionManager = sessionManager;
        this.apiTokenService = apiTokenService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method) || isSkippable(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "缺少认证令牌");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            sendUnauthorized(response, "认证令牌为空");
            return;
        }

        SessionManager.SessionInfo session = sessionManager.validate(token);
        if (session != null) {
            UserContext.set(new UserContext.UserInfo(session.userId(), session.username(), session.role()));
            try {
                chain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
            return;
        }

        ApiToken apiToken = apiTokenService.validateToken(token);
        if (apiToken != null) {
            UserContext.set(new UserContext.UserInfo(apiToken.getUserId(), "api", "API"));
            try {
                chain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
            return;
        }

        sendUnauthorized(response, "令牌无效或已过期");
    }

    /**
     * 判断请求路径是否在免认证白名单中。
     *
     * @param path 请求 URI
     * @return true 表示该路径无需认证即可访问
     */
    private boolean isSkippable(String path) {
        return SKIP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 返回 401 未授权 JSON 响应。
     *
     * @param response HTTP 响应对象
     * @param message  错误描述信息
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
