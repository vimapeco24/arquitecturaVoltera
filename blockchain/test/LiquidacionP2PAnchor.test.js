const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("LiquidacionP2PAnchor", function () {
  let contrato, admin, oraculo2, intruso;

  const txId = ethers.id("TX-VOLTERA-0001");           // bytes32
  const hashCasacion = ethers.id("payload-canonico-1"); // bytes32
  const energiaWh = 12_500;
  const liquidadaEn = Math.floor(Date.now() / 1000);

  beforeEach(async function () {
    [admin, oraculo2, intruso] = await ethers.getSigners();
    const Factory = await ethers.getContractFactory("LiquidacionP2PAnchor");
    contrato = await Factory.deploy(admin.address);
    await contrato.waitForDeployment();
  });

  it("el admin inicial tiene rol de oraculo y ancla correctamente", async function () {
    await expect(contrato.anclar(txId, hashCasacion, energiaWh, liquidadaEn))
      .to.emit(contrato, "LiquidacionAnclada")
      .withArgs(txId, hashCasacion, energiaWh, liquidadaEn, anyUint, admin.address);

    expect(await contrato.estaAnclada(txId)).to.equal(true);
    expect(await contrato.totalAncladas()).to.equal(1n);

    const a = await contrato.obtener(txId);
    expect(a.hashCasacion).to.equal(hashCasacion);
    expect(a.energiaWh).to.equal(energiaWh);
    expect(a.oraculo).to.equal(admin.address);
  });

  it("verificar() confirma el hash anclado y rechaza uno distinto", async function () {
    await contrato.anclar(txId, hashCasacion, energiaWh, liquidadaEn);
    expect(await contrato.verificar(txId, hashCasacion)).to.equal(true);
    expect(await contrato.verificar(txId, ethers.id("otro"))).to.equal(false);
  });

  it("reintento idempotente con el mismo hash no revierte ni duplica", async function () {
    await contrato.anclar(txId, hashCasacion, energiaWh, liquidadaEn);
    await expect(contrato.anclar(txId, hashCasacion, energiaWh, liquidadaEn)).to.not.be.reverted;
    expect(await contrato.totalAncladas()).to.equal(1n); // sigue en 1
  });

  it("re-anclar la misma transaccion con hash distinto revierte (no repudio)", async function () {
    await contrato.anclar(txId, hashCasacion, energiaWh, liquidadaEn);
    await expect(contrato.anclar(txId, ethers.id("hash-alterado"), energiaWh, liquidadaEn))
      .to.be.revertedWithCustomError(contrato, "TransaccionYaAnclada")
      .withArgs(txId);
  });

  it("una cuenta sin rol de oraculo no puede anclar", async function () {
    await expect(
      contrato.connect(intruso).anclar(txId, hashCasacion, energiaWh, liquidadaEn)
    ).to.be.revertedWithCustomError(contrato, "AccessControlUnauthorizedAccount");
  });

  it("rechaza transaccionId o hash vacios", async function () {
    await expect(
      contrato.anclar(ethers.ZeroHash, hashCasacion, energiaWh, liquidadaEn)
    ).to.be.revertedWithCustomError(contrato, "TransaccionIdVacio");

    await expect(
      contrato.anclar(txId, ethers.ZeroHash, energiaWh, liquidadaEn)
    ).to.be.revertedWithCustomError(contrato, "HashVacio");
  });

  it("el admin puede autorizar y revocar otro oraculo", async function () {
    await contrato.autorizarOraculo(oraculo2.address);
    await expect(
      contrato.connect(oraculo2).anclar(txId, hashCasacion, energiaWh, liquidadaEn)
    ).to.not.be.reverted;

    await contrato.revocarOraculo(oraculo2.address);
    const txId2 = ethers.id("TX-VOLTERA-0002");
    await expect(
      contrato.connect(oraculo2).anclar(txId2, hashCasacion, energiaWh, liquidadaEn)
    ).to.be.revertedWithCustomError(contrato, "AccessControlUnauthorizedAccount");
  });

  it("pausado, el anclaje queda bloqueado hasta reanudar", async function () {
    await contrato.pausar();
    await expect(
      contrato.anclar(txId, hashCasacion, energiaWh, liquidadaEn)
    ).to.be.revertedWithCustomError(contrato, "EnforcedPause");

    await contrato.reanudar();
    await expect(contrato.anclar(txId, hashCasacion, energiaWh, liquidadaEn)).to.not.be.reverted;
  });
});

// helper para el matcher withArgs cuando un argumento es un timestamp variable
const { anyValue: anyUint } = require("@nomicfoundation/hardhat-chai-matchers/withArgs");
