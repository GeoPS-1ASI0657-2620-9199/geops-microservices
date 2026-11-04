#!/bin/bash
# Reinyecta en su tópico de origen lo que quedó en <tópico>.DLT, con la misma clave.
# Los consumidores son idempotentes: reinyectar es seguro para los grupos que ya lo procesaron.
# Uso, desde platform/: ./scripts/replay-dlt.sh reservation.events [--max N]
set -euo pipefail
export MSYS_NO_PATHCONV=1   # Git Bash en Windows no debe reescribir /opt/kafka
topic=${1:?tópico de origen, por ejemplo reservation.events}
max=1000
if [ "${2:-}" = "--max" ]; then max=${3:?cuántos mensajes}; fi

docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
  --topic "$topic.DLT" --from-beginning --timeout-ms 5000 --max-messages "$max" \
  --property print.key=true --property key.separator='|' \
| docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:9092 \
  --topic "$topic" --property parse.key=true --property key.separator='|'
