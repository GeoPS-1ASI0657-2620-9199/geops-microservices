# language: es
@US11 @US45 @catalog
Característica: Métricas por campaña y línea base del negocio
  Como dueño de negocio, quiero ver alcance, clics y conversiones de mis campañas y compararlos con los
  clientes que atendía antes de entrar, para saber qué campaña funciona y si la plataforma me sirvió.

  Antecedentes:
    Dado que el comercio 84 "Restaurante Don Pepe" tiene la campaña "Almuerzos de octubre" del "2026-10-01" al "2026-10-31" con la oferta "Menú ejecutivo" en su local

  @US11
  Escenario: Las búsquedas cercanas y los detalles abiertos se cuentan como impresiones y clics
    Dado que un consumidor buscó ofertas junto al local 3 veces
    Y abrió el detalle de "Menú ejecutivo" 2 veces
    Cuando se escriben los contadores de impresiones y clics
    Y el dueño del comercio 84 consulta las métricas de "Almuerzos de octubre" del "2026-10-08" al "2026-10-08"
    Entonces la respuesta tiene el estado 200
    Y los totales son 3 impresiones, 2 clics, 0 reservas y 0 canjes
    Y la tasa de clics es "66.7"

  @US11
  Escenario: Las reservas y los canjes llegan de Reservation y un evento repetido se cuenta una vez
    Cuando Reservation publica 2 ReservationCreated y 1 ReservationRedeemed de la oferta "Menú ejecutivo" el "2026-10-08"
    Y Reservation vuelve a publicar el mismo ReservationRedeemed
    Y el dueño del comercio 84 consulta las métricas de "Almuerzos de octubre" del "2026-10-08" al "2026-10-08"
    Entonces los totales son 0 impresiones, 0 clics, 2 reservas y 1 canjes
    Y la reputación del comercio 84 también contó 1 canje

  @US11
  Escenario: Las cifras corresponden solo al periodo elegido y los días sin actividad salen en cero
    Dado que la campaña "Almuerzos de octubre" registró estas métricas:
      | fecha      | impresiones | clics | reservas | canjes |
      | 2026-10-03 | 100         | 10    | 2        | 1      |
      | 2026-10-05 | 50          | 5     | 1        | 1      |
      | 2026-10-07 | 999         | 99    | 9        | 9      |
    Cuando el dueño del comercio 84 consulta las métricas de "Almuerzos de octubre" del "2026-10-03" al "2026-10-05"
    Entonces la serie diaria tiene 3 días y el "2026-10-04" está en cero
    Y los totales son 150 impresiones, 15 clics, 3 reservas y 2 canjes
    Y la conversión es "13.3"

  @US11
  Escenario: Una campaña sin actividad aparece con sus cifras en cero en el panel
    Dado que el comercio 84 también tiene la campaña "Ceviches en Miraflores" sin actividad
    Y que la campaña "Almuerzos de octubre" registró estas métricas:
      | fecha      | impresiones | clics | reservas | canjes |
      | 2026-10-06 | 80          | 8     | 2        | 1      |
    Cuando el dueño del comercio 84 consulta el resumen de sus campañas
    Entonces el resumen tiene 2 campañas
    Y "Almuerzos de octubre" tiene 80 impresiones en el resumen
    Y "Ceviches en Miraflores" tiene 0 impresiones en el resumen

  @US11
  Escenario: Otro comercio no puede ver las métricas de la campaña
    Cuando el dueño del comercio 85 consulta las métricas de "Almuerzos de octubre" del "2026-10-01" al "2026-10-08"
    Entonces la respuesta tiene el estado 403

  @US11
  Escenario: Un periodo con la fecha final antes que la inicial se rechaza
    Cuando el dueño del comercio 84 consulta las métricas de "Almuerzos de octubre" del "2026-10-08" al "2026-10-01"
    Entonces la respuesta tiene el estado 400
    Y el código de error es "INVALID_METRICS_PERIOD"

  @US45
  Escenario: La línea base declarada queda registrada con su fecha
    Cuando el dueño del comercio 84 declara 140 clientes por semana y un ticket promedio de "18.50"
    Entonces la respuesta tiene el estado 200
    Y la línea base del comercio 84 tiene 140 clientes por semana, ticket "18.50" y fecha "2026-10-08T13:05:00Z"

  @US45
  Escenario: Con línea base la comparación contra el periodo se muestra en porcentaje
    Dado que el comercio 84 declaró 140 clientes por semana y un ticket promedio de "18.50"
    Y que la campaña "Almuerzos de octubre" registró estas métricas:
      | fecha      | impresiones | clics | reservas | canjes |
      | 2026-10-05 | 210         | 15    | 4        | 3      |
      | 2026-10-07 | 430         | 33    | 8        | 6      |
    Cuando el dueño del comercio 84 consulta las métricas de "Almuerzos de octubre" del "2026-10-05" al "2026-10-07"
    Entonces la comparación espera "60.0" clientes y la campaña suma 9 con un cambio de "15.0" %
    Y las ventas estimadas por la campaña son "166.50"

  @US45
  Escenario: Sin línea base se muestran las cifras absolutas
    Cuando el dueño del comercio 84 consulta las métricas de "Almuerzos de octubre" del "2026-10-01" al "2026-10-08"
    Entonces la respuesta tiene el estado 200
    Y la respuesta no trae línea base
    Y la consulta de la línea base del comercio 84 responde sin contenido

  @US45
  Escenario: Una línea base sin clientes se rechaza
    Cuando el dueño del comercio 84 declara 0 clientes por semana y un ticket promedio de "18.50"
    Entonces la respuesta tiene el estado 400
    Y el código de error es "INVALID_REQUEST"
