import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { gw, SERVICIOS } from '../core/gateway';
import { Anclaje, AnclarRequest, ComunidadResumen, RedInfo, ResultadoVerificacion } from '../core/models';

/**
 * Cliente del servicio de borde blockchain-gateway (8101). Ancla en Polygon Amoy
 * el hash de las liquidaciones P2P, consulta su estado on-chain e informa de la
 * red/contrato.
 *
 * Resolución de URL:
 *  - Si `environment.blockchainBase` está definido (p. ej. la URL pública del
 *    gateway en Render), llama DIRECTO a esa URL — el gateway blockchain vive
 *    fuera del server WSO2 (que está en LAN privada e inalcanzable desde Vercel).
 *  - Si está vacío, cae al gateway WSO2 normal: {gatewayBase}/blockchain/v1/...
 */
@Injectable({ providedIn: 'root' })
export class BlockchainService {
  constructor(private http: HttpClient) {}

  /** Construye la URL del endpoint, directa a Render o vía WSO2 según config. */
  private url(path: string): string {
    const base = environment.blockchainBase?.trim();
    if (base) {
      const clean = path.startsWith('/') ? path.slice(1) : path;
      return `${base.replace(/\/$/, '')}/${clean}`;
    }
    return gw(SERVICIOS.blockchain, path);
  }

  anclar(body: AnclarRequest): Observable<Anclaje> {
    return this.http.post<Anclaje>(this.url('api/v1/anclajes'), body);
  }

  obtener(transaccionId: string): Observable<Anclaje> {
    return this.http.get<Anclaje>(this.url(`api/v1/anclajes/${encodeURIComponent(transaccionId)}`));
  }

  listar(): Observable<Anclaje[]> {
    return this.http.get<Anclaje[]>(this.url('api/v1/anclajes'));
  }

  red(): Observable<RedInfo> {
    return this.http.get<RedInfo>(this.url('api/v1/anclajes/red'));
  }

  verificar(body: AnclarRequest): Observable<ResultadoVerificacion> {
    return this.http.post<ResultadoVerificacion>(this.url('api/v1/anclajes/verificar'), body);
  }

  /** Resumen agregado por comunidad energética (lo calcula el gateway). */
  comunidades(): Observable<ComunidadResumen[]> {
    return this.http.get<ComunidadResumen[]>(this.url('api/v1/anclajes/comunidades'));
  }
}
