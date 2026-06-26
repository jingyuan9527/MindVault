package com.mindvault.auth.config;

import com.mindvault.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 管理员账户初始化器，在应用启动时执行。
 *
 * <p>通过环境变量 MINDVAULT_ADMIN_USERNAME 和 MINDVAULT_ADMIN_PASSWORD
 * 配置初始管理员凭据。首次启动时创建 ADMIN 角色用户；如果用户已存在
 * （UserService.createUser 抛出 IllegalArgumentException），则跳过创建。
 * 未配置环境变量时仅记录日志，不执行任何操作。</p>
 *
 * <p>设计决策：使用环境变量而非配置文件，防止密码因版本控制而泄漏。</p>
 *
 * @see UserService#createUser(String, String, String, String)
 */
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
