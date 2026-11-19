id: medidor-fallos
titulo: Fallos conocidos · Medidores AMI
categoria: fallos-conocidos
activos: MEDIDOR
fuente: Histórico de incidentes de la red de comunidades (demostración)
---
## Medidor sin señal
La causa más frecuente es la pérdida de enlace con el concentrador o la caída del gateway de la comunidad, no la falla del medidor. Procedimiento: verificar el gateway de la comunidad y la cobertura antes de asumir falla del equipo. Sin lecturas, la liquidación del periodo queda incompleta.
## Lecturas duplicadas o fuera de orden
Se deben a reintentos del concentrador. Se depuran en la ingesta por marca de tiempo.
