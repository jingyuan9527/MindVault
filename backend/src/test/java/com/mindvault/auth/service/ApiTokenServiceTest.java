package com.mindvault.auth.service;

import com.mindvault.auth.entity.ApiToken;
import com.mindvault.auth.mapper.ApiTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiTokenServiceTest {

    @Mock
    private ApiTokenMapper mapper;

    private ApiTokenService service;

    @Captor
    private ArgumentCaptor<ApiToken> tokenCaptor;

    @BeforeEach
    void setUp() {
        service = new ApiTokenService(mapper);
    }

    private ApiToken createToken(Long id, Long userId, String token, LocalDateTime expiresAt) {
        ApiToken t = new ApiToken();
        t.setId(id);
        t.setUserId(userId);
        t.setToken(token);
        t.setName("test-token");
        t.setExpiresAt(expiresAt);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    void createToken_shouldSetExpiresAtWhenExpireDaysGiven() {
        when(mapper.insert(any(ApiToken.class))).thenReturn(1);

        ApiToken result = service.createToken(1L, "my-token", 30);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("my-token", result.getName());
        assertNotNull(result.getExpiresAt());
        assertNotNull(result.getToken());
        assertTrue(result.getToken().length() > 20);
        verify(mapper).insert(any(ApiToken.class));
    }

    @Test
    void createToken_shouldNotSetExpiresAtWhenExpireDaysNull() {
        when(mapper.insert(any(ApiToken.class))).thenReturn(1);

        ApiToken result = service.createToken(1L, "permanent", null);

        assertNull(result.getExpiresAt());
    }

    @Test
    void validateToken_shouldReturnTokenWhenValid() {
        ApiToken token = createToken(1L, 1L, "valid-token-value", LocalDateTime.now().plusDays(10));
        when(mapper.findByToken("valid-token-value")).thenReturn(token);
        doNothing().when(mapper).updateLastUsedAt(anyLong());

        ApiToken result = service.validateToken("valid-token-value");

        assertNotNull(result);
        assertEquals("valid-token-value", result.getToken());
        verify(mapper).updateLastUsedAt(1L);
    }

    @Test
    void validateToken_shouldReturnNullWhenTokenNotFound() {
        when(mapper.findByToken("unknown")).thenReturn(null);

        ApiToken result = service.validateToken("unknown");

        assertNull(result);
        verify(mapper, never()).updateLastUsedAt(anyLong());
    }

    @Test
    void validateToken_shouldDeleteAndReturnNullWhenExpired() {
        ApiToken token = createToken(1L, 1L, "expired", LocalDateTime.now().minusDays(1));
        when(mapper.findByToken("expired")).thenReturn(token);

        ApiToken result = service.validateToken("expired");

        assertNull(result);
        verify(mapper).deleteById(1L);
        verify(mapper, never()).updateLastUsedAt(anyLong());
    }

    @Test
    void listByUser_shouldReturnTokens() {
        List<ApiToken> tokens = List.of(
                createToken(1L, 1L, "a", null),
                createToken(2L, 1L, "b", null)
        );
        when(mapper.findByUserId(1L)).thenReturn(tokens);

        List<ApiToken> result = service.listByUser(1L);

        assertEquals(2, result.size());
    }

    @Test
    void deleteToken_shouldDeleteWhenOwner() {
        ApiToken token = createToken(1L, 1L, "token", null);
        when(mapper.selectById(1L)).thenReturn(token);

        service.deleteToken(1L, 1L);

        verify(mapper).deleteById(1L);
    }

    @Test
    void deleteToken_shouldNotDeleteWhenNotOwner() {
        ApiToken token = createToken(1L, 2L, "token", null);
        when(mapper.selectById(1L)).thenReturn(token);

        service.deleteToken(1L, 1L);

        verify(mapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteToken_shouldDoNothingWhenTokenNotFound() {
        when(mapper.selectById(99L)).thenReturn(null);

        service.deleteToken(1L, 99L);

        verify(mapper, never()).deleteById(anyLong());
    }
}
