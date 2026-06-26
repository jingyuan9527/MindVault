package com.mindvault.agent.service;

/**
 * AI Agent 核心服务。
 * <p>整合 LLM 调用与工具执行（知识搜索、知识添加），提供同步和流式两种消息处理模式。
 * 输入为用户消息文本，输出为 AI 回复文本（同步）或通过 StreamCallback 逐 token 回调（流式）。</p>
 */
public interface AgentService {

    /**
     * 同步处理用户消息，返回 AI 完整回复。
     * @param userMessage 用户消息文本
     * @return AI 回复文本，异常时返回错误提示
     */
    String processMessage(String userMessage);

    /**
     * 流式处理用户消息，通过回调逐 token 输出 AI 回复。
     * @param userMessage 用户消息文本
     * @param callback 流式回调接口
     */
    void processMessageStream(String userMessage, StreamCallback callback);

    interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(String error);
        default boolean isDisposed() { return false; }
    }
}