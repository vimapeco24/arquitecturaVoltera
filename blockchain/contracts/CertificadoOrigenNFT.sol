// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {ERC721URIStorage} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721URIStorage.sol";
import {ERC721Burnable} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721Burnable.sol";
import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";

/**
 * @title CertificadoOrigenNFT
 * @notice Implementa el patron 4 de la lamina 05 — TOKEN NFT ERC-721
 *         (familia: Tokenizacion / Activos digitales) para los CERTIFICADOS
 *         DE ORIGEN RENOVABLE de Voltera.
 *
 * ┌─ TIPO DE TOKEN ───────────────────────────────────────────────────────────┐
 * │ Estandar: ERC-721 (implementacion de OPENZEPPELIN).                         │
 * │ Tipo: TOKEN NO FUNGIBLE (NFT).                                              │
 * │ Por que NO fungible: cada certificado de origen es UNICO e irrepetible      │
 * │ (identifica una generacion renovable concreta de un medidor en un periodo). │
 * │ No son intercambiables 1:1 como unidades de una moneda, por eso NO se       │
 * │ modela con ERC-20 (fungible) sino con ERC-721 (no fungible).                │
 * │ Alternativa descartada: ERC-1155 (semi-fungible) — util para lotes          │
 * │ fungibles por tipo; aqui prima la unicidad -> ERC-721.                      │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Atributos del certificado (lamina 05/06 — "ID del medidor, propietario,
 * emisor y consumo [neto]"):
 *   - idMedidor         : identificador del medidor IoT que respalda el origen.
 *   - propietario       : dueño del certificado (el owner ERC-721 del token).
 *   - emisor            : cuenta que acuñó el certificado (regulador/operador).
 *   - consumoNetoMensual: energia neta certificada en el periodo (Wh).
 *   - periodo           : periodo de certificacion (p.ej. AAAAMM) — junto con
 *                         idMedidor impide DUPLICADOS (lamina 06: "0 duplicados
 *                         para una misma certificacion").
 *
 * Implementaciones de especificaciones de OpenZeppelin usadas:
 *  - ERC721            : estandar base no fungible (EIP-721).
 *  - ERC721URIStorage  : tokenURI por token -> el documento/metadata del certificado
 *                        vive OFF-CHAIN (IPFS/URL); on-chain solo el URI + el hash
 *                        (patron off-chain storage). No se exponen datos crudos.
 *  - ERC721Burnable    : permite "retirar"/cancelar un certificado (quema del token).
 *  - AccessControl     : patron Embedded Permission (Zero Trust). Solo EMISOR_ROLE
 *                        puede acuñar certificados (p. ej. el regulador/operador).
 *
 * Consecuencias (lamina 05, "que se paga"):
 *  - Costos de gas por emision y transferencia; complejidad de contratos
 *    inteligentes y gestion del ciclo de vida de los NFT.
 */
