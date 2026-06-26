package com.mindvault.systemconfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置默认值及元数据。
 * <p>
 * 集中管理所有 system_config 配置项的默认值、按前缀的模块/分组映射关系、
 * 值校验规则和定时任务元数据。
 * </p>
 * <p>
 * 关键设计:
 * <ul>
 *   <li>DEFAULTS: 所有配置项的默认值，通过 configKey → defaultValue 映射</li>
 *   <li>VALIDATION_RULES: 按配置键的校验规则（值类型、最小值/最大值、可选值列表），根据 key 的名字模式自动推导</li>
 *   <li>KEY_TO_MODULE: 配置键前缀 → 模块 ID 的映射，用于前端按模块分组展示</li>
 *   <li>KEY_TO_GROUP: 配置键前缀 → 分组（prompt/threshold/default/task/other）映射</li>
 *   <li>SCHEDULED_TASKS: 定时任务列表，包含任务 ID、标签、开关 key、调度 key 和调度类型</li>
 * </ul>
 * </p>
 */
public final class SystemConfigDefaults {

    /**
     * 配置值校验规则。
     * @param valueType 值类型: string/int/bool/cron/prompt/double
     * @param min       最小值（数值类型时有效）
     * @param max       最大值（数值类型时有效）
     * @param options   可选值列表（枚举类型时有效）
     */
    public record ValidationRule(String valueType, Double min, Double max, List<String> options) {}

    /**
     * 定时任务元数据。
     * @param id            任务 ID
     * @param label          任务显示名称
     * @param enabledKey    启用开关的配置键
     * @param scheduleKey   调度配置的键（cron 表达式或 poll-ms）
     * @param scheduleType  调度类型: cron 或 poll-ms
     */
    public record TaskMeta(String id, String label, String enabledKey, String scheduleKey, String scheduleType) {}

    private SystemConfigDefaults() {}

    public static final Map<String, String> MODULE_LABELS = new LinkedHashMap<>();
    public static final Map<String, String> KEY_TO_MODULE = new LinkedHashMap<>();
    public static final Map<String, String> KEY_TO_GROUP = new LinkedHashMap<>();
    public static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    public static final Map<String, ValidationRule> VALIDATION_RULES = new LinkedHashMap<>();
    public static final List<TaskMeta> SCHEDULED_TASKS = List.of(
            new TaskMeta("auto-process-round2", "R2 关联发现", "task.auto-process.round2.enabled", "task.auto-process.round2.poll-ms", "poll-ms"),
            new TaskMeta("auto-process-round3", "R3 聚合分析", "task.auto-process.round3.enabled", "task.auto-process.round3.poll-ms", "poll-ms"),
            new TaskMeta("backup", "自动备份", "task.backup.enabled", "task.backup.cron", "cron"),
            new TaskMeta("daily-review", "每日复盘", "task.daily-review.enabled", "task.daily-review.cron", "cron"),
            new TaskMeta("token-usage", "Token 用量统计", "task.token-usage.enabled", "task.token-usage.cron", "cron"),
            new TaskMeta("association", "知识关联发现（旧版）", "task.association.enabled", "task.association.cron", "cron"),
            new TaskMeta("vector-consistency", "向量一致性检查", "task.vector-consistency.enabled", "task.vector-consistency.cron", "cron")
    );

    static {
        initModuleLabels();
        initKeyToModule();
        initKeyToGroup();
        initDefaults();
        initValidationRules();
    }

    private static void initModuleLabels() {
        MODULE_LABELS.put("knowledge", "知识库");
        MODULE_LABELS.put("chat", "AI 对话");
        MODULE_LABELS.put("review", "间隔复习");
        MODULE_LABELS.put("flashcard", "闪卡");
        MODULE_LABELS.put("daily-review", "每日复盘");
        MODULE_LABELS.put("writing", "写作助手");
        MODULE_LABELS.put("auto-process", "自动处理");
        MODULE_LABELS.put("relation", "关联发现");
        MODULE_LABELS.put("agent", "Agent");
        MODULE_LABELS.put("system", "系统");
        MODULE_LABELS.put("scheduler", "定时任务");
    }

