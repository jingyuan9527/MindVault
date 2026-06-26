package com.mindvault.auth.service;

import com.mindvault.auth.entity.User;
import com.mindvault.auth.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户业务服务，封装认证、创建、密码修改及状态管理等核心逻辑。
 *
 * <p>依赖 UserMapper 进行数据持久化，使用 BCryptPasswordEncoder
 * 对密码进行加盐哈希（不可逆存储）。所有密码操作均不保留明文，
 * authenticate 方法仅返回匹配结果而非直接暴露密码字段。</p>
 *
 * <p>关键设计：
 * <ul>
 *   <li>密码从不以明文形式记录日志或返回响应</li>
 *   <li>setEnabled 支持账户启用/禁用，禁用的账户无法通过 authenticate 验证</li>
 *   <li>createUser 在用户名已存在时抛出 IllegalArgumentException 而非静默覆盖</li>
 * </ul>
 * </p>
 *
 * @see UserMapper
 * @see UserContext
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 认证用户：校验用户名和密码是否匹配。
     *
     * @param username 用户名
     * @param password 明文密码（将与存储的哈希比对）
     * @return 认证成功时返回用户实体，失败或用户被禁用时返回 null
     */
    public User authenticate(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !user.getEnabled()) return null;
        if (!passwordEncoder.matches(password, user.getPasswordHash())) return null;
        return user;
    }

    /**
     * 创建新用户。
     *
     * @param username    用户名（唯一）
     * @param password    明文密码，自动进行 BCrypt 哈希后存储
     * @param displayName 显示名称，为 null 时使用 username
     * @param role        角色，为 null 时默认 "USER"
     * @return 创建成功的用户实体（不含密码明文）
     * @throws IllegalArgumentException 用户名已存在时抛出
     */
    public User createUser(String username, String password, String displayName, String role) {
        if (userMapper.findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName != null ? displayName : username);
        user.setRole(role != null ? role : "USER");
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        log.info("创建用户: username={}, role={}", username, role);
        return user;
    }

    /**
     * 修改用户密码。
     *
     * 先校验旧密码匹配性，匹配成功后才将新密码 BCrypt 加密后更新。
     *
     * @param userId      用户 ID
     * @param oldPassword 旧密码（用于身份验证）
     * @param newPassword 新密码（明文，将被哈希后存储）
     * @return true 修改成功；false 用户不存在或旧密码错误
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) return false;
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户修改密码: userId={}", userId);
        return true;
    }

    /**
     * 根据 ID 获取用户。
     *
     * @param id 用户 ID
     * @return 用户实体，不存在时返回 null
     */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 获取所有用户列表。
     *
     * @return 全部用户的列表
     */
    public List<User> listAll() {
        return userMapper.selectList(null);
    }

    /**
     * 设置用户的启用/禁用状态。
     *
     * 禁用的账户将无法通过 authenticate 登录，但数据保留不变。
     *
     * @param userId  用户 ID
     * @param enabled true 启用，false 禁用
     * @return true 操作成功；false 用户不存在
     */
    public boolean setEnabled(Long userId, boolean enabled) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        user.setEnabled(enabled);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("{}用户: userId={}", enabled ? "启用" : "禁用", userId);
        return true;
    }
}
