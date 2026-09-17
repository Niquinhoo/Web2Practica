ALTER TABLE favoritos ADD COLUMN lista_id BIGINT;
ALTER TABLE favoritos ADD CONSTRAINT fk_favoritos_lista
    FOREIGN KEY (lista_id) REFERENCES listas(id);
CREATE INDEX idx_favoritos_lista_id ON favoritos (lista_id);
