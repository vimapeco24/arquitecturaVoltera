# DDD — Domain-Driven Design para Voltera

> Plataforma de energía distribuida: medición inteligente, comercialización P2P, comunidades energéticas y certificados de origen.

---

## 1. Dominio y Subdominios

### Dominio Principal

**Gestión y comercialización de energía distribuida en tiempo real**

Voltera opera en el dominio de la energía eléctrica distribuida, donde prosumidores (hogares y pymes con paneles solares y baterías) generan, consumen, almacenan y venden energía. La plataforma debe medir en tiempo real, facturar de forma precisa, habilitar el comercio entre pares, controlar dispositivos físicos y garantizar el cumplimiento regulatorio — todo sobre infraestructura crítica.

---

### Subdominios

| Subdominio | Tipo | Descripción | Justificación del tipo |
|---|---|---|---|
| **Medición y Telemetría** | Core | Ingesta masiva de lecturas de medidores AMI en tiempo real, validación y almacenamiento | Sin medición no hay negocio: es la fuente de verdad de todo el sistema |
| **Facturación y Liquidación** | Core | Cálculo de consumo neto, aplicación de tarifa dinámica, valoración de excedentes y emisión de factura | Es donde se monetiza: define ingresos y relación contractual con el prosumidor |
| **Mercado P2P de Energía** | Core | Emparejamiento de oferta y demanda de excedentes entre prosumidores vecinos, liquidación de transacciones | Diferenciador competitivo principal de Voltera |
| **Comunidades Energéticas** | Core | Alta, operación y reglas de reparto configurables para comunidades de prosumidores asociados | Habilitado por ley (Decreto 2236/2023), genera lock-in y efecto red |
| **Control de Dispositivos (DER)** | Core | Envío de comandos firmados a baterías, inversores y cargadores; confirmación de ejecución | Infraestructura crítica: un error puede dañar equipos o dejar sin energía |
| **Certificados de Origen** | Supporting | Emisión, transferencia y verificación de certificados de energía renovable en ledger | Importante pero no es el negocio principal; depende de terceros (certificadoras) |
| **Pronóstico de Demanda y Generación** | Supporting | Modelos de ML/IA para predecir consumo y generación solar | Mejora la operación pero Voltera funciona sin pronóstico (degradado) |
| **Respuesta a la Demanda (Flexibilidad)** | Supporting | Orquestación de señales de precio y comandos para desplazar consumo en picos | Negocio futuro con agregadores; hoy es secundario |
| **Identidad y Seguridad** | Generic | Autenticación de usuarios y dispositivos, gestión de credenciales, Zero Trust | Crítico pero no diferenciador: se resuelve con estándares (OAuth2, mTLS) |
| **Notificaciones** | Generic | Push, email, alertas de precio y eventos de red | Commodity: cualquier plataforma de mensajería lo resuelve |
| **Pagos y Cobros** | Generic | Integración con pasarelas de pago, cobro de facturas, pago de excedentes | Se delega a PSPs; no se construye propio |
| **Reportes Regulatorios** | Generic | Generación y envío de reportes a CREG y operador del mercado | Obligatorio pero no diferenciador |

### Diagrama de subdominios

```
                            ┌─────────────────────────────────────────────┐
                            │         DOMINIO VOLTERA                      │
                            │  Gestión y comercialización de energía       │
                            │  distribuida en tiempo real                  │
                            └─────────────────────────────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
    ┌─────────┴─────────┐         ┌──────────┴──────────┐         ┌─────────┴─────────┐
    │       CORE        │         │     SUPPORTING      │         │      GENERIC      │
    └───────────────────┘         └─────────────────────┘         └───────────────────┘
    │                             │                               │
    ├── Medición y Telemetría     ├── Certificados de Origen      ├── Identidad y Seguridad
    ├── Facturación y Liquidación ├── Pronóstico                  ├── Notificaciones
    ├── Mercado P2P de Energía    ├── Respuesta a la Demanda      ├── Pagos y Cobros
    ├── Comunidades Energéticas   │                               ├── Reportes Regulatorios
    └── Control de Dispositivos   │                               │
```

