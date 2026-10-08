package ar.edu.unvime.apiblank.lista;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ListaRequest(
        @Schema(example = "Regalos")
        @NotBlank(message = "es obligatorio y no puede estar vacío")
        @Size(max = 100, message = "no debe superar los 100 caracteres") String nombre) {}
