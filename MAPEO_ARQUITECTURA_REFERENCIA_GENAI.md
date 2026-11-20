# Mapeo: arquitectura de referencia GenAI vs Voltera

Verificado el 2026-11-19 contra el código de `Microservicios/ServiciosDeNegocio/ia-service` y el diagrama
`Modificados/Voltera_Arquitectura_Completa.drawio` (banda GenAI, nodos `ia-svc`, `ia-gw`, `ia-rag`, `ia-guard`,
`ia-human`, `ia-tools-nota`). XML del diagrama validado.

## 1. Las 8 cajas resaltadas

Rutas relativas a `ia-service/src/main/java/com/voltera/ia/`.

| # | Caja | Estado | Implementación | Nivel de realización |
|---|------|--------|----------------|----------------------|
| 1 | Human Review | Implementada | `application/GobernanzaAccionService` (multi-firma 3/3), `infrastructure/revision/RegistroAlertas`, `DatasetRetroalimentacion`; contrato `MultiFirmaAccionDER` vía blockchain-gateway | Real. La acción nunca se ejecuta sin 3 firmas; firmas ECDSA con `Web3jFirmaCriptografica`. |
| 2 | Knowledge Service (RAG) | Implementada | `infrastructure/knowledge/Bm25ConocimientoDer` (BM25 + TF-IDF coseno + RRF), corpus en `resources/conocimiento/` (12 documentos) | Real, léxico. No usa modelo de embeddings ni base vectorial. |
| 3 | MCP Servers | Implementada | `infrastructure/tools/McpDerAdapter` (telemetría, historial, topología; solo lectura) y `McpBlockchainGatewayAdapter` (crear propuesta, firmar, ejecutar) | Adaptadores dentro del proceso del ia-service, no servidores MCP independientes. Fail-closed. |
| 4 | Agent Registry (A2A) | Implementada | `infrastructure/registry/InMemoryAgentRegistry` (4 agentes + Assistant, dueño, permisos, baja) | En memoria; se pierde al reiniciar. |
| 5 | Guardrails | Implementada | `infrastructure/guardrails/RegexGuardrails` (PII e inyección de prompt) y `infrastructure/modelgateway/ModelGatewayAdapter` | Basado en regex. Sustituible por clasificador tras el puerto. |
| 6 | Prompt & Model Registry | Implementada | `infrastructure/registry/InMemoryPromptModelRegistry` (PRODUCCION, CANDIDATA, RETIRADA; promover y revertir con historial); compuerta de Evals en `application/EvalsService` | En memoria. Evals con dataset `resources/evals/casos-der.json`. |
| 7 | Identity (IAM) | Implementada, opcional | `infrastructure/security/` (`IamFilter`, `JwtVerificador`, `IamContexto`). Con `IA_IAM_ENABLED=true` exige Bearer JWT (HS256 o RS256 con clave pública del IdP), valida firma, expiración y emisor, toma la identidad del token y exige el rol OPERADOR/PROSUMIDOR/AGENTE_IA para firmar | Apagado por defecto para no romper la demo abierta. Valida tokens, no los emite: falta configurar el IdP (WSO2) y que el front envíe el token. |
| 8 | Audit & Retention | Implementada | `infrastructure/audit/AuditoriaIa` (registros redactados, plazo y tope de retención), `LogAuditLog`; endpoints `.../propuestas/{id}/auditoria` y `.../gobernanza/metricas` | Registro en memoria con política de retención declarada. |

El diagrama ya refleja las 8 cajas: `ia-gw` (5, 6), `ia-rag` (2), `ia-guard` (4, 7), `ia-human` (1, 8) e
`ia-tools-nota` (3). No hizo falta cambiar el `.drawio`.

## 2. Brechas respecto a lo que implica cada caja

1. **IAM (7):** implementado y probado (opt-in). Falta activarlo en Render con la clave del IdP y que el front envíe el Bearer.
2. **Persistencia:** registro de agentes, registro de prompts, auditoría y propuestas están en memoria.
3. **MCP (3):** si se exige aislamiento real, extraer cada adaptador a un servicio MCP propio.
4. **RAG (2):** sin modelo de embeddings ni índice vectorial (la referencia muestra "Vector Index & Graph" y "Embedding Model").
5. **Guardrails (5):** regex en lugar de un clasificador PII/Safety.

## 3. Otras cajas de la referencia (no resaltadas)

| Caja de la referencia | Estado en Voltera |
|-----------------------|-------------------|
| Model Gateway | Implementado (`ModelGatewayAdapter`; `provider=none` usa motor local). |
| Evals & Tracing | Implementado (`EvalsService`, `ObservabilidadWebConfig`). |
| Agent Harness / State & Memory | Parcial: orquestación en `DiagnosticoDerService`, repositorios en memoria. |
| Event Bus | Implementado: tópico `der.gobernanza` y Redpanda del resto de la plataforma. |
| Frontier LLM, SLM, Reranker, Safety Model, ML Platform | No hay LLM contratado; el motor local lo sustituye. |
| Secrets & Keys, FinOps, Data Classification | Secretos por variables de entorno en Render; sin FinOps ni clasificación formal. |

