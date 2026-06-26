package com.mindvault.auth.service;

import com.mindvault.auth.entity.ApiToken;
import com.mindvault.auth.mapper.ApiTokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API 令牌业务服务，管理令牌的创建、验证、查询和删除。
 *
 * <p>令牌用于程序化访问（如 CI/CD、第三方集成），具有长生命周期，
 * 区别于浏览器的短会话。令牌字符串由两段无分隔符 UUID 拼接生成，
 * 长度为 64 字符，具有高熵值难以猜测。</p>
 *
 * <p>验证逻辑：先查库校验令牌是否存在，再检查 expiresAt 是否超期。
 * 超期令牌自动删除并返回 null，避免过期令牌堆积。</p>
 *
 * @see ApiTokenMapper
 * @see ApiToken
 */
@Service
public class ApiTokenService {

    private static final Logger log = LoggerFactory.getLogger(ApiTokenService.class);

    private final ApiTokenMapper mapper;

    public ApiTokenService(ApiTokenMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 为用户创建新的 API 令牌。
     *
     * 令牌字符串由两段 UUID 无分隔符拼接而成（64 字符）。
     * expireDays 为 null 或 ≤0 时创建永不过期的令牌。
     *
     * @param userId     所属用户 ID
     * @param name       令牌名称（用于 UI 区分不同用途）
     * @param expireDays 过期天数（可选，null 或 ≤0 表示永不过期）
     * @return 创建的令牌实体（包含完整令牌字符串）
     */
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

    /**
     * 验证 API 令牌是否有效。
     *
     * 检查令牌存在性及过期时间。过期的令牌自动从数据库删除。
     * 每次验证成功后更新 lastUsedAt 时间戳。
     *
     * @param token 令牌字符串
     * @return 有效的令牌实体，无效或已过期时返回 null
     */
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

    /**
     * 查询指定用户的所有令牌。
     *
     * @param userId 用户 ID
     * @return 令牌列表（按创建时间倒序）
     */
    public List<ApiToken> listByUser(Long userId) {
        return mapper.findByUserId(userId);
    }

    /**
     * 删除指定令牌。
     *
     * 先校验令牌归属权（仅令牌所属的用户才能删除），防止越权操作。
     *
     * @param userId  用户 ID（用于归属校验）
     * @param tokenId 令牌 ID
     */
    public void deleteToken(Long userId, Long tokenId) {
        ApiToken token = mapper.selectById(tokenId);
        if (token != null && token.getUserId().equals(userId)) {
            mapper.deleteById(tokenId);
            log.info("删除 API Token: id={}, userId={}", tokenId, userId);
        }
    }
}
