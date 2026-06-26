package com.mindvault.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / Swagger 接口文档配置
 *
 * 使用 SpringDoc（替代已过时的 SpringFox）集成 OpenAPI 3.0 规范。
 * 提供两个 Bean：
 * - customOpenAPI: 全局 API 元信息（标题、版本、联系方式、协议）
 * - publicApi: 分组配置，匹配 /api/v1/** 路径下的所有端点
 *
 * 访问入口（通过 Nginx 代理）：
 * - Knife4j UI:  /api/doc.html
 * - Swagger UI:  /api/swagger-ui/index.html
 * - OpenAPI JSON: /api/v3/api-docs
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MindVault API")
                        .version("0.1.0")
                        .description("知忆 - 个人知识库 Agent 后端接口文档")
                        .contact(new Contact()
                                .name("MindVault")
                                .email("dev@mindvault.app"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("mindvault")
                .displayName("MindVault API v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}