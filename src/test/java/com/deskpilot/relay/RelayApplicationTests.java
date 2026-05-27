package com.deskpilot.relay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-testing-purposes-only-32chars",
        "jwt.expiration-ms=900000",
        "jwt.refresh-expiration-ms=604800000",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
class RelayApplicationTests {

    @Test
    void contextLoads() {
    }
}