import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

type ClaveTool = 'grafana' | 'prometheus' | 'kiali' | 'jaeger';

interface Tool { clave: ClaveTool; nombre: string; icon: string; color: string; url: string; }
interface Hop { src: string; dst: string; p50: number; p95: number; }
interface Svc { svc: string; status: 'UP' | 'DOWN' | 'DESCONOCIDO'; ms: number; }
interface BreakerApp { estado: 'CLOSED' | 'OPEN' | 'HALF_OPEN' | 'ND'; failureRate?: string; buffered?: number; }
interface BreakerMesh { servicio: string; maxConn: number; pending: number; consecutive5xx: number; ejection: string; agresivo: boolean; }
interface Pilar { icon: string; titulo: string; que: string; donde: string; herramienta: ClaveTool; }

interface Snapshot {
  mode: 'live' | 'offline';
  latencyHops: Hop[];
  mtlsPct: number;
  services?: Svc[];
  cb503: number;
  breakerApp: BreakerApp;
  generatedAt?: string;
  error?: string;
}

const LS_KEY = 'voltera.observabilidad.urls';

/**
 * Página de Observabilidad & Service Mesh.
 * Consume /api/obs (mismo origen, HTTPS) con datos REALES medidos del clúster:
 * estado y latencia por servicio, mTLS y circuit breaker. Los tableros del
 * clúster (Kiali/Prometheus) se abren por sus URLs reales (LAN).
 */
@Component({
  selector: 'app-observabilidad',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './observabilidad.component.html',
  styleUrl: './observabilidad.component.scss',
})
export class ObservabilidadComponent implements OnInit {
  private http = inject(HttpClient);

  mode: 'live' | 'offline' | 'checking' = 'checking';
  generatedAt = '';
  cargando = true;
  editandoUrls = false;

  tools: Tool[] = [];
  latencyHops: Hop[] = [];
  mtlsPct = 0;
  services: Svc[] = [];
  cb503 = 0;
  breakerApp: BreakerApp = { estado: 'ND' };

  readonly breakersMesh: BreakerMesh[] = [
    { servicio: 'facturacion-service', maxConn: 5, pending: 5, consecutive5xx: 3, ejection: '30s / 100%', agresivo: true },
    { servicio: 'tarifas-service', maxConn: 100, pending: 100, consecutive5xx: 5, ejection: '30s / 50%', agresivo: false },
    { servicio: 'volumetria-service', maxConn: 100, pending: 100, consecutive5xx: 5, ejection: '30s / 50%', agresivo: false },
    { servicio: 'liquidacion-p2p-service', maxConn: 100, pending: 100, consecutive5xx: 5, ejection: '30s / 50%', agresivo: false },
    { servicio: 'liquidacion-mensual-service', maxConn: 100, pending: 100, consecutive5xx: 5, ejection: '30s / 50%', agresivo: false },
    { servicio: 'pagos-service', maxConn: 100, pending: 100, consecutive5xx: 5, ejection: '30s / 50%', agresivo: false },
  ];

  readonly pilares: Pilar[] = [
    { icon: '🔐', titulo: 'mTLS entre microservicios', que: 'Todo el tráfico Pod↔Pod cifrado y autenticado (PeerAuthentication STRICT).', donde: 'Kiali → Graph: candado en cada arista.', herramienta: 'kiali' },
    { icon: '⏱️', titulo: 'Latencia por salto', que: 'Tiempo de cada salto source → destination (lo que más mira el profesor).', donde: 'Kiali → Response Time · Prometheus → istio_request_duration.', herramienta: 'kiali' },
    { icon: '⚡', titulo: 'Circuit Breaker', que: 'Facturación se satura y corta (503); volumetría sigue operando.', donde: 'Kiali: arista roja · Prometheus: 503.', herramienta: 'prometheus' },
    { icon: '📈', titulo: 'Escalabilidad (HPA)', que: 'Ante más carga, el HPA crea pods ("de N a M en X s").', donde: 'Prometheus: réplicas vs. carga.', herramienta: 'prometheus' },
    { icon: '♻️', titulo: 'Disponibilidad / Failover', que: '2 réplicas activo-activo: si una cae, el mesh reenruta sin downtime.', donde: 'Kiali: el tráfico se mantiene al matar un pod.', herramienta: 'kiali' },
  ];

  ngOnInit(): void {
    this.initTools();
    this.cargar();
  }

