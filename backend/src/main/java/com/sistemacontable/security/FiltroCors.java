package com.sistemacontable.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class FiltroCors implements ContainerResponseFilter {
    private static final Set<String> METODOS = Set.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String origin = request.getHeaderString("Origin");
        if (origin != null && origenes().contains(origin)) {
            response.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
            response.getHeaders().putSingle("Vary", "Origin");
            response.getHeaders().putSingle("Access-Control-Allow-Methods", String.join(",", METODOS));
            response.getHeaders().putSingle("Access-Control-Allow-Headers", "Authorization,Content-Type");
            response.getHeaders().putSingle("Access-Control-Expose-Headers", "Location,Total-Records");
        }
    }

    private Set<String> origenes() {
        String cfg = System.getenv().getOrDefault("CORS_ORIGINS", "http://localhost:9000");
        return Arrays.stream(cfg.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }
}
