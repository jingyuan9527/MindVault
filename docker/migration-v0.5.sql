-- system_config 配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value TEXT NOT NULL DEFAULT '',
    description VARCHAR(500) DEFAULT '',
    value_type VARCHAR(50) NOT NULL DEFAULT 'string',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 1. PROMPT 模板（value_type = prompt）
-- ============================================================
INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.auto.title-generation',
'请根据以下内容生成一个简洁准确的中文标题（10-20字），只返回标题内容，不要额外说明。\n\n原始标题: %s\n\n内容: %s',
'自动处理：AI 标题生成 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.auto.summary-generation',
'请为以下内容生成一段简洁的中文摘要（50-100字），只返回摘要内容，不要额外说明。\n\n标题: %s\n\n内容: %s',
'自动处理：摘要生成 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.auto.tag-generation',
'请为以下内容生成3-5个中文标签，以JSON数组格式返回，例如 ["标签1", "标签2", "标签3"]。\n只返回JSON数组，不要额外说明。\n\n标题: %s\n\n内容: %s',
'自动处理：标签生成 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.relation.llm-analysis',
'你是一个知识关联分析助手。新笔记内容如下：\n\n标题: %s\n内容: %s\n\n以下是已有笔记列表（id + 标题 + 摘要）：\n%s\n\n请分析新笔记与哪些已有笔记相关，返回JSON数组，每一项包含：{"id": 已有笔记ID, "type": "COMPLEMENT|CONTRAST|EXTENSION|REFERENCE", "reason": "简短原因"}。如果都不相关则返回 []. 只返回JSON数组。',
'关联发现：LLM 分析 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.daily-review.report',
'你是一个每日知识复盘助手。请根据以下今日新增知识，生成一份复盘报告。返回JSON格式，包含以下字段：\n1. summary: 一段概括性总结（50-100字）\n2. keyInsights: 关键洞见数组（3-5条）\n3. recommendations: 后续建议数组（2-3条）\n4. categoryBreakdown: 知识分类统计对象\n\n只返回JSON，不要额外说明。\n\n%s',
'每日复盘：报告生成 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.flashcard.generation',
'你是一个知识卡片生成助手。请根据以下内容生成3-5个问答式知识卡片。返回JSON数组格式，每个元素包含 question、answer、difficulty 字段。difficulty 取值为 EASY / MEDIUM / HARD。只返回JSON数组，不要额外说明。\n\n标题: %s\n\n内容: %s',
'闪卡：卡片生成 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.writing.article',
'你是一个基于个人知识库的写作助手。请根据参考内容撰写一篇文章。\n\n%s\n%s\n\n主题: %s\n\n%s\n请撰写一篇结构完整、内容详实的文章。使用中文。',
'写作助手：文章生成 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.agent.system-prompt',
'你是 MindVault（知忆）AI 助手，一个个人知识库 Agent。\n\n你可以使用以下工具来帮助用户管理知识库：\n%s\n\n当你需要使用工具时，请按以下格式返回：\n[TOOL_CALL]\nname: 工具名称\nargs: {"key": "value"}\n[END_TOOL_CALL]\n\n请用中文回复用户。',
'Agent：系统 prompt（%s 会被工具列表替换）', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.search.query-rewrite',
'你是一个搜索查询优化助手。请将用户的原始问题改写成更适合向量检索和关键词检索的形式。\n要求：\n1. 提取核心关键词和实体\n2. 补充同义词或相关术语\n3. 保持简洁，长度不超过50字\n4. 只返回改写后的查询文本，不要额外说明\n\n原始问题: %s',
'搜索增强：查询改写 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.search.hyde-document',
'你是一个知识库检索助手。用户提出了一个问题，请生成一段假设性的文档内容，这段内容应当包含回答该问题所需的关键信息。只返回文档内容本身，不要额外说明。\n\n用户问题: %s',
'搜索增强：HyDE 文档生成 prompt', 'prompt');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('prompt.search.rerank',
'请评估以下搜索结果与用户查询的相关性。对每条结果给出 0-10 的分数（10 最相关）。\n\n用户查询: %s\n\n搜索结果:\n%s\n\n请只返回 JSON 数组格式的评分，例如 [9, 5, 7]，不要额外说明。',
'搜索增强：重排序 prompt', 'prompt');


