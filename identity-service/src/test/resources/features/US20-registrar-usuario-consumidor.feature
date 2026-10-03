# language: es
@US20 @identity
Característica: Registrar usuario consumidor
  Como consumidor, quiero registrarme en la plataforma para acceder a las ofertas de mi zona.

  Escenario: Un usuario nuevo se registra con datos válidos
    Dado que no existe una cuenta con el correo "lucia.fernandez@ejemplo.pe"
    Cuando se registra como "CONSUMER" con nombre "Lucía Fernández Ríos", correo "lucia.fernandez@ejemplo.pe", teléfono "987123456" y contraseña "Ofertas#2026"
    Entonces la respuesta tiene código 201
    Y el campo "role" es "CONSUMER"
    Y la cuenta "lucia.fernandez@ejemplo.pe" tiene perfil de consumidor

  Escenario: El correo ya está registrado
    Dado que existe una cuenta de "Lucía Fernández Ríos" con el correo "lucia.fernandez@ejemplo.pe"
    Cuando se registra como "CONSUMER" con nombre "Mario Salas Paz", correo "lucia.fernandez@ejemplo.pe", teléfono "987000111" y contraseña "Ofertas#2026"
    Entonces la respuesta tiene código 409
    Y el campo "code" es "EMAIL_ALREADY_REGISTERED"
    Y la respuesta no contiene "Lucía Fernández Ríos"
