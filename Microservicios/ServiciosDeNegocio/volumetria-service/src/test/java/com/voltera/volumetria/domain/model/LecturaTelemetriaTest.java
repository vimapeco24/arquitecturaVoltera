package com.voltera.volumetria.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Agregado LecturaTelemetria - validacion")
class LecturaTelemetriaTest {

    @Test
    @DisplayName("Lectura normal se marca VALIDA")
    void lecturaValida() {
        Medida m = Medida.de(BigDecimal.valueOf(2.5), Medida.Direccion.CONSUMO);
        LecturaTelemetria l = LecturaTelemetria.ingestar(
                MedidorId.de("MED-001"), m, Instant.now().minus(1, ChronoUnit.MINUTES));
        assertEquals(EstadoLectura.VALIDA, l.estado());
        assertTrue(l.esValida());
    }

    @Test
    @DisplayName("Lectura con valor atipico (> 500 kWh) se marca SOSPECHOSA")
    void lecturaSospechosa() {
        Medida m = Medida.de(BigDecimal.valueOf(750), Medida.Direccion.CONSUMO);
        LecturaTelemetria l = LecturaTelemetria.ingestar(
                MedidorId.de("MED-002"), m, Instant.now().minus(1, ChronoUnit.MINUTES));
        assertEquals(EstadoLectura.SOSPECHOSA, l.estado());
        assertFalse(l.esValida());
    }

    @Test
    @DisplayName("Lectura con timestamp futuro se marca RECHAZADA")
    void lecturaRechazada() {
        Medida m = Medida.de(BigDecimal.valueOf(3), Medida.Direccion.GENERACION);
        LecturaTelemetria l = LecturaTelemetria.ingestar(
                MedidorId.de("MED-003"), m, Instant.now().plus(1, ChronoUnit.HOURS));
        assertEquals(EstadoLectura.RECHAZADA, l.estado());
    }
}
