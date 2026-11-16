const { expect } = require("chai");
const { ethers } = require("hardhat");
const { anyValue } = require("@nomicfoundation/hardhat-chai-matchers/withArgs");

describe("MultiFirmaAccionDER (gobernanza 3/3 de acciones DER)", function () {
  let contrato, admin, agente, operador, prosumidor, intruso;

  const propuestaId = ethers.id("PROP-DER-2026-0001");
  const activoId = ethers.id("INV-1");
  const comandoHash = ethers.id("REINICIAR_INVERSOR(INV-1)");
  const hashDiagnostico = ethers.id("diagnostico-canonico");
  const versionPrompt = ethers.id("diagnosticador@v3");
  const versionModelo = ethers.id("local");
  const TTL = 1800; // 30 min

  beforeEach(async function () {
    [admin, agente, operador, prosumidor, intruso] = await ethers.getSigners();
    const Factory = await ethers.getContractFactory("MultiFirmaAccionDER");
    contrato = await Factory.deploy(admin.address);
    await contrato.waitForDeployment();
    await contrato.autorizarAgente(agente.address);
    await contrato.autorizarOperador(operador.address);
    await contrato.autorizarProsumidor(prosumidor.address);
  });

  async function crear() {
    return contrato
      .connect(agente)
      .crearPropuesta(propuestaId, activoId, comandoHash, hashDiagnostico, versionPrompt, versionModelo, TTL);
  }

  it("crear deja la propuesta PENDIENTE con la Firma 1 del agente (1/3)", async function () {
    await expect(crear())
      .to.emit(contrato, "PropuestaCreada")
      .withArgs(propuestaId, activoId, comandoHash, hashDiagnostico, versionPrompt, versionModelo, agente.address, anyValue, anyValue)
      .and.to.emit(contrato, "FirmaRegistrada")
      .withArgs(propuestaId, 0, agente.address, 1);

    const p = await contrato.obtenerPropuesta(propuestaId);
    expect(p.estado).to.equal(1); // PENDIENTE
    expect(p.aprobaciones).to.equal(1);
    expect(await contrato.firmoRol(propuestaId, 0)).to.equal(true); // AGENTE_IA
  });

  it("con 2/3 NO se aprueba ni se ejecuta (no hay modo 2/3)", async function () {
    await crear();
    await contrato.connect(operador).firmarOperador(propuestaId);
    const p = await contrato.obtenerPropuesta(propuestaId);
    expect(p.estado).to.equal(1); // sigue PENDIENTE
    expect(p.aprobaciones).to.equal(2);
    expect(await contrato.estaAprobada(propuestaId)).to.equal(false);
    await expect(contrato.connect(agente).ejecutar(propuestaId)).to.be.reverted; // fail-closed
  });

  it("con las 3 firmas pasa a APROBADA y solo entonces se ejecuta", async function () {
    await crear();
    await contrato.connect(operador).firmarOperador(propuestaId);
    await expect(contrato.connect(prosumidor).firmarProsumidor(propuestaId))
      .to.emit(contrato, "AccionAprobada")
      .withArgs(propuestaId, activoId, anyValue);
    expect(await contrato.estaAprobada(propuestaId)).to.equal(true);

    await expect(contrato.connect(agente).ejecutar(propuestaId))
      .to.emit(contrato, "AccionEjecutada")
      .withArgs(propuestaId, activoId, anyValue);
    expect(await contrato.estaEjecutada(propuestaId)).to.equal(true);
    expect(await contrato.totalEjecutadas()).to.equal(1n);
  });

  it("el rechazo del prosumidor cierra la propuesta (RECHAZADA)", async function () {
    await crear();
    await expect(contrato.connect(prosumidor).rechazarProsumidor(propuestaId))
      .to.emit(contrato, "AccionRechazada")
      .withArgs(propuestaId, 2, prosumidor.address);
    const p = await contrato.obtenerPropuesta(propuestaId);
    expect(p.estado).to.equal(3); // RECHAZADA
    await expect(contrato.connect(agente).ejecutar(propuestaId)).to.be.reverted;
  });

  it("un rol no puede firmar dos veces", async function () {
    await crear();
    await contrato.connect(operador).firmarOperador(propuestaId);
    await expect(contrato.connect(operador).firmarOperador(propuestaId)).to.be.reverted;
  });

  it("una cuenta sin rol no puede firmar", async function () {
    await crear();
    await expect(contrato.connect(intruso).firmarOperador(propuestaId)).to.be.reverted;
  });

  it("vencido el TTL la propuesta caduca y no se ejecuta", async function () {
    await crear();
    await ethers.provider.send("evm_increaseTime", [TTL + 1]);
    await ethers.provider.send("evm_mine", []);
    await expect(contrato.caducar(propuestaId))
      .to.emit(contrato, "AccionVencida")
      .withArgs(propuestaId, anyValue);
    const p = await contrato.obtenerPropuesta(propuestaId);
    expect(p.estado).to.equal(4); // VENCIDA
  });

  it("no se puede crear dos veces la misma propuesta", async function () {
    await crear();
    await expect(crear()).to.be.reverted;
  });
});
