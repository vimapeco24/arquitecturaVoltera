import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { gw, SERVICIOS } from '../core/gateway';

/** Solicitud al Copiloto de Siniestros (plataforma GenAI). */
export interface AnalizarSiniestroRequest {
  siniestroId?: string;
  polizaId: string;
  prosumidorId?: string;
  descripcion: string;
  montoReclamacion: number;
  disparadoPorEvento?: boolean;
}

/** Alerta de telemetría de un activo DER (entrada al Agente de diagnóstico). */
export interface DiagnosticarRequest {
  activoId: string;
  tipoActivo: 'PANEL_SOLAR' | 'BATERIA' | 'INVERSOR' | 'CARGADOR_EV' | 'MEDIDOR';
  comunidadId: string;
  metrica: string;
  valor: number;
  umbral: number;
  minutosSinSenal: number;
  descripcion?: string;
  usuario?: string;
}

export interface EvidenciaDto {
  fuente: string;
  titulo: string;
  detalle: string;
}

export interface PasoDto {
  orden: number;
  nodo: string;
  patron: string;
  detalle: string;
  ejecutadoEn: string;
}

/** Recomendación (propuesta) que devuelve el copiloto. */
export interface RecomendacionResponse {
  recomendacionId: string;
  siniestroId: string;
  decisionPropuesta: 'APROBAR' | 'RECHAZAR' | 'SOLICITAR_INFO' | 'ESCALAR_HUMANO';
  montoSugerido: number;
  confianza: 'ALTA' | 'MEDIA' | 'BAJA';
  requiereRevisionHumana: boolean;
  justificacion: string;
  motor: string;
  datosEnmascarados: boolean;
  evidencias: EvidenciaDto[];
  traza: PasoDto[];
  generadaEn: string;
}

/** Diagnóstico de activo DER (propuesta de los 3 agentes). */
export interface DiagnosticoResponse {
  diagnosticoId: string;
  activoId: string;
  tipoActivo: string;
  comunidadId: string;
  severidad: 'INFO' | 'ADVERTENCIA' | 'CRITICA';
  diagnostico: string;
  accionPropuesta: 'MONITOREAR' | 'DESPACHAR_TECNICO' | 'COMANDO_FISICO' | 'CORTAR_SUMINISTRO';
  ordenTrabajo: string;
  requiereAprobacionHumana: boolean;
  motor: string;
  datosEnmascarados: boolean;
  promptVersion: string;
  usuario: string;
  evidencias: EvidenciaDto[];
  traza: PasoDto[];
  generadoEn: string;
}

/** Agente registrado en el catálogo A2A. */
export interface AgenteRegistrado {
  nombre: string;
  dueno: string;
  capacidad: string;
  permisos: string;
  activo: boolean;
}

/** Estado de la plataforma GenAI (proveedor de modelo / motor local). */
export interface EstadoIa {
  servicio: string;
  caso: string;
  agentes: string[];
  assistant?: string;
  mcpServers?: string[];
  gobernanza?: {
    contrato: string;
    firmas: string;
    modoDegradado2de3: boolean;
    topico: string;
  };
  cajas: string[];
  proveedorModeloActivo: boolean;
  motor: string;
}

/** Firma registrada sobre una propuesta de acción 3/3. */
export interface FirmaDto {
  firmante: 'AGENTE_IA' | 'OPERADOR' | 'PROSUMIDOR';
  identidad: string;
  aprueba: boolean;
  razon: string;
  firmadoEn: string;
}

/** Estado de un firmante requerido en las aprobaciones múltiples. */
export interface FirmanteEstadoDto {
  firmante: 'AGENTE_IA' | 'OPERADOR' | 'PROSUMIDOR';
  estado: 'FIRMO' | 'PENDIENTE' | 'RECHAZO';
  identidad: string | null;
  razon: string | null;
  firmadoEn: string | null;
  txHash: string | null;
  explorerUrl: string | null;
  modo: 'ON_CHAIN' | 'OFF_CHAIN' | null;
}
/** Vista de lectura de las aprobaciones múltiples (GET .../aprobaciones). */
export interface EstadoAprobacionesDto {
  propuestaId: string;
  activoId: string;
  comando: string;
  estado: string;
  estadoGlobal: string;
  firmasRequeridas: number;
  firmasAprobadas: number;
  progreso: string;
  modo: 'ON_CHAIN' | 'OFF_CHAIN';
  red: string | null;
  contrato: string | null;
  explorerBase: string | null;
  creadaEn: string;
  venceEn: string;
  firmantes: FirmanteEstadoDto[];
  faltantes: string[];
  txEjecucion: string | null;
  txEjecucionUrl: string | null;
}
/** Propuesta de acción gobernada por el contrato multi-firma 3/3. */
export interface PropuestaResponse {
  propuestaId: string;
  diagnosticoId: string;
  activoId: string;
  comunidadId: string;
  tipoAccion: string;
  comando: string;
  severidad: string;
  estado: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'VENCIDA' | 'EJECUTADA';
  firmas: string;
  siguienteFirmante: string | null;
  hashDiagnostico: string;
  versionPrompt: string;
  versionModelo: string;
  resultadoEjecucion: string | null;
  creadaEn: string;
  venceEn: string;
  firmasDetalle: FirmaDto[];
}