    private static void initKeyToModule() {
        KEY_TO_MODULE.put("prompt.auto.", "auto-process");
        KEY_TO_MODULE.put("threshold.auto.", "auto-process");
        KEY_TO_MODULE.put("task.auto-process.", "scheduler");
        KEY_TO_MODULE.put("prompt.relation.", "relation");
        KEY_TO_MODULE.put("threshold.relation.", "relation");
        KEY_TO_MODULE.put("threshold.association.", "relation");
        KEY_TO_MODULE.put("task.association.", "scheduler");
        KEY_TO_MODULE.put("prompt.daily-review.", "daily-review");
        KEY_TO_MODULE.put("threshold.daily-review.", "daily-review");
        KEY_TO_MODULE.put("default.daily-review.", "daily-review");
        KEY_TO_MODULE.put("task.daily-review.", "scheduler");
        KEY_TO_MODULE.put("prompt.flashcard.", "flashcard");
        KEY_TO_MODULE.put("threshold.flashcard.", "flashcard");
        KEY_TO_MODULE.put("prompt.writing.", "writing");
        KEY_TO_MODULE.put("threshold.writing.", "writing");
        KEY_TO_MODULE.put("default.writing.", "writing");
        KEY_TO_MODULE.put("prompt.agent.", "agent");
        KEY_TO_MODULE.put("threshold.agent.", "agent");
        KEY_TO_MODULE.put("default.agent.", "agent");
        KEY_TO_MODULE.put("tool.", "agent");
        KEY_TO_MODULE.put("prompt.search.", "knowledge");
        KEY_TO_MODULE.put("threshold.search.", "knowledge");
        KEY_TO_MODULE.put("threshold.chat.", "chat");
        KEY_TO_MODULE.put("default.chat.", "chat");
        KEY_TO_MODULE.put("chat.keyword.", "chat");
        KEY_TO_MODULE.put("threshold.review.", "review");
        KEY_TO_MODULE.put("threshold.export.", "knowledge");
        KEY_TO_MODULE.put("default.export.", "knowledge");
        KEY_TO_MODULE.put("default.knowledge.", "knowledge");
        KEY_TO_MODULE.put("default.content.", "knowledge");
        KEY_TO_MODULE.put("threshold.circuitbreaker.", "system");
        KEY_TO_MODULE.put("threshold.tokenusage.", "system");
        KEY_TO_MODULE.put("threshold.session.", "system");
        KEY_TO_MODULE.put("threshold.logging.", "system");
        KEY_TO_MODULE.put("threshold.operationlog.", "system");
        KEY_TO_MODULE.put("threshold.ratelimit.", "system");
        KEY_TO_MODULE.put("threshold.system-config.", "system");
        KEY_TO_MODULE.put("default.backup.", "system");
        KEY_TO_MODULE.put("default.user.", "system");
        KEY_TO_MODULE.put("default.system.", "system");
        KEY_TO_MODULE.put("default.cors.", "system");
        KEY_TO_MODULE.put("default.llm.", "system");
        KEY_TO_MODULE.put("task.backup.", "scheduler");
        KEY_TO_MODULE.put("task.daily-review.", "scheduler");
        KEY_TO_MODULE.put("task.token-usage.", "scheduler");
        KEY_TO_MODULE.put("task.vector-consistency.", "scheduler");
    }

    private static void initKeyToGroup() {
        KEY_TO_GROUP.put("prompt.", "prompt");
        KEY_TO_GROUP.put("threshold.", "threshold");
        KEY_TO_GROUP.put("default.", "default");
        KEY_TO_GROUP.put("chat.keyword.", "other");
        KEY_TO_GROUP.put("tool.", "other");
        KEY_TO_GROUP.put("task.", "task");
    }

