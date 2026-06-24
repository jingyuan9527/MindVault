package com.mindvault.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ModelApiIntegrationTest {

    private static final String BASE_URL = "https://apihub.agnes-ai.com/v1";
    private static final String MODEL_NAME = "agnes-2.0-flash";
    private static final String API_KEY = System.getenv("MINDVAULT_TEST_API_KEY");

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static ChatModel chatModel;

    @BeforeAll
    static void setup() {
        assumeTrue(API_KEY != null, "MINDVAULT_TEST_API_KEY env var not set");

        chatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(BASE_URL)
                        .apiKey(API_KEY)
                        .model(MODEL_NAME)
                        .build())
                .build();
    }

    @Test
    void chatCompletionReturnsValidResponse() {
        String result = chatModel.call("Hi");

        assertNotNull(result, "response should not be null");
        assertFalse(result.isBlank(), "extracted content should not be blank");
    }

    @Test
    void queryRewriteWorks() {
        String query = "什么是Java中的虚引用";
        String prompt = """
                你是一个搜索查询优化助手。请将用户的原始问题改写成更适合向量检索和关键词检索的形式。
                要求：
                1. 提取核心关键词和实体
                2. 补充同义词或相关术语
                3. 保持简洁，长度不超过50字
                4. 只返回改写后的查询文本，不要额外说明

                原始问题: %s
                """.formatted(query);

        String result = chatModel.call(prompt);

        assertNotNull(result, "rewrite query should produce output");
        assertFalse(result.isBlank(), "rewrite query output should not be blank");
        assertTrue(result.length() <= 100, "rewrite output should be concise: " + result);
    }

    @Test
    void hydeDocumentGenerationWorks() {
        String query = "如何优化Spring Boot应用性能";
        String prompt = """
                你是一个知识库检索助手。用户提出了一个问题，请生成一段假设性的文档内容，
                这段内容应当包含回答该问题所需的关键信息。只返回文档内容本身，不要额外说明。

                用户问题: %s
                """.formatted(query);

        String result = chatModel.call(prompt);

        assertNotNull(result, "HyDE doc generation should produce output");
        assertTrue(result.length() > 20, "HyDE doc should be substantive: " + result);
    }

    @Test
    void rerankScoringWorks() throws Exception {
        String query = "Spring Boot 性能优化";
        String prompt = """
                请评估以下搜索结果与用户查询的相关性。对每条结果给出 0-10 的分数（10 最相关）。

                用户查询: %s

                搜索结果:
                --- 结果 1 ---
                标题: Spring Boot 启动速度优化
                内容: 介绍如何通过懒加载、排除自动配置等方式加速Spring Boot启动

                --- 结果 2 ---
                标题: Vue.js 组件通信
                内容: 讲解Vue中父子组件通信的各种方式，包括props、emit等

                --- 结果 3 ---
                标题: JVM 性能调优
                内容: JVM内存模型、垃圾回收算法、GC调优实战

                请只返回 JSON 数组格式的评分，例如 [9, 5, 7]，不要额外说明。
                """.formatted(query);

        String result = chatModel.call(prompt);

        assertNotNull(result, "rerank should produce output");

        String cleaned = result.trim();
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        assertTrue(start >= 0 && end > start,
                "rerank output should contain a JSON array: " + result);

        cleaned = cleaned.substring(start, end + 1);
        List<Integer> scores = objectMapper.readValue(cleaned,
                new TypeReference<List<Integer>>() {});
        assertEquals(3, scores.size(), "should have 3 scores matching 3 results");
        assertTrue(scores.get(0) >= scores.get(1),
                "result 1 (Spring Boot) should score higher than result 2 (Vue)");
    }

    @Test
    void longContextStillWorks() {
        String longText = "A ".repeat(2000).trim();

        String result = chatModel.call("Repeat after me: " + longText);

        assertNotNull(result, "should handle ~4K context without error");
    }
}