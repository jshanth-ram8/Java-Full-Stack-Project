package com.ambulance.config;

import com.ambulance.model.Role;
import com.ambulance.model.User;
import com.ambulance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first administrator account if none exists yet. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository users,
                      PasswordEncoder encoder,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.existsByRole(Role.ADMIN)) {
            return;
        }
        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPassword(encoder.encode(adminPassword));
        admin.setPhone("0000000000");
        admin.setRole(Role.ADMIN);
        users.save(admin);
    }
}
