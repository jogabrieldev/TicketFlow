package com.ticketApi.shared.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String gerar(String representacaoCanonica) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(representacaoCanonica.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 não está disponível", excecao);
        }
    }
}