/** Respuesta del Assistant (acotada al diagnóstico, sin herramientas de acción). */
export interface RespuestaAsistente {
  diagnosticoId: string;
  respuesta: string;
  evidencias: EvidenciaDto[];
  promptVersion: string;
  noPersuasion: boolean;
  generadoEn: string;
}

/** Hito de la línea de tiempo de auditoría de una propuesta. */
export interface EventoAuditoriaDto {
  instante: string;
  tipo: string;
  actor: string;
  detalle: string;
  tx: string | null;
}
export interface AuditoriaPropuestaDto {
  propuestaId: string;
  estado: string;
  hashDiagnostico: string;
  versionPrompt: string;
  versionModelo: string;
  eventos: EventoAuditoriaDto[];
}
/** Indicadores agregados de la gobernanza 3/3. */
export interface MetricasGobernanzaDto {
  total: number;
  porEstado: Record<string, number>;
  ejecutadas: number;
  ejecutadasSin3de3: number;
  tasaEjecucion: number;
  tasaRechazo: number;
  promedioFirmas: number;
}

/** Verificación ecrecover de una firma ECDSA (EIP-191). */
export interface VerificacionFirmaDto {
  rol: 'AGENTE_IA' | 'OPERADOR' | 'PROSUMIDOR';
  direccion: string;
  direccionRecuperada: string | null;
  firma: string;
  metodo: 'CUSTODIA' | 'BILLETERA';
  mensaje: string;
  valida: boolean;
  firmadoEn: string;
  detalle: string;
}
/** Aviso de la bandeja del Agente Notificador. */
export interface NotificacionDto {
  id: string;
  instante: string;
  destinatario: 'OPERADOR' | 'PROSUMIDOR' | 'TODOS';
  tipo: 'FIRMA_PENDIENTE' | 'ESCALACION' | 'DESENLACE';
  propuestaId: string;
  activoId: string;
  estado: string;
  firmas: string;
  mensaje: string;
  urgente: boolean;
  leida: boolean;
  webhook: string;
}
/** Capacidad del módulo con su estado calculado en el backend. */
export interface CapacidadDto {
  clave: string;
  componente: string;
  estado: 'OPERATIVO' | 'LOCAL' | 'PENDIENTE';
  detalle: string;
  evidencia: string;
  requiere: string;
}
export interface CapacidadesDto {
  calculadoEn: string;
  resumen: Record<string, number>;
  capacidades: CapacidadDto[];
  direccionesFirmantes: Record<string, string>;
}
/** Resultado de un caso del dataset de Evals. */
export interface CasoEvalDto {
  id: string; descripcion: string; tipoActivo: string; metrica: string; valor: string; umbral: string;
  minutosSinSenal: number; severidadEsperada: string; severidadObtenida: string;
  accionEsperada: string; accionObtenida: string; humanoEsperado: boolean; humanoObtenido: boolean;
  severidadOk: boolean; accionOk: boolean; citasOk: boolean; fuentesRag: number; fuentesMcp: number;
  ms: number; nota: string;
}
export interface ReporteEvalsDto {
  ejecutadoEn: string;
  duracionMs: number;
  dataset: string;
  totalCasos: number;
  familias: ResultadoEval[];
  casos: CasoEvalDto[];
}
/** Fragmento recuperado por el RAG BM25. */
export interface ResultadoRagDto {
  docId: string; titulo: string; categoria: string; seccion: string; texto: string;
  fuente: string; score: number; terminos: string[];
}
export interface DocumentoRagDto {
  id: string; titulo: string; categoria: string; activos: string[]; fuente: string; secciones: number;
}
export interface CorpusRagDto {
  algoritmo: string;
  documentos: DocumentoRagDto[];
  fragmentos: number;
}

