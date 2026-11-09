// QAS02 (US14, CA-14.2): de 30 a 300 peticiones/s en 5 minutos, 15 minutos sostenidos en 300 y 2 de bajada,
// contra GET /api/v1/offers/nearby. Solo en local (C2-02), con el gateway de docker-compose.loadtest.yml.
//
//   docker run --rm -v "$PWD/loadtest:/loadtest" -e BASE_URL=http://host.docker.internal:8080 \
//     grafana/k6:0.54.0 run /loadtest/qas02-nearby.js
//
// Una corrida corta para comprobar el montaje: RAMP=1m HOLD=1m DOWN=10s PEAK=100.
// El p95 de 300 ms se lee en Catalog (http.server.requests); el umbral de k6 es el externo de QAS01 (2 s).
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const START_RATE = Number(__ENV.START || 30);
const PEAK_RATE = Number(__ENV.PEAK || 300);
const RAMP = __ENV.RAMP || '5m';
const HOLD = __ENV.HOLD || '15m';
const DOWN = __ENV.DOWN || '2m';
const RESULT_FILE = __ENV.RESULT_FILE || `/loadtest/results/qas02-${new Date().toISOString().slice(0, 10)}.json`;

// Centros aproximados de los 43 distritos de Lima Metropolitana. Los cinco primeros son los densos y reciben el 70 %.
const DENSE = [
  [-12.1211, -77.0297], // Miraflores
  [-12.0975, -77.0365], // San Isidro
  [-12.0464, -77.0306], // Cercado de Lima
  [-12.1404, -76.9914], // Santiago de Surco
  [-12.0874, -77.0500], // Jesús María
];
const OTHERS = [
  [-11.7742, -77.1736], [-11.8586, -77.0750], [-12.0880, -77.0498], [-12.0590, -77.0500], [-12.1467, -77.0219],
  [-12.0453, -77.0108], [-11.8770, -77.0182], [-12.0333, -76.9300], [-12.0500, -76.9667], [-12.0800, -77.0000],
  [-11.9897, -77.0617], [-12.0731, -77.0806], [-12.0828, -77.0317], [-12.0647, -77.0367], [-12.0661, -77.0167],
  [-12.0889, -76.9922], [-11.9333, -77.0500], [-12.0833, -77.0833], [-12.0617, -77.0447], [-11.9833, -77.0083],
  [-11.9681, -77.0650], [-12.1000, -77.0475], [-12.0333, -77.0167], [-12.0236, -76.9878], [-12.1233, -77.0050],
  [-12.1083, -76.9967], [-11.9581, -76.9983], [-12.0742, -76.9472], [-12.1500, -76.9833], [-12.1750, -77.0167],
  [-12.1967, -76.9533], [-12.1667, -76.9333], [-12.2667, -76.9167], [-12.0464, -77.0428], [-12.0217, -77.0533],
  [-12.0083, -77.0917], [-12.0592, -77.1361], [-12.0600, -77.1000],
];
const CATEGORIES = [
  'Gastronomía', 'Cafetería y panadería', 'Bodega y minimarket', 'Salud y farmacia', 'Belleza y cuidado personal',
  'Deporte', 'Mascotas', 'Hogar y ferretería', 'Librería y útiles', 'Servicios', 'Cultura y entretenimiento',
];

export const options = {
  discardResponseBodies: true,
  scenarios: {
    qas02: {
      executor: 'ramping-arrival-rate',
      startRate: START_RATE,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 600,
      stages: [
        { target: PEAK_RATE, duration: RAMP },
        { target: PEAK_RATE, duration: HOLD },
        { target: 0, duration: DOWN },
      ],
    },
  },
  thresholds: {
    'http_req_duration{expected_response:true}': ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

function pick(list) {
  return list[Math.floor(Math.random() * list.length)];
}

// Hasta JITTER_METERS (1,5 km) alrededor del centro, la misma dispersión del dataset: muchas celdas por distrito,
// como consumidores reales que no están en el mismo punto.
const JITTER_METERS = Number(__ENV.JITTER_METERS || 1500);

function jitter([lat, lng]) {
  const meters = JITTER_METERS;
  return [lat + ((Math.random() * 2 - 1) * meters) / 111320, lng + ((Math.random() * 2 - 1) * meters) / 108900];
}

export default function () {
  const [lat, lng] = jitter(Math.random() < 0.7 ? pick(DENSE) : pick(OTHERS));
  const radius = 5 + Math.floor(Math.random() * 16);
  let url = `${BASE_URL}/api/v1/offers/nearby?lat=${lat.toFixed(6)}&lng=${lng.toFixed(6)}&radiusMinutes=${radius}`;
  if (Math.random() >= 0.8) {
    url += `&category=${encodeURIComponent(pick(CATEGORIES))}`;
  }
  const response = http.get(url, { tags: { name: 'GET /api/v1/offers/nearby' } });
  check(response, { 'status 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return {
    [RESULT_FILE]: JSON.stringify(data, null, 2),
    stdout: `\nQAS02 ${PEAK_RATE} req/s: p95 externo ${data.metrics.http_req_duration.values['p(95)'].toFixed(1)} ms, `
      + `${data.metrics.http_reqs.values.count} peticiones, fallidas ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)} %\n`,
  };
}
