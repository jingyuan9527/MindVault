package com.mindvault.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.common.service.LlmFailoverService;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import com.mindvault.systemconfig.SystemConfigService;
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
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public WritingService(ModelConfigService modelConfigService,
                          LlmFailoverService llmFailoverService,
                          KnowledgeMapper knowledgeMapper,
                          SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.llmFailoverService = llmFailoverService;
        this.knowledgeMapper = knowledgeMapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public String generateArticle(String topic, String style, String keywords) {
        List<ModelConfig> models = modelConfigService.getAvailableChatModels();
        if (models.isEmpty()) {
            return config.getString("default.writing.no-model-message", "系统未配置可用模型，请先在设置中添加并启用模型。");
        }

        List<Knowledge> related = findRelatedKnowledge(topic, keywords);
        StringBuilder knowledgeContext = new StringBuilder();
        if (!related.isEmpty()) {
            int contentTruncate = config.getInt("threshold.writing.content-truncate-length", 500);
            knowledgeContext.append("以下是从知识库中检索到的相关参考内容：\n\n");
            for (Knowledge k : related) {
                knowledgeContext.append("[标题]: ").append(k.getTitle()).append("\n");
                if (k.getSummary() != null) {
                    knowledgeContext.append("[摘要]: ").append(k.getSummary()).append("\n");
                }
                knowledgeContext.append("[内容]: ").append(LlmFailoverService.truncate(k.getContent(), contentTruncate)).append("\n\n");
            }
        }

        String defaultStyle = config.getString("default.writing.style", "写作风格: 清晰、条理、专业");
        String styleGuide = style != null && !style.isBlank()
                ? "写作风格: " + style : defaultStyle;
        String kwGuide = keywords != null && !keywords.isBlank()
                ? "关键词: " + keywords : "";

        double temperature = config.getDouble("threshold.writing.temperature", 0.7);
        int maxTokens = config.getInt("threshold.writing.max-tokens", 4096);
        String promptTmpl = config.getPrompt("prompt.writing.article",
                "你是一个基于个人知识库的写作助手。请根据参考内容撰写一篇文章。\n\n"
                + "%s\n%s\n\n主题: %s\n\n%s\n请撰写一篇结构完整、内容详实的文章。使用中文。");
        String knowledgePart = !related.isEmpty() ? knowledgeContext.toString()
                : "（知识库中没有直接相关的参考内容，请基于你的知识进行创作）\n\n";
        String prompt = String.format(promptTmpl, styleGuide, kwGuide, topic, knowledgePart);

        String result = llmFailoverService.call(models, new LlmFailoverService.LlmCallOptions(prompt, temperature, maxTokens, true, "WRITING"));
        return result != null ? result : config.getString("default.writing.fallback-message", "文章生成失败，请稍后重试。");
    }

    private List<Knowledge> findRelatedKnowledge(String topic, String keywords) {
        Set<String> searchTerms = new LinkedHashSet<>();
        int minTermLen = config.getInt("threshold.writing.min-term-length", 1);
        if (topic != null) {
            Arrays.stream(topic.split("[\\s,，、]+"))
                    .filter(s -> s.length() >= minTermLen)
                    .forEach(searchTerms::add);
        }
        if (keywords != null) {
            Arrays.stream(keywords.split("[\\s,，、]+"))
                    .filter(s -> s.length() >= minTermLen)
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

        int maxResults = config.getInt("threshold.writing.max-related-results", 5);
        return results.subList(0, Math.min(results.size(), maxResults));
    }
}