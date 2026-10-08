package ar.edu.unvime.apiblank.lista;

import io.swagger.v3.oas.annotations.media.Schema;

public record ListaResponse(@Schema(example = "1") Long id, @Schema(example = "Regalos") String nombre) {}
