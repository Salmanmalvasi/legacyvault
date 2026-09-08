#!/usr/bin/env python3
"""
Legacy Vault - One-Command Demo Reset Script
Author: Salman (Backend & Integration Lead)

Resets all state — SQLite DB, in-memory check-in timers, attestation counts —
back to a clean starting point and re-seeds the known-good demo dataset (SBI, LIC, EPFO).

Usage:
    python3 scripts/reset_demo.py
"""

import os
import sys
import json
import time
import sqlite3
import urllib.request
import urllib.error

WORKSPACE_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB_PATH = os.path.join(WORKSPACE_ROOT, "backend", "legacyvault.db")

sys.path.insert(0, os.path.join(WORKSPACE_ROOT, "backend"))
from crypto_utils import ShamirSecretSharing, VaultCrypto

def reset_local_sqlite(accelerated_demo: bool = True):
    """
    Directly reset SQLite database file and seed clean known-good dataset.
    """
    if os.path.exists(DB_PATH):
        try:
            os.remove(DB_PATH)
            print(f"[✓] Removed existing database: {DB_PATH}")
        except Exception as e:
            print(f"[!] Warning removing database: {e}")

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

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

    now = int(time.time())
    owner_id = "owner_sundaram"
    owner_name = "S. Sundaram (74)"
    beneficiary_id = "Ramesh Kumar (Son)"
    interval = 1 if accelerated_demo else 90

    # 1. Seed Owner
    cur.execute("""
    INSERT INTO owners (owner_id, owner_name, beneficiary_id, checkin_interval_days, last_checkin_timestamp, missed_checkins_count, escalation_triggered, vault_released)
    VALUES (?, ?, ?, ?, ?, 0, 0, 0)
    """, (owner_id, owner_name, beneficiary_id, interval, now))

    # 2. Generate Master Key & SSS 2-of-3 Shares
    vault_key = VaultCrypto.generate_vault_key()
    shares = ShamirSecretSharing.split_secret(vault_key, n=3, k=2)

    attestors_data = [
        ("attestor_1", "V. Krishnan (Family Lawyer)", shares[0]),
        ("attestor_2", "Dr. Ananya Iyer (Family Physician)", shares[1]),
        ("attestor_3", "M. Natarajan (Chartered Accountant)", shares[2])
    ]

    for att_id, name, share in attestors_data:
        cur.execute("""
        INSERT INTO attestors (attestor_id, owner_id, name, share_value, has_attested)
        VALUES (?, ?, ?, ?, 0)
        """, (att_id, owner_id, name, share))

    # 3. Seed Known-Good Financial Records
    records = [
        {
            "source": "camera_ocr",
            "type": "bank",
            "raw_text": "STATE BANK OF INDIA ADYAR BRANCH SBIN0001234 S. SUNDARAM 304918239120 NOMINEE: RAMESH KUMAR",
            "extracted_fields": {
                "institution": "State Bank of India",
                "account_type": "Senior Citizen Savings",
                "account_number": "304918239120",
                "branch": "Adyar Chennai Branch (SBIN0001234)",
                "nominee": "Ramesh Kumar (Son - 100%)"
            }
        },
        {
            "source": "camera_ocr",
            "type": "insurance",
            "raw_text": "LIFE INSURANCE CORP OF INDIA POLICY: JEEVAN ANAND S. SUNDARAM POLICY NO: 847291xxx",
            "extracted_fields": {
                "institution": "Life Insurance Corp (LIC)",
                "account_type": "Jeevan Anand Policy",
                "account_number": "847291xxx",
                "branch": "Agent: K. Balaji (Chennai Div)",
                "nominee": "Pending verification",
                "gap_note": "Policy number partially obscured on paper slip — verify at branch."
            }
        },
        {
            "source": "manual",
            "type": "epf",
            "raw_text": "DEPARTMENT OF POSTS PPF-490218-CHE S. SUNDARAM",
            "extracted_fields": {
                "institution": "EPFO & Post Office PPF",
                "account_type": "15-Yr Public Provident Fund",
                "account_number": "PPF-490218-CHE",
                "branch": "Mylapore Head Post Office",
                "nominee": "Ramesh Kumar"
            }
        }
    ]

    for r in records:
        cur.execute("""
        INSERT INTO discovery_records (owner_id, source, type, raw_text, extracted_fields_json, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """, (owner_id, r["source"], r["type"], r["raw_text"], json.dumps(r["extracted_fields"]), now))

    # 4. Seed AES-256 Encrypted Access Instructions
    instructions = [
        ("SBI Safe Deposit Locker Key", "Small brass key kept in second wooden drawer under altar in pooja room. Locker #42 at SBI Adyar."),
        ("Ancestral House Land Deeds", "Physical patta document folder is in Godrej steel almirah, inside navy leather file.")
    ]

    for label, val in instructions:
        enc = VaultCrypto.encrypt(val, vault_key)
        cur.execute("""
        INSERT INTO vault_instructions (owner_id, label, ciphertext_hex, nonce_hex, created_at)
        VALUES (?, ?, ?, ?, ?)
        """, (owner_id, label, enc["ciphertext"], enc["nonce"], now))

    conn.commit()
    conn.close()

    print("[✓] Re-seeded clean demo dataset into SQLite database:")
    print(f"    - Owner: {owner_name}")
    print(f"    - Attestors: 3 designated with Shamir 2-of-3 threshold shares")
    print(f"    - Records: 3 financial accounts (SBI, LIC with gap advisory, EPFO PPF)")
    print(f"    - Instructions: 2 access notes encrypted with AES-256-GCM")
    print(f"    - Living Signal: Active (Timestamp: {now})")

def notify_running_backend():
    """If backend is running on localhost:8000, call POST /demo/reset"""
    try:
        req = urllib.request.Request("http://localhost:8000/demo/reset", method="POST", headers={"Content-Type": "application/json"}, data=b"{}")
        with urllib.request.urlopen(req, timeout=2) as resp:
            data = json.loads(resp.read().decode())
            print(f"[✓] Notified running backend API: {data.get('message', 'Reset confirmed')}")
    except Exception:
        # Backend not currently running - SQLite reset is already complete
        pass

def main():
    print("=======================================================")
    print("      LEGACY VAULT — ONE-COMMAND DEMO RESET TOOL       ")
    print("=======================================================")
    accelerated = "--real-time" not in sys.argv
    reset_local_sqlite(accelerated_demo=accelerated)
    notify_running_backend()
    print("\n[🎉 DEMO STATE IS CLEAN AND READY FOR LIVE RUN!]")

if __name__ == "__main__":
    main()
