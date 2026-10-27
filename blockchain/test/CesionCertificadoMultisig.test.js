const { expect } = require("chai");
const { ethers } = require("hardhat");
const { anyValue } = require("@nomicfoundation/hardhat-chai-matchers/withArgs");

describe("CesionCertificadoMultisig (patron Multiple Authorization)", function () {
  let contrato, admin, firmante2, firmante3, intruso;

  const cesionId = ethers.id("CESION-REC-2026-0001");
  const certificadoId = ethers.id("REC-SOLAR-0001");
  const cedente = ethers.id("PRO-SOLAR-01");
  const cesionario = ethers.id("COMERCIALIZADORA-07");
  const hashDetalle = ethers.id("doc-canonico-de-la-cesion");

  beforeEach(async function () {
    [admin, firmante2, firmante3, intruso] = await ethers.getSigners();
    const Factory = await ethers.getContractFactory("CesionCertificadoMultisig");
    contrato = await Factory.deploy(admin.address);
    await contrato.waitForDeployment();
    // El admin autoriza un 2o firmante (sin el, nada puede registrarse).
    await contrato.autorizarFirmante(firmante2.address);
  });

  it("proponer deja la cesion PENDIENTE con 1 firma y emite eventos", async function () {
    await expect(
      contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle)
    )
      .to.emit(contrato, "CesionPropuesta")
      .withArgs(cesionId, certificadoId, cedente, cesionario, hashDetalle, admin.address, anyValue)
      .and.to.emit(contrato, "CesionFirmada")
      .withArgs(cesionId, admin.address, 1);

    const c = await contrato.obtenerCesion(cesionId);
    expect(c.estado).to.equal(1); // PENDIENTE
    expect(c.aprobaciones).to.equal(1);
    expect(await contrato.estaRegistrada(cesionId)).to.equal(false);
    expect(await contrato.totalRegistradas()).to.equal(0n);
  });

  it("UNA sola firma NO basta: la cesion no queda registrada", async function () {
    await contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle);
    expect(await contrato.estaRegistrada(cesionId)).to.equal(false);
  });

  it("la 2a firma de OTRO firmante distinto registra la cesion", async function () {
    await contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle);

    await expect(contrato.connect(firmante2).confirmarCesion(cesionId))
      .to.emit(contrato, "CesionRegistrada")
      .withArgs(cesionId, certificadoId, 2, anyValue);

    expect(await contrato.estaRegistrada(cesionId)).to.equal(true);
    expect(await contrato.totalRegistradas()).to.equal(1n);
    const c = await contrato.obtenerCesion(cesionId);
    expect(c.estado).to.equal(2); // REGISTRADA
  });

  it("el MISMO firmante no puede firmar dos veces (no se auto-aprueba)", async function () {
    await contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle);
    await expect(contrato.confirmarCesion(cesionId))
      .to.be.revertedWithCustomError(contrato, "FirmanteDuplicado")
      .withArgs(cesionId, admin.address);
    // Sigue pendiente, no registrada.
    expect(await contrato.estaRegistrada(cesionId)).to.equal(false);
  });

  it("no se puede confirmar una cesion inexistente", async function () {
    await expect(contrato.connect(firmante2).confirmarCesion(ethers.id("NO-EXISTE")))
      .to.be.revertedWithCustomError(contrato, "CesionNoExiste");
  });

  it("no se puede proponer dos veces la misma cesion", async function () {
    await contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle);
    await expect(
      contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle)
    )
      .to.be.revertedWithCustomError(contrato, "CesionYaExiste")
      .withArgs(cesionId);
  });

  it("confirmar una cesion ya registrada revierte (YaRegistrada)", async function () {
    await contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle);
    await contrato.connect(firmante2).confirmarCesion(cesionId);
    await contrato.autorizarFirmante(firmante3.address);
    await expect(contrato.connect(firmante3).confirmarCesion(cesionId))
      .to.be.revertedWithCustomError(contrato, "YaRegistrada")
      .withArgs(cesionId);
  });

  it("una cuenta sin rol de firmante no puede proponer ni confirmar", async function () {
    await expect(
      contrato.connect(intruso).proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle)
    ).to.be.revertedWithCustomError(contrato, "AccessControlUnauthorizedAccount");

    await contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle);
    await expect(
      contrato.connect(intruso).confirmarCesion(cesionId)
    ).to.be.revertedWithCustomError(contrato, "AccessControlUnauthorizedAccount");
  });

  it("rechaza datos vacios al proponer", async function () {
    await expect(
      contrato.proponerCesion(ethers.ZeroHash, certificadoId, cedente, cesionario, hashDetalle)
    ).to.be.revertedWithCustomError(contrato, "CesionIdVacio");

    await expect(
      contrato.proponerCesion(cesionId, ethers.ZeroHash, cedente, cesionario, hashDetalle)
    ).to.be.revertedWithCustomError(contrato, "DatoVacio");
  });

  it("pausado bloquea proponer/confirmar hasta reanudar", async function () {
    await contrato.pausar();
    await expect(
      contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle)
    ).to.be.revertedWithCustomError(contrato, "EnforcedPause");

    await contrato.reanudar();
    await expect(
      contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle)
    ).to.not.be.reverted;
  });

  it("el admin puede revocar un firmante (deja de poder firmar)", async function () {
    await contrato.revocarFirmante(firmante2.address);
    await contrato.proponerCesion(cesionId, certificadoId, cedente, cesionario, hashDetalle);
    await expect(
      contrato.connect(firmante2).confirmarCesion(cesionId)
    ).to.be.revertedWithCustomError(contrato, "AccessControlUnauthorizedAccount");
  });
});
