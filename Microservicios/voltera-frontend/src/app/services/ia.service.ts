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
  /** Instante en que el dispositivo tomó la lectura (mide el tiempo de detección). */
  observadoEn?: string;
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
  intencion?: string;
  fueraDeAlcance?: boolean;
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
  ms: number; nota: string; origen: 'DATASET' | 'HUMAN_REVIEW';
}
export interface ReporteEvalsDto {
  ejecutadoEn: string;
  duracionMs: number;
  dataset: string;
  totalCasos: number;
  familias: ResultadoEval[];
  casos: CasoEvalDto[];
  versionDiagnosticador: string;
  casosRetroalimentacion: number;
}
/** Fragmento recuperado por el RAG BM25. */
export interface ResultadoRagDto {
  docId: string; titulo: string; categoria: string; seccion: string; texto: string;
  fuente: string; score: number; terminos: string[]; bm25: number; coseno: number; cobertura: number; acceso: string;
}
export interface DocumentoRagDto {
  id: string; titulo: string; categoria: string; activos: string[]; fuente: string; secciones: number;
  acceso: string; origen: string;
}
// ── Gobierno y operación ────────────────────────────────────────────────────
export interface VersionPromptDto {
  familia: string; version: string; estado: 'PRODUCCION' | 'CANDIDATA' | 'RETIRADA'; descripcion: string;
  aprobadaPor: string | null; aprobadaEn: string | null; plantilla: string;
}
export interface CambioVersionDto { instante: string; familia: string; desde: string; hacia: string; tipo: string; responsable: string; motivo: string; }
export interface PromptsDto {
  familias: { familia: string; produccion: string; versiones: VersionPromptDto[] }[];
  historial: CambioVersionDto[];
  compuerta: string;
}
export interface ComparacionVersionDto {
  version: string; estado: string; aciertoMonitor: number; aciertoDiagnosticador: number; aciertoAccion: number;
  citas: number; inventados: number; apta: boolean; fallidos: string[];
}
export interface PromocionDto {
  resultado: 'PROMOVIDA' | 'RECHAZADA'; motivo?: string; aciertoProduccion?: number; aciertoCandidata?: number;
  compuerta?: ResultadoEval[]; produccion?: VersionPromptDto;
}
export interface AgenteRegistroDto {
  agentes: AgenteRegistrado[];
  historial: { instante: string; agente: string; activo: boolean; responsable: string; motivo: string }[];
}
export interface RevisionDto {
  id: string; instante: string; diagnosticoId: string; revisor: string; severidadAgente: string; severidadCorrecta: string;
  accionAgente: string; accionCorrecta: string; deAcuerdo: boolean; comentario: string; casoId: string;
}
export interface ResumenRevisionDto { revisiones: number; deAcuerdo: number; corregidas: number; tasaAcuerdo: number; casosAgregadosAEvals: number; }
export interface RegistroAuditoriaDto { id: string; instante: string; tipo: string; actor: string; recurso: string; detalle: string; version: string | null; }
export interface AuditoriaDto {
  politica: { retencionDias: number; maximoRegistros: number; redaccion: string; seRegistra: string[]; total: number; porTipo: Record<string, number> };
  registros: RegistroAuditoriaDto[];
}
export interface PoliticaGuardrailDto { id: string; tipo: string; version: string; descripcion: string; accion: string; activaciones: number; }
export interface DecisionGatewayDto {
  instante: string; funcionalidad: string; clasificacion: string; ruta: string; modelo: string; motivo: string;
  tokensEntrada: number; tokensSalida: number; costoUsd: number; cache: boolean; latenciaMs: number;
}
export interface ModelGatewayDto {
  proveedor: string; modelo: string; proveedorActivo: boolean; zeroRetention: boolean; maxTokens: number; timeoutMs: number;
  presupuestoTokens: number; tokensUsados: number; costoUsd: number; precioUsd1kTokens: number; solicitudes: number;
  aciertosCache: number; porRuta: Record<string, number>; reglas: string[]; decisiones: DecisionGatewayDto[];
}
export interface MetricaFuncionalidadDto { nombre: string; llamadas: number; errores: number; tasaError: number; p50Ms: number; p95Ms: number; maxMs: number; }
export interface ObservabilidadDto {
  kpiNegocio: { diagnosticos: number; mttdSegundos: number; p95DeteccionSegundos: number; lineaBaseMinutos: number; metaMinutos: number; cumpleMeta: boolean; nota: string };
  funcionalidades: MetricaFuncionalidadDto[];
  pasosFlujo: MetricaFuncionalidadDto[];
  modelGateway: { solicitudes: number; tokensUsados: number; costoUsd: number; porRuta: Record<string, number> };
  guardrails: PoliticaGuardrailDto[];
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

