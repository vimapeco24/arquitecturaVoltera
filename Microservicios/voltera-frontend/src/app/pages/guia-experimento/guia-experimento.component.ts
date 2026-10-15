import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SERVICIOS } from '../../core/gateway';

/**
 * Página "Guía del Experimento" — explica, de forma detallada y fiel a la
 * arquitectura real desplegada, cómo debe funcionar el experimento de Voltera:
 * objetivo, prerrequisitos, el flujo de negocio paso a paso (6 pasos),
 * la cadena EDA de telemetría (coreografía + KEDA), cómo ejecutar la prueba
 * desde el propio front y qué resultado esperar en cada etapa.
 *
 * Es una vista 100% documentativa: no llama servicios, pero cada bloque está
 * alineado con los microservicios y rutas que el resto del front consume vía
 * el API Gateway WSO2 (ver core/gateway.ts y services/*.service.ts).
 */
@Component({
  selector: 'app-guia-experimento',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './guia-experimento.component.html',
  styleUrl: './guia-experimento.component.scss',
})
export class GuiaExperimentoComponent {
  /** Nombres de servicio tal como están publicados en el gateway (fuente única). */
  readonly svc = SERVICIOS;

  /** Prerrequisitos para que la prueba corra de punta a punta. */
  readonly prerequisitos = [
    {
      titulo: 'Backend desplegado en Kubernetes',
      detalle:
        'Namespace voltera-app con los 16 microservicios + infraestructura (Redpanda, Redis, TimescaleDB, service-registry) en estado Running.',
    },
    {
      titulo: 'API Gateway WSO2 arriba',
      detalle:
        'Publica cada servicio como API en https://atiesia.synology.me:3000 y expone el endpoint OAuth2 (:3001). El front nunca habla directo con los pods: todo pasa por el gateway.',
    },
    {
      titulo: 'Credenciales OAuth2 (client_credentials)',
      detalle:
        'El front obtiene un token Bearer vía /api/token (BFF serverless) y lo adjunta a cada llamada. El token se cachea y se renueva solo.',
    },
    {
      titulo: 'Conectividad',
      detalle:
        'En producción el front corre en Vercel y usa funciones serverless como proxy. En desarrollo (ng serve) usa proxy.conf.json contra 192.168.3.13 (requiere VPN a la LAN).',
    },
  ];

  /** Flujo de negocio principal — los 6 pasos que recorren el ciclo comercial. */
  readonly flujoNegocio = [
    {
      paso: 1,
      servicio: 'Tarifas',
      ruta: '/tarifas',
      api: 'POST /tarifas/v1/api/v1/tarifas',
      que: 'Se define una tarifa con sus franjas horarias (precio por hora).',
      espera: 'La tarifa queda registrada y disponible para consultar su precio por hora.',
    },
    {
      paso: 2,
      servicio: 'Volumetría',
      ruta: '/volumetria',
      api: 'POST /volumetria/v1/api/v1/lecturas',
      que: 'Se cargan las lecturas de energía (generada / consumida) de los prosumidores.',
      espera: 'Las lecturas quedan agregadas por periodo, listas para liquidar.',
    },
    {
      paso: 3,
      servicio: 'Liquidación P2P',
      ruta: '/liquidacion-p2p',
      api: 'POST /liquidacion-p2p/v1/api/v1/transacciones',
      que: 'Se casan excedentes entre vecinos (peer-to-peer) usando la tarifa vigente.',
      espera: 'Se generan transacciones P2P con el monto liquidado entre pares.',
    },
    {
      paso: 4,
      servicio: 'Liquidación Mensual',
      ruta: '/liquidacion-mensual',
      api: 'POST /liquidacion-mensual/v1/api/v1/liquidaciones',
      que: 'Se consolida el balance del mes por prosumidor (neto energía + P2P).',
      espera: 'Una liquidación mensual por usuario con el saldo a facturar.',
    },
    {
      paso: 5,
      servicio: 'Facturación',
      ruta: '/facturacion',
      api: 'POST /facturacion/v1/api/v1/facturas',
      que: 'Se emite la factura a partir de la liquidación (con circuit breaker a Tarifas).',
      espera: 'FacturaEmitida con detalle de cargos; resiliente si Tarifas degrada.',
    },
    {
      paso: 6,
      servicio: 'Pagos',
      ruta: '/pagos',
      api: 'POST /pagos/v1/api/v1/pagos',
      que: 'Se registra el pago de la factura.',
      espera: 'PagoConfirmado; dispara la conciliación en Cobros (dominio seguros).',
    },
  ];

  /** Cadena EDA de telemetría (coreografía dirigida por eventos). */
  readonly cadenaEda = [
    {
      comp: 'Integración AMI (ACL)',
      ruta: '/integracion-ami',
      rol: 'Traduce el protocolo del head-end/medidor y emite LecturaCrudaRecibida.',
    },
    {
      comp: 'Habilitación de Medidores',
      ruta: '/habilitacion',
      rol: 'Da de alta medidores; emite MedidorHabilitado (sin datos personales).',
    },
    {
      comp: 'Ingesta',
      ruta: '/ingesta',
      rol: 'Valida y deduplica lecturas; emite LecturaValidada / LecturaSospechosa.',
    },
    {
      comp: 'Telemetría Core (CQRS)',
      ruta: '/telemetria-core',
      rol: 'Serie append-only en TimescaleDB + vista agregada en Redis (lado query).',
    },
    {
      comp: 'Notificaciones',
      ruta: '/notificaciones',
      rol: 'Consume eventos de negocio y notifica al cliente (escala a cero sin carga).',
    },
  ];

  /** Pasos concretos para ejecutar la prueba desde el front. */
  readonly comoProbar = [
    'Abre el Panel y revisa que el estado de salud de los 16 servicios esté en UP (se consulta /actuator/health de cada uno vía gateway).',
    'Recorre en orden las páginas del flujo de negocio (pasos 1 → 6) creando datos de ejemplo en cada una.',
    'Verifica en Facturación que la factura refleja la liquidación, y en Pagos que el pago queda confirmado.',
    'Entra a Cadena EDA y Telemetría Core para seguir la coreografía de eventos y la vista CQRS agregada.',
    'Dispara carga de telemetría (página Experimentos / Lab Métricas) y observa en Observabilidad cómo KEDA escala Ingesta y Telemetría Core según el lag.',
    'Abre Observabilidad para ver Grafana, Prometheus, Kiali y Jaeger (trazas distribuidas end-to-end).',
  ];

  /** Qué se espera observar: criterios de una ejecución correcta. */
  readonly resultadosEsperados = [
    { criterio: 'Salud', ok: 'Los servicios de negocio responden 200 UP a través del gateway.' },
    { criterio: 'Flujo comercial', ok: 'Tarifa → lecturas → P2P → liquidación → factura → pago encadenan sin error.' },
    { criterio: 'Idempotencia', ok: 'Reenviar el mismo evento (mismo eventId) no duplica el efecto de negocio (inbox).' },
    { criterio: 'CQRS', ok: 'La serie en TimescaleDB crece (append-only) y la vista agregada en Redis se actualiza.' },
    { criterio: 'Autoescalado', ok: 'KEDA sube réplicas de Ingesta / Telemetría Core cuando crece el lag y las baja al drenar.' },
    { criterio: 'Observabilidad', ok: 'Métricas en Grafana/Prometheus, grafo de malla en Kiali y trazas en Jaeger.' },
  ];
}
