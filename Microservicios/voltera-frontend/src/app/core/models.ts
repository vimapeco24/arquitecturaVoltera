/**
 * Modelos TypeScript que reflejan los DTOs de respuesta de los microservicios
 * Voltera. Los campos coinciden 1:1 con los `record` de Java expuestos por cada
 * adaptador REST.
 */

/* ---------------- Tarifas (8084) ---------------- */
export interface Franja {
  horaInicio: number;
  horaFin: number;
  precioKwh: number;
}
export interface Tarifa {
  id: string;
  nombre: string;
  franjas: Franja[];
}
export interface CrearTarifaRequest {
  nombre: string;
  franjas: Franja[];
}
export interface PrecioHora {
  hora: number;
  precioKwh: number;
}

/* ---------------- Volumetría (8083) ---------------- */
export type Direccion = 'CONSUMO' | 'GENERACION';
export type EstadoLectura = 'VALIDA' | 'SOSPECHOSA' | 'RECHAZADA';
export interface Lectura {
  id: string;
  medidorId: string;
  kwh: number;
  direccion: string;
  estado: string;
  valida: boolean;
  capturadaEn: string;
}
export interface IngestarLecturaRequest {
  medidorId: string;
  kwh: number;
  direccion: Direccion;
  capturadaEn: string;
}

/* ---------------- Liquidación P2P (8081) ---------------- */
export interface TransaccionP2P {
  id: string;
  ordenVentaId: string;
  ordenCompraId: string;
  vendedorId: string;
  compradorId: string;
  energiaKwh: number;
  precioCasacionKwh: number;
  valorTotal: number;
  liquidadaEn: string;
  // Resultado de la casación
  tipoCasacion: string; // TOTAL | PARCIAL
  casacionTotal: boolean;
  energiaSolicitadaVentaKwh: number;
  energiaSolicitadaCompraKwh: number;
  pendienteVentaKwh: number;
  pendienteCompraKwh: number;
}
export interface EmparejarRequest {
  vendedorId: string;
  kwhVenta: number;
  precioVenta: number;
  compradorId: string;
  kwhCompra: number;
  precioCompra: number;
}

/* ---------------- Liquidación Mensual (8082) ---------------- */
export type EstadoLiquidacion = 'ABIERTA' | 'CERRADA';
export interface LiquidacionMensual {
  id: string;
  prosumidorId: string;
  periodo: string;
  estado: string;
  cantidadMovimientos: number;
  totalConsumoKwh: number;
  totalExcedenteKwh: number;
  netoKwh: number;
  saldoAFavor: boolean;
  // Valoración económica (COP)
  precioConsumoKwh: number;
  precioExcedenteKwh: number;
  cargoConsumo: number;
  creditoExcedente: number;
  netoCop: number;
  creadaEn: string;
  cerradaEn: string | null;
}
export interface AbrirLiquidacionRequest {
  prosumidorId: string;
  anio: number;
  mes: number;
  precioConsumoKwh?: number;
  precioExcedenteKwh?: number;
}
export interface MovimientoRequest {
  tipo: 'CONSUMO' | 'EXCEDENTE';
  kwh: number;
}

/* ---------------- Facturación (8088) ---------------- */
export type EstadoFactura = 'EMITIDA' | 'PAGADA' | 'ANULADA';
export interface Factura {
  id: string;
  prosumidorId: string;
  periodoInicio: string;
  periodoFin: string;
  kwhConsumidos: number;
  kwhInyectados: number;
  consumoNetoKwh: number;
  excedenteKwh: number;
  cargoConsumo: number;
  creditoExcedente: number;
  total: number;
  saldoAFavor: boolean;
  estado: string;
  emitidaEn: string;
}
export interface EmitirFacturaRequest {
  prosumidorId: string;
  periodoInicio: string;
  periodoFin: string;
  kwhConsumidos: number;
  kwhInyectados: number;
  precioConsumoKwh: number;
  precioExcedenteKwh: number;
  // OPCIONALES: si se indica tarifaId, facturación consulta el precio vigente a
  // tarifas-service (salto real entre microservicios, visible en Jaeger/Kiali).
  tarifaId?: string;
  hora?: number;
}

