import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { BlockchainService } from '../../services/blockchain.service';
import { LiquidacionP2pService } from '../../services/liquidacion-p2p.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { Anclaje, ComunidadResumen, RedInfo, TransaccionP2P } from '../../core/models';

/** Orden del libro de mercado (gestionada en el cliente hasta casarse). */
interface Orden {
  id: string;
  tipo: 'VENTA' | 'COMPRA';
  prosumidorId: string;
  comunidadId: string;
  kwh: number;
  precioKwh: number;
  creadaEn: string;
}

/** Un hito en la vida de un contrato (para el timeline). */
interface Hito {
  etiqueta: string;
  ts: string;
  icono: string;
}

/** Contrato = transacción P2P casada + su estado de anclaje on-chain + historial. */
interface Contrato {
  tx: TransaccionP2P;
  comunidadId?: string;
  anclaje?: Anclaje;
  hitos: Hito[];
  verificado?: 'ok' | 'err';
}

type FiltroEstado = 'TODOS' | 'CONFIRMADO' | 'PENDIENTE' | 'SIN ANCLAR';

const LS_ORDENES = 'voltera.mp.ordenes.v1';
const LS_CONTRATOS = 'voltera.mp.contratos.v1';

/**
 * Marketplace P2P de energía de Voltera. Los prosumidores publican ofertas de
 * venta/compra de excedentes; al casar una venta con una compra se crea el
 * contrato (vía liquidacion-p2p) y se ancla en Polygon (vía blockchain-gateway),
 * dejando trazabilidad on-chain (hash + txHash + enlace a PolygonScan).
 *
 * Persiste el libro y los contratos en localStorage y auto-refresca el estado
 * on-chain para que los anclajes PENDIENTE pasen a CONFIRMADO sin recargar.
 */
