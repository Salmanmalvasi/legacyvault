#!/usr/bin/env python3
"""
Test script for Legacy Vault Cryptography, Document Pipeline, and Backend Endpoints.
"""

import os
import sys
import secrets

sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

def test_crypto():
    print("--- 1. Testing Shamir's Secret Sharing (SSS) & AES-256 ---")
    from crypto_utils import ShamirSecretSharing, VaultCrypto

    original_key = VaultCrypto.generate_vault_key()
    assert len(original_key) == 32, "Key should be 32 bytes (256 bits)"
    print(f"[✓] Generated 256-bit AES Vault Key: {original_key.hex()[:16]}...")

    # Split into 3 shares with threshold 2
    shares = ShamirSecretSharing.split_secret(original_key, n=3, k=2)
    assert len(shares) == 3, "Expected 3 shares"
    print(f"[✓] Split into 3 shares (2-of-3 threshold):")
    for s in shares:
        print(f"    - Share ID {s.split('-')[0]}: {s.split('-')[1][:16]}...")

    # Reconstruct from shares (1, 2)
    rec_key_1_2 = ShamirSecretSharing.combine_shares([shares[0], shares[1]])
    assert rec_key_1_2 == original_key, "Reconstruction from shares (1, 2) failed!"
    print(f"[✓] Successfully reconstructed key using shares (1, 2)")

    # Reconstruct from shares (2, 3)
    rec_key_2_3 = ShamirSecretSharing.combine_shares([shares[1], shares[2]])
    assert rec_key_2_3 == original_key, "Reconstruction from shares (2, 3) failed!"
    print(f"[✓] Successfully reconstructed key using shares (2, 3)")

    # Reconstruct from shares (1, 3)
    rec_key_1_3 = ShamirSecretSharing.combine_shares([shares[0], shares[2]])
    assert rec_key_1_3 == original_key, "Reconstruction from shares (1, 3) failed!"
    print(f"[✓] Successfully reconstructed key using shares (1, 3)")

    # Test AES-256-GCM
    test_secret = "Godrej wooden wardrobe, locker key hidden behind Bhagavad Gita on shelf 2."
    encrypted = VaultCrypto.encrypt(test_secret, original_key)
    print(f"[✓] Encrypted access instruction with AES-256-GCM: {encrypted['ciphertext'][:24]}...")

    decrypted = VaultCrypto.decrypt(encrypted, rec_key_1_2)
    assert decrypted == test_secret, "Decrypted text did not match original!"
    print(f"[✓] Decrypted access instruction with reconstructed key: '{decrypted[:35]}...'")

def test_document_pipeline():
    print("\n--- 2. Testing Document Synthesis Pipeline ---")
    from document_pipeline.pipeline import run_pipeline, SAMPLE_RECORDS

    pdf_out = os.path.join(os.path.dirname(__file__), "test_legacy_summary.pdf")
    res_path = run_pipeline(SAMPLE_RECORDS, pdf_out)
    assert os.path.exists(res_path), "PDF generation output file not found!"
    print(f"[✓] Verified output document created at: {res_path} ({os.path.getsize(res_path)} bytes)")

def test_api():
    print("\n--- 3. Testing Backend Endpoints Directly ---")
    from main import (
        health_check, setup_attestors, add_discovery_entry,
        store_vault_instruction, checkin_respond, attestor_submit_attestation,
        get_blockchain_logs, trigger_document_generation,
        SetupAttestorsRequest, DiscoveryEntryRequest, VaultStoreRequest,
        CheckinRespondRequest, AttestorAttestRequest
    )

    # 1. Health check
    res = health_check()
    assert res["status"] == "healthy"
    print("[✓] GET / health check passed")

    # 2. Setup attestors
    res = setup_attestors(SetupAttestorsRequest(
        owner_id="test_owner_1",
        owner_name="S. Sundaram",
        beneficiary_id="ramesh_kumar",
        threshold=2
    ))
    shares_data = res["distributed_shares"]
    assert len(shares_data) == 3
    print(f"[✓] POST /setup/attestors created {len(shares_data)} SSS shares")

    # 3. Add discovery record
    res = add_discovery_entry(DiscoveryEntryRequest(
        owner_id="test_owner_1",
        source="ocr",
        type="bank",
        extracted_fields={
            "institution": "State Bank of India",
            "account_number": "304918239120",
            "branch": "Adyar"
        }
    ))
    assert res["status"] == "success"
    print("[✓] POST /discovery/entry ingested record")

    # 4. Store encrypted access instruction
    res = store_vault_instruction(VaultStoreRequest(
        owner_id="test_owner_1",
        label="Bank Locker Key",
        value="Behind pooja room altar in velvet pouch"
    ))
    assert res["status"] == "encrypted_and_stored"
    print("[✓] POST /vault/store encrypted and saved instruction")

    # 5. Check-in respond
    res = checkin_respond(CheckinRespondRequest(owner_id="test_owner_1"))
    assert res["status"] == "checkin_confirmed"
    print("[✓] POST /checkin/respond reset living signal")

    # 6. Attestor 1 attests
    res = attestor_submit_attestation(AttestorAttestRequest(
        owner_id="test_owner_1",
        attestor_id="attestor_1"
    ))
    assert res["vault_unlocked"] is False
    print("[✓] Attestor 1 attested (1/2 threshold, vault remains locked)")

    # 6b. Test Edge Case: Duplicate attestation from same attestor
    from fastapi import HTTPException
    duplicate_rejected = False
    try:
        attestor_submit_attestation(AttestorAttestRequest(
            owner_id="test_owner_1",
            attestor_id="attestor_1"
        ))
    except HTTPException as e:
        if e.status_code == 400:
            duplicate_rejected = True
    assert duplicate_rejected, "Duplicate attestation was not rejected with HTTP 400!"
    print("[✓] Edge Case Handled: Duplicate attestation rejected cleanly with HTTP 400")

    # 6c. Test Edge Case: Malformed SSS share format
    malformed_rejected = False
    try:
        attestor_submit_attestation(AttestorAttestRequest(
            owner_id="test_owner_1",
            attestor_id="attestor_3",
            share="not-a-valid-hex-share-xyz"
        ))
    except HTTPException as e:
        if e.status_code == 400:
            malformed_rejected = True
    assert malformed_rejected, "Malformed SSS share was not rejected with HTTP 400!"
    print("[✓] Edge Case Handled: Malformed SSS share rejected cleanly with HTTP 400")

    # 7. Attestor 2 attests -> Threshold met!
    res = attestor_submit_attestation(AttestorAttestRequest(
        owner_id="test_owner_1",
        attestor_id="attestor_2"
    ))
    assert res["vault_unlocked"] is True
    print("[✓] Attestor 2 attested (2/2 threshold met -> VAULT UNLOCKED!)")

    # 8. Blockchain logs
    res = get_blockchain_logs(owner_id="test_owner_1")
    logs = res["logs"]
    assert len(logs) >= 2
    print(f"[✓] GET /blockchain/logs retrieved {len(logs)} on-chain receipts on Polygon Amoy")

    # 9. Generate document
    res = trigger_document_generation(owner_id="test_owner_1")
    assert res["status"] == "document_generated"
    assert os.path.exists(res["pdf_path"])
    print(f"[✓] POST /release/generate-document generated synthesized PDF for beneficiary: {res['pdf_path']}")

if __name__ == "__main__":
    test_crypto()
    test_document_pipeline()
    test_api()
    print("\n[🎉 ALL BACKEND, CRYPTO & PIPELINE TESTS PASSED!]")
