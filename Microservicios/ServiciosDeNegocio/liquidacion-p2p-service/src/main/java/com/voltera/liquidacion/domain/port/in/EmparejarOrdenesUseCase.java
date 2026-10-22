package com.voltera.liquidacion.domain.port.in;

import com.voltera.liquidacion.domain.model.TransaccionP2P;

import java.math.BigDecimal;

/**
 * Puerto de ENTRADA (driving port): casar una orden de venta con una de compra
 * y liquidar la transaccion P2P resultante.
 */
public interface EmparejarOrdenesUseCase {

    TransaccionP2P emparejar(ComandoEmparejar comando);

    TransaccionP2P actualizar(String transaccionId, ComandoEmparejar comando);

    void eliminar(String transaccionId);

    /**
     * Comando inmutable con las dos ordenes a casar. {@code comunidadId} es
     * opcional (metadato off-chain) y puede ser null.
     */
    record ComandoEmparejar(
            String vendedorId,
            BigDecimal kwhVenta,
            BigDecimal precioVenta,
            String compradorId,
            BigDecimal kwhCompra,
            BigDecimal precioCompra,
            String comunidadId
    ) {
        /** Sobrecarga retrocompatible sin comunidad (comunidadId = null). */
        public ComandoEmparejar(String vendedorId, BigDecimal kwhVenta, BigDecimal precioVenta,
                                String compradorId, BigDecimal kwhCompra, BigDecimal precioCompra) {
            this(vendedorId, kwhVenta, precioVenta, compradorId, kwhCompra, precioCompra, null);
        }
    }
}
