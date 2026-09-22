package com.voltera.liquidacionmensual.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidad Raiz («root») del agregado LiquidacionMensual.
 *
 * Funcionalidad requerida (paso 4 del flujo): TOTALIZA los excedentes y consumos
 * del mes de un prosumidor. Acumula movimientos (consumos y excedentes) mientras
 * esta ABIERTA y, al cerrar, calcula los totales y el neto que sirve de base para
 * la factura mensual.
 *
 * Es la unica puerta de entrada: los movimientos solo se agregan a traves de ella,
 * que hace valer las invariantes (no acumular sobre una liquidacion CERRADA).
 */
public class LiquidacionMensual {

    private final LiquidacionId id;
    private final String prosumidorId;
    private final Periodo periodo;
    private EstadoLiquidacion estado;
    private final List<Movimiento> movimientos;
    private final Instant creadaEn;
    private Instant cerradaEn;
    // Precios aplicados para valorar la liquidacion (COP/kWh). Se fijan al abrir.
    private final BigDecimal precioConsumoKwh;
    private final BigDecimal precioExcedenteKwh;

    private LiquidacionMensual(LiquidacionId id, String prosumidorId, Periodo periodo,
                               EstadoLiquidacion estado, List<Movimiento> movimientos,
                               Instant creadaEn, Instant cerradaEn,
                               BigDecimal precioConsumoKwh, BigDecimal precioExcedenteKwh) {
        this.id = id;
        this.prosumidorId = prosumidorId;
        this.periodo = periodo;
        this.estado = estado;
        this.movimientos = movimientos;
        this.creadaEn = creadaEn;
        this.cerradaEn = cerradaEn;
        this.precioConsumoKwh = precioConsumoKwh;
        this.precioExcedenteKwh = precioExcedenteKwh;
    }

    /** Abre una liquidacion mensual con precios por defecto (compatibilidad). */
    public static LiquidacionMensual abrir(String prosumidorId, Periodo periodo) {
        return abrir(prosumidorId, periodo, null, null);
    }

    /**
     * Abre una nueva liquidacion mensual para un prosumidor y periodo, fijando
     * los precios (COP/kWh) con los que se valorara al cerrar. Si no se indican,
     * se usan valores por defecto razonables (consumo 680, excedente 420).
     */
    public static LiquidacionMensual abrir(String prosumidorId, Periodo periodo,
                                           BigDecimal precioConsumoKwh, BigDecimal precioExcedenteKwh) {
        if (prosumidorId == null || prosumidorId.isBlank()) {
            throw new IllegalArgumentException("prosumidorId requerido");
        }
        Objects.requireNonNull(periodo, "periodo requerido");
        BigDecimal pc = normalizarPrecio(precioConsumoKwh, new BigDecimal("680"));
        BigDecimal pe = normalizarPrecio(precioExcedenteKwh, new BigDecimal("420"));
        return new LiquidacionMensual(LiquidacionId.nuevo(), prosumidorId, periodo,
                EstadoLiquidacion.ABIERTA, new ArrayList<>(), Instant.now(), null, pc, pe);
    }

    /**
     * Abre (rehace) una liquidacion mensual CONSERVANDO el id dado, aplicando las
     * mismas invariantes que {@link #abrir}. Reinicia los movimientos y el estado.
     */
    public static LiquidacionMensual abrirCon(LiquidacionId id, String prosumidorId, Periodo periodo,
                                              BigDecimal precioConsumoKwh, BigDecimal precioExcedenteKwh) {
        Objects.requireNonNull(id, "id requerido");
        if (prosumidorId == null || prosumidorId.isBlank()) {
            throw new IllegalArgumentException("prosumidorId requerido");
        }
        Objects.requireNonNull(periodo, "periodo requerido");
        BigDecimal pc = normalizarPrecio(precioConsumoKwh, new BigDecimal("680"));
        BigDecimal pe = normalizarPrecio(precioExcedenteKwh, new BigDecimal("420"));
        return new LiquidacionMensual(id, prosumidorId, periodo,
                EstadoLiquidacion.ABIERTA, new ArrayList<>(), Instant.now(), null, pc, pe);
    }

