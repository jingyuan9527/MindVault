package com.mindvault.auto.service;

/**
 * AI 自动处理服务（R1 阶段）
 *
 * 对新增的知识条目自动执行以下处理（按顺序）：
 * 1. AI 标题生成 — 基于内容生成精简中文标题
 * 2. AI 标签生成 — 生成 3-5 个中文标签
 * 3. AI 摘要生成 — 生成 50-100 字中文摘要
 * 4. 嵌入向量生成 — 构建语义搜索向量
 *
 * 触发方式：
 * - 自动：知识创建时由 @Async 异步触发
 * - 手动：KnowledgeService.reprocessKnowledge() 触发
 */
public interface AutoProcessService {

    /** 异步触发自动处理（由知识创建/更新事件调用） */
    void autoProcessAsync(Long knowledgeId, String userTitle, String content);

    /** 同步生成 AI 标题并更新到知识条目（用于手动 reprocess） */
    String generateAiTitleSync(Long knowledgeId, String userTitle, String content);

    /** R1 自动处理主流程（同步执行） */
    void autoProcess(Long knowledgeId, String userTitle, String content);
}