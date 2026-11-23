-- Precio normal opcional de la oferta: la web muestra el ahorro tachando este precio junto al precio de la oferta.
-- Las ofertas existentes quedan sin precio normal.
ALTER TABLE offers ADD COLUMN regular_price numeric(10,2);
