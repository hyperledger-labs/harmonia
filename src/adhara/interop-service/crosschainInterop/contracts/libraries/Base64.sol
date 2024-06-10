/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.13;

/*
 * Library to handle Base64 encoding.
 * Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/utils/Base64.sol
 */
library Base64 {

  string internal constant _TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

  /*
   * Encodes the given bytes to base-64 encoding and return a string representation thereof.
   * @param {bytes} data The bytes to encode.
   * @return {string} The base-64 encoded data as a string.
   */
  function encode(
    bytes memory data
  ) internal pure returns (string memory) {
    if (data.length == 0) return "";
    // Loads the table into memory
    string memory table = _TABLE;
    // Encoding takes 3 bytes chunks of binary data from `bytes` data parameter
    // and split into 4 numbers of 6 bits.
    // The final Base64 length should be `bytes` data length multiplied by 4/3 rounded up
    // - `data.length + 2`  -> Round up
    // - `/ 3`              -> Number of 3-bytes chunks
    // - `4 *`              -> 4 characters for each chunk
    string memory result = new string(4 * ((data.length + 2) / 3));

    assembly ("memory-safe")  {
    // Prepare the lookup table (skip the first "length" byte)
      let tablePtr := add(table, 1)

    // Prepare result pointer, jump over length
      let resultPtr := add(result, 32)

    // Run over the input, 3 bytes at a time
      for {
        let dataPtr := data
        let endPtr := add(data, mload(data))
      } lt(dataPtr, endPtr) {

      } {
      // Advance 3 bytes
        dataPtr := add(dataPtr, 3)
        let input := mload(dataPtr)

      // To write each character, shift the 3 bytes (18 bits) chunk
      // 4 times in blocks of 6 bits for each character (18, 12, 6, 0)
      // and apply logical AND with 0x3F which is the number of
      // the previous character in the ASCII table prior to the Base64 Table
      // The result is then added to the table to get the character to write,
      // and finally write it in the result pointer but with a left shift
      // of 256 (1 byte) - 8 (1 ASCII char) = 248 bits

        mstore8(resultPtr, mload(add(tablePtr, and(shr(18, input), 0x3F))))
        resultPtr := add(resultPtr, 1) // Advance

        mstore8(resultPtr, mload(add(tablePtr, and(shr(12, input), 0x3F))))
        resultPtr := add(resultPtr, 1) // Advance

        mstore8(resultPtr, mload(add(tablePtr, and(shr(6, input), 0x3F))))
        resultPtr := add(resultPtr, 1) // Advance

        mstore8(resultPtr, mload(add(tablePtr, and(input, 0x3F))))
        resultPtr := add(resultPtr, 1) // Advance
      }

    // When data `bytes` is not exactly 3 bytes long
    // it is padded with `=` characters at the end
      switch mod(mload(data), 3)
      case 1 {
        mstore8(sub(resultPtr, 1), 0x3d)
        mstore8(sub(resultPtr, 2), 0x3d)
      }
      case 2 {
        mstore8(sub(resultPtr, 1), 0x3d)
      }
    }
    return result;
  }
}
