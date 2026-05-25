package com.deskpilot.relay.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String username;
    private boolean totpEnabled;
    private String totpSecret;   // só retornado no primeiro setup do 2FA
    private String totpQrUri;    // URI para gerar o QR code no app
}