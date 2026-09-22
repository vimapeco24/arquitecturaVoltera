/**
 * Entorno de desarrollo.
 * En `ng serve` se usa el proxy (proxy.conf.json) para evitar CORS y el
 * certificado autofirmado del gateway. Las rutas relativas /gw y /oauth2
 * se redirigen al servidor WSO2 (192.168.3.13).
 */
export const environment = {
  production: false,
  /** Prefijo del API Gateway WSO2 (proxyeado a https://192.168.3.13:8243). */
  gatewayBase: '/gw',
  /** Endpoint OAuth2 token (proxyeado a https://192.168.3.13:9443). */
  tokenUrl: '/oauth2/token',
  /** Credenciales del cliente OAuth2 (client_credentials). */
  oauth: {
    consumerKey: 'ndzpaXfp_KkDgRacWPzwfZvAjLMa',
    consumerSecret: 'mQHpTjKHPHCNViwFNBsTCnfffDMa',
  },
  // Tableros del clúster (LAN).
  observabilidad: {
    grafana: 'http://192.168.3.11:32421',
    prometheus: 'http://192.168.3.11:31659',
    kiali: 'http://192.168.3.11:32375/kiali',
    jaeger: 'http://192.168.3.11:31402',
  },
};
