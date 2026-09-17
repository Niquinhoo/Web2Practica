package ar.edu.unvime.apiblank.favorito;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FavoritoRequest(
        @Schema(description = "Referencia a un producto externo; no se consulta al proveedor al guardar", example = "1")
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0") Long productoId,
        @Schema(description = "Nota opcional, hasta 500 caracteres; PUT la reemplaza", example = "Comprar para regalar")
        @Size(max = 500, message = "no debe superar los 500 caracteres") String nota,
        @Schema(description = "Lista existente a la que pertenece", example = "1")
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0") Long listaId) {}