---

## 2. Subdominios → Bounded Contexts

### Mapa de Bounded Contexts

Cada bounded context es un candidato natural a microservicio. La decisión de split/merge se basa en: cohesión del lenguaje, ritmo de cambio, equipo responsable y fronteras transaccionales (agregados).

| Subdominio | Bounded Context | Agregados principales | Justificación de la frontera |
|---|---|---|---|
| Medición y Telemetría | **BC Telemetría** | Medidor, LecturaTelemetría, SesiónIngesta | Altísimo throughput (2.200 lecturas/s), equipo especializado en streaming, base de datos time-series separada |
| Facturación y Liquidación | **BC Facturación** | Factura, PeriodoFacturación, ConsumoNeto, Tarifa | Lógica de negocio regulada (CREG), transacciones financieras, auditoría obligatoria |
| Mercado P2P de Energía | **BC Mercado P2P** | OrdenMercado, TransacciónP2P, LibroOfertas | Motor de matching en tiempo real, latencia crítica (p95 ≤ 300ms), dominio financiero |
| Comunidades Energéticas | **BC Comunidades** | Comunidad, Miembro, ReglaReparto, PeriodoOperación | Reglas de reparto configurables por comunidad, ciclo de vida propio |
| Control de Dispositivos | **BC Control DER** | Dispositivo, Comando, SesiónControl, ReglaAutomación | Infraestructura crítica, fail-safe obligatorio, latencia de comando ≤ 2s |
| Certificados de Origen | **BC Certificados** | Certificado, Generación Verificada, Transferencia | Interacción con ledger/blockchain, validación por terceros |
| Pronóstico | **BC Pronóstico** | ModeloPronóstico, PredicciónDemanda, PredicciónGeneración | Equipo de data science, artefactos ML, ritmo de cambio diferente |
| Respuesta a la Demanda | **BC Flexibilidad** | EventoFlexibilidad, SeñalPrecio, Compensación | Orquestación de comandos masivos, mercado futuro de flexibilidad |
| Identidad y Seguridad | **BC Identidad** | Usuario, DispositivoIoT, Credencial, Sesión | Transversal, estándares OAuth2/OIDC/mTLS, equipo de seguridad |
| Notificaciones | **BC Notificaciones** | CanalNotificación, Preferencia, MensajeEnviado | Infraestructura de mensajería, múltiples canales (push, email, SMS) |
| Pagos y Cobros | **BC Pagos** | OrdenPago, Recaudo, Conciliación | Integración con PSPs, PCI-DSS, idempotencia |
| Reportes Regulatorios | **BC Reportes** | Reporte, PlantillaReguladora, EnvíoRegulatorio | Batch/scheduled, formatos CREG, baja frecuencia de cambio |

### Mapa de Contextos (Context Map)

```
                    ┌──────────────┐
                    │ BC Identidad │◄─── Todos los BCs dependen (Shared Kernel: tokens JWT)
                    └──────────────┘

    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
    │BC Telemetría │────────►│BC Facturación│────────►│  BC Pagos    │
    └──────────────┘ publish └──────────────┘ publish └──────────────┘
          │ LecturaValidada       │ FacturaEmitida         
          │                       │                        
          ▼                       ▼                        
    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
    │BC Control DER│         │BC Mercado P2P│         │BC Certificado│
    └──────────────┘         └──────────────┘         └──────────────┘
          │                       │                        │
          │ ComandoEjecutado      │ TransacciónLiquidada   │ CertificadoEmitido
          ▼                       ▼                        ▼
    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
    │BC Flexibilid.│         │BC Comunidades│         │ BC Reportes  │
    └──────────────┘         └──────────────┘         └──────────────┘
                                    │
                                    ▼
                             ┌──────────────┐
                             │BC Notificac. │
                             └──────────────┘
```

### Relaciones entre Bounded Contexts

