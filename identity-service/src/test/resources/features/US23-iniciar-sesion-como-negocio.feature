# language: es
@US23 @identity
Característica: Iniciar sesión como negocio
  Como dueño de negocio, quiero iniciar sesión para gestionar mis campañas publicitarias.

  Antecedentes:
    Dado que existe el negocio "Bodega Doña Rosa" del dueño "rosa.quispe@ejemplo.pe" con contraseña "Bodega#2026"

  Escenario: Credenciales de negocio válidas
    Cuando inicia sesión con "rosa.quispe@ejemplo.pe" y "Bodega#2026"
    Entonces la respuesta tiene código 200
    Y el campo "role" es "BUSINESS_OWNER"
    Y el campo "businessId" es el id del negocio "Bodega Doña Rosa"
    Y el token lleva el claim "businessId" con el id del negocio "Bodega Doña Rosa"
    Y el token lleva el claim "roles" con "ROLE_BUSINESS_OWNER"
    Y el token lleva el claim "sub" con el id de la cuenta "rosa.quispe@ejemplo.pe"
    Y el token lleva el claim "iss" con "geops-identity"
    Y el token lleva el claim "aud" con "geops-api"
    Y el token lleva solo los claims "sub, iss, aud, roles, businessId, iat, exp"
    Y el token no lleva los claims "consumerId, jti"
    Y el token vence una hora después de emitido
    Y la firma del token se valida con la clave publicada en "/.well-known/jwks.json"
