package ar.edu.unvime.apiblank.error;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

public record ApiError(
        @Schema(example = "400") int status,
        @Schema(example = "Hay datos inválidos") String mensaje,
        @Schema(description = "Detalle por campo; vacío si el error no corresponde a campos")
        Map<String, String> campos) {}
