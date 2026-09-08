# legacyvault

> **Legacy Vault** helps families discover everything a senior citizen owns, keeps their important access details safe and easy to manage, and passes it all down safely — verified by people they trust, only when it's actually needed.

Built for the **iQOO Hackathon 2026, Chennai Battle — FinTech and Commerce track**.

---

## 🌟 Key Architecture & Pillars

1. **Elder-Friendly Asset Discovery (~50%)**
   - **On-Device Google ML Kit Neural OCR:** Extracts accounts from physical passbooks, old LIC policy receipts, and postal savings certificates with zero cloud transmission.
   - **Honest Gap Flagging:** Flags obscured account numbers or missing nominees with plain-language branch verification notices instead of hallucinating.
   - **Rule-Based Deduplication:** Fuzzy-matches institution names, account types, and trailing digit identifiers.

2. **Practical Access Storage (~20%)**
   - Stores where physical bank locker keys, land deeds, and safe combos are kept using soft, reassuring language (never "passwords" or "secrets").
   - Encrypted at rest using **AES-256-GCM**.

3. **Cryptographic Release & Passing Down (~30%)**
   - **Living-Signal Check-In:** Caring, 1-tap periodic living signal.
   - **Shamir's Secret Sharing (SSS):** The master 256-bit AES encryption key is mathematically split into 3 shares over Galois Field $GF(2^8)$ with a 2-of-3 threshold across trusted attestors (e.g. family lawyer, family physician, and CA). No single party or company holds the whole key.
   - **Polygon Amoy Smart Contract:** Immutably logs attestation consensus events on-chain (`AttestationRegistry.sol`) so release sequence timestamps cannot be disputed or rewritten.

4. **Office Kit Cross-Device Synergy (10% Rubric)**
   - Screen mirror bridge between the senior's iQOO phone and the adult child's laptop.
   - One-click cross-device PDF file drop of the synthesized estate summary.

---

## 📁 Repository Structure

```
legacyvault/
├── app/                  # Senior-friendly Android Kotlin Jetpack Compose App
│   ├── src/main/java/com/example/legacyvault/
│   │   ├── ui/screens/   # OwnerHomeScreen, DiscoveryScanScreen (ML Kit), AttestorBeneficiaryScreen
│   │   ├── ui/theme/     # Senior-friendly warm cream palette & high-contrast typography
│   │   ├── data/network/ # Retrofit + OkHttp API client
│   │   └── data/         # Offline-first demo repository
├── backend/              # FastAPI Python Backend
│   ├── main.py           # REST API endpoints & check-in escalation engine
│   ├── crypto_utils.py   # Shamir's Secret Sharing in GF(256) + AES-256-GCM
│   └── test_backend.py   # Automated backend & crypto test suite
├── blockchain/           # Smart Contract & Web3
│   ├── contracts/        # AttestationRegistry.sol (Polygon Amoy Testnet)
│   └── deploy_and_test.py # Live Web3 deployment & verification harness
├── document_pipeline/    # Synthesis & PDF Generation
│   └── pipeline.py       # Deduplication, honest gap flagging, and ReportLab PDF generator
├── scripts/              # Hackathon Stage Reliability Tools
│   ├── preflight_check.py # 30-min pre-stage diagnostic tool (ADB reverse, RPC, Backend check)
│   └── reset_demo.py     # 1-command demo reset & seed tool
└── IQOO Hackathon/       # Pitch Scripts & Context
    └── DEMO-PITCH-GUIDE.md # 3-minute rehearsed spoken pitch script & judge Q&A
```

---

## 🚀 Quickstart & Running the Demo

### 1. Start the Backend Server
```bash
# Setup virtual environment & dependencies
python3 -m venv backend/venv
source backend/venv/bin/activate
pip install -r backend/requirements.txt web3

# Launch server
python3 backend/main.py
```

### 2. Run Pre-Flight Stage Diagnostics
```bash
python3 scripts/preflight_check.py
```

### 3. Reset Demo State (One-Command)
```bash
python3 scripts/reset_demo.py
```

### 4. Build & Install Android App
```bash
# Compile debug APK
./gradlew assembleDebug

# Install to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Reverse port for zero-flake USB connectivity
adb reverse tcp:8000 tcp:8000
```
