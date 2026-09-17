CREATE TABLE favoritos (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    nota VARCHAR(500),
    fecha_alta TIMESTAMPTZ NOT NULL
);