    private static void initDefaults() {
        DEFAULTS.put("prompt.auto.title-generation",
                "请根据以下内容生成一个简洁准确的中文标题（10-20字），只返回标题内容，不要额外说明。\n\n原始标题: %s\n\n内容: %s");
        DEFAULTS.put("prompt.auto.summary-generation",
                "请为以下内容生成一段简洁的中文摘要（50-100字），只返回摘要内容，不要额外说明。\n\n标题: %s\n\n内容: %s");
        DEFAULTS.put("prompt.auto.tag-generation",
                "请为以下内容生成3-5个中文标签，以JSON数组格式返回，例如 [\"标签1\", \"标签2\", \"标签3\"]。\n只返回JSON数组，不要额外说明。\n\n标题: %s\n\n内容: %s");
        DEFAULTS.put("prompt.relation.llm-analysis",
                "你是一个知识关联分析助手。新笔记内容如下：\n\n标题: %s\n内容: %s\n\n以下是已有笔记列表（id + 标题 + 摘要）：\n%s\n\n请分析新笔记与哪些已有笔记相关，返回JSON数组，每一项包含：{\"id\": 已有笔记ID, \"type\": \"COMPLEMENT|CONTRAST|EXTENSION|REFERENCE\", \"reason\": \"简短原因\"}。如果都不相关则返回 []. 只返回JSON数组。");
        DEFAULTS.put("prompt.daily-review.report",
                "你是一个每日知识复盘助手。请根据以下今日新增知识，生成一份复盘报告。返回JSON格式，包含以下字段：\n1. summary: 一段概括性总结（50-100字）\n2. keyInsights: 关键洞见数组（3-5条）\n3. recommendations: 后续建议数组（2-3条）\n4. categoryBreakdown: 知识分类统计对象\n\n只返回JSON，不要额外说明。\n\n%s");
        DEFAULTS.put("prompt.flashcard.generation",
                "你是一个知识卡片生成助手。请根据以下内容生成3-5个问答式知识卡片。返回JSON数组格式，每个元素包含 question、answer、difficulty 字段。difficulty 取值为 EASY / MEDIUM / HARD。只返回JSON数组，不要额外说明。\n\n标题: %s\n\n内容: %s");
        DEFAULTS.put("prompt.writing.article",
                "你是一个基于个人知识库的写作助手。请根据参考内容撰写一篇文章。\n\n%s\n%s\n\n主题: %s\n\n%s\n请撰写一篇结构完整、内容详实的文章。使用中文。");
        DEFAULTS.put("prompt.agent.system-prompt",
                "你是 MindVault（知忆）AI 助手，一个个人知识库 Agent。\n\n你可以使用以下工具来帮助用户管理知识库：\n%s\n\n当你需要使用工具时，请按以下格式返回：\n[TOOL_CALL]\nname: 工具名称\nargs: {\"key\": \"value\"}\n[END_TOOL_CALL]\n\n请用中文回复用户。");
        DEFAULTS.put("prompt.search.query-rewrite",
                "你是一个搜索查询优化助手。请将用户的原始问题改写成更适合向量检索和关键词检索的形式。\n要求：\n1. 提取核心关键词和实体\n2. 补充同义词或相关术语\n3. 保持简洁，长度不超过50字\n4. 只返回改写后的查询文本，不要额外说明\n\n原始问题: %s");
        DEFAULTS.put("prompt.search.hyde-document",
                "你是一个知识库检索助手。用户提出了一个问题，请生成一段假设性的文档内容，这段内容应当包含回答该问题所需的关键信息。只返回文档内容本身，不要额外说明。\n\n用户问题: %s");
        DEFAULTS.put("prompt.search.rerank",
                "请评估以下搜索结果与用户查询的相关性。对每条结果给出 0-10 的分数（10 最相关）。\n\n用户查询: %s\n\n搜索结果:\n%s\n\n请只返回 JSON 数组格式的评分，例如 [9, 5, 7]，不要额外说明。");
        DEFAULTS.put("task.auto-process.round2.enabled", "true");
        DEFAULTS.put("task.auto-process.round2.poll-ms", "300000");
        DEFAULTS.put("task.auto-process.round3.enabled", "true");
        DEFAULTS.put("task.auto-process.round3.poll-ms", "1800000");
        DEFAULTS.put("task.backup.enabled", "true");
        DEFAULTS.put("task.backup.cron", "0 0 3 * * ?");
        DEFAULTS.put("task.daily-review.enabled", "true");
        DEFAULTS.put("task.daily-review.cron", "0 30 2 * * ?");
        DEFAULTS.put("task.token-usage.enabled", "true");
        DEFAULTS.put("task.token-usage.cron", "0 30 3 * * ?");
        DEFAULTS.put("task.association.enabled", "true");
        DEFAULTS.put("task.association.cron", "0 0 2 * * ?");
        DEFAULTS.put("task.vector-consistency.enabled", "true");
        DEFAULTS.put("task.vector-consistency.cron", "0 45 3 * * ?");
        DEFAULTS.put("threshold.auto.truncate-length", "2000");
        DEFAULTS.put("threshold.auto.title-max-tokens", "100");
        DEFAULTS.put("threshold.auto.summary-max-tokens", "300");
        DEFAULTS.put("threshold.auto.tags-max-tokens", "300");
        DEFAULTS.put("threshold.auto.embedding-truncate-length", "8000");
        DEFAULTS.put("threshold.auto.llm-temperature", "0.3");
        DEFAULTS.put("threshold.relation.batch-size", "20");
        DEFAULTS.put("threshold.relation.candidate-limit", "50");
        DEFAULTS.put("threshold.relation.vector-top-n", "10");
        DEFAULTS.put("threshold.relation.similarity-min", "0.5");
        DEFAULTS.put("threshold.relation.score-per-tag", "0.25");
        DEFAULTS.put("threshold.relation.tag-score-max", "1.0");
        DEFAULTS.put("threshold.relation.llm-candidate-limit", "10");
        DEFAULTS.put("threshold.relation.llm-max-tokens", "500");
        DEFAULTS.put("threshold.relation.llm-temperature", "0.3");
        DEFAULTS.put("threshold.relation.content-truncate-length", "1500");
        DEFAULTS.put("threshold.relation.llm-default-score", "0.75");
        DEFAULTS.put("threshold.aggregation.batch-size", "50");
        DEFAULTS.put("threshold.aggregation.tag-cloud-top-n", "50");
        DEFAULTS.put("threshold.search.fetch-limit-multiplier", "3");
        DEFAULTS.put("threshold.search.min-fetch-limit", "20");
        DEFAULTS.put("threshold.search.rrf-k", "60.0");
        DEFAULTS.put("threshold.search.embedding-truncate-length", "8000");
        DEFAULTS.put("threshold.search.rewrite-max-tokens", "100");
        DEFAULTS.put("threshold.search.rewrite-temperature", "0.2");
        DEFAULTS.put("threshold.search.hyde-max-tokens", "500");
        DEFAULTS.put("threshold.search.hyde-temperature", "0.3");
        DEFAULTS.put("threshold.search.hyde-embedding-truncate", "8000");
        DEFAULTS.put("threshold.search.rerank-truncate-length", "300");
        DEFAULTS.put("threshold.search.rerank-max-tokens", "200");
        DEFAULTS.put("threshold.search.rerank-temperature", "0.1");
        DEFAULTS.put("threshold.export.max-filename-length", "80");
        DEFAULTS.put("threshold.flashcard.truncate-length", "3000");
        DEFAULTS.put("threshold.flashcard.max-tokens", "1000");
        DEFAULTS.put("threshold.flashcard.temperature", "0.3");
        DEFAULTS.put("threshold.writing.content-truncate-length", "500");
        DEFAULTS.put("threshold.writing.max-tokens", "4096");
        DEFAULTS.put("threshold.writing.temperature", "0.7");
        DEFAULTS.put("threshold.writing.max-related-results", "5");
        DEFAULTS.put("threshold.writing.min-term-length", "1");
        DEFAULTS.put("threshold.agent.token-estimate-ratio", "3.0");
        DEFAULTS.put("threshold.agent.stream-read-timeout-ms", "120000");
        DEFAULTS.put("threshold.agent.default-temperature", "0.7");
        DEFAULTS.put("threshold.agent.max-retries", "2");
        DEFAULTS.put("threshold.agent.retry-delay-ms", "2000");
        DEFAULTS.put("threshold.agent.search-default-topn", "5");
        DEFAULTS.put("threshold.agent.search-result-truncate", "200");
        DEFAULTS.put("threshold.circuitbreaker.failure-threshold", "3");
        DEFAULTS.put("threshold.circuitbreaker.cooldown-seconds", "60");
        DEFAULTS.put("threshold.circuitbreaker.half-open-max", "2");
        DEFAULTS.put("threshold.chat.log-truncate-length", "50");
        DEFAULTS.put("threshold.chat.sse-timeout-ms", "300000");
        DEFAULTS.put("threshold.chat.title-truncate-length", "30");
        DEFAULTS.put("threshold.daily-review.max-tokens", "1500");
        DEFAULTS.put("threshold.daily-review.temperature", "0.3");
        DEFAULTS.put("threshold.review.initial-interval-days", "1");
        DEFAULTS.put("threshold.review.quality-min", "0");
        DEFAULTS.put("threshold.review.quality-max", "5");
        DEFAULTS.put("threshold.review.failed-interval-days", "1");
        DEFAULTS.put("threshold.review.ease-factor-penalty", "0.20");
        DEFAULTS.put("threshold.review.first-success-interval", "1");
        DEFAULTS.put("threshold.review.second-success-interval", "6");
        DEFAULTS.put("threshold.review.ease-factor-adjustment", "0.10");
        DEFAULTS.put("threshold.review.min-ease-factor", "1.30");
        DEFAULTS.put("threshold.tokenusage.calc-divisor", "1000.0");
        DEFAULTS.put("threshold.session.cleaner-interval-ms", "60000");
        DEFAULTS.put("threshold.logging.trace-id-length", "16");
        DEFAULTS.put("threshold.logging.error-body-truncate", "2000");
        DEFAULTS.put("threshold.logging.slow-request-ms", "2000");
        DEFAULTS.put("threshold.operationlog.args-truncate-length", "200");
        DEFAULTS.put("threshold.ratelimit.window-minutes", "1");
        DEFAULTS.put("threshold.system-config.cache-refresh-ms", "30000");
        DEFAULTS.put("threshold.association.top-n", "6");
        DEFAULTS.put("default.export.version", "0.4.0");
        DEFAULTS.put("default.export.csv-header", "标题,内容,类型,摘要,标签,来源,创建时间");
        DEFAULTS.put("default.export.markdown-untagged-folder", "未分类");
        DEFAULTS.put("default.knowledge.auto-process-status", "PENDING");
        DEFAULTS.put("default.knowledge.tags-empty-json", "[]");
        DEFAULTS.put("default.knowledge.import-default-title", "未命名");
        DEFAULTS.put("default.knowledge.content-type", "TEXT");
        DEFAULTS.put("default.content.pdf-default-title", "未命名文档");
        DEFAULTS.put("default.content.jsoup-user-agent", "Mozilla/5.0 (compatible; MindVault/1.0)");
        DEFAULTS.put("default.chat.session-title", "新对话");
        DEFAULTS.put("default.daily-review.empty-summary", "当日无新增知识。");
        DEFAULTS.put("default.daily-review.fallback-summary", "当日知识较多，自动摘要生成失败。");
        DEFAULTS.put("default.writing.style", "写作风格: 清晰、条理、专业");
        DEFAULTS.put("default.writing.fallback-message", "文章生成失败，请稍后重试。");
        DEFAULTS.put("default.writing.no-model-message", "系统未配置可用模型，请先在设置中添加并启用模型。");
        DEFAULTS.put("default.agent.no-model-message", "系统未配置主模型，请先在设置中添加并设置主模型。");
        DEFAULTS.put("default.agent.error-message", "抱歉，处理您的消息时遇到了问题，请稍后重试。");
        DEFAULTS.put("default.agent.tool-result-prefix", "工具执行结果: ");
        DEFAULTS.put("default.backup.filename-date-format", "yyyyMMdd-HHmmss");
        DEFAULTS.put("default.backup.filename-prefix", "mindvault-backup-");
        DEFAULTS.put("default.backup.file-pattern", "mindvault-backup-");
        DEFAULTS.put("default.user.role", "USER");
        DEFAULTS.put("default.system.app-version", "1.0.0");
        DEFAULTS.put("default.llm.source-name", "CHAT");
        DEFAULTS.put("default.cors.allowed-origin-pattern", "*");
        DEFAULTS.put("chat.keyword.blocklist", "");
        DEFAULTS.put("chat.keyword.block-message", "消息包含受限内容，已拦截");
        DEFAULTS.put("chat.keyword.case-sensitive", "false");
        DEFAULTS.put("tool.search-knowledge.default-limit", "3");
        DEFAULTS.put("tool.search-knowledge.max-limit", "10");
        DEFAULTS.put("tool.search-knowledge.search-method", "rewrite");
    }

