package com.voltera.reaseguro.application;

import com.voltera.reaseguro.domain.event.SiniestroAprobado;
import com.voltera.reaseguro.domain.exception.CesionNoEncontradaException;
import com.voltera.reaseguro.domain.model.CesionRiesgo;
import com.voltera.reaseguro.domain.model.CesionVista;
import com.voltera.reaseguro.domain.port.in.ConsultarCesionUseCase;
import com.voltera.reaseguro.domain.port.in.ProcesarEventoSiniestroUseCase;
import com.voltera.reaseguro.domain.port.out.CesionRepositoryPort;
import com.voltera.reaseguro.domain.port.out.CesionVistaRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicacion del bounded context Reaseguro.
 *
 * <p>Implementa CQRS: la escritura vive en {@link CesionRepositoryPort} y la
 * lectura se sirve desde la proyeccion {@link CesionVistaRepositoryPort}. El
 * procesamiento del evento es idempotente por {@code eventId}.</p>
 */
public class ReaseguroService implements ProcesarEventoSiniestroUseCase, ConsultarCesionUseCase {

    private final CesionRepositoryPort escrituraRepo;
    private final CesionVistaRepositoryPort lecturaRepo;

    public ReaseguroService(CesionRepositoryPort escrituraRepo,
                            CesionVistaRepositoryPort lecturaRepo) {
        this.escrituraRepo = escrituraRepo;
        this.lecturaRepo = lecturaRepo;
    }

    @Override
    public CesionRiesgo procesar(SiniestroAprobado evento) {
        // 1. IDEMPOTENCIA: si ya procesamos este eventId, devolvemos la cesion existente.
        var existente = escrituraRepo.buscarPorEventId(evento.eventId());
        if (existente.isPresent()) {
            return existente.get();
        }

        // 2. Transferencia de estado: creamos la cesion con los datos del evento.
        CesionRiesgo cesion = CesionRiesgo.crearDesdeEvento(
                evento.eventId(),
                evento.siniestroId(),
                evento.polizaId(),
                evento.prosumidorId(),
                evento.montoAprobado()
        );

        // 3. Simulacion de aprobacion por la reaseguradora: PENDIENTE -> CEDIDA.
        cesion.confirmarCesion();

        // 4. Guardamos en el modelo de ESCRITURA.
        escrituraRepo.guardar(cesion);

        // 5. Proyectamos a la vista de LECTURA (CQRS).
        lecturaRepo.guardar(CesionVista.desdeEscritura(cesion));

        return cesion;
    }

    @Override
    public CesionVista porId(String id) {
        return lecturaRepo.buscarPorId(id)
                .orElseThrow(() -> new CesionNoEncontradaException(
                        "No se encontro la cesion con id: " + id));
    }

    @Override
    public List<CesionVista> porPoliza(String polizaId) {
        return lecturaRepo.buscarPorPoliza(polizaId);
    }

    @Override
    public List<CesionVista> todas() {
        return lecturaRepo.buscarTodas();
    }
}
