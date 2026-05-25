package com.deskpilot.relay.security.totp;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class TotpService {

    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP   = 30;
    private static final int WINDOW      = 1;

    // ─── Geração de secret ───────────────────────────────────────────────────────

    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return new Base32().encodeToString(buffer);
    }

    // ─── Verificação ─────────────────────────────────────────────────────────────

    public boolean verify(String secret, int code) {
        long currentStep = Instant.now().getEpochSecond() / TIME_STEP;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            if (generateCode(secret, currentStep + i) == code) return true;
        }
        return false;
    }

    // Sobrecarga para aceitar Integer (pode vir null do LoginRequest)
    public boolean verify(String secret, Integer code) {
        if (code == null) return false;
        return verify(secret, code.intValue());
    }

    // ─── QR Code URI ─────────────────────────────────────────────────────────────

    public String generateOtpAuthUri(String secret, String account, String issuer) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                issuer, account, secret, issuer, CODE_DIGITS, TIME_STEP
        );
    }

    // ─── Algoritmo TOTP (RFC 6238) ───────────────────────────────────────────────

    private int generateCode(String secret, long timeStep) {
        try {
            byte[] key  = new Base32().decode(secret);
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (timeStep & 0xFF);
                timeStep >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash   = mac.doFinal(data);
            int    offset = hash[hash.length - 1] & 0x0F;
            int    binary = ((hash[offset]     & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) <<  8)
                    |  (hash[offset + 3] & 0xFF);
            return binary % (int) Math.pow(10, CODE_DIGITS);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar código TOTP", e);
        }
    }
}