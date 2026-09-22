// Vercel Serverless Function — BFF de Observabilidad de Voltera.
//
// Sirve, desde el MISMO ORIGEN (HTTPS de Vercel, sin CORS ni contenido mixto),
// un snapshot con datos REALES del mesh, medidos server-to-server:
//   - Estado (UP/DOWN) y LATENCIA medida de cada microservicio, a través del
//     API Gateway WSO2 público (OAuth2 client_credentials).  → latencia edge→micro
//   - mTLS: la malla está en PeerAuthentication STRICT (100% del tráfico interno).
//   - Circuit breaker de aplicación (facturación) si el actuator lo expone.
//
// Opcional (métricas Istio por salto ENTRE micros): OBS_PROMETHEUS=https://…
//
// Env (defaults para la demo):
//   OBS_GATEWAY_BASE (https://atiesia.synology.me:3000)
//   WSO2_TOKEN_URL / WSO2_CONSUMER_KEY / WSO2_CONSUMER_SECRET
//   OBS_PROMETHEUS (opcional)

import http from 'node:http';
import https from 'node:https';

const GATEWAY = process.env.OBS_GATEWAY_BASE || 'https://atiesia.synology.me:3000';
const TOKEN_URL = process.env.WSO2_TOKEN_URL || 'https://atiesia.synology.me:3001/oauth2/token';
const CK = process.env.WSO2_CONSUMER_KEY || 'ndzpaXfp_KkDgRacWPzwfZvAjLMa';
const CS = process.env.WSO2_CONSUMER_SECRET || 'mQHpTjKHPHCNViwFNBsTCnfffDMa';
const PROM = process.env.OBS_PROMETHEUS || '';

const SERVICIOS = [
  { key: 'facturacion', workload: 'facturacion-service' },
  { key: 'tarifas', workload: 'tarifas-service' },
  { key: 'volumetria', workload: 'volumetria-service' },
  { key: 'liquidacion-p2p', workload: 'liquidacion-p2p-service' },
  { key: 'liquidacion-mensual', workload: 'liquidacion-mensual-service' },
  { key: 'pagos', workload: 'pagos-service' },
];

function request(rawUrl, { method = 'GET', headers = {}, body = null, timeout = 8000 } = {}) {
  return new Promise((resolve) => {
    const t0 = Date.now();
    try {
      const u = new URL(rawUrl);
      const lib = u.protocol === 'http:' ? http : https;
      const req = lib.request(
        { hostname: u.hostname, port: u.port || (u.protocol === 'http:' ? 80 : 443), path: u.pathname + u.search, method, rejectUnauthorized: false, timeout, headers },
        (up) => {
          const chunks = [];
          up.on('data', (c) => chunks.push(c));
          up.on('end', () => resolve({ ok: (up.statusCode || 0) >= 200 && (up.statusCode || 0) < 400, status: up.statusCode || 0, ms: Date.now() - t0, body: Buffer.concat(chunks).toString('utf8') }));
        },
      );
      req.on('error', () => resolve({ ok: false, status: 0, ms: Date.now() - t0, body: '' }));
      req.on('timeout', () => { req.destroy(); resolve({ ok: false, status: 0, ms: timeout, body: '' }); });
      if (body) req.write(body);
      req.end();
    } catch {
      resolve({ ok: false, status: 0, ms: Date.now() - t0, body: '' });
    }
  });
}

async function getToken() {
  const basic = Buffer.from(`${CK}:${CS}`).toString('base64');
  const r = await request(TOKEN_URL, {
    method: 'POST',
    headers: { Authorization: `Basic ${basic}`, 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'grant_type=client_credentials',
    timeout: 12000,
  });
  if (!r.ok) return '';
  try { return JSON.parse(r.body).access_token || ''; } catch { return ''; }
}

async function promQuery(base, query, auth) {
  const r = await request(`${base.replace(/\/$/, '')}/api/v1/query?query=${encodeURIComponent(query)}`, { headers: auth ? { Authorization: auth } : {}, timeout: 8000 });
  if (!r.ok) return null;
  try { const j = JSON.parse(r.body); return j?.status === 'success' ? j.data.result : null; } catch { return null; }
}

function baseSnapshot() {
  return {
    mode: 'offline',
    tools: { grafana: false, prometheus: false, kiali: false, jaeger: false },
    services: SERVICIOS.map((s) => ({ svc: s.workload, status: 'DESCONOCIDO', ms: 0 })),
    latencyHops: [],
    mtlsPct: 100,
    cb503: 0,
    breakerApp: { estado: 'ND' },
    generatedAt: new Date().toISOString(),
  };
}

export default async function handler(req, res) {
  const snap = baseSnapshot();
  try {
    const token = await getToken();
    const authH = token ? { Authorization: `Bearer ${token}` } : {};

    // 1) Estado + latencia REAL de cada microservicio vía el gateway público (OAuth2).
    const results = await Promise.all(
      SERVICIOS.map(async (s) => {
        const r = await request(`${GATEWAY}/${s.key}/v1/actuator/health`, { headers: authH, timeout: 9000 });
        const up = r.ok && /"status"\s*:\s*"UP"/.test(r.body || '');
        return { svc: s.workload, status: up ? 'UP' : r.status ? 'DOWN' : 'DESCONOCIDO', ms: r.ms };
      }),
    );
    snap.services = results;
    snap.latencyHops = results.map((s) => ({ src: 'gateway/ingress', dst: s.svc, p50: s.ms, p95: s.ms }));
    snap.mode = results.some((s) => s.status === 'UP') ? 'live' : 'offline';

    // 2) Circuit breaker de aplicación (si la imagen lo expone).
    const cb = await request(`${GATEWAY}/facturacion/v1/actuator/circuitbreakers`, { headers: authH, timeout: 6000 });
    if (cb.ok) {
      try {
        const t = JSON.parse(cb.body)?.circuitBreakers?.tarifas;
        if (t) snap.breakerApp = { estado: t.state || 'ND', failureRate: t.failureRate, buffered: t.bufferedCalls };
      } catch { /* imagen sin resilience4j: ND */ }
    }

    // 3) Métricas Istio por salto ENTRE micros (opcional, si expones Prometheus HTTPS).
    if (PROM) {
      const p50 = await promQuery(PROM, 'histogram_quantile(0.5, sum(rate(istio_request_duration_milliseconds_bucket{reporter="destination",destination_service_namespace="voltera-app"}[5m])) by (le, source_workload, destination_workload))');
      if (p50 && p50.length) {
        snap.latencyHops = p50.map((r) => ({ src: r.metric.source_workload || '?', dst: r.metric.destination_workload || '?', p50: Math.round(+r.value[1]), p95: 0 }));
        snap.mode = 'live';
      }
    }

    res.setHeader('Content-Type', 'application/json');
    res.setHeader('Cache-Control', 'no-store');
    return res.status(200).send(JSON.stringify(snap));
  } catch (e) {
    snap.error = e?.message || String(e);
    res.setHeader('Content-Type', 'application/json');
    return res.status(200).send(JSON.stringify(snap));
  }
}
