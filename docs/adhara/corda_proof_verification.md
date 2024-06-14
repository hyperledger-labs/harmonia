# Corda transaction attestation proofs

## Introduction 

This section describes how Corda transactions can be turned into EEA-compliant crosschain function calls that is cryptographically secured by Corda transaction attestation proofs.

The DvP implementation that makes use of Corda proofs to trade a security that is recorded on a Corda network, for cash that is recorded on a private permissioned Ethereum network, is used as example throughout this section.

A transaction is drafted to earmark a security on the Corda network and signatures of all parties involved are collected before the transaction is notarized and finalized on the Corda network. The crosschain interop service will receive a settlement instruction, which includes the encoded wire transaction and signatures, as taken from the Corda network. It constructs a Corda transaction attestation proof from the transaction, to be able to perform a crosschain function call to an Ethereum network. The Ethereum network will receive an attestation proof of the transaction, including the signatures of all parties involved in the transaction, as well as details of the function to call once the proof is verified.

It is important to note that this proving methodology does not achieve the same level of trust achieved by attestation proofs on an Ethereum network. Corda notaries are not necessarily validating notaries. As a result, the Corda proofs used in this PoC are not providing proof that a transaction is valid, only that it occurred and was signed by the parties involved as well as a notary. Whereas Ethereum proofs provide proof that a transaction is valid and that it occurred in a block that was signed by active validators.

To mitigate this, it is required and trusted that the receiver of the traded securities on the Corda network has fulfilled the following Corda transaction validation requirements before signing the transaction during the CorDapp flows:
  - The full Corda transaction history (full Merkle tree) has been validated.
  - The Corda contract code has been executed to verify the contractual validity of the transaction.

The verification of an EEA-compliant Corda proof consists of:

    1. verifying that the contents of the commands component group match up with the provided Ethereum crosschain function that is being called.
    2. verifying that the contents of the outputs component group match up with the provided Ethereum crosschain function call parameters.
    3. verifying that the contents of the signers component group match up with the provided ECDSA/EDDSA signatures.
    4. verifying that the contents of the notary component group match up with the provided ECDSA/EDDSA signatures.
    5. verifying a multivalued Merkle inclusion proof for the given component groups.
    6. verifying that the provided signatures are over the calculated Merkle tree root.
    7. verifying the provided signatures for onboarded participants and notaries.
    8. verifying that at least 1 onboarded notary signature is present.
    9. verifying that at least 2 onboarded participant signatures are present.

## Building the proof

AMQP/1.0 deserialization is the process of reconstructing an object from an AMQP/1.0 serialized byte array. It is required to build the cryptographically secure crosschain function call and to perform proof verification in Solidity contracts.

The Corda AMQP serialization format uses the concept of a fingerprint to uniquely identify objects serialized in a proton graph. This means that information can be extracted from Corda transaction component groups by using known fingerprints of a Corda wire transaction. 

The transaction elements required for the attestation proof include the following:

- The salt used for computing the nonce.
- The hash algorithm used in the Merkle tree.
- The wire transaction's AMQP/1.0 serialized component groups with their respective group indices.
- The list of signatures with public keys and metadata.

An extendable list of component groups used in Corda transactions are shown in the table. The ordinal column shows the fixed group index used to calculate the component group hashes.

| Component Group | Ordinal |
|-----------------|---------|
| INPUTS          | 0       |
| OUTPUTS         | 1       |
| COMMANDS        | 2       | 
| ATTACHMENTS     | 3       |
| NOTARY          | 4       |
| TIMEWINDOW      | 5       |
| SIGNERS         | 6       |
| REFERENCES      | 7       |
| PARAMETERS      | 8       |

The Merkle tree constructed from a Corda transaction contains the calculated hashes (`hash`) of its component groups as leaves. It is required to be able to build the Merkle tree when generating the Corda proof.

The hash of each serialized component group, to be used as a Merkle tree leaf, is computed using the following equation:
```
hash = HASH(HASH(HASH(HASH(nonce || serialized)) || serialized))
```
where:

* `HASH` equals the `SHA-256` hash function and a double `SHA-256` is used to prevent length extension attacks.
* `||` denotes concatenation.

The method to compute a nonce is based on the provided salt, the component group's fixed index (or ordinal) and the
component's internal index inside it's parent group. It is computed using the following equation:
```
nonce = HASH(HASH(salt || group index || internal index))
```
where:

* `HASH` equals the `SHA-256` hash function.
* `||` denotes concatenation.

