# language: es
@US22 @identity
Característica: Registrar un negocio
  Como dueño de negocio, quiero registrar mi comercio para publicar campañas dirigidas a clientes cercanos.

  Escenario: El negocio se registra con sus datos y su ubicación
    Cuando registra el negocio "Bodega Doña Rosa" con RUC "10456789019", dirección "Jr. Huánuco 1250, La Victoria", latitud -12.0681 y longitud -77.0350
    Entonces la respuesta tiene código 201
    Y el campo "role" es "BUSINESS_OWNER"
    Y el campo "verificationStatus" es "UNVERIFIED"
    Y la cuenta "rosa.quispe@ejemplo.pe" tiene perfil de negocio

  Esquema del escenario: La ubicación es inválida
    Cuando registra el negocio "Bodega Doña Rosa" con RUC "10456789019", dirección "<dirección>", latitud <latitud> y longitud <longitud>
    Entonces la respuesta tiene código 400
    Y el campo "code" es "INVALID_LOCATION"

    Ejemplos:
      | dirección                      | latitud  | longitud |
      | Jr. Huánuco 1250, La Victoria  | 95.0     | -77.0350 |
      | Jr. Huánuco 1250, La Victoria  | -12.0681 | -200.0   |
      |                                | -12.0681 | -77.0350 |
