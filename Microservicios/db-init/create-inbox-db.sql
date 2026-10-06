-- Crea la base de datos del INBOX de idempotencia (lamina 10), separada de la de
-- series temporales (voltera_tsdb). Se ejecuta UNA sola vez, al inicializar el
-- cluster de PostgreSQL/TimescaleDB (docker-entrypoint-initdb.d).
--
-- Las tablas (inbox_evento, tc_lectura, tc_serie_snapshot) las crean los propios
-- servicios al arrancar con su perfil (DDL idempotente CREATE TABLE IF NOT EXISTS).
CREATE DATABASE voltera_inbox OWNER voltera;

-- CQRS · Database per Service (diagrama 04 · Patrones): BD escritura y lectura
-- separadas por cada servicio de negocio que el diagrama muestra con CQRS.
CREATE DATABASE voltera_tarifas_write OWNER voltera;
CREATE DATABASE voltera_tarifas_read OWNER voltera;
CREATE DATABASE voltera_notif_write OWNER voltera;
CREATE DATABASE voltera_notif_read OWNER voltera;
CREATE DATABASE voltera_habilitacion_write OWNER voltera;
CREATE DATABASE voltera_habilitacion_read OWNER voltera;