  initTools(): void {
    const o: any = (environment as any).observabilidad ?? {};
    const g = this.leerLocalStorage();
    this.tools = [
      { clave: 'grafana', nombre: 'Grafana', icon: '📊', color: '#F46800', url: g.grafana ?? o.grafana ?? '' },
      { clave: 'prometheus', nombre: 'Prometheus', icon: '🔥', color: '#E6522C', url: g.prometheus ?? o.prometheus ?? '' },
      { clave: 'kiali', nombre: 'Kiali', icon: '🕸️', color: '#0EA5E9', url: g.kiali ?? o.kiali ?? '' },
      { clave: 'jaeger', nombre: 'Jaeger', icon: '🔎', color: '#60A5FA', url: g.jaeger ?? o.jaeger ?? '' },
    ];
  }

  cargar(): void {
    this.cargando = true;
    this.mode = 'checking';
    this.http.get<Snapshot>('/api/obs').subscribe({
      next: (s) => this.aplicar(s),
      error: () => this.offline(),
    });
  }

  private aplicar(s: Snapshot): void {
    this.mode = s.mode === 'live' ? 'live' : 'offline';
    this.latencyHops = s.latencyHops ?? [];
    this.mtlsPct = s.mtlsPct ?? 0;
    this.services = s.services ?? [];
    this.cb503 = s.cb503 ?? 0;
    this.breakerApp = s.breakerApp ?? { estado: 'ND' };
    this.generatedAt = s.generatedAt ?? new Date().toISOString();
    this.cargando = false;
  }

  private offline(): void {
    this.mode = 'offline';
    this.services = [];
    this.latencyHops = [];
    this.generatedAt = new Date().toISOString();
    this.cargando = false;
  }

  get serviciosUp(): number {
    return this.services.filter((s) => s.status === 'UP').length;
  }
  get latenciaMedia(): number {
    const up = this.services.filter((s) => s.ms > 0);
    return up.length ? Math.round(up.reduce((a, s) => a + s.ms, 0) / up.length) : 0;
  }
  get breakerLabel(): string {
    return this.breakerApp.estado === 'ND' ? 'No disponible' : this.breakerApp.estado;
  }

  /** Consultas PromQL que se precargan (un panel por consulta) al abrir Prometheus. */
  readonly promQueries: { expr: string; tab: 'table' | 'graph'; range?: string }[] = [
    {
      // Latencia p50 por salto (source -> destination): la métrica clave del mesh.
      // Ventana amplia [1h] para que muestre datos aunque el tráfico sea intermitente.
      expr:
        'histogram_quantile(0.5, sum(rate(istio_request_duration_milliseconds_bucket{reporter="destination",destination_service_namespace="voltera-app"}[1h])) by (le, source_workload, destination_workload))',
      tab: 'table',
    },
    {
      // Total de peticiones por salto en la última hora (siempre visible tras generar tráfico).
      expr:
        'sum(increase(istio_requests_total{destination_service_namespace="voltera-app"}[1h])) by (source_workload, destination_workload)',
      tab: 'table',
    },
    {
      // Porcentaje de tráfico interno cifrado con mTLS (ventana [1h]).
      expr:
        'sum(rate(istio_requests_total{connection_security_policy="mutual_tls"}[1h])) / sum(rate(istio_requests_total[1h]))',
      tab: 'table',
    },
    {
      // Peticiones por segundo por servicio destino (gráfica en 1h).
      expr: 'sum(rate(istio_requests_total{destination_service_namespace="voltera-app"}[5m])) by (destination_workload)',
      tab: 'graph',
      range: '1h',
    },
  ];

  /**
   * Dashboard de Grafana que se abre YA cargado: "Istio Mesh Dashboard".
   * El UID es estable (viene provisionado por el addon de Istio) y Prometheus
   * es el datasource por defecto, así que abre mostrando tráfico, éxito y la
   * tabla de latencia p50/p90/p99 por servicio sin tocar nada.
   */
  readonly grafanaDashboard = {
    uid: '1a9a8ea49444aae205c7737573e894f9',
    slug: 'istio-mesh-dashboard',
    from: 'now-1h',
    to: 'now',
    refresh: '15s',
    // UID real del datasource Prometheus provisionado en este Grafana (evita el
    // "No data" por variable de datasource sin resolver).
    datasourceUid: 'PBFA97CFB590B2093',
    timezone: 'utc',
  };

