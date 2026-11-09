-- Una fila por carga del padrón: la fecha y el número de filas se leen de aquí y no con max() sobre
-- sunat_taxpayers. row_count sirve además para rechazar un archivo parcial (menos del 90 % de la carga anterior).
CREATE TABLE sunat_registry_loads (
    id bigserial NOT NULL,
    loaded_at timestamptz NOT NULL,
    row_count bigint NOT NULL,
    source varchar(255) NOT NULL,
    CONSTRAINT pk_sunat_registry_loads PRIMARY KEY (id)
);
