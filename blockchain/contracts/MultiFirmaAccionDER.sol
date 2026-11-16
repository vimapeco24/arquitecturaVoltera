// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";

/**
 * @title MultiFirmaAccionDER
 * @notice Gobernanza MULTI-FIRMA 3/3 sobre acciones correctivas de activos DER
 *         (Módulo 5 · Agente de diagnóstico con gobernanza multi-firma). Es el
 *         patrón MULTIPLE AUTHORIZATION (familia SEGURIDAD) especializado a TRES
 *         firmantes TIPADOS y OBLIGATORIOS, no un M-de-N anónimo:
 *
 *            Firma 1 · AGENTE_IA    — el agente certifica su recomendación.
 *            Firma 2 · OPERADOR     — el operador valida técnicamente.
 *            Firma 3 · PROSUMIDOR   — el dueño del activo consiente.
 *
 *  - PROBLEMA: ejecutar un comando físico sobre un DER (reiniciar inversor,
 *    descargar batería, cortar suministro) sin autorización trazable de todas
 *    las partes es un riesgo operativo, regulatorio y de confianza.
 *  - SOLUCIÓN: la acción solo se EJECUTA cuando las TRES firmas distintas y de
 *    los tres roles están registradas (3/3). No existe modo degradado 2/3
 *    (fail-closed). Un rechazo de cualquier firmante cierra la propuesta; si
 *    vence el TTL sin las 3 firmas, queda VENCIDA y no se ejecuta.
 *  - CONSECUENCIAS:
 *      Se GANA: no repudio y trazabilidad del 100 % de las acciones (quién
 *               propuso, quién firmó, cuándo se ejecutó); soberanía del
 *               prosumidor sobre su activo; auditoría regulatoria (CREG).
 *      Se PAGA: mayor latencia (3 transacciones) y gas; custodia de 3 llaves.
 *  - FAMILIA: SEGURIDAD.
 *
 * Trazabilidad Tema 5: cada propuesta guarda el hash del diagnóstico y la
 * versión de prompt+modelo que la generaron, de forma inmutable on-chain.
 *
 * Alineado con el árbol de decisión: on-chain solo IDs y hashes (bytes32), nunca
 * datos personales; roles vía AccessControl = Embedded Permission (Zero Trust).
 */
