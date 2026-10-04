# language: es
@US40 @GEO-208 @reservation
Característica: Reservar una oferta sin pagar en la plataforma
  Como consumidor, quiero reservar una oferta y pagar en el local para no arriesgar dinero por adelantado.

  Antecedentes:
    Dado que Catalog tiene la oferta 1052 del comercio 301 titulada "Menú ejecutivo a mitad de precio" vigente hasta el "2026-10-14"

  Escenario: La oferta está vigente y la reserva no pide medio de pago
    Cuando el consumidor 2001 reserva la oferta 1052
    Entonces la respuesta tiene código 201
    Y la respuesta solo trae los campos reservationId, code y expiresAt
    Y el campo "code" tiene 8 caracteres
    Y el campo "expiresAt" es "2026-10-15T04:59:59Z"
    Y la cabecera Location apunta a la reserva creada
    Y la reserva guardada está en ACTIVE con el título "Menú ejecutivo a mitad de precio" y el comercio 301

  Escenario: La oferta ya venció
    Dado que Catalog tiene la oferta 1053 del comercio 301 titulada "Desayuno 2x1" vigente hasta el "2026-01-01"
    Cuando el consumidor 2001 reserva la oferta 1053
    Entonces la respuesta tiene código 409
    Y el campo "code" es "OFFER_NOT_AVAILABLE"
    Y no se guardó ninguna reserva

  Escenario: Catalog indica que la oferta ya no está disponible
    Dado que Catalog marca la oferta 1052 como no disponible
    Cuando el consumidor 2001 reserva la oferta 1052
    Entonces la respuesta tiene código 409
    Y el campo "code" es "OFFER_NOT_AVAILABLE"
    Y no se guardó ninguna reserva

  Escenario: La oferta no existe
    Cuando el consumidor 2001 reserva la oferta 9999
    Entonces la respuesta tiene código 404
    Y el campo "code" es "OFFER_NOT_FOUND"

  Escenario: Catalog no responde
    Dado que Catalog no responde
    Cuando el consumidor 2001 reserva la oferta 1052
    Entonces la respuesta tiene código 503
    Y el campo "code" es "CATALOG_UNAVAILABLE"
    Y no se guardó ninguna reserva

  Escenario: Repetir la reserva de una oferta con reserva activa devuelve la misma
    Dado que el consumidor 2001 ya reservó la oferta 1052
    Cuando el consumidor 2001 reserva la oferta 1052
    Entonces la respuesta tiene código 200
    Y la respuesta es la misma reserva de antes
    Y el consumidor 2001 tiene 1 reserva en ACTIVE de la oferta 1052

  Escenario: Peticiones simultáneas de la misma reserva crean una sola
    Cuando el consumidor 2001 envía 5 reservas simultáneas de la oferta 1052
    Entonces una respuesta tiene código 201 y las demás 200 con la misma reserva
    Y el consumidor 2001 tiene 1 reserva en ACTIVE de la oferta 1052

  Escenario: Una reserva ya canjeada no impide reservar de nuevo la misma oferta
    Dado que el consumidor 2001 ya reservó la oferta 1052
    Y que esa reserva ya fue canjeada
    Cuando el consumidor 2001 reserva la oferta 1052
    Entonces la respuesta tiene código 201
    Y la respuesta es una reserva nueva
    Y el consumidor 2001 tiene 1 reserva en ACTIVE de la oferta 1052

  Escenario: La petición no indica la oferta
    Cuando el consumidor 2001 envía una reserva sin offerId
    Entonces la respuesta tiene código 400
    Y el campo "code" es "VALIDATION_ERROR"

  Esquema del escenario: Solo un consumidor con un token válido de Identity puede reservar
    Cuando se reserva la oferta 1052 <token>
    Entonces la respuesta tiene código <estado>
    Y el campo "code" es "<código>"
    Y no se guardó ninguna reserva

    Ejemplos:
      | token                            | estado | código       |
      | sin token                        | 401    | UNAUTHORIZED |
      | con token de otro emisor         | 401    | UNAUTHORIZED |
      | con token para otra audiencia    | 401    | UNAUTHORIZED |
      | con token firmado con otra clave | 401    | UNAUTHORIZED |
      | con token de dueño de negocio    | 403    | FORBIDDEN    |
      | con token sin rol                | 403    | FORBIDDEN    |
