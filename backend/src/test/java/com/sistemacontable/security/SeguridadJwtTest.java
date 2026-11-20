package com.sistemacontable.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sistemacontable.security.VerificadorJwt.Identidad;
import com.sistemacontable.security.VerificadorJwt.TokenInvalidoException;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.ForbiddenException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SeguridadJwtTest {
    private static HttpServer server;
    private static ECKey key;
    private static String base;
    private final VerificadorJwt verificador = new VerificadorJwt();

    @BeforeAll
    static void iniciarJwks() throws Exception {
        key = new ECKeyGenerator(Curve.P_256).keyID("correcta").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/auth/v1/.well-known/jwks.json", exchange -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void cerrarJwks() { server.stop(0); }

    @Test
    void aceptaJwtEs256ValidoConClaimsSupabase() throws Exception {
        Identidad identidad = verificador.verificar(token(key, "correcta", Instant.now().plusSeconds(300)), base);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", identidad.sub());
        assertEquals("ana@example.com", identidad.email());
    }

    @Test
    void rechazaTokenExpirado() throws Exception {
        assertThrows(TokenInvalidoException.class,
                () -> verificador.verificar(token(key, "correcta", Instant.now().minusSeconds(120)), base));
    }

    @Test
    void rechazaFirmaDeOtraClaveAunqueElTokenDeclareUnKid() throws Exception {
        ECKey otra = new ECKeyGenerator(Curve.P_256).keyID("incorrecta").generate();
        assertThrows(TokenInvalidoException.class,
                () -> verificador.verificar(token(otra, "incorrecta", Instant.now().plusSeconds(300)), base));
    }

    @Test
    void creditoPermiteSoloElRolCorrespondienteYElAdministrador() {
        UsuarioActual usuario = new UsuarioActual();
        Perfil perfil = new Perfil();
        perfil.setId(UUID.randomUUID());
        perfil.setIdEmpleado(54L);
        perfil.setNombre("Ana");
        perfil.setRol(RolApp.Ventas);
        perfil.setActivo(true);
        usuario.establecer(perfil, "ana@example.com");
        assertEquals(54L, usuario.getIdEmpleado());
        assertDoesNotThrow(() -> usuario.exigirCredito(true));
        assertThrows(ForbiddenException.class, () -> usuario.exigirCredito(false));
        perfil.setRol(RolApp.Contabilidad);
        usuario.establecer(perfil, "ana@example.com");
        assertDoesNotThrow(() -> usuario.exigirCredito(false));
        perfil.setRol(RolApp.Administrador);
        usuario.establecer(perfil, "ana@example.com");
        assertDoesNotThrow(() -> usuario.exigirCredito(false));
    }

    private static String token(ECKey signerKey, String kid, Instant exp) throws Exception {
        String issuer = base + "/auth/v1";
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).subject("123e4567-e89b-12d3-a456-426614174000")
                .audience("authenticated").expirationTime(Date.from(exp)).issueTime(new Date())
                .claim("email", "ana@example.com").build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid).build(), claims);
        jwt.sign(new ECDSASigner(signerKey));
        return jwt.serialize();
    }
}
