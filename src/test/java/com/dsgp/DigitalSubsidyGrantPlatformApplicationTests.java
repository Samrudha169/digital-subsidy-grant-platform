package com.dsgp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test to verify the Spring application context loads successfully.
 *
 * <p>This test is intentionally minimal — it only verifies that the Spring Boot
 * application context starts without errors. Business-level unit and integration
 * tests will be added in subsequent implementation phases.
 *
 * <p><strong>Note:</strong> This test requires a MySQL instance at the configured URL,
 * or can be run with an in-memory datasource override for CI/CD purposes.
 */
@SpringBootTest
@TestPropertySource(properties = {
        // Override datasource to prevent connection failure in environments without MySQL.
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DigitalSubsidyGrantPlatformApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context loads without errors.
    }
}
