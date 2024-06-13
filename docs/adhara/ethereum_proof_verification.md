# Ethereum event attestation proofs

## Introduction

This section describes how Ethereum events can be turned into EEA-compliant crosschain function calls, that is cryptographically secured by Ethereum event attestation proofs, using block headers.

An Ethereum block header consists of the following fields:

1. `parentHash`: The Keccak 256-bit hash of the parent block's header.
2. `sha3Uncles`: The Keccak 256-bit hash of the ommers list portion of this block.
3. `miner`: Miner who mined the block.
4. `stateRoot`: The Keccak 256-bit hash of the root node of the state trie.
5. `transactionsRoot`: The Keccak 256-bit hash of the root node of the transaction trie.
6. `receiptsRoot`: The Keccak 256-bit hash of the root node of the receipts trie.
7. `logsBloom`: The Bloom filter composed out of information contained in each log from the receipt of each transaction.
8. `difficulty`: A scalar value corresponding to the effort required to mine the block.
9. `number`: A scalar value equal to the number of ancestor blocks.
10. `gasLimit`: A scalar value equal to the current limit of gas expenditure per block.
11. `gasUsed`: A scalar value equal to the total gas used in transactions in this block.
12. `timestamp`: A scalar value equal to the time at which the block was mined.
13. `extraData`: An arbitrary byte array containing data relevant to this block.
14. `mixHash`: A 256-bit hash. When combined with the nonce proves that a sufficient amount of work has been carried out on this block.
15. `nonce`: A 256-bit hash. When combined with the mixHash proves that a sufficient amount of work has been carried out on this block.

The verification of an EEA-compliant Ethereum block header proof consists of:

1. verifying that the block header and block header preimage match up and hash to the block hash that was signed over.
2. verifying that the receipts root extracted from the verified block header match up with the root provided in the proof.
3. verifying a Merkle inclusion proof for the given logs in the receipts tree.
4. verifying the provided validator signatures over the block header containing the receipts tree root.
5. verifying that at least half the onboarded validators' signatures are present.

## Building the proof

The encoding format of the `encodedInfo` and `encodedProof` parameters for  block header event attestation proofs are described below.

The `encodedInfo` parameter contains the combined encoding of the destination network id, the destination contract address and the event data to be used in the proof.

The following structure is used to encode the event data:

```solidity
struct EventData {
  uint256 index;
  bytes32 signature;
  bytes logs;
}
```
The `EventData` structure contains the Ethereum event log records where:

* `index` is the index of the event in the event log.
* `signature` is the event signatures used as topic in the event log.
* `logs` is the rlp-encoded event log records as taken from an Ethereum block.

The `encodedProof` parameter contains the proof data and signatures needed to validate the event data. The following structures are used to encode this information:

```solidity
struct BlockHeaderMeta {
  bytes rlpBlockHeader;
  bytes rlpBlockHeaderPreimage;
}

struct ProofData {
  bytes witnesses;
  bytes32 root;
  bytes32 blockHash;
  bytes blockHeaderMeta;
}

struct Signature {
  uint256 by;
  uint256 sigR;
  uint256 sigS;
  uint256 sigV;
  bytes meta;
}

struct Proof {
  uint256 typ;
  ProofData proofData;
  Signature[] signatures;
}
```
The `BlockHeaderMeta` structure contains the Ethereum block header metadata where:

* `rlpBlockHeader` is the rlp-encoded block header.
* `rlpBlockHeaderPreimage` is the rlp-encoded block header preimage.

The `ProofData` structure contains the Merkle Patricia tree inclusion proof data where:

* `witnesses` is the Merkle inclusion proof witnesses.
* `root` is the block receipt root.
* `blockHash` is the block hash.
* `blockHeaderMeta` is the abi-encoded `BlockHeaderMeta` structure.

The `Signature` structure contains an EEA-compliant signature where:

* `by` is the 160-bit derived Ethereum address of the validator.
* `sigR` is the ECDSA signature's R value.
* `sigS` is the ECDSA signature's S value.
* `sigV` is the ECDSA signature's V value.
* `meta` is the signature meta data.

The `Proof` structure contains the EEA-compliant proof consisting of proof data and signatures where:

* `proofData` is the abi-encoded `ProofData` structure, e.g. witnesses, flags and values.
* `signatures` is the abi-encoded array of `Signature` structures.

## Verifying the proof

The Ethereum block header proof is verified on-chain by a Solidity contract. In alignment with the EEA DTL interoperability specification, the Solidity function that performs the verification is called `decodeAndVerify` and belongs to the `ICrosschainVerifier` interface. This interface and function is defined as follows:

```solidity
interface ICrosschainVerifier {
  function decodeAndVerify(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) external view returns (bytes memory decodedInfo);
}
```
The `decodeAndVerify` function decodes and verifies the event data according to the registered proving scheme for the given Ethereum network where:

* `networkId` is the source network identification.
* `encodedInfo` is the combined encoding of the destination network identifier, the destination contract's address and the event data.
* `encodedProof` is the information that a validating implementation can use to determine if the transaction data is valid.
* `decodedInfo` is the decoded information that needs to be acted on after verification of it by means of the provided proof, if any.

The previous section described how the `encodedInfo` and `encodedProof` parameters are constructed.

### Onboarding a source Ethereum network

The ECDSA ethereum addresses of the active validators on the source Ethereum network are onboarded as part of the setup process before proof verification on the target Ethereum network. 

### Crosschain verifier steps

The following code block shows the `verifyBFTBlockHeader` and `verifyEVMEvent` functions, belonging to the `Ethereum` library, which is called with the `EventData` struct as listed below.
```solidity
struct EventData {
  uint256 index;
  bytes32 signature;
  bytes logs;
}
function verifyBFTBlockHeader(bytes32 blockHash, bytes memory blockHeader, bytes memory blockHeaderPreImage) internal view returns (uint256 blockNumber, bytes32 receiptsRoot);
function verifyEVMEvent(Ethereum.EventData memory eventData, bytes32 root, bytes memory witnesses) internal pure returns (bool);
```
where:

* `blockHash` is extracted from `encodedProof`.
* `blockHeader` is extracted from `encodedProof`.
* `blockHeaderPreimage` is extracted from `encodedProof`.
* `eventData` is extracted from `encodedInfo`.
* `root` is extracted from `encodedProof`.
* `witnesses` is extracted from `encodedProof`.

The `verifyBFTBlockHeader` function is used to perform step 1 by comparing the header with its preimage and verifying that the correct hash is produced. The result on this function will be used by the crosschain verifier contract to perform step 2.

The `verifyEVMEvent` function is used to perform step 3 by looping through the parent nodes, each time checking that the hash of the child node is present in the parent node.

The crosschain verifier contract performs steps 4-6 in the verification process by using the appropriate elliptic curve (`SECP256K1` or `SECP256R1`) libraries and keccak hashing to verify the provided validators signatures. It makes use of the registered validator addresses stored in this contract.


