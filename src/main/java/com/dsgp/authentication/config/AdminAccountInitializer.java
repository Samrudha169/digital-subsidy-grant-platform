package com.dsgp.authentication.config;

import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.entity.OfficerRole;
import com.dsgp.authentication.repository.OfficerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(
            OfficerRepository officerRepository,
            PasswordEncoder passwordEncoder) {

        this.officerRepository = officerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        /*
         * Create the first ADMIN account only if
         * an account with username "admin" does not exist.
         */
        if (officerRepository.existsByUsername("admin")) {
            System.out.println("ADMIN account already exists.");
            return;
        }

        Officer admin = Officer.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin@123"))
                .fullName("System Administrator")
                .email("admin@dsgp.gov.in")
                .role(OfficerRole.ADMIN)
                .district(null)
                .active(true)
                .build();

        officerRepository.save(admin);

        System.out.println("==============================================");
        System.out.println("  DEFAULT ADMIN ACCOUNT CREATED");
        System.out.println("  Username : admin");
        System.out.println("  Password : Admin@123");
        System.out.println("  Role     : ADMIN");
        System.out.println("==============================================");
    }
}