-- ============================================================
-- 2. CRON 表达式 + 定时任务开关（value_type = cron / bool）
-- ============================================================
INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.auto-process.round2.enabled', 'true', 'R2 关联发现是否启用', 'bool');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.auto-process.round2.poll-ms', '300000', 'R2 轮询间隔（毫秒）', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.auto-process.round3.enabled', 'true', 'R3 聚合分析是否启用', 'bool');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.auto-process.round3.poll-ms', '1800000', 'R3 轮询间隔（毫秒）', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.backup.enabled', 'true', '自动备份是否启用', 'bool');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.backup.cron', '0 0 3 * * ?', '自动备份 cron 表达式', 'cron');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.daily-review.enabled', 'true', '每日复盘是否启用', 'bool');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.daily-review.cron', '0 30 2 * * ?', '每日复盘 cron 表达式', 'cron');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.token-usage.enabled', 'true', 'Token 用量统计是否启用', 'bool');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.token-usage.cron', '0 30 3 * * ?', 'Token 用量结算 cron 表达式', 'cron');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.association.enabled', 'true', '知识关联发现（旧版）是否启用', 'bool');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.association.cron', '0 0 2 * * ?', '知识关联发现 cron 表达式', 'cron');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.vector-consistency.enabled', 'true', '向量一致性检查是否启用', 'bool');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('task.vector-consistency.cron', '0 45 3 * * ?', '向量一致性检查 cron 表达式', 'cron');


-- ============================================================
-- 3. 业务阈值（value_type = int / double）
-- ============================================================
INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.auto.truncate-length', '2000', '自动处理 prompt 输入截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.auto.title-max-tokens', '100', 'AI 标题生成 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.auto.summary-max-tokens', '300', '摘要生成 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.auto.tags-max-tokens', '300', '标签生成 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.auto.embedding-truncate-length', '8000', '嵌入向量文本截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.auto.llm-temperature', '0.3', '自动处理 LLM temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.batch-size', '20', 'R2 每批处理数量', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.candidate-limit', '50', 'R2 待关联候选知识上限', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.vector-top-n', '10', '向量搜索 top-N', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.similarity-min', '0.5', '向量相似度最低阈值', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.score-per-tag', '0.25', '标签重叠单标签得分', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.tag-score-max', '1.0', '标签得分上限', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.llm-candidate-limit', '10', 'LLM 分析候选上限', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.llm-max-tokens', '500', 'LLM 关联分析 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.llm-temperature', '0.3', 'LLM 关联分析 temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.content-truncate-length', '1500', '关联分析内容截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.relation.llm-default-score', '0.75', 'LLM 发现关联默认得分', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.aggregation.batch-size', '50', 'R3 每批处理数量', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.aggregation.tag-cloud-top-n', '50', '标签云 top-N', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.fetch-limit-multiplier', '3', '混合搜索 fetch limit 倍数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.min-fetch-limit', '20', '混合搜索最小 fetch limit', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.rrf-k', '60.0', 'RRF 常数 k', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.embedding-truncate-length', '8000', '搜索嵌入截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.export.max-filename-length', '80', 'Markdown 导出文件名最大长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.flashcard.truncate-length', '3000', '闪卡内容截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.flashcard.max-tokens', '1000', '闪卡生成 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.flashcard.temperature', '0.3', '闪卡生成 temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.writing.content-truncate-length', '500', '写作相关内容截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.writing.max-tokens', '4096', '文章生成 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.writing.temperature', '0.7', '文章生成 temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.writing.max-related-results', '5', '写作检索相关知识上限', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.writing.min-term-length', '1', '写作搜索词最小长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.agent.token-estimate-ratio', '3.0', 'Agent Token 估算比例（chars/token）', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.agent.stream-read-timeout-ms', '120000', 'Agent 流式读取超时（毫秒）', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.agent.default-temperature', '0.7', 'Agent 默认 temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.agent.max-retries', '2', 'LLM failover 最大重试次数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.agent.retry-delay-ms', '2000', 'LLM failover 基础重试延迟（毫秒）', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.agent.search-default-topn', '5', 'Agent 知识搜索默认返回条数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.agent.search-result-truncate', '200', 'Agent 搜索结果内容截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.circuitbreaker.failure-threshold', '3', '熔断器：连续失败阈值', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.circuitbreaker.cooldown-seconds', '60', '熔断器：冷却秒数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.circuitbreaker.half-open-max', '2', '熔断器：半开最大尝试次数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.chat.log-truncate-length', '50', '聊天日志截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.chat.sse-timeout-ms', '300000', 'SSE 超时（毫秒）', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.chat.title-truncate-length', '30', '对话标题截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.daily-review.max-tokens', '1500', '每日复盘 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.daily-review.temperature', '0.3', '每日复盘 temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.initial-interval-days', '1', '初始复习间隔天数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.quality-min', '0', '复习质量评分最小值', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.quality-max', '5', '复习质量评分最大值', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.failed-interval-days', '1', '复习失败后间隔天数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.ease-factor-penalty', '0.20', '复习失败 ease factor 惩罚', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.first-success-interval', '1', '首次成功复习间隔天数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.second-success-interval', '6', '二次成功复习间隔天数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.ease-factor-adjustment', '0.10', '复习质量每分 ease factor 调整量', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.review.min-ease-factor', '1.30', '最小 ease factor', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.tokenusage.calc-divisor', '1000.0', 'Token 费用计算除数', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.session.cleaner-interval-ms', '60000', '会话清理轮询间隔（毫秒）', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.logging.trace-id-length', '16', 'Trace ID 长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.logging.error-body-truncate', '2000', '错误请求体日志截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.logging.slow-request-ms', '2000', '慢请求阈值（毫秒）', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.operationlog.args-truncate-length', '200', '操作日志参数截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.ratelimit.window-minutes', '1', '限流窗口分钟数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.system-config.cache-refresh-ms', '30000', '系统配置缓存刷新间隔（毫秒）', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.rewrite-max-tokens', '100', '查询改写 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.rewrite-temperature', '0.2', '查询改写 temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.hyde-max-tokens', '500', 'HyDE 文档 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.hyde-temperature', '0.3', 'HyDE 文档 temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.hyde-embedding-truncate', '8000', 'HyDE 嵌入截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.rerank-truncate-length', '300', '重排序内容截断长度', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.rerank-max-tokens', '200', '重排序 max_tokens', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.search.rerank-temperature', '0.1', '重排序 temperature', 'double');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('threshold.association.top-n', '6', '知识关联 top-N', 'int');


