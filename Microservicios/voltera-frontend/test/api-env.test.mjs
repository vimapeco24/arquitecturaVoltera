// Pruebas de variables WSO2 faltantes. Ejecutar: npm test  (node --test, sin red).
import test from 'node:test';
import assert from 'node:assert/strict';
import token from '../api/token.js';
import obs from '../api/obs.js';

function mockRes() {
  return {
    code: 200, body: undefined, headers: {},
    status(c) { this.code = c; return this; },
    setHeader(k, v) { this.headers[k] = v; },
    json(b) { this.body = JSON.stringify(b); return this; },
    send(b) { this.body = typeof b === 'string' ? b : String(b); return this; },
  };
}
const SECRET = 'valor-secreto-de-prueba';

test('token.js: sin WSO2_CONSUMER_KEY responde 500 nombrando la variable', async () => {
  delete process.env.WSO2_CONSUMER_KEY;
  process.env.WSO2_CONSUMER_SECRET = SECRET;
  const res = mockRes();
  await token({ method: 'POST' }, res);
  assert.equal(res.code, 500);
  assert.match(res.body, /WSO2_CONSUMER_KEY/);
  assert.doesNotMatch(res.body, new RegExp(SECRET));
});

test('token.js: sin WSO2_CONSUMER_SECRET responde 500 nombrando la variable', async () => {
  process.env.WSO2_CONSUMER_KEY = 'k';
  delete process.env.WSO2_CONSUMER_SECRET;
  const res = mockRes();
  await token({ method: 'GET' }, res);
  assert.equal(res.code, 500);
  assert.match(res.body, /WSO2_CONSUMER_SECRET/);
});

test('obs.js: sin credenciales responde 500 nombrando ambas variables', async () => {
  delete process.env.WSO2_CONSUMER_KEY;
  delete process.env.WSO2_CONSUMER_SECRET;
  const res = mockRes();
  await obs({}, res);
  assert.equal(res.code, 500);
  assert.match(res.body, /WSO2_CONSUMER_KEY/);
  assert.match(res.body, /WSO2_CONSUMER_SECRET/);
});
