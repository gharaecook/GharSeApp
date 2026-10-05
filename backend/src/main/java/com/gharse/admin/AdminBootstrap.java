package com.gharse.admin;

import com.gharse.common.AppProperties;
import com.gharse.user.User;
import com.gharse.user.UserRepository;
import com.gharse.user.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD when set and no such account exists. */
@Component
@Order(1)
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrap(AppProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = props.adminBootstrap().email();
        String password = props.adminBootstrap().password();
        if (email == null || email.isBlank() || password == null || password.length() < 8) return;
        if (users.existsByEmail(email)) return;
        User admin = new User();
        admin.setName("GharSe Admin");
        admin.setPhone("0000000000");
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        users.save(admin);
        log.info("Bootstrap admin account created");
    }
}
