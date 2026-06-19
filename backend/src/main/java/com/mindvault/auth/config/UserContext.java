package com.mindvault.auth.config;

public class UserContext {

    private static final ThreadLocal<UserInfo> currentUser = new ThreadLocal<>();

    public static void set(UserInfo info) {
        currentUser.set(info);
    }

    public static UserInfo get() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }

    public record UserInfo(Long userId, String username, String role) {}
}
