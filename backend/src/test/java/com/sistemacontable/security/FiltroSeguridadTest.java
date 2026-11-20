package com.sistemacontable.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
import com.sistemacontable.security.VerificadorJwt.TokenInvalidoException;
import com.sistemacontable.boundary.AutenticacionResource;
import com.sistemacontable.boundary.ManejadorExcepciones;
import com.sistemacontable.control.service.AutenticacionService;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FiltroSeguridadTest {
    private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static HttpServer server;
    private static ECKey key;
    private static String base;

    @BeforeAll
    static void jwks() throws Exception {
        key = new ECKeyGenerator(Curve.P_256).keyID("api-key").generate();
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
        System.setProperty("supabase.url", base);
    }

    @AfterAll
    static void cerrar() {
        server.stop(0);
        System.clearProperty("supabase.url");
    }

    @Test
    void faltaPerfilDa403ConCodigoSinPerfil() throws Exception {
        PerfilDAO perfiles = mock(PerfilDAO.class);
        when(perfiles.buscar(ID)).thenReturn(Optional.empty());
        var salida = ejecutar(perfiles, "Bearer " + jwt(key), "protected", "GET", "restricted");
        assertEquals(403, salida.getStatus());
        assertEquals("SIN_PERFIL", ((com.sistemacontable.boundary.dto.ErrorDto) salida.getEntity()).codigo());
    }

    @Test
    void rolSinPermisoDa403ConCodigoProhibido() throws Exception {
        PerfilDAO perfiles = mock(PerfilDAO.class);
        when(perfiles.buscar(ID)).thenReturn(Optional.of(perfil(RolApp.Ventas)));
        var salida = ejecutar(perfiles, "Bearer " + jwt(key), "protected", "GET", "restricted");
        assertEquals(403, salida.getStatus());
        assertEquals("PROHIBIDO", ((com.sistemacontable.boundary.dto.ErrorDto) salida.getEntity()).codigo());
    }

    @Test
    void usuarioSinPermisoConfiguradoQuedaDenegadoPorDefecto() throws Exception {
        PerfilDAO perfiles = mock(PerfilDAO.class);
        when(perfiles.buscar(ID)).thenReturn(Optional.of(perfil(RolApp.Bodega)));
        var salida = ejecutar(perfiles, "Bearer " + jwt(key), "protected", "GET", "unannotated");
        assertEquals(403, salida.getStatus());
        assertEquals("PROHIBIDO", ((com.sistemacontable.boundary.dto.ErrorDto) salida.getEntity()).codigo());
    }

    @Test
    void tokenMaloDa401YPreflightNoExigeToken() throws Exception {
        PerfilDAO perfiles = mock(PerfilDAO.class);
        var salida = ejecutar(perfiles, "Bearer esto-no-es-un-jwt", "protected", "GET", "restricted");
        assertEquals(401, salida.getStatus());
        assertEquals("NO_AUTENTICADO", ((com.sistemacontable.boundary.dto.ErrorDto) salida.getEntity()).codigo());
        var preflight = ejecutar(perfiles, null, "protected", "OPTIONS", "restricted");
        assertNull(preflight);
        verifyNoInteractions(perfiles);
    }

    @Test
    void getMeSinTokenResponde401DesdeServidorJaxRs() throws Exception {
        JerseyTest servidor = new JerseyTest(new ResourceConfig()
                .register(AutenticacionResource.class)
                .register(FiltroSeguridad.class)
                .register(ManejadorExcepciones.class)
                .register(new AbstractBinder() {
                    @Override protected void configure() {
                        bind(mock(AutenticacionService.class)).to(AutenticacionService.class);
                        bind(mock(PerfilDAO.class)).to(PerfilDAO.class);
                        bind(new UsuarioActual()).to(UsuarioActual.class);
                    }
                })) { };
        servidor.setUp();
        try (Response response = servidor.target("me").request().get()) {
            assertEquals(401, response.getStatus());
            assertTrue(response.readEntity(String.class).contains("NO_AUTENTICADO"));
        } finally {
            servidor.tearDown();
        }
    }

    private static Response ejecutar(PerfilDAO perfiles, String authorization, String path, String method,
                                      String resourceMethodName) throws Exception {
        FiltroSeguridad filtro = new FiltroSeguridad();
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        Class<?> testClass = EndpointPermisos.class;
        Method endpoint = resourceMethodName.equals("unannotated")
                ? testClass.getMethod("unannotated") : testClass.getMethod("restricted");
        when(resourceInfo.getResourceMethod()).thenReturn(endpoint);
        doReturn(testClass).when(resourceInfo).getResourceClass();
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uri = mock(UriInfo.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getUriInfo()).thenReturn(uri);
        when(uri.getPath()).thenReturn(path);
        when(request.getHeaderString("Authorization")).thenReturn(authorization);
        set(filtro, "resourceInfo", resourceInfo);
        set(filtro, "perfiles", perfiles);
        set(filtro, "usuario", new UsuarioActual());
        filtro.filter(request);
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(request, method.equals("OPTIONS") ? never() : (authorization == null ? times(1) : times(1)))
                .abortWith(captor.capture());
        if (method.equals("OPTIONS")) return null;
        return captor.getValue();
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Perfil perfil(RolApp rol) {
        Perfil p = new Perfil();
        p.setId(ID);
        p.setRol(rol);
        p.setNombre("Ana");
        p.setActivo(true);
        return p;
    }

    private static String jwt(ECKey signerKey) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(base + "/auth/v1").subject(ID.toString())
                .audience("authenticated").expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("email", "ana@example.com").build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("api-key").build(), claims);
        jwt.sign(new ECDSASigner(signerKey));
        return jwt.serialize();
    }

    static class EndpointPermisos {
        @Roles({RolApp.Bodega}) public void restricted() { }
        public void unannotated() { }
    }
}
