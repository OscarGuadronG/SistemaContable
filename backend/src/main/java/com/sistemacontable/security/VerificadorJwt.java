package com.sistemacontable.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Verifica ES256, issuer, audiencia y expiración usando JWKS remoto cacheado. */
public final class VerificadorJwt {
    private static final int TOLERANCIA_SEGUNDOS = 60;
    private static final String SUPABASE_URL_DEFAULT = "https://xyukidsdmihmsmlpddqz.supabase.co";
    private static final Map<String, JWTProcessor<SecurityContext>> CACHE = new ConcurrentHashMap<>();

    public record Identidad(String sub, String email) { }

    public Identidad verificar(String token) {
        String base = System.getProperty("supabase.url");
        if (base == null || base.isBlank()) base = System.getenv("SUPABASE_URL");
        if (base == null || base.isBlank()) base = SUPABASE_URL_DEFAULT;
        return verificar(token, base);
    }

    Identidad verificar(String token, String base) {
        String issuer = quitarSlash(base) + "/auth/v1";
        try {
            JWTClaimsSet claims = procesador(issuer).process(SignedJWT.parse(token), null);
            if (!issuer.equals(claims.getIssuer()) || !audienciaValida(claims.getAudience())) {
                throw new TokenInvalidoException();
            }
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now().minusSeconds(TOLERANCIA_SEGUNDOS))) {
                throw new TokenInvalidoException();
            }
            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) throw new TokenInvalidoException();
            return new Identidad(sub, claims.getStringClaim("email"));
        } catch (TokenInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenInvalidoException();
        }
    }

    private JWTProcessor<SecurityContext> procesador(String issuer) throws MalformedURLException {
        return CACHE.computeIfAbsent(issuer, key -> {
            try {
                ResourceRetriever retriever = new DefaultResourceRetriever(2000, 3000, 1024 * 1024);
                JWKSource<SecurityContext> source = new RemoteJWKSet<>(
                        new URL(key + "/.well-known/jwks.json"), retriever);
                DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
                processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, source));
                return processor;
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo configurar JWKS", e);
            }
        });
    }

    private boolean audienciaValida(List<String> audiencia) {
        return audiencia != null && audiencia.contains("authenticated");
    }

    private static String quitarSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    public static final class TokenInvalidoException extends RuntimeException { }
}