    private static BigDecimal normalizarPrecio(BigDecimal valor, BigDecimal porDefecto) {
        BigDecimal v = (valor == null) ? porDefecto : valor;
        if (v.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    public static LiquidacionMensual reconstituir(LiquidacionId id, String prosumidorId, Periodo periodo,
                                                  EstadoLiquidacion estado, List<Movimiento> movimientos,
                                                  Instant creadaEn, Instant cerradaEn) {
        return new LiquidacionMensual(id, prosumidorId, periodo, estado,
                new ArrayList<>(movimientos), creadaEn, cerradaEn,
                new BigDecimal("680.00"), new BigDecimal("420.00"));
    }

    /** Invariante: solo se acumulan movimientos mientras la liquidacion esta ABIERTA. */
    public void registrarMovimiento(Movimiento movimiento) {
        Objects.requireNonNull(movimiento, "movimiento requerido");
        if (estado != EstadoLiquidacion.ABIERTA) {
            throw new IllegalStateException(
                    "No se pueden registrar movimientos en una liquidacion CERRADA");
        }
        movimientos.add(movimiento);
    }

    /** Invariante: solo una liquidacion ABIERTA puede cerrarse (una vez). */
    public void cerrar() {
        if (estado != EstadoLiquidacion.ABIERTA) {
            throw new IllegalStateException("Solo una liquidacion ABIERTA puede cerrarse");
        }
        this.estado = EstadoLiquidacion.CERRADA;
        this.cerradaEn = Instant.now();
    }

    /** Total de kWh consumidos en el mes. */
    public Energia totalConsumo() {
        return movimientos.stream()
                .filter(Movimiento::esConsumo)
                .map(Movimiento::energia)
                .reduce(Energia.cero(), Energia::sumar);
    }

    /** Total de kWh de excedente (inyectado) en el mes. */
    public Energia totalExcedente() {
        return movimientos.stream()
                .filter(Movimiento::esExcedente)
                .map(Movimiento::energia)
                .reduce(Energia.cero(), Energia::sumar);
    }

    /**
     * Neto del mes = consumo - excedente.
     * Positivo => el prosumidor debe pagar; negativo => tiene saldo a favor.
     * Este neto es la base que consume el servicio de Facturacion mensual.
     */
    public BigDecimal netoKwh() {
        return totalConsumo().restarComoBigDecimal(totalExcedente());
    }

    public boolean tieneSaldoAFavor() {
        return netoKwh().compareTo(BigDecimal.ZERO) < 0;
    }

    // ----------------- Valoracion economica (COP) -----------------

    public BigDecimal precioConsumoKwh() { return precioConsumoKwh; }
    public BigDecimal precioExcedenteKwh() { return precioExcedenteKwh; }

    /** Cargo por consumo = consumo (kWh) * precioConsumo (COP/kWh). */
    public BigDecimal cargoConsumo() {
        return totalConsumo().kwh().multiply(precioConsumoKwh).setScale(2, RoundingMode.HALF_UP);
    }

    /** Credito por excedente = excedente (kWh) * precioExcedente (COP/kWh). */
    public BigDecimal creditoExcedente() {
        return totalExcedente().kwh().multiply(precioExcedenteKwh).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Neto economico del mes en COP = cargoConsumo - creditoExcedente.
     * Positivo => el prosumidor debe pagar; negativo => saldo a favor.
     * Esta es la base monetaria que consume la Facturacion mensual.
     */
    public BigDecimal netoCop() {
        return cargoConsumo().subtract(creditoExcedente()).setScale(2, RoundingMode.HALF_UP);
    }

    public LiquidacionId id() { return id; }
    public String prosumidorId() { return prosumidorId; }
    public Periodo periodo() { return periodo; }
    public EstadoLiquidacion estado() { return estado; }
    public List<Movimiento> movimientos() { return Collections.unmodifiableList(movimientos); }
    public int cantidadMovimientos() { return movimientos.size(); }
    public Instant creadaEn() { return creadaEn; }
    public Instant cerradaEn() { return cerradaEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LiquidacionMensual l)) return false;
        return id.equals(l.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