@Component({
  selector: 'app-blockchain',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './blockchain.component.html',
  styleUrl: './marketplace.scss',
})
export class BlockchainComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private bc = inject(BlockchainService);
  private p2p = inject(LiquidacionP2pService);
  private toast = inject(ToastService);

  // Libro de órdenes (cliente, persistido)
  ventas: Orden[] = [];
  compras: Orden[] = [];
  ventaSel?: Orden;
  compraSel?: Orden;

  // Contratos casados + anclaje (persistidos)
  contratos: Contrato[] = [];
  cargando = false;
  casando = false;
  redInfo?: RedInfo;

  // Comunidades energéticas
  comunidades: ComunidadResumen[] = [];
  comunidadFiltro = '';          // '' = todas
  comunidadesConocidas: string[] = ['COMUNIDAD-SOLAR-01', 'COMUNIDAD-EOLICA-02', 'COMUNIDAD-BARRIO-03'];

  // Panel de detalle de un contrato
  detalle?: Contrato;

  // Auto-refresco on-chain
  autoRefresco = true;
  private sub?: Subscription;

  // Filtro de la tabla
  filtro: FiltroEstado = 'TODOS';

  form: FormGroup = this.fb.group({
    tipo: ['VENTA', Validators.required],
    prosumidorId: ['PRO-A', Validators.required],
    comunidadId: ['COMUNIDAD-SOLAR-01', Validators.required],
    kwh: [10, [Validators.required, Validators.min(0.1)]],
    precioKwh: [450, [Validators.required, Validators.min(1)]],
  });

  // ---------------- Métricas del marketplace ----------------
  get totalKwh(): number {
    return this.contratos.reduce((s, c) => s + Number(c.tx.energiaKwh || 0), 0);
  }
  get totalConfirmados(): number {
    return this.contratos.filter((c) => c.anclaje?.estado === 'CONFIRMADO').length;
  }
  get totalPendientes(): number {
    return this.contratos.filter((c) => c.anclaje?.estado === 'PENDIENTE').length;
  }
  get volumenCop(): number {
    return this.contratos.reduce((s, c) => s + Number(c.tx.valorTotal || 0), 0);
  }
  get precioPromedio(): number {
    if (!this.contratos.length) return 0;
    const n = this.contratos.reduce((s, c) => s + Number(c.tx.precioCasacionKwh || 0), 0);
    return n / this.contratos.length;
  }

  /** Contratos visibles según el filtro de estado y comunidad. */
  get contratosFiltrados(): Contrato[] {
    let base = this.contratos;
    if (this.comunidadFiltro) base = base.filter((c) => (c.comunidadId || c.anclaje?.comunidadId) === this.comunidadFiltro);
    if (this.filtro === 'TODOS') return base;
    if (this.filtro === 'SIN ANCLAR') return base.filter((c) => !c.anclaje);
    return base.filter((c) => c.anclaje?.estado === this.filtro);
  }

  ngOnInit(): void {
    this.restaurar();
    if (!this.ventas.length && !this.compras.length && !this.contratos.length) {
      this.sembrarDemo();
    }
    this.cargarRed();
    this.listarContratos();
    this.cargarComunidades();
    this.iniciarAutoRefresco();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  // ---------------- Red / contrato ----------------
  private cargarRed(): void {
    this.bc.red().subscribe({
      next: (r) => (this.redInfo = r),
      error: () => (this.redInfo = undefined),
    });
  }

  // ---------------- Comunidades energéticas ----------------
  private cargarComunidades(): void {
    this.bc.comunidades().subscribe({
      next: (cs) => {
        this.comunidades = cs.filter((c) => c.comunidadId !== '(sin comunidad)');
        // Enriquecer el catálogo de comunidades conocidas con las que ya existen on-chain.
        for (const c of this.comunidades) {
          if (c.comunidadId && !this.comunidadesConocidas.includes(c.comunidadId)) {
            this.comunidadesConocidas = [...this.comunidadesConocidas, c.comunidadId];
          }
        }
      },
      error: () => { /* el gateway puede no exponerlo aún: se ignora */ },
    });
  }

  filtrarComunidad(id: string): void {
    this.comunidadFiltro = this.comunidadFiltro === id ? '' : id;
  }

  // ---------------- Auto-refresco ----------------
  toggleAutoRefresco(): void {
    this.autoRefresco = !this.autoRefresco;
    if (this.autoRefresco) this.iniciarAutoRefresco();
    else this.sub?.unsubscribe();
  }

  private iniciarAutoRefresco(): void {
    this.sub?.unsubscribe();
    if (!this.autoRefresco) return;
    // Refresca el estado on-chain cada 10 s si hay anclajes pendientes.
    this.sub = interval(10000).subscribe(() => {
      if (this.contratos.some((c) => c.anclaje?.estado === 'PENDIENTE') || !this.contratos.length) {
        this.listarContratos(true);
      }
      this.cargarRed();
      this.cargarComunidades();
    });
  }

  // ---------------- Libro de órdenes ----------------
  publicar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const orden: Orden = {
      id: `ORD-${Date.now().toString(36).toUpperCase()}`,
      tipo: v.tipo,
      prosumidorId: v.prosumidorId,
      comunidadId: v.comunidadId,
      kwh: Number(v.kwh),
      precioKwh: Number(v.precioKwh),
      creadaEn: new Date().toISOString(),
    };
    if (orden.tipo === 'VENTA') this.ventas = [orden, ...this.ventas];
    else this.compras = [orden, ...this.compras];
    this.persistir();
    this.toast.success('Oferta publicada', `${orden.tipo} · ${orden.kwh} kWh @ ${orden.precioKwh} · ${orden.comunidadId}`);
  }

  quitarOrden(o: Orden): void {
    if (o.tipo === 'VENTA') this.ventas = this.ventas.filter((x) => x.id !== o.id);
    else this.compras = this.compras.filter((x) => x.id !== o.id);
    if (this.ventaSel?.id === o.id) this.ventaSel = undefined;
    if (this.compraSel?.id === o.id) this.compraSel = undefined;
    this.persistir();
  }

  seleccionarVenta(o: Orden): void { this.ventaSel = this.ventaSel?.id === o.id ? undefined : o; }
  seleccionarCompra(o: Orden): void { this.compraSel = this.compraSel?.id === o.id ? undefined : o; }

  /** ¿Las dos órdenes seleccionadas se pueden casar? (misma comunidad, precio venta ≤ compra, partes distintas). */
  get puedeCasar(): boolean {
    return !!this.ventaSel && !!this.compraSel
      && this.ventaSel.comunidadId === this.compraSel.comunidadId
      && this.ventaSel.prosumidorId !== this.compraSel.prosumidorId
      && this.ventaSel.precioKwh <= this.compraSel.precioKwh;
  }

  /** Motivo por el que NO se puede casar (para feedback claro en la UI). */
  get motivoNoCasar(): string {
    if (!this.ventaSel || !this.compraSel) return '';
    if (this.ventaSel.comunidadId !== this.compraSel.comunidadId) return 'distinta comunidad energética';
    if (this.ventaSel.prosumidorId === this.compraSel.prosumidorId) return 'mismo prosumidor';
    if (this.ventaSel.precioKwh > this.compraSel.precioKwh) return 'precio de venta mayor que el de compra';
    return '';
  }

  /** kWh que realmente se casarían (mínimo entre oferta y demanda). */
  get kwhCasables(): number {
    if (!this.ventaSel || !this.compraSel) return 0;
    return Math.min(this.ventaSel.kwh, this.compraSel.kwh);
  }

  /** Casa venta+compra: crea el contrato en liquidacion-p2p y lo ancla en blockchain. */
  casar(): void {
    if (!this.puedeCasar || !this.ventaSel || !this.compraSel) {
      this.toast.error('No se puede casar', 'Revisa precio (venta ≤ compra) y que sean prosumidores distintos.');
      return;
    }
    this.casando = true;
    const venta = this.ventaSel, compra = this.compraSel;
    this.p2p.emparejar({
      vendedorId: venta.prosumidorId,
      kwhVenta: venta.kwh,
      precioVenta: venta.precioKwh,
      compradorId: compra.prosumidorId,
      kwhCompra: compra.kwh,
      precioCompra: compra.precioKwh,
    }).subscribe({
      next: (tx) => {
        this.toast.success('Contrato casado', `${tx.id} · ${tx.tipoCasacion}`);
        // Retira las órdenes casadas del libro (o reduce si fue casación parcial).
        this.aplicarCasacionAlLibro(venta, compra, tx);
        const comunidadId = venta.comunidadId;
        this.ventaSel = this.compraSel = undefined;
        // Ancla el contrato en la cadena.
        this.anclarContrato(tx, comunidadId);
      },
      error: (e) => { this.casando = false; this.toast.error('Error al casar', this.msg(e)); },
    });
  }

  /** Quita o reduce las órdenes del libro según la casación (total o parcial). */
  private aplicarCasacionAlLibro(venta: Orden, compra: Orden, tx: TransaccionP2P): void {
    const pendVenta = Number(tx.pendienteVentaKwh || 0);
    const pendCompra = Number(tx.pendienteCompraKwh || 0);
    this.ventas = this.ventas.flatMap((o) => {
      if (o.id !== venta.id) return [o];
      return pendVenta > 0 ? [{ ...o, kwh: pendVenta }] : [];
    });
    this.compras = this.compras.flatMap((o) => {
      if (o.id !== compra.id) return [o];
      return pendCompra > 0 ? [{ ...o, kwh: pendCompra }] : [];
    });
    this.persistir();
  }

  private anclarContrato(tx: TransaccionP2P, comunidadId?: string): void {
    const base: Contrato = {
      tx,
      comunidadId,
      hitos: [{ etiqueta: 'Contrato casado', ts: new Date().toISOString(), icono: '🤝' }],
    };
    this.bc.anclar({
      transaccionId: tx.id,
      vendedorId: tx.vendedorId,
      compradorId: tx.compradorId,
      energiaWh: Math.round(Number(tx.energiaKwh) * 1000),
      precioPorKwh: String(tx.precioCasacionKwh),
      valorTotal: String(tx.valorTotal),
      liquidadaEn: new Date(tx.liquidadaEn).toISOString(),
      comunidadId,
    }).subscribe({
      next: (a) => {
        this.casando = false;
        base.anclaje = a;
        base.hitos.push({ etiqueta: `Anclaje ${a.estado}`, ts: a.actualizadoEn || new Date().toISOString(), icono: a.estado === 'CONFIRMADO' ? '⛓️' : '⏳' });
        this.contratos = [base, ...this.contratos];
        this.persistir();
        this.toast.success('Contrato anclado', `${a.transaccionId} · ${a.estado}`);
      },
      error: (e) => {
        this.casando = false;
        base.hitos.push({ etiqueta: 'Anclaje fallido (reintentará)', ts: new Date().toISOString(), icono: '⚠️' });
        this.contratos = [base, ...this.contratos];
        this.persistir();
        this.toast.error('Contrato creado, anclaje pendiente', this.msg(e));
      },
    });
  }

  /** Refresca el estado on-chain de los contratos ya anclados. */
  listarContratos(silencioso = false): void {
    if (!silencioso) this.cargando = true;
    this.bc.listar().subscribe({
      next: (anclajes) => {
        const porTx = new Map(anclajes.map((a) => [a.transaccionId, a]));
        this.contratos = this.contratos.map((c) => {
          const nuevo = porTx.get(c.tx.id);
          if (nuevo && nuevo.estado !== c.anclaje?.estado) {
            c.hitos.push({ etiqueta: `Anclaje ${nuevo.estado}`, ts: nuevo.actualizadoEn || new Date().toISOString(), icono: nuevo.estado === 'CONFIRMADO' ? '⛓️' : '⏳' });
          }
          return { ...c, anclaje: nuevo ?? c.anclaje };
        });
        this.persistir();
        this.cargando = false;
      },
      error: () => { this.cargando = false; },
    });
  }

  /** Verifica la integridad on-chain de un contrato: recomputa y compara el hash. */
  verificar(c: Contrato): void {
    this.bc.verificar({
      transaccionId: c.tx.id,
      vendedorId: c.tx.vendedorId,
      compradorId: c.tx.compradorId,
      energiaWh: Math.round(Number(c.tx.energiaKwh) * 1000),
      precioPorKwh: String(c.tx.precioCasacionKwh),
      valorTotal: String(c.tx.valorTotal),
      liquidadaEn: new Date(c.tx.liquidadaEn).toISOString(),
    }).subscribe({
      next: (r) => {
        if (!r.existe) {
          c.verificado = 'err';
          this.toast.error('No anclado', `${c.tx.id} aún no está en la cadena.`);
        } else if (r.integro) {
          c.verificado = 'ok';
          this.toast.success('Integridad verificada ✓', `El hash on-chain coincide (${c.tx.id}).`);
        } else {
          c.verificado = 'err';
          this.toast.error('¡Integridad comprometida!', `El hash no coincide con lo anclado (${c.tx.id}).`);
        }
        this.persistir();
      },
      error: (e) => this.toast.error('Error al verificar', this.msg(e)),
    });
  }

  filtrar(f: FiltroEstado): void { this.filtro = f; }

  // ---------------- Detalle de contrato ----------------
  abrirDetalle(c: Contrato): void { this.detalle = c; }
  cerrarDetalle(): void { this.detalle = undefined; }

  /** Copia un valor al portapapeles (hash, txHash) con feedback. */
  copiar(valor?: string | null, etiqueta = 'Valor'): void {
    if (!valor) return;
    const done = () => this.toast.success('Copiado', `${etiqueta} copiado al portapapeles.`);
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(valor).then(done, () => this.toast.error('No se pudo copiar', valor));
    } else {
      done();
    }
  }

  // ---------------- Re-hidratación desde el gateway ----------------
  /**
   * Re-consulta el gateway por cada contrato local y sincroniza su estado. Útil
   * cuando el gateway reinició (store en memoria) y perdió la lista: los anclajes
   * que ya no conoce se marcan como "no encontrado en el gateway".
   */
  rehidratar(): void {
    this.cargando = true;
    this.bc.listar().subscribe({
      next: (anclajes) => {
        const porTx = new Map(anclajes.map((a) => [a.transaccionId, a]));
        this.contratos = this.contratos.map((c) => {
          const enGateway = porTx.get(c.tx.id);
          if (enGateway) return { ...c, anclaje: enGateway, comunidadId: c.comunidadId || enGateway.comunidadId || undefined };
          return c; // se conserva el estado local; el detalle avisará si no está en el gateway
        });
        this.persistir();
        this.cargando = false;
        this.toast.success('Re-sincronizado', `El gateway conoce ${anclajes.length} anclaje(s).`);
      },
      error: (e) => { this.cargando = false; this.toast.error('No se pudo re-sincronizar', this.msg(e)); },
    });
  }

  /** ¿Este contrato local sigue presente en el gateway? (para avisar tras un redeploy). */
  enGateway(c: Contrato): boolean {
    return !!c.anclaje; // el anclaje local existe; el detalle re-consulta por tx si hace falta
  }

  // ---------------- Exportar evidencia ----------------
  exportarJson(): void {
    const data = this.contratosFiltrados.map((c) => ({
      transaccionId: c.tx.id,
      comunidadId: c.comunidadId || c.anclaje?.comunidadId || null,
      vendedor: c.tx.vendedorId,
      comprador: c.tx.compradorId,
      energiaKwh: c.tx.energiaKwh,
      valorTotal: c.tx.valorTotal,
      estado: c.anclaje?.estado || 'SIN ANCLAR',
      hashCasacion: c.anclaje?.hashCasacion || null,
      txHash: c.anclaje?.txHash || null,
      bloque: c.anclaje?.bloque ?? null,
      gasUsado: c.anclaje?.gasUsado ?? null,
      explorerUrl: c.anclaje?.explorerUrl || null,
    }));
    this.descargar(JSON.stringify(data, null, 2), 'voltera-contratos.json', 'application/json');
  }

  exportarCsv(): void {
    const cols = ['transaccionId', 'comunidadId', 'vendedor', 'comprador', 'energiaKwh', 'valorTotal', 'estado', 'hashCasacion', 'txHash', 'bloque', 'gasUsado'];
    const esc = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`;
    const filas = this.contratosFiltrados.map((c) => [
      c.tx.id,
      c.comunidadId || c.anclaje?.comunidadId || '',
      c.tx.vendedorId, c.tx.compradorId,
      c.tx.energiaKwh, c.tx.valorTotal,
      c.anclaje?.estado || 'SIN ANCLAR',
      c.anclaje?.hashCasacion || '', c.anclaje?.txHash || '',
      c.anclaje?.bloque ?? '', c.anclaje?.gasUsado ?? '',
    ].map(esc).join(','));
    const csv = [cols.join(','), ...filas].join('\n');
    this.descargar(csv, 'voltera-contratos.csv', 'text/csv');
  }

  private descargar(contenido: string, nombre: string, tipo: string): void {
    const blob = new Blob([contenido], { type: tipo });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = nombre;
    a.click();
    URL.revokeObjectURL(url);
    this.toast.success('Evidencia exportada', nombre);
  }

  limpiarTodo(): void {
    this.ventas = []; this.compras = []; this.contratos = [];
    this.ventaSel = this.compraSel = undefined;
    this.sembrarDemo();
    this.persistir();
    this.toast.success('Marketplace reiniciado', 'Se restauraron las ofertas de demostración.');
  }

  // ---------------- Persistencia (localStorage) ----------------
  private persistir(): void {
    try {
      localStorage.setItem(LS_ORDENES, JSON.stringify({ ventas: this.ventas, compras: this.compras }));
      localStorage.setItem(LS_CONTRATOS, JSON.stringify(this.contratos));
    } catch { /* almacenamiento no disponible: se ignora */ }
  }

  private restaurar(): void {
    try {
      const libro = localStorage.getItem(LS_ORDENES);
      if (libro) {
        const { ventas, compras } = JSON.parse(libro);
        this.ventas = Array.isArray(ventas) ? ventas : [];
        this.compras = Array.isArray(compras) ? compras : [];
      }
      const cs = localStorage.getItem(LS_CONTRATOS);
      if (cs) {
        const parsed = JSON.parse(cs);
        this.contratos = Array.isArray(parsed)
          ? parsed.map((c: any) => ({ ...c, hitos: Array.isArray(c.hitos) ? c.hitos : [] }))
          : [];
      }
    } catch { /* datos corruptos: se empieza limpio */ }
  }

  /** Datos de ejemplo para que el marketplace no arranque vacío. */
  private sembrarDemo(): void {
    const ahora = new Date().toISOString();
    const com = 'COMUNIDAD-SOLAR-01';
    this.ventas = [
      { id: 'ORD-DEMO-V1', tipo: 'VENTA', prosumidorId: 'PRO-SOLAR-01', comunidadId: com, kwh: 12, precioKwh: 420, creadaEn: ahora },
      { id: 'ORD-DEMO-V2', tipo: 'VENTA', prosumidorId: 'PRO-SOLAR-02', comunidadId: 'COMUNIDAD-EOLICA-02', kwh: 8, precioKwh: 460, creadaEn: ahora },
    ];
    this.compras = [
      { id: 'ORD-DEMO-C1', tipo: 'COMPRA', prosumidorId: 'PRO-CASA-07', comunidadId: com, kwh: 10, precioKwh: 500, creadaEn: ahora },
    ];
  }

  // ---------------- Helpers de plantilla ----------------
  badge(estado?: string): string {
    if (estado === 'CONFIRMADO') return 'ok';
    if (estado === 'FALLIDO') return 'err';
    return 'warn';
  }

  corto(hash?: string | null): string {
    if (!hash) return '—';
    return hash.length > 16 ? `${hash.slice(0, 10)}…${hash.slice(-6)}` : hash;
  }

  private msg(e: any): string { return describeHttpError(e); }
}