| Upstream (publica) | Downstream (consume) | Tipo de relación | Evento/Interfaz |
|---|---|---|---|
| BC Telemetría | BC Facturación | **Published Language** (evento) | `LecturaValidada` |
| BC Telemetría | BC Pronóstico | **Published Language** (evento) | `LecturaValidada` |
| BC Telemetría | BC Control DER | **Partnership** | `AlertaUmbral` |
| BC Facturación | BC Pagos | **Customer-Supplier** | `FacturaEmitida` |
| BC Facturación | BC Reportes | **Conformist** | `FacturaEmitida` |
| BC Mercado P2P | BC Comunidades | **Partnership** | `TransacciónLiquidada` |
| BC Mercado P2P | BC Certificados | **Customer-Supplier** | `GeneraciónVerificada` |
| BC Control DER | BC Flexibilidad | **Customer-Supplier** | `ComandoEjecutado`, `DispositivoDisponible` |
| BC Comunidades | BC Notificaciones | **Customer-Supplier** | `MiembroAgregado`, `RepartoCalculado` |
| BC Identidad | Todos | **Shared Kernel** | Token JWT, políticas de acceso |
| Operador de red (externo) | BC Telemetría | **ACL (Anti-Corruption Layer)** | Traducción DLMS/COSEM → dominio |
| Pasarela de pagos (externo) | BC Pagos | **ACL** | Traducción API PSP → dominio |
| Ledger blockchain (externo) | BC Certificados | **ACL** | Traducción smart contract → dominio |

---

## 3. Lenguaje Ubicuo (Ubiquitous Language)

> El lenguaje ubicuo es el vocabulario compartido entre negocio y tecnología dentro de cada bounded context. Si alguien dice "lectura" en el contexto de Telemetría, TODOS entienden lo mismo.

### BC Telemetría

| Término | Definición | Ejemplo |
|---|---|---|
| **Lectura** | Dato de consumo o generación capturado por un medidor en un instante (timestamp + valor + unidad + dirección) | "La lectura de las 14:30 registró 1.2 kWh de generación" |
| **Medidor** | Dispositivo AMI que captura lecturas bidireccionales y las transmite periódicamente | "El medidor M-2024-001 reporta cada 15 minutos" |
| **Ingesta** | Proceso de recibir, deserializar y validar una lectura entrante | "La ingesta rechazó la lectura por timestamp futuro" |
| **Validación** | Verificación de integridad, rango y consistencia de una lectura | "La validación detectó un salto de 500 kWh — marcada como sospechosa" |
| **Sesión de ingesta** | Conexión activa entre un medidor y la plataforma durante la cual se transmiten lecturas | "La sesión se perdió; las lecturas se bufferean en el medidor" |
| **Lectura sospechosa** | Lectura que pasó validación básica pero tiene valores atípicos que requieren revisión | "3 lecturas sospechosas en el último batch del medidor M-1022" |

### BC Facturación

| Término | Definición | Ejemplo |
|---|---|---|
| **Factura** | Documento de liquidación de un período que detalla consumo, generación, excedentes, tarifa y monto | "La factura de julio muestra $45.000 a favor del prosumidor" |
| **Consumo neto** | Diferencia entre energía consumida de la red y energía inyectada en el período | "El consumo neto fue -120 kWh (generó más de lo que consumió)" |
| **Excedente** | Energía generada y no consumida por el prosumidor, disponible para venta P2P o inyección a red | "El excedente de hoy fue de 8 kWh entre las 10:00 y 14:00" |
| **Tarifa** | Precio por kWh que puede variar por hora, estrato, y tipo de energía (red vs P2P) | "La tarifa pico de las 19:00 es $680/kWh" |
| **Período de facturación** | Intervalo (normalmente mensual) sobre el que se calcula la factura | "El período cierra el día 25 de cada mes" |
| **Crédito de excedente** | Valor monetario reconocido al prosumidor por la energía inyectada a la red | "Se acreditaron $12.400 por excedentes del período" |

### BC Mercado P2P

