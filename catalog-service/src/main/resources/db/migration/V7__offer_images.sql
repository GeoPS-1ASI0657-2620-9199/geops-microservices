-- Fotos de las ofertas subidas por el negocio (US05). Se guardan en catalog_db para no sumar un volumen ni un
-- servicio de archivos al despliegue; cada foto pesa como máximo 2 MB.
CREATE TABLE offer_images (
    image_id     uuid        PRIMARY KEY,
    business_id  bigint      NOT NULL,
    content_type varchar(30) NOT NULL,
    content      bytea       NOT NULL,
    uploaded_at  timestamptz NOT NULL
);

CREATE INDEX idx_offer_images_business ON offer_images (business_id);
