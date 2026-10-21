// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";

/**
 * @title LiquidacionP2PAnchor
 * @notice Ancla en Polygon la PRUEBA DE INTEGRIDAD (hash) de cada transaccion de
 *         liquidacion del mercado energetico P2P de Voltera.
 *
 * Diseño alineado con el arbol de decision (ver BLOCKCHAIN_VOLTERA.md seccion 5/6):
 *  - Gate 8 (off-chain storage): on-chain SOLO va el hash del registro de casacion
 *    y metadatos minimos. Los datos crudos/personales viven off-chain en los
 *    microservicios Spring Boot. Asi la red puede ser publica (Amoy) sin exponer
 *    informacion sensible.
 *  - Gate 4 (inmutabilidad / no repudio): una transaccion solo se puede anclar una
 *    vez; cualquier reintento con hash distinto se rechaza. El registro no se borra.
 *  - Patron Embedded Permission (Zero Trust on-chain): solo cuentas con el rol
 *    ORACULO_ROLE pueden anclar; el servicio de borde "blockchain-gateway" es ese
 *    oraculo. El rol se administra con AccessControl de OpenZeppelin.
 *  - EDA: cada anclaje emite el evento LiquidacionAnclada, que el indexador del
 *    gateway escucha y republica en Kafka/Redpanda (gate 6: confirmacion por evento,
 *    cadena fuera del camino critico).
 */
contract LiquidacionP2PAnchor is AccessControl, Pausable {
    /// @dev Rol autorizado a anclar liquidaciones (lo porta el blockchain-gateway).
    bytes32 public constant ORACULO_ROLE = keccak256("ORACULO_ROLE");

    /// @dev Registro inmutable de una liquidacion P2P anclada.
    struct Anclaje {
        bytes32 hashCasacion;   // keccak256 del registro canonico de la transaccion
        uint64  liquidadaEn;    // epoch seconds de la liquidacion (reportado off-chain)
        uint64  ancladaEn;      // epoch seconds del bloque que ancló
        uint32  energiaWh;      // energia casada en Wh (metadato minimo, no sensible)
        address oraculo;        // quien ancló (trazabilidad / no repudio)
        bool    existe;         // flag de presencia
    }

    /// @notice transaccionId (bytes32) => anclaje. Fuente de verdad on-chain.
    mapping(bytes32 => Anclaje) private _anclajes;

    /// @notice total de liquidaciones ancladas (contador monotono).
    uint256 public totalAncladas;

    /**
     * @notice Se emite al anclar una nueva liquidacion P2P.
     * @dev El indexador del gateway filtra por este evento. transaccionId va
     *      indexed para busqueda eficiente.
     */
    event LiquidacionAnclada(
        bytes32 indexed transaccionId,
        bytes32 hashCasacion,
        uint32  energiaWh,
        uint64  liquidadaEn,
        uint64  ancladaEn,
        address indexed oraculo
    );

    error TransaccionYaAnclada(bytes32 transaccionId);
    error HashVacio();
    error TransaccionIdVacio();

    /**
     * @param adminInicial cuenta que recibe DEFAULT_ADMIN_ROLE y ORACULO_ROLE.
     *        En el deploy se usa la cuenta del gateway; puede delegar roles despues.
     */
    constructor(address adminInicial) {
        require(adminInicial != address(0), "admin invalido");
        _grantRole(DEFAULT_ADMIN_ROLE, adminInicial);
        _grantRole(ORACULO_ROLE, adminInicial);
    }

    /**
     * @notice Ancla una liquidacion P2P. Idempotencia estricta: si ya existe con el
     *         mismo hash, no revierte (permite reintentos del gateway sin romper
     *         el OUTBOX); si existe con hash distinto, revierte (no repudio).
     * @param transaccionId identificador de la transaccion P2P (del dominio Voltera).
     * @param hashCasacion  keccak256 del registro canonico (partes, kWh, precio, intervalo).
     * @param energiaWh     energia casada en Wh (metadato publico, no sensible).
     * @param liquidadaEn   epoch seconds de la liquidacion off-chain.
     */
    function anclar(
        bytes32 transaccionId,
        bytes32 hashCasacion,
        uint32 energiaWh,
        uint64 liquidadaEn
    ) external onlyRole(ORACULO_ROLE) whenNotPaused {
        if (transaccionId == bytes32(0)) revert TransaccionIdVacio();
        if (hashCasacion == bytes32(0)) revert HashVacio();

        Anclaje storage a = _anclajes[transaccionId];
        if (a.existe) {
            // Reintento idempotente: mismo hash -> no-op; hash distinto -> rechazo.
            if (a.hashCasacion == hashCasacion) {
                return;
            }
            revert TransaccionYaAnclada(transaccionId);
        }

        uint64 ahora = uint64(block.timestamp);
        _anclajes[transaccionId] = Anclaje({
            hashCasacion: hashCasacion,
            liquidadaEn: liquidadaEn,
            ancladaEn: ahora,
            energiaWh: energiaWh,
            oraculo: msg.sender,
            existe: true
        });
        totalAncladas += 1;

        emit LiquidacionAnclada(
            transaccionId, hashCasacion, energiaWh, liquidadaEn, ahora, msg.sender
        );
    }

    /**
     * @notice Verifica off-chain: ¿coincide el hash recomputado con el anclado?
     *         Pensado para que el regulador/auditor valide la integridad sin
     *         confiar en el operador.
     */
    function verificar(bytes32 transaccionId, bytes32 hashEsperado)
        external
        view
        returns (bool)
    {
        Anclaje storage a = _anclajes[transaccionId];
        return a.existe && a.hashCasacion == hashEsperado;
    }

    /// @notice Devuelve el anclaje completo de una transaccion.
    function obtener(bytes32 transaccionId)
        external
        view
        returns (Anclaje memory)
    {
        return _anclajes[transaccionId];
    }

    /// @notice ¿Esta anclada esta transaccion?
    function estaAnclada(bytes32 transaccionId) external view returns (bool) {
        return _anclajes[transaccionId].existe;
    }

    // ── Operacion (solo admin) ──────────────────────────────────────────────

    function pausar() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _pause();
    }

    function reanudar() external onlyRole(DEFAULT_ADMIN_ROLE) {
        _unpause();
    }

    /// @notice Autoriza a otra cuenta como oraculo (p. ej. un gateway redundante).
    function autorizarOraculo(address cuenta) external onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(ORACULO_ROLE, cuenta);
    }

    function revocarOraculo(address cuenta) external onlyRole(DEFAULT_ADMIN_ROLE) {
        _revokeRole(ORACULO_ROLE, cuenta);
    }
}
