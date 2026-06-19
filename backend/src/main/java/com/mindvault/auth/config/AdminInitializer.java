package com.mindvault.auth.config;

import com.mindvault.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mindvault.auth.enabled", havingValue = "true", matchIfMissing = true)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserService userService;

    public AdminInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        String adminUsername = System.getenv("MINDVAULT_ADMIN_USERNAME");
        String adminPassword = System.getenv("MINDVAULT_ADMIN_PASSWORD");

        if (adminUsername != null && !adminUsername.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
            try {
                userService.createUser(adminUsername, adminPassword, "管理员", "ADMIN");
                log.info("管理员用户已创建: username={}", adminUsername);
            } catch (IllegalArgumentException e) {
                log.info("管理员用户已存在: username={}", adminUsername);
            }
        } else {
            log.info("未配置 MINDVAULT_ADMIN_USERNAME/MINDVAULT_ADMIN_PASSWORD，跳过管理员创建");
        }
    }
}
