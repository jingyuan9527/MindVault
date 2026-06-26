package com.mindvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MindVault 知忆 — 应用启动入口
 *
 * 职责：
 * - 初始化 Spring Boot 上下文，扫描所有 @Component / @Service / @Controller / @Mapper 等 Bean
 * - 启用定时任务调度（@EnableScheduling）
 * - JDK 21 虚拟线程通过 VirtualThreadConfig 配置（Tomcat + @Async 均使用虚拟线程）
 *
 * 新人指引：
 * - 应用启动后，从 common → ai → content → knowledge → ... 逐层初始化
 * - 数据源为 PostgreSQL 16 + pgvector，通过 MyBatis-Plus 操作
 * - 前端为 Vue 3，由 Nginx 反向代理访问后端 /api/v1/*
 */
@SpringBootApplication
@EnableScheduling
public class MindVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindVaultApplication.class, args);
    }
}