contract CertificadoOrigenNFT is ERC721, ERC721URIStorage, ERC721Burnable, AccessControl {
    /// @dev Rol autorizado a acuñar (emitir) certificados. Patron Embedded Permission.
    bytes32 public constant EMISOR_ROLE = keccak256("EMISOR_ROLE");

    /// @dev Contador incremental de tokenIds (cada certificado es unico).
    uint256 private _siguienteId;

    /// @dev Datos del certificado anclados on-chain (lamina 05/06).
    struct Certificado {
        bytes32 idMedidor;         // identificador del medidor IoT (keccak256 del serial)
        address emisor;            // cuenta que acuñó el certificado
        uint64  consumoNetoMensual;// energia neta certificada en el periodo (Wh)
        uint32  periodo;           // periodo de certificacion (AAAAMM)
        uint64  emitidoEn;         // epoch seconds de la emision
        bytes32 hashDetalle;       // keccak256 del documento canonico (off-chain)
    }

    /// @notice tokenId => datos del certificado.
    mapping(uint256 => Certificado) public certificados;

    /// @notice Clave de unicidad (idMedidor, periodo) => tokenId ya emitido (+1).
    ///         Impide emitir dos certificados para el mismo medidor y periodo
    ///         (lamina 06: "100% de intentos de emision duplicada rechazados").
    mapping(bytes32 => uint256) private _emitidoPorMedidorPeriodo;

    event CertificadoEmitido(
        uint256 indexed tokenId,
        address indexed propietario,
        bytes32 indexed idMedidor,
        address emisor,
        uint64  consumoNetoMensual,
        uint32  periodo
    );

    error IdMedidorVacio();
    error ConsumoInvalido();
    error PeriodoInvalido();
    error HashVacio();
    error CertificadoDuplicado(bytes32 idMedidor, uint32 periodo, uint256 tokenIdExistente);

    /**
     * @param adminInicial recibe DEFAULT_ADMIN_ROLE y EMISOR_ROLE.
     *        Nombre y simbolo de la coleccion ERC-721 fijos para Voltera.
     */
    constructor(address adminInicial)
        ERC721("Voltera Certificado de Origen", "VCO")
    {
        require(adminInicial != address(0), "admin invalido");
        _grantRole(DEFAULT_ADMIN_ROLE, adminInicial);
        _grantRole(EMISOR_ROLE, adminInicial);
    }

    /**
     * @notice Acuña (emite) un NUEVO certificado de origen como NFT y se lo asigna a
     *         `propietario`. Devuelve el tokenId unico generado.
     *         Rechaza duplicados para el mismo (idMedidor, periodo).
     * @param propietario        cuenta que recibe el certificado (prosumidor/comercializador).
     * @param uri                URI de la metadata off-chain (IPFS/https) — off-chain storage.
     * @param idMedidor          identificador del medidor IoT (bytes32), != 0.
     * @param consumoNetoMensual energia neta certificada en el periodo (Wh), > 0.
     * @param periodo            periodo de certificacion (AAAAMM), > 0.
     * @param hashDetalle        keccak256 del documento canonico del certificado.
     */
    function emitirCertificado(
        address propietario,
        string calldata uri,
        bytes32 idMedidor,
        uint64 consumoNetoMensual,
        uint32 periodo,
        bytes32 hashDetalle
    ) external onlyRole(EMISOR_ROLE) returns (uint256 tokenId) {
        if (idMedidor == bytes32(0)) revert IdMedidorVacio();
        if (consumoNetoMensual == 0) revert ConsumoInvalido();
        if (periodo == 0) revert PeriodoInvalido();
        if (hashDetalle == bytes32(0)) revert HashVacio();

        bytes32 claveUnicidad = keccak256(abi.encodePacked(idMedidor, periodo));
        uint256 existente = _emitidoPorMedidorPeriodo[claveUnicidad];
        if (existente != 0) {
            revert CertificadoDuplicado(idMedidor, periodo, existente);
        }

        tokenId = ++_siguienteId;
        _emitidoPorMedidorPeriodo[claveUnicidad] = tokenId;

        _safeMint(propietario, tokenId);
        _setTokenURI(tokenId, uri);
        certificados[tokenId] = Certificado({
            idMedidor: idMedidor,
            emisor: msg.sender,
            consumoNetoMensual: consumoNetoMensual,
            periodo: periodo,
            emitidoEn: uint64(block.timestamp),
            hashDetalle: hashDetalle
        });

        emit CertificadoEmitido(
            tokenId,
            propietario,
            idMedidor,
            msg.sender,
            consumoNetoMensual,
            periodo
        );
    }

    /// @notice Total de certificados acuñados (contador monotono; no descuenta quemados).
    function totalEmitidos() external view returns (uint256) {
        return _siguienteId;
    }

    /// @notice tokenId emitido para un (idMedidor, periodo), o 0 si no existe.
    function tokenDe(bytes32 idMedidor, uint32 periodo) external view returns (uint256) {
        return _emitidoPorMedidorPeriodo[keccak256(abi.encodePacked(idMedidor, periodo))];
    }

    // ── Overrides exigidos por heredar varias implementaciones de OpenZeppelin ──

    function tokenURI(uint256 tokenId)
        public
        view
        override(ERC721, ERC721URIStorage)
        returns (string memory)
    {
        return super.tokenURI(tokenId);
    }

    function supportsInterface(bytes4 interfaceId)
        public
        view
        override(ERC721, ERC721URIStorage, AccessControl)
        returns (bool)
    {
        return super.supportsInterface(interfaceId);
    }
}
