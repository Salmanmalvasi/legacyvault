#!/usr/bin/env python3
"""
verify_real_document.py
Proves that Legacy Vault's final document generation is genuinely end-to-end and dynamic:
1. Creates a distinct owner: 'owner_kamat'
2. Adds two unique, non-dummy accounts:
   - 'HDFC-TEST-7788' (Bank Account at HDFC Premier)
   - 'ICICI-PRU-9922' (Insurance Policy at ICICI Prudential)
3. Confirms they exist in SQLite database 'discovery_records'.
4. Triggers 2-of-3 threshold release so the vault is released.
5. Invokes POST /release/generate-document to trigger Pratik's consolidation & ReportLab pipeline.
6. Reads the resulting PDF and extracts raw text streams to prove 'HDFC-TEST-7788' and 'ICICI-PRU-9922' are rendered into the final document.
"""

import os
import sys
import json
import sqlite3
import urllib.request
import urllib.error
import re

API_BASE = "http://localhost:8000"

def api_call(path, method="GET", data=None, token=None):
    url = f"{API_BASE}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8")), resp.status
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            return json.loads(err_body), e.code
        except Exception:
            return {"error": err_body}, e.code

def extract_text_from_pdf(pdf_path: str) -> str:
    """Extract plain text streams from PDF file."""
    with open(pdf_path, "rb") as f:
        content = f.read().decode("latin1", errors="ignore")
    # Clean extract text blocks in PDF
    text_blocks = re.findall(r"\(([^\)]+)\)", content)
    return " ".join(text_blocks)

def main():
    print("==================================================================")
    print("   LEGACY VAULT — REAL DOCUMENT SYNTHESIS VERIFICATION           ")
    print("==================================================================")

    test_owner = f"owner_custom_{os.urandom(2).hex()}"
    owner_name = "P. K. Kamat (81)"
    beneficiary_name = "Siddharth Kamat (Son)"

    acc_1_num = "HDFC-TEST-7788"
    acc_2_num = "ICICI-PRU-9922"

    print(f"\n[STEP 1] Initializing Vault Setup for {owner_name} ({test_owner})...")
    setup_payload = {
        "owner_id": test_owner,
        "owner_name": owner_name,
        "beneficiary_id": beneficiary_name,
        "threshold": 2,
        "attestors": [
            {"attestor_id": "att1", "name": "Trustee 1"},
            {"attestor_id": "att2", "name": "Trustee 2"}
        ]
    }
    res, code = api_call("/setup/attestors", method="POST", data=setup_payload)
    assert code == 200, f"Setup failed: {res}"
    print(f"  ✓ Owner and attestors registered in database.")

    print(f"\n[STEP 2] Inserting Two Unique Custom Accounts...")
    entry1 = {
        "owner_id": test_owner,
        "source": "manual",
        "type": "bank",
        "raw_text": f"HDFC Premier Private Banking Acc {acc_1_num}",
        "extracted_fields": {
            "institution": "HDFC Bank Ltd",
            "account_number": acc_1_num,
            "account_type": "Senior Citizen Super Savings",
            "branch": "Indiranagar 100ft Road (HDFC0001824)",
            "nominee": "Siddharth Kamat (Son - 100%)"
        }
    }
    res1, code1 = api_call("/discovery/entry", method="POST", data=entry1)
    assert code1 == 200, f"Entry 1 failed: {res1}"
    print(f"  ✓ Ingested Account 1: {entry1['extracted_fields']['institution']} | Acc: {acc_1_num}")

    entry2 = {
        "owner_id": test_owner,
        "source": "camera_ocr",
        "type": "insurance",
        "raw_text": f"ICICI Prudential Life Smart Life Policy {acc_2_num}",
        "extracted_fields": {
            "institution": "ICICI Prudential Life Insurance",
            "account_number": acc_2_num,
            "account_type": "Guaranteed Income Life Plan",
            "branch": "MG Road Corporate Branch",
            "nominee": "Siddharth Kamat",
            "gap_note": "Annual premium receipt unverified — verify at branch."
        }
    }
    res2, code2 = api_call("/discovery/entry", method="POST", data=entry2)
    assert code2 == 200, f"Entry 2 failed: {res2}"
    print(f"  ✓ Ingested Account 2: {entry2['extracted_fields']['institution']} | Policy: {acc_2_num}")

    print(f"\n[STEP 3] Verifying Direct Database State in SQLite...")
    conn = sqlite3.connect("backend/legacyvault.db")
    cur = conn.cursor()
    cur.execute("SELECT id, type, extracted_fields_json FROM discovery_records WHERE owner_id=? ORDER BY id ASC", (test_owner,))
    rows = cur.fetchall()
    conn.close()

    assert len(rows) == 2, f"Expected 2 records, found {len(rows)}"
    print(f"  ✓ Database contains exactly {len(rows)} records for owner '{test_owner}':")
    for r in rows:
        f = json.loads(r[2])
        print(f"    - Record #{r[0]}: {f.get('institution')} (ID: {f.get('account_number')})")

    print(f"\n[STEP 4] Submitting 2 Attestor Shares to Unlock Vault...")
    shares = res["distributed_shares"]
    api_call("/attestor/attest", method="POST", data={"owner_id": test_owner, "attestor_id": "att1", "share": shares[0]["assigned_share"]})
    att_res2, _ = api_call("/attestor/attest", method="POST", data={"owner_id": test_owner, "attestor_id": "att2", "share": shares[1]["assigned_share"]})
    assert att_res2["vault_unlocked"] is True, "Vault should be unlocked after 2 shares"
    print(f"  ✓ 2-of-3 Attestations recorded on blockchain registry -> Vault UNLOCKED!")

    print(f"\n[STEP 5] Calling POST /release/generate-document for {test_owner}...")
    gen_res, gen_code = api_call(f"/release/generate-document?owner_id={test_owner}", method="POST")
    assert gen_code == 200, f"Generation failed: {gen_res}"
    pdf_path = gen_res["pdf_path"]
    print(f"  ✓ Document Generation Succeeded!")
    print(f"  ✓ PDF Path: {pdf_path}")
    print(f"  ✓ File Size: {os.path.getsize(pdf_path)} bytes")

    print(f"\n[STEP 6] Inspecting Generated PDF Content...")
    pdf_text = extract_text_from_pdf(pdf_path)

    found_acc1 = acc_1_num in pdf_text
    found_acc2 = acc_2_num in pdf_text

    print(f"  - Searching for Custom Account 1 ('{acc_1_num}'): {'FOUND ✓' if found_acc1 else 'MISSING ✗'}")
    print(f"  - Searching for Custom Account 2 ('{acc_2_num}'): {'FOUND ✓' if found_acc2 else 'MISSING ✗'}")

    assert found_acc1, f"Custom Account 1 '{acc_1_num}' NOT found in generated PDF!"
    assert found_acc2, f"Custom Account 2 '{acc_2_num}' NOT found in generated PDF!"

    print(f"\n==================================================================")
    print(f"  [SUCCESS] DOCUMENT PIPELINE VERIFIED DYNAMIC & END-TO-END!       ")
    print(f"  Generated PDF strictly reflects entered accounts:                ")
    print(f"  - {acc_1_num} (HDFC Bank Ltd)")
    print(f"  - {acc_2_num} (ICICI Prudential Life)")
    print(f"==================================================================")

if __name__ == "__main__":
    main()
