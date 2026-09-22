package com.voltera.tarifas.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidad Raiz («root») del agregado Tarifa.
 * Agrupa franjas horarias con su precio. Invariante: las franjas no se solapan
 * y en conjunto no dejan huecos entre las horas cubiertas (se valida solape).
 */
public class Tarifa {

    private final TarifaId id;
    private final String nombre;
    private final List<FranjaHoraria> franjas;

    private Tarifa(TarifaId id, String nombre, List<FranjaHoraria> franjas) {
        this.id = id;
        this.nombre = nombre;
        this.franjas = franjas;
    }

    /**
     * Factory de dominio: crea una tarifa validando que las franjas no se solapen.
     */
    public static Tarifa crear(String nombre, List<FranjaHoraria> franjas) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la tarifa es obligatorio");
        }
        Objects.requireNonNull(franjas, "franjas requeridas");
        if (franjas.isEmpty()) {
            throw new IllegalArgumentException("La tarifa debe tener al menos una franja");
        }
        for (int i = 0; i < franjas.size(); i++) {
            for (int j = i + 1; j < franjas.size(); j++) {
                if (franjas.get(i).seSolapaCon(franjas.get(j))) {
                    throw new IllegalArgumentException("Las franjas horarias no pueden solaparse");
                }
            }
        }
        return new Tarifa(TarifaId.nuevo(), nombre, new ArrayList<>(franjas));
    }

    /**
     * Factory de dominio: crea una tarifa CONSERVANDO el id dado, validando igual que {@link #crear}.
     */
    public static Tarifa crearCon(TarifaId id, String nombre, List<FranjaHoraria> franjas) {
        Objects.requireNonNull(id, "id requerido");
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la tarifa es obligatorio");
        }
        Objects.requireNonNull(franjas, "franjas requeridas");
        if (franjas.isEmpty()) {
            throw new IllegalArgumentException("La tarifa debe tener al menos una franja");
        }
        for (int i = 0; i < franjas.size(); i++) {
            for (int j = i + 1; j < franjas.size(); j++) {
                if (franjas.get(i).seSolapaCon(franjas.get(j))) {
                    throw new IllegalArgumentException("Las franjas horarias no pueden solaparse");
                }
            }
        }
        return new Tarifa(id, nombre, new ArrayList<>(franjas));
    }

    public static Tarifa reconstituir(TarifaId id, String nombre, List<FranjaHoraria> franjas) {
        return new Tarifa(id, nombre, new ArrayList<>(franjas));
    }

    /**
     * Devuelve el precio aplicable a una hora dada.
     * Invariante de consulta: debe existir una franja que cubra la hora.
     */
    public Precio precioEn(int hora) {
        return franjas.stream()
                .filter(f -> f.cubre(hora))
                .findFirst()
                .map(FranjaHoraria::precio)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay franja definida para la hora " + hora));
    }

    public TarifaId id() { return id; }
    public String nombre() { return nombre; }
    public List<FranjaHoraria> franjas() { return Collections.unmodifiableList(franjas); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tarifa t)) return false;
        return id.equals(t.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