  /** Namespace del mesh que se abre ya filtrado en el grafo de Kiali. */
  readonly kialiNamespace = 'voltera-app';
  /** Servicio cuyas trazas se precargan en la búsqueda de Jaeger. */
  readonly jaegerService = 'facturacion-service';

  abrir(url: string): void {
    if (url) window.open(url, '_blank', 'noopener');
  }

  /** Subtítulo de cada tarjeta de tablero. */
  subtituloTool(clave: ClaveTool): string {
    switch (clave) {
      case 'prometheus': return 'Abrir con consultas del mesh';
      case 'grafana': return 'Abrir dashboard del mesh';
      case 'kiali': return 'Abrir grafo del namespace';
      case 'jaeger': return 'Abrir trazas de facturación';
      default: return 'Abrir tablero';
    }
  }

  /** Abre un tablero; los 4 se abren con la consulta/vista ya cargada. */
  abrirTool(t: Tool): void {
    if (!t.url) return;
    let url = t.url;
    switch (t.clave) {
      case 'prometheus': url = this.prometheusUrl(t.url); break;
      case 'grafana': url = this.grafanaUrl(t.url); break;
      case 'kiali': url = this.kialiUrl(t.url); break;
      case 'jaeger': url = this.jaegerUrl(t.url); break;
    }
    window.open(url, '_blank', 'noopener');
  }

  /**
   * Construye la URL de Prometheus (UI nueva) con las consultas ya cargadas.
   * La UI itera paneles g0, g1, … mientras exista g{i}.expr y ejecuta cada uno
   * al abrir, así al entrar ya se ven los datos sin escribir nada.
   */
  private prometheusUrl(base: string): string {
    const root = base.replace(/\/+$/, '').replace(/\/(graph|query)$/, '');
    const params = new URLSearchParams();
    this.promQueries.forEach((q, i) => {
      params.append(`g${i}.expr`, q.expr);
      params.append(`g${i}.tab`, q.tab);
      if (q.range) params.append(`g${i}.range_input`, q.range);
    });
    return `${root}/query?${params.toString()}`;
  }

  /**
   * Construye la URL del dashboard de Grafana (Istio Mesh) con rango de tiempo
   * y refresh, de modo que abra directamente mostrando las métricas del mesh.
   */
  private grafanaUrl(base: string): string {
    const root = base.replace(/\/+$/, '');
    if (/\/d\//.test(root)) return root; // ya apunta a un dashboard concreto
    const g = this.grafanaDashboard;
    const params = new URLSearchParams({
      orgId: '1',
      from: g.from,
      to: g.to,
      refresh: g.refresh,
      timezone: g.timezone,
      'var-datasource': g.datasourceUid,
    });
    return `${root}/d/${g.uid}/${g.slug}?${params.toString()}`;
  }

  /**
   * Construye la URL de Kiali abriendo directamente el grafo del namespace del
   * mesh (voltera-app), con duración y refresh, para ver los saltos y el candado
   * mTLS entre microservicios sin navegar.
   */
  private kialiUrl(base: string): string {
    const root = base.replace(/\/+$/, '');
    if (/\/console\//.test(root)) return root; // ya apunta a una vista concreta
    const params = new URLSearchParams({
      namespaces: this.kialiNamespace,
      duration: '300',
      refresh: '15000',
      graphType: 'versionedApp',
    });
    return `${root}/console/graph/namespaces?${params.toString()}`;
  }

  /**
   * Construye la URL de Jaeger abriendo la búsqueda de trazas del servicio
   * facturacion-service. El addon de Istio sirve Jaeger bajo el base path
   * /jaeger (QUERY_BASE_PATH), así que se garantiza ese prefijo.
   */
  private jaegerUrl(base: string): string {
    let root = base.replace(/\/+$/, '');
    if (!/\/jaeger(\/|$)/.test(root)) root = `${root}/jaeger`;
    if (/\/search/.test(root)) return root; // ya es una búsqueda
    const params = new URLSearchParams({
      service: this.jaegerService,
      lookback: '1h',
      limit: '20',
    });
    return `${root}/search?${params.toString()}`;
  }

  guardarUrls(): void {
    const map: Record<string, string> = {};
    this.tools.forEach((t) => (map[t.clave] = t.url));
    try { localStorage.setItem(LS_KEY, JSON.stringify(map)); } catch { /* ignore */ }
    this.editandoUrls = false;
  }

  private leerLocalStorage(): Partial<Record<ClaveTool, string>> {
    try {
      const raw = localStorage.getItem(LS_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }
}
