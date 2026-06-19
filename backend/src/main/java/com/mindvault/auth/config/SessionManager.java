package com.mindvault.auth.config;

import com.mindvault.auth.entity.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final long SESSION_TTL_HOURS = 24;

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Thread cleaner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                    Instant cutoff = Instant.now().minus(SESSION_TTL_HOURS, ChronoUnit.HOURS);
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

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionInfo(user.getId(), user.getUsername(), user.getRole(), Instant.now()));
        return token;
    }

    public SessionInfo validate(String token) {
        SessionInfo info = sessions.get(token);
        if (info == null) return null;
        if (info.createdAt().isBefore(Instant.now().minus(SESSION_TTL_HOURS, ChronoUnit.HOURS))) {
            sessions.remove(token);
            return null;
        }
        return info;
    }

    public void remove(String token) {
        sessions.remove(token);
    }

    public record SessionInfo(Long userId, String username, String role, Instant createdAt) {}
}
