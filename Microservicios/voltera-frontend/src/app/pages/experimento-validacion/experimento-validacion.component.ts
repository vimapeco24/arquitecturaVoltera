import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BlockchainService } from '../../services/blockchain.service';
import { CertificadoService } from '../../services/certificado.service';
import { ToastService } from '../../core/toast.service';
import { AnclarRequest } from '../../core/models';

/** Un criterio medible del experimento (lámina 06). */
interface Criterio {
  texto: string;
  estado: 'pendiente' | 'ok' | 'fail' | 'corriendo';
  evidencia?: string;
}

/**
 * Lámina 06 · Experimento de validación (30% de la nota).
 * Ejecuta DESDE LA UI los dos experimentos y muestra cada criterio ✓/✗ con su
 * evidencia, incluida la PRUEBA DE INTEGRIDAD (la cadena detecta la manipulación).
 */
@Component({
  selector: 'app-experimento-validacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './experimento-validacion.component.html',
  styleUrl: '../blockchain/marketplace.scss',
})
export class ExperimentoValidacionComponent {
  private bc = inject(BlockchainService);
  private cert = inject(CertificadoService);
  private toast = inject(ToastService);

  corriendo1 = false;
  corriendo2 = false;

  // Experimento 1 · Oráculo + prueba de integridad
  exp1: Criterio[] = [
    { texto: 'Medición anclada con evidencia verificable (hash on-chain)', estado: 'pendiente' },
    { texto: 'Publicación de evidencia en ≤ 60 s', estado: 'pendiente' },
    { texto: 'Datos originales → integro:true (coincidencia original == verificado)', estado: 'pendiente' },
    { texto: 'PRUEBA DE INTEGRIDAD: datos alterados → integro:false (manipulación DETECTADA)', estado: 'pendiente' },
  ];

  // Experimento 2 · NFT ERC-721
  exp2: Criterio[] = [
    { texto: 'Token emitido con identificador único (tokenId)', estado: 'pendiente' },
    { texto: 'Confirmación de transacción en ≤ 60 s', estado: 'pendiente' },
    { texto: 'Transferencia trazable (billetera → billetera)', estado: 'pendiente' },
    { texto: '0 duplicados: emisión duplicada (mismo medidor+periodo) RECHAZADA', estado: 'pendiente' },
  ];

  private reset(c: Criterio[]): void { c.forEach((x) => { x.estado = 'pendiente'; x.evidencia = undefined; }); }

  // ---------------- Experimento 1 ----------------
  async correrExp1(): Promise<void> {
    this.corriendo1 = true;
    this.reset(this.exp1);
    const txId = 'EXP06-' + Date.now().toString(36).toUpperCase();
    const body: AnclarRequest = {
      transaccionId: txId,
      vendedorId: '0x1111111111111111111111111111111111111111',
      compradorId: '0x2222222222222222222222222222222222222222',
      energiaWh: 1_000_000,
      precioPorKwh: '450',
      valorTotal: '450000',
      liquidadaEn: new Date().toISOString(),
      comunidadId: 'COMUNIDAD-SOLAR-01',
    };
    const t0 = Date.now();
    try {
      const a = await this.bc.anclar(body).toPromise();
      const dur = Math.round((Date.now() - t0) / 1000);
      this.set(this.exp1[0], !!a?.hashCasacion, `hash ${this.corto(a?.hashCasacion)}`);
      this.set(this.exp1[1], dur <= 60, `t=${dur}s`);

      const verOk = await this.bc.verificar(body).toPromise();
      this.set(this.exp1[2], verOk?.integro === true, `integro=${verOk?.integro}`);

      // Prueba de integridad: alteramos energiaWh y verificamos -> debe ser false
      const alterado: AnclarRequest = { ...body, energiaWh: 999_999 };
      const verAlt = await this.bc.verificar(alterado).toPromise();
      this.set(this.exp1[3], verAlt?.integro === false, `datos alterados → integro=${verAlt?.integro}`);

      this.toast.success('Experimento 1 completado', 'Oráculo + prueba de integridad');
    } catch (e) {
      this.exp1.forEach((c) => { if (c.estado === 'pendiente') c.estado = 'fail'; });
      this.toast.error('Experimento 1', 'El gateway no respondió (¿modo demo / cold start?).');
    } finally {
      this.corriendo1 = false;
    }
  }

  // ---------------- Experimento 2 ----------------
  async correrExp2(): Promise<void> {
    this.corriendo2 = true;
    this.reset(this.exp2);
    const medidor = 'MEDIDOR-EXP06-' + Date.now().toString(36).toUpperCase();
    const periodo = 202610;
    const t0 = Date.now();
    try {
      const c = await this.cert.emitir({
        emisor: '0xA0A18C1D64272f9a6a248Deb5106994F5D8AFDf2',
        propietario: '0x1111111111111111111111111111111111111111',
        idMedidor: medidor, consumoNetoMensualWh: 120000, periodo,
      } as any).toPromise();
      const dur = Math.round((Date.now() - t0) / 1000);
      this.set(this.exp2[0], !!c?.tokenId, `tokenId=${c?.tokenId}`);
      this.set(this.exp2[1], dur <= 60, `t=${dur}s`);

      if (c?.tokenId) {
        const t = await this.cert.transferir(c.tokenId, '0x2222222222222222222222222222222222222222').toPromise();
        this.set(this.exp2[2], (t as any)?.propietario?.toLowerCase() === '0x2222222222222222222222222222222222222222', 'propiedad movida');
      }

      // Prueba de unicidad: emisión duplicada (mismo medidor+periodo) debe fallar.
      try {
        await this.cert.emitir({
          emisor: '0xA0A18C1D64272f9a6a248Deb5106994F5D8AFDf2',
          propietario: '0x3333333333333333333333333333333333333333',
          idMedidor: medidor, consumoNetoMensualWh: 99999, periodo,
        } as any).toPromise();
        this.set(this.exp2[3], false, 'se permitió un duplicado (mal)');
      } catch {
        this.set(this.exp2[3], true, 'duplicado rechazado (HTTP 4xx)');
      }
      this.toast.success('Experimento 2 completado', 'NFT único + rechazo de duplicados');
    } catch (e) {
      this.exp2.forEach((c) => { if (c.estado === 'pendiente') c.estado = 'fail'; });
      this.toast.error('Experimento 2', 'El gateway no respondió (¿modo demo / cold start?).');
    } finally {
      this.corriendo2 = false;
    }
  }

  private set(c: Criterio, ok: boolean, evidencia?: string): void {
    c.estado = ok ? 'ok' : 'fail';
    c.evidencia = evidencia;
  }

  get resumen1(): string { return this.conteo(this.exp1); }
  get resumen2(): string { return this.conteo(this.exp2); }
  private conteo(c: Criterio[]): string {
    const ok = c.filter((x) => x.estado === 'ok').length;
    return `${ok}/${c.length}`;
  }

  corto(h?: string | null): string {
    if (!h) return '—';
    return h.length > 16 ? `${h.slice(0, 10)}…${h.slice(-6)}` : h;
  }
}
