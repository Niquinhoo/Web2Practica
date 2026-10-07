package ar.edu.unvime.apiblank.lista;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MoverFavoritosRequest(
        @Schema(description = "Lista existente que recibirá los favoritos", example = "2")
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor que 0") Long destinoId) {}
