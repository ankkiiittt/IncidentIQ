package com.incidentiq.user.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import com.incidentiq.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class AdminBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public AdminBootstrapService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            @Value("${ADMIN_BOOTSTRAP_EMAIL:admin@incidentiq.local}") String bootstrapEmail,
            @Value("${ADMIN_BOOTSTRAP_PASSWORD:AdminInitialPassword123!}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmailIgnoreCase(bootstrapEmail)) {
            log.info("No administrator found. Bootstrapping initial admin account for {}", bootstrapEmail);
            User admin = new User(
                    "IncidentIQ Administrator",
                    bootstrapEmail.toLowerCase().trim(),
                    passwordEncoder.encode(bootstrapPassword),
                    UserRole.ADMIN
            );
            admin.setMustChangePassword(true);
            User saved = userRepository.save(admin);

            auditService.record(
                    null,
                    "BOOTSTRAP_ADMIN_CREATED",
                    "USER",
                    saved.getId().toString(),
                    Map.of("email", bootstrapEmail),
                    "system"
            );
            log.info("Administrator account bootstrapped successfully with must_change_password=true.");
        }
    }
}
