package com.mindvault.auth.config;

import com.mindvault.auth.entity.User;
import com.mindvault.common.config.MindVaultProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存会话管理器，基于 ConcurrentHashMap 实现用户登录态的会话管理。
 *
 * <p>会话生命周期由 TTL（从 MindVaultProperties 读取，默认按小时计）控制。
 * 后台守护线程每分钟扫描一次，清除过期会话。创建会话时生成 UUID 作为令牌，
 * 验证时检查令牌是否存在及是否超期。</p>
 *
 * <p>设计决策：
 * <ul>
 *   <li>使用内存存储而非 Redis，简化部署依赖。单实例重启后所有会话失效，用户需重新登录。</li>
 *   <li>采用守护线程定期清理而非惰性删除，防止已过期会话长期占用内存。</li>
 *   <li>SessionInfo 为不可变 record，包含 userId、username、role 和创建时间戳。</li>
 * </ul>
 * </p>
 *
 * @see AuthFilter 验证流程中调用 validate()
 * @see MindVaultProperties 提供 session.ttl-hours 配置
 */
@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final long sessionTtlHours;

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public SessionManager(MindVaultProperties properties) {
        this.sessionTtlHours = properties.getSession().getTtlHours();
    }

    /**
     * 初始化会话清理守护线程。
     *
     * 每分钟扫描一次所有会话，移除创建时间早于 (当前时间 - TTL) 的过期会话。
     */
    @PostConstruct
    public void init() {
        Thread cleaner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                    Instant cutoff = Instant.now().minus(sessionTtlHours, ChronoUnit.HOURS);
                    sessions.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "session-cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
    }

    /**
     * 为用户创建新会话令牌。
     *
     * @param user 已认证的用户实体
     * @return UUID 格式的会话令牌字符串
     */
    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionInfo(user.getId(), user.getUsername(), user.getRole(), Instant.now()));
        return token;
    }

    /**
     * 验证会话令牌是否有效。
     *
     * 校验令牌存在性和 TTL 超期情况，过期令牌会被自动移除。
     *
     * @param token 会话令牌字符串
     * @return 有效时返回会话信息，无效或过期返回 null
     */
    public SessionInfo validate(String token) {
        SessionInfo info = sessions.get(token);
        if (info == null) return null;
        if (info.createdAt().isBefore(Instant.now().minus(sessionTtlHours, ChronoUnit.HOURS))) {
            sessions.remove(token);
            return null;
        }
        return info;
    }

    /**
     * 移除指定会话令牌（登出操作）。
     *
     * @param token 要移除的会话令牌
     */
    public void remove(String token) {
        sessions.remove(token);
    }

    public record SessionInfo(Long userId, String username, String role, Instant createdAt) {}
}
