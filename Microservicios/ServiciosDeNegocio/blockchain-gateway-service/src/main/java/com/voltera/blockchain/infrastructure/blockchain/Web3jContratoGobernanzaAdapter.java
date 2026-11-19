package com.voltera.blockchain.infrastructure.blockchain;

import com.voltera.blockchain.domain.model.LecturaGobernanza;
import com.voltera.blockchain.domain.port.out.ContratoGobernanzaPort;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Lectura SOLO-LECTURA (eth_call / eth_getLogs) del contrato MultiFirmaAccionDER.
 * No firma ni envía transacciones, no usa claves privadas.
 */
public class Web3jContratoGobernanzaAdapter implements ContratoGobernanzaPort {

    private static final String[] ESTADOS =
            {"INEXISTENTE", "PENDIENTE", "APROBADA", "RECHAZADA", "VENCIDA", "EJECUTADA"};
    private static final String[] ROLES = {"AGENTE_IA", "OPERADOR", "PROSUMIDOR"};

    static final Event FIRMA_REGISTRADA = new Event("FirmaRegistrada", Arrays.asList(
            new TypeReference<Bytes32>(true) { },
            new TypeReference<Uint8>() { },
            new TypeReference<Address>(true) { },
            new TypeReference<Uint8>() { }));

    private final Web3j web3j;
    private final String address;

    public Web3jContratoGobernanzaAdapter(Web3j web3j, String address) {
        this.web3j = web3j;
        this.address = address;
    }

    @Override
    public boolean configurado() {
        return web3j != null && address != null && !address.isBlank();
    }

    @Override
    public String direccionContrato() {
        return address;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Optional<PropuestaOnChain> obtenerPropuesta(String id) {
        try {
            Function fn = new Function("obtenerPropuesta",
                    List.of(new Bytes32(Numeric.hexStringToByteArray(id))),
                    Arrays.<TypeReference<?>>asList(
                            new TypeReference<Bytes32>() { }, new TypeReference<Bytes32>() { },
                            new TypeReference<Bytes32>() { }, new TypeReference<Bytes32>() { },
                            new TypeReference<Bytes32>() { }, new TypeReference<Uint8>() { },
                            new TypeReference<Uint64>() { }, new TypeReference<Uint64>() { },
                            new TypeReference<Uint8>() { }));
            String data = FunctionEncoder.encode(fn);
            String raw = web3j.ethCall(Transaction.createEthCallTransaction(null, address, data),
                    DefaultBlockParameterName.LATEST).send().getValue();
            List<Type> out = FunctionReturnDecoder.decode(raw, fn.getOutputParameters());
            if (out.size() < 9) return Optional.empty();
            int estado = ((Uint8) out.get(8)).getValue().intValue();
            if (estado == 0) return Optional.empty();
            int aprob = ((Uint8) out.get(5)).getValue().intValue();
            return Optional.of(new PropuestaOnChain(ESTADOS[Math.min(estado, ESTADOS.length - 1)], aprob));
        } catch (IOException e) {
            throw new IllegalStateException("RPC: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<LecturaGobernanza.FirmaLeida> firmas(String id) {
        try {
            EthFilter filtro = new EthFilter(DefaultBlockParameterName.EARLIEST,
                    DefaultBlockParameterName.LATEST, address);
            filtro.addSingleTopic(EventEncoder.encode(FIRMA_REGISTRADA));
            filtro.addSingleTopic(id);
            List<EthLog.LogResult> logs = web3j.ethGetLogs(filtro).send().getLogs();
            List<LecturaGobernanza.FirmaLeida> res = new ArrayList<>();
            for (EthLog.LogResult<?> r : logs) {
                Log l = (Log) r.get();
                List<Type> data = FunctionReturnDecoder.decode(l.getData(), FIRMA_REGISTRADA.getNonIndexedParameters());
                int rol = ((Uint8) data.get(0)).getValue().intValue();
                String firmante = l.getTopics().size() > 2
                        ? "0x" + l.getTopics().get(2).substring(l.getTopics().get(2).length() - 40) : null;
                Instant cuando = null;
                try {
                    var b = web3j.ethGetBlockByHash(l.getBlockHash(), false).send().getBlock();
                    if (b != null) cuando = Instant.ofEpochSecond(b.getTimestamp().longValue());
                } catch (Exception ignored) { }
                res.add(new LecturaGobernanza.FirmaLeida(
                        ROLES[Math.min(rol, ROLES.length - 1)], firmante, l.getTransactionHash(), cuando));
            }
            return res;
        } catch (IOException e) {
            throw new IllegalStateException("RPC: " + e.getMessage(), e);
        }
    }
}