| Término | Definición | Ejemplo |
|---|---|---|
| **Orden de mercado** | Intención de compra o venta de energía publicada por un prosumidor | "Orden de venta: 5 kWh a $450/kWh, vigente hasta las 16:00" |
| **Emparejamiento (matching)** | Proceso de cruzar una orden de venta con una orden de compra compatible | "El matching emparejó a María (vende) con Pedro (compra) en 180ms" |
| **Transacción P2P** | Intercambio efectivo de energía entre dos prosumidores con precio y cantidad acordados | "Transacción #T-4521: 3 kWh de M-1001 a M-1044 a $420/kWh" |
| **Libro de ofertas** | Registro vivo de todas las órdenes activas de compra y venta | "El libro muestra 340 ofertas de venta y 280 de compra en la comunidad Norte" |
| **Liquidación** | Proceso de confirmar, valorar y registrar una transacción completada | "La liquidación del matching de las 14:00 generó 52 transacciones" |
| **Spread** | Diferencia entre el mejor precio de venta y el mejor precio de compra | "El spread actual es de $30/kWh — mercado líquido" |

### BC Comunidades Energéticas

| Término | Definición | Ejemplo |
|---|---|---|
| **Comunidad** | Agrupación de prosumidores que comparten generación y consumo bajo reglas de reparto comunes | "La comunidad 'Solar del Parque' tiene 45 miembros activos" |
| **Miembro** | Prosumidor asociado a una comunidad con un rol (generador, consumidor o ambos) | "María es miembro generadora con 6 paneles" |
| **Regla de reparto** | Fórmula que define cómo se distribuyen los excedentes entre miembros de la comunidad | "Reparto proporcional al consumo promedio de los últimos 3 meses" |
| **Gestor de comunidad** | Rol que administra las reglas, aprueba miembros y supervisa la operación | "El gestor actualizó la regla de reparto para incluir al nuevo miembro" |
| **Período de operación** | Ciclo sobre el que se calcula y ejecuta el reparto dentro de la comunidad | "El período de operación es semanal para esta comunidad" |
| **Saldo comunitario** | Energía neta disponible para repartir entre los miembros en un período | "El saldo comunitario de esta semana es de 120 kWh excedentes" |

### BC Control de Dispositivos (DER)

| Término | Definición | Ejemplo |
|---|---|---|
| **Dispositivo** | Activo energético controlable: batería, inversor, cargador EV o panel con control | "Dispositivo BAT-2024-001: batería LFP 10kWh, capacidad 90%" |
| **Comando** | Instrucción firmada y autenticada enviada a un dispositivo para cambiar su estado | "Comando: descargar batería al 50% durante pico de las 19:00" |
| **Estado del dispositivo** | Condición actual reportada: nivel de carga, potencia, temperatura, modo | "Estado: cargando, SOC 72%, potencia 3.2 kW" |
| **Regla de automatización** | Condición que dispara un comando automáticamente | "Si precio > $600/kWh Y SOC > 80%, entonces descargar" |
| **Fail-safe** | Estado seguro al que regresa un dispositivo si pierde comunicación o recibe comando inválido | "Fail-safe: batería pasa a modo standby si pierde conexión > 60s" |
| **Sesión de control** | Conexión activa entre la plataforma y un dispositivo para enviar/recibir comandos | "Sesión activa con 342 dispositivos en zona Norte" |

### BC Certificados de Origen

| Término | Definición | Ejemplo |
|---|---|---|
| **Certificado de origen** | Registro verificable que acredita que 1 MWh fue generado de fuente renovable | "Certificado #COO-2024-8821: 1 MWh solar, generado el 15/jul" |
| **Generación verificada** | Producción de energía renovable confirmada por telemetría y validada contra criterios | "230 kWh verificados del medidor M-1001 entre 01-15 julio" |
| **Emisión** | Acto de crear un nuevo certificado asociado a generación verificada | "Se emitieron 3 certificados por la generación de julio" |
| **Transferencia** | Cambio de titularidad de un certificado entre dos partes | "Certificado transferido de María (generadora) a EmpresaX (comprador)" |
| **Doble conteo** | Error en el que la misma generación se certifica más de una vez — debe ser imposible | "El ledger previene doble conteo verificando que el rango no esté cubierto" |
| **Retiro** | Acto de usar un certificado (p.ej. para compensar huella de carbono), dejándolo inactivo | "EmpresaX retiró 10 certificados para su reporte de sostenibilidad" |