The following two structures are used to encode the transaction data, required to construct the `encodedInfo` input to a crosschain function call:

```solidity
struct ComponentData {
  uint8 groupIndex;
  uint8 internalIndex;
  bytes encodedBytes;
}

struct EventData {
  bytes callParameters;
  string hashAlgorithm;
  bytes32 privacySalt;
  ComponentData[] componentData;
}
```
The `ComponentData` structure contains the Corda transaction component group data, that needs to be decoded, where:

* `groupIndex` is the global component group index.
* `internalIndex` is the internal component group index.
* `encodedBytes` contains the hex-encoded component group of the Corda transaction.

The `EventData` structure contains the Ethereum function input parameters, Corda transaction component group data and algorithmic details to verify the component group hash's inclusion in the transaction tree, where:

* `callParameters` are the parameters of the function we want to call through the interop service.
* `hashAlgorithm` is the hash algorithm used in the Merkle tree. Only SHA-256 is currently supported.
* `privacySalt` is the salt needed to compute a Merkle tree leaf.
* `componentData` is the abi-encoded array of `ComponentGroup` structures to produce the hashes of components that we want to proof Merkle tree membership of.

The `encodedProof` input, required to do a crosschain function call, contains the proof data and signatures needed to validate the transaction data. The following structures are used to encode this information:

```soldiity
struct ProofData {
  bytes32 root;
  bytes32[] witnesses;
  uint8[] flags;
  bytes32[] values;
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
  ProofData proof;
  Signature[] signatures;
}
```
The `ProofData` structure contains the Merkle tree inclusion proof data where:

* `root` is the Merkle tree root.
* `witnesses` is the Merkle multivalued proof's witnesses.
* `flags` is the Merkle multivalued proof's flags.
* `values` is the Merkle multivalued proof's leaves.

The `Signature` structure contains an EEA-compliant signature where:

* `by` is the 256-bit ECDSA public key or the 256-bit ED25519 public key of consensus participant.
* `sigR` is the ECDSA/EDDSA signature's R value.
* `sigS` is the ECDSA/EDDSA signature's S value.
* `sigV` is the ECDSA parity bit.
* `meta` is the signature meta data.

The `Proof` structure contains the EEA-compliant proof consisting of proof data and signatures where:

* `proofData` is the abi-encoded `ProofData` structure, e.g. witnesses, flags and values.
* `signatures` is the abi-encoded array of `Signature` structures.

## Verifying the proof

The Corda transaction attestation proof is verified on-chain by a Solidity contract. In alignment with the EEA DTL interoperability specification. The Solidity function that performs the verification is called `decodeAndVerify` and belongs to the `ICrosschainVerifier` interface. This interface and function is defined as follows:

```solidity
interface ICrosschainVerifier {
  function decodeAndVerify(
    uint256 networkId,
    bytes calldata encodedInfo,
    bytes calldata signatureOrProof
  ) external view returns (bytes memory decodedInfo);
}
```
The `decodeAndVerify` function decodes and verifies the transaction according to the registered proving scheme for the given Corda network where:

* `networkId` is the source network identification.
* `encodedInfo` is the combined encoding of the destination network identifier, the destination contract's address and the transaction data.
* `encodedProof` is the information that a validating implementation can use to determine if the transaction data is valid.
* `decodedInfo` is the decoded information that needs to be acted on after verification of it by means of the provided proof, if any.

The previous section described how the `encodedInfo` and `encodedProof` parameters are constructed from a Corda transaction.

### Onboarding a source Corda network

The ECDSA/EDDSA public keys of the trusted parties on the Corda network are onboarded as part of the setup process before proof verification on the Ethereum network. Examples of such parties are those involved in the trade of the securities, possibly a custodian or beneficiary, and a notary.

The crosschain verifier contract requires the transaction to be signed by all the required signers and the designated notary for the transaction. It furthermore requires that at least two required signers and the notary are onboarded parties.

Signed Corda transactions contain signatures over the original transaction root or a partial tree root. The signature scheme used for a particular signature is contained in the metadata of the signature. There are three supported schemes, namely using the SECP256K1 or SECP256R1 curve with SHA-256 hashing, or the ED25519 curve with SHA-512 hashing.

For every crosschain function call, that uses a Corda transaction attestation proof, the command to trigger a crosschain function call needs to be registered and maintained. For the DvP implementation, Corda proofs for transactions with commands that puts a security on hold and cancels a trade, is used.

