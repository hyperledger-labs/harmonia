
/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;

import "contracts/libraries/X509.sol";
import "contracts/libraries/Base64.sol";
import "contracts/libraries/Merkle.sol";
import "contracts/libraries/SolidityUtils.sol";

/*
 * Library to handle Corda proof verification.
 */
library Corda {
  /*
   * Structure to hold parameter handler information.
   * A parameter handler is registered for each input parameter of the smart contract function you want to invoke remotely.
   * Their information are used in extracting data from Corda serializations without the need of parsing large schemas in the EVM.
   * @property {string} fingerprint Fingerprint to use when extracting data from the Corda component.
   * @property {uint8} componentIndex Index in list of extracted items for this fingerprint.
   * @property {uint8} describedSize Size of the structure that was extracted under this fingerprint.
   * @property {string} describedType Extracted AMQP type.
   * @property {bytes} describedPath Path to walk for nested objects in extracted items.
   * @property {string} solidityType Solidity type to which the parameter value must be extracted from the corda component data.
   * @property {bytes} calldata Path Path to walk for nested objects in calldata.
   * @property {string} parser Parser to use on extracted element.
   */
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

  /*
   * Structure to hold Corda transaction component group data that needs to be decoded.
   * @property {uint8} groupIndex Global component group index.
   * @property {uint8} internalIndex Internal component group index.
   * @property {bytes} encodedBytes Contains a hex-encoded component group of the Corda transaction.
   */
  struct ComponentData {
    uint8 groupIndex;
    uint8 internalIndex;
    bytes encodedBytes;
  }

  /*
   * Structure to hold Merkle multi-valued proof data.
   * @property {bytes32} root Merkle tree root
   * @property {bytes32[]} witnesses Merkle multi-proof witnesses
   * @property {uint8[]} flags Merkle multi-proof flags
   * @property {bytes32[]} values Merkle multi-proof leaves
   */
  struct ProofData {
    bytes32 root;
    bytes32[] witnesses;
    uint8[] flags;
    bytes32[] values;
  }

  /*
   * Contains the Ethereum function input parameters, Corda component group data and algorithmic details to verify the component group hash's inclusion in the transaction tree.
   * @property {bytes} callParameters Parameters of the function we want to call through the interop service.
   * @property {string} hashAlgorithm Hash algorithm used in the Merkle tree. Only SHA-256 is currently supported.
   * @property {bytes32} privacySalt Salt needed to compute a Merkle tree leaf.
   * @property {ComponentData[]} componentData Hashes of these components become the values we want to proof Merkle tree membership of in our multi-valued proof.
   */
  struct EventData {
    bytes callParameters;
    string hashAlgorithm;
    bytes32 privacySalt;
    ComponentData[] componentData;
  }

  /*
   * Structure to hold a Corda transaction signature.
   * @property {uint256} by The 160-bit Ethereum address or the 256-bit ED25519 public key.
   * @property {uint256} sigR The ECDSA/EDDSA signature's R value.
   * @property {uint256} sigS The ECDSA/EDDSA signature's S value.
   * @property {uint256} sigV The ECDSA signature's V value.
   * @property {bytes} meta Signature meta data. Contains platform version, schema number and partial merkle tree root that was signed.
   */
  struct Signature {
    uint256 by;
    uint256 sigR;
    uint256 sigS;
    uint256 sigV;
    bytes meta;
  }

  /*
   * Structure to hold Corda proof data and signatures.
   * @property {ProofData} proof The data contained in the proof, e.g. witnesses, flags and values or just a root when used for trade verification.
   * @property {Signature[]} signatures The array of signatures.
   */
  struct Proof {
    uint256 typ;
    ProofData proof;
    Signature[] signatures;
  }

  /*
   * Structure to hold validation data to prove that the extracted data was signed over and that the extracted parameters match the remote function input parameters.
   * @param {Corda.EventData} eventData Contains remote function input parameters and component group data.
   * @param {string} functionPrototype Contains remote function input parameters and component group data.
   * @param {string} functionCommand Contains remote function input parameters and component group data.
   * @param {Corda.ParameterHandler[]} handlers The parameter handlers used to extract data from the Corda component group.
   * @param {Corda.ProofData} proofData Data needed to prove component group membership in the transaction tree, which root was signed over.
   * @param {Corda.Signature[]} signatures Signatures over the transaction tree root.
   * @return {bool} Returns true if the Corda was successfully validated.
   */
  struct ValidationData {
    Corda.EventData eventData;
    string functionPrototype;
    string functionCommand;
    Corda.ParameterHandler[] handlers;
    Corda.ProofData proofData;
    Corda.Signature[] signatures;
  }

  using Parser for Parser.Parsed;
  using Object for Object.Obj;

  /* Parser definitions. */
  uint32 private constant parserPath = uint32(bytes4(keccak256(bytes("PathParser"))));
  uint32 private constant parserParty = uint32(bytes4(keccak256(bytes("PartyParser"))));
  uint32 private constant parserCommand = uint32(bytes4(keccak256(bytes("CommandParser"))));
  uint32 private constant parserNone = uint32(bytes4(keccak256(bytes("NoParser"))));

  /* Component group indices and count */
  uint8 private constant componentGroupIndexOutputs = 1;
  uint8 private constant componentGroupIndexCommands = 2;
  uint8 private constant componentGroupIndexNotary = 4;
  uint8 private constant componentGroupIndexSigners = 6;
  uint8 private constant componentGroupLimit = 12;  // Max number of component groups in a transaction
  uint8 private constant componentGroupCount = 12;  // Max number of components in a group

  /* Command group limit */
  uint8 private constant commandsGroupLimit = 10; // Max number of commands tag in a group. This limit will never be reached as there are only one command per component group.

  /*
   * Validate an event by proving that the extracted data was signed over and that the extracted parameters match the remote function input parameters.
   * @param {ValidationData} vd Data to use in the validation.
   * @return {bool} Returns true if the Corda event was successfully validated.
   */
  function validateEvent(
    ValidationData memory vd
  ) internal view returns (bool) {
    uint8[] memory componentCounts = new uint8[](componentGroupCount);
    bytes32[componentGroupLimit][] memory hashesToCheck = new bytes32[componentGroupLimit][](componentGroupCount);
    bytes1 outputIsPresent = 0x00;
    bytes1 commandIsPresent = 0x00;
    for (uint c = 0; c < vd.eventData.componentData.length; c++) {
      uint8 groupIndex = vd.eventData.componentData[c].groupIndex;
      if (groupIndex == componentGroupIndexOutputs) {
        // Parsing of only the first outputs component group is currently supported.
        if (outputIsPresent != 0x01) {
          outputIsPresent = 0x01;
          // Check that extracted parameters match function call parameters in event data.
          Object.Obj[] memory parsed = extractByFingerprint(vd.eventData.componentData[c].encodedBytes, vd.handlers);
          Object.Obj[] memory parameters = extractParameters(vd.eventData.callParameters, vd.functionPrototype, vd.handlers);
          for (uint256 i = 0; i < vd.handlers.length; i++) {
            if (keccak256(abi.encodePacked(vd.handlers[i].fingerprint)) != keccak256(abi.encodePacked("")) && !parameters[i].isEqual(parsed[i])) {
              if (parameters[i].selector != parsed[i].selector) {
                if (!parsed[i].convertTo(parameters[i].selector)) {
                  revert(string(abi.encodePacked(
                    "Failed to convert parsed object at index=[",
                    SolUtils.UIntToString(i),
                    "] with selector=[",
                    SolUtils.Bytes4ToHexString(bytes4(parsed[i].selector)),
                    "] into object with selector=[",
                    SolUtils.Bytes4ToHexString(bytes4(parameters[i].selector)),
                    "]."
                  )));
                }
              }
              if (!parameters[i].isEqual(parsed[i])) {
                revert(string(abi.encodePacked(
                  "Parsed object at index=[",
                  SolUtils.UIntToString(i),
                  "] with selector=[",
                  SolUtils.Bytes4ToHexString(bytes4(parsed[i].selector)),
                  "] is not equal to given object with selector=[",
                  SolUtils.Bytes4ToHexString(bytes4(parameters[i].selector)),
                  "]."
                )));
              }
            }
          }
        }
      } else if (groupIndex == componentGroupIndexCommands) {
        string[] memory parsed = extractCommands(vd.eventData.componentData[c].encodedBytes);
        require(parsed.length > 0, string(abi.encodePacked(
          "Failed to extract commands from component group."
        )));
        for (uint i=0; i<parsed.length; i++) {
          if (keccak256(abi.encodePacked(parsed[i])) == keccak256(abi.encodePacked(vd.functionCommand))) {
            commandIsPresent = 0x01;
            break;
          }
        }
      } else if (groupIndex == componentGroupIndexNotary) {
        // Check that the required notary signature is present in the proof data.
        string[] memory parsed = extractPublicKeys(vd.eventData.componentData[c].encodedBytes);
        uint256 notarySignatures = 0;
        for (uint i = 0; i < parsed.length; i++) {
          bytes memory key = X509.decodeKey(parsed[i]);
          for (uint s = 0; s < vd.signatures.length; s++) {
            if (keccak256(abi.encodePacked(vd.signatures[s].by)) == keccak256(abi.encodePacked(key))) {
              notarySignatures++;
              break;
            }
          }
        }
        require(notarySignatures == parsed.length, string(abi.encodePacked(
          "The required notary signature is not present in the proof."
        )));
      } else if (groupIndex == componentGroupIndexSigners) {
        // Check that all required signatures are present in the proof data.
        string[] memory parsed = extractPublicKeys(vd.eventData.componentData[c].encodedBytes);
        uint256 participantSignatures = 0;
        for (uint i = 0; i < parsed.length; i++) {
          bytes memory key = X509.decodeKey(parsed[i]);
          for (uint s = 0; s < vd.signatures.length; s++) {
            if (keccak256(abi.encodePacked(vd.signatures[s].by)) == keccak256(abi.encodePacked(key))) {
              participantSignatures++;
              break;
            }
          }
        }
        require(participantSignatures == parsed.length, string(abi.encodePacked(
          "Some participant signatures are missing, expected to find=[",
          SolUtils.UIntToString(parsed.length),
          "] signatures."
        )));
      } else {
        revert(string(abi.encodePacked(
          "Event data contains unknown component group with index=[",
          SolUtils.UIntToString(uint256(groupIndex)),
          "]."
        )));
      }
      hashesToCheck[groupIndex][componentCounts[groupIndex]] = calculateComponentHash(
        vd.eventData.componentData[c],
        vd.eventData.privacySalt
      );
      componentCounts[groupIndex] = componentCounts[groupIndex]+1;
    }
    // Enforce the presence of the outputs group at a minimum for now.
    require(outputIsPresent != 0x00, string(abi.encodePacked(
      "Required outputs is not present in the proof, expected at least one outputs group."
    )));
    require(commandIsPresent != 0x00, string(abi.encodePacked(
      "Required command is not present in the proof, expected command=[",
      vd.functionCommand,
      "]."
    )));
    require(componentCounts[componentGroupIndexSigners] > 0, string(abi.encodePacked(
      "Required signers is not present in the proof, expected at least one signers group."
    )));
    require(componentCounts[componentGroupIndexNotary] > 0, string(abi.encodePacked(
      "Required notary is not present in the proof, expected one notary group."
    )));
    // Check that all components hashes are present in the proof data.
    for (uint g = 0; g < componentGroupCount; g++) {
      if (componentCounts[g] == 1) {
        bytes1 found = 0x00;
        for (uint256 i = 0; i < vd.proofData.values.length; i++) {
          if (keccak256(abi.encodePacked(vd.proofData.values[i])) == keccak256(abi.encodePacked(hashesToCheck[g][0]))) {
            found = 0x01;
            break;
          }
        }
        if (found != 0x01) {
          revert(string(abi.encodePacked(
            "Component hash=[",
            SolUtils.Bytes32ToHexString(hashesToCheck[g][0]),
            "] with one child was not found in merkle multi-valued proof with root=[",
            SolUtils.Bytes32ToHexString(vd.proofData.root),
            "]."
          )));
        }
      } else if (componentCounts[g] > 1) {
        bytes32[] memory leaves = new bytes32[](componentCounts[g]);
        for (uint k = 0; k < componentCounts[g]; k++) {
          leaves[k] = hashesToCheck[g][k];
        }
        bytes32 hash = Merkle.getRoot(Merkle.padWithZeros(leaves, false));
        bytes1 found = 0x00;
        for (uint i = 0; i < vd.proofData.values.length; i++) {
          if (keccak256(abi.encodePacked(vd.proofData.values[i])) == keccak256(abi.encodePacked(hash))) {
            found = 0x01;
            break;
          }
        }
        if (found != 0x01) {
          revert(string(abi.encodePacked(
            "Component hash=[",
            SolUtils.Bytes32ToHexString(hash),
            "] with multiple children was not found in merkle multi-valued proof with root=[",
            SolUtils.Bytes32ToHexString(vd.proofData.root),
            "]."
          )));
        }
      }
    }
    // Verify the multi-valued merkle proof.
    if (!Merkle.verifyMultiProof(vd.proofData.root, vd.proofData.witnesses, vd.proofData.flags, vd.proofData.values)) {
      revert(string(abi.encodePacked(
        "Failed to verify merkle multi-valued proof with root=[",
        SolUtils.Bytes32ToHexString(vd.proofData.root),
        "]."
      )));
    }
    // Check that correct root are used in signature data.
    for (uint256 i = 0; i < vd.signatures.length; i++) {
      bytes32 root = SolUtils.BytesToNBytes32AtIndex(vd.signatures[i].meta, 8, 32);
      if (keccak256(abi.encodePacked(root)) != keccak256(abi.encodePacked(vd.proofData.root))) {
        if (!Merkle.verifyIncludedLeaf(root, vd.proofData.root)) {
          revert(string(abi.encodePacked(
            "Failed to verify included merkle leaf with root=[",
            SolUtils.Bytes32ToHexString(root),
            "]."
          )));
        }
      }
    }
    return true;
  }

  /*
 * Validate a trade by proving that the trade identifier was signed over and that this identifier is part of the remote function input parameters.
 * @param {Corda.EventData} eventData Contains remote function input parameters and component group data.
   * @param {Corda.ParameterHandler[]} handlers The parameter handlers used to extract data from the Corda component group.
   * @param {Corda.ProofData} proofData Data needed to prove component group membership in the transaction tree, which root was signed over.
   * @param {Corda.Signature[]} signatures Signatures over the transaction tree root.
   * @return {bool} Returns true if the trade was successfully validated.
   */
  function validateTrade(
    ValidationData memory vd
  ) internal pure returns (bool) {
    Object.Obj[] memory parameters = extractParameters(vd.eventData.callParameters, vd.functionPrototype, vd.handlers);
    Object.Obj memory obj;
    obj.setString(SolUtils.Bytes32ToHexString(vd.proofData.root));
    for (uint i = 0; i < vd.handlers.length; i++) {
      if (keccak256(abi.encodePacked(vd.handlers[i].fingerprint)) != keccak256(abi.encodePacked(""))) {
        if (!parameters[i].isEqual(obj))
          revert("Signed values do not match function call parameters");
      }
    }
    for (uint i = 0; i < vd.signatures.length; i++) {
      bytes32 root = SolUtils.BytesToBytes32(vd.signatures[i].meta, 8);
      if (keccak256(abi.encodePacked(root)) != keccak256(abi.encodePacked(vd.proofData.root))) {
        if (!Merkle.verifyIncludedLeaf(root, vd.proofData.root)) {
          revert("Multi proof failed to verify");
        }
      }
    }
    return true;
  }

  /*
   * Computes the hash of a serialised component to be used as Merkle tree leaf. The resultant output leaf is calculated using the service's hash algorithm, thus HASH(HASH(nonce || serializedComponent)) for SHA256.
   * @param {uint8} groupIndex Contains the group index of the group the component belongs to.
   * @param {uint8} internalIndex Contains the internal index of this component inside the group.
   * @param {bytes32} privacySalt Salt needed to compute a Merkle tree leaf.
   * @param {bytes} encodedComponent Contains the hex-encoded component group.
   * @return {bytes} Returns the calculated component hash.
   */
  function calculateComponentHash(
    ComponentData memory group,
    bytes32 privacySalt
  ) internal pure returns (bytes32) {
    bytes memory encodedComponent = bytes.concat(hex'636F726461010000', group.encodedBytes);
    return calculateHash(calculateHash(calculateNonce(group.groupIndex, group.internalIndex, privacySalt), encodedComponent), encodedComponent);
  }

  /*
   * Compute the transaction tree hash as HASH(HASH(nonce || serializedComponent)) for SHA256.
   * @param {bytes32} nonce The nonce as calculated from the group index, internal index and salt.
   * @param {bytes} opaque The encoded component group data.
   * @return {bytes32} Returns the calculated hash.
   */
  function calculateHash(bytes32 nonce, bytes memory opaque) internal pure returns (bytes32) {
    bytes memory data = bytes.concat(nonce, opaque);
    return sha256(abi.encodePacked(sha256(data)));
  }

  /*
   * Compute the nonce as HASH(HASH(privacySalt || groupIndex || internalIndex)) for SHA256.
   * @param {uint8} groupIndex Contains the group index of the group the component belongs to.
   * @param {uint8} internalIndex Contains the internal index of this component inside the group.
   * @param {bytes32} privacySalt Salt needed to compute a Merkle tree leaf.
   * @return {bytes32} Returns the calculated none.
   */
  function calculateNonce(
    uint8 groupIndex,
    uint8 internalIndex,
    bytes32 privacySalt
  ) internal pure returns (bytes32) {
    bytes memory data = bytes.concat(
      privacySalt,
      bytes.concat(bytes4(uint32(groupIndex)), bytes4(uint32(internalIndex)))
    );
    return sha256(abi.encodePacked(sha256(data)));
  }

  /*
   * Extract function call input parameters.
   * @param {bytes} callData Function call input parameters as call data.
   * @param {Corda.ParameterHandler[]} handlers Handlers to use for extracting.
   * @return {Object.Obj[]} Returns an array of extracted values, one for each input parameter for which there is a handler.
   */
  function extractParameters(
    bytes memory callData,
    string memory functionPrototype,
    Corda.ParameterHandler[] memory handlers
  ) internal pure returns (Object.Obj[] memory) {
    Object.Obj[] memory extractedParameters = new Object.Obj[](handlers.length);
    string memory functionTopLevelTypesString = SolUtils.StringSlice(
      functionPrototype,
      uint256(SolUtils.StringIndexOf(functionPrototype, "(")) + 1,
      SolUtils.StringLength(functionPrototype) - 1
    );
    uint256 callDataReferenceOffset = 4;
    for (uint256 i = 0; i < handlers.length; i++) {
      if (keccak256(abi.encodePacked(handlers[i].fingerprint)) != keccak256(abi.encodePacked(""))) {
        extractedParameters[i] = Object.extractFromCalldata(
          functionTopLevelTypesString,
          handlers[i].calldataPath,
          callDataReferenceOffset,
          callData
        );
      }
    }
    return extractedParameters;
  }

  /*
   * Extract values from an AMQP-encoded Corda serialization.
   * @param {bytes} encoded The encoded data from which to extract values.
   * @param {Corda.ParameterHandler[]} handlers Handlers to use for extracting.
   * @return {Object.Obj[]} Returns an array of extracted values, one for each input parameter for which there is a handler.
   */
  function extractByFingerprint(
    bytes memory encoded,
    Corda.ParameterHandler[] memory handlers
  ) internal pure returns (Object.Obj[] memory)  {
    Object.Obj[] memory result = new Object.Obj[](handlers.length);
    string[] memory fingerprintFilters = new string[](handlers.length);
    for (uint256 i = 0; i < handlers.length; i++) {
      if (keccak256(abi.encodePacked(handlers[i].fingerprint)) != keccak256(abi.encodePacked(""))) {
        fingerprintFilters[i] = handlers[i].fingerprint;
      }
    }
    Parser.Parsed memory parsed;
    parsed.filters = fingerprintFilters;
    uint256 extracted = parsed.parseCordaSerializationPayload(encoded);
    for (uint256 i = 0; i < handlers.length; i++) {
      if (keccak256(abi.encodePacked(handlers[i].fingerprint)) != keccak256(abi.encodePacked(""))) {
        uint8 index = 0;
        for (uint256 j = 0; j < extracted; j++) {
          if (
            keccak256(abi.encodePacked(handlers[i].fingerprint)) ==
            keccak256(abi.encodePacked(parsed.extracted[j].fingerprint))
          ) {
            if (index == handlers[i].componentIndex) {
              result[i] = parsed.extracted[j].extract;
              if (result[i].selector != Object.selectorObjArray) {
                revert(string(abi.encodePacked(
                  "Unable to parse parameter with selector=[",
                  SolUtils.Bytes4ToHexString(bytes4(result[i].selector)),
                  "]."
                )));
              }
              Object.Obj[] memory value = result[i].getArray();
              uint256 size = value.length;
              if (uint8(size) != handlers[i].describedSize) {
                revert(string(abi.encodePacked(
                  "Unexpected size when parsing parameter with selector = [",
                  SolUtils.Bytes4ToHexString(bytes4(result[i].selector)),
                  "]. Got [",
                  SolUtils.UIntToString(size),
                  "], expected [",
                  SolUtils.UIntToString(uint256(handlers[i].describedSize)),
                  "]."
                )));
              }
              uint32 parserId = uint32(bytes4(keccak256(bytes(handlers[i].parser))));
              if (parserPath == parserId) result[i] = cordaPathParser(value, handlers[i].describedPath);
              else if (parserParty == parserId) result[i] = cordaPartyParser(value, handlers[i].describedPath);
              else
                revert(string(abi.encodePacked(
                  "Unable to parse parameter. Parameter handler has unknown parser id=[",
                  SolUtils.Bytes32ToHexString(SolUtils.UInt32ToBytes32(parserId)),
                  "]."
                )));
            }
            index++;
          }
        }
      }
    }
    return result;
  }

  /*
   * Extract command tag values from an AMQP-encoded Corda serialization.
   * @param {bytes} encoded The encoded data from which to extract values.
   * @return {Object.Obj[]} Returns an array of extracted values, one for each encountered command.
   */
  function extractCommands(
    bytes memory encoded
  ) internal pure returns (string[] memory) {
    Parser.Parsed memory parsed;
    uint256 extracted = parsed.parseCordaSerializationSchema(encoded);
    require(extracted > 0, string(abi.encodePacked(
      "Failed to parse corda schema."
    )));
    string[] memory result = new string[](commandsGroupLimit);
    uint numParsed = Parser.parseCordaCommandTags(parsed.extracted[0].extract, 0, result);
    require(numParsed > 0, string(abi.encodePacked(
      "Failed to extract commands during corda deserialization."
    )));
    string[] memory res = new string[](numParsed);
    for (uint j = 0; j < numParsed; j++) {
      res[j] = result[j];
    }
    return res;
  }

  /*
   * Extract public key values from an AMQP-encoded Corda serialization.
   * @param {bytes} encoded The encoded data from which to extract values.
   * @return {Object.Obj[]} Returns an array of extracted values, one for each encountered public key.
   */
  function extractPublicKeys(
    bytes memory encoded
  ) internal pure returns (string[] memory) {
    string[] memory fingerprintFilters = new string[](1);
    fingerprintFilters[0] = "net.corda:java.security.PublicKey";
    Parser.Parsed memory parsed;
    parsed.filters = fingerprintFilters;
    uint256 extracted = parsed.parseCordaSerializationPayload(encoded);
    require(extracted > 0, string(abi.encodePacked(
      "Failed to parse corda payload."
    )));
    string[] memory result = new string[](extracted);
    for (uint j = 0; j < extracted; j++) {
      if (parsed.extracted[j].extract.selector == Object.selectorObjArray) {
        Object.Obj[] memory value = parsed.extracted[j].extract.getArray();
        uint size = value.length;
        require(uint8(size) > 0, string(abi.encodePacked(
          "Unable to extract public keys from empty array."
        )));
        result[j] = (value[0].selector == Object.selectorString ? value[0].getString() : "");
      } else if (parsed.extracted[j].extract.selector == Object.selectorBytes) {
        parsed.extracted[j].extract.convertTo(Object.selectorString);
        result[j] = string(parsed.extracted[j].extract.getBytes());
      }
    }
    return result;
  }

  /*
   * Parse a Corda party object following the provided recursion path.
   * @param {Object.Obj[]} memory obj,
   * @param {bytes} memory path
   * @return {Object.Obj} Returns the parsed representation of the arty.
   */
  function cordaPartyParser(
    Object.Obj[] memory obj,
    bytes memory path
  ) internal pure returns (Object.Obj memory) {
    require(obj.length == 6, string(abi.encodePacked(
      "Unexpected party object size, expected=[6], but got=[",
      SolUtils.UIntToString(obj.length),
      "] field elements."
    )));
    Object.Obj memory result;
    string memory commonName = (obj[0].selector == Object.selectorString ? obj[0].getString() : "");
    string memory country = (obj[1].selector == Object.selectorString ? obj[1].getString() : "");
    string memory locality = (obj[2].selector == Object.selectorString ? obj[2].getString() : "");
    string memory organisation = (obj[3].selector == Object.selectorString ? obj[3].getString() : "");
    string memory organisationUnit = (obj[4].selector == Object.selectorString ? obj[4].getString() : "");
    string memory state = (obj[5].selector == Object.selectorString ? obj[5].getString() : "");
    string memory x500Name = generateRFC1779DistinguishedName(
      generateX500Names(commonName, country, locality, organisation, organisationUnit, state)
    );
    result.setString(Base64.encode(bytes(x500Name)));
    return result;
  }

  /*
   * Generate a RFC1779 distinguished name for the array of input values.
   * @param {string[]} names The input names to use.
   * @return {string} Return the generated name.
   */
  function generateRFC1779DistinguishedName(string[] memory names) internal pure returns (string memory) {
    string memory name = "";
    uint256 size = names.length;
    if (size == 0) {
      return name;
    } else if (size == 1) {
      return names[0];
    } else {
      for (uint256 i = 0; i < size; i++) {
        if (i != 0) {
          name = string(abi.encodePacked(name, ", "));
        }
        name = string(abi.encodePacked(name, names[size - 1 - i]));
      }
    }
    // TODO: Limit to 48 bytes
    return name;
  }

  /*
   * Generate a RFC1779 distinguished name for the array of input values.
   * @param {string[]} names The input names to use.
   * @return {string} Return the generated name.
   */
  function generateX500Names(
    string memory commonName,
    string memory country,
    string memory locality,
    string memory organisation,
    string memory organisationUnit,
    string memory state
  ) internal pure returns (string[] memory) {
    uint256 hasCommonName = keccak256(abi.encodePacked(commonName)) == keccak256(abi.encodePacked("")) ? 0 : 1;
    uint256 hasOrganisationUnit = keccak256(abi.encodePacked(organisationUnit)) == keccak256(abi.encodePacked("")) ? 0 : 1;
    uint256 hasState = keccak256(abi.encodePacked(state)) == keccak256(abi.encodePacked("")) ? 0 : 1;
    uint256 size = 3 + hasCommonName + hasOrganisationUnit + hasState;
    uint256 i = 0;
    string[] memory list = new string[](size);
    list[i++] = string(abi.encodePacked("C=", country));
    if (hasState > 0) list[i++] = string(abi.encodePacked("ST=", state));
    list[i++] = string(abi.encodePacked("L=", locality));
    list[i++] = string(abi.encodePacked("O=", organisation));
    if (hasOrganisationUnit > 0) list[i++] = string(abi.encodePacked("OU=", organisationUnit));
    if (hasCommonName > 0) list[i++] = string(abi.encodePacked("CN=", commonName));
    return list;
  }

  /*
   * Parse a specific element out of the object tree according to the given path.
   * @param {Object.Obj[]} obj The object tree to traverse.
   * @param {bytes} path The path to take when traversing.
   * @return {Object.Obj} Return the parsed element.
   */
  function cordaPathParser(Object.Obj[] memory obj, bytes memory path) internal pure returns (Object.Obj memory) {
    Object.Obj memory result;
    Object.Obj[] memory current = obj;
    for (uint256 i = 0; i < path.length; i++) {
      uint8 index = uint8(path[i]);
      require(current.length > index, string(abi.encodePacked(
        "Malformed path when parsing object from tree."
      )));
      result = current[index];
      if (result.selector == Object.selectorObjArray)
        current = result.getArray();
      else
        require(i == path.length - 1, string(abi.encodePacked(
          "Unexpected path when parsing object from tree."
        )));
    }
    return result;
  }
}

