package com.mindvault.auth.service;

import com.mindvault.auth.entity.User;
import com.mindvault.auth.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User authenticate(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !user.getEnabled()) return null;
        if (!passwordEncoder.matches(password, user.getPasswordHash())) return null;
        return user;
    }

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

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    public List<User> listAll() {
        return userMapper.selectList(null);
    }

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
