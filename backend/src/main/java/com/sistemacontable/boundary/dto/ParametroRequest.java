package com.sistemacontable.boundary.dto;
import jakarta.validation.constraints.NotBlank;
public record ParametroRequest(@NotBlank String valor) {}
