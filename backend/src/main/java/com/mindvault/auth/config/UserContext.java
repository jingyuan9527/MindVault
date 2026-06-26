package com.mindvault.auth.config;

/**
 * 当前请求的用户上下文，基于 ThreadLocal 实现请求级线程隔离。
 *
 * <p>在 AuthFilter 中通过 Authorization 头解析出用户身份后，
 * 将用户信息存入此上下文；请求处理完成后在 finally 块中清除，
 * 避免线程复用导致的信息泄漏。</p>
 *
 * <p>使用注意事项：
 * <ul>
 *   <li>仅应在 Controller/Service 层读取，不可在异步线程中使用</li>
 *   <li>每次请求必须成对调用 set/clear，推荐在 Filter 中用 try-finally 保证</li>
 *   <li>UserInfo 为不可变 record，字段包括 userId、username、role</li>
 * </ul>
 * </p>
 *
 * @see AuthFilter
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> currentUser = new ThreadLocal<>();

    /**
     * 设置当前请求的用户信息。
     *
     * @param info 用户身份信息（userId, username, role）
     */
    public static void set(UserInfo info) {
        currentUser.set(info);
    }

    /**
     * 获取当前请求的用户信息。
     *
     * @return 用户身份信息，未认证时返回 null
     */
    public static UserInfo get() {
        return currentUser.get();
    }

    /**
     * 清除当前请求的用户信息。
     *
     * <p>必须在请求处理完成后的 finally 块中调用，防止线程池复用
     * 导致后续请求误用前一个用户的身份。</p>
     */
    public static void clear() {
        currentUser.remove();
    }

    /**
     * 请求作用域内的用户身份信息。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色（USER/ADMIN/API）
     */
    public record UserInfo(Long userId, String username, String role) {}
}
