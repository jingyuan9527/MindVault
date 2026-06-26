package com.mindvault.auto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * R1 自动处理编排器，作为 KnowledgeService 和 AutoProcessService 之间的单一协调入口。
 * 消除 {@code KnowledgeServiceImpl ↔ AutoProcessServiceImpl} 的循环依赖。
 */
@Service
public class AutoProcessOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AutoProcessOrchestrator.class);

    private final AutoProcessService autoProcessService;

    public AutoProcessOrchestrator(AutoProcessService autoProcessService) {
        this.autoProcessService = autoProcessService;
    }

    /** 同步生成 AI 标题 */
    public String generateAiTitle(Long knowledgeId, String userTitle, String content) {
        return autoProcessService.generateAiTitleSync(knowledgeId, userTitle, content);
    }

    /** 异步触发 R1 自动处理 */
    public void processAsync(Long knowledgeId, String userTitle, String content) {
        autoProcessService.autoProcessAsync(knowledgeId, userTitle, content);
    }
}