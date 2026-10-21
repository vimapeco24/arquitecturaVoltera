# Arquitectura Voltera — diagrama vivo (REGLA PERMANENTE)

> Esta regla se aplica SIEMPRE, en todas las sesiones. No borrar.

## Diagrama de arquitectura completa (fuente de verdad visual)

El estado completo y actual de la arquitectura de Voltera está en:

**`Modificados/Voltera_Arquitectura_Completa.drawio`**

Es un diagrama draw.io (XML `mxGraphModel` sin comprimir, importable en Lucidchart/draw.io)
con 5 capas: (1) cliente/borde, (2) los 17 microservicios, (3) event mesh Redpanda/Kafka,
(4) blockchain (Polygon Amoy), (5) observabilidad e infraestructura.

## OBLIGACIÓN EN CADA CAMBIO DE ARQUITECTURA

Cada vez que se **agregue, modifique o elimine** algo que afecte la arquitectura, hay que
**actualizar también `Voltera_Arquitectura_Completa.drawio`** en el mismo trabajo. Aplica a:

- Nuevos microservicios o cambios de puerto/rol de los existentes.
- Nuevos topics de Kafka/Redpanda o cambios de nombres.
- Cambios en el borde (WSO2, Istio, BFF, Vercel) o en el gateway blockchain / contrato / red.
- Nuevos patrones (Outbox, Inbox, CQRS, store-and-forward, ACL) aplicados a un servicio.
- Cambios de infraestructura (K8s, KEDA, observabilidad) relevantes.

Reglas de edición del diagrama:
- Mantener la codificación por color de capa ya establecida (azul cliente · morado gateway/mesh ·
  amarillo negocio · verde cadena EDA · naranja blockchain · gris infra).
- Validar que siga siendo XML bien formado tras editar
  (`/Library/Developer/CommandLineTools/usr/bin/python3 -c "import xml.dom.minidom; xml.dom.minidom.parse('<ruta>')"`).
- No usar stencils `mxgraph.archimate*` ni imágenes `images.lucid.app` (no cargan en Lucidchart).

## Registro persistente entre sesiones

Al retomar una sesión, leer primero `SESION_BLOCKCHAIN_ESTADO.md` y las bitácoras
(`BITACORA_AVANCE.md`, `Microservicios/BITACORA_ENTREGA3_EDA.md`, `BLOCKCHAIN_VOLTERA.md`).
Cuando se actualice el diagrama, dejar constancia en `SESION_BLOCKCHAIN_ESTADO.md` (qué se cambió y por qué),
para que la siguiente sesión sepa que el `.drawio` refleja el estado real.
