// Vercel Serverless Function — BFF de token OAuth2 para Voltera.
//
// El navegador llama a /api/token (mismo origen que el front en Vercel, con
// certificado válido). Esta función hace la llamada server-to-server a WSO2
// (:3001/oauth2/token), ignorando el certificado autofirmado del gateway.
//
// Ventajas:
//   - Elimina el error TLS "status 0" en el navegador (WSO2 usa cert self-signed).
//   - Oculta el consumerSecret: nunca viaja al navegador.
//   - Evita problemas de CORS del navegador contra el gateway.
//
// Config por variables de entorno de Vercel (con defaults para la demo):
//   WSO2_TOKEN_URL, WSO2_CONSUMER_KEY, WSO2_CONSUMER_SECRET

import https from 'node:https';

const TOKEN_URL = process.env.WSO2_TOKEN_URL || 'https://atiesia.synology.me:3001/oauth2/token';
const CK = process.env.WSO2_CONSUMER_KEY || 'ndzpaXfp_KkDgRacWPzwfZvAjLMa';
const CS = process.env.WSO2_CONSUMER_SECRET || 'mQHpTjKHPHCNViwFNBsTCnfffDMa';

/** POST server-to-server ignorando el certificado autofirmado del gateway. */
function requestToken() {
  return new Promise((resolve, reject) => {
    const url = new URL(TOKEN_URL);
    const basic = Buffer.from(`${CK}:${CS}`).toString('base64');
    const payload = 'grant_type=client_credentials';

    const req = https.request(
      {
        hostname: url.hostname,
        port: url.port || 443,
        path: url.pathname + url.search,
        method: 'POST',
        rejectUnauthorized: false, // acepta cert self-signed (solo lado servidor)
        headers: {
          Authorization: `Basic ${basic}`,
          'Content-Type': 'application/x-www-form-urlencoded',
          'Content-Length': Buffer.byteLength(payload),
        },
        timeout: 15000,
      },
      (up) => {
        let data = '';
        up.on('data', (c) => (data += c));
        up.on('end', () => resolve({ status: up.statusCode || 502, body: data, contentType: up.headers['content-type'] }));
      },
    );
    req.on('error', reject);
    req.on('timeout', () => req.destroy(new Error('timeout contactando WSO2')));
    req.write(payload);
    req.end();
  });
}

export default async function handler(req, res) {
  if (req.method !== 'POST' && req.method !== 'GET') {
    res.setHeader('Allow', 'GET, POST');
    return res.status(405).json({ error: 'method_not_allowed' });
  }
  try {
    const r = await requestToken();
    res.status(r.status);
    res.setHeader('Content-Type', r.contentType || 'application/json');
    return res.send(r.body);
  } catch (e) {
    return res.status(502).json({
      error: 'token_upstream_error',
      message: e?.message || String(e),
      hint: 'No se pudo contactar el endpoint de token de WSO2 (¿gateway arriba?).',
    });
  }
}
