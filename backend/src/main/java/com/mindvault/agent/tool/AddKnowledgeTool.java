package com.mindvault.agent.tool;

import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * AI 工具：向知识库添加条目。
 * <p>注册为 Spring AI Tool（名称 add_knowledge），供 LLM 在对话过程中调用。
 * 接收标题和内容两个参数，创建 TEXT 类型的 Knowledge 条目并持久化。
 * 输入为标题+内容字符串，输出为保存结果的成功/失败消息。
 * 依赖 KnowledgeService 完成实际的存储和 AI 自动处理。</p>
 */
@Component
public class AddKnowledgeTool {

    private static final Logger log = LoggerFactory.getLogger(AddKnowledgeTool.class);

    private final KnowledgeService knowledgeService;

    public AddKnowledgeTool(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Tool(name = "add_knowledge", description = "向个人知识库中添加一条新的知识条目，包含标题和内容")
    public String addKnowledge(
            @ToolParam(description = "标题") String title,
            @ToolParam(description = "内容") String content) {
        log.info("Agent 调用 add_knowledge: title={}", title);

        Knowledge knowledge = new Knowledge();
        knowledge.setTitle(title);
        knowledge.setContent(content);
        knowledge.setContentType("TEXT");

        try {
            Knowledge saved = knowledgeService.addKnowledge(knowledge);
            String result = "知识已成功保存，ID: " + saved.getId();
            log.info("知识添加成功: id={}", saved.getId());
            return result;
        } catch (Exception e) {
            log.error("知识添加失败: {}", e.getMessage(), e);
            return "知识保存失败: " + e.getMessage();
        }
    }
}