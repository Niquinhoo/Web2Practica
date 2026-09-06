package ar.edu.unvime.apiblank.producto;

import static org.assertj.core.api.Assertions.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import ar.edu.unvime.apiblank.error.ServicioExternoException;

class DummyJsonProductoClientTests {
    private HttpServer server;
    private ExecutorService executor;
    private DummyJsonProductoClient client;
    private volatile int status = 200;
    private volatile String body = "";
    private volatile long delay;
    private final AtomicReference<String> uri = new AtomicReference<>();
    private static final String PRODUCTO = """
            {"id":1,"title":"Prueba","description":"Descripción","price":9.99,"category":"beauty","reviews":[]}
            """;

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            uri.set(exchange.getRequestURI().toString());
            try (exchange) {
                if (delay > 0) {
                    try { Thread.sleep(delay); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); return; }
                }
                var bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        client = crearClient(Duration.ofSeconds(3));
    }

    private DummyJsonProductoClient crearClient(Duration timeout) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1)).build());
        factory.setReadTimeout(timeout);
        return new DummyJsonProductoClient(RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).requestFactory(factory).build());
    }

    @AfterEach
    void cerrar() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void consultaCatalogoCompletoYDeserializa() {
        body = "{\"products\":[" + PRODUCTO + "],\"total\":1,\"skip\":0,\"limit\":1}";
        assertThat(client.listar()).singleElement().satisfies(p -> {
            assertThat(p.title()).isEqualTo("Prueba");
            assertThat(p.price()).isEqualByComparingTo("9.99");
        });
        assertThat(uri.get()).isEqualTo("/products?limit=0");
    }

    @Test
    void consultaDetalleYTraduce404() {
        body = PRODUCTO;
        assertThat(client.obtener(1L)).isPresent();
        assertThat(uri.get()).isEqualTo("/products/1");
        status = 404;
        body = "{\"message\":\"not found\"}";
        assertThat(client.obtener(99L)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 429, 500, 503})
    void erroresHttpDelProveedorSonFallasExternas(int codigo) {
        status = codigo;
        body = "{\"message\":\"detalle externo\"}";
        assertThatThrownBy(client::listar).isInstanceOf(ServicioExternoException.class);
        assertThatThrownBy(() -> client.obtener(1L)).isInstanceOf(ServicioExternoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "{}", "{\"products\":null}", "{\"products\":[null]}", "{\"products\":[{\"id\":1}]}"})
    void rechazaCatalogosInvalidos(String json) {
        body = json;
        assertThatThrownBy(client::listar).isInstanceOf(ServicioExternoException.class);
    }

    @Test
    void aceptaListaVaciaPeroNoProductoInvalido() {
        body = "{\"products\":[]}";
        assertThat(client.listar()).isEmpty();
        body = "{}";
        assertThatThrownBy(() -> client.obtener(1L)).isInstanceOf(ServicioExternoException.class);
        body = PRODUCTO;
        assertThatThrownBy(() -> client.obtener(2L)).isInstanceOf(ServicioExternoException.class);
    }

    @Test
    void timeoutRealSeTraduceAExcepcionPropia() {
        delay = 1500;
        body = PRODUCTO;
        var lento = crearClient(Duration.ofMillis(150));
        assertThatThrownBy(() -> lento.obtener(1L)).isInstanceOf(ServicioExternoException.class);
    }
}
