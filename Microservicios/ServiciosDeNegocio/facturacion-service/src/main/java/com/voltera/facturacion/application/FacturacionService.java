package com.voltera.facturacion.application;

import com.voltera.facturacion.domain.exception.FacturaNoEncontradaException;
import com.voltera.facturacion.domain.model.ConsumoNeto;
import com.voltera.facturacion.domain.model.Dinero;
import com.voltera.facturacion.domain.model.Factura;
import com.voltera.facturacion.domain.model.FacturaId;
import com.voltera.facturacion.domain.model.PeriodoFacturacion;
import com.voltera.facturacion.domain.model.Tarifa;
import com.voltera.facturacion.domain.port.in.ConsultarFacturaUseCase;
import com.voltera.facturacion.domain.port.in.EmitirFacturaUseCase;
import com.voltera.facturacion.domain.port.out.FacturaRepositoryPort;
import com.voltera.facturacion.domain.port.out.TarifaConsultaPort;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de aplicacion: implementa los casos de uso orquestando el dominio
 * y los puertos de salida. NO contiene reglas de negocio (viven en el dominio),
 * solo coordina. Es agnostico de framework (no usa anotaciones Spring aqui:
 * el wiring se hace en la capa de infraestructura).
 */
public class FacturacionService implements EmitirFacturaUseCase, ConsultarFacturaUseCase {

    private final FacturaRepositoryPort repositorio;
    private final TarifaConsultaPort tarifaConsulta;   // puede ser null (modo sin consulta)

    /** Constructor de compatibilidad: sin consulta a tarifas. */
    public FacturacionService(FacturaRepositoryPort repositorio) {
        this(repositorio, null);
    }

    public FacturacionService(FacturaRepositoryPort repositorio, TarifaConsultaPort tarifaConsulta) {
        this.repositorio = repositorio;
        this.tarifaConsulta = tarifaConsulta;
    }

    @Override
    public Factura emitir(ComandoEmitirFactura c) {
        Factura factura = construir(FacturaId.nuevo(), c);
        return repositorio.guardar(factura);
    }

    @Override
    public Factura actualizar(String facturaId, ComandoEmitirFactura c) {
        FacturaId id = FacturaId.de(facturaId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new FacturaNoEncontradaException(facturaId));
        Factura factura = construir(id, c);
        return repositorio.guardar(factura);
    }

    @Override
    public void eliminar(String facturaId) {
        FacturaId id = FacturaId.de(facturaId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new FacturaNoEncontradaException(facturaId));
        repositorio.eliminar(id);
    }

    /**
     * Construye una Factura re-ejecutando la MISMA logica de dominio (incluida la
     * consulta a Tarifas si aplica) con el id indicado. Usado por emitir y actualizar.
     */
    private Factura construir(FacturaId id, ComandoEmitirFactura c) {
        PeriodoFacturacion periodo = PeriodoFacturacion.de(c.periodoInicio(), c.periodoFin());
        ConsumoNeto consumo = ConsumoNeto.de(c.kwhConsumidos(), c.kwhInyectados());

        // Si se indica una tarifaId, se consulta el PRECIO VIGENTE al servicio de
        // Tarifas (salto real entre microservicios). Si no, se usa el precio del comando.
        BigDecimal precioConsumo = c.precioConsumoKwh();
        if (tarifaConsulta != null && c.tarifaId() != null && !c.tarifaId().isBlank()) {
            int hora = c.hora() != null ? c.hora() : 0;
            precioConsumo = tarifaConsulta.obtenerPrecioKwh(c.tarifaId(), hora);
        }

        Tarifa tarifa = Tarifa.de(
                Dinero.de(precioConsumo),
                Dinero.de(c.precioExcedenteKwh())
        );

        return Factura.emitir(id, c.prosumidorId(), periodo, consumo, tarifa);
    }

    @Override
    public Factura porId(String facturaId) {
        return repositorio.buscarPorId(FacturaId.de(facturaId))
                .orElseThrow(() -> new FacturaNoEncontradaException(facturaId));
    }

    @Override
    public List<Factura> porProsumidor(String prosumidorId) {
        return repositorio.buscarPorProsumidor(prosumidorId);
    }
}
