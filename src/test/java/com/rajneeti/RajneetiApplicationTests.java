package com.rajneeti;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test – verifies the Spring application context loads without errors.
 *
 * <p>Uses {@code @TestPropertySource} to override the datasource with an
 * in-memory H2 database so the context test does not require a running MySQL
 * instance in CI/CD.
 *
 * <p>TODO: Add H2 test dependency and enable these properties once the project
 * has entity classes.  For Module 01, the context test passes because
 * {@code spring.jpa.hibernate.ddl-auto=create-drop} is overridden to {@code none}
 * and no entity scanning occurs at startup.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaG1hYy1zaGEyNTY=",
        "logging.level.root=WARN"
})
class RajneetiApplicationTests {

    @Test
    void contextLoads() {
        // If the application context starts without throwing, this test passes.
    }
}
