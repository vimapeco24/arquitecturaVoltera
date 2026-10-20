import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { BlockchainService } from '../../services/blockchain.service';
import { LiquidacionP2pService } from '../../services/liquidacion-p2p.service';
import { ToastService } from '../../core/toast.service';
import { describeHttpError } from '../../core/http-error';
import { Anclaje, RedInfo, TransaccionP2P } from '../../core/models';

/** Orden del libro de mercado (gestionada en el cliente hasta casarse). */
interface Orden {
  id: string;
  tipo: 'VENTA' | 'COMPRA';
  prosumidorId: string;
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

  // Auto-refresco on-chain
  autoRefresco = true;
  private sub?: Subscription;

  // Filtro de la tabla
  filtro: FiltroEstado = 'TODOS';

  form: FormGroup = this.fb.group({
    tipo: ['VENTA', Validators.required],
    prosumidorId: ['PRO-A', Validators.required],
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

  /** Contratos visibles según el filtro de estado. */
  get contratosFiltrados(): Contrato[] {
    if (this.filtro === 'TODOS') return this.contratos;
    if (this.filtro === 'SIN ANCLAR') return this.contratos.filter((c) => !c.anclaje);
    return this.contratos.filter((c) => c.anclaje?.estado === this.filtro);
  }

  ngOnInit(): void {
    this.restaurar();
    if (!this.ventas.length && !this.compras.length && !this.contratos.length) {
      this.sembrarDemo();
    }
    this.cargarRed();
    this.listarContratos();
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
      kwh: Number(v.kwh),
      precioKwh: Number(v.precioKwh),
      creadaEn: new Date().toISOString(),
    };
    if (orden.tipo === 'VENTA') this.ventas = [orden, ...this.ventas];
    else this.compras = [orden, ...this.compras];
    this.persistir();
    this.toast.success('Oferta publicada', `${orden.tipo} · ${orden.kwh} kWh @ ${orden.precioKwh}`);
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

  /** ¿Las dos órdenes seleccionadas se pueden casar? (precio venta <= compra, partes distintas). */
  get puedeCasar(): boolean {
    return !!this.ventaSel && !!this.compraSel
      && this.ventaSel.prosumidorId !== this.compraSel.prosumidorId
      && this.ventaSel.precioKwh <= this.compraSel.precioKwh;
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
        this.ventaSel = this.compraSel = undefined;
        // Ancla el contrato en la cadena.
        this.anclarContrato(tx);
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

  private anclarContrato(tx: TransaccionP2P): void {
    const base: Contrato = {
      tx,
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
    this.ventas = [
      { id: 'ORD-DEMO-V1', tipo: 'VENTA', prosumidorId: 'PRO-SOLAR-01', kwh: 12, precioKwh: 420, creadaEn: ahora },
      { id: 'ORD-DEMO-V2', tipo: 'VENTA', prosumidorId: 'PRO-SOLAR-02', kwh: 8, precioKwh: 460, creadaEn: ahora },
    ];
    this.compras = [
      { id: 'ORD-DEMO-C1', tipo: 'COMPRA', prosumidorId: 'PRO-CASA-07', kwh: 10, precioKwh: 500, creadaEn: ahora },
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
