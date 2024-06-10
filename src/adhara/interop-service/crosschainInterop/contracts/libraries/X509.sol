/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.13;

import "contracts/libraries/SolidityUtils.sol";

library X509 {

	/* Decodes a DER-encoded X.509 public key, also known as SubjectPublicKeyInfo (SPKI), as defined in RFC 5280.
	 * SubjectPublicKeyInfo  ::=  SEQUENCE  {
	 *   algorithm         AlgorithmIdentifier,
	 *   subjectPublicKey  BIT STRING
	 * }
	 * @param {h} The DER-encoded key as hex string.
   * @return The extracted elliptic curve point as decoded key.
   */
	function decodeKey(
		string memory h
	) internal pure returns (bytes memory) {
		bytes memory k = SolUtils.HexStringToBytes(h);
		uint256 r = ASN1.getRootNode(k);
		uint256 c = ASN1.firstChildOf(k, r);
		uint256 s = ASN1.nextSiblingOf(k, c);
		bytes memory d = ASN1.getBitStringAt(k, s);
		if (d.length == 32) {
			return d;
		} else {
			uint256 offset = 0;
			// According to section 2.2 of RFC 5480, the first header byte, 0x04 indicates that this is an uncompressed key.
			if (d[offset++] != 0x04) {
				revert("X.509 public key contains an invalid uncompressed point encoding, no uncompressed point indicator.");
			}
			// Per ANSI X9.62, an encoded point is a 1 byte type followed by ceiling(log base 2 field-size / 8) bytes of x and the same of y.
			uint256 keySizeBytes = 32;
			if (d.length != 1 + 2 * keySizeBytes) {
				revert(string(abi.encodePacked(
					"X.509 public key contains an invalid uncompressed point encoding, incorrect size, was given size=[",
					SolUtils.UIntToString(d.length),
					"], but expected size=[",
					SolUtils.UIntToString(1+2*keySizeBytes),
					"]"
				)));
			}
			// The x-coordinate of the elliptic curve point: x = uint256(bytes32(d.getNBytesAtIndex(offset, keySizeBytes)));
			// The y-coordinate of the elliptic curve point: y = uint256(bytes32(d.getNBytesAtIndex(offset + keySizeBytes, keySizeBytes)));
			// The parity of the y coordinate used to reconstruct key uniquely for x-coordinate: parity = (y & 0x1) | 0x2;
			return SolUtils.BytesToNBytesAtIndex(d, offset, keySizeBytes);
		}
	}
}

/*
 * ASN1 library to read DER-encoded ASN1 structures.
 * Source: Based on https://github.com/JonahGroendal/asn1-decode.
 */
library ASN1 {
	using NodePtr for uint256;

	/*
	 * Get the root node. First step in traversing an ASN1 structure.
	 * @param {der} The DER-encoded ASN1 structure.
   * @return A pointer to the outermost node.
   */
	function getRootNode(
		bytes memory der
	) internal pure returns (uint256) {
		require(der[0] != 0x00, "ASN1 encoding contains a zero value that can not be used as root node length.");
		return readNodeLength(der, 0);
	}

	/*
	 * Get the next sibling node.
	 * @param {der} The DER-encoded ASN1 structure.
   * @param {ptr} Points to the indices of the current node.
   * @return A pointer to the next sibling node.
   */
	function nextSiblingOf(
		bytes memory der,
		uint256 ptr
	) internal pure returns (uint256) {
		return readNodeLength(der, ptr.ixl()+1);
	}

	/*
	 * Get the first child node of the current node.
	 * @param {der} The DER-encoded ASN1 structure.
   * @param {ptr} Points to the indices of the current node.
   * @return A pointer to the first child node.
   */
	function firstChildOf(
		bytes memory der,
		uint256 ptr
	) internal pure returns (uint256) {
		require(der[ptr.ixs()] & 0x20 == 0x20, "ASN1 encoding contains a value that is not a constructed type.");
		return readNodeLength(der, ptr.ixf());
	}

	/*
	 * Extract value of bit string node from DER-encoded structure.
	 * @param {der} The DER-encoded ASN1 structure.
   * @param {ptr} Points to the indices of the current node.
   * @return Value of bit string converted to bytes.
   */
	function getBitStringAt(
		bytes memory der,
		uint256 ptr
	) internal pure returns (bytes memory) {
		require(der[ptr.ixs()] == 0x03, "ASN1 encoding contains a value that is not of type bit string.");
		require(der[ptr.ixf()] == 0x00, "ASN1 encoding only allows for zero-padded bit strings to be converted to byte strings.");
		uint256 valueLength = ptr.ixl()+1 - ptr.ixf();
		return SolUtils.BytesToNBytesAtIndex(der, ptr.ixf()+1, valueLength-1);
	}

	/*
	 * Reads the node length from a DER-encoded structure.
	 * @param {der} The DER-encoded ASN1 structure.
   * @param {ix} Points to the index of the current node.
   * @return A pointer to the node at given index.
   */
	function readNodeLength(
		bytes memory der,
		uint256 ix
	) internal pure returns (uint256) {
		uint256 length;
		uint80 ixFirstContentByte;
		uint80 ixLastContentByte;
		if ((der[ix+1] & 0x80) == 0) {
			length = uint8(der[ix+1]);
			ixFirstContentByte = uint80(ix+2);
			ixLastContentByte = uint80(ixFirstContentByte + length -1);
		} else {
			uint8 bytesLength = uint8(der[ix+1] & 0x7F);
			if (bytesLength == 1)
				length = SolUtils.BytesToUInt8AtIndex(der, ix+2);
			else if (bytesLength == 2)
				length = SolUtils.BytesToUInt16AtIndex(der, ix+2);
			else
				length = uint256(SolUtils.BytesToNBytes32AtIndex(der, ix+2, bytesLength) >> (32- bytesLength)*8);
			ixFirstContentByte = uint80(ix+2+ bytesLength);
			ixLastContentByte = uint80(ixFirstContentByte + length -1);
		}
		return NodePtr.getPtr(ix, ixFirstContentByte, ixLastContentByte);
	}
}

/*
 * Helper library for positions in DER-encoded ASN1 structures.
 */
library NodePtr {
	// Unpack first byte index
	function ixs(uint256 ptr) internal pure returns (uint256) {
		return uint80(ptr);
	}
	// Unpack first content byte index
	function ixf(uint256 ptr) internal pure returns (uint256) {
		return uint80(ptr>>80);
	}
	// Unpack last content byte index
	function ixl(uint256 ptr) internal pure returns (uint256) {
		return uint80(ptr>>160);
	}
	// Pack 3 uint80s into a uint256
	function getPtr(uint256 iixs, uint256 iixf, uint256 iixl) internal pure returns (uint256) {
		iixs |= iixf<<80;
		iixs |= iixl<<160;
		return iixs;
	}
}
