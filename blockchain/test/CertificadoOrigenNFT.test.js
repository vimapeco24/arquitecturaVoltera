const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("CertificadoOrigenNFT (token NO fungible · ERC-721 OpenZeppelin)", function () {
  let contrato, admin, prosumidor, otro, intruso;

  const uri = "ipfs://bafy-certificado-0001";
  const hashDetalle = ethers.id("doc-certificado-solar-0001");
  const idMedidor = ethers.id("MEDIDOR-IOT-0001");
  const consumoNetoMensual = 120000n; // Wh
  const periodo = 202610; // AAAAMM

  beforeEach(async function () {
    [admin, prosumidor, otro, intruso] = await ethers.getSigners();
    const Factory = await ethers.getContractFactory("CertificadoOrigenNFT");
    contrato = await Factory.deploy(admin.address);
    await contrato.waitForDeployment();
  });

  it("es un ERC-721 (no fungible): nombre, símbolo e interfaceId 0x80ac58cd", async function () {
    expect(await contrato.name()).to.equal("Voltera Certificado de Origen");
    expect(await contrato.symbol()).to.equal("VCO");
    // ERC-721 interface id
    expect(await contrato.supportsInterface("0x80ac58cd")).to.equal(true);
    // ERC-721 Metadata
    expect(await contrato.supportsInterface("0x5b5e139f")).to.equal(true);
  });

  it("acuña un certificado con ID medidor, propietario, emisor, consumo neto y periodo", async function () {
    await expect(
      contrato.emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, periodo, hashDetalle)
    )
      .to.emit(contrato, "CertificadoEmitido")
      .withArgs(1, prosumidor.address, idMedidor, admin.address, consumoNetoMensual, periodo);

    expect(await contrato.ownerOf(1)).to.equal(prosumidor.address);
    expect(await contrato.balanceOf(prosumidor.address)).to.equal(1n);
    expect(await contrato.tokenURI(1)).to.equal(uri);
    expect(await contrato.totalEmitidos()).to.equal(1n);

    const c = await contrato.certificados(1);
    expect(c.idMedidor).to.equal(idMedidor);
    expect(c.emisor).to.equal(admin.address);
    expect(c.consumoNetoMensual).to.equal(consumoNetoMensual);
    expect(c.periodo).to.equal(periodo);
  });

  it("cada certificado tiene un tokenId distinto (unicidad = no fungible)", async function () {
    await contrato.emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, periodo, hashDetalle);
    await contrato.emitirCertificado(otro.address, "ipfs://otro", ethers.id("MEDIDOR-IOT-0002"), 50000n, periodo, ethers.id("doc2"));
    expect(await contrato.ownerOf(1)).to.equal(prosumidor.address);
    expect(await contrato.ownerOf(2)).to.equal(otro.address);
    expect(await contrato.totalEmitidos()).to.equal(2n);
  });

  it("rechaza DUPLICADOS para el mismo (idMedidor, periodo) — lámina 06", async function () {
    await contrato.emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, periodo, hashDetalle);
    await expect(
      contrato.emitirCertificado(otro.address, "ipfs://dup", idMedidor, 99999n, periodo, ethers.id("otro-doc"))
    ).to.be.revertedWithCustomError(contrato, "CertificadoDuplicado");
    // el mismo medidor en OTRO periodo sí se permite
    await expect(
      contrato.emitirCertificado(otro.address, "ipfs://nuevo", idMedidor, 77000n, 202611, ethers.id("doc-nov"))
    ).to.not.be.reverted;
    expect(await contrato.tokenDe(idMedidor, periodo)).to.equal(1n);
    expect(await contrato.tokenDe(idMedidor, 202611)).to.equal(2n);
  });

  it("el propietario puede transferir el certificado (de una billetera a otra, trazable on-chain)", async function () {
    await contrato.emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, periodo, hashDetalle);
    await expect(
      contrato.connect(prosumidor).transferFrom(prosumidor.address, otro.address, 1)
    )
      .to.emit(contrato, "Transfer")
      .withArgs(prosumidor.address, otro.address, 1);
    expect(await contrato.ownerOf(1)).to.equal(otro.address);
  });

  it("solo EMISOR_ROLE puede acuñar", async function () {
    await expect(
      contrato.connect(intruso).emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, periodo, hashDetalle)
    ).to.be.revertedWithCustomError(contrato, "AccessControlUnauthorizedAccount");
  });

  it("rechaza ID medidor vacío, consumo cero, periodo cero o hash vacío", async function () {
    await expect(
      contrato.emitirCertificado(prosumidor.address, uri, ethers.ZeroHash, consumoNetoMensual, periodo, hashDetalle)
    ).to.be.revertedWithCustomError(contrato, "IdMedidorVacio");
    await expect(
      contrato.emitirCertificado(prosumidor.address, uri, idMedidor, 0, periodo, hashDetalle)
    ).to.be.revertedWithCustomError(contrato, "ConsumoInvalido");
    await expect(
      contrato.emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, 0, hashDetalle)
    ).to.be.revertedWithCustomError(contrato, "PeriodoInvalido");
    await expect(
      contrato.emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, periodo, ethers.ZeroHash)
    ).to.be.revertedWithCustomError(contrato, "HashVacio");
  });

  it("el certificado se puede quemar/retirar (ERC721Burnable)", async function () {
    await contrato.emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, periodo, hashDetalle);
    await contrato.connect(prosumidor).burn(1);
    await expect(contrato.ownerOf(1)).to.be.revertedWithCustomError(contrato, "ERC721NonexistentToken");
  });

  it("el admin puede delegar EMISOR_ROLE a otro emisor", async function () {
    await contrato.grantRole(await contrato.EMISOR_ROLE(), otro.address);
    await expect(
      contrato.connect(otro).emitirCertificado(prosumidor.address, uri, idMedidor, consumoNetoMensual, periodo, hashDetalle)
    ).to.not.be.reverted;
  });
});
