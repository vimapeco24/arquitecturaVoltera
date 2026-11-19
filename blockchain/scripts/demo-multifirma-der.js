// =============================================================================
// Demo ON-CHAIN de la gobernanza multi-firma 3/3 (MultiFirmaAccionDER)
// -----------------------------------------------------------------------------
// Despliega el contrato en una red local de Hardhat (sin gas real, sin claves) y
// recorre los escenarios del Módulo 5 con transacciones reales de EVM:
// cada paso imprime tx hash, bloque, gas y eventos emitidos.
//
//   npx hardhat run scripts/demo-multifirma-der.js                 # red en memoria
//   npx hardhat node  &&  npx hardhat run scripts/demo-multifirma-der.js --network localhost
//
// Salida: tabla en consola + JSON en $EVID_ONCHAIN (por defecto
// ../Microservicios/evidencia-experimento/onchain-multifirma-<ts>.json).
// La última línea es "ONCHAIN_RESULT ok=<n> fail=<m>" (la lee experimento-ia-der.sh).
// =============================================================================
const hre = require("hardhat");
const fs = require("fs");
const path = require("path");
const { ethers } = hre;

const ESTADOS = ["INEXISTENTE", "PENDIENTE", "APROBADA", "RECHAZADA", "VENCIDA", "EJECUTADA"];
const ROLES = ["AGENTE_IA", "OPERADOR", "PROSUMIDOR"];

const pasos = [];     // bitácora de transacciones
const criterios = []; // [ok, texto]
const check = (ok, texto) => {
  criterios.push({ ok: !!ok, texto });
  console.log(`   ${ok ? "✓" : "✗"} ${texto}`);
};

async function tx(etiqueta, promesa, contrato) {
  const t = await promesa;
  const r = await t.wait();
  const eventos = r.logs
    .map((l) => { try { return contrato.interface.parseLog(l); } catch { return null; } })
    .filter(Boolean)
    .map((e) => e.name + (e.name === "FirmaRegistrada" ? `(${ROLES[Number(e.args.rol)]}, ${e.args.aprobaciones}/3)` : ""));
  const paso = { etiqueta, txHash: r.hash, bloque: r.blockNumber, gas: r.gasUsed.toString(), eventos };
  pasos.push(paso);
  console.log(`   · ${etiqueta.padEnd(34)} tx=${r.hash.slice(0, 12)}… bloque=${r.blockNumber} gas=${r.gasUsed} eventos=[${eventos.join(", ")}]`);
  return r;
}

async function revierte(promesa) {
  try { const t = await promesa; await t.wait(); return false; } catch { return true; }
}

