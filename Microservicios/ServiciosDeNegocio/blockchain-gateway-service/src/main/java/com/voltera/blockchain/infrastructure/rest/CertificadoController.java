package com.voltera.blockchain.infrastructure.rest;

import com.voltera.blockchain.domain.port.in.GestionarCertificadoUseCase;
import com.voltera.blockchain.domain.port.in.GestionarCertificadoUseCase.ComandoEmitir;
import com.voltera.blockchain.domain.port.out.CertificadoNftPort;
import com.voltera.blockchain.infrastructure.config.BlockchainProperties;
import com.voltera.blockchain.infrastructure.rest.dto.CertificadoResponse;
import com.voltera.blockchain.infrastructure.rest.dto.EmitirCertificadoRequest;
import com.voltera.blockchain.infrastructure.rest.dto.TransferirCertificadoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST de los Certificados de Origen NFT (ERC-721). Emision, transferencia
 * billetera->billetera, quema y consulta. Ruta base alineada con WSO2:
 *   /blockchain/v1/api/v1/certificados
 */
@RestController
@RequestMapping("/api/v1/certificados")
public class CertificadoController {

    private final GestionarCertificadoUseCase certificados;
    private final CertificadoNftPort cadena;
    private final BlockchainProperties props;

    public CertificadoController(GestionarCertificadoUseCase certificados,
                                 CertificadoNftPort cadena, BlockchainProperties props) {
        this.certificados = certificados;
        this.cadena = cadena;
        this.props = props;
    }

    @PostMapping
    public ResponseEntity<CertificadoResponse> emitir(@Valid @RequestBody EmitirCertificadoRequest req) {
        var cert = certificados.emitir(new ComandoEmitir(
                req.emisor(), req.propietario(), req.idMedidor(),
                req.consumoNetoMensualWh(), req.periodo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CertificadoResponse.de(cert));
    }

    @PostMapping("/{tokenId}/transferir")
    public CertificadoResponse transferir(@PathVariable long tokenId,
                                          @Valid @RequestBody TransferirCertificadoRequest req) {
        return CertificadoResponse.de(certificados.transferir(tokenId, req.hacia()));
    }

    @PostMapping("/{tokenId}/quemar")
    public CertificadoResponse quemar(@PathVariable long tokenId) {
        return CertificadoResponse.de(certificados.quemar(tokenId));
    }

    @GetMapping("/{tokenId}")
    public CertificadoResponse porToken(@PathVariable long tokenId) {
        return CertificadoResponse.de(certificados.porToken(tokenId));
    }

    @GetMapping
    public List<CertificadoResponse> todos(@RequestParam(required = false) String propietario) {
        var lista = (propietario == null || propietario.isBlank())
                ? certificados.todos()
                : certificados.porPropietario(propietario);
        return lista.stream().map(CertificadoResponse::de).toList();
    }

    /** Info del contrato NFT para la UI (badge de estado). */
    @GetMapping("/red")
    public Map<String, Object> red() {
        return Map.of(
                "red", props.getNetwork(),
                "chainId", props.getChainId(),
                "contrato", props.getCertificadoNftAddress() == null ? "" : props.getCertificadoNftAddress(),
                "tipo", "ERC-721 no fungible",
                "disponible", cadena.disponible());
    }
}
