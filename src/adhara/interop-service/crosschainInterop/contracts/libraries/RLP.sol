/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.13;

/*
 * RLP library to read and parse RLP encoded data in memory.
 * Source: Andreas Olofsson (androlo1980@gmail.com) - https://github.com/androlo/standard-contracts licenced as MIT as of 06/11/2023
 */
library RLP {

  uint constant DATA_SHORT_START = 0x80;
  uint constant DATA_LONG_START = 0xB8;
  uint constant LIST_SHORT_START = 0xC0;
  uint constant LIST_LONG_START = 0xF8;

  uint constant DATA_LONG_OFFSET = 0xB7;
  uint constant LIST_LONG_OFFSET = 0xF7;

  /*
   * Structure to hold the RLP item data
   * @param {uint} unsafe_memPtr Pointer to the RLP-encoded bytes.
   * @param {uint} unsafe_length Number of bytes. This is the full length of the string.
   */
  struct RLPItem {
    uint unsafe_memPtr;
    uint unsafe_length;
  }

  /*
   * Iterator to iterate over items in a RLP list
   * @param {RLPItem} unsafe_item Item that's being iterated over.
   * @param	{uint} unsafe_nextPtr Position of the next item in the list.
   */
  struct Iterator {
    RLPItem unsafe_item;
    uint unsafe_nextPtr;
  }

  /*
   * Retrieves the next item using the given iterator.
   * @param {Iterator} self The iterator.
   * @return {RLPItem} subItem Return the next RLP sub item.
   */
  function next(
    Iterator memory self
  ) internal pure returns (RLPItem memory subItem) {
    if (hasNext(self)) {
      uint ptr = self.unsafe_nextPtr;
      uint itemLength = itemLength(ptr);
      subItem.unsafe_memPtr = ptr;
      subItem.unsafe_length = itemLength;
      self.unsafe_nextPtr = ptr + itemLength;
    }
    else
      revert();
  }

  /*
   * Retrieves the next item using the given iterator and optionally validates the item.
   * @param {Iterator} self The iterator.
   * @param {bool} strict Indicate whether to validate the retrieved item.
   * @return {RLPItem} subItem Return the next RLP sub item.
   */
  function next(
    Iterator memory self,
    bool strict
  ) internal pure returns (RLPItem memory subItem) {
    subItem = next(self);
    if (strict && !validate(subItem))
      revert();
  }

  /*
   * Check if an iterator has a next item.
   * @param {Iterator} self The iterator.
   * @return {bool} Returns true if the iterator has a next item.
   */
  function hasNext(
    Iterator memory self
  ) internal pure returns (bool) {
    RLPItem memory item = self.unsafe_item;
    return self.unsafe_nextPtr < item.unsafe_memPtr + item.unsafe_length;
  }

  /*
   * Creates an RLP item from an array of RLP encoded bytes.
   * @param {bytes} self The RLP encoded bytes.
   * @return {RLPItem} Returns the RLP item.
   */
  function toRLPItem(
    bytes memory self
  ) internal pure returns (RLPItem memory) {
    uint len = self.length;
    if (len == 0) {
      return RLPItem(0, 0);
    }
    uint memPtr;
    assembly ("memory-safe")  {
      memPtr := add(self, 0x20)
    }
    return RLPItem(memPtr, len);
  }

  /* Creates an RLP item from an array of RLP-encoded bytes.
   * @param {bytes} self The RLP-encoded bytes.
   * @param {bool} strict Indicate whether to revert if the data is not RLP-encoded.
   * @return {RLPItem} Returns an RLP item.
   */
  function toRLPItem(
    bytes memory self,
    bool strict
  ) internal pure returns (RLPItem memory) {
    RLPItem memory item = toRLPItem(self);
    if (strict) {
      uint len = self.length;
      if (payloadOffset(item) > len)
        revert();
      if (itemLength(item.unsafe_memPtr) != len)
        revert();
      if (!validate(item))
        revert();
    }
    return item;
  }

  /*
   * Check if the RLP item is null.
   * @param {RLPItem} self The RLP item.
   * @return {bool} ret Returns true if the item is null.
   */
  function isNull(
    RLPItem memory self
  ) internal pure returns (bool ret) {
    return self.unsafe_length == 0;
  }

  /* Check if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {bool} ret Returns true if the item is a list.
   */
  function isList(
    RLPItem memory self
  ) internal pure returns (bool ret) {
    if (self.unsafe_length == 0)
      return false;
    uint memPtr = self.unsafe_memPtr;
    assembly ("memory-safe")  {
      ret := iszero(lt(byte(0, mload(memPtr)), 0xC0))
    }
  }

  /*
   * Check if the RLP item is data.
   * @param {RLPItem} self The RLP item.
   * @return {bool} ret Returns true if the item is data.
   */
  function isData(
    RLPItem memory self
  ) internal pure returns (bool ret) {
    if (self.unsafe_length == 0)
      return false;
    uint memPtr = self.unsafe_memPtr;
    assembly ("memory-safe")  {
      ret := lt(byte(0, mload(memPtr)), 0xC0)
    }
  }

  /* Check if the RLP item is empty (string or list).
   * @param {RLPItem} self The RLP item.
   * @return {bool} ret Returns true if the item is null.
   */
  function isEmpty(
    RLPItem memory self
  ) internal pure returns (bool ret) {
    if (isNull(self))
      return false;
    uint b0;
    uint memPtr = self.unsafe_memPtr;
    assembly ("memory-safe")  {
      b0 := byte(0, mload(memPtr))
    }
    return (b0 == DATA_SHORT_START || b0 == LIST_SHORT_START);
  }

  /*
   * Get the number of items in an RLP encoded list.
   * @param {RLPItem} self The RLP item.
   * @return {uint} The number of items.
   */
  function items(
    RLPItem memory self
  ) internal pure returns (uint) {
    if (!isList(self))
      return 0;
    uint b0;
    uint memPtr = self.unsafe_memPtr;
    assembly ("memory-safe")  {
      b0 := byte(0, mload(memPtr))
    }
    uint pos = memPtr + payloadOffset(self);
    uint last = memPtr + self.unsafe_length - 1;
    uint itms;
    while (pos <= last) {
      pos += itemLength(pos);
      itms++;
    }
    return itms;
  }

  /* Create an iterator over an RLP item.
   * @param {RLPItem} self The RLP item.
   * @return {Iterator} it Returns an iterator over the item.
   */
  function iterator(
    RLPItem memory self
  ) internal pure returns (Iterator memory it) {
    if (!isList(self))
      revert();
    uint ptr = self.unsafe_memPtr + payloadOffset(self);
    it.unsafe_item = self;
    it.unsafe_nextPtr = ptr;
  }

  /* Return the RLP encoded bytes.
   * @param {RLPItem} self The RLP item.
   * @return {bytes} bts The converted bytes.
   */
  function toBytes(
    RLPItem memory self
  ) internal pure returns (bytes memory bts) {
    uint len = self.unsafe_length;
    bts = new bytes(len);
    if (len != 0) {
      copyToBytes(self.unsafe_memPtr, bts, len);
    }
  }

  /*
   * Decode an RLP item into bytes. This will not work if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {bytes} bts The decoded bytes.
   */
  function toData(
    RLPItem memory self
  ) internal pure returns (bytes memory bts) {
    if (!isData(self))
      revert();
    uint rStartPos;
    uint len;
    (rStartPos, len) = decode(self);
    bts = new bytes(len);
    copyToBytes(rStartPos, bts, len);
  }

  /*
   * Get the list of sub-items from an RLP encoded list.
   * Warning: This is inefficient, as it requires that the list is read twice.
   * @param {RLPItem} self The RLP item.
   * @return {RLPItem[]} list The array of RLP items.
   */
  function toList(
    RLPItem memory self
  ) internal pure returns (RLPItem[] memory list) {
    if (!isList(self))
      revert("RLPItem is not a list");
    uint numItems = items(self);
    list = new RLPItem[](numItems);
    Iterator memory it = iterator(self);
    uint idx;
    while (hasNext(it)) {
      list[idx] = next(it);
      idx++;
    }
  }

  /*
   * Decode an RLPItem into an ascii string. This will not work if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {string} str The decoded string.
   */
  function toAscii(
    RLPItem memory self
  ) internal pure returns (string memory str) {
    if (!isData(self))
      revert();
    uint rStartPos;
    uint len;
    (rStartPos, len) = decode(self);
    bytes memory bts = new bytes(len);
    copyToBytes(rStartPos, bts, len);
    str = string(bts);
  }

  /*
   * Decode an RLP item into a uint. This will not work if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {uint} data The decoded uint.
   */
  function toUint(
    RLPItem memory self
  ) internal pure returns (uint data) {
    if (!isData(self))
      revert();
    uint rStartPos;
    uint len;
    (rStartPos, len) = decode(self);
    if (len > 32)
      revert();
    else if (len == 0)
      return 0;
    assembly ("memory-safe")  {
      data := div(mload(rStartPos), exp(256, sub(32, len)))
    }
  }

  /*
   * Decode an RLP item into a boolean. This will not work if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {bool} data The decoded boolean.
   */
  function toBool(
    RLPItem memory self
  ) internal pure returns (bool data) {
    if (!isData(self))
      revert();
    uint rStartPos;
    uint len;
    (rStartPos, len) = decode(self);
    if (len != 1)
      revert();
    uint temp;
    assembly ("memory-safe")  {
      temp := byte(0, mload(rStartPos))
    }
    if (temp > 1)
      revert();
    return temp == 1 ? true : false;
  }

  /*
   * Decode an RLP item into a byte. This will not work if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {bytes1} data The decoded bytes1.
   */
  function toByte(
    RLPItem memory self
  ) internal pure returns (bytes1 data) {
    if (!isData(self))
      revert();
    uint rStartPos;
    uint len;
    (rStartPos, len) = decode(self);
    if (len != 1)
      revert();
    bytes1 temp;
    assembly ("memory-safe")  {
      temp := byte(0, mload(rStartPos))
    }
    return temp;
  }

  /* Decode a RLP item into an integer. This will not work if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {int} data The decoded int.
   */
  function toInt(
    RLPItem memory self
  ) internal pure returns (int data) {
    return int(toUint(self));
  }

  /* Decode an RLP item into a bytes32. This will not work if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {bytes32} data The decoded bytes32.
   */
  function toBytes32(
    RLPItem memory self
  ) internal pure returns (bytes32 data) {
    return bytes32(toUint(self));
  }

  /* Decode an RLP item into an address. This will not work if the RLP item is a list.
   * @param {RLPItem} self The RLP item.
   * @return {address} data The decoded address.
   */
  function toAddress(
    RLPItem memory self
  ) internal pure returns (address data) {
    if (!isData(self))
      revert();
    uint rStartPos;
    uint len;
    (rStartPos, len) = decode(self);
    if (len != 20)
      revert();
    assembly ("memory-safe")  {
      data := div(mload(rStartPos), exp(256, 12))
    }
  }

  /*
   * Get the payload offset.
   * @param {RLPItem} self The RLP item.
   * @return {uint} Returns the payload offset.
   */
  function payloadOffset(
    RLPItem memory self
  ) private pure returns (uint) {
    if (self.unsafe_length == 0)
      return 0;
    uint b0;
    uint memPtr = self.unsafe_memPtr;
    assembly ("memory-safe")  {
      b0 := byte(0, mload(memPtr))
    }
    if (b0 < DATA_SHORT_START)
      return 0;
    if (b0 < DATA_LONG_START || (b0 >= LIST_SHORT_START && b0 < LIST_LONG_START))
      return 1;
    if (b0 < LIST_SHORT_START)
      return b0 - DATA_LONG_OFFSET + 1;
    return b0 - LIST_LONG_OFFSET + 1;
  }

  /* Get the full length of an RLP item. */
  function itemLength(
    uint memPtr
  ) private pure returns (uint len) {
    uint b0;
    assembly ("memory-safe")  {
      b0 := byte(0, mload(memPtr))
    }
    if (b0 < DATA_SHORT_START)
      len = 1;
    else if (b0 < DATA_LONG_START)
      len = b0 - DATA_SHORT_START + 1;
    else if (b0 < LIST_SHORT_START) {
      assembly ("memory-safe")  {
        let bLen := sub(b0, 0xB7) // bytes length (DATA_LONG_OFFSET)
        let dLen := div(mload(add(memPtr, 1)), exp(256, sub(32, bLen))) // data length
        len := add(1, add(bLen, dLen)) // total length
      }
    }
    else if (b0 < LIST_LONG_START)
      len = b0 - LIST_SHORT_START + 1;
    else {
      assembly ("memory-safe")  {
        let bLen := sub(b0, 0xF7) // bytes length (LIST_LONG_OFFSET)
        let dLen := div(mload(add(memPtr, 1)), exp(256, sub(32, bLen))) // data length
        len := add(1, add(bLen, dLen)) // total length
      }
    }
  }

  /* Get start position and length of the data. */
  function decode(
    RLPItem memory self
  ) private pure returns (uint memPtr, uint len) {
    if (!isData(self))
      revert();
    uint b0;
    uint start = self.unsafe_memPtr;
    assembly ("memory-safe")  {
      b0 := byte(0, mload(start))
    }
    if (b0 < DATA_SHORT_START) {
      memPtr = start;
      len = 1;
      return (memPtr, len);
    }
    if (b0 < DATA_LONG_START) {
      len = self.unsafe_length - 1;
      memPtr = start + 1;
    } else {
      uint bLen;
      assembly ("memory-safe")  {
        bLen := sub(b0, 0xB7) // DATA_LONG_OFFSET
      }
      len = self.unsafe_length - 1 - bLen;
      memPtr = start + bLen + 1;
    }
    return (memPtr, len);
  }

  /* Assumes that enough memory has been allocated to store in target. */
  function copyToBytes(
    uint btsPtr,
    bytes memory tgt,
    uint btsLen
  ) private pure {
    // Exploiting the fact that 'tgt' was the last thing to be allocated,
    // we can write entire words, and just overwrite any excess.
    assembly ("memory-safe")  {
      {
        let words := div(add(btsLen, 31), 32)
        let rOffset := btsPtr
        let wOffset := add(tgt, 0x20)
        for
        {let i := 0} // Start at arr + 0x20
        lt(i, words)
        {i := add(i, 1)}
        {
          let offset := mul(i, 0x20)
          mstore(add(wOffset, offset), mload(add(rOffset, offset)))
        }
        mstore(add(tgt, add(0x20, mload(tgt))), 0)
      }
    }
  }

  /* Check that an RLP item is valid. */
  function validate(
    RLPItem memory self
  ) private pure returns (bool ret) {
    // Check that RLP is well-formed.
    uint b0;
    uint b1;
    uint memPtr = self.unsafe_memPtr;
    assembly ("memory-safe")  {
      b0 := byte(0, mload(memPtr))
      b1 := byte(1, mload(memPtr))
    }
    if (b0 == DATA_SHORT_START + 1 && b1 < DATA_SHORT_START)
      return false;
    return true;
  }
}
