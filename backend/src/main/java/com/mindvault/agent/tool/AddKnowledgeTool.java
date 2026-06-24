package com.mindvault.agent.tool;

import com.mindvault.knowledge.KnowledgeService;
import com.mindvault.knowledge.entity.Knowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

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