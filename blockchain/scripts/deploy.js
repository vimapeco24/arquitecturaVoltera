const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

// ──────────────────────────────────────────────────────────────────────────
// Despliega LiquidacionP2PAnchor en la red seleccionada.
//   Local : npm run deploy:local   (requiere `npm run node` en otra terminal)
//   Amoy  : npm run deploy:amoy     (testnet GRATUITA via RPC de tercero)
// Guarda la direccion en deployments/<red>.json para que la consuma el gateway.
// ──────────────────────────────────────────────────────────────────────────
async function main() {
  const net = hre.network.name;
  const [deployer] = await hre.ethers.getSigners();

  if (!deployer) {
    throw new Error(
      "No hay cuenta configurada. Define PRIVATE_KEY en .env (cuenta de prueba con gas del faucet)."
    );
  }

  const balance = await hre.ethers.provider.getBalance(deployer.address);
  console.log(`Red:        ${net} (chainId ${hre.network.config.chainId})`);
  console.log(`Deployer:   ${deployer.address}`);
  console.log(`Balance:    ${hre.ethers.formatEther(balance)} POL`);

  if (balance === 0n) {
    console.warn(
      "\n⚠  Balance 0. Pide POL gratis en un faucet de Amoy antes de desplegar:\n" +
      "   https://faucet.polygon.technology (red Amoy, token POL)\n"
    );
  }

  const Factory = await hre.ethers.getContractFactory("LiquidacionP2PAnchor");
  // El admin/oraculo inicial es el propio deployer (la cuenta del gateway).
  const contrato = await Factory.deploy(deployer.address);
  await contrato.waitForDeployment();

  const address = await contrato.getAddress();
  console.log(`\n✓ LiquidacionP2PAnchor desplegado en: ${address}`);
  if (net === "amoy") {
    console.log(`  Explorer: https://amoy.polygonscan.com/address/${address}`);
  }

  // Registro de comunidades energeticas (padron on-chain). Complementa al anchor.
  const FactoryCom = await hre.ethers.getContractFactory("ComunidadEnergeticaRegistry");
  const registro = await FactoryCom.deploy(deployer.address);
  await registro.waitForDeployment();
  const addressRegistro = await registro.getAddress();
  console.log(`✓ ComunidadEnergeticaRegistry desplegado en: ${addressRegistro}`);
  if (net === "amoy") {
    console.log(`  Explorer: https://amoy.polygonscan.com/address/${addressRegistro}`);
  }

  // Cesion de certificados de origen con MULTIPLE AUTHORIZATION (2 firmas).
  const FactoryCesion = await hre.ethers.getContractFactory("CesionCertificadoMultisig");
  const cesion = await FactoryCesion.deploy(deployer.address);
  await cesion.waitForDeployment();
  const addressCesion = await cesion.getAddress();
  console.log(`✓ CesionCertificadoMultisig desplegado en: ${addressCesion}`);
  if (net === "amoy") {
    console.log(`  Explorer: https://amoy.polygonscan.com/address/${addressCesion}`);
  }
  console.log(
    `  ⚠ Recuerda: el multisig necesita >= 2 firmantes. Autoriza un 2o firmante con\n` +
    `    autorizarFirmante(<otra_cuenta>) o nada se podra registrar.`
  );

  // Certificado de origen como TOKEN NO FUNGIBLE (ERC-721 de OpenZeppelin).
  const FactoryNft = await hre.ethers.getContractFactory("CertificadoOrigenNFT");
  const nft = await FactoryNft.deploy(deployer.address);
  await nft.waitForDeployment();
  const addressNft = await nft.getAddress();
  console.log(`✓ CertificadoOrigenNFT (ERC-721 no fungible) desplegado en: ${addressNft}`);
  if (net === "amoy") {
    console.log(`  Explorer: https://amoy.polygonscan.com/address/${addressNft}`);
  }

  // Persistir las direcciones para el gateway.
  const outDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outDir, { recursive: true });
  const outFile = path.join(outDir, `${net}.json`);
  fs.writeFileSync(
    outFile,
    JSON.stringify(
      {
        network: net,
        chainId: hre.network.config.chainId,
        contract: "LiquidacionP2PAnchor",
        address,
        registroComunidades: {
          contract: "ComunidadEnergeticaRegistry",
          address: addressRegistro,
        },
        cesionCertificados: {
          contract: "CesionCertificadoMultisig",
          address: addressCesion,
        },
        certificadoOrigenNFT: {
          contract: "CertificadoOrigenNFT",
          address: addressNft,
          tipo: "ERC-721 no fungible",
        },
        deployer: deployer.address,
        deployedAt: new Date().toISOString(),
      },
      null,
      2
    )
  );
  console.log(`  Direcciones guardadas en: deployments/${net}.json`);
  console.log(
    `\n→ Configura el gateway con:\n` +
    `    VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS=${address}\n` +
    `    VOLTERA_BLOCKCHAIN_REGISTRO_COMUNIDADES=${addressRegistro}\n` +
    `    VOLTERA_BLOCKCHAIN_CESION_CERTIFICADOS=${addressCesion}\n` +
    `    VOLTERA_BLOCKCHAIN_CERTIFICADO_NFT=${addressNft}`
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