## 4. Validación contra la carpeta `IA/` (13 PDF)

La carpeta tiene 13 PDF que son 5 páginas distintas de arquibook, varias repetidas con nombres alterados
(`1virtual`, `virt1ual`, etc.; son capturas distintas de la misma página, no copias idénticas). Páginas:
`arquitectura-referencia-genai` (8 capturas), `taller-propuesta-ia` (3 capturas: pasos 1, 2 y 3 con las
notas del Grupo 5), `genai-en-solventa`, `arquitectura-solventa-completa` y `caso-siniestro-genai`.

Criterios "Listo cuando" del taller y del caso integrador, contrastados con el experimento
(`EXPERIMENTO_IA_DER_VOLTERA.md`, 68/68 criterios en la última corrida local):

| Criterio de `IA/` | Cubierto por el experimento | Observación |
|-------------------|-----------------------------|-------------|
| Paso 1: caso, indicador y qué decide una persona (MTTD 45 min → < 5 min; −40 % incidentes de facturación) | Parcial | Bloque O mide el MTTD (< 5 min). El −40 % de incidentes que afectan facturación no se mide en ningún bloque. |
| Paso 3: ≥ 3 cajas con nota; deben estar quién llama al modelo, por dónde pasa el dato sensible y quién revisa | Sí | Model Gateway (bloque O), PII/Guardrails (bloque E), Human Review 3/3 (bloque B). La nota del taller para Human Review pide al menos 2 personas; el proyecto exige 3/3, que lo cumple. |
| Paso 3: RAG con permisos del usuario y fuentes citadas | Sí | Bloques G, N y O (`rol=`). |
| Caso integrador: interrupción con checkpoint antes de acciones con efecto | Sí | El flujo se detiene en `human-review`; propuesta con TTL (bloque B5). |
| Caso integrador: MCP con mínimo privilegio (el agente no tiene la herramienta de aprobar) | Sí | El Agente-Acción solo propone; ejecutar exige 3/3. |
| Caso integrador: dos modelos (SLM local enmascara, LLM de frontera redacta) | Parcial | El gateway clasifica por dato y mantiene el RESTRINGIDO en el servicio, pero `provider=none` usa motor local. No hay SLM ni LLM de frontera reales. |
| Paso 2, microservicios: Tarifas, Pagos, Facturación, Telemetría y Notificación como herramientas MCP | Sí (cerrada) | `McpNegocioAdapter` (solo lectura: tarifas, facturación, pagos, certificados) + `McpDerAdapter` (telemetría). Notificaciones ya existía. Sin URL pública la evidencia queda «no verificable». Experimento Q. |
| Paso 2, eventos: qué evento dispara a la IA y si el índice del RAG se mantiene con eventos | Sí (cerrada) | `EventosKafkaConsumer`: `topic.notificaciones` (MedidorSinReporte → diagnóstico, idempotente) y `topic.ia.conocimiento` (ingesta/supresión del RAG). En la nube, puente HTTP `POST /api/v1/ia/eventos/*`. Experimento Q. |
| Paso 2, blockchain: «consulta certificados de energía renovable emitidos por Voltera» | Sí (cerrada) | La herramienta `tool:certificados-renovables` consulta `certificados-renovables-service`; además la cadena sigue como gobernanza 3/3. |
| Paso 4: tabla de servicios nuevos, modificados y retirados con atributo de calidad | Sí | `Voltera_Modulo5_Agente_Diagnostico_DER- Final.md`, sección 3 (retirados: revisión manual de dashboards y triage manual de alarmas). |

### Cierre de brechas (2026-11-20)
Se cerraron las tres brechas en negrita con código y el bloque Q del experimento (6 criterios; corrida local
74/74 con `--sin-onchain`; JUnit del ia-service 54/54). Quedan abiertas, declaradas (actualizado 2026-11-20, segunda pasada):
- El **−40 % de incidentes que afectan facturación** es una meta de negocio que solo se puede medir con datos
  de operación reales; el experimento mide el MTTD (< 5 min), no ese indicador.
- Los consumidores Kafka solo corren con `voltera.kafka.enabled=true` (on-premise). Se probaron contra un broker
  embebido real (`EventosKafkaConsumerIT`); no contra el Redpanda del clúster.
- IAM está implementado pero apagado por defecto: falta activarlo con la clave del IdP y que el front envíe el token.
- Sigue sin haber SLM ni LLM de frontera reales (el Model Gateway ya admite un proveedor OpenAI-compatible por
  variables de entorno) ni persistencia de registros, auditoría y propuestas (decisión de alcance: en memoria,
  igual que el Outbox del resto de servicios).
