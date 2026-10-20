import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { Anclaje, AnclarRequest, RedInfo, ResultadoVerificacion } from '../core/models';

/**
 * Cliente del servicio de borde blockchain-gateway (8101). Ancla en Polygon Amoy
 * el hash de las liquidaciones P2P, consulta su estado on-chain e informa de la
 * red/contrato.
 */
@Injectable({ providedIn: 'root' })
export class BlockchainService {
  constructor(private http: HttpClient) {}

  anclar(body: AnclarRequest): Observable<Anclaje> {
    return this.http.post<Anclaje>(gw(SERVICIOS.blockchain, 'api/v1/anclajes'), body);
  }

  obtener(transaccionId: string): Observable<Anclaje> {
    return this.http.get<Anclaje>(
      gw(SERVICIOS.blockchain, `api/v1/anclajes/${encodeURIComponent(transaccionId)}`),
    );
  }

  listar(): Observable<Anclaje[]> {
    return this.http.get<Anclaje[]>(gw(SERVICIOS.blockchain, 'api/v1/anclajes'));
  }

  red(): Observable<RedInfo> {
    return this.http.get<RedInfo>(gw(SERVICIOS.blockchain, 'api/v1/anclajes/red'));
  }

  verificar(body: AnclarRequest): Observable<ResultadoVerificacion> {
    return this.http.post<ResultadoVerificacion>(
      gw(SERVICIOS.blockchain, 'api/v1/anclajes/verificar'),
      body,
    );
  }
}
