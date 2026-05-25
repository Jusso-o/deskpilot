package com.deskpilot.relay.auth.service;

import com.deskpilot.relay.auth.dto.AuthResponse;
import com.deskpilot.relay.auth.dto.LoginRequest;
import com.deskpilot.relay.auth.dto.RegisterRequest;
import com.deskpilot.relay.security.jwt.JwtService;
import com.deskpilot.relay.security.totp.TotpService;
import com.deskpilot.relay.user.model.Role;
import com.deskpilot.relay.user.model.User;
import com.deskpilot.relay.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository       userRepository;
    private final JwtService           jwtService;
    private final TotpService          totpService;
    private final PasswordEncoder      passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            TotpService totpService,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository       = userRepository;
        this.jwtService           = jwtService;
        this.totpService          = totpService;
        this.passwordEncoder      = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    // ─── Registro ────────────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username já está em uso");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        // Gera secret TOTP — o usuário pode ativar depois
        String totpSecret = totpService.generateSecret();

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .totpSecret(totpSecret)
                .totpEnabled(false)
                .role(Role.USER)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user.getUsername()))
                .refreshToken(jwtService.generateRefreshToken(user.getUsername()))
                .username(user.getUsername())
                .totpEnabled(false)
                .totpSecret(totpSecret)
                .totpQrUri(totpService.generateOtpAuthUri(
                        totpSecret, user.getEmail(), "DeskPilot"))
                .build();
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {

        // Valida username e senha via Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        // Se 2FA ativo, valida o código TOTP
        if (user.isTotpEnabled()) {
            if (request.getTotpCode() == null) {
                throw new BadCredentialsException("Código 2FA obrigatório");
            }
            if (!totpService.verify(user.getTotpSecret(), request.getTotpCode())) {
                throw new BadCredentialsException("Código 2FA inválido");
            }
        }

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user.getUsername()))
                .refreshToken(jwtService.generateRefreshToken(user.getUsername()))
                .username(user.getUsername())
                .totpEnabled(user.isTotpEnabled())
                .build();
    }

    // ─── Refresh Token ───────────────────────────────────────────────────────────

    public AuthResponse refresh(String refreshToken) {

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Token inválido");
        }

        String username = jwtService.extractUsername(refreshToken);

        userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        if (!jwtService.isTokenValid(refreshToken, username)) {
            throw new BadCredentialsException("Token expirado");
        }

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(username))
                .refreshToken(jwtService.generateRefreshToken(username))
                .username(username)
                .build();
    }

    // ─── Ativar 2FA ──────────────────────────────────────────────────────────────

    public void enableTotp(String username, int totpCode) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!totpService.verify(user.getTotpSecret(), totpCode)) {
            throw new BadCredentialsException("Código 2FA inválido — tente novamente");
        }

        user.setTotpEnabled(true);
        userRepository.save(user);
    }
}