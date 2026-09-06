package ar.edu.unvime.apiblank.favorito;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class FavoritoRepositoryMemoriaTests {
    private final FavoritoRepositoryMemoria repository = new FavoritoRepositoryMemoria();
    private final Instant fecha = Instant.parse("2026-09-06T12:00:00Z");

    @Test
    void crudConservaFechaYNoReutilizaIds() {
        var creado = repository.crear(3L, "Primera", fecha);
        assertThat(repository.buscarPorId(creado.id())).contains(creado);
        var actualizado = repository.actualizar(creado.id(), 4L, null).orElseThrow();
        assertThat(actualizado).isEqualTo(new Favorito(creado.id(), 4L, null, fecha));
        assertThat(repository.eliminar(creado.id())).isTrue();
        assertThat(repository.eliminar(creado.id())).isFalse();
        assertThat(repository.buscarPorId(creado.id())).isEmpty();
        assertThat(repository.actualizar(creado.id(), 5L, "No recrear")).isEmpty();
        assertThat(repository.crear(3L, null, fecha).id()).isGreaterThan(creado.id());
    }

    @Test
    void listaEsUnaCopiaInmutableOrdenada() {
        var primero = repository.crear(1L, null, fecha);
        var copia = repository.buscarTodos();
        var segundo = repository.crear(2L, null, fecha);
        assertThat(copia).containsExactly(primero);
        assertThatThrownBy(copia::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(repository.buscarTodos()).containsExactly(primero, segundo);
    }

    @Test
    void creacionesConcurrentesTienenIdsUnicos() {
        IntStream.range(0, 500).parallel().forEach(i -> repository.crear(1L, null, fecha));
        assertThat(repository.buscarTodos()).hasSize(500)
                .extracting(Favorito::id).doesNotHaveDuplicates().isSorted();
    }
}
