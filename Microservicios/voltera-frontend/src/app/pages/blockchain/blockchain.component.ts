import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
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

/** Contrato = transacción P2P casada + su estado de anclaje on-chain. */
interface Contrato {
  tx: TransaccionP2P;
  anclaje?: Anclaje;
}

/**
 * Marketplace P2P de energía de Voltera. Los prosumidores publican ofertas de
 * venta/compra de excedentes; al casar una venta con una compra se crea el
 * contrato (vía liquidacion-p2p) y se ancla en Polygon (vía blockchain-gateway),
 * dejando trazabilidad on-chain (hash + txHash + enlace a PolygonScan).
 */
@Component({
  selector: 'app-blockchain',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './blockchain.component.html',
  styleUrl: './marketplace.scss',
})
export class BlockchainComponent implements OnInit {
  private fb = inject(FormBuilder);
  private bc = inject(BlockchainService);
  private p2p = inject(LiquidacionP2pService);
  private toast = inject(ToastService);

  // Libro de órdenes (cliente)
  ventas: Orden[] = [];
  compras: Orden[] = [];
  ventaSel?: Orden;
  compraSel?: Orden;

  // Contratos casados + anclaje
  contratos: Contrato[] = [];
  cargando = false;
  casando = false;
  redInfo?: RedInfo;

  // Métricas del marketplace
  get totalKwh(): number {
    return this.contratos.reduce((s, c) => s + Number(c.tx.energiaKwh || 0), 0);
  }
  get totalConfirmados(): number {
    return this.contratos.filter((c) => c.anclaje?.estado === 'CONFIRMADO').length;
  }
  get volumenCop(): number {
    return this.contratos.reduce((s, c) => s + Number(c.tx.valorTotal || 0), 0);
  }

  form: FormGroup = this.fb.group({
    tipo: ['VENTA', Validators.required],
    prosumidorId: ['PRO-A', Validators.required],
    kwh: [10, [Validators.required, Validators.min(0.1)]],
    precioKwh: [450, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.sembrarDemo();
    this.listarContratos();
    this.cargarRed();
  }

  private cargarRed(): void {
    this.bc.red().subscribe({
      next: (r) => (this.redInfo = r),
      error: () => (this.redInfo = undefined),
    });
  }

  /** Publica una oferta en el libro (estado del cliente). */
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
    this.toast.success('Oferta publicada', `${orden.tipo} · ${orden.kwh} kWh @ ${orden.precioKwh}`);
  }

  seleccionarVenta(o: Orden): void { this.ventaSel = this.ventaSel?.id === o.id ? undefined : o; }
  seleccionarCompra(o: Orden): void { this.compraSel = this.compraSel?.id === o.id ? undefined : o; }

  /** ¿Las dos órdenes seleccionadas se pueden casar? (precio venta <= compra, partes distintas). */
  get puedeCasar(): boolean {
    return !!this.ventaSel && !!this.compraSel
      && this.ventaSel.prosumidorId !== this.compraSel.prosumidorId
      && this.ventaSel.precioKwh <= this.compraSel.precioKwh;
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
        // Retira las órdenes casadas del libro.
        this.ventas = this.ventas.filter((o) => o.id !== venta.id);
        this.compras = this.compras.filter((o) => o.id !== compra.id);
        this.ventaSel = this.compraSel = undefined;
        // Ancla el contrato en la cadena.
        this.anclarContrato(tx);
      },
      error: (e) => { this.casando = false; this.toast.error('Error al casar', this.msg(e)); },
    });
  }

  private anclarContrato(tx: TransaccionP2P): void {
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
        this.contratos = [{ tx, anclaje: a }, ...this.contratos];
        this.toast.success('Contrato anclado', `${a.transaccionId} · ${a.estado}`);
      },
      error: (e) => {
        this.casando = false;
        // El contrato existe aunque el anclaje falle; se muestra sin anclaje.
        this.contratos = [{ tx }, ...this.contratos];
        this.toast.error('Contrato creado, anclaje pendiente', this.msg(e));
      },
    });
  }

  /** Refresca el estado on-chain de los contratos ya anclados. */
  listarContratos(): void {
    this.cargando = true;
    this.bc.listar().subscribe({
      next: (anclajes) => {
        // Enlaza anclajes existentes con contratos ya en pantalla (por transaccionId).
        const porTx = new Map(anclajes.map((a) => [a.transaccionId, a]));
        this.contratos = this.contratos.map((c) => ({ tx: c.tx, anclaje: porTx.get(c.tx.id) ?? c.anclaje }));
        this.cargando = false;
      },
      error: () => { this.cargando = false; },
    });
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

  badge(estado?: string): string {
    if (estado === 'CONFIRMADO') return 'ok';
    if (estado === 'FALLIDO') return 'err';
    return 'warn';
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
          this.toast.error('No anclado', `${c.tx.id} aún no está en la cadena.`);
        } else if (r.integro) {
          this.toast.success('Integridad verificada ✓', `El hash on-chain coincide (${c.tx.id}).`);
        } else {
          this.toast.error('¡Integridad comprometida!', `El hash no coincide con lo anclado (${c.tx.id}).`);
        }
      },
      error: (e) => this.toast.error('Error al verificar', this.msg(e)),
    });
  }

  corto(hash?: string | null): string {
    if (!hash) return '—';
    return hash.length > 16 ? `${hash.slice(0, 10)}…${hash.slice(-6)}` : hash;
  }

  private msg(e: any): string { return describeHttpError(e); }
}
