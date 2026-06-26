package com.mindvault.chat.service;

/**
 * 关键字拦截服务接口。
 * <p>基于系统配置的关键字列表对用户消息进行敏感内容过滤。</p>
 */
public interface KeywordBlockingService {

    boolean isBlocked(String message);

    String getBlockMessage();
}