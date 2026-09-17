INSERT INTO listas (nombre)
SELECT 'Sin clasificar'
WHERE NOT EXISTS (SELECT 1 FROM listas WHERE nombre = 'Sin clasificar');

UPDATE favoritos
SET lista_id = (SELECT id FROM listas WHERE nombre = 'Sin clasificar' ORDER BY id LIMIT 1)
WHERE lista_id IS NULL;

ALTER TABLE favoritos ALTER COLUMN lista_id SET NOT NULL;
