package com.reactive.nexo.test.controller;

import com.reactive.nexo.dto.LoginRequest;
import com.reactive.nexo.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Slf4j
public class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Test that JWT generation works
     */
    @Test
    public void testJwtGeneration() {
        String token = jwtUtil.generateToken(1, "Test User", "ADMIN", java.util.Arrays.asList("read", "write"));
        
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        
        Integer userId = jwtUtil.extractUserId(token);
        assertEquals(1, userId);
        
        String username = jwtUtil.extractUsername(token);
        assertEquals("Test User", username);
    }

    /**
     * Test that invalid tokens are rejected
     */
    @Test
    public void testInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertFalse(jwtUtil.validateToken(invalidToken));
        assertNull(jwtUtil.extractUserId(invalidToken));
    }

    /**
     * Test login endpoint structure (without actual backend)
     */
    @Test
    public void testLoginEndpointExists() {
        LoginRequest request = new LoginRequest("CC", "12345", "password");
        
        // This will fail because employees module is not running, but we verify endpoint exists
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    /**
     * Test logout endpoint
     */
    @Test
    public void testLogoutEndpoint() {
        webTestClient.post()
                .uri("/api/v1/auth/logout")
                .exchange()
                .expectStatus()
                .isOk();
    }
}