/** Resultado de Evals por versión de prompt (Tema 5). */
export interface ResultadoEval {
  agente: string;
  version: string;
  casos: number;
  acierto: number;
  citasValidas: number;
  comandosInventados: number;
  apta: boolean;
}

/**
 * Cliente del ia-service (Copiloto de Siniestros).
 *
 * Resolución de URL (mismo patrón que BlockchainService):
 *  - Si `environment.iaBase` está definido (URL pública del ia-service en Render),
 *    llama DIRECTO — el servidor on-premise está saturado y esta capa corre del
 *    lado de la nube, fuera del WSO2 (que vive en LAN privada).
 *  - Si está vacío (dev local), cae al gateway WSO2: {gatewayBase}/ia/v1/...
 */
@Injectable({ providedIn: 'root' })
export class IaService {
  constructor(private http: HttpClient) {}

  /** Construye la URL del endpoint, directa a Render o vía WSO2 según config. */
  private url(path: string): string {
    const base = environment.iaBase?.trim();
    if (base) {
      const clean = path.startsWith('/') ? path.slice(1) : path;
      return `${base.replace(/\/$/, '')}/${clean}`;
    }
    return gw(SERVICIOS.ia, path);
  }

  estado(): Observable<EstadoIa> {
    return this.http.get<EstadoIa>(this.url('api/v1/ia/estado'));
  }

  analizarSiniestro(body: AnalizarSiniestroRequest): Observable<RecomendacionResponse> {
    return this.http.post<RecomendacionResponse>(
      this.url('api/v1/ia/copilotos/siniestros/analizar'),
      body,
    );
  }

  /** Agente de diagnóstico de activos DER (caso del Grupo 5). */
  diagnosticarDer(body: DiagnosticarRequest): Observable<DiagnosticoResponse> {
    return this.http.post<DiagnosticoResponse>(this.url('api/v1/ia/diagnostico/der'), body);
  }

  /** Catálogo A2A de agentes (Agent Registry). */
  agentes(): Observable<AgenteRegistrado[]> {
    return this.http.get<AgenteRegistrado[]>(this.url('api/v1/ia/agentes'));
  }

  // ── Gobernanza multi-firma 3/3 (contrato MultiFirmaAccionDER) ──────────────

  /** El Agente de Acción crea la propuesta (Firma 1) a partir de un diagnóstico. */
  proponerAccion(diagnosticoId: string): Observable<PropuestaResponse> {
    return this.http.post<PropuestaResponse>(
      this.url(`api/v1/ia/gobernanza/propuestas/${diagnosticoId}`), {});
  }

  /** Firma 2: el operador valida técnicamente (aprueba). */
  firmarOperador(propuestaId: string, identidad: string, razon: string, direccion?: string, firma?: string): Observable<PropuestaResponse> {
    return this.http.post<PropuestaResponse>(
      this.url(`api/v1/ia/gobernanza/propuestas/${propuestaId}/firmas/operador`), { identidad, razon, direccion, firma });
  }

  /** Firma 3: el prosumidor consiente (aprueba). */
  firmarProsumidor(propuestaId: string, identidad: string, razon: string, direccion?: string, firma?: string): Observable<PropuestaResponse> {
    return this.http.post<PropuestaResponse>(
      this.url(`api/v1/ia/gobernanza/propuestas/${propuestaId}/firmas/prosumidor`), { identidad, razon, direccion, firma });
  }

  /** Rechazo del operador (razón obligatoria). */
  rechazarOperador(propuestaId: string, identidad: string, razon: string, direccion?: string, firma?: string): Observable<PropuestaResponse> {
    return this.http.post<PropuestaResponse>(
      this.url(`api/v1/ia/gobernanza/propuestas/${propuestaId}/rechazos/operador`), { identidad, razon, direccion, firma });
  }

  /** Rechazo del prosumidor (razón obligatoria). */
  rechazarProsumidor(propuestaId: string, identidad: string, razon: string, direccion?: string, firma?: string): Observable<PropuestaResponse> {
    return this.http.post<PropuestaResponse>(
      this.url(`api/v1/ia/gobernanza/propuestas/${propuestaId}/rechazos/prosumidor`), { identidad, razon, direccion, firma });
  }

  /** Aprobaciones múltiples: firmantes, firmas con tx/explorador, faltantes y modo ON_CHAIN / OFF_CHAIN. */
  aprobaciones(propuestaId: string): Observable<EstadoAprobacionesDto> {
    return this.http.get<EstadoAprobacionesDto>(
      this.url(`api/v1/ia/gobernanza/propuestas/${propuestaId}/aprobaciones`));
  }

