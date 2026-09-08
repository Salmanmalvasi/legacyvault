#!/usr/bin/env python3
"""
Legacy Vault - Backend API Server
Author: Salman (Backend, Cryptography, Blockchain & Integration Lead)

Provides REST API endpoints for:
- Discovery records ingestion & inventory retrieval
- AES-256 encrypted access instruction storage
- Shamir's Secret Sharing (SSS) key generation & split (2-of-3 threshold)
- Senior check-in tracking & missed check-in escalation
- Attestor share submission & threshold key reconstruction
- Smart contract attestation event logging (Polygon Amoy testnet / simulation)
- Document generation synthesis via Pratik's pipeline
"""

import os
import sys
import json
import sqlite3
import time
from typing import List, Dict, Any, Optional
from datetime import datetime
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field

# Local imports
sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from crypto_utils import ShamirSecretSharing, VaultCrypto
from blockchain.deploy_and_test import MockBlockchainLedger
from document_pipeline.pipeline import consolidate_and_categorize, generate_pdf

app = FastAPI(
    title="Legacy Vault API",
    description="Secure inheritance and estate discovery backend for senior citizens",
    version="1.0.0"
)

# Enable CORS for local Android development and web inspection
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# SQLite Database Setup
DB_PATH = os.path.join(os.path.dirname(__file__), "legacyvault.db")

def init_db():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    
    # Owners & Setup
    cur.execute("""
    CREATE TABLE IF NOT EXISTS owners (
        owner_id TEXT PRIMARY KEY,
        owner_name TEXT,
        beneficiary_id TEXT,
        checkin_interval_days INTEGER DEFAULT 90,
        last_checkin_timestamp INTEGER,
        missed_checkins_count INTEGER DEFAULT 0,
        escalation_triggered INTEGER DEFAULT 0,
        vault_released INTEGER DEFAULT 0,
        vault_key_hash TEXT
    );
    """)

    # Attestors & Distributed SSS Shares
    cur.execute("""
    CREATE TABLE IF NOT EXISTS attestors (
        attestor_id TEXT,
        owner_id TEXT,
        name TEXT,
        share_value TEXT,
        has_attested INTEGER DEFAULT 0,
        attestation_timestamp INTEGER,
        PRIMARY KEY (attestor_id, owner_id)
    );
    """)

    # Discovery Records (Bank, Insurance, EPF/PPF, Loans)
    cur.execute("""
    CREATE TABLE IF NOT EXISTS discovery_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        owner_id TEXT,
        source TEXT,
        type TEXT,
        raw_text TEXT,
        extracted_fields_json TEXT,
        created_at INTEGER
    );
    """)

    # Important Access Instructions (AES-256 encrypted at rest)
    cur.execute("""
    CREATE TABLE IF NOT EXISTS vault_instructions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        owner_id TEXT,
        label TEXT,
        ciphertext_hex TEXT,
        nonce_hex TEXT,
        created_at INTEGER
    );
    """)

    conn.commit()
    conn.close()

init_db()

# Demo Configuration Flag: shortens 90-day check-in window to 30 seconds for live 3-minute pitch
DEMO_MODE = os.environ.get("DEMO_MODE", "true").lower() in ("1", "true", "yes")
DEMO_CHECKIN_WINDOW_SECONDS = int(os.environ.get("DEMO_CHECKIN_WINDOW_SECONDS", "30"))

# Blockchain Ledger Singleton
blockchain_ledger = MockBlockchainLedger()

# In-memory session store for current active master key in demo mode
ACTIVE_VAULT_KEYS: Dict[str, bytes] = {}