/*
 * Library to handle Corda serialization.
 */
library Parser {
  using AMQP for AMQP.Buffer;
  using Object for Object.Obj;

  /*
   * Structure to hold data, extracted from Corda transaction component groups.
   * @property {string} fingerprint Fingerprint used to extract the data.
   * @property {Object.Obj} extract The extracted data.
   */
  struct Extracted {
    string fingerprint;
    Object.Obj extract;
  }

  /*
   * Structure to hold data, parsed from the AMQP proton graph, according to provided filters.
   * @property {string[]} filters Filter used to limit the amount of data that gets parsed in the EVM.
   * @property {Extracted[256]} extracted The extracted data, limited to 256 extracted items.
   * @property {uint256} processed The amount of data that was parsed.
   */
  struct Parsed {
    string[] filters;
    Extracted[256] extracted;
    uint256 processed;
  }

  /*
   * Parse the payload of a Corda serialization as an AMQP proton graph.
   * @param {Parsed} parsed The structure to parse data into according to filters.
   * @param {bytes} encoded The AMQP-encoded bytes.
   * @return {uint256} The amount of data that was parsed.
   */
  function parseCordaSerializationPayload(
    Parsed memory parsed,
    bytes memory encoded
  ) internal pure returns (uint256) {
    Object.Obj[] memory graph = readCordaGraph(encoded, false);
    parseCordaGraph(parsed, graph[0], 0, 0, "", false);
    return parsed.processed;
  }

  /*
   * Parse the schema of a Corda serialization as an AMQP proton graph.
   * @param {Parsed} parsed The structure to parse data into according to filters.
   * @param {bytes} encoded The AMQP-encoded bytes.
   * @return {uint256} The amount of data that was parsed.
   */
  function parseCordaSerializationSchema(
    Parsed memory parsed,
    bytes memory encoded
  ) internal pure returns (uint256) {
    Object.Obj[] memory graph = readCordaGraph(encoded, true);
    Extracted memory extracted;
    extracted.fingerprint = "Schema";
    extracted.extract = graph[1];
    parsed.extracted[parsed.processed++] = extracted;
    return parsed.processed;
  }

  /*
   * Parse the AMQP proton graph.
   * @param {Parsed} parsed The structure to parse data into according to filters.
   * @param {Object.Obj} graph The AMQP proton graph to parse from.
   * @param {uint256} level The current recursion level.
   * @param {uint256} index The current index int that level.
   * @param {string} extract The current fingerprint.
   * @return {Extracted} extract The extracted data for the given fingerprint.
   */
  function parseCordaGraph(
    Parsed memory parsed,
    Object.Obj memory graph,
    uint256 level,
    uint256 index,
    string memory extract,
    bool inclSchema
  ) internal pure returns (Extracted memory) {
    Object.Obj memory obj;
    Extracted memory extracted;
    if (graph.selector == Object.selectorObjArray) {
      Object.Obj[] memory objects = graph.getArray();
      if (objects.length > 0) {
        if (objects[0].tag == AMQP.tagDescriptorSymbol) { // Parsing a payload
          require(objects.length == 2, string(abi.encodePacked(
            "Malformed payload in proton graph, expected=[2] but got=[",
            SolUtils.UIntToString(objects.length),
            "] objects indexed by a symbol tag descriptor."
          )));
          string memory fingerprint = objects[0].getString();
          extracted = parseCordaGraph(parsed, objects[1], level + 1, 1, fingerprint, inclSchema);
          if (extracted.extract.selector != Object.selectorObjArray) { // Symbol that did not hold a nested object
            if (SolUtils.StringInArray(parsed.filters, fingerprint)) {
              parsed.extracted[parsed.processed] = extracted;
              parsed.processed++;
            }
          }
        } else if (objects[0].tag == AMQP.tagDescriptorUnsignedLong && inclSchema) { // Parsing a schema. This is currently not used due to the recursive nature of this function and the size of schemas.
          require(objects.length == 2, string(abi.encodePacked(
            "Malformed schema in proton graph, expected=[2], but got=[",
            SolUtils.UIntToString(objects.length),
            "] objects indexed by a unsigned long tag descriptor."
          )));
          string memory fingerprint = SolUtils.UIntToString(objects[0].getUInt256());
          extracted = parseCordaGraph(parsed, objects[1], level + 1, 1, fingerprint, inclSchema);
        } else {
          Object.Obj[] memory grouped = new Object.Obj[](objects.length);
          for (uint i = 0; i < objects.length; i++) {
            grouped[i] = parseCordaGraph(parsed, objects[i], level + 1, i, extract, inclSchema).extract;
          }
          obj.setArray(grouped);
          extracted.fingerprint = extract;
          extracted.extract = obj;
          if (SolUtils.StringInArray(parsed.filters, extract)) {
            parsed.extracted[parsed.processed] = extracted;
            parsed.processed++;
          }
        }
      }
    } else {
      if (graph.selector > 0 && !SolUtils.StringIsEmpty(extract)) {
        obj = graph;
        extracted.fingerprint = extract;
        extracted.extract = obj;
      }
    }
    return extracted;
  }

  /*
   * Parse command tags out of the AMQP proton graph.
   * @param {Object.Obj} obj The AMQP proton graph to parse from.
   * @param {uint256} ctr The current extraction counter.
   * @param {string[]} extracted The current extracted list limited by commandsGroupLimit items.
   * @return {uint256} The number of command tags that could be extracted.
   */
  function parseCordaCommandTags(
    Object.Obj memory obj,
    uint ctr,
    string[] memory extracted
  ) internal pure returns (uint256) {
    if (uint32(obj.selector) == Object.selectorObjArray) {
      Object.Obj[] memory objs = obj.getArray();
      for (uint k=0; k<objs.length; k++) {
        ctr = parseCordaCommandTags(objs[k], ctr, extracted);
      }
    } else if (uint32(obj.selector) == Object.selectorString) {
      string memory str = obj.getString();
      if (SolUtils.StringIndexOf(str, "$") > 0) {
        require(ctr < extracted.length, string(abi.encodePacked(
          "A maximum of=[",
          SolUtils.UIntToString(extracted.length),
          "] commands are currently supported."
        )));
        extracted[ctr++] = str;
      }
    }
    return ctr;
  }

  /*
   * Read the Corda envelope and payload from the AMQP buffer.
   * @param {bytes} encoded The AMQP-encode bytes.
   * @return {Object.Obj} Returns the extracted payload as an object graph
   */
  function readCordaGraph(
    bytes memory encoded,
    bool inclSchema
  ) internal pure returns (Object.Obj[] memory) {
    AMQP.Buffer memory buffer;
    buffer.set(encoded);
    readCordaEnvelope(buffer);
    return readCordaPayloadAndSchema(buffer, inclSchema);
  }

  /*
   * Read the Corda envelope from the AMQP buffer.
   * @param {bytes} buffer The AMQP buffer being operated on.
   * @return {Object.Obj} Returns the extracted envelope as an object graph.
   */
  function readCordaEnvelope(
    AMQP.Buffer memory buffer
  ) internal pure returns (Object.Obj memory) {
    bytes1 code = buffer.readType();
    if (AMQP.DESCRIBED_TYPE == code) {
      bytes1 encoding = buffer.readByte(buffer.position);
      if (AMQP.SMALLULONG != encoding && AMQP.ULONG != encoding && AMQP.SYM8 != encoding && AMQP.SYM32 != encoding) {
        revert(string(abi.encodePacked(
          "Unexpected descriptor in envelope."
        )));
      }
      return buffer.readObject();
    }
    revert(string(abi.encodePacked("Unexpected envelope.")));
  }

  /*
   * Read the Corda payload, and optionally schema, from the AMQP buffer.
   * @param {bytes} buffer The AMQP buffer being operated on.
   * @return {Object.Obj} Returns the extracted payload as an object graph.
   */
  function readCordaPayloadAndSchema(
    AMQP.Buffer memory buffer,
    bool inclSchema
  ) internal pure returns (Object.Obj[] memory) {
    uint256 num = inclSchema ? 2 : 1;
    Object.Obj[] memory result = new Object.Obj[](num);
    bytes1 code = buffer.readType();
    // Filter here by reading ONLY the first element of the list so that ONLY the payload gets read unless specifically indicating that the schema should be read.
    if (AMQP.LIST8 == code) {
      uint8 size = uint8(buffer.readByte()) & 0xFF;
      AMQP.Buffer memory buf = buffer.getSlice();
      buf.limit = size;
      buffer.position = buffer.position + size;
      uint8 count = uint8(buf.readByte()) & 0xFF;
      require(count == 3, string(abi.encodePacked(
        "Expected list with=[3] elements=[payload, schema, transform schema] in envelope, got=[",
        SolUtils.UIntToString(count),
        "] elements in small list."
      )));
      result[0] = buf.parseListElements(1);
      if (inclSchema)
        result[1] = buf.parseListElements(1);
      return result;
    } else if (AMQP.LIST32 == code) {
      uint32 size = uint32(buffer.readInteger());
      AMQP.Buffer memory buf = buffer.getSlice();
      buf.limit = size;
      buffer.position = buffer.position + size;
      uint32 count = uint32(buf.readInteger());
      require(count == 3, string(abi.encodePacked(
        "Expected list with=[3] elements=[payload, schema, transform schema] in envelope, got=[",
        SolUtils.UIntToString(count),
        "] elements in list."
      )));
      result[0] = buf.parseListElements(1);
      if (inclSchema)
        result[1] = buf.parseListElements(1);
      return result;
    }
    revert(string(abi.encodePacked(
      "Unexpected payload or schema."
    )));
  }
}

