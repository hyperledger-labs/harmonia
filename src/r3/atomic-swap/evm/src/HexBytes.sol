// SPDX-License-Identifier: Apache-2.0

/******************************************************************************
 * Copyright 2023 R3 LLC                                                      *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

pragma solidity 0.8.20;

library HexBytes {
    function hexToBytes(string calldata hexString) internal pure returns (bytes memory) {
        return hexToBytes(bytes(hexString));
    }

    function hexToBytes(bytes calldata hexString) internal pure returns (bytes memory) {
        if (hexString[0] == "0" && hexString[1] == "x") {
            hexString = hexString[2:];
        }
        require(hexString.length % 2 == 0, "Invalid hex string");
        bytes memory bytesArray = new bytes(hexString.length / 2);
        for (uint256 i = 0; i < bytesArray.length; i++) {
            uint8 b = uint8(hexToNibble(uint8(hexString[i * 2])) << 4 | uint8(hexToNibble(uint8(hexString[i * 2 + 1]))));
            bytesArray[i] = bytes1(b);
        }
        return bytesArray;
    }

    function bytesToHex(bytes calldata byteArray) internal pure returns (string memory) {
        bytes memory hexString = new bytes(byteArray.length * 2);
        for (uint256 i = 0; i < byteArray.length; i++) {
            uint8 value = uint8(byteArray[i]);
            hexString[i * 2] = toHexChar(uint8(value >> 4));
            hexString[i * 2 + 1] = toHexChar(uint8(value & 0x0F));
        }
        return string(hexString);
    }

    function bytes32ToHex(bytes32 value) internal view returns (string memory resultStr) {
        bytes memory byteArray = new bytes(32);
        for (uint256 i = 0; i < 32; i++) {
            byteArray[i] = value[i];
        }

        (bool success, bytes memory result) =
            address(this).staticcall(abi.encodeWithSignature("bytesToHex(bytes)", byteArray));

        resultStr = abi.decode(result, (string));

        require(success, resultStr);
    }

    function toHexChar(uint8 value) private pure returns (bytes1) {
        if (value < 10) {
            return bytes1(uint8(value) + 48);
        } else {
            return bytes1(uint8(value) + 87);
        }
    }

    function hexToNibble(uint8 hexDigit) private pure returns (uint8) {
        if (hexDigit >= 48 && hexDigit <= 57) {
            return hexDigit - 48;
        } else if (hexDigit >= 65 && hexDigit <= 70) {
            return hexDigit - 65 + 10;
        } else {
            return hexDigit - 97 + 10;
        }
    }
}
