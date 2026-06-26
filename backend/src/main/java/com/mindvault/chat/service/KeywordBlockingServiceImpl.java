package com.mindvault.chat.service;

import com.mindvault.systemconfig.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 关键字拦截服务。
 * <p>基于系统配置的关键字列表对用户消息进行敏感内容过滤。拦截规则支持通配符（* 匹配多个非空字符，? 匹配单个非空字符），
 * 可配置大小写敏感。关键字列表和提示消息通过 SystemConfigService 动态读取，支持运行期修改。
 * 使用 30 秒缓存过期策略减少配置查询频率。输出为用户可见的拦截提示消息。</p>
 */
@Service
public class KeywordBlockingServiceImpl implements KeywordBlockingService {

    private static final Logger log = LoggerFactory.getLogger(KeywordBlockingServiceImpl.class);

    private final SystemConfigService config;

    private volatile List<Pattern> cachedPatterns = List.of();
    private volatile long lastRefresh = 0;

    public KeywordBlockingServiceImpl(SystemConfigService config) {
        this.config = config;
    }

    /**
     * 检查消息是否命中关键字拦截规则。
     * @param message 用户消息
     * @return true 表示消息被拦截
     */
    public boolean isBlocked(String message) {
        if (message == null || message.isBlank()) return false;
        refreshPatterns();
        for (Pattern p : cachedPatterns) {
            if (p.matcher(message).find()) return true;
        }
        return false;
    }

    /**
     * 获取拦截时返回给用户的提示消息。
     * 从系统配置中读取 chat.keyword.block-message，不存在时使用默认值。
     * @return 拦截提示文本
     */
    public String getBlockMessage() {
        return config.getString("chat.keyword.block-message", "消息包含受限内容，已拦截");
    }

    /**
     * 刷新编译后的关键字正则缓存。
     * 30 秒内不重复刷新。从配置读取关键字列表，用逗号/换行分割，支持通配符转正则。
     */
    private void refreshPatterns() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < 30_000) return;
        lastRefresh = now;

        String raw = config.getString("chat.keyword.blocklist", "");
        List<Pattern> patterns = new ArrayList<>();
        if (!raw.isBlank()) {
            for (String keyword : raw.split("\\s*[,\n]\\s*")) {
                keyword = keyword.trim();
                if (keyword.isEmpty()) continue;
                String regex = keyword
                        .replace("*", "\\S*")
                        .replace(".", "\\.")
                        .replace("?", "\\S")
                        .replace("(", "\\(")
                        .replace(")", "\\)")
                        .replace("[", "\\[")
                        .replace("]", "\\]");
                boolean caseSensitive = config.getBool("chat.keyword.case-sensitive", false);
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                try {
                    patterns.add(Pattern.compile(regex, flags));
                } catch (Exception e) {
                    log.warn("关键字正则编译失败: '{}'", keyword, e);
                }
            }
        }
        cachedPatterns = patterns;
    }
}