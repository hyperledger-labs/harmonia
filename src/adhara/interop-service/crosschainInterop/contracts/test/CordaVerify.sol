/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;
pragma abicoder v2;

import "contracts/../../contracts/libraries/Corda.sol";

contract CordaVerify {

  using Object for Object.Obj;
  using AMQP for AMQP.Buffer;
  using Parser for Parser.Parsed;

  event Bytes1(bytes1, string);
  event Bytes4(bytes4, string);
  event Bytes8(bytes8, string);
  event Bytes32(bytes32, string);
  event Bytes(bytes, string);
  event Bool(bool, string);
  event UInt8(uint8, string);
  event UInt32(uint32, string);
  event UInt64(uint64, string);
  event UInt256(uint256, string);
  event Address(address, string);
  event String(string, string);

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

  function computeComponentHash(uint8 groupIndex, uint8 internalIndex, bytes32 privacySalt, bytes memory encodedComponent) public returns (bytes32) {
    bytes32 hash = Corda.calculateComponentHash(groupIndex, internalIndex, privacySalt, encodedComponent);
    emit Bytes32(hash, "Hash");
    return hash;
  }

  function calculateComponentHash(uint8 groupIndex, uint8 internalIndex, bytes32 privacySalt, bytes memory encodedComponent) public view returns (bytes32) {
    return Corda.calculateComponentHash(groupIndex, internalIndex, privacySalt, encodedComponent);
  }

  function calculateNonce(uint8 groupIndex, uint8 internalIndex, bytes32 privacySalt) public view returns (bytes32) {
    return Corda.calculateNonce(groupIndex, internalIndex, privacySalt);
  }

  function validateEvent(bytes memory eventData, bytes memory paramHandlers, bytes memory proofData, bytes memory txSignatures) public returns (bool) {
    Corda.EventData memory data = abi.decode(eventData, (Corda.EventData));
    Corda.ParameterHandler[] memory handlers = abi.decode(paramHandlers, (Corda.ParameterHandler[]));
    Corda.ProofData memory proof = abi.decode(proofData, (Corda.ProofData));
    Corda.Signature[] memory sigs = abi.decode(txSignatures, (Corda.Signature[]));
    return Corda.validateEvent(data, handlers, proof, sigs);
  }

  function extractByFingerprint(bytes memory value, bytes memory paramHandlers) public {
    Corda.ParameterHandler[] memory handlers = abi.decode(paramHandlers, (Corda.ParameterHandler[]));
    Object.Obj[] memory parsed = Corda.extractByFingerprint(value, handlers);
    for (uint i=0; i<handlers.length; i++) {
      if (parsed[i].selector == Object.selectorString)
        emit String(parsed[i].getString(), "Result");
    }
  }

  function extractParameters(bytes memory callParameters, bytes memory paramHandlers) external returns (Object.Obj[] memory) {
    Corda.ParameterHandler[] memory handlers = abi.decode(paramHandlers, (Corda.ParameterHandler[]));
    Object.Obj[] memory extracted = Corda.extractParameters(callParameters, handlers);
    for (uint i=0; i<extracted.length; i++) {
      if (extracted[i].selector == Object.selectorUInt256)
        emit UInt256(extracted[i].getUInt256(), "Parameter");
      if (extracted[i].selector == Object.selectorString)
        emit String(extracted[i].getString(), "Parameter");
      if (extracted[i].selector == Object.selectorBytes)
        emit Bytes(extracted[i].getBytes(), "Parameter");
      if (extracted[i].selector == Object.selectorBytes4)
        emit Bytes4(extracted[i].getBytes4(), "Parameter");
    }
    return extracted;
  }
}