/*
 * Library to handle AMQP encoding.
 */
library AMQP {
  using Object for Object.Obj;

  /* AMQP encoding codes */
  bytes1 public constant DESCRIBED_TYPE = 0x00;
  bytes1 public constant NULL = 0x40;
  bytes1 public constant BOOLEAN = 0x56;
  bytes1 public constant BOOLEAN_TRUE = 0x41;
  bytes1 public constant BOOLEAN_FALSE = 0x42;
  bytes1 public constant UBYTE = 0x50;
  bytes1 public constant USHORT = 0x60;
  bytes1 public constant UINT = 0x70;
  bytes1 public constant SMALLUINT = 0x52;
  bytes1 public constant UINT0 = 0x43;
  bytes1 public constant ULONG = 0x80;
  bytes1 public constant SMALLULONG = 0x53;
  bytes1 public constant ULONG0 = 0x44;
  bytes1 public constant BYTE = 0x51;
  bytes1 public constant SHORT = 0x61;
  bytes1 public constant INT = 0x71;
  bytes1 public constant SMALLINT = 0x54;
  bytes1 public constant LONG = 0x81;
  bytes1 public constant SMALLLONG = 0x55;
  bytes1 public constant FLOAT = 0x72;
  bytes1 public constant DOUBLE = 0x82;
  bytes1 public constant DECIMAL32 = 0x74;
  bytes1 public constant DECIMAL64 = 0x84;
  bytes1 public constant DECIMAL128 = 0x94;
  bytes1 public constant CHAR = 0x73;
  bytes1 public constant TIMESTAMP = 0x83;
  bytes1 public constant UUID = 0x98;
  bytes1 public constant VBIN8 = 0xa0;
  bytes1 public constant VBIN32 = 0xb0;
  bytes1 public constant STR8 = 0xa1;
  bytes1 public constant STR32 = 0xb1;
  bytes1 public constant SYM8 = 0xa3;
  bytes1 public constant SYM32 = 0xb3;
  bytes1 public constant LIST0 = 0x45;
  bytes1 public constant LIST8 = 0xc0;
  bytes1 public constant LIST32 = 0xd0;
  bytes1 public constant MAP8 = 0xc1;
  bytes1 public constant MAP32 = 0xd1;
  bytes1 public constant ARRAY8 = 0xe0;
  bytes1 public constant ARRAY32 = 0xf0;

  /* AMQP tag descriptors */
  uint8 public constant tagDescriptorUnsignedLong = uint8(0x01);
  uint8 public constant tagDescriptorSymbol = uint8(0x02);
  uint8 public constant tagDescriptorObject = uint8(0x03);

  uint8 public constant tagDescribedType = uint8(0x10);

  uint8 public constant tagDescribedArray = uint8(0x11);
  uint8 public constant tagDescribedBoolean = uint8(0x12);
  uint8 public constant tagDescribedBinary = uint8(0x13);
  uint8 public constant tagDescribedByte = uint8(0x14);
  uint8 public constant tagDescribedCharacter = uint8(0x15);
  uint8 public constant tagDescribedDecimal32 = uint8(0x16);
  uint8 public constant tagDescribedDecimal64 = uint8(0x17);
  uint8 public constant tagDescribedDecimal128 = uint8(0x18);
  uint8 public constant tagDescribedDouble = uint8(0x19);
  uint8 public constant tagDescribedFloat = uint8(0x20);
  uint8 public constant tagDescribedEmpty = uint8(0x21);
  uint8 public constant tagDescribedInteger = uint8(0x22);
  uint8 public constant tagDescribedList = uint8(0x23);
  uint8 public constant tagDescribedLong = uint8(0x24);
  uint8 public constant tagDescribedNull = uint8(0x25);
  uint8 public constant tagDescribedShort = uint8(0x26);
  uint8 public constant tagDescribedString = uint8(0x27);
  uint8 public constant tagDescribedSymbol = uint8(0x28);
  uint8 public constant tagDescribedTimestamp = uint8(0x29);
  uint8 public constant tagDescribedUnsignedByte = uint8(0x30);
  uint8 public constant tagDescribedUnsignedShort = uint8(0x31);
  uint8 public constant tagDescribedUnsignedInteger = uint8(0x32);
  uint8 public constant tagDescribedUnsignedLong = uint8(0x33);
  uint8 public constant tagDescribedUUID = uint8(0x34);

  /*
   * Structure to allow operations on a buffer of bytes.
   * @property {bytes} value Contents of byte buffer.
   * @property {uint256} position A marker for the current position in the buffer.
   * @property {uint256} limit The buffer's predefined limit.
   */
  struct Buffer {
    bytes value;
    uint256 position;
    uint256 limit;
  }

  /*
   * Initializes the buffer with the given bytes.
   */
  function set(Buffer memory buffer, bytes memory value) internal pure {
    buffer.value = value;
    buffer.position = 0;
    buffer.limit = value.length;
  }

  /*
   * Returns a buffer of which the content will start at this buffer's current position. The new buffer's position will be zero, its limit will be the number of bytes remaining in this buffer, and its byte order will be BIG_ENDIAN.
   */
  function getSlice(Buffer memory buffer) internal pure returns (Buffer memory) {
    uint256 pos = buffer.position;
    uint256 lim = buffer.limit;
    uint256 rem = (pos <= lim ? lim - pos : 0);
    uint256 off = (pos << 0);
    require(off >= 0, string(abi.encodePacked(
      "Error trying to slice byte array."
    )));
    Buffer memory result;
    result.value = getBytes(buffer, pos, rem);
    result.position = 0;
    result.limit = rem;
    return result;
  }

  /*
   * Returns a slice of bytes from the buffer.
   */
  function getBytes(
    Buffer memory buffer,
    uint256 start,
    uint256 length
  ) internal pure returns (bytes memory) {
    require(length + 31 >= length, string(abi.encodePacked(
      "Error trying to slice byte array, slice overflow."
    )));
    require(buffer.value.length >= start + length, string(abi.encodePacked(
      "Error trying to slice byte array, slice out of bounds."
    )));
    bytes memory tempBytes;
    bytes memory bufBytes = buffer.value;
    // Check length is 0.
    assembly ("memory-safe")  {
      switch iszero(length)
      case 0 {
      // Get a location of some free memory and store it in tempBytes as Solidity does for memory variables.
        tempBytes := mload(0x40)
      // Calculate length mod 32 to handle slices that are not a multiple of 32 in size.
        let lengthMod := and(length, 31)
      // Format of tempBytes in memory: <length><data>
      // When copying data we will offset the start forward to avoid allocating additional memory. Therefore part of the length area will be written, but this will be overwritten later anyways.
      // In case no offset is require, the start is set to the data region (0x20 from the tempBytes) mc will be used to keep track where to copy the data to.
        let mc := add(add(tempBytes, lengthMod), mul(0x20, iszero(lengthMod)))
        let end := add(mc, length)
        for {
        // Same logic as for mc is applied and additionally the start offset specified for the method is added.
          let cc := add(add(add(bufBytes, lengthMod), mul(0x20, iszero(lengthMod))), start)
        } lt(mc, end) {
        // Increase mc and cc to read the next word from memory
          mc := add(mc, 0x20)
          cc := add(cc, 0x20)
        } {
        // Copy the data from source (cc location) to the slice data (mc location).
          mstore(mc, mload(cc))
        }
      // Store the length of the slice. This will overwrite any partial data that was copied when having slices that are not a multiple of 32.
        mstore(tempBytes, length)
      // Update free-memory pointer. Allocating the array padded to 32 bytes like the compiler does now. To set the used memory as a multiple of 32, add 31 to the actual memory usage (mc) and remove the modulo 32 (the 'and' with 'not(31)')
        mstore(0x40, and(add(mc, 31), not(31)))
      }
      // If we want a zero-length slice let's just return a zero-length array.
      default {
        tempBytes := mload(0x40)
      // Zero out the 32 bytes slice we are about to return we need to do it because Solidity does not garbage collect.
        mstore(tempBytes, 0)
      // Update free-memory pointer. The length of tempBytes uses 32 bytes in memory (even when empty).
        mstore(0x40, add(tempBytes, 0x20))
      }
    }
    return tempBytes;
  }

  /*
   * Returns the number of elements between the current position and the limit.
   */
  function getRemaining(Buffer memory buffer) internal pure returns (uint256) {
    uint256 rem = buffer.limit - buffer.position;
    return rem > 0 ? rem : 0;
  }

  /*
   * Returns the size of the type represented by code.
   */
  function getTypeSize(Buffer memory buffer, bytes1 code) internal pure returns (uint256) {
    if (code == DESCRIBED_TYPE) {
      Buffer memory buf = getSlice(buffer);
      if (getRemaining(buf) > 0) {
        code = readType(buf);
        uint256 size = getTypeSize(buf, code);
        if (getRemaining(buf) > size) {
          buf.position = size + 1;
          code = readType(buf);
          return size + 2 + getTypeSize(buf, code);
        } else {
          return size + 2;
        }
      } else {
        return 1;
      }
    } else if (code == NULL) return 0;
    else if (code == BOOLEAN_TRUE) return 0;
    else if (code == BOOLEAN_FALSE) return 0;
    else if (code == UINT0) return 0;
    else if (code == ULONG0) return 0;
    else if (code == LIST0) return 0;
    else if (code == UBYTE) return 1;
    else if (code == BYTE) return 1;
    else if (code == SMALLUINT) return 1;
    else if (code == SMALLULONG) return 1;
    else if (code == SMALLINT) return 1;
    else if (code == SMALLLONG) return 1;
    else if (code == BOOLEAN) return 1;
    else if (code == USHORT) return 2;
    else if (code == SHORT) return 2;
    else if (code == UINT) return 4;
    else if (code == INT) return 4;
    else if (code == FLOAT) return 4;
    else if (code == CHAR) return 4;
    else if (code == DECIMAL32) return 4;
    else if (code == ULONG) return 8;
    else if (code == LONG) return 8;
    else if (code == DOUBLE) return 8;
    else if (code == TIMESTAMP) return 8;
    else if (code == DECIMAL64) return 8;
    else if (code == DECIMAL128) return 16;
    else if (code == UUID) return 16;
    else if (code == VBIN8 || code == STR8 || code == SYM8 || code == LIST8 || code == MAP8 || code == ARRAY8) {
      uint256 position = buffer.position;
      if (getRemaining(buffer) > 0) {
        bytes1 size = readByte(buffer) & 0xFF;
        buffer.position = position;
        return uint8(size) + 1;
      } else {
        return 1;
      }
    } else if (code == VBIN32 || code == STR32 || code == SYM32 || code == LIST32 || code == MAP32 || code == ARRAY32) {
      uint256 position = buffer.position;
      if (getRemaining(buffer) >= 4) {
        bytes4 size = readInteger(buffer);
        buffer.position = position;
        return uint32(size) + 4;
      } else {
        return 4;
      }
    }
    revert(string(abi.encodePacked(
        "Unable to determine type size for code=[",
        SolUtils.UIntToString(uint8(code)),
        "]."
      )));
  }

  /*
   * Reads an object the proton graph from the buffer.
   */
  function readObject(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    bytes1 code = readType(buffer);
    if (code == DESCRIBED_TYPE) return parseDescribedType(buffer);
    else if (code == NULL) return parseNull(buffer);
    else if (code == BOOLEAN_TRUE) return parseTrue(buffer);
    else if (code == BOOLEAN_FALSE) return parseFalse(buffer);
    else if (code == UINT0) return parseUInt0(buffer);
    else if (code == ULONG0) return parseULong0(buffer);
    else if (code == LIST0) return parseEmptyList(buffer);
    else if (code == UBYTE) return parseUByte(buffer);
    else if (code == BYTE) return parseByte(buffer);
    else if (code == SMALLUINT) return parseSmallUInt(buffer);
    else if (code == SMALLULONG) return parseSmallULong(buffer);
    else if (code == SMALLINT) return parseSmallInt(buffer);
    else if (code == SMALLLONG) return parseSmallLong(buffer);
    else if (code == BOOLEAN) return parseBoolean(buffer);
    else if (code == USHORT) return parseUShort(buffer);
    else if (code == SHORT) return parseShort(buffer);
    else if (code == UINT) return parseUInt(buffer);
    else if (code == INT) return parseInt(buffer);
    else if (code == FLOAT) return parseFloat(buffer);
    else if (code == CHAR) return parseChar(buffer);
    else if (code == DECIMAL32) return parseDecimal32(buffer);
    else if (code == ULONG) return parseULong(buffer);
    else if (code == LONG) return parseLong(buffer);
    else if (code == DOUBLE) return parseDouble(buffer);
    else if (code == TIMESTAMP) return parseTimestamp(buffer);
    else if (code == DECIMAL64) return parseDecimal64(buffer);
    else if (code == DECIMAL128) return parseDecimal128(buffer);
    else if (code == UUID) return parseUUID(buffer);
    else if (code == VBIN8) return parseSmallBinary(buffer);
    else if (code == STR8) return parseSmallString(buffer);
    else if (code == SYM8) return parseSmallSymbol(buffer);
    else if (code == VBIN32) return parseBinary(buffer);
    else if (code == STR32) return parseString(buffer);
    else if (code == SYM32) return parseSymbol(buffer);
    else if (code == LIST8) return parseSmallList(buffer);
    else if (code == MAP8) return parseSmallMap(buffer);
    else if (code == LIST32) return parseList(buffer);
    else if (code == MAP32) return parseMap(buffer);
    else if (code == ARRAY8) return parseSmallArray(buffer);
    else if (code == ARRAY32) return parseArray(buffer);
    revert(string(abi.encodePacked(
      "No type constructor was found for code=[",
      SolUtils.UIntToString(uint8(code)),
      "]."
    )));
  }

  /*
   * Reads the type from the buffer.
   */
  function readType(Buffer memory buffer) internal pure returns (bytes1) {
    return readByte(buffer) & 0xFF;
  }

  /*
   * Reads the byte at the given position, the buffer position is not affected.
   */
  function readByte(Buffer memory buffer, uint256 position) internal pure returns (bytes1) {
    return bytes1(getBytes(buffer, position, 1));
  }

  /*
   * Reads the byte at this buffer's current position, and then increments the position by one.
   */
  function readByte(Buffer memory buffer) internal pure returns (bytes1) {
    bytes1 result = bytes1(getBytes(buffer, buffer.position, 1));
    buffer.position++;
    return result;
  }

  /*
   * Reads the next count bytes at this buffer's current position, and then increments the position by count.
   */
  function readBytes(Buffer memory buffer, uint256 count) internal pure returns (bytes memory) {
    bytes memory result = getBytes(buffer, buffer.position, count);
    buffer.position += count;
    return result;
  }

  /*
   * Reads the next eight bytes at this buffer's current position, composing them into a double value according to the current byte order, and then increments the position by eight.
   */
  function readDouble(Buffer memory buffer) internal pure returns (bytes8) {
    bytes8 result = bytes8(getBytes(buffer, buffer.position, 8));
    buffer.position += 8;
    return result;
  }

  /*
   * Reads the next four bytes at this buffer's current position, composing them into a float value according to the current byte order, and then increments the position by four.
   */
  function readFloat(Buffer memory buffer) internal pure returns (bytes8) {
    bytes8 result = bytes8(getBytes(buffer, buffer.position, 8));
    buffer.position += 4;
    return result;
  }

  /*
   * Reads the next four bytes at this buffer's current position, composing them into an int value according to the current byte order, and then increments the position by four.
   */
  function readInteger(Buffer memory buffer) internal pure returns (bytes4) {
    bytes4 result = bytes4(getBytes(buffer, buffer.position, 4));
    buffer.position += 4;
    return result;
  }

  /*
   * Reads the next eight bytes at this buffer's current position, composing them into a long value according to the current byte order, and then increments the position by eight.
   */
  function readLong(Buffer memory buffer) internal pure returns (bytes8) {
    bytes8 result = bytes8(getBytes(buffer, buffer.position, 8));
    buffer.position += 8;
    return result;
  }

  /*
   * Reads the next two bytes at this buffer's current position, composing them into a short value according to the current byte order, and then increments the position by two.
   */
  function readShort(Buffer memory buffer) internal pure returns (bytes2) {
    bytes2 result = bytes2(getBytes(buffer, buffer.position, 2));
    buffer.position += 2;
    return result;
  }

  /*
   * Parser for AMQP type code 0x00.
   */
  function parseDescribedType(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj[] memory objects = new Object.Obj[](2);
    objects[0] = readObject(buffer);
    if (tagDescribedSymbol == objects[0].tag) objects[0].tag = tagDescriptorSymbol;
    else if (tagDescribedUnsignedLong == objects[0].tag) objects[0].tag = tagDescriptorUnsignedLong;
    else objects[0].tag = tagDescriptorObject;
    objects[1] = readObject(buffer);
    Object.Obj memory obj;
    obj.setArray(objects);
    obj.tag = tagDescribedType;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x40.
   */
  function parseNull(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    obj.tag = tagDescribedNull;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x41.
   */
  function parseTrue(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    obj.setBool(true);
    obj.tag = tagDescribedBoolean;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x42.
   */
  function parseFalse(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    obj.setBool(false);
    obj.tag = tagDescribedBoolean;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x43.
   */
  function parseUInt0(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    obj.setUInt32(uint32(0));
    obj.tag = tagDescribedUnsignedInteger;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x44
   */
  function parseULong0(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    obj.setUInt64(uint64(0));
    obj.tag = tagDescribedUnsignedLong;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x45
   */
  function parseEmptyList(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    obj.tag = tagDescribedEmpty;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x50
   */
  function parseUByte(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes1 b = readByte(buffer);
    obj.setBytes1(b);
    return obj;
  }

  /*
   * Parser for AMQP type code 0x51
   */
  function parseByte(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes1 b = readByte(buffer);
    obj.setBytes1(b);
    obj.tag = tagDescribedByte;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x52
   */
  function parseSmallUInt(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes1 b = readByte(buffer) & 0xFF;
    obj.setUInt8(uint8(b));
    obj.tag = tagDescribedUnsignedInteger;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x53
   */
  function parseSmallULong(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes1 b = readByte(buffer) & 0xFF;
    obj.setUInt32(uint32(uint8(b)));
    obj.tag = tagDescribedUnsignedLong;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x54
   */
  function parseSmallInt(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes1 b = readByte(buffer);
    obj.setInt16(int16(int8(uint8(b))));
    obj.tag = tagDescribedInteger;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x55
   */
  function parseSmallLong(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes1 b = readByte(buffer) & 0xFF;
    obj.setInt32(int32(int8(uint8(b))));
    obj.tag = tagDescribedLong;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x56
   */
  function parseBoolean(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    uint8 i = uint8(readByte(buffer));
    if (i != 0 && i != 1)
      revert(string(abi.encodePacked(
        "Error trying to parse boolean value."
      )));
    obj.setBool(i == 1);
    obj.tag = tagDescribedBoolean;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x60
   */
  function parseUShort(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes2 b = readShort(buffer);
    obj.setUInt16(uint16(b));
    obj.tag = tagDescribedUnsignedShort;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x61
   */
  function parseShort(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes2 b = readShort(buffer);
    obj.setInt16(int16(uint16(b)));
    obj.tag = tagDescribedShort;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x70
   */
  function parseUInt(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes4 b = readInteger(buffer);
    obj.setUInt32(uint32(b));
    obj.tag = tagDescribedUnsignedInteger;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x71
   */
  function parseInt(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes4 b = readInteger(buffer);
    obj.setInt32(int32(uint32(b)));
    obj.tag = tagDescribedInteger;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x72
   */
  function parseFloat(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes8 b = readFloat(buffer);
    obj.setInt64(int64(uint64(b)));
    obj.tag = tagDescribedFloat;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x73
   */
  function parseChar(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes4 b = readInteger(buffer);
    obj.setBytes4(b);
    obj.tag = tagDescribedCharacter;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x74
   */
  function parseDecimal32(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes4 b = readInteger(buffer);
    obj.setInt32(int32(uint32(b)));
    obj.tag = tagDescribedDecimal32;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x80
   */
  function parseULong(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes8 b = readLong(buffer);
    obj.setUInt64(uint64(b));
    obj.tag = tagDescribedUnsignedLong;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x81
   */
  function parseLong(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes8 b = readLong(buffer);
    obj.setInt64(int64(uint64(b)));
    return obj;
  }

  /*
   * Parser for AMQP type code 0x82
   */
  function parseDouble(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes8 b = readDouble(buffer);
    obj.setInt64(int64(uint64(b)));
    obj.tag = tagDescribedDouble;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x83
   */
  function parseTimestamp(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes8 b = readLong(buffer);
    obj.setInt64(int64(uint64(b)));
    obj.tag = tagDescribedTimestamp;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x84
   */
  function parseDecimal64(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes8 b = readLong(buffer);
    obj.setInt64(int64(uint64(b)));
    obj.tag = tagDescribedDecimal64;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x94
   */
  function parseDecimal128(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes8 msb = readLong(buffer);
    bytes8 lsb = readLong(buffer);
    obj.setInt256(int256(uint256(bytes32(bytes.concat(msb, lsb)))));
    // TODO: Calculate big decimal
    obj.tag = tagDescribedDecimal128;
    return obj;
  }

  /*
   * Parser for AMQP type code 0x98
   */
  function parseUUID(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    bytes8 msb = readLong(buffer);
    bytes8 lsb = readLong(buffer);
    obj.setBytes32(bytes32(bytes.concat(msb, lsb)));
    obj.tag = tagDescribedUUID;
    return obj;
  }

  /*
   * Parser for AMQP type code 0xa0
   */
  function parseSmallBinary(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    uint8 size = uint8(readByte(buffer)) & 0xFF;
    bytes memory binary = readBytes(buffer, size);
    obj.setBytes(binary);
    obj.tag = tagDescribedBinary;
    return obj;
  }

  /*
   * Parser for AMQP type code 0xa1
   */
  function parseSmallString(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    uint8 size = uint8(readByte(buffer)) & 0xFF;
    bytes memory binary = readBytes(buffer, size);
    // Encode to UTF-8 string
    obj.setString(string(binary));
    obj.tag = tagDescribedString;
    return obj;
  }

  /*
   * Parser for AMQP type code 0xa3
   */
  function parseSmallSymbol(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    uint8 size = uint8(readByte(buffer)) & 0xFF;
    bytes memory binary = readBytes(buffer, size);
    // Encode to ASCII string
    obj.setString(string(binary));
    obj.tag = tagDescribedSymbol;
    return obj;
  }

  /*
   * Parser for AMQP type code 0xb0
   */
  function parseBinary(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    uint32 size = uint32(readInteger(buffer));
    bytes memory binary = readBytes(buffer, size);
    obj.setBytes(binary);
    obj.tag = tagDescribedBinary;
    return obj;
  }

  /*
   * Parser for AMQP type code 0xb1
   */
  function parseString(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    uint32 size = uint32(readInteger(buffer));
    bytes memory binary = readBytes(buffer, size);
    // Encode to UTF-8 string
    obj.setString(string(binary));
    obj.tag = tagDescribedString;
    return obj;
  }

  /*
   * Parser for AMQP type code 0xb3
   */
  function parseSymbol(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    Object.Obj memory obj;
    uint32 size = uint32(readInteger(buffer));
    bytes memory binary = readBytes(buffer, size);
    // Encode to ASCII string
    obj.setString(string(binary));
    obj.tag = tagDescribedSymbol;
    return obj;
  }

  /*
   * Parser for AMQP type code 0xc0
   */
  function parseSmallList(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    uint8 size = uint8(readByte(buffer)) & 0xFF;
    Buffer memory buf = getSlice(buffer);
    buf.limit = size;
    buffer.position = buffer.position + size;
    uint8 count = uint8(readByte(buf)) & 0xFF;
    return parseListElements(buf, count);
  }

  /*
   * Parser for AMQP type code 0xc1
   */
  function parseSmallMap(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    uint8 size = uint8(readByte(buffer)) & 0xFF;
    Buffer memory buf = getSlice(buffer);
    buf.limit = size;
    buffer.position = buffer.position + size;
    uint8 count = uint8(readByte(buf)) & 0xFF;
    return parseListElements(buf, count);
  }

  /*
   * Parser for AMQP type code 0xd0
   */
  function parseList(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    uint32 size = uint32(readInteger(buffer));
    Buffer memory buf = getSlice(buffer);
    buf.limit = size;
    buffer.position = buffer.position + size;
    uint32 count = uint32(readInteger(buf));
    return parseListElements(buf, count);
  }

  /*
   * Parser for AMQP type code 0xd1
   */
  function parseMap(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    uint32 size = uint32(readInteger(buffer));
    Buffer memory buf = getSlice(buffer);
    buf.limit = size;
    buffer.position = buffer.position + size;
    uint32 count = uint32(readInteger(buf));
    return parseListElements(buf, count);
  }

  /*
   * Parser for AMQP type code 0xe0
   */
  function parseSmallArray(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    uint8 size = uint8(readByte(buffer)) & 0xFF;
    Buffer memory buf = getSlice(buffer);
    buf.limit = size;
    buffer.position = buffer.position + size;
    uint8 count = uint8(readByte(buf)) & 0xFF;
    return parseArrayElements(buf, count);
  }

  /*
   * Parser for AMQP type code 0xf0
   */
  function parseArray(Buffer memory buffer) internal pure returns (Object.Obj memory) {
    uint32 size = uint32(readInteger(buffer));
    Buffer memory buf = getSlice(buffer);
    buf.limit = size;
    buffer.position = buffer.position + size;
    uint32 count = uint32(readInteger(buf));
    return parseArrayElements(buf, count);
  }

  /*
   * Parser for AMQP array
   */
  function parseArrayElements(Buffer memory buffer, uint256 count) internal pure returns (Object.Obj memory) {
    bytes1 code = readByte(buffer);
    bool isDescribed = code == 0x00;
    uint256 descriptorPosition = buffer.position;
    if (isDescribed) {
      bytes1 descriptorCode = readType(buffer);
      buffer.position = buffer.position + getTypeSize(buffer, descriptorCode);
      code = readByte(buffer);
      if (code == 0x00) {
        revert(string(abi.encodePacked(
          "Malformed array data."
        )));
      }
    }
    Object.Obj memory described;
    if (isDescribed) {
      uint256 position = buffer.position;
      buffer.position = descriptorPosition;
      described = readObject(buffer);
      buffer.position = position;
    }
    uint256 start = isDescribed ? 1 : 0;
    uint256 num = count + start;
    Object.Obj[] memory objects = new Object.Obj[](num);
    if (isDescribed) {
      objects[start++] = described;
    }
    for (uint256 i = start; i < count + start; i++) {
      objects[i] = readObject(buffer);
    }
    Object.Obj memory obj;
    obj.setArray(objects);
    obj.tag = tagDescribedArray;
    return obj;
  }

  /*
   * Parser for AMQP list
   */
  function parseListElements(Buffer memory buffer, uint256 count) internal pure returns (Object.Obj memory) {
    Object.Obj[] memory objects = new Object.Obj[](count);
    for (uint256 i = 0; i < count; i++) {
      uint256 pos = buffer.position;
      bytes1 code = readType(buffer);
      uint256 size = getTypeSize(buffer, code);
      if (size > getRemaining(buffer)) {
        revert(string(abi.encodePacked(
            "Malformed list data, size=[",
            SolUtils.UIntToString(size),
            "] of list element with index=[",
            SolUtils.UIntToString(i),
            "] having code=[",
            SolUtils.UIntToString(uint8(code)),
            "] is larger than remaining=[",
            SolUtils.UIntToString(getRemaining(buffer)),
            "] bytes in the buffer."
          )));
      }
      buffer.position = pos;
      objects[i] = readObject(buffer);
    }
    Object.Obj memory obj;
    obj.setArray(objects);
    obj.tag = tagDescribedList;
    return obj;
  }
}

/*
 * Library to handle Ethereum types.
 */
library Object {
  /*
   * Structure to hold data, parsed from the AMQP proton graph, in an encoding that can easily be converted to Ethereum types.
   * @property {uint32} selector The selector to identify the Ethereum type.
   * @property {uint8} tag Tag generated by the source of the value that was extracted was extracted.
   * @property {bytes} value The extracted value as encoded bytes.
   */
  struct Obj {
    uint32 selector;
    uint8 tag;
    bytes value;
  }

  /* Selector used to identify Ethereum types. */
  uint32 public constant selectorAddress = uint32(bytes4(keccak256(bytes("address"))));
  uint32 public constant selectorBool = uint32(bytes4(keccak256(bytes("bool"))));
  uint32 public constant selectorBytes1 = uint32(bytes4(keccak256(bytes("bytes1"))));
  uint32 public constant selectorBytes2 = uint32(bytes4(keccak256(bytes("bytes2"))));
  uint32 public constant selectorBytes3 = uint32(bytes4(keccak256(bytes("bytes3"))));
  uint32 public constant selectorBytes4 = uint32(bytes4(keccak256(bytes("bytes4"))));
  uint32 public constant selectorBytes5 = uint32(bytes4(keccak256(bytes("bytes5"))));
  uint32 public constant selectorBytes6 = uint32(bytes4(keccak256(bytes("bytes6"))));
  uint32 public constant selectorBytes7 = uint32(bytes4(keccak256(bytes("bytes7"))));
  uint32 public constant selectorBytes8 = uint32(bytes4(keccak256(bytes("bytes8"))));
  uint32 public constant selectorBytes9 = uint32(bytes4(keccak256(bytes("bytes9"))));
  uint32 public constant selectorBytes10 = uint32(bytes4(keccak256(bytes("bytes10"))));
  uint32 public constant selectorBytes11 = uint32(bytes4(keccak256(bytes("bytes11"))));
  uint32 public constant selectorBytes12 = uint32(bytes4(keccak256(bytes("bytes12"))));
  uint32 public constant selectorBytes13 = uint32(bytes4(keccak256(bytes("bytes13"))));
  uint32 public constant selectorBytes14 = uint32(bytes4(keccak256(bytes("bytes14"))));
  uint32 public constant selectorBytes15 = uint32(bytes4(keccak256(bytes("bytes15"))));
  uint32 public constant selectorBytes16 = uint32(bytes4(keccak256(bytes("bytes16"))));
  uint32 public constant selectorBytes17 = uint32(bytes4(keccak256(bytes("bytes17"))));
  uint32 public constant selectorBytes18 = uint32(bytes4(keccak256(bytes("bytes18"))));
  uint32 public constant selectorBytes19 = uint32(bytes4(keccak256(bytes("bytes19"))));
  uint32 public constant selectorBytes20 = uint32(bytes4(keccak256(bytes("bytes20"))));
  uint32 public constant selectorBytes21 = uint32(bytes4(keccak256(bytes("bytes21"))));
  uint32 public constant selectorBytes22 = uint32(bytes4(keccak256(bytes("bytes22"))));
  uint32 public constant selectorBytes23 = uint32(bytes4(keccak256(bytes("bytes23"))));
  uint32 public constant selectorBytes24 = uint32(bytes4(keccak256(bytes("bytes24"))));
  uint32 public constant selectorBytes25 = uint32(bytes4(keccak256(bytes("bytes25"))));
  uint32 public constant selectorBytes26 = uint32(bytes4(keccak256(bytes("bytes26"))));
  uint32 public constant selectorBytes27 = uint32(bytes4(keccak256(bytes("bytes27"))));
  uint32 public constant selectorBytes28 = uint32(bytes4(keccak256(bytes("bytes28"))));
  uint32 public constant selectorBytes29 = uint32(bytes4(keccak256(bytes("bytes29"))));
  uint32 public constant selectorBytes30 = uint32(bytes4(keccak256(bytes("bytes30"))));
  uint32 public constant selectorBytes31 = uint32(bytes4(keccak256(bytes("bytes31"))));
  uint32 public constant selectorBytes32 = uint32(bytes4(keccak256(bytes("bytes32"))));
  uint32 public constant selectorBytes = uint32(bytes4(keccak256(bytes("bytes"))));
  uint32 public constant selectorUInt8 = uint32(bytes4(keccak256(bytes("uint8"))));
  uint32 public constant selectorUInt16 = uint32(bytes4(keccak256(bytes("uint16"))));
  uint32 public constant selectorUInt24 = uint32(bytes4(keccak256(bytes("uint24"))));
  uint32 public constant selectorUInt32 = uint32(bytes4(keccak256(bytes("uint32"))));
  uint32 public constant selectorUInt40 = uint32(bytes4(keccak256(bytes("uint40"))));
  uint32 public constant selectorUInt48 = uint32(bytes4(keccak256(bytes("uint48"))));
  uint32 public constant selectorUInt56 = uint32(bytes4(keccak256(bytes("uint56"))));
  uint32 public constant selectorUInt64 = uint32(bytes4(keccak256(bytes("uint64"))));
  uint32 public constant selectorUInt72 = uint32(bytes4(keccak256(bytes("uint72"))));
  uint32 public constant selectorUInt80 = uint32(bytes4(keccak256(bytes("uint80"))));
  uint32 public constant selectorUInt88 = uint32(bytes4(keccak256(bytes("uint88"))));
  uint32 public constant selectorUInt96 = uint32(bytes4(keccak256(bytes("uint96"))));
  uint32 public constant selectorUInt104 = uint32(bytes4(keccak256(bytes("uint104"))));
  uint32 public constant selectorUInt112 = uint32(bytes4(keccak256(bytes("uint112"))));
  uint32 public constant selectorUInt120 = uint32(bytes4(keccak256(bytes("uint120"))));
  uint32 public constant selectorUInt128 = uint32(bytes4(keccak256(bytes("uint128"))));
  uint32 public constant selectorUInt136 = uint32(bytes4(keccak256(bytes("uint136"))));
  uint32 public constant selectorUInt144 = uint32(bytes4(keccak256(bytes("uint144"))));
  uint32 public constant selectorUInt152 = uint32(bytes4(keccak256(bytes("uint152"))));
  uint32 public constant selectorUInt160 = uint32(bytes4(keccak256(bytes("uint160"))));
  uint32 public constant selectorUInt168 = uint32(bytes4(keccak256(bytes("uint168"))));
  uint32 public constant selectorUInt176 = uint32(bytes4(keccak256(bytes("uint176"))));
  uint32 public constant selectorUInt184 = uint32(bytes4(keccak256(bytes("uint184"))));
  uint32 public constant selectorUInt192 = uint32(bytes4(keccak256(bytes("uint192"))));
  uint32 public constant selectorUInt200 = uint32(bytes4(keccak256(bytes("uint200"))));
  uint32 public constant selectorUInt208 = uint32(bytes4(keccak256(bytes("uint208"))));
  uint32 public constant selectorUInt216 = uint32(bytes4(keccak256(bytes("uint216"))));
  uint32 public constant selectorUInt224 = uint32(bytes4(keccak256(bytes("uint224"))));
  uint32 public constant selectorUInt232 = uint32(bytes4(keccak256(bytes("uint232"))));
  uint32 public constant selectorUInt240 = uint32(bytes4(keccak256(bytes("uint240"))));
  uint32 public constant selectorUInt248 = uint32(bytes4(keccak256(bytes("uint248"))));
  uint32 public constant selectorUInt256 = uint32(bytes4(keccak256(bytes("uint256"))));
  uint32 public constant selectorInt8 = uint32(bytes4(keccak256(bytes("int8"))));
  uint32 public constant selectorInt16 = uint32(bytes4(keccak256(bytes("int16"))));
  uint32 public constant selectorInt24 = uint32(bytes4(keccak256(bytes("int24"))));
  uint32 public constant selectorInt32 = uint32(bytes4(keccak256(bytes("int32"))));
  uint32 public constant selectorInt40 = uint32(bytes4(keccak256(bytes("int40"))));
  uint32 public constant selectorInt48 = uint32(bytes4(keccak256(bytes("int48"))));
  uint32 public constant selectorInt56 = uint32(bytes4(keccak256(bytes("int56"))));
  uint32 public constant selectorInt64 = uint32(bytes4(keccak256(bytes("int64"))));
  uint32 public constant selectorInt72 = uint32(bytes4(keccak256(bytes("int72"))));
  uint32 public constant selectorInt80 = uint32(bytes4(keccak256(bytes("int80"))));
  uint32 public constant selectorInt88 = uint32(bytes4(keccak256(bytes("int88"))));
  uint32 public constant selectorInt96 = uint32(bytes4(keccak256(bytes("int96"))));
  uint32 public constant selectorInt104 = uint32(bytes4(keccak256(bytes("int104"))));
  uint32 public constant selectorInt112 = uint32(bytes4(keccak256(bytes("int112"))));
  uint32 public constant selectorInt120 = uint32(bytes4(keccak256(bytes("int120"))));
  uint32 public constant selectorInt128 = uint32(bytes4(keccak256(bytes("int128"))));
  uint32 public constant selectorInt136 = uint32(bytes4(keccak256(bytes("int136"))));
  uint32 public constant selectorInt144 = uint32(bytes4(keccak256(bytes("int144"))));
  uint32 public constant selectorInt152 = uint32(bytes4(keccak256(bytes("int152"))));
  uint32 public constant selectorInt160 = uint32(bytes4(keccak256(bytes("int160"))));
  uint32 public constant selectorInt168 = uint32(bytes4(keccak256(bytes("int168"))));
  uint32 public constant selectorInt176 = uint32(bytes4(keccak256(bytes("int176"))));
  uint32 public constant selectorInt184 = uint32(bytes4(keccak256(bytes("int184"))));
  uint32 public constant selectorInt192 = uint32(bytes4(keccak256(bytes("int192"))));
  uint32 public constant selectorInt200 = uint32(bytes4(keccak256(bytes("int200"))));
  uint32 public constant selectorInt208 = uint32(bytes4(keccak256(bytes("int208"))));
  uint32 public constant selectorInt216 = uint32(bytes4(keccak256(bytes("int216"))));
  uint32 public constant selectorInt224 = uint32(bytes4(keccak256(bytes("int224"))));
  uint32 public constant selectorInt232 = uint32(bytes4(keccak256(bytes("int232"))));
  uint32 public constant selectorInt240 = uint32(bytes4(keccak256(bytes("int240"))));
  uint32 public constant selectorInt248 = uint32(bytes4(keccak256(bytes("int248"))));
  uint32 public constant selectorInt256 = uint32(bytes4(keccak256(bytes("int256"))));
  uint32 public constant selectorString = uint32(bytes4(keccak256(bytes("string"))));
  uint32 public constant selectorObjArray = uint32(bytes4(keccak256(bytes("Obj[]"))));

  function isAnyIntegerType(uint32 selector) internal pure returns (bool) {
    return (selector == Object.selectorInt8 ||
    selector == Object.selectorInt16 ||
    selector == Object.selectorInt24 ||
    selector == Object.selectorInt32 ||
    selector == Object.selectorInt40 ||
    selector == Object.selectorInt48 ||
    selector == Object.selectorInt56 ||
    selector == Object.selectorInt64 ||
    selector == Object.selectorInt72 ||
    selector == Object.selectorInt80 ||
    selector == Object.selectorInt88 ||
    selector == Object.selectorInt96 ||
    selector == Object.selectorInt104 ||
    selector == Object.selectorInt112 ||
    selector == Object.selectorInt120 ||
    selector == Object.selectorInt128 ||
    selector == Object.selectorInt136 ||
    selector == Object.selectorInt144 ||
    selector == Object.selectorInt152 ||
    selector == Object.selectorInt160 ||
    selector == Object.selectorInt168 ||
    selector == Object.selectorInt176 ||
    selector == Object.selectorInt184 ||
    selector == Object.selectorInt192 ||
    selector == Object.selectorInt200 ||
    selector == Object.selectorInt208 ||
    selector == Object.selectorInt216 ||
    selector == Object.selectorInt224 ||
    selector == Object.selectorInt232 ||
    selector == Object.selectorInt240 ||
    selector == Object.selectorInt248 ||
    selector == Object.selectorInt256);
  }

  function isAnyUnsignedIntegerType(uint32 selector) internal pure returns (bool) {
    return (selector == Object.selectorUInt8 ||
    selector == Object.selectorUInt16 ||
    selector == Object.selectorUInt24 ||
    selector == Object.selectorUInt32 ||
    selector == Object.selectorUInt40 ||
    selector == Object.selectorUInt48 ||
    selector == Object.selectorUInt56 ||
    selector == Object.selectorUInt64 ||
    selector == Object.selectorUInt72 ||
    selector == Object.selectorUInt80 ||
    selector == Object.selectorUInt88 ||
    selector == Object.selectorUInt96 ||
    selector == Object.selectorUInt104 ||
    selector == Object.selectorUInt112 ||
    selector == Object.selectorUInt120 ||
    selector == Object.selectorUInt128 ||
    selector == Object.selectorUInt136 ||
    selector == Object.selectorUInt144 ||
    selector == Object.selectorUInt152 ||
    selector == Object.selectorUInt160 ||
    selector == Object.selectorUInt168 ||
    selector == Object.selectorUInt176 ||
    selector == Object.selectorUInt184 ||
    selector == Object.selectorUInt192 ||
    selector == Object.selectorUInt200 ||
    selector == Object.selectorUInt208 ||
    selector == Object.selectorUInt216 ||
    selector == Object.selectorUInt224 ||
    selector == Object.selectorUInt232 ||
    selector == Object.selectorUInt240 ||
    selector == Object.selectorUInt248 ||
    selector == Object.selectorUInt256);
  }

  function isAnyFixedLengthBytesType(uint32 selector) internal pure returns (bool) {
    return (selector == Object.selectorBytes1 ||
    selector == Object.selectorBytes2 ||
    selector == Object.selectorBytes3 ||
    selector == Object.selectorBytes4 ||
    selector == Object.selectorBytes5 ||
    selector == Object.selectorBytes6 ||
    selector == Object.selectorBytes7 ||
    selector == Object.selectorBytes8 ||
    selector == Object.selectorBytes9 ||
    selector == Object.selectorBytes10 ||
    selector == Object.selectorBytes11 ||
    selector == Object.selectorBytes12 ||
    selector == Object.selectorBytes13 ||
    selector == Object.selectorBytes14 ||
    selector == Object.selectorBytes15 ||
    selector == Object.selectorBytes16 ||
    selector == Object.selectorBytes17 ||
    selector == Object.selectorBytes18 ||
    selector == Object.selectorBytes19 ||
    selector == Object.selectorBytes20 ||
    selector == Object.selectorBytes21 ||
    selector == Object.selectorBytes22 ||
    selector == Object.selectorBytes23 ||
    selector == Object.selectorBytes24 ||
    selector == Object.selectorBytes25 ||
    selector == Object.selectorBytes26 ||
    selector == Object.selectorBytes27 ||
    selector == Object.selectorBytes28 ||
    selector == Object.selectorBytes29 ||
    selector == Object.selectorBytes30 ||
    selector == Object.selectorBytes31 ||
    selector == Object.selectorBytes32);
  }

  function extractFromCalldata(
    string memory calldataTypes, // x, where functionPrototype is func(x)
    bytes memory calldataPath,
    uint256 calldataReferenceOffset,
    bytes memory _calldata
  ) internal pure returns (Object.Obj memory extractedObject) {
    // find all top-level types
    string[] memory calldataTypeTokens = SolUtils.StringTokenise(calldataTypes, ",");
    string[] memory topLevelTypes = Object.getTopLevelTypes(calldataTypeTokens);
    uint256 topLevelTypeIndex = uint256(uint8(calldataPath[0]));
    uint256 topLevelTypeOffset = 0;
    // calculate the calldata offset due to all parameters prior to the target parameter
    for (uint256 i = 0; i < topLevelTypeIndex; ++i) {
      if (Object.isDynamicType(topLevelTypes[i])) {
        topLevelTypeOffset += 32;
      } else {
        if (Object.isTupleType(topLevelTypes[i])) {
          // statically sized tuple
          revert(string(abi.encodePacked(
            "Statically sized tuple type=[",
            topLevelTypes[i],
            "] is not supported."
          )));
          //topLevelTypeOffset += Object.getStaticTupleSize(topLevelTypes[i]);
        } else if (Object.isArrayType(topLevelTypes[i])) {
          // statically sized array
          revert(string(abi.encodePacked(
            "Statically sized array type=[",
            topLevelTypes[i],
            "] is not supported."
          )));
        } else {
          topLevelTypeOffset += 32;
        }
      }
    }
    string memory targetTopLevelType = topLevelTypes[topLevelTypeIndex];
    if (Object.isTupleType(targetTopLevelType)) {
      if (!(calldataPath.length > 1)) {
        revert(string(abi.encodePacked(
          "Expected calldataPath=[",
          SolUtils.BytesToHexString(calldataPath),
          "] to contain more elements for nested type=[",
          targetTopLevelType,
          "]."
        )));
      }
      // determine the starting point of the nested type
      bytes memory indicator = SolUtils.ByteSlice(
        _calldata,
        calldataReferenceOffset + topLevelTypeOffset,
        calldataReferenceOffset + topLevelTypeOffset + 32
      );
      uint256 nestedTupleTypeOffset = abi.decode(indicator, (uint256));
      return
      Object.extractFromCalldata(
        SolUtils.StringSlice(targetTopLevelType, 1, SolUtils.StringLength(targetTopLevelType) - 1), // remove ( and )
        SolUtils.ByteSlice(calldataPath, 1, calldataPath.length), // remove first path index
        calldataReferenceOffset + nestedTupleTypeOffset, // this becomes the new reference offset
        _calldata
      );
    } else {
      return
      Object.extractParameterFromCalldata(targetTopLevelType, calldataReferenceOffset, topLevelTypeOffset, _calldata);
    }
  }

  function getTopLevelTypes(string[] memory calldataTypeTokens) internal pure returns (string[] memory) {
    string[] memory topLevelTypes = new string[](calldataTypeTokens.length); // we won't use all the slots if there is a tuple
    uint256 topLevelTypeIndex = 0;
    for (uint256 i = 0; i < calldataTypeTokens.length; ++i) {
      string memory _type = "";
      uint256 j = 0;
      uint256 matchingTokenIndex = Object.getMatchingTokenIndex(calldataTypeTokens, i);
      if (matchingTokenIndex == i) {
        _type = calldataTypeTokens[i];
      } else {
        for (j = i; j <= matchingTokenIndex; ++j) {
          if (j > i) {
            _type = SolUtils.StringConcat(_type, string(","));
          }
          _type = SolUtils.StringConcat(_type, calldataTypeTokens[j]);
        }
        i = j - 1;
      }
      topLevelTypes[topLevelTypeIndex++] = _type;
    }
    return topLevelTypes;
  }

  function getMatchingTokenIndex(
    string[] memory calldataTypeTokens,
    uint256 currentIndex
  ) internal pure returns (uint256) {
    // step to find the either ( or )
    uint256 numOpenBrackets = 0;
    uint256 numClosingBrackets = 0;
    for (uint256 i = currentIndex; i < calldataTypeTokens.length; ++i) {
      numOpenBrackets += SolUtils.StringIncludeCount(calldataTypeTokens[i], "(");
      numClosingBrackets += SolUtils.StringIncludeCount(calldataTypeTokens[i], ")");
      if (numOpenBrackets == 0) {
        return currentIndex; // current token not a tuple
      } else if (numClosingBrackets == numOpenBrackets) {
        return i;
      } else if (numClosingBrackets > numOpenBrackets) {
        // too many closing brackets found
        revert(string(abi.encodePacked(
          "Calldata parameter types contains too many closing brackets."
        )));
      } // otherwise continue
    }
    // not enough closing brackets found
    revert(string(abi.encodePacked(
      "Calldata parameter types contains too many opening brackets."
    )));
  }

  function extractParameterFromCalldata(
    string memory parameterType,
    uint256 callDataReferenceOffset,
    uint256 callDataParameterOffset,
    bytes memory callData
  ) internal pure returns (Object.Obj memory extractedObject) {
    // Extract 32-byte aligned bytes for i-th parameter according to the parameterIndex
    bytes memory indicator = SolUtils.ByteSlice(
      callData,
      callDataReferenceOffset + callDataParameterOffset,
      callDataReferenceOffset + callDataParameterOffset + 32
    );
    uint32 selector = uint32(bytes4(keccak256(bytes(parameterType))));
    bytes memory extractedBytes;
    if (
    // Handling uint<M> where enc(X) is the big-endian encoding of X,
    // padded on the higher-order (left) side with zero-bytes such that the length is 32 bytes.
      Object.isAnyUnsignedIntegerType(selector) ||
      // Handling address where as in the uint160 case.
      selector == Object.selectorAddress ||
      // Handling bool where as in the uint8 case, where 1 is used for true and 0 for false.
      selector == Object.selectorBool ||
      // Handling int<M> where enc(X) is the big-endian two's complement encoding of X,
      // padded on the higher-order (left) side with 0xff bytes for negative X and with
      // zero-bytes for non-negative X such that the length is 32 bytes.
      Object.isAnyIntegerType(selector) ||
      // Handling bytes<M> where enc(X) is the sequence of bytes in X padded with
      // trailing zero-bytes to a length of 32 bytes.
      Object.isAnyFixedLengthBytesType(selector)
    ) {
      extractedBytes = indicator;
      extractedObject = Object.fromEncodedBytes(parameterType, extractedBytes);
    } else if (
    // Handling string where enc(X) = enc(enc_utf8(X)), i.e. X is UTF-8 encoded
    // and this value is interpreted as of bytes type and encoded further.
    // Note that the length used in this subsequent encoding is the number
    // of bytes of the UTF-8 encoded string, not its number of characters.
      selector == Object.selectorString ||
      // Handling bytes, of length k (which is assumed to be of type uint256),
      // where enc(X) = enc(k) pad_right(X), i.e. the number of bytes is encoded
      // as a uint256 followed by the actual value of X as a byte sequence,
      // followed by the minimum number of zero-bytes such that len(enc(X)) is a multiple of 32.
      selector == Object.selectorBytes
    ) {
      uint256 pos = abi.decode(indicator, (uint256));
      uint256 size = abi.decode(
        SolUtils.ByteSlice(callData, callDataReferenceOffset + pos, callDataReferenceOffset + pos + 32),
        (uint256)
      );
      extractedBytes = SolUtils.ByteSlice(
        callData,
        callDataReferenceOffset + pos,
        callDataReferenceOffset + pos + 32 + size + ((32 - (size % 32)) % 32)
      );
      bytes memory prefix = hex"0000000000000000000000000000000000000000000000000000000000000020";
      extractedBytes = bytes.concat(prefix, extractedBytes);
      extractedObject = Object.fromEncodedBytes(parameterType, extractedBytes);
    } else {
      revert(string(abi.encodePacked(
        "Parameter type = [",
        parameterType,
        "] not supported."
      )));
    }
    return extractedObject;
  }

  function isTupleType(string memory parameterType) internal pure returns (bool) {
    return (SolUtils.StringIncludeCount(parameterType, "(") > 0 && SolUtils.StringIncludeCount(parameterType, ")") > 0);
  }

  function isArrayType(string memory parameterType) internal pure returns (bool) {
    return (SolUtils.StringIncludeCount(parameterType, "[") > 0 && SolUtils.StringIncludeCount(parameterType, "]") > 0);
  }

  function isDynamicType(string memory parameterType) internal pure returns (bool) {
    uint32 selector = uint32(bytes4(keccak256(bytes(parameterType))));
    // string and bytes are the only two dynamic basic types currently supported
    if (selector == Object.selectorString || selector == Object.selectorBytes) {
      return true;
    } else if (Object.isTupleType(parameterType)) {
      bool isDynamic = false;
      string[] memory trimmedTupleTypes = SolUtils.StringTokenise(
        SolUtils.StringSlice(parameterType, 1, SolUtils.StringLength(parameterType) - 1), // remove ( and )
        ","
      );
      string[] memory memberTypes = Object.getTopLevelTypes(trimmedTupleTypes);
      for (uint256 i = 0; i < memberTypes.length; ++i) {
        // if a tuple contains any dynamic types, the tuple itself is regarded as dynamic as well
        if (Object.isDynamicType(memberTypes[i])) {
          return true;
        }
      }
      return isDynamic;
    } else if (Object.isArrayType(parameterType)) {
      revert(string(abi.encodePacked(
        "Parameter type=[",
        parameterType,
        "] is not supported."
      )));
    }
    return false;
  }

  /* Helper to marshal into object from encoded bytes. */
  function fromEncodedBytes(string memory typ, bytes memory encodedBytes) internal pure returns (Obj memory) {
    Obj memory obj;
    uint32 selector = uint32(bytes4(keccak256(bytes(typ))));
    obj.value = encodedBytes;
    obj.selector = selector;
    return obj;
  }

  /* Helper function to determine if two objects are equal. */
  function isEqual(Obj memory obj, Obj memory other) internal pure returns (bool) {
    if (
      obj.selector == other.selector && // obj.tag == other.tag &&
      keccak256(abi.encode(obj.value)) == keccak256(abi.encode(other.value))
    ) {
      return true;
    }
    return false;
  }

  /* Helper function to convert an object into another of given type. */
  function convertTo(Obj memory obj, uint32 toSelector) internal pure returns (bool) {
    if (obj.selector == selectorString && toSelector == selectorUInt256) {
      setUInt256(obj, SolUtils.StringToUInt(getString(obj)));
      return true;
    } else if (obj.selector == selectorUInt256 && toSelector == selectorString) {
      setString(obj, SolUtils.UIntToString(getUInt256(obj)));
      return true;
    } else if (obj.selector == selectorBytes && toSelector == selectorString) {
      setString(obj, SolUtils.BytesToHexString(getBytes(obj)));
    }
    return false;
  }

  /* Helper function to get the size of the object's underlying type. */
  function getSize(string memory typ) internal pure returns (uint256) {
    uint32 selector = uint32(bytes4(keccak256(bytes(typ))));
    if (selector == selectorUInt8) {
      return 32;
    }
    // TODO: Complete
    return 0;
  }

  /* Helper function to check if the object's underlying type is a dynamic type. */
  function isDynamic(string memory typ) internal pure returns (bool) {
    uint32 selector = uint32(bytes4(keccak256(bytes(typ))));
    if (selector == selectorString) {
      return true;
    }
    // TODO: Complete
    return false;
  }

  /* Getter function to decode the object as an array of objects. */
  function getArray(Obj memory obj) internal pure returns (Obj[] memory) {
    return abi.decode(obj.value, (Obj[]));
  }

  /* Setter function to encode the object as an array of objects. */
  function setArray(Obj memory obj, Obj[] memory value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorObjArray;
  }

  /* Getter function to decode the object as a bool. */
  function getBool(Obj memory obj) internal pure returns (bool) {
    return abi.decode(obj.value, (bool));
  }

  /* Setter function to encode the object as a bool. */
  function setBool(Obj memory obj, bool value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorBool;
  }

  /* Getter function to decode the object as bytes1. */
  function getBytes1(Obj memory obj) internal pure returns (bytes1) {
    return abi.decode(obj.value, (bytes1));
  }

  /* Setter function to encode the object as bytes1. */
  function setBytes1(Obj memory obj, bytes1 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorBytes1;
  }

  /* Getter function to decode the object as bytes4. */
  function getBytes4(Obj memory obj) internal pure returns (bytes4) {
    return abi.decode(obj.value, (bytes4));
  }

  /* Setter function to encode the object as bytes4. */
  function setBytes4(Obj memory obj, bytes4 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorBytes4;
  }

  /* Getter function to decode the object as bytes8. */
  function getBytes8(Obj memory obj) internal pure returns (bytes8) {
    return abi.decode(obj.value, (bytes8));
  }

  /* Setter function to encode the object as bytes8. */
  function setBytes8(Obj memory obj, bytes8 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorBytes8;
  }

  /* Getter function to decode the object as bytes32. */
  function getBytes32(Obj memory obj) internal pure returns (bytes32) {
    return abi.decode(obj.value, (bytes32));
  }

  /* Setter function to encode the object as bytes32. */
  function setBytes32(Obj memory obj, bytes32 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorBytes32;
  }

  /* Getter function to decode the object as bytes. */
  function getBytes(Obj memory obj) internal pure returns (bytes memory) {
    return abi.decode(obj.value, (bytes));
  }

  /* Setter function to encode the object as bytes. */
  function setBytes(Obj memory obj, bytes memory value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorBytes;
  }

  /* Getter function to decode the object as a int16. */
  function getInt16(Obj memory obj) internal pure returns (int16) {
    return abi.decode(obj.value, (int16));
  }

  /* Setter function to encode the object as a int16. */
  function setInt16(Obj memory obj, int16 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorInt16;
  }

  /* Getter function to decode the object as a int32. */
  function getInt32(Obj memory obj) internal pure returns (int32) {
    return abi.decode(obj.value, (int32));
  }

  /* Setter function to encode the object as a int32. */
  function setInt32(Obj memory obj, int32 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorInt32;
  }

  /* Getter function to decode the object as a int64. */
  function getInt64(Obj memory obj) internal pure returns (int64) {
    return abi.decode(obj.value, (int64));
  }

  /* Setter function to encode the object as a int64. */
  function setInt64(Obj memory obj, int64 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorInt64;
  }

  /* Getter function to decode the object as a int128. */
  function getInt128(Obj memory obj) internal pure returns (int128) {
    return abi.decode(obj.value, (int128));
  }

  /* Setter function to encode the object as a int128. */
  function setInt128(Obj memory obj, int128 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorInt128;
  }

  /* Getter function to decode the object as a int256. */
  function getInt256(Obj memory obj) internal pure returns (int256) {
    return abi.decode(obj.value, (int256));
  }

  /* Setter function to encode the object as a int256. */
  function setInt256(Obj memory obj, int256 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorInt256;
  }

  /* Getter function to decode the object as a string. */
  function getString(Obj memory obj) internal pure returns (string memory) {
    string memory decoded = abi.decode(obj.value, (string));
    return decoded;
  }

  /* Setter function to encode the object as a string. */
  function setString(Obj memory obj, string memory value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorString;
  }

  /* Getter function to decode the object as a uint8. */
  function getUInt8(Obj memory obj) internal pure returns (uint8) {
    return abi.decode(obj.value, (uint8));
  }

  /* Setter function to encode the object as a uint8. */
  function setUInt8(Obj memory obj, uint8 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorUInt8;
  }

  /* Getter function to decode the object as a uint16. */
  function getUInt16(Obj memory obj) internal pure returns (uint16) {
    return abi.decode(obj.value, (uint16));
  }

  /* Setter function to encode the object as a uint16. */
  function setUInt16(Obj memory obj, uint16 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorUInt16;
  }

  /* Getter function to decode the object as a uint32. */
  function getUInt32(Obj memory obj) internal pure returns (uint32) {
    return abi.decode(obj.value, (uint32));
  }

  /* Setter function to encode the object as a uint32. */
  function setUInt32(Obj memory obj, uint32 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorUInt32;
  }

  /* Getter function to decode the object as a uint64. */
  function getUInt64(Obj memory obj) internal pure returns (uint64) {
    return abi.decode(obj.value, (uint64));
  }

  /* Setter function to encode the object as a uint64. */
  function setUInt64(Obj memory obj, uint64 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorUInt64;
  }

  /* Getter function to decode the object as a uint256. */
  function getUInt256(Obj memory obj) internal pure returns (uint256) {
    return abi.decode(obj.value, (uint256));
  }

  /* Setter function to encode the object as a uint256. */
  function setUInt256(Obj memory obj, uint256 value) internal pure {
    obj.value = abi.encode(value);
    obj.selector = selectorUInt256;
  }
}
