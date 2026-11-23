-- La línea base (US45) la declara cualquier dueño autenticado, también el que acaba de registrarse: Catalog solo
-- tiene la copia local del negocio (merchant_standings) después de un evento posterior, así que la clave foránea
-- rechazaba la declaración. business_id sigue siendo la clave primaria y sale siempre del token.
ALTER TABLE business_baselines DROP CONSTRAINT fk_business_baselines_business_id;