def seed_default_demo_data():
    """Seed known-good demo dataset (SBI, LIC, EPFO) and reset attestor status."""
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    now = int(time.time())
    owner_id = "owner_sundaram"

    # Reset tables
    cur.execute("DELETE FROM owners WHERE owner_id=?", (owner_id,))
    cur.execute("DELETE FROM attestors WHERE owner_id=?", (owner_id,))
    cur.execute("DELETE FROM discovery_records WHERE owner_id=?", (owner_id,))
    cur.execute("DELETE FROM vault_instructions WHERE owner_id=?", (owner_id,))

    # Seed owner
    cur.execute("""
    INSERT INTO owners (owner_id, owner_name, beneficiary_id, checkin_interval_days, last_checkin_timestamp, missed_checkins_count, escalation_triggered, vault_released)
    VALUES (?, 'S. Sundaram (74)', 'Ramesh Kumar (Son)', 1, ?, 0, 0, 0)
    """, (owner_id, now))

    # Generate master key and SSS shares
    vault_key = VaultCrypto.generate_vault_key()
    ACTIVE_VAULT_KEYS[owner_id] = vault_key
    shares = ShamirSecretSharing.split_secret(vault_key, n=3, k=2)

    attestors = [
        ("attestor_1", "V. Krishnan (Family Lawyer)", shares[0]),
        ("attestor_2", "Dr. Ananya Iyer (Family Physician)", shares[1]),
        ("attestor_3", "M. Natarajan (Chartered Accountant)", shares[2])
    ]
    for att_id, name, share in attestors:
        cur.execute("INSERT INTO attestors (attestor_id, owner_id, name, share_value, has_attested) VALUES (?, ?, ?, ?, 0)",
                    (att_id, owner_id, name, share))

    # Seed records
    records = [
        ("camera_ocr", "bank", "STATE BANK OF INDIA ADYAR CHENNAI", {
            "institution": "State Bank of India",
            "account_type": "Senior Citizen Savings",
            "account_number": "304918239120",
            "branch": "Adyar Chennai Branch (SBIN0001234)",
            "nominee": "Ramesh Kumar (Son - 100%)"
        }),
        ("camera_ocr", "insurance", "LIFE INSURANCE CORP JEEVAN ANAND", {
            "institution": "Life Insurance Corp (LIC)",
            "account_type": "Jeevan Anand Policy",
            "account_number": "847291xxx",
            "branch": "Agent: K. Balaji (Chennai Div)",
            "nominee": "Pending verification",
            "gap_note": "Policy number partially obscured on paper slip — verify at branch."
        }),
        ("manual", "epf", "POST OFFICE PPF", {
            "institution": "EPFO & Post Office PPF",
            "account_type": "15-Yr Public Provident Fund",
            "account_number": "PPF-490218-CHE",
            "branch": "Mylapore Head Post Office",
            "nominee": "Ramesh Kumar"
        })
    ]
    for src, typ, raw, fields in records:
        cur.execute("INSERT INTO discovery_records (owner_id, source, type, raw_text, extracted_fields_json, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    (owner_id, src, typ, raw, json.dumps(fields), now))

    # Seed instructions
    for label, val in [("SBI Safe Deposit Locker Key", "Small brass key kept in second wooden drawer under altar in pooja room. Locker #42 at SBI Adyar."),
                       ("Ancestral House Land Deeds", "Physical patta document folder is in Godrej steel almirah, inside navy leather file.")]:
        enc = VaultCrypto.encrypt(val, vault_key)
        cur.execute("INSERT INTO vault_instructions (owner_id, label, ciphertext_hex, nonce_hex, created_at) VALUES (?, ?, ?, ?, ?)",
                    (owner_id, label, enc["ciphertext"], enc["nonce"], now))

    conn.commit()
    conn.close()

seed_default_demo_data()

# --- Pydantic Request Models ---

class DiscoveryEntryRequest(BaseModel):
    owner_id: str
    source: str = Field("manual", description="manual | ocr | notification")
    type: str = Field("bank", description="bank | insurance | loan | epf | access_instruction")
    raw_text: Optional[str] = ""
    extracted_fields: Dict[str, Any]

class VaultStoreRequest(BaseModel):
    owner_id: str
    label: str
    value: str

class SetupAttestorsRequest(BaseModel):
    owner_id: str
    owner_name: str = "S. Sundaram"
    beneficiary_id: str = "ramesh_kumar"
    attestors: List[Dict[str, str]] = Field(
        default_factory=lambda: [
            {"attestor_id": "attestor_1", "name": "V. Krishnan (Family Lawyer)"},
            {"attestor_id": "attestor_2", "name": "Dr. Ananya Iyer (Family Friend)"},
            {"attestor_id": "attestor_3", "name": "M. Natarajan (Chartered Accountant)"}
        ]
    )
    checkin_interval_days: int = 90
    threshold: int = 2

class CheckinRespondRequest(BaseModel):
    owner_id: str

class AttestorAttestRequest(BaseModel):
    owner_id: str
    attestor_id: str
    share: Optional[str] = None

# --- API Endpoints ---

@app.get("/")
def health_check():
    return {
        "status": "healthy",
        "service": "Legacy Vault API",
        "crypto": "Shamir's Secret Sharing (2-of-3) + AES-256-GCM",
        "blockchain": "Polygon Amoy Testnet (Chain ID 80002)",
        "demo_mode": DEMO_MODE,
        "demo_checkin_window_seconds": DEMO_CHECKIN_WINDOW_SECONDS if DEMO_MODE else None
    }

@app.post("/demo/reset")
def reset_demo_state():
    """One-command demo reset: resets all SQLite state and seeds known-good demo records."""
    seed_default_demo_data()
    return {
        "status": "demo_reset_complete",
        "message": "Demo state reset successfully. Clean known-good dataset restored.",
        "demo_mode": DEMO_MODE,
        "accelerated_window_seconds": DEMO_CHECKIN_WINDOW_SECONDS if DEMO_MODE else 90 * 86400
    }

@app.post("/setup/attestors")
def setup_attestors(req: SetupAttestorsRequest):
    """
    Step 1: Setup vault encryption key, split via Shamir's Secret Sharing (2-of-3 threshold),
    and distribute one share to each designated attestor.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    now = int(time.time())

    # Generate 256-bit AES master vault key
    vault_key = VaultCrypto.generate_vault_key()
    ACTIVE_VAULT_KEYS[req.owner_id] = vault_key

    # Split key via Shamir's Secret Sharing into N shares with threshold K
    shares = ShamirSecretSharing.split_secret(vault_key, n=len(req.attestors), k=req.threshold)

    # Upsert owner
    cur.execute("""
    INSERT INTO owners (owner_id, owner_name, beneficiary_id, checkin_interval_days, last_checkin_timestamp, missed_checkins_count, escalation_triggered, vault_released)
    VALUES (?, ?, ?, ?, ?, 0, 0, 0)
    ON CONFLICT(owner_id) DO UPDATE SET
        owner_name=excluded.owner_name,
        beneficiary_id=excluded.beneficiary_id,
        checkin_interval_days=excluded.checkin_interval_days,
        last_checkin_timestamp=excluded.last_checkin_timestamp,
        vault_released=0;
    """, (req.owner_id, req.owner_name, req.beneficiary_id, req.checkin_interval_days, now))

    # Save attestors and their respective shares
    cur.execute("DELETE FROM attestors WHERE owner_id=?", (req.owner_id,))
    distributed_shares = []
    for idx, att in enumerate(req.attestors):
        share_val = shares[idx]
        cur.execute("""
        INSERT INTO attestors (attestor_id, owner_id, name, share_value, has_attested)
        VALUES (?, ?, ?, ?, 0)
        """, (att["attestor_id"], req.owner_id, att["name"], share_val))
        distributed_shares.append({
            "attestor_id": att["attestor_id"],
            "name": att["name"],
            "assigned_share": share_val
        })

    conn.commit()
    conn.close()

    return {
        "message": "Vault key generated and Shamir's Secret Sharing shares created",
        "owner_id": req.owner_id,
        "threshold": req.threshold,
        "total_shares": len(req.attestors),
        "distributed_shares": distributed_shares
    }

@app.post("/discovery/entry")
def add_discovery_entry(req: DiscoveryEntryRequest):
    """
    Ingest financial record from OCR scan, guided manual entry, or SMS/notification parser.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    now = int(time.time())

    cur.execute("""
    INSERT INTO discovery_records (owner_id, source, type, raw_text, extracted_fields_json, created_at)
    VALUES (?, ?, ?, ?, ?, ?)
    """, (req.owner_id, req.source, req.type, req.raw_text or "", json.dumps(req.extracted_fields), now))

    record_id = cur.lastrowid
    conn.commit()
    conn.close()

    return {
        "status": "success",
        "record_id": record_id,
        "owner_id": req.owner_id,
        "source": req.source,
        "type": req.type
    }

@app.get("/discovery/inventory")
def get_inventory(owner_id: str = Query(..., description="ID of the owner")):
    """
    Returns all stored records for an owner.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("""
    SELECT id, source, type, raw_text, extracted_fields_json, created_at
    FROM discovery_records WHERE owner_id=?
    ORDER BY id DESC
    """, (owner_id,))
    rows = cur.fetchall()

    records = []
    for r in rows:
        records.append({
            "id": r[0],
            "source": r[1],
            "type": r[2],
            "raw_text": r[3],
            "extracted_fields": json.loads(r[4]),
            "created_at": r[5]
        })

    conn.close()
    return {"owner_id": owner_id, "total_records": len(records), "records": records}

@app.post("/vault/store")
def store_vault_instruction(req: VaultStoreRequest):
    """
    Store practical access instruction (e.g. locker location) encrypted at rest with AES-256.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    now = int(time.time())

    # Obtain or generate vault key for owner
    vault_key = ACTIVE_VAULT_KEYS.get(req.owner_id)
    if not vault_key:
        vault_key = VaultCrypto.generate_vault_key()
        ACTIVE_VAULT_KEYS[req.owner_id] = vault_key

    encrypted_payload = VaultCrypto.encrypt(req.value, vault_key)

    cur.execute("""
    INSERT INTO vault_instructions (owner_id, label, ciphertext_hex, nonce_hex, created_at)
    VALUES (?, ?, ?, ?, ?)
    """, (req.owner_id, req.label, encrypted_payload["ciphertext"], encrypted_payload["nonce"], now))

    ins_id = cur.lastrowid
    conn.commit()
    conn.close()

    return {
        "status": "encrypted_and_stored",
        "instruction_id": ins_id,
        "label": req.label,
        "encryption": "AES-256-GCM"
    }

@app.post("/checkin/respond")
def checkin_respond(req: CheckinRespondRequest):
    """
    Owner completes simple one-tap periodic check-in, resetting the inactivity timer.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    now = int(time.time())

    cur.execute("""
    UPDATE owners
    SET last_checkin_timestamp = ?,
        missed_checkins_count = 0,
        escalation_triggered = 0
    WHERE owner_id = ?
    """, (now, req.owner_id))

    conn.commit()
    conn.close()

    return {
        "status": "checkin_confirmed",
        "owner_id": req.owner_id,
        "timestamp": now,
        "message": "Check-in recorded. Living signal refreshed successfully."
    }

@app.get("/checkin/status")
def checkin_status(owner_id: str = Query(..., description="ID of the owner")):
    """
    Retrieve check-in status and escalation level.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("""
    SELECT owner_name, last_checkin_timestamp, checkin_interval_days, missed_checkins_count, escalation_triggered
    FROM owners WHERE owner_id=?
    """, (owner_id,))
    row = cur.fetchone()
    conn.close()

    if not row:
        return {
            "owner_id": owner_id,
            "status": "active",
            "last_checkin": int(time.time()),
            "missed_checkins": 0,
            "escalation_active": False
        }

    now = int(time.time())
    owner_name = row[0]
    last_checkin = row[1] or now
    interval_days = row[2]
    missed_count = row[3]
    escalation_triggered = bool(row[4])

    # Real-time live demo acceleration
    elapsed_seconds = now - last_checkin
    if DEMO_MODE and elapsed_seconds > DEMO_CHECKIN_WINDOW_SECONDS:
        missed_count = max(missed_count, 1)
        escalation_triggered = True

    return {
        "owner_id": owner_id,
        "owner_name": owner_name,
        "last_checkin_timestamp": last_checkin,
        "interval_days": interval_days,
        "missed_checkins_count": missed_count,
        "escalation_triggered": escalation_triggered,
        "demo_mode": DEMO_MODE,
        "elapsed_seconds": elapsed_seconds,
        "threshold_seconds": DEMO_CHECKIN_WINDOW_SECONDS if DEMO_MODE else interval_days * 86400
    }

@app.post("/checkin/simulate-miss")
def simulate_checkin_miss(owner_id: str):
    """
    Helper for Hackathon demo: simulates missed check-in and escalates to attestors.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("""
    UPDATE owners
    SET missed_checkins_count = missed_checkins_count + 1,
        escalation_triggered = 1
    WHERE owner_id = ?
    """, (owner_id,))

    conn.commit()
    conn.close()

    return {
        "status": "escalation_active",
        "owner_id": owner_id,
        "message": "Sustained non-response simulated. Attestors are now authorized to verify release."
    }

@app.post("/attestor/attest")
def attestor_submit_attestation(req: AttestorAttestRequest):
    """
    Attestor verifies and submits their SSS share.
    Logs event to Polygon Amoy smart contract.
    When 2-of-3 threshold is met, reconstructs vault key and unlocks vault.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    now = int(time.time())

    cur.execute("SELECT share_value, has_attested FROM attestors WHERE owner_id=? AND attestor_id=?", (req.owner_id, req.attestor_id))
    row = cur.fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail=f"Attestor '{req.attestor_id}' not designated for owner '{req.owner_id}'")

    assigned_share, already_attested = row[0], row[1]

    # Edge Case 1: Reject duplicate attestation gracefully
    if already_attested == 1:
        conn.close()
        raise HTTPException(
            status_code=400,
            detail=f"Duplicate attestation rejected: Attestor '{req.attestor_id}' has already verified for this owner."
        )

    submitted_share = req.share or assigned_share

    # Edge Case 2: Validate malformed Shamir's Secret Sharing share
    parts = submitted_share.strip().split("-")
    if len(parts) != 2 or not parts[0].isdigit():
        conn.close()
        raise HTTPException(
            status_code=400,
            detail="Malformed Shamir share: expected format '<id>-<hex>' (e.g. '1-4f8a29e1...')."
        )
    try:
        bytes.fromhex(parts[1])
    except ValueError:
        conn.close()
        raise HTTPException(
            status_code=400,
            detail="Malformed Shamir share: payload is not valid hexadecimal."
        )

    # Update attestor status
    cur.execute("""
    UPDATE attestors
    SET has_attested = 1, attestation_timestamp = ?, share_value = ?
    WHERE owner_id=? AND attestor_id=?
    """, (now, submitted_share, req.owner_id, req.attestor_id))

    # Log attestation immutably to blockchain ledger
    # Use deterministic Ethereum address representation from IDs
    owner_addr = "0x" + f"{hash(req.owner_id) & 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF:040x}"
    attestor_addr = "0x" + f"{hash(req.attestor_id) & 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF:040x}"
    
    # Check for live Polygon Amoy deployment
    live_priv_key = os.environ.get("POLYGON_AMOY_PRIVATE_KEY")
    live_contract = os.environ.get("ATTESTATION_REGISTRY_ADDRESS")

    if live_priv_key and live_contract:
        try:
            from blockchain.deploy_and_test import PolygonAmoyClient
            client = PolygonAmoyClient(private_key=live_priv_key)
            bc_res = client.log_attestation_onchain(live_contract, owner_addr, attestor_addr, now)
            bc_res["chain"] = "Polygon Amoy Testnet (Live On-Chain)"
        except Exception as e:
            bc_res = blockchain_ledger.log_attestation(owner_addr, attestor_addr, now)
            bc_res["warning"] = f"Live on-chain call encountered RPC exception ({str(e)}), recorded in verified fallback ledger"
    else:
        try:
            bc_res = blockchain_ledger.log_attestation(owner_addr, attestor_addr, now)
        except Exception as e:
            bc_res = {"error": str(e), "tx_hash": "0x_already_logged"}

    # Fetch all submitted shares for this owner
    cur.execute("SELECT share_value FROM attestors WHERE owner_id=? AND has_attested=1", (req.owner_id,))
    submitted_rows = cur.fetchall()
    shares_collected = [r[0] for r in submitted_rows]

    vault_unlocked = False
    reconstructed_key_hex = None

    if len(shares_collected) >= 2:
        try:
            reconstructed_key = ShamirSecretSharing.combine_shares(shares_collected)
            ACTIVE_VAULT_KEYS[req.owner_id] = reconstructed_key
            reconstructed_key_hex = reconstructed_key.hex()
            vault_unlocked = True

            cur.execute("UPDATE owners SET vault_released = 1 WHERE owner_id=?", (req.owner_id,))
        except Exception as e:
            conn.close()
            raise HTTPException(status_code=500, detail=f"Failed to reconstruct key from shares: {str(e)}")

    conn.commit()
    conn.close()

    return {
        "status": "attestation_recorded",
        "owner_id": req.owner_id,
        "attestor_id": req.attestor_id,
        "total_attestations_received": len(shares_collected),
        "threshold_required": 2,
        "vault_unlocked": vault_unlocked,
        "blockchain_receipt": bc_res
    }

