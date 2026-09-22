/**
 * Entorno de producción (build para Vercel) — vía BFF serverless.
 *
 * El navegador ya NO habla directamente con WSO2 (que usa un certificado
 * autofirmado y provocaba "status 0 / Unknown Error"). En su lugar llama a las
 * funciones serverless del mismo dominio Vercel (certificado válido):
 *   - gatewayBase = '/api/gw'   -> proxy a WSO2 :3000  (api/gw/[...path].js)
 *   - tokenUrl    = '/api/token'-> token OAuth2 WSO2 :3001 (api/token.js)
 *
 * Esas funciones hacen la llamada server-to-server ignorando el cert
 * autofirmado del gateway, ocultan el consumerSecret y evitan CORS del navegador.
 *
 * NOTA: como el secret vive en el servidor (variables de entorno de Vercel),
 * los valores de `oauth` aquí ya no se usan en el navegador; se dejan por
 * compatibilidad de tipos. El AuthService detecta que tokenUrl es '/api/token'
 * y no envía Basic desde el cliente.
 */
export const environment = {
  production: true,
  gatewayBase: '/api/gw',
  tokenUrl: '/api/token',
  oauth: {
    consumerKey: '',
    consumerSecret: '',
  },
  // Tableros del clúster (LAN). Enlaces "Abrir" de la página de Observabilidad.
  observabilidad: {
    grafana: 'http://192.168.3.11:32421',
    prometheus: 'http://192.168.3.11:31659',
    kiali: 'http://192.168.3.11:32375/kiali',
    jaeger: 'http://192.168.3.11:31402',
  },
};
