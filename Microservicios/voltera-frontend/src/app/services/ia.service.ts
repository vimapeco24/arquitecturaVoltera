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
  cajas: string[];
  proveedorModeloActivo: boolean;
  motor: string;
  zeroRetention: boolean;
  humanInTheLoop: boolean;
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
}
