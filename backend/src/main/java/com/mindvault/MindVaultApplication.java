package com.mindvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MindVault 知忆 — 启动入口
 *
 * 使用 JDK 21 虚拟线程（Virtual Threads）：
 * Spring Boot 3.2+ 通过 @EnableAsync 或 spring.threads.virtual.enabled=true
 * 可以将异步任务调度到虚拟线程池，大幅提升并发处理能力。
 *
 * AgentScope 的 @AgentScan 由 Spring Boot 自动配置扫描，
 * 所有带 @Agent 注解的 Bean 会自动注册到 AgentScope 运行时。
 */
@SpringBootApplication
public class MindVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindVaultApplication.class, args);
    }
}