/* ---------------- Pagos (8085) ---------------- */
export type EstadoPago = 'PENDIENTE' | 'PROCESADO' | 'FALLIDO';
export type TipoPago = 'COBRO_FACTURA' | 'PAGO_EXCEDENTE';
export interface Pago {
  id: string;
  prosumidorId: string;
  referencia: string;
  tipo: string;
  monto: number;
  estado: string;
  motivoFallo: string | null;
  creadaEn: string;
}
export interface ProcesarPagoRequest {
  prosumidorId: string;
  referencia: string;
  tipo: TipoPago;
  monto: number;
}

/* ---------------- Salud ---------------- */
export interface HealthStatus {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
}

/* ---------------- Siniestros (8091) — Entrega 3 EDA ---------------- */
export type EstadoSiniestro = 'REPORTADO' | 'EN_PERITAJE' | 'APROBADO' | 'RECHAZADO';
export interface Siniestro {
  id: string;
  polizaId: string;
  prosumidorId: string;
  descripcion: string;
  montoReclamacion: number;
  estado: string;
  fechaOcurrencia: string;
  reportadoEn: string;
}
export interface ReportarSiniestroRequest {
  polizaId: string;
  prosumidorId: string;
  descripcion: string;
  montoReclamacion: number;
  fechaOcurrencia: string;
}
export interface SiniestroAprobadoEvento {
  eventId: string;
  siniestroId: string;
  polizaId: string;
  prosumidorId: string;
  montoAprobado: number;
  descripcion: string;
  aprobadoEn: string;
  emitidoEn: string;
}

/* ---------------- Reaseguro (8092) — Entrega 3 EDA / CQRS ---------------- */
export type EstadoCesion = 'PENDIENTE' | 'CEDIDA' | 'RECHAZADA_REASEGURO';
export interface CesionVista {
  id: string;
  siniestroId: string;
  polizaId: string;
  prosumidorId: string;
  montoAprobado: number;
  montoCedido: number;
  porcentajeCedido: number;
  estado: string;
  creadaEn: string;
}
export interface ReaseguroStats {
  totalCesiones: number;
  totalCedidas: number;
  totalPendientes: number;
  totalRechazadas: number;
  montoTotalAprobado: number;
  montoTotalCedido: number;
}

/* ---------------- Telemetría (8093) — EDA / IoT store-and-forward ---------------- */
export type EstadoMedidor = 'INACTIVO' | 'ACTIVO' | 'SUSPENDIDO';
export interface Medidor {
  id: string;
  prosumidorId: string;
  umbralKwh: number;
  estado: string;
  registradoEn: string;
}
export interface RegistrarMedidorRequest {
  prosumidorId: string;
  umbralKwh: number;
}
export interface IngestarConsumoRequest {
  consumoKwh: number;
  capturadaEn?: string;
}
/** Respuesta de la ingesta: indica si el evento fue al broker o al buffer offline. */
export interface IngestaResponse {
  eventId: string;
  medidorId: string;
  prosumidorId: string;
  consumoKwh: number;
  umbralKwh: number;
  consumoExtra: boolean;
  publicadoEnBroker: boolean;
  enBufferOffline: boolean;
  ocurridoEn: string;
}
export interface BufferResponse {
  reenviados: number;
  pendientes: number;
}

/* ---------------- Tarifa-Eventos (8094) — EDA / CQRS consumidor ---------------- */
export type EstadoCargo = 'SIN_CARGO' | 'LIQUIDADO';
export interface CargoVista {
  id: string;
  medidorId: string;
  prosumidorId: string;
  consumoKwh: number;
  umbralKwh: number;
  excedenteKwh: number;
  precioExtraKwh: number;
  montoCargo: number;
  estado: string;
  creadoEn: string;
}
export interface TarifaEventosStats {
  totalCargos: number;
  totalLiquidados: number;
  totalSinCargo: number;
  montoTotalCargado: number;
  excedenteTotalKwh: number;
}
export interface SimularConsumoEvento {
  eventId: string;
  medidorId: string;
  prosumidorId: string;
  consumoKwh: number;
  umbralKwh: number;
  consumoExtra: boolean;
  ocurridoEn?: string;
  emitidoEn?: string;
}
export interface ActivarMedidorRequest {
  medidorId: string;
  prosumidorId: string;
  umbralKwh: number;
}
export interface NotificacionLiquidada {
  notificacionId: string;
  medidorId: string;
  prosumidorId: string;
  tipo: string;
  cargoFijoMensual: number;
  precioBaseKwh: number;
  umbralKwh: number;
  precioExtraKwh: number;
  mensaje: string;
  generadaEn: string;
}
