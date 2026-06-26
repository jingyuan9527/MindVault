package com.mindvault.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mindvault.chat")
public class ChatProperties {

    private Keyword keyword = new Keyword();

    public Keyword getKeyword() { return keyword; }
    public void setKeyword(Keyword keyword) { this.keyword = keyword; }

    public static class Keyword {
        private String blockMessage = "消息包含受限内容，已拦截";
        private String blocklist = "";
        private boolean caseSensitive = false;

        public String getBlockMessage() { return blockMessage; }
        public void setBlockMessage(String blockMessage) { this.blockMessage = blockMessage; }
        public String getBlocklist() { return blocklist; }
        public void setBlocklist(String blocklist) { this.blocklist = blocklist; }
        public boolean isCaseSensitive() { return caseSensitive; }
        public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
    }
}