  buscarRag(q: string, tipoActivo?: string, k = 5, rol = 'OPERADOR'): Observable<ResultadoRagDto[]> {
    let u = `api/v1/ia/conocimiento/buscar?q=${encodeURIComponent(q)}&k=${k}&rol=${rol}`;
    if (tipoActivo) u += '&tipoActivo=' + encodeURIComponent(tipoActivo);
    return this.http.get<ResultadoRagDto[]>(this.url(u));
  }

  ingerirDocumento(contenido: string, responsable: string): Observable<DocumentoRagDto> {
    return this.http.post<DocumentoRagDto>(this.url('api/v1/ia/conocimiento/documentos'), { contenido, responsable });
  }

  retirarDocumento(id: string, responsable: string): Observable<unknown> {
    return this.http.delete(this.url(`api/v1/ia/conocimiento/documentos/${encodeURIComponent(id)}?responsable=${encodeURIComponent(responsable)}`));
  }

  // ── Gobierno y operación ─────────────────────────────────────────────────

  prompts(): Observable<PromptsDto> {
    return this.http.get<PromptsDto>(this.url('api/v1/ia/prompts'));
  }

  comparacionVersiones(): Observable<ComparacionVersionDto[]> {
    return this.http.get<ComparacionVersionDto[]>(this.url('api/v1/ia/prompts/diagnosticador/comparacion'));
  }

  promoverPrompt(familia: string, version: string, responsable: string, motivo: string): Observable<PromocionDto> {
    return this.http.post<PromocionDto>(this.url(`api/v1/ia/prompts/${familia}/promover`), { version, responsable, motivo });
  }

  rollbackPrompt(familia: string, responsable: string, motivo: string): Observable<VersionPromptDto> {
    return this.http.post<VersionPromptDto>(this.url(`api/v1/ia/prompts/${familia}/rollback`), { responsable, motivo });
  }

  registroAgentes(): Observable<AgenteRegistroDto> {
    return this.http.get<AgenteRegistroDto>(this.url('api/v1/ia/agentes/registro'));
  }

  cambiarEstadoAgente(nombre: string, activo: boolean, responsable: string, motivo: string): Observable<AgenteRegistrado> {
    return this.http.post<AgenteRegistrado>(this.url(`api/v1/ia/agentes/${encodeURIComponent(nombre)}/estado`), { activo, responsable, motivo });
  }

  retroalimentar(diagnosticoId: string, body: { revisor: string; deAcuerdo: boolean; severidadCorrecta?: string; accionCorrecta?: string; comentario?: string }):
      Observable<{ revision: RevisionDto; casoEval: unknown; resumen: ResumenRevisionDto }> {
    return this.http.post<{ revision: RevisionDto; casoEval: unknown; resumen: ResumenRevisionDto }>(
      this.url(`api/v1/ia/diagnostico/${diagnosticoId}/retroalimentacion`), body);
  }

  retroalimentaciones(): Observable<{ resumen: ResumenRevisionDto; revisiones: RevisionDto[] }> {
    return this.http.get<{ resumen: ResumenRevisionDto; revisiones: RevisionDto[] }>(this.url('api/v1/ia/retroalimentacion'));
  }

  auditoriaIa(tipo?: string, limite = 100): Observable<AuditoriaDto> {
    return this.http.get<AuditoriaDto>(this.url(`api/v1/ia/auditoria?limite=${limite}` + (tipo ? '&tipo=' + tipo : '')));
  }

  guardrails(): Observable<PoliticaGuardrailDto[]> {
    return this.http.get<PoliticaGuardrailDto[]>(this.url('api/v1/ia/guardrails'));
  }

  modelGateway(): Observable<ModelGatewayDto> {
    return this.http.get<ModelGatewayDto>(this.url('api/v1/ia/model-gateway'));
  }

  observabilidad(): Observable<ObservabilidadDto> {
    return this.http.get<ObservabilidadDto>(this.url('api/v1/ia/observabilidad'));
  }
}
