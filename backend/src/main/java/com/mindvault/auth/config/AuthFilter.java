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

    private boolean isSkippable(String path) {
        return SKIP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

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
