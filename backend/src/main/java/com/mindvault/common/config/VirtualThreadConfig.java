package com.mindvault.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * 虚拟线程配置
 *
 * JDK 21 虚拟线程是轻量级线程（平台线程的"载体"），
 * 一个平台线程可以承载数千个虚拟线程，适合 IO 密集型任务。
 *
 * 此处配置：
 * 1. Tomcat 请求处理使用虚拟线程
 * 2. Spring @Async 异步方法使用虚拟线程池
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * 配置 Tomcat 使用虚拟线程处理 HTTP 请求
     * spring.threads.virtual.enabled=true 的编程式等价实现
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutor() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    /**
     * Spring @Async 注解的默认执行器
     * 所有标注了 @Async 的方法都会在虚拟线程中执行
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.threads", name = "virtual", havingValue = "true")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}