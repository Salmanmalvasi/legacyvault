#!/usr/bin/env python3
"""
verify_ui_logic_fixes.py
Comprehensive verification script for 07-prompt-ui-and-logic-fixes.md:
1. Security Model: Owner cannot attest on someone else's behalf (proxy attestation rejected).
2. Attestor can submit their own share.
3. Adding a new attestor triggers genuine SSS re-split (N -> N+1) and updates all attestor shares.
4. "Show the Math" session proof endpoint returns real session key, shares, and Polygonscan URL.
5. Reconstructed key hash matches expected key hash byte-for-byte.
"""

import sys
import os
import json
import urllib.request
import urllib.error

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

def test_prompt_07_fixes():
    print("==================================================================")
    print("  VERIFYING UI/UX & LOGIC FIXES (07-prompt-ui-and-logic-fixes)    ")
    print("==================================================================")

    # Reset demo state to clean baseline
    reset_res, code = api_call("/demo/reset", method="POST")
    assert code == 200
    print("\n[STEP 1] Reset demo state: baseline restored.")

    # 1. Login as Owner
    login_owner, code = api_call("/auth/login", method="POST", data={"email": "owner@vault.local", "password": "pass123"})
    assert code == 200, f"Owner login failed: {login_owner}"
    owner_token = login_owner["token"]
    print(f"  ✓ Logged in as Owner: {login_owner['user']['display_name']}")

    # 2. Test Security Model: Owner CANNOT submit attestation on behalf of an attestor
    print("\n[STEP 2] Testing Security Model: Owner proxy attestation bypass prevention...")
    proxy_payload = {
        "owner_id": "owner_sundaram",
        "attestor_id": "attestor_1",
        "share": "1-dummy"
    }
    proxy_res, proxy_code = api_call("/attestor/attest", method="POST", data=proxy_payload, token=owner_token)
    print(f"  Proxy attempt status code: {proxy_code} (Expected: 403 Forbidden)")
    assert proxy_code == 403, f"Owner was able to proxy-attest! Code: {proxy_code}, body: {proxy_res}"
    print(f"  ✓ PASS: Owner proxy attestation strictly REJECTED by security policy.")

    # 3. Test "Add Attestor" flow with SSS Re-Split
    print("\n[STEP 3] Testing 'Add Attestor' flow with SSS Master Key Re-Split (3 -> 4 shares)...")
    add_payload = {
        "owner_id": "owner_sundaram",
        "name": "K. Venkatesh (Brother)",
        "role": "Brother",
        "phone": "+91 98405 11223"
    }
    add_res, add_code = api_call("/attestor/add", method="POST", data=add_payload, token=owner_token)
    assert add_code == 200, f"Add attestor failed: {add_res}"
    assert add_res["total_attestors"] == 4, f"Expected 4 attestors, got {add_res['total_attestors']}"
    assert len(add_res["all_attestors"]) == 4, "Expected 4 re-split shares"
    print(f"  ✓ Added: {add_res['new_attestor_name']} (ID: {add_res['new_attestor_id']})")
    print(f"  ✓ SSS Re-split Explanation: {add_res['explanation']}")
    for att in add_res["all_attestors"]:
        print(f"    - {att['name']}: {att['assigned_share'][:24]}...")

    # 4. Test "Show the Math" Live Session Cryptographic Proof
    print("\n[STEP 4] Testing 'Show the Math' session proof endpoint...")
    proof_res, proof_code = api_call("/crypto/session-proof?owner_id=owner_sundaram")
    assert proof_code == 200, f"Session proof failed: {proof_res}"
    assert proof_res["total_shares"] == 4
    assert proof_res["shares_submitted_count"] == 0
    assert proof_res["contract_address"] == "0x892a01B93A9e97148bA95d2D48B41e9766EfF0A2"
    assert "polygonscan.com" in proof_res["polygonscan_url"]
    print(f"  ✓ Scheme: {proof_res['cryptographic_scheme']}")
    print(f"  ✓ Original Master Key Hash: {proof_res['original_master_key_hash']}")
    print(f"  ✓ Polygonscan Amoy URL: {proof_res['polygonscan_url']}")
    print(f"  ✓ Total SSS Shares tracked: {proof_res['total_shares']}")

    # 5. Attestor 1 Login and Real Attestation Submission
    print("\n[STEP 5] Authenticating as Attestor 1 (V. Krishnan) and submitting share...")
    att1_login, code = api_call("/auth/login", method="POST", data={"email": "attestor1@vault.local", "password": "pass123"})
    assert code == 200
    att1_token = att1_login["token"]

    # Get updated share for attestor 1
    updated_share_1 = add_res["all_attestors"][0]["assigned_share"]
    att_res1, code = api_call("/attestor/attest", method="POST", data={
        "owner_id": "owner_sundaram",
        "attestor_id": "attestor_1",
        "share": updated_share_1
    }, token=att1_token)
    assert code == 200
    print(f"  ✓ Attestor 1 submitted. Received: {att_res1['total_attestations_received']}/2. Unlocked: {att_res1['vault_unlocked']}")

    # 6. Attestor 2 Login and Real Attestation Submission -> Threshold Reached
    print("\n[STEP 6] Authenticating as Attestor 2 (Dr. Ananya Iyer) and submitting share...")
    att2_login, code = api_call("/auth/login", method="POST", data={"email": "attestor2@vault.local", "password": "pass123"})
    assert code == 200
    att2_token = att2_login["token"]

    updated_share_2 = add_res["all_attestors"][1]["assigned_share"]
    att_res2, code = api_call("/attestor/attest", method="POST", data={
        "owner_id": "owner_sundaram",
        "attestor_id": "attestor_2",
        "share": updated_share_2
    }, token=att2_token)
    assert code == 200
    assert att_res2["vault_unlocked"] is True
    print(f"  ✓ Attestor 2 submitted. Threshold reached (2/2) -> VAULT UNLOCKED!")
    print(f"  ✓ Reconstructed Key Hex: {att_res2['reconstructed_key_hex']}")
    print(f"  ✓ Key Hash Match Confirmed: {att_res2['crypto_verification']['key_matches_expected']}")

    # 7. Check "Show the Math" after threshold unlock
    print("\n[STEP 7] Verifying 'Show the Math' debug panel reflects threshold unlock...")
    proof_after, code = api_call("/crypto/session-proof?owner_id=owner_sundaram")
    assert code == 200
    assert proof_after["vault_released"] is True
    assert proof_after["shares_submitted_count"] == 2
    assert proof_after["hash_match_confirmed"] is True
    print(f"  ✓ 'Show the Math' reports Hash Match Confirmed: {proof_after['hash_match_confirmed']}")
    print(f"  ✓ Reconstructed Hash: {proof_after['reconstructed_key_hash']}")

    print("\n==================================================================")
    print("  [SUCCESS] ALL PROMPT 07 REQUIREMENTS VERIFIED END-TO-END!       ")
    print("==================================================================")

if __name__ == "__main__":
    test_prompt_07_fixes()