  /** Línea de tiempo de auditoría (quién, qué, cuándo, tx) de una propuesta. */
  auditoria(propuestaId: string): Observable<AuditoriaPropuestaDto> {
    return this.http.get<AuditoriaPropuestaDto>(
      this.url(`api/v1/ia/gobernanza/propuestas/${propuestaId}/auditoria`));
  }

  /** Indicadores agregados de la gobernanza (estados, tasas, garantía fail-closed). */
  metricasGobernanza(): Observable<MetricasGobernanzaDto> {
    return this.http.get<MetricasGobernanzaDto>(this.url('api/v1/ia/gobernanza/metricas'));
  }

  /** Caduca las propuestas cuyo TTL expiró sin 3/3 (fail-closed). */
  caducarVencidas(): Observable<PropuestaResponse[]> {
    return this.http.post<PropuestaResponse[]>(this.url('api/v1/ia/gobernanza/vencidas'), {});
  }

  propuestas(): Observable<PropuestaResponse[]> {
    return this.http.get<PropuestaResponse[]>(this.url('api/v1/ia/gobernanza/propuestas'));
  }

  // ── Assistant (conversación acotada antes de firmar) ───────────────────────

  preguntarAsistente(diagnosticoId: string, audiencia: 'PROSUMIDOR' | 'OPERADOR',
                     texto: string, usuario?: string): Observable<RespuestaAsistente> {
    return this.http.post<RespuestaAsistente>(
      this.url('api/v1/ia/asistente/preguntar'),
      { diagnosticoId, audiencia, texto, usuario });
  }

  // ── Evals & Tracing (Tema 5) ───────────────────────────────────────────────

  evals(): Observable<ResultadoEval[]> {
    return this.http.get<ResultadoEval[]>(this.url('api/v1/ia/evals'));
  }

  // ── Integraciones reales: firmas ECDSA, notificaciones, RAG, Evals y capacidades ──

  /** Verificación ecrecover de las firmas de la propuesta. */
  verificarFirmas(propuestaId: string): Observable<VerificacionFirmaDto[]> {
    return this.http.get<VerificacionFirmaDto[]>(
      this.url(`api/v1/ia/gobernanza/propuestas/${propuestaId}/firmas/verificacion`));
  }

  /** Mensaje canónico que el rol firma con su billetera (personal_sign). */
  mensajeFirma(propuestaId: string, rol: string, decision: 'APRUEBA' | 'RECHAZA'): Observable<{ mensaje: string; rol: string; estandar: string }> {
    return this.http.get<{ mensaje: string; rol: string; estandar: string }>(
      this.url(`api/v1/ia/gobernanza/propuestas/${propuestaId}/mensaje-firma?rol=${rol}&decision=${decision}`));
  }

  notificaciones(destinatario?: string, propuestaId?: string): Observable<NotificacionDto[]> {
    const q: string[] = [];
    if (destinatario) q.push('destinatario=' + encodeURIComponent(destinatario));
    if (propuestaId) q.push('propuestaId=' + encodeURIComponent(propuestaId));
    return this.http.get<NotificacionDto[]>(this.url('api/v1/ia/notificaciones' + (q.length ? '?' + q.join('&') : '')));
  }

  marcarLeida(id: string): Observable<NotificacionDto> {
    return this.http.post<NotificacionDto>(this.url(`api/v1/ia/notificaciones/${id}/leida`), {});
  }

  capacidades(): Observable<CapacidadesDto> {
    return this.http.get<CapacidadesDto>(this.url('api/v1/ia/capacidades'));
  }

  evalsReporte(forzar = false): Observable<ReporteEvalsDto> {
    return this.http.get<ReporteEvalsDto>(this.url('api/v1/ia/evals/reporte' + (forzar ? '?forzar=true' : '')));
  }

  corpusRag(): Observable<CorpusRagDto> {
    return this.http.get<CorpusRagDto>(this.url('api/v1/ia/conocimiento/documentos'));
  }

  buscarRag(q: string, tipoActivo?: string, k = 5): Observable<ResultadoRagDto[]> {
    let u = `api/v1/ia/conocimiento/buscar?q=${encodeURIComponent(q)}&k=${k}`;
    if (tipoActivo) u += '&tipoActivo=' + encodeURIComponent(tipoActivo);
    return this.http.get<ResultadoRagDto[]>(this.url(u));
  }
}