async function main() {
  const [admin, agente, operador, prosumidor, intruso] = await ethers.getSigners();
  const F = await ethers.getContractFactory("MultiFirmaAccionDER");
  const c = await F.deploy(admin.address);
  await c.waitForDeployment();
  const addr = await c.getAddress();
  console.log("============================================================");
  console.log(" MultiFirmaAccionDER · demo on-chain (red:", hre.network.name + ")");
  console.log(" contrato:", addr);
  console.log("============================================================");
  await c.autorizarAgente(agente.address);
  await c.autorizarOperador(operador.address);
  await c.autorizarProsumidor(prosumidor.address);

  const nuevo = (n) => ({
    id: ethers.id("PROP-DEMO-" + n),
    activo: ethers.id("INV-1"),
    cmd: ethers.id("REINICIAR_INVERSOR(INV-1)"),
    diag: ethers.id("diagnostico-canonico-" + n),
    prompt: ethers.id("diagnosticador@v3"),
    modelo: ethers.id("local"),
  });
  const crear = (p, ttl = 1800) =>
    c.connect(agente).crearPropuesta(p.id, p.activo, p.cmd, p.diag, p.prompt, p.modelo, ttl);
  const estado = async (p) => ESTADOS[Number((await c.obtenerPropuesta(p.id)).estado)];
  const firmas = async (p) => Number((await c.obtenerPropuesta(p.id)).aprobaciones);

  // ── B1 · camino feliz ────────────────────────────────────────────────────
  console.log("\n── ON-CHAIN B1 · camino feliz 1/3 → 2/3 → 3/3 → EJECUTADA");
  const p1 = nuevo(1);
  await tx("crearPropuesta (Firma 1 · agente)", crear(p1), c);
  check((await firmas(p1)) === 1 && (await estado(p1)) === "PENDIENTE", "crear deja 1/3 y PENDIENTE");
  const stored = await c.obtenerPropuesta(p1.id);
  check(stored.hashDiagnostico === p1.diag && stored.versionPrompt === p1.prompt && stored.versionModelo === p1.modelo,
    "hash del diagnóstico + versión de prompt/modelo quedan inmutables on-chain");
  await tx("firmarOperador (Firma 2)", c.connect(operador).firmarOperador(p1.id), c);
  check((await firmas(p1)) === 2 && (await estado(p1)) === "PENDIENTE", "con 2/3 sigue PENDIENTE");
  check(await revierte(c.connect(agente).ejecutar(p1.id)), "con 2/3 ejecutar() revierte (fail-closed, sin modo 2/3)");
  await tx("firmarProsumidor (Firma 3)", c.connect(prosumidor).firmarProsumidor(p1.id), c);
  check((await estado(p1)) === "APROBADA" && (await c.estaAprobada(p1.id)), "con 3/3 pasa a APROBADA");
  await tx("ejecutar (solo desde APROBADA)", c.connect(agente).ejecutar(p1.id), c);
  check((await estado(p1)) === "EJECUTADA" && (await c.estaEjecutada(p1.id)), "queda EJECUTADA y emite AccionEjecutada");
  check(await revierte(c.connect(agente).ejecutar(p1.id)), "no se puede ejecutar dos veces");

  // ── B2 · rechazo del prosumidor ──────────────────────────────────────────
  console.log("\n── ON-CHAIN B2 · soberanía del prosumidor (rechazo)");
  const p2 = nuevo(2);
  await tx("crearPropuesta", crear(p2), c);
  await tx("firmarOperador", c.connect(operador).firmarOperador(p2.id), c);
  await tx("rechazarProsumidor", c.connect(prosumidor).rechazarProsumidor(p2.id), c);
  check((await estado(p2)) === "RECHAZADA", "el rechazo del prosumidor cierra la propuesta (RECHAZADA)");
  check(await revierte(c.connect(operador).firmarOperador(p2.id)), "tras el rechazo no se admiten más firmas");
  check(await revierte(c.connect(agente).ejecutar(p2.id)), "una propuesta RECHAZADA no se ejecuta");

  // ── B3 · no repudio ──────────────────────────────────────────────────────
  console.log("\n── ON-CHAIN B3 · no repudio y control de roles");
  const p3 = nuevo(3);
  await tx("crearPropuesta", crear(p3), c);
  await tx("firmarOperador", c.connect(operador).firmarOperador(p3.id), c);
  check(await revierte(c.connect(operador).firmarOperador(p3.id)), "el mismo rol no puede firmar dos veces (RolYaFirmo)");
  check(await revierte(c.connect(intruso).firmarProsumidor(p3.id)), "una cuenta sin rol PROSUMIDOR no puede firmar (AccessControl)");
  check(await revierte(c.connect(operador).firmarProsumidor(p3.id)), "el operador no puede firmar en nombre del prosumidor");
  check(await revierte(c.connect(operador).crearPropuesta(nuevo(30).id, p3.activo, p3.cmd, p3.diag, p3.prompt, p3.modelo, 60)),
    "solo el AGENTE_IA puede crear propuestas");
  check(await revierte(crear(p3)), "no se puede recrear una propuesta con el mismo id");
  check(await firmas(p3) === 2, "las firmas inválidas no alteraron el contador (2/3)");

  // ── B5 · caducidad por TTL ───────────────────────────────────────────────
  console.log("\n── ON-CHAIN B5 · caducidad por TTL (fail-closed)");
  const p5 = nuevo(5);
  await tx("crearPropuesta (TTL 60 s)", crear(p5, 60), c);
  check(await revierte(c.caducar(p5.id)), "antes del TTL caducar() revierte (aún vigente)");
  await hre.network.provider.send("evm_increaseTime", [120]);
  await hre.network.provider.send("evm_mine");
  await tx("caducar (TTL vencido)", c.caducar(p5.id), c);
  check((await estado(p5)) === "VENCIDA", "vencido el TTL sin 3/3 queda VENCIDA");
  check(await revierte(c.connect(operador).firmarOperador(p5.id)), "no se firma una propuesta VENCIDA");
  check(await revierte(c.connect(agente).ejecutar(p5.id)), "una propuesta VENCIDA no se ejecuta");

  // ── B7 · parada de emergencia ────────────────────────────────────────────
  console.log("\n── ON-CHAIN B7 · parada de emergencia (Pausable)");
  await tx("pausar (admin)", c.connect(admin).pausar(), c);
  check(await revierte(crear(nuevo(7))), "con el contrato pausado no se crean propuestas");
  check(await revierte(c.connect(intruso).pausar()), "solo el admin puede pausar");
  await tx("reanudar (admin)", c.connect(admin).reanudar(), c);
  await tx("crearPropuesta tras reanudar", crear(nuevo(8)), c);

  // ── B8 · consistencia de contadores y eventos ────────────────────────────
  console.log("\n── ON-CHAIN B8 · consistencia y auditoría por eventos");
  check((await c.totalPropuestas()) === 5n, "totalPropuestas = 5");
  check((await c.totalEjecutadas()) === 1n, "totalEjecutadas = 1 (solo la de 3/3)");
  const logs = await c.queryFilter(c.filters.FirmaRegistrada(p1.id));
  const orden = logs.map((l) => ROLES[Number(l.args.rol)]).join("→");
  check(orden === "AGENTE_IA→OPERADOR→PROSUMIDOR", `la auditoría por eventos reconstruye el orden de firmas (${orden})`);
  const firmantes = new Set(logs.map((l) => l.args.firmante));
  check(firmantes.size === 3, "las 3 firmas provienen de 3 cuentas distintas");

  const ok = criterios.filter((x) => x.ok).length;
  const fail = criterios.length - ok;
  const ts = new Date().toISOString().replace(/[-:T]/g, "").slice(0, 14);
  const out = process.env.EVID_ONCHAIN ||
    path.join(__dirname, "..", "..", "Microservicios", "evidencia-experimento", `onchain-multifirma-${ts}.json`);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify({
    red: hre.network.name, chainId: Number((await ethers.provider.getNetwork()).chainId),
    contrato: addr, nota: "Red local de Hardhat: transacciones EVM reales, sin gas ni claves reales. Mismo bytecode que se desplegará en Amoy.",
    transacciones: pasos, criterios, ok, fail,
  }, null, 2));
  console.log(`\n   Evidencia on-chain: ${out}`);
  console.log(`ONCHAIN_RESULT ok=${ok} fail=${fail}`);
  if (fail > 0) process.exitCode = 1;
}

main().catch((e) => { console.error(e); process.exit(1); });