    private static void initValidationRules() {
        for (var entry : DEFAULTS.entrySet()) {
            String key = entry.getKey();
            String defaultValue = entry.getValue();
            if (key.contains("temperature")) {
                VALIDATION_RULES.put(key, new ValidationRule("double", 0.0, 2.0, null));
            } else if (key.contains("max-tokens") || key.contains("max-token")) {
                VALIDATION_RULES.put(key, new ValidationRule("int", 1.0, 128000.0, null));
            } else if (key.contains("truncate-length") || key.contains("truncate")) {
                VALIDATION_RULES.put(key, new ValidationRule("int", 1.0, 50000.0, null));
            } else if (key.contains(".enabled")) {
                VALIDATION_RULES.put(key, new ValidationRule("bool", null, null, List.of("true", "false")));
            } else if (key.startsWith("prompt.")) {
                VALIDATION_RULES.put(key, new ValidationRule("prompt", null, null, null));
            } else if (key.startsWith("default.")) {
                VALIDATION_RULES.put(key, new ValidationRule("string", null, null, null));
            } else if (key.startsWith("chat.keyword.")) {
                VALIDATION_RULES.put(key, new ValidationRule("string", null, null, null));
            } else if (key.startsWith("tool.")) {
                VALIDATION_RULES.put(key, new ValidationRule("int", 1.0, 100.0, null));
            }
        }

        VALIDATION_RULES.put("tool.search-knowledge.search-method",
                new ValidationRule("string", null, null, List.of("rewrite", "hyde", "hybrid")));
        VALIDATION_RULES.put("chat.keyword.case-sensitive",
                new ValidationRule("bool", null, null, List.of("true", "false")));
        VALIDATION_RULES.put("threshold.review.quality-min",
                new ValidationRule("int", 0.0, 5.0, null));
        VALIDATION_RULES.put("threshold.review.quality-max",
                new ValidationRule("int", 0.0, 5.0, null));
        VALIDATION_RULES.put("threshold.relation.similarity-min",
                new ValidationRule("double", 0.0, 1.0, null));
        VALIDATION_RULES.put("threshold.relation.tag-score-max",
                new ValidationRule("double", 0.0, 1.0, null));
        VALIDATION_RULES.put("threshold.relation.score-per-tag",
                new ValidationRule("double", 0.0, 1.0, null));
        VALIDATION_RULES.put("threshold.relation.llm-default-score",
                new ValidationRule("double", 0.0, 1.0, null));
        VALIDATION_RULES.put("threshold.agent.token-estimate-ratio",
                new ValidationRule("double", 0.1, 10.0, null));
        VALIDATION_RULES.put("threshold.agent.max-retries",
                new ValidationRule("int", 0.0, 10.0, null));
        VALIDATION_RULES.put("threshold.agent.retry-delay-ms",
                new ValidationRule("int", 0.0, 60000.0, null));
        VALIDATION_RULES.put("threshold.agent.stream-read-timeout-ms",
                new ValidationRule("int", 1000.0, 600000.0, null));
        VALIDATION_RULES.put("threshold.circuitbreaker.failure-threshold",
                new ValidationRule("int", 1.0, 20.0, null));
        VALIDATION_RULES.put("threshold.circuitbreaker.cooldown-seconds",
                new ValidationRule("int", 1.0, 600.0, null));
        VALIDATION_RULES.put("threshold.circuitbreaker.half-open-max",
                new ValidationRule("int", 1.0, 10.0, null));
        VALIDATION_RULES.put("threshold.ratelimit.window-minutes",
                new ValidationRule("int", 1.0, 60.0, null));
        VALIDATION_RULES.put("threshold.system-config.cache-refresh-ms",
                new ValidationRule("int", 1000.0, 600000.0, null));
        VALIDATION_RULES.put("threshold.session.cleaner-interval-ms",
                new ValidationRule("int", 1000.0, 3600000.0, null));
        VALIDATION_RULES.put("threshold.review.ease-factor-penalty",
                new ValidationRule("double", 0.0, 1.0, null));
        VALIDATION_RULES.put("threshold.review.ease-factor-adjustment",
                new ValidationRule("double", 0.0, 1.0, null));
        VALIDATION_RULES.put("threshold.review.min-ease-factor",
                new ValidationRule("double", 1.0, 3.0, null));
        VALIDATION_RULES.put("threshold.review.initial-interval-days",
                new ValidationRule("int", 0.0, 365.0, null));
        VALIDATION_RULES.put("threshold.review.failed-interval-days",
                new ValidationRule("int", 0.0, 365.0, null));
        VALIDATION_RULES.put("threshold.review.first-success-interval",
                new ValidationRule("int", 0.0, 365.0, null));
        VALIDATION_RULES.put("threshold.review.second-success-interval",
                new ValidationRule("int", 0.0, 365.0, null));
        VALIDATION_RULES.put("threshold.search.rrf-k",
                new ValidationRule("double", 1.0, 100.0, null));
        VALIDATION_RULES.put("threshold.search.fetch-limit-multiplier",
                new ValidationRule("int", 1.0, 10.0, null));
        VALIDATION_RULES.put("threshold.search.min-fetch-limit",
                new ValidationRule("int", 1.0, 200.0, null));
        VALIDATION_RULES.put("threshold.export.max-filename-length",
                new ValidationRule("int", 10.0, 200.0, null));
        VALIDATION_RULES.put("threshold.agent.search-default-topn",
                new ValidationRule("int", 1.0, 50.0, null));
        VALIDATION_RULES.put("threshold.agent.search-result-truncate",
                new ValidationRule("int", 50.0, 5000.0, null));
        VALIDATION_RULES.put("threshold.writing.max-related-results",
                new ValidationRule("int", 1.0, 50.0, null));
        VALIDATION_RULES.put("threshold.writing.min-term-length",
                new ValidationRule("int", 1.0, 10.0, null));
        VALIDATION_RULES.put("threshold.chat.sse-timeout-ms",
                new ValidationRule("int", 1000.0, 600000.0, null));
        VALIDATION_RULES.put("threshold.chat.log-truncate-length",
                new ValidationRule("int", 10.0, 1000.0, null));
        VALIDATION_RULES.put("threshold.chat.title-truncate-length",
                new ValidationRule("int", 5.0, 100.0, null));
        VALIDATION_RULES.put("threshold.daily-review.max-tokens",
                new ValidationRule("int", 100.0, 32000.0, null));
        VALIDATION_RULES.put("threshold.daily-review.temperature",
                new ValidationRule("double", 0.0, 2.0, null));
        VALIDATION_RULES.put("threshold.flashcard.max-tokens",
                new ValidationRule("int", 100.0, 32000.0, null));
        VALIDATION_RULES.put("threshold.flashcard.temperature",
                new ValidationRule("double", 0.0, 2.0, null));
        VALIDATION_RULES.put("threshold.flashcard.truncate-length",
                new ValidationRule("int", 100.0, 10000.0, null));
        VALIDATION_RULES.put("threshold.writing.max-tokens",
                new ValidationRule("int", 100.0, 32000.0, null));
        VALIDATION_RULES.put("threshold.writing.temperature",
                new ValidationRule("double", 0.0, 2.0, null));
        VALIDATION_RULES.put("threshold.writing.content-truncate-length",
                new ValidationRule("int", 100.0, 5000.0, null));
        VALIDATION_RULES.put("threshold.tokenusage.calc-divisor",
                new ValidationRule("double", 1.0, 1000000.0, null));
        VALIDATION_RULES.put("tool.search-knowledge.default-limit",
                new ValidationRule("int", 1.0, 50.0, null));
        VALIDATION_RULES.put("tool.search-knowledge.max-limit",
                new ValidationRule("int", 1.0, 100.0, null));
    }

    /**
     * 根据配置键前缀推导所属模块。
     * @param key 配置键
     * @return 模块 ID，无法匹配时返回 "system"
     */
    public static String deriveModule(String key) {
        for (var entry : KEY_TO_MODULE.entrySet()) {
            if (key.startsWith(entry.getKey())) return entry.getValue();
        }
        return "system";
    }

    /**
     * 根据配置键前缀推导所属分组。
     * @param key 配置键
     * @return 分组 ID: prompt/threshold/default/task/other
     */
    public static String deriveGroup(String key) {
        for (var entry : KEY_TO_GROUP.entrySet()) {
            if (key.startsWith(entry.getKey())) return entry.getValue();
        }
        return "other";
    }
}