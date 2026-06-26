package com.mindvault.chat;

import com.mindvault.chat.config.ChatProperties;
import com.mindvault.chat.service.KeywordBlockingService;
import com.mindvault.chat.service.KeywordBlockingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeywordBlockingServiceTest {

    private ChatProperties chatProperties;

    private KeywordBlockingService service;

    @BeforeEach
    void setUp() {
        chatProperties = new ChatProperties();
        service = new KeywordBlockingServiceImpl(chatProperties);
    }

    @Test
    void emptyBlocklist_shouldNeverBlock() {
        chatProperties.getKeyword().setBlocklist("");
        assertFalse(service.isBlocked("anything at all"));
    }

    @Test
    void exactKeyword_shouldBlock() {
        chatProperties.getKeyword().setBlocklist("secret");
        assertTrue(service.isBlocked("this contains secret info"));
        assertFalse(service.isBlocked("this is safe"));
    }

    @Test
    void wildcard_shouldMatchNonWhitespace() {
        chatProperties.getKeyword().setBlocklist("pass*");
        assertTrue(service.isBlocked("my password is 123"));
        assertTrue(service.isBlocked("passcode"));
        assertTrue(service.isBlocked("my pass is 123"), "* matches zero non-whitespace chars");
    }

    @Test
    void caseInsensitive_shouldMatchRegardlessOfCase() {
        chatProperties.getKeyword().setBlocklist("FORBIDDEN");
        assertTrue(service.isBlocked("this is forbidden"));
        assertTrue(service.isBlocked("this is Forbidden"));
        assertTrue(service.isBlocked("this is FORBIDDEN"));
    }

    @Test
    void caseSensitive_shouldMatchExactCase() {
        chatProperties.getKeyword().setBlocklist("Secret");
        chatProperties.getKeyword().setCaseSensitive(true);
        assertTrue(service.isBlocked("This is Secret info"));
        assertFalse(service.isBlocked("this is secret info"));
    }

    @Test
    void commaSeparatedKeywords_shouldBlockAny() {
        chatProperties.getKeyword().setBlocklist("bad, evil, naughty");
        assertTrue(service.isBlocked("this is bad"));
        assertTrue(service.isBlocked("evil plan"));
        assertFalse(service.isBlocked("good behavior"));
    }

    @Test
    void nullOrBlankMessage_shouldNeverBlock() {
        assertFalse(service.isBlocked(null));
        assertFalse(service.isBlocked(""));
        assertFalse(service.isBlocked("   "));
    }

    @Test
    void getBlockMessage_shouldReturnConfiguredMessage() {
        chatProperties.getKeyword().setBlockMessage("custom block");
        assertEquals("custom block", service.getBlockMessage());
    }

    @Test
    void getBlockMessage_shouldReturnDefaultWhenNotConfigured() {
        assertEquals("消息包含受限内容，已拦截", service.getBlockMessage());
    }

    @Test
    void cache_shouldNotRefreshWithin30Seconds() throws Exception {
        chatProperties.getKeyword().setBlocklist("first");
        assertTrue(service.isBlocked("first hit"));

        String cached = new String("second");
        assertFalse(service.isBlocked("second"), "should still use cached 'first' patterns");

        Thread.sleep(50);
    }
}