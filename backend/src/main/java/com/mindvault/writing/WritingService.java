package com.mindvault.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WritingService {

    private static final Logger log = LoggerFactory.getLogger(WritingService.class);

    private final ModelConfigService modelConfigService;
    private final LlmFailoverService llmFailoverService;
    private final KnowledgeMapper knowledgeMapper;
    private final ObjectMapper objectMapper;

    public WritingService(ModelConfigService modelConfigService,
                          LlmFailoverService llmFailoverService,
                          KnowledgeMapper knowledgeMapper) {
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.knowledgeMapper = knowledgeMapper;
        this.objectMapper = new ObjectMapper();
    }

    public String generateArticle(String topic, String style, String keywords) {
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) {
            return "系统未配置可用模型，请先在设置中添加并启用模型。";
        }

        List<Knowledge> related = findRelatedKnowledge(topic, keywords);
        StringBuilder knowledgeContext = new StringBuilder();
        if (!related.isEmpty()) {
            knowledgeContext.append("以下是从知识库中检索到的相关参考内容：\n\n");
            for (Knowledge k : related) {
                knowledgeContext.append("[标题]: ").append(k.getTitle()).append("\n");
                if (k.getSummary() != null) {
                    knowledgeContext.append("[摘要]: ").append(k.getSummary()).append("\n");
                }
                knowledgeContext.append("[内容]: ").append(LlmFailoverService.truncate(k.getContent(), 500)).append("\n\n");
            }
        }

        String styleGuide = style != null && !style.isBlank()
                ? "写作风格: " + style : "写作风格: 清晰、条理、专业";
        String kwGuide = keywords != null && !keywords.isBlank()
                ? "关键词: " + keywords : "";

        String prompt = "你是一个基于个人知识库的写作助手。请根据参考内容撰写一篇文章。\n\n"
                + styleGuide + "\n" + kwGuide + "\n\n"
                + "主题: " + topic + "\n\n"
                + (!related.isEmpty() ? knowledgeContext.toString() : "（知识库中没有直接相关的参考内容，请基于你的知识进行创作）\n\n")
                + "请撰写一篇结构完整、内容详实的文章。使用中文。";

        String result = llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, 0.7, 4096, true, "WRITING"));
        return result != null ? result : "文章生成失败，请稍后重试。";
    }

    private List<Knowledge> findRelatedKnowledge(String topic, String keywords) {
        Set<String> searchTerms = new LinkedHashSet<>();
        if (topic != null) {
            Arrays.stream(topic.split("[\\s,，、]+"))
                    .filter(s -> s.length() > 1)
                    .forEach(searchTerms::add);
        }
        if (keywords != null) {
            Arrays.stream(keywords.split("[\\s,，、]+"))
                    .filter(s -> s.length() > 1)
                    .forEach(searchTerms::add);
        }

        if (searchTerms.isEmpty()) return List.of();

        List<Knowledge> results = new ArrayList<>();
        for (String term : searchTerms) {
            List<Knowledge> matches = knowledgeMapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(term, term);
            for (Knowledge k : matches) {
                if (results.stream().noneMatch(r -> r.getId().equals(k.getId()))) {
                    results.add(k);
                }
            }
        }

        return results.subList(0, Math.min(results.size(), 5));
    }
}