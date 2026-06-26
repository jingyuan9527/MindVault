package com.mindvault.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置（仅 dev profile 生效）
 *
 * 开发阶段允许前端 Vite dev server 跨域访问后端 API。
 * 生产环境由 Nginx 反向代理统一处理跨域，此配置不加载。
 *
 * 策略：允许所有来源/方法/头，支持凭据（credentials）。
 * 仅对 /api/** 路径生效。
 */
@Configuration
@Profile("dev")
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}