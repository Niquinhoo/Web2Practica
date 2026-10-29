-- Las listas previas se conservan sin dueño y quedan accesibles solo para ADMIN.
ALTER TABLE listas ADD COLUMN usuario_id BIGINT REFERENCES usuarios(id);
CREATE INDEX idx_listas_usuario_id ON listas(usuario_id);
