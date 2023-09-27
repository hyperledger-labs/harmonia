/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// Copyright (c) 2016-2018 Clearmatics Technologies Ltd
// SPDX-License-Identifier: LGPL-3.0+
pragma solidity ^0.8.13;

/*
 * A library of funky data manipulation stuff.
 */
library SolUtils {
  /*
  * Extracts 1 byte from the given byte buffer.
  * @param {bytes} input The array from which the data should be extracted.
  * @param {uint256} buf Index in the input array from where the byte needs to be extracted.
  * @return {bytes1} The extracted bytes.
  */
  function BytesToBytes1(bytes memory input, uint256 buf) internal pure returns (bytes1) {
    bytes1 output;
    output |= bytes1(input[buf] & 0xFF);
    return output;
  }

  /*
  * Extract 2 bytes from the given byte buffer.
  * @param {bytes} input The array from which the data should be extracted.
  * @param {uint256} buf Index in the input array from where the bytes needs to be extracted.
  * @return {bytes2} The extracted bytes.
  */
  function BytesToBytes2(bytes memory input, uint256 buf) internal pure returns (bytes2) {
    bytes2 output;
    for (uint i = 0; i < 2; i++) {
      output |= bytes2(input[buf + i] & 0xFF) >> (i * 8);
    }
    return output;
  }

  /*
  * Extract 4 bytes from the given byte buffer.
  * @param {bytes} input The array from which the data should be extracted.
  * @param {uint256} buf Index in the input array from where the bytes needs to be extracted.
  * @return {bytes4} The extracted bytes.
  */
  function BytesToBytes4(bytes memory input, uint256 buf) internal pure returns (bytes4) {
    bytes4 output;
    for (uint i = 0; i < 4; i++) {
      output |= bytes4(input[buf + i] & 0xFF) >> (i * 8);
    }
    return output;
  }

  /*
  * Extract 32 bytes from the given byte buffer.
  * @param {bytes} input The array from which the data should be extracted.
  * @param {uint256} buf Index in the input array from where the bytes needs to be extracted.
  * @return {bytes32} output The extracted bytes.
  */
  function BytesToBytes32(bytes memory input, uint256 buf) internal pure returns (bytes32 output) {
    buf = buf + 32;
    assembly {
      output := mload(add(input, buf))
    }
  }

  /*
  * Extract 20 bytes from the given byte buffer.
  * @param {bytes} input The array from which the data should be extracted.
  * @param {uint256} buf Index in the input array from where the bytes needs to be extracted.
  * @return {byte20} The memory allocation for the data you need to extract.
  */
  function BytesToBytes20(bytes memory input, uint256 buf) internal pure returns (bytes20) {
    bytes20 output;
    for (uint i = 0; i < 20; i++) {
      output |= bytes20(input[buf + i] & 0xFF) >> (i * 8);
    }
    return output;
  }

  /*
  * Extract 20 bytes from given byte buffer and return it as an address.
  * @param {bytes} input The array from which the data should be extracted.
  * @param {uint256} buf Index in the input array from where the bytes needs to be extracted.
  * @return {address} output The extracted bytes converted to an address.
  */
  function BytesToAddress(bytes memory input, uint256 buf) internal pure returns (address output) {
    buf = buf + 20;
    assembly {
      output := mload(add(input, buf))
    }
  }

  bytes16 private constant SYMBOLS = "0123456789abcdef";
  /*
   * Convert 32 bytes to an hex-encode string.
   * @param {bytes32} value The bytes to convert.
   * @return {string} The hex-encoded string.
   */
  function Bytes32ToHexString(bytes32 value) internal pure returns (string memory) {
    bytes memory buffer = new bytes(64);
    for (uint256 i = 0; i < value.length; i++) {
      buffer[i * 2] = SYMBOLS[uint8(value[i]) / SYMBOLS.length];
      buffer[i * 2 + 1] = SYMBOLS[uint8(value[i]) % SYMBOLS.length];
    }
    return string(buffer);
  }

  /*
  * Copies output.length bytes from the input into the output.
  * @param {bytes} output The output array to where the data should be copied.
  * @param {bytes} input The input array from which the data should be copied.
  * @param {uint256} buf Index in the input array where the bytes needs to be copied from.
  */
  function BytesToBytes(bytes memory output, bytes memory input, uint256 buf) view internal {
    uint256 outputLength = output.length;
    buf = buf + 32;
    // Append 32 as we need to point past the variable type definition
    assembly {
      let ret := staticcall(3000, 4, add(input, buf), outputLength, add(output, 32), outputLength)
    }
  }

  /*
   * Converts an 256-bit unsigned integer to a string.
   * @param {uint} i The input 256-bit unsigned integer.
   * @return {string} uintAsString The converted string value.
   */
  function UintToString(uint i) internal pure returns (string memory uintAsString) {
    if (i == 0) {
      return "0";
    }
    uint j = i;
    uint len;
    while (j != 0) {
      len++;
      j /= 10;
    }
    bytes memory bstr = new bytes(len);
    uint k = len - 1;
    while (i != 0) {
      bstr[k--] = bytes1(uint8(48 + i % 10));
      i /= 10;
    }
    return string(bstr);
  }

  /*
   * Converts a boolean to a string.
   * @param {bool} b The input boolean value.
   * @return {string} The converted string value.
   */
  function BoolToString(bool b) internal pure returns (string memory) {
    if (b)
      return "true";
    else
      return "false";
  }

}