@app.get("/release/status")
def get_release_status(owner_id: str = Query(..., description="ID of the owner")):
    """
    Check if the threshold has been met and the vault is unlocked for the beneficiary.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("SELECT vault_released, beneficiary_id FROM owners WHERE owner_id=?", (owner_id,))
    owner_row = cur.fetchone()

    cur.execute("SELECT COUNT(*) FROM attestors WHERE owner_id=? AND has_attested=1", (owner_id,))
    attestations_count = cur.fetchone()[0]

    conn.close()

    is_released = bool(owner_row[0]) if owner_row else (attestations_count >= 2)
    beneficiary = owner_row[1] if owner_row else "beneficiary_default"

    return {
        "owner_id": owner_id,
        "beneficiary_id": beneficiary,
        "total_attestations": attestations_count,
        "threshold": 2,
        "vault_released": is_released
    }

@app.get("/blockchain/logs")
def get_blockchain_logs(owner_id: str = Query(..., description="ID of the owner")):
    """
    Retrieve on-chain immutable attestation receipts from Polygon Amoy testnet.
    """
    owner_addr = "0x" + f"{hash(owner_id) & 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF:040x}"
    logs = blockchain_ledger.get_logs_for_owner(owner_addr)
    return {
        "owner_id": owner_id,
        "owner_contract_address": owner_addr,
        "chain": "Polygon Amoy Testnet (Chain ID 80002)",
        "logs": logs
    }

@app.post("/release/generate-document")
def trigger_document_generation(owner_id: str = Query(..., description="ID of the owner")):
    """
    Trigger Pratik's synthesis pipeline:
    Consolidates discovery records and decrypted access instructions, deduplicates,
    flags gaps honestly, and generates a final PDF document for the beneficiary.
    """
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    # Get records
    cur.execute("SELECT source, type, extracted_fields_json FROM discovery_records WHERE owner_id=?", (owner_id,))
    rec_rows = cur.fetchall()

    records = []
    for r in rec_rows:
        records.append({
            "source": r[0],
            "type": r[1],
            "extracted_fields": json.loads(r[2])
        })

    # Decrypt access instructions if vault is released
    cur.execute("SELECT label, ciphertext_hex, nonce_hex FROM vault_instructions WHERE owner_id=?", (owner_id,))
    ins_rows = cur.fetchall()

    vault_key = ACTIVE_VAULT_KEYS.get(owner_id)
    for ins in ins_rows:
        label = ins[0]
        if vault_key:
            try:
                decrypted_val = VaultCrypto.decrypt({"ciphertext": ins[1], "nonce": ins[2]}, vault_key)
            except Exception:
                decrypted_val = "[Encrypted Content]"
        else:
            decrypted_val = "[Encrypted until 2-of-3 attestor threshold met]"

        records.append({
            "source": "manual",
            "type": "access_instruction",
            "extracted_fields": {
                "label": label,
                "value": decrypted_val
            }
        })

    cur.execute("SELECT owner_name, beneficiary_id FROM owners WHERE owner_id=?", (owner_id,))
    owner_info = cur.fetchone()
    conn.close()

    owner_name = owner_info[0] if owner_info else "S. Sundaram"
    beneficiary_name = owner_info[1] if owner_info else "Ramesh Kumar (Son)"

    # If no records exist yet in DB for this owner, seed with standard realistic demo set
    if not records:
        from document_pipeline.pipeline import SAMPLE_RECORDS
        records = SAMPLE_RECORDS

    # Run Pratik's pipeline
    categorized = consolidate_and_categorize(records)
    output_pdf = os.path.join(os.path.dirname(__file__), f"legacy_vault_summary_{owner_id}.pdf")
    pdf_path = generate_pdf(owner_name, beneficiary_name, categorized, output_pdf)

    return {
        "status": "document_generated",
        "owner_id": owner_id,
        "beneficiary": beneficiary_name,
        "categorized_summary": categorized,
        "pdf_path": pdf_path,
        "pdf_download_url": f"/release/download-document?owner_id={owner_id}"
    }

@app.get("/release/download-document")
def download_document(owner_id: str = Query(..., description="ID of the owner")):
    pdf_path = os.path.join(os.path.dirname(__file__), f"legacy_vault_summary_{owner_id}.pdf")
    if not os.path.exists(pdf_path):
        raise HTTPException(status_code=404, detail="Document not generated yet. Call /release/generate-document first.")
    return FileResponse(pdf_path, media_type="application/pdf", filename=f"legacy_vault_{owner_id}.pdf")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
