package com.deskpilot.relay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username obrigatório")
    private String username;

    @NotBlank(message = "Senha obrigatória")
    private String password;

    // Opcional — só preenchido se o usuário tiver 2FA ativo
    private Integer totpCode;
}