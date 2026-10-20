package com.voltera.blockchain.infrastructure.rest;

import com.voltera.blockchain.domain.model.Anclaje;
import com.voltera.blockchain.domain.model.LiquidacionP2P;
import com.voltera.blockchain.domain.port.in.AnclarLiquidacionUseCase;
import com.voltera.blockchain.domain.port.in.ConsultarAnclajeUseCase;
import com.voltera.blockchain.domain.port.out.CadenaBlockchainPort;
import com.voltera.blockchain.infrastructure.config.BlockchainProperties;
import com.voltera.blockchain.infrastructure.rest.dto.AnclajeResponse;
import com.voltera.blockchain.infrastructure.rest.dto.AnclarRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST del gateway. Ruta base alineada con el gateway WSO2:
 *   /blockchain/v1/api/v1/anclajes
 */
@RestController
@RequestMapping("/api/v1/anclajes")
public class AnclajeController {

    private final AnclarLiquidacionUseCase anclar;
    private final ConsultarAnclajeUseCase consultar;
    private final CadenaBlockchainPort cadena;
    private final BlockchainProperties props;
    private final String explorerBase;

    public AnclajeController(AnclarLiquidacionUseCase anclar, ConsultarAnclajeUseCase consultar,
                             CadenaBlockchainPort cadena, BlockchainProperties props,
                             @Value("${voltera.blockchain.explorer-base:https://amoy.polygonscan.com}") String explorerBase) {
        this.anclar = anclar;
        this.consultar = consultar;
        this.cadena = cadena;
        this.props = props;
        this.explorerBase = explorerBase;
    }

    @PostMapping
    public ResponseEntity<AnclajeResponse> anclar(@Valid @RequestBody AnclarRequest req) {
        LiquidacionP2P liq = new LiquidacionP2P(
                req.transaccionId(), req.vendedorId(), req.compradorId(),
                req.energiaWh(), req.precioPorKwh(), req.valorTotal(), req.liquidadaEn());
        Anclaje a = anclar.anclar(liq);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AnclajeResponse.de(a, explorerBase));
    }

    /**
     * Verifica la integridad: recomputa el hash del cuerpo y lo compara con el
     * anclado on-chain. Pensado para auditoria (regulador/contraparte).
     */
    @PostMapping("/verificar")
    public ConsultarAnclajeUseCase.ResultadoVerificacion verificar(@Valid @RequestBody AnclarRequest req) {
        LiquidacionP2P liq = new LiquidacionP2P(
                req.transaccionId(), req.vendedorId(), req.compradorId(),
                req.energiaWh(), req.precioPorKwh(), req.valorTotal(), req.liquidadaEn());
        return consultar.verificar(liq);
    }

    @GetMapping("/{transaccionId}")
    public AnclajeResponse porId(@PathVariable String transaccionId) {
        return AnclajeResponse.de(consultar.porTransaccion(transaccionId), explorerBase);
    }

    @GetMapping
    public List<AnclajeResponse> todos() {
        return consultar.todos().stream()
                .map(a -> AnclajeResponse.de(a, explorerBase))
                .toList();
    }

    /** Info de la red/contrato para la UI (tarjeta de estado del marketplace). */
    @GetMapping("/red")
    public Map<String, Object> red() {
        return Map.of(
                "red", props.getNetwork(),
                "chainId", props.getChainId(),
                "contrato", props.getContractAddress() == null ? "" : props.getContractAddress(),
                "explorer", explorerBase,
                "configurada", props.estaConfigurada(),
                "disponible", cadena.disponible());
    }
}
