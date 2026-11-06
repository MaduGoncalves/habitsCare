package com.example.habitscare;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class SecurityUtils {

    // Gera um novo hash com Salt automático usando BCrypt
    public static String hashSenha(String senha) {
        if (senha == null || senha.isEmpty()) {
            return null;
        }
        // cost 12 define a força/tempo do processamento contra brute force
        return BCrypt.withDefaults().hashToString(12, senha.toCharArray());
    }

    // Compara a senha informada em texto puro com o hash salvo no banco
    public static boolean verificarSenha(String senhaDigitada, String hashSalvo) {
        if (senhaDigitada == null || hashSalvo == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(senhaDigitada.toCharArray(), hashSalvo);
        return result.verified;
    }
}