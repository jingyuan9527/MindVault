package com.mindvault.writing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import com.mindvault.model.service.ModelConfigService;
import com.mindvault.systemconfig.service.SystemConfigService;
import com.mindvault.writing.config.WritingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI 写作助手服务。
 * <p>
 * 核心职责: 根据用户提供的主题、风格和关键词，结合知识库中的相关参考内容，
 * 通过 LLM 生成结构化文章。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>自动从知识库中检索与主题和关键词相关的条目作为参考上下文</li>
 *   <li>LLM 调用使用 PromptRegistry.WRITING_ARTICLE 模板，可自定义温度和 maxTokens</li>
 *   <li>无可用模型时返回配置的提示文案，不抛出异常</li>
 * </ul>
 * </p>
 * <p>依赖: AiService, KnowledgeMapper, ModelConfigService, SystemConfigService</p>
 */
@Service
public class WritingServiceImpl implements WritingService {

    private static final Logger log = LoggerFactory.getLogger(WritingServiceImpl.class);

    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final KnowledgeMapper knowledgeMapper;
    private final SystemConfigService systemConfigService;
    private final WritingProperties writingProperties;
    private final ObjectMapper objectMapper;

    public WritingServiceImpl(ModelConfigService modelConfigService,
                              AiService aiService,
                              KnowledgeMapper knowledgeMapper,
                              WritingProperties writingProperties,
                              SystemConfigService systemConfigService) {
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.knowledgeMapper = knowledgeMapper;
        this.writingProperties = writingProperties;
        this.systemConfigService = systemConfigService;
        this.objectMapper = new ObjectMapper();
    }

    public String generateArticle(String topic, String style, String keywords) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
            return writingProperties.getNoModelMessage();
        }

        List<Knowledge> related = findRelatedKnowledge(topic, keywords);
        StringBuilder knowledgeContext = new StringBuilder();
        if (!related.isEmpty()) {
            int contentTruncate = writingProperties.getContentTruncateLength();
            knowledgeContext.append("以下是从知识库中检索到的相关参考内容：\n\n");
            for (Knowledge k : related) {
                knowledgeContext.append("[标题]: ").append(k.getTitle()).append("\n");
                if (k.getSummary() != null) {
                    knowledgeContext.append("[摘要]: ").append(k.getSummary()).append("\n");
                }
                knowledgeContext.append("[内容]: ").append(AiService.truncate(k.getContent(), contentTruncate)).append("\n\n");
            }
        }

        String defaultStyle = writingProperties.getStyle();
        String styleGuide = style != null && !style.isBlank()
                ? "写作风格: " + style : defaultStyle;
        String kwGuide = keywords != null && !keywords.isBlank()
                ? "关键词: " + keywords : "";

        double temperature = writingProperties.getTemperature();
        int maxTokens = writingProperties.getMaxTokens();
        String knowledgePart = !related.isEmpty() ? knowledgeContext.toString()
                : "（知识库中没有直接相关的参考内容，请基于你的知识进行创作）\n\n";
        String prompt = PromptRegistry.WRITING_ARTICLE.resolve(systemConfigService, styleGuide, kwGuide, topic, knowledgePart);

        String result = aiService.call(prompt, temperature, maxTokens, "WRITING");
        return result != null ? result : writingProperties.getFallbackMessage();
    }

    private List<Knowledge> findRelatedKnowledge(String topic, String keywords) {
        Set<String> searchTerms = new LinkedHashSet<>();
        int minTermLen = writingProperties.getMinTermLength();
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

        int maxResults = writingProperties.getMaxRelatedResults();
        return results.subList(0, Math.min(results.size(), maxResults));
    }
}
