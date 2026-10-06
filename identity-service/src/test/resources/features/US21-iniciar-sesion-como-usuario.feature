# language: es
@US21 @identity
Característica: Iniciar sesión como usuario
  Como consumidor, quiero iniciar sesión para acceder a mis ofertas guardadas y preferencias.

  Antecedentes:
    Dado que existe el consumidor "lucia.fernandez@ejemplo.pe" con contraseña "Ofertas#2026"

  Escenario: Credenciales válidas
    Cuando inicia sesión con "lucia.fernandez@ejemplo.pe" y "Ofertas#2026"
    Entonces la respuesta tiene código 200
    Y el campo "role" es "CONSUMER"
    Y la firma del token se valida con la clave publicada en "/.well-known/jwks.json"

  Esquema del escenario: Credenciales inválidas
    Cuando inicia sesión con "<correo>" y "<contraseña>"
    Entonces la respuesta tiene código 401
    Y el campo "code" es "INVALID_CREDENTIALS"
    Y el campo "message" es "No se pudo iniciar sesión con esos datos."

    Ejemplos:
      | correo                      | contraseña   |
      | lucia.fernandez@ejemplo.pe  | Otra#2026    |
      | nadie@ejemplo.pe            | Ofertas#2026 |
