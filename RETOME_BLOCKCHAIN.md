# 🔖 Punto de retome — Blockchain Voltera

> Última actualización: 2026-10-21. Lee este archivo PRIMERO al reconectar, junto con
> `SESION_BLOCKCHAIN_ESTADO.md` (detalle completo). Este es el resumen accionable.

---

## ✅ Estado actual (verificado en vivo el 2026-10-21)

| Pieza | Estado | Evidencia |
|---|---|---|
| **Git** `main` | ✅ todo pusheado | HEAD `084fce7`, árbol limpio, origin == local |
| **Gateway (Render)** | ✅ LIVE y al día | `/actuator/health` 200; `/api/v1/anclajes/comunidades` 200; red `configurada:true, disponible:true` |
| **Contrato LiquidacionP2PAnchor (Amoy)** | ✅ desplegado | `0xd0Ef1686fcAF3f026134DE4188bd977c4EBcf530`, chainId 80002 |
| **Código en GitHub** | ✅ completo | proyecto Hardhat + diagrama ya versionados (antes estaban ignorados) |
| **Front (Vercel)** | ⚠️ VERSIÓN VIEJA | sirve `main-7URIMLIU.js`; NO tiene la feature de comunidades. El build local nuevo es `main-JNXGBHCI.js` |

URLs:
- Front: https://voltera-frontend.vercel.app  (y `/blockchain`)
- Gateway: https://voltera-blockchain-gateway.onrender.com  (free tier: duerme ~15 min, cold-start ~50-90 s)
- Repo: https://github.com/vimapeco24/arquitecturaVoltera.git

---

## ❌ LO QUE FALTA POR AJUSTAR (requiere credenciales del usuario — el agente no puede)

### 1. Re-deploy del FRONT a Vercel  ← ESTO ES LO PRINCIPAL QUE FALTA
El código del marketplace nuevo (comunidades, detalle de contrato, exportar CSV/JSON,
re-sincronizar, bloque/gas) está **commiteado y compilado**, pero Vercel aún sirve la versión
anterior. Para publicarlo:

```bash
cd Microservicios/voltera-frontend
VERCEL_TOKEN=<tu-token-nuevo> node_modules/.bin/vercel deploy --prod --yes
```

- ⚠️ Generar un token NUEVO en Vercel → Settings → Tokens (el anterior quedó expuesto en chat).
- Alternativa: si el proyecto de Vercel está conectado al repo de GitHub con auto-deploy,
  puede que ya se haya redeplegado solo tras el último push → recargar y revisar.
- Cómo comprobar que quedó: abrir `/blockchain` y ver el campo "Comunidad energética" y el
  panel de comunidades. O desde terminal, que el `main-*.js` servido coincida con el build local
  y contenga el texto "Comunidad energética".

### 2. (OPCIONAL) Desplegar ComunidadEnergeticaRegistry a Polygon Amoy
El contrato nuevo del padrón de comunidades está **escrito y probado (17/17 tests)** pero NO
desplegado. Hoy la comunidad funciona como metadato off-chain (suficiente para el marketplace);
desplegarlo solo añade el registro de membresía on-chain. Requiere tu wallet de prueba con POL:

```bash
cd blockchain
npm run deploy:amoy          # despliega AMBOS contratos y guarda direcciones en deployments/amoy.json
# luego poner en el gateway (Render → Environment):
#   VOLTERA_BLOCKCHAIN_REGISTRO_COMUNIDADES=<direccion_registro>
```

### 3. (SEGURIDAD) Rotar el token de Vercel
El `VERCEL_TOKEN` de una sesión previa quedó pegado en el chat → revocarlo/rotarlo en
Vercel → Settings → Tokens. La wallet de prueba de Amoy es desechable (no urge, pero conviene).

---

## 🧭 Checklist rápido al volver

```bash
export PATH="/Library/Developer/CommandLineTools/usr/bin:$PATH"
cd /Users/vimapeco/Downloads/ArquitectutaNuevaGenegeracion
git log --oneline -1 && git status --short            # limpio, HEAD >= 084fce7

# gateway vivo + feature nueva (puede tardar por cold-start)
curl -s -m 120 https://voltera-blockchain-gateway.onrender.com/api/v1/anclajes/red
curl -s -m 60  -o /dev/null -w "comunidades: %{http_code}\n" \
  https://voltera-blockchain-gateway.onrender.com/api/v1/anclajes/comunidades

# ¿el front en Vercel ya es el nuevo? (busca la feature)
curl -s https://voltera-frontend.vercel.app/ | grep -oE 'main-[A-Z0-9]+\.js'
#   -> si NO es el build local nuevo, falta el deploy del punto 1
```

---

## 📦 Lo que se implementó en la última sesión (ya en git, commits 58acd2f → 084fce7)

**Gateway (retrocompatible, no rompe el contrato ya desplegado):**
- `comunidadId` como metadato OFF-CHAIN (no entra en el hash → no afecta idempotencia/verificación).
- `AnclajeResponse` expone `bloque` y `gasUsado` del recibo on-chain.
- Fix: `Web3jCadenaAdapter` llamaba `waitForTransactionReceipt` dos veces.
- Nuevo `GET /api/v1/anclajes/comunidades` (resumen por comunidad).
- Tests 6 → 8.

**Contrato (blockchain/):**
- Nuevo `ComunidadEnergeticaRegistry.sol` (padrón + membresía + `puedenCasar()`), 9 tests.
- `deploy.js` despliega ambos contratos. Total tests Hardhat: 17/17.

**Frontend (pages/blockchain):**
- Campo comunidad + regla "solo casan ofertas de la misma comunidad".
- Panel de comunidades, detalle de contrato (modal con copiar hash/txHash, bloque, gas, timeline),
  re-sincronizar desde el gateway, exportar evidencia CSV/JSON.
- `tsc` OK, `ng build` prod OK (blockchain-component 44.79 kB).

**Git / repo:**
- Arreglado el `.gitignore`: `/Blockchain/` tapaba (en macOS case-insensitive) la carpeta
  `blockchain/` del Hardhat → ahora se ignoran solo PDFs + artefactos de build, y se versiona
  el código del contrato, los tests, `deploy.js`, `hardhat.config.js`, `package.json`, el diagrama
  (`Modificados/Voltera_Arquitectura_Completa.drawio` + `.png`) y las plantillas `.env.example`
  (el `.env` real sigue ignorado).

**Validado en producción:** anclaje real con comunidad → CONFIRMADO, `bloque:50778774`,
`gasUsado:119976`; `/comunidades` agrega bien.

---

## ⚙️ Notas técnicas (no olvidar)
- **git bloqueado por licencia Xcode**: usar `export PATH="/Library/Developer/CommandLineTools/usr/bin:$PATH"` antes de cualquier `git`.
- **Render free tier duerme**: el 1er request tras inactividad puede dar HTTP 000 / tardar ~50-90 s. No es un fallo: reintentar.
- **Puerto gateway**: 8101. **Modo nube**: solo-REST (`KAFKA_ENABLED=false`), Kafka del server es LAN privada.
- **Ruta del endpoint verificar**: `/api/v1/anclajes/verificar` (NO `/api/v1/verificar`).
- **Diagrama vivo**: si se cambia arquitectura, actualizar también
  `Modificados/Voltera_Arquitectura_Completa.drawio` (regla permanente en `.kiro/steering/`).
