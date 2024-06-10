/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: LGPL-3.0+
pragma solidity ^0.8.13;

/*
 * A library of funky data manipulation stuff.
 * Source: https://github.com/clearmatics/ion/blob/master/contracts/libraries/SolidityUtils.sol under LGPL-3.0+ license
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
    assembly ("memory-safe")  {
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
    assembly ("memory-safe")  {
      output := mload(add(input, buf))
    }
  }

  /*
   * Helper function to convert bytes into hex-encoded bytes.
   * @param {bytes4} i Bytes to be converted.
   * @return {bytes} Returns hex-encoded bytes.
   */
  function Bytes4ToHexBytes(bytes4 i) internal pure returns (bytes memory) {
    return ByteSlice(abi.encodePacked(i), 3, 4);
  }

  bytes16 private constant SYMBOLS = "0123456789abcdef";

  /*
   * Convert 4 bytes to an hex-encoded string.
   * @param {bytes4} value The bytes to convert.
   * @return {string} The hex-encoded string.
   */
  function Bytes4ToHexString(bytes4 value) internal pure returns (string memory) {
    bytes memory buffer = new bytes(2 * value.length);
    for (uint256 i = 0; i < value.length; i++) {
      buffer[i * 2] = SYMBOLS[uint8(value[i]) / SYMBOLS.length];
      buffer[i * 2 + 1] = SYMBOLS[uint8(value[i]) % SYMBOLS.length];
    }
    return string(buffer);
  }

  /*
   * Convert 32 bytes to an hex-encoded string.
   * @param {bytes32} value The bytes to convert.
   * @return {string} The hex-encoded string.
   */
  function Bytes32ToHexString(bytes32 value) internal pure returns (string memory) {
    bytes memory buffer = new bytes(2 * value.length);
    for (uint256 i = 0; i < value.length; i++) {
      buffer[i * 2] = SYMBOLS[uint8(value[i]) / SYMBOLS.length];
      buffer[i * 2 + 1] = SYMBOLS[uint8(value[i]) % SYMBOLS.length];
    }
    return string(buffer);
  }

  /*
   * Convert bytes to an hex-encoded string.
   * @param {bytes} value The bytes to convert.
   * @return {string} The hex-encoded string.
   */
  function BytesToHexString(bytes memory value) internal pure returns (string memory) {
    bytes memory buffer = new bytes(2 * value.length);
    for (uint256 i = 0; i < value.length; i++) {
      buffer[i * 2] = SYMBOLS[uint8(value[i]) / SYMBOLS.length];
      buffer[i * 2 + 1] = SYMBOLS[uint8(value[i]) % SYMBOLS.length];
    }
    return string(buffer);
  }

  /*
   * Converts a hex-encoded ASCII string without 0x prefix to raw bytes.
   * @param {string} s The hex string without 0x prefix to convert.
   * @return {bytes} The decoded bytes.
   */
  function HexStringToBytes(
    string memory s
  ) internal pure returns (bytes memory) {
    bytes memory ss = bytes(s);
    require(ss.length >= 2 && ss.length % 2 == 0,
      string(abi.encodePacked(
        "Hex-encoded ASCII string should be of even length greater than or equal to 2, was given string of length=[",
        UIntToString(ss.length),
        "]."
      ))
    );
    bytes memory r = new bytes(ss.length/2);
    for (uint i=0; i<ss.length/2; ++i) {
      r[i] = bytes1(HexCharacterToUint(ss[2*i]) * 16 + HexCharacterToUint(ss[2*i+1]));
    }
    return r;
  }


  /*
	 * Converts a hex-encoded ASCII byte, or hex character, to its integer representative.
	 * @param {bytes1} c The character to convert.
   * @return {uint8} The integer value for this character.
   */
  function HexCharacterToUint(bytes1 c) internal pure returns (uint8) {
    if (c >= bytes1('0') && c <= bytes1('9')) return uint8(c) - uint8(bytes1('0'));
    if (c >= bytes1('a') && c <= bytes1('f')) return 10 + uint8(c) - uint8(bytes1('a'));
    if (c >= bytes1('A') && c <= bytes1('F')) return 10 + uint8(c) - uint8(bytes1('A'));
    revert(string(abi.encodePacked(
        "Failed to convert hex character=[",
        c,
        "] to a byte."
      )));
  }

  /*
  * Copies output.length bytes from the input into the output.
  * @param {bytes} output The output array to where the data should be copied.
  * @param {bytes} input The input array from which the data should be copied.
  * @param {uint256} buf Index in the input array where the bytes needs to be copied from.
  */
  function BytesToBytes(bytes memory output, bytes memory input, uint256 buf) internal view {
    uint256 outputLength = output.length;
    buf = buf + 32;
    // Append 32 as we need to point past the variable type definition
    assembly ("memory-safe")  {
      let ret := staticcall(3000, 4, add(input, buf), outputLength, add(output, 32), outputLength)
    }
  }

  /*
 * Returns the n byte value at the specified index of the given byte array.
 * @param {bts} The byte array.
	 * @param {index} The index into the byte array.
	 * @param {len} The number of bytes to copy.
	 * @return The extracted n bytes of the array, given as a slice.
	 */
  function BytesToNBytesAtIndex(
    bytes memory bts,
    uint256 idx,
    uint256 len
  ) internal pure returns (bytes memory) {
    return ByteSlice(bts, idx, idx + len);
  }

  /*
   * Returns the n byte value at the specified index of the given byte array as a 32 byte value.
   * @param {bts} The byte array.
	 * @param {idx} The index into the byte array.
	 * @param {len} The number of bytes.
	 * @return The extracted n bytes of the array, given as a 32 byte value.
	 */
  function BytesToNBytes32AtIndex(
    bytes memory bts,
    uint256 idx,
    uint256 len
  ) internal pure returns (bytes32 ret) {
    require(len <= 32);
    require(idx + len <= bts.length);
    assembly ("memory-safe")  {
      let mask := not(sub(exp(256, sub(32, len)), 1))
      ret := and(mload(add(add(bts, 32), idx)), mask)
    }
    return ret;
  }

  /*
   * Returns the 8-bit number at the specified index of the given byte array.
   * @param {bts} The byte array.
	 * @param {idx} The index into the byte array
	 * @return The extracted 8 bits of the array, interpreted as an integer.
	 */
  function BytesToUInt8AtIndex(
    bytes memory bts,
    uint256 idx
  ) internal pure returns (uint8 ret) {
    return uint8(bts[idx]);
  }

  /*
   * Returns the 16-bit number at the specified index of the given byte array.
   * @param {bts} The byte array.
	 * @param {idx} The index into the byte array.
	 * @return The extracted 16 bits of the array, interpreted as an integer.
	 */
  function BytesToUInt16AtIndex(
    bytes memory bts,
    uint256 idx
  ) internal pure returns (uint16 ret) {
    require(idx + 2 <= bts.length);
    assembly ("memory-safe")  {
      ret := and(mload(add(add(bts, 2), idx)), 0xFFFF)
    }
  }

  /*
   * Helper function to get a byte slice.
   * @param {bytes} buf Buffer to extract the slice.
   * @param {uint256} startIndex Starting index.
   * @param {uint256} endIndex ending index.
   * @return {bytes} Returns the extracted byte slice.
   */
  function ByteSlice(
    bytes memory buf,
    uint256 startIndex,
    uint256 endIndex
  ) internal pure returns (bytes memory slicedBytes) {
    if (buf.length == 0) {
      return "";
    }
    uint256 start = startIndex;
    uint256 end = endIndex;
    if (start > buf.length) {
      start = buf.length;
    }
    if (end > buf.length) {
      end = buf.length;
    }
    if (start > end) {
      uint256 tempStartIndex = start;
      start = end;
      end = tempStartIndex;
    }
    slicedBytes = new bytes(end - start);
    uint256 i = start;
    while (i < end) {
      slicedBytes[i - start] = buf[i];
    unchecked {
      ++i;
    }
    }
    return slicedBytes;
  }

  function StringIsEqual(
    string memory s1,
    string memory s2
  ) internal pure returns (bool) {
    return (keccak256(abi.encodePacked(s1)) == keccak256(abi.encodePacked(s2)));
  }

  function StringIsEmpty(
    string memory s
  ) internal pure returns (bool) {
    return StringIsEqual(s, "");
  }

  function StringInArray(
    string[] memory a,
    string memory s
  ) internal pure returns (bool) {
    for (uint i = 0; i < a.length; i++) {
      if (StringIsEqual(a[i], s))
        return true;
    }
    return false;
  }

  function StringLength(
    string memory s
  ) internal pure returns (uint256) {
    return bytes(s).length;
  }

  function AsciiStringLength(
    string memory s
  ) internal pure returns (uint256) {
    uint256 len;
    uint256 i = 0;
    uint256 byteLength = bytes(s).length;
    for (len = 0; i < byteLength; len++) {
      bytes1 b = bytes(s)[i];
      if (b < 0x80) {
        i += 1;
      } else if (b < 0xE0) {
        i += 2;
      } else if (b < 0xF0) {
        i += 3;
      } else if (b < 0xF8) {
        i += 4;
      } else if (b < 0xFC) {
        i += 5;
      } else {
        i += 6;
      }
    }
    return len;
  }

  function StringTokenise(string memory str, bytes1 token) internal pure returns (string[] memory result) {
    if (token == bytes1("")) {
      result = new string[](StringLength(str));
      uint256 i = 0;
      while (i < StringLength(str)) {
        result[i] = StringSlice(str, i, i + 1);
      unchecked {
        ++i;
      }
      }
    } else {
      result = new string[](StringIncludeCount(str, token) + 1);
      string memory remainingStr = str;
      uint256 tokenCount = 0;
      int256 indexOfToken = StringIndexOf(remainingStr, token);
      while (indexOfToken >= 0) {
        result[tokenCount] = StringSlice(remainingStr, 0, uint256(indexOfToken));
        remainingStr = StringSlice(remainingStr, uint256(indexOfToken) + 1, StringLength(remainingStr));
        indexOfToken = StringIndexOf(remainingStr, token);
      unchecked {
        ++tokenCount;
      }
      }
      result[tokenCount] = remainingStr;
      return result;
    }
  }

  function StringIndexOf(string memory str, bytes1 token) internal pure returns (int256 index) {
    bytes memory strAsBytes = bytes(str);
    index = 0;
    while (index < int256(strAsBytes.length) && strAsBytes[uint256(index)] != token) {
    unchecked {
      ++index;
    }
    }
    if (uint256(index) == strAsBytes.length) {
      return -1;
    } else {
      return index;
    }
  }

  function StringIncludeCount(string memory str, bytes1 token) internal pure returns (uint256 count) {
    bytes memory strAsBytes = bytes(str);
    uint256 i = 0;
    while (i < strAsBytes.length) {
      if (strAsBytes[i] == token) {
        count++;
      }
    unchecked {
      ++i;
    }
    }
    return count;
  }

  function StringSlice(
    string memory str,
    uint256 startIndex,
    uint256 endIndex
  ) internal pure returns (string memory result) {
    if (StringLength(str) == 0) {
      return "";
    }
    uint256 start = startIndex;
    uint256 end = endIndex;
    if (start > StringLength(str)) {
      start = StringLength(str);
    }
    if (end > StringLength(str)) {
      end = StringLength(str);
    }
    if (start > end) {
      uint256 tempStartIndex = start;
      start = end;
      end = tempStartIndex;
    }
    bytes memory resultAsBytes = new bytes(end - start);
    uint256 i = start;
    while (i < end) {
      resultAsBytes[i - start] = bytes(str)[i];
    unchecked {
      ++i;
    }
    }
    result = string(resultAsBytes);
    return result;
  }

  function StringConcat(string memory s1, string memory s2) internal pure returns (string memory) {
    return string(abi.encodePacked(s1, s2));
  }

  /* Helper function to convert a string object into a 256-bit unsigned integer object. */
  function StringToUInt(string memory s) internal pure returns (uint256) {
    bytes memory b = bytes(s);
    uint256 result = 0;
    for (uint256 i = 0; i < b.length; i++) {
      uint256 c = uint256(uint8(b[i]));
      if (c >= 48 && c <= 57) {
        result = result * 10 + (c - 48);
      }
    }
    return result;
  }

  /*
   * Converts an 256-bit unsigned integer to a string. Authored by Mikhail Vladimirov as optimization for large numbers.
   * This function calls the 32 digit alignment function up to three times to convert an arbitrary 256-bit integer
   * after which it concatenate the results and remove leading zeros. It uses a binary search to find the exact number
   * of leading zeros to be removed from which it can remove leading zeros from a string in place.
   * @param {uint} x The input 256-bit unsigned integer.
   * @return {string} The converted string value.
   */
  function UIntToString(uint x) internal pure returns (string memory s) {
  unchecked {
    if (x == 0) return "0";
    else {
      uint c1 = UIntToString32(x % 1e32);
      x /= 1e32;
      if (x == 0) s = string(abi.encode(c1));
      else {
        uint c2 = UIntToString32(x % 1e32);
        x /= 1e32;
        if (x == 0) {
          s = string(abi.encode(c2, c1));
          c1 = c2;
        } else {
          uint c3 = UIntToString32(x);
          s = string(abi.encode(c3, c2, c1));
          c1 = c3;
        }
      }
      uint z = 0;
      if (c1 >> 128 == 0x30303030303030303030303030303030) { c1 <<= 128; z += 16; }
      if (c1 >> 192 == 0x3030303030303030) { c1 <<= 64; z += 8; }
      if (c1 >> 224 == 0x30303030) { c1 <<= 32; z += 4; }
      if (c1 >> 240 == 0x3030) { c1 <<= 16; z += 2; }
      if (c1 >> 248 == 0x30) { z += 1; }
      assembly ("memory-safe")  {
        let l := mload (s)
        s := add (s, z)
        mstore (s, sub (l, z))
      }
    }
  }
  }

  /*
   * Converts a 256-bit unsigned integer below 10^32 into exactly 32 digits, padding it with zeros if necessary.
	 * @param {uint} x The input 256-bit unsigned integer.
   * @return {uint} The resulting 256-bit unsigned integer after aligning to 32 digits.
  */
  function UIntToString32(uint x) internal pure returns (uint y) {
  unchecked {
    require (x < 1e32);
    y = 0x3030303030303030303030303030303030303030303030303030303030303030;
    y += x % 10; x /= 10;
    y += x % 10 << 8; x /= 10;
    y += x % 10 << 16; x /= 10;
    y += x % 10 << 24; x /= 10;
    y += x % 10 << 32; x /= 10;
    y += x % 10 << 40; x /= 10;
    y += x % 10 << 48; x /= 10;
    y += x % 10 << 56; x /= 10;
    y += x % 10 << 64; x /= 10;
    y += x % 10 << 72; x /= 10;
    y += x % 10 << 80; x /= 10;
    y += x % 10 << 88; x /= 10;
    y += x % 10 << 96; x /= 10;
    y += x % 10 << 104; x /= 10;
    y += x % 10 << 112; x /= 10;
    y += x % 10 << 120; x /= 10;
    y += x % 10 << 128; x /= 10;
    y += x % 10 << 136; x /= 10;
    y += x % 10 << 144; x /= 10;
    y += x % 10 << 152; x /= 10;
    y += x % 10 << 160; x /= 10;
    y += x % 10 << 168; x /= 10;
    y += x % 10 << 176; x /= 10;
    y += x % 10 << 184; x /= 10;
    y += x % 10 << 192; x /= 10;
    y += x % 10 << 200; x /= 10;
    y += x % 10 << 208; x /= 10;
    y += x % 10 << 216; x /= 10;
    y += x % 10 << 224; x /= 10;
    y += x % 10 << 232; x /= 10;
    y += x % 10 << 240; x /= 10;
    y += x % 10 << 248;
  }
  }

  function UInt32ToBytes32(uint32 x) internal pure returns (bytes32) {
    return bytes32(uint256(x));
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

  /*
   * Extract a revert from the raw data as returned from a smart contract call. It supports panic, error & custom errors.
   * Source: https://github.com/superfluid-finance/protocol-monorepo/blob/dev/packages/ethereum-contracts/contracts/libs/CallUtils.sol under AGPLv3 license.
   * @param {bytes} returnedData The data returned from calling another contract.
   */
  function revertFromReturnedData(
    bytes memory returnedData
  ) internal pure {
    if (returnedData.length < 4) {
      // case 1: catch all
      revert("Target revert");
    } else {
      bytes4 errorSelector;
      assembly ("memory-safe")  {
        errorSelector := mload(add(returnedData, 0x20))
      }
      if (errorSelector == bytes4(0x4e487b71) /* `seth sig "Panic(uint256)"` */) {
        // case 2: Panic(uint256) (Defined since 0.8.0)
        // solhint-disable-next-line max-line-length
        // ref: https://docs.soliditylang.org/en/v0.8.0/control-structures.html#panic-via-assert-and-error-via-require)
        string memory reason = "Target panicked: 0x__";
        uint errorCode;
        assembly ("memory-safe")  {
          errorCode := mload(add(returnedData, 0x24))
          let reasonWord := mload(add(reason, 0x20))
        // [0..9] is converted to ['0'..'9']
        // [0xa..0xf] is not correctly converted to ['a'..'f']
        // but since panic code doesn't have those cases, we will ignore them for now!
          let e1 := add(and(errorCode, 0xf), 0x30)
          let e2 := shl(8, add(shr(4, and(errorCode, 0xf0)), 0x30))
          reasonWord := or(
          and(reasonWord, 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000),
          or(e2, e1))
          mstore(add(reason, 0x20), reasonWord)
        }
        revert(reason);
      } else {
        // case 3: Error(string) (Defined at least since 0.7.0)
        // case 4: Custom errors (Defined since 0.8.0)
        uint len = returnedData.length;
        assembly ("memory-safe")  {
          revert(add(returnedData, 32), len)
        }
      }
    }
  }
}
