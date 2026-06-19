package com.dlight.payments;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Container is started exactly once via the static initializer and never explicitly stopped
 * (Ryuk reaps it at JVM exit). Avoiding the {@code @Testcontainers}/{@code @Container} JUnit5
 * extension lifecycle here, since across multiple test classes it was observed to restart the
 * "shared" static container, leaving later test classes pointed at a stale, no-longer-running
 * port via the previously resolved {@code @ServiceConnection} details.
 */
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
