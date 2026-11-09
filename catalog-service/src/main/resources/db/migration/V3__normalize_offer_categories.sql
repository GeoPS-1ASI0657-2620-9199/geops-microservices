-- US08 y E2-06: las categorías pasan a una lista cerrada. Los textos libres se llevan a la lista
-- con las mismas palabras clave que usaba la web para elegir el ícono (category-icon.ts), en el
-- mismo orden; lo que no coincide queda en "Servicios".
UPDATE offers
SET category = CASE
    WHEN category IN ('Gastronomía', 'Cafetería y panadería', 'Bodega y minimarket', 'Salud y farmacia',
                      'Belleza y cuidado personal', 'Deporte', 'Mascotas', 'Hogar y ferretería',
                      'Librería y útiles', 'Servicios', 'Cultura y entretenimiento') THEN category
    WHEN category ~* 'caf[eé]' THEN 'Cafetería y panadería'
    WHEN category ~* 'pan|pastel|torta|postre' THEN 'Cafetería y panadería'
    WHEN category ~* 'belleza|barber|corte|spa|u[ñn]as' THEN 'Belleza y cuidado personal'
    WHEN category ~* 'salud|botica|farmacia|[óo]ptica' THEN 'Salud y farmacia'
    WHEN category ~* 'deporte|gimnasio|yoga|fitness' THEN 'Deporte'
    WHEN category ~* 'mascota|veterinari' THEN 'Mascotas'
    WHEN category ~* 'bodega|minimarket|market' THEN 'Bodega y minimarket'
    WHEN category ~* 'librer[ií]a|[úu]tiles' THEN 'Librería y útiles'
    WHEN category ~* 'hogar|ferreter|flor' THEN 'Hogar y ferretería'
    WHEN category ~* 'cultura|taller|arte|cine' THEN 'Cultura y entretenimiento'
    WHEN category ~* 'servicio|lavander' THEN 'Servicios'
    WHEN category ~* 'gastronom|comida|men[úu]|restaurant|cevich|poller|chifa|pizza|buffet' THEN 'Gastronomía'
    ELSE 'Servicios'
END;

-- US07: la corrida de geocodificación recorre solo las ofertas que esperan coordenadas.
CREATE INDEX ix_offers_geocoding_pending ON offers (id) WHERE geocoding_status = 'PENDING';
