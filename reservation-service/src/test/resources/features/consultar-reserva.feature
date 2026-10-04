# language: es
@GEO-208 @reservation
Característica: Consultar reservas
  Como consumidor quiero ver mis reservas, y como dueño de negocio quiero ver la reserva que corresponde a un código.

  Antecedentes:
    Dado que Catalog tiene la oferta 1052 del comercio 301 titulada "Menú ejecutivo a mitad de precio" vigente hasta el "2026-10-14"
    Y que el consumidor 2001 ya reservó la oferta 1052

  Escenario: El consumidor consulta su reserva
    Cuando el consumidor 2001 consulta esa reserva
    Entonces la respuesta tiene código 200
    Y la respuesta muestra la oferta 1052 titulada "Menú ejecutivo a mitad de precio" en ACTIVE
    Y el campo "expiresAt" es "2026-10-15T04:59:59Z"

  Escenario: La reserva no existe
    Cuando el consumidor 2001 consulta una reserva que no existe
    Entonces la respuesta tiene código 404
    Y el campo "code" es "RESERVATION_NOT_FOUND"

  Escenario: Otro consumidor no puede ver la reserva
    Cuando el consumidor 2002 consulta esa reserva
    Entonces la respuesta tiene código 403
    Y el campo "code" es "FORBIDDEN"

  Escenario: El comercio dueño de la oferta consulta el código
    Cuando el comercio 301 consulta el código de esa reserva
    Entonces la respuesta tiene código 200
    Y la respuesta muestra la oferta 1052 titulada "Menú ejecutivo a mitad de precio" en ACTIVE
    Y el campo "expiresAt" es "2026-10-15T04:59:59Z"

  Escenario: Otro comercio no puede consultar el código
    Cuando el comercio 302 consulta el código de esa reserva
    Entonces la respuesta tiene código 403
    Y el campo "code" es "FORBIDDEN"

  Escenario: Un consumidor no puede consultar por código
    Cuando el consumidor 2001 consulta el código de esa reserva
    Entonces la respuesta tiene código 403
    Y el campo "code" es "FORBIDDEN"

  Escenario: El consumidor lista sus reservas activas
    Dado que Catalog tiene la oferta 1053 del comercio 301 titulada "Desayuno 2x1" vigente hasta el "2026-10-20"
    Y que el consumidor 2001 ya reservó la oferta 1053
    Y que esa reserva ya fue canjeada
    Cuando el consumidor 2001 lista sus reservas en ACTIVE
    Entonces la respuesta tiene código 200
    Y la lista tiene solo reservas en ACTIVE de la oferta 1052

  Escenario: El estado pedido no existe
    Cuando el consumidor 2001 lista sus reservas en VIGENTE
    Entonces la respuesta tiene código 400
    Y el campo "code" es "VALIDATION_ERROR"
