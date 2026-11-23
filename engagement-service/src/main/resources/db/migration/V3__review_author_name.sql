-- Nombre corto del cliente que publicó la reseña ("Lucía F."), tomado del token al publicarla
-- para que el comercio sepa a quién responde sin llamar a Identity. Las reseñas anteriores quedan en null.
ALTER TABLE reviews ADD COLUMN author_name varchar(60);
