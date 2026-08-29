package com.nandinirai.customers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: the context wiring itself is a thing that can break, and this
 * fails fast and cheaply when it does.
 */
@SpringBootTest
class CustomerManagementApplicationTest {

    @Test
    @DisplayName("the application context loads")
    void contextLoads() {
    }
}
