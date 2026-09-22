// Vercel Serverless Function — Proxy al API Gateway WSO2 de Voltera.
//
// Recibe /api/gw/<servicio>/v1/api/v1/...  (reescrito por vercel.json a
// /api/gw?path=<servicio>/v1/...&<query original>). Reenvía server-to-server
// al gateway WSO2 (:3000) ignorando su certificado autofirmado y propaga el
// header Authorization (Bearer) que el front ya envía.
//
// Config por variable de entorno (default para la demo):
//   WSO2_GATEWAY_BASE  (p. ej. https://atiesia.synology.me:3000)

import https from 'node:https';

const GATEWAY_BASE = process.env.WSO2_GATEWAY_BASE || 'https://atiesia.synology.me:3000';

function forward({ method, path, headers, bodyBuffer }) {
  return new Promise((resolve, reject) => {
    const base = new URL(GATEWAY_BASE);
    const req = https.request(
      {
        hostname: base.hostname,
        port: base.port || 443,
        path,
        method,
        rejectUnauthorized: false,
        headers,
        timeout: 20000,
      },
      (up) => {
        const chunks = [];
        up.on('data', (c) => chunks.push(c));
        up.on('end', () =>
          resolve({ status: up.statusCode || 502, body: Buffer.concat(chunks), contentType: up.headers['content-type'] }),
        );
      },
    );
    req.on('error', reject);
    req.on('timeout', () => req.destroy(new Error('timeout contactando el gateway')));
    if (bodyBuffer && bodyBuffer.length) req.write(bodyBuffer);
    req.end();
  });
}

function readRawBody(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', () => resolve(Buffer.alloc(0)));
  });
}

export default async function handler(req, res) {
  try {
    // El path destino llega como query param "path" (lo pone el rewrite de
    // vercel.json). El resto de query params se reenvían tal cual.
    const reqUrl = new URL(req.url, 'http://localhost');
    const rawPath = reqUrl.searchParams.get('path') || '';
    reqUrl.searchParams.delete('path');
    const qs = reqUrl.searchParams.toString();
    const upstreamPath = '/' + rawPath.replace(/^\/+/, '') + (qs ? `?${qs}` : '');

    const bodyBuffer = ['GET', 'HEAD'].includes(req.method) ? Buffer.alloc(0) : await readRawBody(req);

    const headers = {};
    if (req.headers['authorization']) headers['Authorization'] = req.headers['authorization'];
    if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];
    headers['Accept'] = req.headers['accept'] || 'application/json';
    if (bodyBuffer.length) headers['Content-Length'] = bodyBuffer.length;

    const r = await forward({ method: req.method, path: upstreamPath, headers, bodyBuffer });
    res.status(r.status);
    if (r.contentType) res.setHeader('Content-Type', r.contentType);
    return res.send(r.body);
  } catch (e) {
    return res.status(502).json({
      error: 'gateway_upstream_error',
      message: e?.message || String(e),
      hint: 'No se pudo contactar el gateway WSO2 (¿arriba? ¿red?).',
    });
  }
}