contract MultiFirmaAccionDER is AccessControl, Pausable {
    bytes32 public constant AGENTE_IA_ROLE = keccak256("AGENTE_IA_ROLE");
    bytes32 public constant OPERADOR_ROLE = keccak256("OPERADOR_ROLE");
    bytes32 public constant PROSUMIDOR_ROLE = keccak256("PROSUMIDOR_ROLE");

    uint8 public constant FIRMAS_REQUERIDAS = 3;

    enum Estado {
        INEXISTENTE, // 0
        PENDIENTE,   // 1 — propuesta, faltan firmas
        APROBADA,    // 2 — 3/3 completas, lista para ejecutar
        RECHAZADA,   // 3 — un firmante rechazó
        VENCIDA,     // 4 — expiró el TTL sin 3/3
        EJECUTADA    // 5 — comando emitido
    }

    enum Rol { AGENTE_IA, OPERADOR, PROSUMIDOR }

    struct Propuesta {
        bytes32 activoId;        // activo DER objetivo
        bytes32 comandoHash;     // keccak256 del comando + parámetros
        bytes32 hashDiagnostico; // hash del diagnóstico que la justifica
        bytes32 versionPrompt;   // versión de prompt (Tema 5)
        bytes32 versionModelo;   // versión de modelo (Tema 5)
        uint8   aprobaciones;    // firmas de aprobación recogidas (0..3)
        uint64  creadaEn;
        uint64  venceEn;
        Estado  estado;
    }

    mapping(bytes32 => Propuesta) private _propuestas;
    // propuestaId => rol => ya firmó (evita doble firma del mismo rol).
    mapping(bytes32 => mapping(uint8 => bool)) private _firmoRol;

    uint256 public totalPropuestas;
    uint256 public totalEjecutadas;

    event PropuestaCreada(
        bytes32 indexed propuestaId,
        bytes32 indexed activoId,
        bytes32 comandoHash,
        bytes32 hashDiagnostico,
        bytes32 versionPrompt,
        bytes32 versionModelo,
        address indexed agente,
        uint64 creadaEn,
        uint64 venceEn
    );
    event FirmaRegistrada(bytes32 indexed propuestaId, Rol rol, address indexed firmante, uint8 aprobaciones);
    event AccionAprobada(bytes32 indexed propuestaId, bytes32 indexed activoId, uint64 aprobadaEn);
    event AccionRechazada(bytes32 indexed propuestaId, Rol rol, address indexed firmante);
    event AccionVencida(bytes32 indexed propuestaId, uint64 vencidaEn);
    event AccionEjecutada(bytes32 indexed propuestaId, bytes32 indexed activoId, uint64 ejecutadaEn);

    error PropuestaIdVacio();
    error DatoVacio();
    error PropuestaYaExiste(bytes32 id);
    error PropuestaNoExiste(bytes32 id);
    error NoPendiente(bytes32 id);
    error RolYaFirmo(bytes32 id, Rol rol);
    error NoAprobada(bytes32 id);
    error TtlInvalido();

    /**
     * @param admin recibe DEFAULT_ADMIN_ROLE. Debe otorgar los tres roles de
     *        firmante (AGENTE_IA, OPERADOR, PROSUMIDOR) a cuentas distintas; sin
     *        los tres roles ninguna propuesta puede alcanzar 3/3 (fail-closed).
     */
    constructor(address admin) {
        require(admin != address(0), "admin invalido");
        _grantRole(DEFAULT_ADMIN_ROLE, admin);
    }

    /**
     * @notice Firma 1: el AGENTE_IA crea la propuesta (cuenta como su firma, 1/3).
     * @param ttlSegundos vigencia de la propuesta; vencido el plazo sin 3/3 no se ejecuta.
     */
    function crearPropuesta(
        bytes32 propuestaId,
        bytes32 activoId,
        bytes32 comandoHash,
        bytes32 hashDiagnostico,
        bytes32 versionPrompt,
        bytes32 versionModelo,
        uint64 ttlSegundos
    ) external onlyRole(AGENTE_IA_ROLE) whenNotPaused {
        if (propuestaId == bytes32(0)) revert PropuestaIdVacio();
        if (activoId == bytes32(0) || comandoHash == bytes32(0) || hashDiagnostico == bytes32(0)) {
            revert DatoVacio();
        }
        if (ttlSegundos == 0) revert TtlInvalido();
        if (_propuestas[propuestaId].estado != Estado.INEXISTENTE) revert PropuestaYaExiste(propuestaId);

        uint64 ahora = uint64(block.timestamp);
        _propuestas[propuestaId] = Propuesta({
            activoId: activoId,
            comandoHash: comandoHash,
            hashDiagnostico: hashDiagnostico,
            versionPrompt: versionPrompt,
            versionModelo: versionModelo,
            aprobaciones: 1,
            creadaEn: ahora,
            venceEn: ahora + ttlSegundos,
            estado: Estado.PENDIENTE
        });
        _firmoRol[propuestaId][uint8(Rol.AGENTE_IA)] = true;
        totalPropuestas += 1;

        emit PropuestaCreada(
            propuestaId, activoId, comandoHash, hashDiagnostico,
            versionPrompt, versionModelo, msg.sender, ahora, ahora + ttlSegundos
        );
        emit FirmaRegistrada(propuestaId, Rol.AGENTE_IA, msg.sender, 1);
    }

    /// @notice Firma 2: el OPERADOR aprueba (valida técnicamente).
    function firmarOperador(bytes32 propuestaId) external onlyRole(OPERADOR_ROLE) whenNotPaused {
        _aprobar(propuestaId, Rol.OPERADOR);
    }

    /// @notice Firma 3: el PROSUMIDOR aprueba (consiente sobre su activo).
    function firmarProsumidor(bytes32 propuestaId) external onlyRole(PROSUMIDOR_ROLE) whenNotPaused {
        _aprobar(propuestaId, Rol.PROSUMIDOR);
    }

    /// @notice Cualquier firmante humano puede RECHAZAR; cierra la propuesta.
    function rechazarOperador(bytes32 propuestaId) external onlyRole(OPERADOR_ROLE) whenNotPaused {
        _rechazar(propuestaId, Rol.OPERADOR);
    }

    function rechazarProsumidor(bytes32 propuestaId) external onlyRole(PROSUMIDOR_ROLE) whenNotPaused {
        _rechazar(propuestaId, Rol.PROSUMIDOR);
    }

    /**
     * @notice Ejecuta la acción. SOLO procede desde APROBADA (3/3). Es el único
     *         punto donde se emite AccionEjecutada; off-chain el MCP Blockchain
     *         Gateway lo traduce en el comando vía MCP Device Management.
     */
    function ejecutar(bytes32 propuestaId) external onlyRole(AGENTE_IA_ROLE) whenNotPaused {
        Propuesta storage p = _propuestas[propuestaId];
        if (p.estado == Estado.INEXISTENTE) revert PropuestaNoExiste(propuestaId);
        if (p.estado != Estado.APROBADA) revert NoAprobada(propuestaId);
        uint64 ahora = uint64(block.timestamp);
        p.estado = Estado.EJECUTADA;
        totalEjecutadas += 1;
        emit AccionEjecutada(propuestaId, p.activoId, ahora);
    }

    /// @notice Marca VENCIDA una propuesta cuyo TTL expiró sin completar 3/3.
    function caducar(bytes32 propuestaId) external whenNotPaused {
        Propuesta storage p = _propuestas[propuestaId];
        if (p.estado != Estado.PENDIENTE) revert NoPendiente(propuestaId);
        require(block.timestamp > p.venceEn, "aun vigente");
        uint64 ahora = uint64(block.timestamp);
        p.estado = Estado.VENCIDA;
        emit AccionVencida(propuestaId, ahora);
    }

    // ── interno ─────────────────────────────────────────────────────────────

    function _aprobar(bytes32 propuestaId, Rol rol) private {
        Propuesta storage p = _propuestas[propuestaId];
        if (p.estado == Estado.INEXISTENTE) revert PropuestaNoExiste(propuestaId);
        if (p.estado != Estado.PENDIENTE) revert NoPendiente(propuestaId);
        if (block.timestamp > p.venceEn) {
            p.estado = Estado.VENCIDA;
            emit AccionVencida(propuestaId, uint64(block.timestamp));
            revert NoPendiente(propuestaId);
        }
        if (_firmoRol[propuestaId][uint8(rol)]) revert RolYaFirmo(propuestaId, rol);

        _firmoRol[propuestaId][uint8(rol)] = true;
        p.aprobaciones += 1;
        emit FirmaRegistrada(propuestaId, rol, msg.sender, p.aprobaciones);

        if (p.aprobaciones >= FIRMAS_REQUERIDAS) {
            p.estado = Estado.APROBADA;
            emit AccionAprobada(propuestaId, p.activoId, uint64(block.timestamp));
        }
    }

    function _rechazar(bytes32 propuestaId, Rol rol) private {
        Propuesta storage p = _propuestas[propuestaId];
        if (p.estado == Estado.INEXISTENTE) revert PropuestaNoExiste(propuestaId);
        if (p.estado != Estado.PENDIENTE) revert NoPendiente(propuestaId);
        p.estado = Estado.RECHAZADA;
        emit AccionRechazada(propuestaId, rol, msg.sender);
    }

    // ── lecturas ──────────────────────────────────────────────────────────

    function obtenerPropuesta(bytes32 propuestaId) external view returns (Propuesta memory) {
        return _propuestas[propuestaId];
    }

    function estaAprobada(bytes32 propuestaId) external view returns (bool) {
        return _propuestas[propuestaId].estado == Estado.APROBADA;
    }

    function estaEjecutada(bytes32 propuestaId) external view returns (bool) {
        return _propuestas[propuestaId].estado == Estado.EJECUTADA;
    }

    function firmoRol(bytes32 propuestaId, Rol rol) external view returns (bool) {
        return _firmoRol[propuestaId][uint8(rol)];
    }

    // ── operación (admin) ───────────────────────────────────────────────────

    function pausar() external onlyRole(DEFAULT_ADMIN_ROLE) { _pause(); }
    function reanudar() external onlyRole(DEFAULT_ADMIN_ROLE) { _unpause(); }

    function autorizarAgente(address c) external onlyRole(DEFAULT_ADMIN_ROLE) { _grantRole(AGENTE_IA_ROLE, c); }
    function autorizarOperador(address c) external onlyRole(DEFAULT_ADMIN_ROLE) { _grantRole(OPERADOR_ROLE, c); }
    function autorizarProsumidor(address c) external onlyRole(DEFAULT_ADMIN_ROLE) { _grantRole(PROSUMIDOR_ROLE, c); }
}
