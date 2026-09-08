"""
Legacy Vault - Cryptography Utilities
Author: Salman (Backend & Cryptography Lead)

Includes:
1. Shamir's Secret Sharing (SSS) over Galois Field GF(2^8)
   - Splits a 256-bit AES vault encryption key into N shares with threshold K.
   - Any K shares can reconstruct the exact secret; fewer than K reveal zero information.
2. AES-256-GCM Encryption / Decryption
   - Standard authenticated symmetric encryption for stored vault records and access instructions.
"""

import os
import secrets
from typing import List, Tuple
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

# --- GF(2^8) Arithmetic for Shamir's Secret Sharing ---
# Irreducible primitive polynomial 0x11d: x^8 + x^4 + x^3 + x^2 + 1 with generator 2
GF_EXP = [0] * 512
GF_LOG = [0] * 256

def _init_gf_tables():
    x = 1
    for i in range(255):
        GF_EXP[i] = x
        GF_LOG[x] = i
        x <<= 1
        if x & 0x100:
            x ^= 0x11d
    for i in range(255, 512):
        GF_EXP[i] = GF_EXP[i - 255]

_init_gf_tables()

def _gf_add(a: int, b: int) -> int:
    return a ^ b

def _gf_sub(a: int, b: int) -> int:
    return a ^ b

def _gf_mul(a: int, b: int) -> int:
    if a == 0 or b == 0:
        return 0
    return GF_EXP[GF_LOG[a] + GF_LOG[b]]

def _gf_div(a: int, b: int) -> int:
    if b == 0:
        raise ZeroDivisionError("GF(2^8) division by zero")
    if a == 0:
        return 0
    return GF_EXP[(GF_LOG[a] - GF_LOG[b] + 255) % 255]

def _eval_poly(coeffs: List[int], x: int) -> int:
    """Evaluate polynomial with coefficients coeffs at point x in GF(2^8)."""
    # coeffs[0] is constant term (the secret byte)
    result = 0
    x_pow = 1
    for c in coeffs:
        result = _gf_add(result, _gf_mul(c, x_pow))
        x_pow = _gf_mul(x_pow, x)
    return result

class ShamirSecretSharing:
    """
    Shamir's Secret Sharing (k, n) implementation over GF(2^8).
    Splits any byte sequence into n shares such that any k shares reconstruct it.
    Share format: "<x_int>-<hex_encoded_y_bytes>"
    """

    @staticmethod
    def split_secret(secret_bytes: bytes, n: int = 3, k: int = 2) -> List[str]:
        if k > n:
            raise ValueError("Threshold k cannot exceed number of shares n")
        if n > 254:
            raise ValueError("Maximum 254 shares supported in GF(2^8)")

        shares_y = [bytearray() for _ in range(n)]
        # Use x values 1, 2, ..., n
        x_coords = list(range(1, n + 1))

        for b in secret_bytes:
            # Random polynomial of degree k-1: coeffs = [b, r1, r2, ... r_(k-1)]
            coeffs = [b] + [secrets.randbelow(256) for _ in range(k - 1)]
            for i, x in enumerate(x_coords):
                y = _eval_poly(coeffs, x)
                shares_y[i].append(y)

        # Encode shares: e.g. "1-a4f029..."
        formatted_shares = [f"{x}-{bytes(shares_y[idx]).hex()}" for idx, x in enumerate(x_coords)]
        return formatted_shares

    @staticmethod
    def combine_shares(shares: List[str]) -> bytes:
        """
        Reconstruct secret from at least k shares using Lagrange interpolation.
        """
        if not shares:
            raise ValueError("No shares provided for reconstruction")

        parsed_shares: List[Tuple[int, bytes]] = []
        for s in shares:
            parts = s.strip().split("-")
            if len(parts) != 2:
                raise ValueError(f"Invalid share format: {s}")
            x = int(parts[0])
            y_bytes = bytes.fromhex(parts[1])
            parsed_shares.append((x, y_bytes))

        # Validate consistent length
        secret_len = len(parsed_shares[0][1])
        for _, y in parsed_shares:
            if len(y) != secret_len:
                raise ValueError("Inconsistent share lengths")

        # Lagrange interpolation at x = 0
        reconstructed = bytearray()
        for byte_idx in range(secret_len):
            secret_byte = 0
            for j, (xj, yj_bytes) in enumerate(parsed_shares):
                yj = yj_bytes[byte_idx]
                # Lagrange basis polynomial L_j(0) = product_{m != j} (0 - x_m) / (x_j - x_m)
                basis = 1
                for m, (xm, _) in enumerate(parsed_shares):
                    if m == j:
                        continue
                    num = xm  # 0 - xm in GF(2^8) is xm
                    denom = _gf_sub(xj, xm)
                    basis = _gf_mul(basis, _gf_div(num, denom))
                secret_byte = _gf_add(secret_byte, _gf_mul(yj, basis))
            reconstructed.append(secret_byte)

        return bytes(reconstructed)

# --- AES-256 Vault Encryption ---

class VaultCrypto:
    """
    AES-256-GCM authenticated encryption for sensitive vault records.
    """

    @staticmethod
    def generate_vault_key() -> bytes:
        """Generate a random 256-bit (32-byte) master encryption key."""
        return AESGCM.generate_key(bit_length=256)

    @staticmethod
    def encrypt(plaintext: str, key: bytes) -> dict:
        """
        Encrypt plaintext string using AES-256-GCM.
        Returns dict with ciphertext and nonce (hex encoded).
        """
        aesgcm = AESGCM(key)
        nonce = os.urandom(12)  # Standard 96-bit nonce for GCM
        data = plaintext.encode("utf-8")
        ciphertext = aesgcm.encrypt(nonce, data, None)
        return {
            "ciphertext": ciphertext.hex(),
            "nonce": nonce.hex()
        }

    @staticmethod
    def decrypt(encrypted_payload: dict, key: bytes) -> str:
        """
        Decrypt AES-256-GCM payload.
        """
        aesgcm = AESGCM(key)
        nonce = bytes.fromhex(encrypted_payload["nonce"])
        ciphertext = bytes.fromhex(encrypted_payload["ciphertext"])
        decrypted = aesgcm.decrypt(nonce, ciphertext, None)
        return decrypted.decode("utf-8")
