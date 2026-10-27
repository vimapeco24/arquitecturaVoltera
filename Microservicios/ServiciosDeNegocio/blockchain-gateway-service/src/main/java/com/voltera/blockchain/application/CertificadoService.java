package com.voltera.blockchain.application;

import com.voltera.blockchain.domain.model.CertificadoNft;
import com.voltera.blockchain.domain.port.in.GestionarCertificadoUseCase;
import com.voltera.blockchain.domain.port.out.CertificadoNftPort;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orquesta el ciclo de vida de los certificados NFT (ERC-721). Aplica las MISMAS
 * reglas del contrato {@code CertificadoOrigenNFT.sol}:
 * <ul>
 *   <li>Unicidad: 0 duplicados para un mismo (idMedidor, periodo).</li>
 *   <li>Solo-emisor para acuñar (se asume que el emisor es la billetera autorizada).</li>
 *   <li>Transferencia valida solo entre billeteras distintas y sobre un token vivo.</li>
 * </ul>
 * Delega la ejecucion on-chain en {@link CertificadoNftPort}; si no hay contrato
 * desplegado, el adaptador trabaja en modo demo y el estado queda en memoria.
 */
public class CertificadoService implements GestionarCertificadoUseCase {

    private final CertificadoNftPort cadena;

    private final Map<Long, CertificadoNft> porToken = new ConcurrentHashMap<>();
    private final Map<String, Long> unicidad = new ConcurrentHashMap<>(); // (medidor|periodo) -> tokenId
    private final AtomicLong secuencia = new AtomicLong(0);

    public CertificadoService(CertificadoNftPort cadena) {
        this.cadena = cadena;
    }

    @Override
    public CertificadoNft emitir(ComandoEmitir cmd) {
        validarEmision(cmd);
        String clave = CertificadoNft.claveUnicidad(cmd.idMedidor(), cmd.periodo());
        if (unicidad.containsKey(clave)) {
            throw new IllegalArgumentException(
                    "Emision duplicada rechazada: ya existe el certificado #" + unicidad.get(clave)
                            + " para medidor=" + cmd.idMedidor() + " periodo=" + cmd.periodo());
        }

        long tokenId = secuencia.incrementAndGet();
        String hashDetalle = keccak256Hex(
                cmd.idMedidor() + "|" + cmd.periodo() + "|" + cmd.consumoNetoMensualWh() + "|" + cmd.emisor());
        String tokenUri = "ipfs://voltera/certificado/" + tokenId;

        CertificadoNft cert = new CertificadoNft(tokenId, cmd.idMedidor(), cmd.propietario(),
                cmd.emisor(), cmd.consumoNetoMensualWh(), cmd.periodo(), tokenUri, hashDetalle, Instant.now());

        CertificadoNftPort.Resultado r = cadena.emitir(cert);
        if (!r.ok()) {
            throw new IllegalStateException("No se pudo emitir el certificado on-chain: " + r.error());
        }
        cert.registrarEmision(r.txHash());

        porToken.put(tokenId, cert);
        unicidad.put(clave, tokenId);
        return cert;
    }

    @Override
    public CertificadoNft transferir(long tokenId, String hacia) {
        CertificadoNft cert = requerir(tokenId);
        if (cert.quemado()) {
            throw new IllegalArgumentException("El certificado #" + tokenId + " fue retirado (quemado).");
        }
        if (hacia == null || hacia.isBlank() || hacia.equals(CertificadoNft.ZERO)) {
            throw new IllegalArgumentException("Billetera destino invalida.");
        }
        if (hacia.equalsIgnoreCase(cert.propietario())) {
            throw new IllegalArgumentException("El destino ya es el propietario actual.");
        }

        CertificadoNftPort.Resultado r = cadena.transferir(tokenId, cert.propietario(), hacia);
        if (!r.ok()) {
            throw new IllegalStateException("No se pudo transferir on-chain: " + r.error());
        }
        cert.transferir(hacia, r.txHash());
        return cert;
    }

    @Override
    public CertificadoNft quemar(long tokenId) {
        CertificadoNft cert = requerir(tokenId);
        if (cert.quemado()) return cert;
        CertificadoNftPort.Resultado r = cadena.quemar(tokenId, cert.propietario());
        if (!r.ok()) {
            throw new IllegalStateException("No se pudo quemar on-chain: " + r.error());
        }
        cert.quemar(r.txHash());
        return cert;
    }

    @Override
    public CertificadoNft porToken(long tokenId) {
        return requerir(tokenId);
    }

    @Override
    public List<CertificadoNft> todos() {
        List<CertificadoNft> lista = new ArrayList<>(porToken.values());
        lista.sort(Comparator.comparingLong(CertificadoNft::tokenId).reversed());
        return lista;
    }

    @Override
    public List<CertificadoNft> porPropietario(String propietario) {
        return todos().stream()
                .filter(c -> c.propietario().equalsIgnoreCase(propietario))
                .toList();
    }

    private CertificadoNft requerir(long tokenId) {
        CertificadoNft cert = porToken.get(tokenId);
        if (cert == null) {
            throw new IllegalArgumentException("No existe el certificado NFT #" + tokenId + ".");
        }
        return cert;
    }

    private static void validarEmision(ComandoEmitir cmd) {
        if (cmd == null) throw new IllegalArgumentException("Comando nulo.");
        if (esVacio(cmd.emisor())) throw new IllegalArgumentException("Falta la billetera emisora.");
        if (esVacio(cmd.propietario())) throw new IllegalArgumentException("Falta la billetera propietaria.");
        if (esVacio(cmd.idMedidor())) throw new IllegalArgumentException("Falta el ID del medidor.");
        if (cmd.consumoNetoMensualWh() <= 0) throw new IllegalArgumentException("El consumo neto debe ser > 0.");
        if (cmd.periodo() <= 0) throw new IllegalArgumentException("El periodo (AAAAMM) debe ser > 0.");
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }

    /** keccak256 (igual que Solidity/EVM) en hex 0x del documento canonico off-chain. */
    private static String keccak256Hex(String canonico) {
        byte[] hash = Hash.sha3(canonico.getBytes(StandardCharsets.UTF_8));
        return Numeric.toHexString(hash);
    }
}
