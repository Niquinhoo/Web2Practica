package ar.edu.unvime.apiblank.favorito;

import java.time.Instant;

public record Favorito(Long id, Long productoId, String nota, Instant fechaAgregado, Long listaId) {}
