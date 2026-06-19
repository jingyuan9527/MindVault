package com.mindvault.auth.service;

import com.mindvault.auth.entity.ApiToken;
import com.mindvault.auth.mapper.ApiTokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ApiTokenService {

    private static final Logger log = LoggerFactory.getLogger(ApiTokenService.class);

    private final ApiTokenMapper mapper;

    public ApiTokenService(ApiTokenMapper mapper) {
        this.mapper = mapper;
    }

    public ApiToken createToken(Long userId, String name, Integer expireDays) {
        ApiToken token = new ApiToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        token.setName(name);
        if (expireDays != null && expireDays > 0) {
            token.setExpiresAt(LocalDateTime.now().plusDays(expireDays));
        }
        token.setCreatedAt(LocalDateTime.now());
        mapper.insert(token);
        log.info("创建 API Token: userId={}, name={}", userId, name);
        return token;
    }

    public ApiToken validateToken(String token) {
        ApiToken apiToken = mapper.findByToken(token);
        if (apiToken == null) return null;
        if (apiToken.getExpiresAt() != null && apiToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            mapper.deleteById(apiToken.getId());
            return null;
        }
        mapper.updateLastUsedAt(apiToken.getId());
        return apiToken;
    }

    public List<ApiToken> listByUser(Long userId) {
        return mapper.findByUserId(userId);
    }

    public void deleteToken(Long userId, Long tokenId) {
        ApiToken token = mapper.selectById(tokenId);
        if (token != null && token.getUserId().equals(userId)) {
            mapper.deleteById(tokenId);
            log.info("删除 API Token: id={}, userId={}", tokenId, userId);
        }
    }
}