-- ============================================================
-- 4. 全局默认值（value_type = string）
-- ============================================================
INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.export.version', '0.4.0', '导出版本号', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.export.csv-header', '标题,内容,类型,摘要,标签,来源,创建时间', 'CSV 导出行头', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.export.markdown-untagged-folder', '未分类', 'Markdown 导出无标签归文件夹', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.knowledge.auto-process-status', 'PENDING', '知识自动处理初始状态', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.knowledge.tags-empty-json', '[]', '标签为空时的 JSON', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.knowledge.import-default-title', '未命名', '导入时缺省标题', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.knowledge.content-type', 'TEXT', '默认知识类型', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.content.pdf-default-title', '未命名文档', 'PDF 解析缺省标题', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.content.jsoup-user-agent', 'Mozilla/5.0 (compatible; MindVault/1.0)', 'Jsoup User-Agent', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.chat.session-title', '新对话', '对话默认标题', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.daily-review.empty-summary', '当日无新增知识。', '日志复盘空摘要', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.daily-review.fallback-summary', '当日知识较多，自动摘要生成失败。', '日志复盘失败摘要', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.writing.style', '写作风格: 清晰、条理、专业', '写作默认风格', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.writing.fallback-message', '文章生成失败，请稍后重试。', '写作失败提示', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.writing.no-model-message', '系统未配置可用模型，请先在设置中添加并启用模型。', '写作无模型提示', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.agent.no-model-message', '系统未配置主模型，请先在设置中添加并设置主模型。', 'Agent 无模型提示', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.agent.error-message', '抱歉，处理您的消息时遇到了问题，请稍后重试。', 'Agent 错误提示', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.agent.tool-result-prefix', '工具执行结果: ', 'Agent 工具结果前缀', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.backup.filename-date-format', 'yyyyMMdd-HHmmss', '备份文件名日期格式', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.backup.filename-prefix', 'mindvault-backup-', '备份文件名前缀', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.backup.file-pattern', 'mindvault-backup-', '备份文件匹配模式', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.user.role', 'USER', '新用户默认角色', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.system.app-version', '1.0.0', '系统版本号', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.llm.source-name', 'CHAT', 'LLM 调用默认来源', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('default.cors.allowed-origin-pattern', '*', 'CORS 允许源', 'string');

-- ============================================================
-- 5. 聊天关键字拦截 + 工具配置（v0.6）
-- ============================================================
INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('chat.keyword.blocklist', '', '聊天关键字拦截列表，逗号或换行分隔，支持 * 通配', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('chat.keyword.block-message', '消息包含受限内容，已拦截', '关键字拦截提示消息', 'string');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('chat.keyword.case-sensitive', 'false', '关键字匹配是否大小写敏感', 'bool');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('tool.search-knowledge.default-limit', '3', '@tool 知识搜索默认返回条数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('tool.search-knowledge.max-limit', '10', '@tool 知识搜索最大返回条数', 'int');

INSERT INTO system_config (config_key, config_value, description, value_type) VALUES
('tool.search-knowledge.search-method', 'rewrite', '@tool 知识搜索方法 (rewrite/hyde/hybrid)', 'string');