Configuration needed for verification, that is registered during onboarding of a network, are stored in the crosschain verifier contract. It uses the storage structures listed below:

```solidity
contract CrosschainMessaging is ICrosschainVerifier {
  mapping(uint256 => mapping(uint256 => bool)) activeNotaries;
  mapping(uint256 => mapping(uint256 => bool)) activeParticipants;
  mapping(uint256 => mapping(uint32 => Corda.ParameterHandler[])) functionParameterHandlers;
  mapping(uint256 => mapping(uint32 => string)) functionPrototypes;
  mapping(uint256 => mapping(uint32 => string)) functionCommands;
}
```
where

* `activeNotaries` contains Corda notaries that are authenticated as signers indexed by network.
* `activeParticipants` contains the Corda participants that are authenticated as signers indexed by network.
* `functionParameterHandlers` contains Corda parameter handlers, indexed by network and function signature.
* `functionPrototypes` contains the Ethereum function prototype, indexed by network and function signature.
* `functionCommands` contains a Corda command, indexed by network and function signature.

The Corda parameter handlers are used when extracting values from a Corda transaction component group. They are defined for every parameter of a function that is callable via the crosschain interop protocol. They are stored using the following structure:
```solidity
struct ParameterHandler {
  string fingerprint;
  uint8 componentIndex;
  uint8 describedSize;
  string describedType;
  bytes describedPath;
  string solidityType;
  bytes calldataPath;
  string parser;
}
```
where:

* `fingerprint` is the fingerprint used when extracting data from the Corda component.
* `componentIndex` is the index in list of extracted items for this fingerprint.
* `describedSize` is the size of the structure that was extract under this fingerprint.
* `describedType` is the extracted AMQP type.
* `describedPath` is the path to walk for nested objects in extracted items.
* `solidityType` is the Solidity type of the extracted value.
* `calldataPath` is the path to walk for nested objects in Ethereum calldata.
* `parser` is the parser used to parse the extracted element.

For every crosschain function call, that uses a Corda transaction attestation proof, the required command to trigger the crosschain call, the function prototype and the parameter handlers, need to be registered and maintained. The crosschain verifier contract needs these parser definitions to extract the correct data, by fingerprint, when validating the crosschain function call input.

### Crosschain verifier steps

The following code block shows the `validateEvent` function, belonging to the `Corda` library, which is called with the `ValidationData` struct as listed below.
```solidity
struct ValidationData {
  Corda.EventData eventData;
  string functionPrototype;
  string functionCommand;
  Corda.ParameterHandler[] handlers;
  Corda.ProofData proofData;
  Corda.Signature[] signatures;
}
function validateEvent(ValidationData memory vd) internal view returns (bool)
```
where:

* `eventData` is extracted from `encodedInfo`.
* `functionPrototype` is the registered prototype for a specific function that can be called via the crosschain protocol.
* `functionCommand` is the registered command for a specific function that can be called via the crosschain protocol.
* `handlers` is the registered handlers for a specific function that can be called via the crosschain protocol.
* `proofData` is extracted from `encodedProof`.
* `signatures` is extracted from `encodedProof`.

The `validateEvent` function takes as input, the contents of the `encodedInfo` and `encodedProof` structures, as well as the registered information stored in the crosschain verifier contract. It validates an event by performing steps 1-6, to verify an EEA-compliant Corda proof, as listed in the introduction. The `Parser` library handles Corda deserialization and extraction of data based on fingerprints and filters. The outputs, commands, signers and notary component groups are AMQP-encoded, which means that they need to be decoded and hashed again on-chain. The `Parser` library uses the `AMQP` library for building the AMQP proton graph. The `X509` library is used to decode a DER-encoded X.509 public key as extracted from the signers and notary groups.

The `Merkle` library is used to verify the multivalued Merkle proof. The verification process rebuilds the root hash by traversing the tree up from the leaves. The root is calculated by consuming either a leaf value from the values of the proof, or a witness from the witnesses of the proof, or a previously calculated value off the stack, and producing hashes from them. At the end of the process, the last hash will contain the root of the Merkle tree. Finally, the calculated root are verified against the root that was signed over.

The crosschain verifier contract performs steps 7-9 in the verification process by using the appropriate elliptic curve (`SECP256K1`, `SECP256R1`, `ED25519`) and hashing (`SHA512`)libraries to verify the provided signatures. It makes use of the registered public keys stored in this contract.


