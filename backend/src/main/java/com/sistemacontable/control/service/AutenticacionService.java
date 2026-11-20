package com.sistemacontable.control.service;

import com.sistemacontable.boundary.dto.AutenticacionDto.ActualizarUsuario;
import com.sistemacontable.boundary.dto.AutenticacionDto.CrearUsuario;
import com.sistemacontable.boundary.dto.AutenticacionDto.EmpleadoMini;
import com.sistemacontable.boundary.dto.AutenticacionDto.Me;
import com.sistemacontable.boundary.dto.AutenticacionDto.Usuario;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.security.Perfil;
import com.sistemacontable.security.PerfilDAO;
import com.sistemacontable.security.UsuarioActual;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso de sesión y administración de usuarios de Supabase Auth. */
@Stateless
@LocalBean
public class AutenticacionService {
    private static final String SUPABASE_URL_DEFAULT = "https://xyukidsdmihmsmlpddqz.supabase.co";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Inject private PerfilDAO perfiles;
    @Inject private UsuarioActual actual;

    public Me me() {
        Perfil p = perfiles.requerido(actual.getId());
        if (p == null || !p.isActivo()) throw new com.sistemacontable.security.PerfilNoActivoException();
        EmpleadoMini empleado = p.getEmpleado() == null ? null
                : new EmpleadoMini(p.getEmpleado().getId(), p.getEmpleado().getNombre(), p.getEmpleado().getApellido());
        return new Me(p.getId(), actual.getEmail(), p.getRol(), p.getNombre(), p.getIdEmpleado(), empleado);
    }

    public List<Usuario> listarUsuarios() {
        Map<String, Map<String, Object>> auth = usuariosAuth();
        List<Usuario> resultado = new ArrayList<>();
        for (Perfil p : perfiles.listar()) {
            Map<String, Object> u = auth.get(p.getId().toString());
            resultado.add(new Usuario(p.getId(), u == null ? null : texto(u.get("email")), p.getRol(), p.getNombre(),
                    p.getIdEmpleado(), p.isActivo(), u == null ? null : texto(u.get("created_at"))));
        }
        return resultado;
    }

    public Usuario crearUsuario(CrearUsuario cuerpo) {
        validarEmpleado(cuerpo.idEmpleado());
        Map<String, Object> creado;
        try {
            creado = admin("POST", "/admin/users", Map.of("email", cuerpo.email(), "password", cuerpo.password(),
                    "email_confirm", true));
        } catch (RuntimeException e) {
            throw new AuthAdminException("No se pudo crear el usuario en Auth", e);
        }
        UUID id;
        try { id = UUID.fromString(String.valueOf(creado.get("id"))); }
        catch (RuntimeException e) { throw new AuthAdminException("Auth devolvio una respuesta invalida", e); }
        Perfil p = new Perfil();
        p.setId(id);
        p.setRol(cuerpo.rol());
        p.setNombre(cuerpo.nombre());
        p.setIdEmpleado(cuerpo.idEmpleado());
        p.setActivo(true);
        try {
            perfiles.guardar(p);
        } catch (RuntimeException errorPersistiendoPerfil) {
            try { admin("DELETE", "/admin/users/" + id, null); }
            catch (RuntimeException ignorado) { /* La transaccion debe revertir por el error original. */ }
            throw errorPersistiendoPerfil;
        }
        return new Usuario(id, cuerpo.email(), p.getRol(), p.getNombre(), p.getIdEmpleado(), true,
                texto(creado.get("created_at")));
    }

    public void actualizarUsuario(UUID id, ActualizarUsuario cuerpo) {
        validarEmpleado(cuerpo.idEmpleado());
        Perfil p = perfiles.requerido(id);
        if (p == null) throw new com.sistemacontable.control.exception.NoEncontradoException("No existe el perfil solicitado");
        p.setRol(cuerpo.rol());
        p.setNombre(cuerpo.nombre());
        p.setIdEmpleado(cuerpo.idEmpleado());
        p.setActivo(cuerpo.activo());
        perfiles.modificar(p);
    }

    private void validarEmpleado(Long idEmpleado) {
        if (idEmpleado != null && !perfiles.existeEmpleado(idEmpleado)) {
            throw new ReglaNegocioException("No existe el empleado vinculado al perfil");
        }
    }

    private Map<String, Map<String, Object>> usuariosAuth() {
        Map<String, Map<String, Object>> resultado = new HashMap<>();
        for (int page = 1; page <= 20; page++) {
            Map<String, Object> payload = admin("GET", "/admin/users?page=" + page + "&per_page=1000", null);
            Object rows = payload.get("users");
            if (!(rows instanceof List<?> users)) break;
            for (Object row : users) {
                if (row instanceof Map<?, ?> user && user.get("id") != null) {
                    @SuppressWarnings("unchecked") Map<String, Object> typed = (Map<String, Object>) user;
                    resultado.put(String.valueOf(user.get("id")), typed);
                }
            }
            if (users.size() < 1000) break;
        }
        return resultado;
    }

    private Map<String, Object> admin(String method, String path, Map<String, Object> body) {
        String base = System.getenv("SUPABASE_URL");
        if (base == null || base.isBlank()) base = SUPABASE_URL_DEFAULT;
        String key = System.getenv("SUPABASE_SERVICE_ROLE_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Falta configuracion SUPABASE_SERVICE_ROLE_KEY");
        String url = base.replaceAll("/+$", "") + "/auth/v1" + path;
        try (Jsonb jsonb = JsonbBuilder.create()) {
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                    .header("apikey", key).header("Authorization", "Bearer " + key).header("Accept", "application/json");
            if (body == null && method.equals("DELETE")) req.DELETE();
            else if (body == null) req.GET();
            else req.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(jsonb.toJson(body), StandardCharsets.UTF_8));
            HttpResponse<String> response = HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Auth Admin API fallo");
            if (response.body() == null || response.body().isBlank()) return Map.of();
            @SuppressWarnings("unchecked") Map<String, Object> result = jsonb.fromJson(response.body(), Map.class);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Auth Admin API no disponible", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Auth Admin API interrumpida", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Respuesta invalida de Auth Admin API", e);
        }
    }

    private String texto(Object value) { return value == null ? null : String.valueOf(value); }

    public static class AuthAdminException extends RuntimeException {
        public AuthAdminException(String mensaje, Throwable causa) { super(mensaje, causa); }
    }
}
