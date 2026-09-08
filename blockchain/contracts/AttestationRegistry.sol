// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title AttestationRegistry
 * @dev Minimal, tamper-proof smart contract for Legacy Vault on Polygon Amoy Testnet.
 * Used strictly to log attestation release events immutably without ever storing
 * secrets, private documents, or personal financial details on-chain.
 */
contract AttestationRegistry {
    // Event emitted when a trusted attestor confirms release eligibility
    event AttestationLogged(
        address indexed ownerId,
        address indexed attestorId,
        uint256 timestamp,
        uint256 totalAttestationsForOwner
    );

    // Event emitted when the required threshold is reached and vault is unlocked
    event VaultReleaseUnlocked(
        address indexed ownerId,
        uint256 timestamp,
        uint256 totalAttestations
    );

    // Structure recording each attestation
    struct AttestationRecord {
        address attestorId;
        uint256 timestamp;
    }

    // Mapping from owner address => list of attestation records
    mapping(address => AttestationRecord[]) private _attestations;

    // Mapping from owner address => attestor address => already attested
    mapping(address => mapping(address => bool)) private _hasAttested;

    // Required threshold of attestations (default 2)
    uint256 public constant REQUIRED_THRESHOLD = 2;

    /**
     * @notice Log an attestation event from a designated trusted attestor for an owner.
     * @param ownerId The address/identifier of the vault owner.
     * @param attestorId The address/identifier of the attestor submitting confirmation.
     * @param timestamp The timestamp of the attestation submission.
     */
    function logAttestation(
        address ownerId,
        address attestorId,
        uint256 timestamp
    ) external returns (uint256 totalCount, bool thresholdMet) {
        require(ownerId != address(0), "Invalid owner address");
        require(attestorId != address(0), "Invalid attestor address");
        require(!_hasAttested[ownerId][attestorId], "Attestor already attested for this owner");

        _hasAttested[ownerId][attestorId] = true;
        _attestations[ownerId].push(AttestationRecord({
            attestorId: attestorId,
            timestamp: timestamp
        }));

        totalCount = _attestations[ownerId].length;

        emit AttestationLogged(
            ownerId,
            attestorId,
            timestamp,
            totalCount
        );

        if (totalCount >= REQUIRED_THRESHOLD) {
            thresholdMet = true;
            emit VaultReleaseUnlocked(ownerId, timestamp, totalCount);
        } else {
            thresholdMet = false;
        }

        return (totalCount, thresholdMet);
    }

    /**
     * @notice Get total number of attestations logged for an owner.
     * @param ownerId The address of the vault owner.
     */
    function getAttestationCount(address ownerId) external view returns (uint256) {
        return _attestations[ownerId].length;
    }

    /**
     * @notice Retrieve an attestation record by index for an owner.
     */
    function getAttestation(address ownerId, uint256 index)
        external
        view
        returns (address attestorId, uint256 timestamp)
    {
        require(index < _attestations[ownerId].length, "Index out of range");
        AttestationRecord memory rec = _attestations[ownerId][index];
        return (rec.attestorId, rec.timestamp);
    }
}
