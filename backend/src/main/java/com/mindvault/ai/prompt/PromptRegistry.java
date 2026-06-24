package com.mindvault.ai.prompt;

import com.mindvault.systemconfig.SystemConfigService;

import java.util.function.Function;

public enum PromptRegistry {

    AGENT_SYSTEM("prompt.agent.system-prompt",
            "你是 MindVault（知忆）AI 助手，一个个人知识库 Agent。\n\n你可以使用以下工具来帮助用户管理知识库：\n%s\n\n请用中文回复用户。"),

    AUTO_TITLE("prompt.auto.title-generation",
            "请根据以下内容生成一个简洁准确的中文标题（10-20字），只返回标题内容，不要额外说明。\n\n原始标题: %s\n\n内容: %s"),

    AUTO_TAGS("prompt.auto.tag-generation",
            "请为以下内容生成3-5个中文标签，以JSON数组格式返回，例如 [\"标签1\", \"标签2\", \"标签3\"]。\n只返回JSON数组，不要额外说明。\n\n标题: %s\n\n内容: %s"),

    AUTO_SUMMARY("prompt.auto.summary-generation",
            "请为以下内容生成一段简洁的中文摘要（50-100字），只返回摘要内容，不要额外说明。\n\n标题: %s\n\n内容: %s"),

    SEARCH_QUERY_REWRITE("prompt.search.query-rewrite",
            "你是一个搜索查询优化助手。请将用户的原始问题改写成更适合向量检索和关键词检索的形式。\n要求：\n1. 提取核心关键词和实体\n2. 补充同义词或相关术语\n3. 保持简洁，长度不超过50字\n4. 只返回改写后的查询文本，不要额外说明\n\n原始问题: %s"),

    SEARCH_HYDE("prompt.search.hyde-document",
            "你是一个知识库检索助手。用户提出了一个问题，请生成一段假设性的文档内容，\n这段内容应当包含回答该问题所需的关键信息。只返回文档内容本身，不要额外说明。\n\n用户问题: %s"),

    SEARCH_RERANK("prompt.search.rerank",
            "请评估以下搜索结果与用户查询的相关性。对每条结果给出 0-10 的分数（10 最相关）。\n\n用户查询: %s\n\n搜索结果:\n%s\n\n请只返回 JSON 数组格式的评分，例如 [9, 5, 7]，不要额外说明。"),

    WRITING_ARTICLE("prompt.writing.article",
            "你是一个基于个人知识库的写作助手。请根据参考内容撰写一篇文章。\n\n%s\n%s\n\n主题: %s\n\n%s\n请撰写一篇结构完整、内容详实的文章。使用中文。"),

    FLASHCARD_GENERATION("prompt.flashcard.generation",
            "你是一个知识卡片生成助手。请根据以下内容生成3-5个问答式知识卡片。" +
            "返回JSON数组格式，每个元素包含 question、answer、difficulty 字段。" +
            "difficulty 取值为 EASY / MEDIUM / HARD。" +
            "只返回JSON数组，不要额外说明。\n\n标题: %s\n\n内容: %s"),

    DAILY_REVIEW_REPORT("prompt.daily-review.report",
            "你是一个每日知识复盘助手。请根据以下今日新增知识，生成一份复盘报告。" +
            "返回JSON格式，包含以下字段：\n" +
            "1. summary: 一段概括性总结（50-100字）\n" +
            "2. keyInsights: 关键洞见数组（3-5条）\n" +
            "3. recommendations: 后续建议数组（2-3条）\n" +
            "4. categoryBreakdown: 知识分类统计对象\n\n" +
            "只返回JSON，不要额外说明。\n\n%s"),

    RELATION_LLM("prompt.relation.llm-analysis",
            "你是一个知识关联分析助手。新笔记内容如下：\n\n"
            + "标题: %s\n内容: %s\n\n以下是已有笔记列表（id + 标题 + 摘要）：\n"
            + "%s\n\n请分析新笔记与哪些已有笔记相关，返回JSON数组，每一项包含："
            + "{\"id\": 已有笔记ID, \"type\": \"COMPLEMENT|CONTRAST|EXTENSION|REFERENCE\", \"reason\": \"简短原因\"}。"
            + "如果都不相关则返回 []. 只返回JSON数组。");

    private final String configKey;
    private final String defaultTemplate;

    PromptRegistry(String configKey, String defaultTemplate) {
        this.configKey = configKey;
        this.defaultTemplate = defaultTemplate;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDefaultTemplate() {
        return defaultTemplate;
    }

    public String resolve(SystemConfigService config, Object... args) {
        String template = config.getPrompt(configKey, defaultTemplate);
        return String.format(template, args);
    }
}