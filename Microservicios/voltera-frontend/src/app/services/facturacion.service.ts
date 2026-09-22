import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { gw, SERVICIOS } from '../core/gateway';
import { EmitirFacturaRequest, Factura } from '../core/models';

@Injectable({ providedIn: 'root' })
export class FacturacionService {
  constructor(private http: HttpClient) {}

  emitir(body: EmitirFacturaRequest): Observable<Factura> {
    return this.http.post<Factura>(gw(SERVICIOS.facturacion, 'api/v1/facturas'), body);
  }

  obtener(id: string): Observable<Factura> {
    return this.http.get<Factura>(gw(SERVICIOS.facturacion, `api/v1/facturas/${id}`));
  }

  porProsumidor(prosumidorId: string): Observable<Factura[]> {
    return this.http.get<Factura[]>(
      gw(SERVICIOS.facturacion, `api/v1/facturas?prosumidorId=${encodeURIComponent(prosumidorId)}`),
    );
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(gw(SERVICIOS.facturacion, `api/v1/facturas/${id}`));
  }

  actualizar(id: string, body: EmitirFacturaRequest): Observable<Factura> {
    return this.http.put<Factura>(gw(SERVICIOS.facturacion, `api/v1/facturas/${id}`), body);
  }
}
