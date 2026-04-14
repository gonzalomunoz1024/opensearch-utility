package com.opensearch.utility.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.opensearch.testcontainers.OpensearchContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    static final OpensearchContainer<?> opensearchContainer =
            new OpensearchContainer<>("opensearchproject/opensearch:2.11.0")
                    .withSecurityEnabled();

    @BeforeAll
    static void startContainers() {
        opensearchContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("opensearch.cluster.url", opensearchContainer::getHttpHostAddress);
        registry.add("opensearch.cluster.username", () -> "admin");
        registry.add("opensearch.cluster.password", () -> "admin");
    }
}