### BC Pronóstico

| Término | Definición | Ejemplo |
|---|---|---|
| **Predicción de demanda** | Estimación del consumo futuro de un prosumidor, comunidad o zona | "Predicción: zona Norte consumirá 450 MWh mañana entre 18-22h" |
| **Predicción de generación** | Estimación de la producción solar futura basada en clima y capacidad instalada | "Mañana se espera 60% de generación vs capacidad (nublado)" |
| **Modelo** | Artefacto de ML entrenado que produce predicciones | "Modelo v3.2 entrenado con 6 meses de datos, MAE 4.2%" |
| **Horizonte** | Ventana temporal de la predicción (15 min, 1h, 24h, 7d) | "Pronóstico con horizonte de 24h para la señal de precio" |
| **Señal de precio** | Recomendación de precio futuro derivada del pronóstico para informar decisiones | "Señal: precio esperado mañana 19h = $720/kWh (pico)" |

### BC Flexibilidad (Respuesta a la Demanda)

| Término | Definición | Ejemplo |
|---|---|---|
| **Evento de flexibilidad** | Solicitud del operador o agregador para modificar consumo/generación en un período | "Evento: reducir 50 kW en zona Sur entre 19:00-20:00" |
| **Señal de precio** | Indicador de precio futuro que incentiva al prosumidor a cambiar su comportamiento | "Señal: precio en 2h será $800/kWh — considerar descargar batería" |
| **Compensación** | Pago o crédito al prosumidor por haber respondido a un evento de flexibilidad | "Compensación de $3.200 por reducir 2 kWh durante el pico" |
| **Capacidad disponible** | Potencia que los dispositivos DER pueden ofrecer al mercado de flexibilidad | "Capacidad disponible: 120 kW de baterías con SOC > 60%" |
| **Despacho** | Activación efectiva de dispositivos para responder al evento de flexibilidad | "Despacho ejecutado: 45 baterías descargando 2.5 kW cada una" |

### BC Identidad

| Término | Definición | Ejemplo |
|---|---|---|
| **Usuario** | Persona registrada (prosumidor, gestor, operador) con identidad verificada | "Usuario María García, verificada con cédula, rol: prosumidora" |
| **Dispositivo IoT** | Entidad no-humana (medidor, batería) con identidad propia y credenciales | "Dispositivo M-2024-001, certificado X.509 vigente hasta dic-2025" |
| **Credencial** | Secreto (certificado, token, API key) que autentica a un usuario o dispositivo | "Credencial rotada automáticamente cada 24h" |
| **Sesión** | Período autenticado durante el cual un actor puede operar sin re-autenticarse | "Sesión móvil activa, expira en 4h, refresh token vigente" |
| **Revocación** | Invalidación inmediata de una credencial comprometida | "Credencial del dispositivo BAT-005 revocada en ≤ 5 min" |
| **Mínimo privilegio** | Principio: cada actor tiene solo los permisos necesarios para su función actual | "El medidor solo puede publicar lecturas, no recibir comandos" |

---

## Resumen ejecutivo

| Aspecto | Valor |
|---|---|
| **Dominio** | Gestión y comercialización de energía distribuida en tiempo real |
| **Subdominios Core** | 5 (Telemetría, Facturación, Mercado P2P, Comunidades, Control DER) |
| **Subdominios Supporting** | 3 (Certificados, Pronóstico, Flexibilidad) |
| **Subdominios Generic** | 4 (Identidad, Notificaciones, Pagos, Reportes) |
| **Bounded Contexts** | 12 |
| **Relaciones entre BCs** | 13 (mayoría asíncrona por eventos de dominio) |
| **Patrón de comunicación dominante** | Publish/Subscribe (eventos de dominio) |
| **Patrón de integración con externos** | Anti-Corruption Layer (ACL) |
| **Consistencia intra-BC** | Transaccional (ACID dentro del agregado) |
| **Consistencia inter-BC** | Eventual (eventos + idempotencia) |
