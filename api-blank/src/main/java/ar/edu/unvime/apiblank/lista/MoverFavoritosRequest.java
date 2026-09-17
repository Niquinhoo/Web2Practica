package ar.edu.unvime.apiblank.lista;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MoverFavoritosRequest(
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0") Long destinoId) {}
