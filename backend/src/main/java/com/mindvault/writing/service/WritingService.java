package com.mindvault.writing.service;

/**
 * AI 写作助手服务接口。
 * <p>根据用户提供的主题、风格和关键词，结合知识库中的相关参考内容，通过 LLM 生成结构化文章。</p>
 */
public interface WritingService {

    String generateArticle(String topic, String style, String keywords);
}