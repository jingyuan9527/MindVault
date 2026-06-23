package com.mindvault.chat;

import com.mindvault.systemconfig.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class KeywordBlockingService {

    private static final Logger log = LoggerFactory.getLogger(KeywordBlockingService.class);

    private final SystemConfigService config;

    private volatile List<Pattern> cachedPatterns = List.of();
    private volatile long lastRefresh = 0;

    public KeywordBlockingService(SystemConfigService config) {
        this.config = config;
    }

    public boolean isBlocked(String message) {
        if (message == null || message.isBlank()) return false;
        refreshPatterns();
        for (Pattern p : cachedPatterns) {
            if (p.matcher(message).find()) return true;
        }
        return false;
    }

    public String getBlockMessage() {
        return config.getString("chat.keyword.block-message", "消息包含受限内容，已拦截");
    }

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