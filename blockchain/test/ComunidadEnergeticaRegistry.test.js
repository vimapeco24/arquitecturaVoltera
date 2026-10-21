const { expect } = require("chai");
const { ethers } = require("hardhat");
const { anyValue } = require("@nomicfoundation/hardhat-chai-matchers/withArgs");

describe("ComunidadEnergeticaRegistry", function () {
  let contrato, admin, operador2, intruso;

  const comunidadId = ethers.id("COMUNIDAD-SOLAR-01");
  const nombreHash = ethers.id("Comunidad Solar Chapinero");
  const vendedor = ethers.id("PRO-SOLAR-01");
  const comprador = ethers.id("PRO-CASA-07");

  beforeEach(async function () {
    [admin, operador2, intruso] = await ethers.getSigners();
    const Factory = await ethers.getContractFactory("ComunidadEnergeticaRegistry");
    contrato = await Factory.deploy(admin.address);
    await contrato.waitForDeployment();
  });

  it("registra una comunidad y emite el evento", async function () {
    await expect(contrato.registrarComunidad(comunidadId, nombreHash))
      .to.emit(contrato, "ComunidadRegistrada")
      .withArgs(comunidadId, nombreHash, anyValue, admin.address);

    expect(await contrato.existeComunidad(comunidadId)).to.equal(true);
    expect(await contrato.totalComunidades()).to.equal(1n);
  });

  it("no permite registrar dos veces la misma comunidad", async function () {
    await contrato.registrarComunidad(comunidadId, nombreHash);
    await expect(contrato.registrarComunidad(comunidadId, nombreHash))
      .to.be.revertedWithCustomError(contrato, "ComunidadYaExiste")
      .withArgs(comunidadId);
  });

  it("agrega y remueve miembros actualizando el contador", async function () {
    await contrato.registrarComunidad(comunidadId, nombreHash);

    await expect(contrato.agregarMiembro(comunidadId, vendedor))
      .to.emit(contrato, "MiembroAgregado")
      .withArgs(comunidadId, vendedor, anyValue, admin.address);

    expect(await contrato.esMiembro(comunidadId, vendedor)).to.equal(true);
    let c = await contrato.obtenerComunidad(comunidadId);
    expect(c.totalMiembros).to.equal(1);

    await contrato.removerMiembro(comunidadId, vendedor);
    expect(await contrato.esMiembro(comunidadId, vendedor)).to.equal(false);
    c = await contrato.obtenerComunidad(comunidadId);
    expect(c.totalMiembros).to.equal(0);
  });

  it("no agrega miembro a comunidad inexistente", async function () {
    await expect(contrato.agregarMiembro(ethers.id("NO-EXISTE"), vendedor))
      .to.be.revertedWithCustomError(contrato, "ComunidadNoExiste");
  });

  it("no agrega dos veces al mismo miembro", async function () {
    await contrato.registrarComunidad(comunidadId, nombreHash);
    await contrato.agregarMiembro(comunidadId, vendedor);
    await expect(contrato.agregarMiembro(comunidadId, vendedor))
      .to.be.revertedWithCustomError(contrato, "YaEsMiembro");
  });

  it("puedenCasar() solo si ambos son miembros activos de la misma comunidad", async function () {
    await contrato.registrarComunidad(comunidadId, nombreHash);
    await contrato.agregarMiembro(comunidadId, vendedor);
    // comprador aun no es miembro -> false
    expect(await contrato.puedenCasar(comunidadId, vendedor, comprador)).to.equal(false);

    await contrato.agregarMiembro(comunidadId, comprador);
    expect(await contrato.puedenCasar(comunidadId, vendedor, comprador)).to.equal(true);

    // mismo prosumidor -> false
    expect(await contrato.puedenCasar(comunidadId, vendedor, vendedor)).to.equal(false);

    // tras remover al comprador -> false
    await contrato.removerMiembro(comunidadId, comprador);
    expect(await contrato.puedenCasar(comunidadId, vendedor, comprador)).to.equal(false);
  });

  it("una cuenta sin rol de operador no puede gestionar el padron", async function () {
    await expect(
      contrato.connect(intruso).registrarComunidad(comunidadId, nombreHash)
    ).to.be.revertedWithCustomError(contrato, "AccessControlUnauthorizedAccount");
  });

  it("el admin puede delegar y revocar el rol de operador", async function () {
    await contrato.autorizarOperador(operador2.address);
    await expect(
      contrato.connect(operador2).registrarComunidad(comunidadId, nombreHash)
    ).to.not.be.reverted;

    await contrato.revocarOperador(operador2.address);
    await expect(
      contrato.connect(operador2).registrarComunidad(ethers.id("OTRA"), nombreHash)
    ).to.be.revertedWithCustomError(contrato, "AccessControlUnauthorizedAccount");
  });

  it("pausado bloquea la gestion del padron hasta reanudar", async function () {
    await contrato.pausar();
    await expect(
      contrato.registrarComunidad(comunidadId, nombreHash)
    ).to.be.revertedWithCustomError(contrato, "EnforcedPause");

    await contrato.reanudar();
    await expect(contrato.registrarComunidad(comunidadId, nombreHash)).to.not.be.reverted;
  });
});
