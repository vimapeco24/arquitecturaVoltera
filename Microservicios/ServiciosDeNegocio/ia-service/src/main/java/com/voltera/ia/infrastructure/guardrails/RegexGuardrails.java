package com.voltera.ia.infrastructure.guardrails;

import com.voltera.ia.domain.exception.GuardrailViolationException;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Guardrails declarativos (politicas versionadas, no instrucciones dentro del
 * prompt). Implementacion ligera basada en patrones: enmascara PII y detecta
 * intentos basicos de inyeccion de prompt / temas fuera de alcance.
 *
 * <p>Es un PII Detector / Safety Model de demostracion: suficiente para mostrar
 * el patron arquitectonico sin dependencias externas. En produccion se sustituye
 * por un clasificador dedicado sin tocar al agente (esta detras del puerto).</p>
 */
@Component
public class RegexGuardrails implements GuardrailsPort {

    // Correos, telefonos largos y numeros de documento/cuenta (>= 6 digitos).
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern DIGITOS_LARGOS = Pattern.compile("\\b\\d{6,}\\b");

    private static final List<String> FRASES_INYECCION = List.of(
            "ignore previous", "ignora las instrucciones", "olvida las instrucciones",
            "actua como", "system prompt", "revela tus instrucciones", "jailbreak");

    @Override
    public String enmascararPII(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String out = EMAIL.matcher(texto).replaceAll("[EMAIL]");
        out = DIGITOS_LARGOS.matcher(out).replaceAll("[ID]");
        return out;
    }

    @Override
    public void validarEntrada(String texto) {
        if (texto == null) return;
        String lower = texto.toLowerCase();
        for (String frase : FRASES_INYECCION) {
            if (lower.contains(frase)) {
                throw new GuardrailViolationException(
                        "Guardrail de entrada: posible inyeccion de prompt detectada ('" + frase + "').");
            }
        }
    }
}
