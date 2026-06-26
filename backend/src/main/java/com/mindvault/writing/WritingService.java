package com.mindvault.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.ai.client.AiService;
import com.mindvault.ai.prompt.PromptRegistry;
import com.mindvault.knowledge.KnowledgeMapper;
import com.mindvault.knowledge.entity.Knowledge;
import com.mindvault.model.ModelConfigService;
import com.mindvault.systemconfig.SystemConfigService;
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
public class WritingService {

    private static final Logger log = LoggerFactory.getLogger(WritingService.class);

    private final ModelConfigService modelConfigService;
    private final AiService aiService;
    private final KnowledgeMapper knowledgeMapper;
    private final SystemConfigService config;
    private final ObjectMapper objectMapper;

    public WritingService(ModelConfigService modelConfigService,
                          AiService aiService,
                          KnowledgeMapper knowledgeMapper,
                          SystemConfigService config) {
        this.modelConfigService = modelConfigService;
        this.aiService = aiService;
        this.knowledgeMapper = knowledgeMapper;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 生成文章。
     * <p>
     * 流程: 检查主模型可用性 → 按主题和关键词检索知识库 → 拼接提示词上下文 → 调用 LLM → 返回结果。
     * 如果知识库无相关条目，LLM 基于自身知识进行创作。
     * </p>
     * @param topic    文章主题（必填）
     * @param style    写作风格（可选，为空则使用默认风格）
     * @param keywords 关键词（可选）
     * @return AI 生成的文章文本，失败时返回 fallback 提示
     */
    public String generateArticle(String topic, String style, String keywords) {
        try {
            modelConfigService.getPrimaryChatModel();
        } catch (Exception e) {
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
                knowledgeContext.append("[内容]: ").append(AiService.truncate(k.getContent(), contentTruncate)).append("\n\n");
            }
        }

        String defaultStyle = config.getString("default.writing.style", "写作风格: 清晰、条理、专业");
        String styleGuide = style != null && !style.isBlank()
                ? "写作风格: " + style : defaultStyle;
        String kwGuide = keywords != null && !keywords.isBlank()
                ? "关键词: " + keywords : "";

        double temperature = config.getDouble("threshold.writing.temperature", 0.7);
        int maxTokens = config.getInt("threshold.writing.max-tokens", 4096);
        String knowledgePart = !related.isEmpty() ? knowledgeContext.toString()
                : "（知识库中没有直接相关的参考内容，请基于你的知识进行创作）\n\n";
        String prompt = PromptRegistry.WRITING_ARTICLE.resolve(config, styleGuide, kwGuide, topic, knowledgePart);

        String result = aiService.call(prompt, temperature, maxTokens, "WRITING");
        return result != null ? result : config.getString("default.writing.fallback-message", "文章生成失败，请稍后重试。");
    }

    /**
     * 根据主题和关键词从知识库中检索相关条目。
     * 先拆分关键词为词条集合，逐词模糊匹配标题和内容，结果去重后按配置上限截取。
     * @param topic    文章主题
     * @param keywords 附加关键词
     * @return 相关知识条目列表，上限由 threshold.writing.max-related-results 控制
     */
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