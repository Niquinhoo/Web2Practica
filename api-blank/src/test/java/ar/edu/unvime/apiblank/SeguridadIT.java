package ar.edu.unvime.apiblank;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import ar.edu.unvime.apiblank.producto.ProductoClient;
import ar.edu.unvime.apiblank.seguridad.JwtService;
import ar.edu.unvime.apiblank.usuario.*;
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SeguridadIT {
    static String schema;
    @BeforeAll static void baseReal() throws Exception { schema = PostgresTestDatabase.nuevoEsquema(); }
    @AfterAll static void limpiar() throws Exception { if (schema != null) PostgresTestDatabase.borrar(schema); }
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry p) { PostgresTestDatabase.configurar(p, schema); }
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @MockitoBean ProductoClient productos;
    MockMvc mvc;
    final JsonMapper json = JsonMapper.builder().build();
    Cuenta uno, otro;
    record Cuenta(long id, String email, String token) {}
    @BeforeEach void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        uno = cuenta(); otro = cuenta();
    }
    String credenciales(String email, String password) {
        return json.writeValueAsString(Map.of("email", email, "password", password));
    }
    Cuenta cuenta() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        var registro = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(credenciales(email,"Una-clave-123"))).andExpect(status().isCreated()).andReturn();
        long id = json.readTree(registro.getResponse().getContentAsString()).get("id").asLong();
        return new Cuenta(id,email,login(email));
    }
    String login(String email) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credenciales(email,"Una-clave-123"))).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }
    long lista(Cuenta cuenta) throws Exception {
        var result = mvc.perform(post("/api/listas").header("Authorization","Bearer " + cuenta.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Propia\",\"usuarioId\":" + otro.id() + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(cuenta.id())).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    long favorito(Cuenta cuenta, long lista) throws Exception {
        var result = mvc.perform(post("/api/favoritos").header("Authorization","Bearer " + cuenta.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":1,\"listaId\":" + lista + "}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void registro_yLogin_guardanHashYNoExponenSecretos() throws Exception {
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuarios WHERE id=?", String.class, uno.id());
        assertThat(hash).isNotEqualTo("Una-clave-123");
        assertThat(passwords.matches("Una-clave-123",hash)).isTrue();
        String email = UUID.randomUUID()+"@example.com";
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+ email +"\",\"password\":\"Una-clave-123\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credenciales(uno.email(),"incorrecta")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credenciales("nadie@example.com","incorrecta")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credenciales(uno.email().toUpperCase(Locale.ROOT),"Una-clave-123")))
                .andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"email\":\"no-es-email\",\"password\":\"12345678\"}","{\"email\":\"a@example.com\",\"password\":\"corta\"}","null"})
    void registro_datosInvalidos_responde400(String body) throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"", "Bearer basura", "Basic abc", "Bearer a.b.c"})
    void acceder_tokenAusenteOInvalido_responde401(String auth) throws Exception {
        mvc.perform(get("/api/listas").header("Authorization",auth)).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer")).andExpect(jsonPath("$.status").value(401));
    }
    @Test void acceder_tokenVencidoOFirmaAjena_responde401() throws Exception {
        Usuario usuario = new Usuario(uno.id(),uno.email(),"no-exponer",Rol.USER);
        String vencido = new JwtService(encoder, Clock.fixed(Instant.now().minusSeconds(7200),ZoneOffset.UTC),30).emitir(usuario).token();
        var key = new SecretKeySpec("otra-clave-privada-de-al-menos-32-bytes-123456".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256");
        JwtEncoder ajeno = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        String firmaAjena = new JwtService(ajeno,Clock.systemUTC(),30).emitir(usuario).token();
        for (String token : List.of(vencido,firmaAjena))
            mvc.perform(get("/api/listas").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void listas_usuarioAjeno_noPuedeLeerBorrarNiMover() throws Exception {
        long propia=lista(uno), ajena=lista(otro);
        for (String ruta:List.of("/api/listas/"+ajena,"/api/listas/"+ajena+"/favoritos"))
            mvc.perform(get(ruta).header("Authorization","Bearer "+uno.token())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/listas/"+ajena).header("Authorization","Bearer "+uno.token())).andExpect(status().isForbidden());
        for (long[] par:List.of(new long[]{propia,ajena},new long[]{ajena,propia}))
            mvc.perform(post("/api/listas/"+par[0]+"/mover-favoritos").header("Authorization","Bearer "+uno.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"destinoId\":"+par[1]+"}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM listas WHERE id IN (?,?)",Long.class,propia,ajena)).isEqualTo(2);
        mvc.perform(get("/api/listas/"+propia).header("Authorization","Bearer "+uno.token())).andExpect(status().isOk());
        mvc.perform(get("/api/listas").header("Authorization","Bearer "+uno.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(propia));
    }
    @Test void favoritos_usuarioAjeno_noPuedeOperarPorIdNiUsarListaAjena() throws Exception {
        long propia=lista(uno), ajena=lista(otro), id=favorito(otro,ajena);
        String body="{\"productoId\":1,\"listaId\":"+propia+"}";
        mvc.perform(get("/api/favoritos/"+id).header("Authorization","Bearer "+uno.token())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/favoritos/"+id).header("Authorization","Bearer "+uno.token())).andExpect(status().isForbidden());
        mvc.perform(put("/api/favoritos/"+id).header("Authorization","Bearer "+uno.token()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/favoritos").header("Authorization","Bearer "+uno.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"listaId\":"+ajena+"}")).andExpect(status().isForbidden());
        long mio=favorito(uno,propia);
        mvc.perform(put("/api/favoritos/"+mio).header("Authorization","Bearer "+uno.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"listaId\":"+ajena+"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/favoritos").header("Authorization","Bearer "+uno.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(mio));
        assertThat(jdbc.queryForObject("SELECT lista_id FROM favoritos WHERE id=?",Long.class,mio)).isEqualTo(propia);
    }
    @Test void admin_listasAjenasYLegadas_accedeSinCambiarPropiedad() throws Exception {
        long ajena=lista(otro), favorito=favorito(otro,ajena);
        jdbc.update("UPDATE usuarios SET rol='ADMIN' WHERE id=?",uno.id());
        String admin=login(uno.email());
        long legado=jdbc.queryForObject("SELECT id FROM listas WHERE usuario_id IS NULL ORDER BY id LIMIT 1",Long.class);
        mvc.perform(get("/api/listas/"+legado).header("Authorization","Bearer "+otro.token())).andExpect(status().isForbidden());
        for (long id:List.of(ajena,legado)) mvc.perform(get("/api/listas/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isOk());
        mvc.perform(get("/api/favoritos/"+favorito).header("Authorization","Bearer "+admin)).andExpect(status().isOk());
        mvc.perform(delete("/api/favoritos/"+favorito).header("Authorization","Bearer "+admin)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/listas/"+ajena).header("Authorization","Bearer "+admin)).andExpect(status().isNoContent());
        int total=jdbc.queryForObject("SELECT count(*) FROM listas",Integer.class);
        mvc.perform(get("/api/listas").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(total));
    }
    @Test void rutasPublicas_yOpenApi_documentanBearerSinPedirToken() throws Exception {
        when(productos.listar()).thenReturn(List.of());
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/health")).andExpect(status().isOk());
        var result=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var spec=json.readTree(result.getResponse().getContentAsString());
        assertThat(spec.at("/components/securitySchemes/bearerAuth/scheme").asString()).isEqualTo("bearer");
        assertThat(spec.at("/paths/~1api~1listas/get/security/0").has("bearerAuth")).isTrue();
        assertThat(spec.at("/paths/~1api~1auth~1login").has("post")).isTrue();
        assertThat(spec.at("/paths/~1api~1productos/get").has("security")).isFalse();
    }
    @Test void registro_concurrente_unSoloUsuarioYConflicto() throws Exception {
        String email=UUID.randomUUID()+"@example.com", body=credenciales(email,"Una-clave-123");
        try (var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var salidas=pool.invokeAll(List.<Callable<Integer>>of(
                ()->mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus(),
                ()->mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus()));
            assertThat(List.of(salidas.get(0).get(),salidas.get(1).get())).containsExactlyInAnyOrder(201,409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE email=?",Integer.class,email)).isEqualTo(1);
    }
}
