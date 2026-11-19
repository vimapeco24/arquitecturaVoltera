import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  IaService, PropuestaResponse, EstadoAprobacionesDto, AuditoriaPropuestaDto, MetricasGobernanzaDto,
} from '../../services/ia.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';

interface TxOnChain { etiqueta: string; txHash: string; bloque: number; gas: string; eventos: string[]; }
interface EvidenciaOnChain {
  red: string; chainId: number; contrato: string; nota: string;
  transacciones: TxOnChain[]; criterios: { ok: boolean; texto: string }[]; ok: number; fail: number;
}

/**
 * Consola de gobernanza on-chain (multi-firma 3/3): lista de propuestas, máquina de
 * estados del contrato, firmantes con progreso circular, auditoría y evidencia EVM real
 * (transacciones del contrato desplegado en red local de Hardhat).
 */
@Component({
  selector: 'app-gobernanza-onchain',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './gobernanza-onchain.component.html',
  styleUrl: './gobernanza-onchain.component.scss',
})
export class GobernanzaOnchainComponent implements OnInit {
  private readonly svc = inject(IaService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  propuestas: PropuestaResponse[] = [];
  seleccionada?: PropuestaResponse;
  aprob?: EstadoAprobacionesDto;
  audit?: AuditoriaPropuestaDto;
  metricas?: MetricasGobernanzaDto;
  evidencia?: EvidenciaOnChain;
  filtro = 'TODAS';
  cargando = false;
  ocupado = false;
  readonly circ = 2 * Math.PI * 52;

  /** Nodos de la máquina de estados del contrato MultiFirmaAccionDER. */
  readonly estados = [
    { k: 'PENDIENTE', x: 40, y: 70 }, { k: 'APROBADA', x: 230, y: 70 }, { k: 'EJECUTADA', x: 420, y: 70 },
    { k: 'RECHAZADA', x: 135, y: 150 }, { k: 'VENCIDA', x: 325, y: 150 },
  ];
  readonly filtros = ['TODAS', 'PENDIENTE', 'APROBADA', 'EJECUTADA', 'RECHAZADA', 'VENCIDA'];

  ngOnInit(): void {
    this.recargar();
    this.http.get<EvidenciaOnChain>('assets/onchain-evidencia.json').subscribe({
      next: (e) => (this.evidencia = e), error: () => {},
    });
  }

  get visibles(): PropuestaResponse[] {
    return this.propuestas.filter((p) => this.filtro === 'TODAS' || p.estado === this.filtro);
  }

  get gasTotal(): number {
    return (this.evidencia?.transacciones ?? []).reduce((s, t) => s + Number(t.gas), 0);
  }

  recargar(): void {
    this.cargando = true;
    this.svc.propuestas().subscribe({
      next: (l) => {
        this.propuestas = (l ?? []).sort((a, b) => b.creadaEn.localeCompare(a.creadaEn));
        this.cargando = false;
        const sel = this.seleccionada && this.propuestas.find((p) => p.propuestaId === this.seleccionada!.propuestaId);
        if (sel || this.propuestas.length) this.seleccionar(sel ?? this.propuestas[0]);
      },
      error: (e) => { this.cargando = false; this.toast.error('No se pudo cargar', describeHttpError(e)); },
    });
    this.svc.metricasGobernanza().subscribe({ next: (m) => (this.metricas = m), error: () => (this.metricas = undefined) });
  }

  seleccionar(p: PropuestaResponse): void {
    this.seleccionada = p;
    this.aprob = undefined; this.audit = undefined;
    this.svc.aprobaciones(p.propuestaId).subscribe({ next: (a) => (this.aprob = a), error: () => {} });
    this.svc.auditoria(p.propuestaId).subscribe({ next: (a) => (this.audit = a), error: () => {} });
  }

  /** Crea un caso demo completo para mostrar el flujo: diagnóstico + propuesta (1/3). */
  async crearDemo(): Promise<void> {
    this.ocupado = true;
    try {
      const d = await firstValueFrom(this.svc.diagnosticarDer({
        activoId: 'INV-DEMO-' + Date.now().toString().slice(-5), tipoActivo: 'INVERSOR', comunidadId: 'COM-NORTE',
        metrica: 'voltaje', valor: 265, umbral: 240, minutosSinSenal: 0, usuario: 'consola.gobernanza',
      }));
      const p = await firstValueFrom(this.svc.proponerAccion(d.diagnosticoId));
      this.toast.success('Propuesta creada', 'Firma 1/3 del agente registrada');
      this.seleccionada = p;
      this.recargar();
    } catch (e) { this.toast.error('Error', describeHttpError(e)); } finally { this.ocupado = false; }
  }

  async firmar(rol: 'OPERADOR' | 'PROSUMIDOR', aprueba: boolean): Promise<void> {
    if (!this.seleccionada) return;
    const id = this.seleccionada.propuestaId;
    this.ocupado = true;
    try {
      const ident = rol === 'OPERADOR' ? 'operador@voltera' : 'prosumidor@casa';
      const r = aprueba
        ? await firstValueFrom(rol === 'OPERADOR' ? this.svc.firmarOperador(id, ident, 'validado') : this.svc.firmarProsumidor(id, ident, 'autorizo'))
        : await firstValueFrom(rol === 'OPERADOR' ? this.svc.rechazarOperador(id, ident, 'no procede') : this.svc.rechazarProsumidor(id, ident, 'no autorizo'));
      this.toast.success('Firma registrada', `${r.firmas} · ${r.estado}`);
      this.seleccionada = r;
      this.recargar();
    } catch (e) { this.toast.error('No se pudo firmar', describeHttpError(e)); } finally { this.ocupado = false; }
  }

  /** Demo guiada: crea la propuesta y la lleva de 1/3 a 3/3 → EJECUTADA con pausas visibles. */
  async demoGuiada(): Promise<void> {
    if (this.ocupado) return;
    this.ocupado = true;
    const pausa = (ms: number) => new Promise((r) => setTimeout(r, ms));
    try {
      const d = await firstValueFrom(this.svc.diagnosticarDer({
        activoId: 'INV-DEMO-' + Date.now().toString().slice(-5), tipoActivo: 'INVERSOR', comunidadId: 'COM-NORTE',
        metrica: 'voltaje', valor: 265, umbral: 240, minutosSinSenal: 0, usuario: 'demo.guiada',
      }));
      let p = await firstValueFrom(this.svc.proponerAccion(d.diagnosticoId));
      this.seleccionada = p; this.propuestas = [p, ...this.propuestas]; this.seleccionar(p);
      this.toast.success('Paso 1/3', 'El agente IA propone y firma');
      await pausa(1800);
      p = await firstValueFrom(this.svc.firmarOperador(p.propuestaId, 'operador@voltera', 'validado técnicamente'));
      this.seleccionada = p; this.seleccionar(p);
      this.toast.success('Paso 2/3', 'Operador firma; aún no se ejecuta');
      await pausa(1800);
      p = await firstValueFrom(this.svc.firmarProsumidor(p.propuestaId, 'prosumidor@casa', 'autorizo'));
      this.seleccionada = p; this.seleccionar(p);
      this.toast.success('Paso 3/3', `Prosumidor firma · ${p.estado}`);
      this.recargar();
    } catch (e) { this.toast.error('Demo interrumpida', describeHttpError(e)); } finally { this.ocupado = false; }
  }

  /** Posición del arco de progreso (0..3 de 3). */
  dash(): number { return this.circ * (1 - (this.aprob?.firmasAprobadas ?? 0) / 3); }

  nodoActivo(k: string): boolean { return this.seleccionada?.estado === k; }
  nodoPasado(k: string): boolean {
    const orden = ['PENDIENTE', 'APROBADA', 'EJECUTADA'];
    const e = this.seleccionada?.estado ?? '';
    return orden.indexOf(k) >= 0 && orden.indexOf(k) < orden.indexOf(e);
  }
  chip(e: string): string {
    return e === 'EJECUTADA' || e === 'APROBADA' ? 'ok' : e === 'RECHAZADA' || e === 'VENCIDA' ? 'err' : 'warn';
  }
  avatar(r: string): string { return r === 'AGENTE_IA' ? '🤖' : r === 'OPERADOR' ? '🛠' : '🏠'; }
  rolTxt(r: string): string { return r === 'AGENTE_IA' ? 'Agente IA' : r === 'OPERADOR' ? 'Operador' : 'Prosumidor'; }
  icono(t: string): string {
    return t.startsWith('RECHAZO') ? '✘' : t === 'EJECUTADA' ? '⚡' : t === 'APROBADA_3_DE_3' ? '✅' : t === 'VENCIDA' ? '⌛' : t === 'PROPUESTA_CREADA' ? '📝' : '✍';
  }
  corto(h: string | null | undefined, n = 10): string { return h ? h.slice(0, n) + '…' + h.slice(-6) : ''; }
  copiar(t: string): void { navigator.clipboard?.writeText(t); this.toast.success('Copiado', this.corto(t, 14)); }
}
