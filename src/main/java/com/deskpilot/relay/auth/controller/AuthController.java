package com.deskpilot.relay.auth.controller;

import com.deskpilot.relay.auth.dto.AuthResponse;
import com.deskpilot.relay.auth.dto.LoginRequest;
import com.deskpilot.relay.auth.dto.RegisterRequest;
import com.deskpilot.relay.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ─── POST /auth/register ─────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    // ─── POST /auth/login ────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─── POST /auth/refresh ──────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody Map<String, String> body
    ) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    // ─── POST /auth/2fa/enable ───────────────────────────────────────────────────

    @PostMapping("/2fa/enable")
    public ResponseEntity<Void> enableTotp(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Integer> body
    ) {
        Integer code = body.get("code");
        if (code == null) {
            return ResponseEntity.badRequest().build();
        }
        authService.enableTotp(userDetails.getUsername(), code);
        return ResponseEntity.ok().build();
    }

    // ─── GET /auth/me ────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(Map.of("username", userDetails.getUsername()));
    }
}