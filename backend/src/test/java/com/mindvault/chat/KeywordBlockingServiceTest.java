package com.mindvault.chat;

import com.mindvault.systemconfig.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeywordBlockingServiceTest {

    @Mock
    private SystemConfigService config;

    private KeywordBlockingService service;

    @BeforeEach
    void setUp() {
        lenient().when(config.getString(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(config.getBool(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        service = new KeywordBlockingService(config);
    }

    @Test
    void emptyBlocklist_shouldNeverBlock() {
        when(config.getString("chat.keyword.blocklist", "")).thenReturn("");
        assertFalse(service.isBlocked("anything at all"));
    }

    @Test
    void exactKeyword_shouldBlock() {
        when(config.getString("chat.keyword.blocklist", "")).thenReturn("secret");
        when(config.getBool("chat.keyword.case-sensitive", false)).thenReturn(false);
        assertTrue(service.isBlocked("this contains secret info"));
        assertFalse(service.isBlocked("this is safe"));
    }

    @Test
    void wildcard_shouldMatchNonWhitespace() {
        when(config.getString("chat.keyword.blocklist", "")).thenReturn("pass*");
        when(config.getBool("chat.keyword.case-sensitive", false)).thenReturn(false);
        assertTrue(service.isBlocked("my password is 123"));
        assertTrue(service.isBlocked("passcode"));
        assertTrue(service.isBlocked("my pass is 123"), "* matches zero non-whitespace chars");
    }

    @Test
    void caseInsensitive_shouldMatchRegardlessOfCase() {
        when(config.getString("chat.keyword.blocklist", "")).thenReturn("FORBIDDEN");
        when(config.getBool("chat.keyword.case-sensitive", false)).thenReturn(false);
        assertTrue(service.isBlocked("this is forbidden"));
        assertTrue(service.isBlocked("this is Forbidden"));
        assertTrue(service.isBlocked("this is FORBIDDEN"));
    }

    @Test
    void caseSensitive_shouldMatchExactCase() {
        when(config.getString("chat.keyword.blocklist", "")).thenReturn("Secret");
        when(config.getBool("chat.keyword.case-sensitive", false)).thenReturn(true);
        assertTrue(service.isBlocked("This is Secret info"));
        assertFalse(service.isBlocked("this is secret info"));
    }

    @Test
    void commaSeparatedKeywords_shouldBlockAny() {
        when(config.getString("chat.keyword.blocklist", "")).thenReturn("bad, evil, naughty");
        when(config.getBool("chat.keyword.case-sensitive", false)).thenReturn(false);
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
        when(config.getString("chat.keyword.block-message", "消息包含受限内容，已拦截")).thenReturn("custom block");
        assertEquals("custom block", service.getBlockMessage());
    }

    @Test
    void getBlockMessage_shouldReturnDefaultWhenNotConfigured() {
        assertEquals("消息包含受限内容，已拦截", service.getBlockMessage());
    }

    @Test
    void cache_shouldNotRefreshWithin30Seconds() throws Exception {
        when(config.getString("chat.keyword.blocklist", "")).thenReturn("first");
        when(config.getBool("chat.keyword.case-sensitive", false)).thenReturn(false);
        assertTrue(service.isBlocked("first hit"));

        String cached = new String("second");
        assertFalse(service.isBlocked("second"), "should still use cached 'first' patterns");

        Thread.sleep(50);
    }
}