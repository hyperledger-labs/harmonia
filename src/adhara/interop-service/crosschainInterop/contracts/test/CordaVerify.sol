/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;

import "contracts/libraries/Corda.sol";
import "contracts/libraries/SolidityUtils.sol";

contract CordaVerify {
  using Object for Object.Obj;
  using AMQP for AMQP.Buffer;
  using Parser for Parser.Parsed;

  mapping(uint32 => Corda.ParameterHandler[]) parameterHandlers;

  function verifyBoolObject() public returns (bool) {
    Object.Obj memory obj;
    obj.setBool(true);
    bool result = obj.getBool();
    require(result == true, "Failed to set boolean value to true");
    return true;
  }

  function verifyArrayObject() public returns (bool) {
    Object.Obj memory item;
    item.setInt16(int16(255));
    Object.Obj[] memory items = new Object.Obj[](1);
    items[0] = item;
    Object.Obj memory obj;
    obj.setArray(items);
    Object.Obj[] memory results = obj.getArray();
    require(results.length == 1, "Failed to return array value of length 1");
    Object.Obj memory result = results[0];
    require(result.getInt16() == int16(255), "Failed to return first array value");
    return true;
  }

  function calculateComponentHash(
    uint8 groupIndex,
    uint8 internalIndex,
    bytes32 privacySalt,
    bytes memory encodedComponent
  ) public view returns (bytes32) {
    return Corda.calculateComponentHash(Corda.ComponentData(groupIndex, internalIndex, encodedComponent), privacySalt);
  }

  function calculateNonce(
    uint8 groupIndex,
    uint8 internalIndex,
    bytes32 privacySalt
  ) public view returns (bytes32) {
    return Corda.calculateNonce(groupIndex, internalIndex, privacySalt);
  }

  function validateEvent(
    bytes memory eventData,
    string memory functionPrototype,
    bytes memory paramHandlers,
    string memory functionCommand,
    bytes memory proofData,
    bytes memory txSignatures
  ) public returns (bool) {
    Corda.EventData memory data = abi.decode(eventData, (Corda.EventData));
    Corda.ParameterHandler[] memory handlers = abi.decode(paramHandlers, (Corda.ParameterHandler[]));
    Corda.ProofData memory proof = abi.decode(proofData, (Corda.ProofData));
    Corda.Signature[] memory sigs = abi.decode(txSignatures, (Corda.Signature[]));
    return Corda.validateEvent(Corda.ValidationData(data, functionPrototype, functionCommand, handlers, proof, sigs));
  }

  function extractByFingerprint(bytes memory value, bytes memory callParameters, string memory functionPrototype, bytes memory paramHandlers) external pure returns (string[] memory) {
    Corda.ParameterHandler[] memory handlers = abi.decode(paramHandlers, (Corda.ParameterHandler[]));
    Object.Obj[] memory extracted = Corda.extractParameters(callParameters, functionPrototype, handlers);
    Object.Obj[] memory parsed = Corda.extractByFingerprint(value, handlers);
    string[] memory result = new string[](handlers.length);
    for (uint256 i = 0; i < handlers.length; i++) {
      if ((extracted[i].selector != parsed[i].selector) && (!parsed[i].convertTo(extracted[i].selector)))
        revert("Failed to convert parsed object");
      if (parsed[i].selector == Object.selectorUInt256) result[i] = SolUtils.UIntToString(parsed[i].getUInt256());
      if (parsed[i].selector == Object.selectorString) result[i] = parsed[i].getString();
      if (parsed[i].selector == Object.selectorBytes) result[i] = SolUtils.BytesToHexString(parsed[i].getBytes());
      if (parsed[i].selector == Object.selectorBytes4) result[i] = SolUtils.Bytes4ToHexString(parsed[i].getBytes4());
    }
    return result;
  }

  function extractParameters(
    bytes memory callParameters,
    string memory functionPrototype,
    bytes memory paramHandlers
  ) external view returns (string[] memory) {
    Corda.ParameterHandler[] memory handlers = abi.decode(paramHandlers, (Corda.ParameterHandler[]));
    Object.Obj[] memory extracted = Corda.extractParameters(callParameters, functionPrototype, handlers);
    string[] memory result = new string[](extracted.length);
    for (uint256 i = 0; i < extracted.length; i++) {
      if (extracted[i].selector == Object.selectorUInt256) result[i] = SolUtils.UIntToString(extracted[i].getUInt256());
      if (extracted[i].selector == Object.selectorString) result[i] = extracted[i].getString();
      if (extracted[i].selector == Object.selectorBytes) result[i] = SolUtils.BytesToHexString(extracted[i].getBytes());
      if (extracted[i].selector == Object.selectorBytes4) result[i] = SolUtils.Bytes4ToHexString(extracted[i].getBytes4());
    }
    return result;
  }

  function getMatchingTokenIndex(string[] memory calldataTypeTokens, uint256 currentIndex) external view returns (uint256 matchingTokenIndex) {
    return Object.getMatchingTokenIndex(calldataTypeTokens, currentIndex);
  }

  function getTopLevelTypes(string[] memory calldataTypeTokens) external view returns (string[] memory types) {
    return Object.getTopLevelTypes(calldataTypeTokens);
  }

  function extractPublicKeys(bytes memory evtData) external view returns (string[] memory) {
    Corda.EventData memory eventData = abi.decode(evtData, (Corda.EventData));
    string[] memory extracted = Corda.extractPublicKeys(eventData.componentData[0].encodedBytes);
    return extracted;
  }

  function extractCommands(bytes memory evtData) external view returns (string[] memory) {
    Corda.EventData memory eventData = abi.decode(evtData, (Corda.EventData));
    string[] memory extracted = Corda.extractCommands(eventData.componentData[0].encodedBytes);
    return extracted;
  }
}





