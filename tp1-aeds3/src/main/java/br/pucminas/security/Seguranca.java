package br.pucminas.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

public final class Seguranca {
    private Seguranca() {
    }

    public static String hashSenha(String senha) {
        return sha256(senha == null ? "" : senha);
    }

    public static String hashResposta(String resposta) {
        String normalizada = Normalizer.normalize(resposta == null ? "" : resposta,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return sha256(normalizada);
    }

    public static boolean corresponde(String valor, String hash) {
        return sha256(valor == null ? "" : valor).equals(hash);
    }

    private static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não está disponível", e);
        }
    }
}