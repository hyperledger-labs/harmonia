/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.13;

/*
 * Library to handle SHA-512 hashing
 * Modified from source: https://github.com/chengwenxi/Ed25519/blob/main/contracts/libraries/Sha512.sol
 * Reference: https://csrc.nist.gov/csrc/media/publications/fips/180/2/archive/2002-08-01/documents/fips180-2.pdf
 */

library SHA512 {

  /*
   * Calculates the SHA-512 hash of the given input data.
   * @param {bytes} data The input data as bytes.
   * @return Returns the 512-bit hash result.
   */
  function hash(
    bytes memory data
  ) internal pure returns (uint64[8] memory) {
    uint64[8] memory H = [
    0x6a09e667f3bcc908,
    0xbb67ae8584caa73b,
    0x3c6ef372fe94f82b,
    0xa54ff53a5f1d36f1,
    0x510e527fade682d1,
    0x9b05688c2b3e6c1f,
    0x1f83d9abfb41bd6b,
    0x5be0cd19137e2179
    ];

    uint64 T1;
    uint64 T2;

    uint64[80] memory W;
    FuncVar memory fvar;

    uint64[80] memory K = [
    0x428a2f98d728ae22,
    0x7137449123ef65cd,
    0xb5c0fbcfec4d3b2f,
    0xe9b5dba58189dbbc,
    0x3956c25bf348b538,
    0x59f111f1b605d019,
    0x923f82a4af194f9b,
    0xab1c5ed5da6d8118,
    0xd807aa98a3030242,
    0x12835b0145706fbe,
    0x243185be4ee4b28c,
    0x550c7dc3d5ffb4e2,
    0x72be5d74f27b896f,
    0x80deb1fe3b1696b1,
    0x9bdc06a725c71235,
    0xc19bf174cf692694,
    0xe49b69c19ef14ad2,
    0xefbe4786384f25e3,
    0x0fc19dc68b8cd5b5,
    0x240ca1cc77ac9c65,
    0x2de92c6f592b0275,
    0x4a7484aa6ea6e483,
    0x5cb0a9dcbd41fbd4,
    0x76f988da831153b5,
    0x983e5152ee66dfab,
    0xa831c66d2db43210,
    0xb00327c898fb213f,
    0xbf597fc7beef0ee4,
    0xc6e00bf33da88fc2,
    0xd5a79147930aa725,
    0x06ca6351e003826f,
    0x142929670a0e6e70,
    0x27b70a8546d22ffc,
    0x2e1b21385c26c926,
    0x4d2c6dfc5ac42aed,
    0x53380d139d95b3df,
    0x650a73548baf63de,
    0x766a0abb3c77b2a8,
    0x81c2c92e47edaee6,
    0x92722c851482353b,
    0xa2bfe8a14cf10364,
    0xa81a664bbc423001,
    0xc24b8b70d0f89791,
    0xc76c51a30654be30,
    0xd192e819d6ef5218,
    0xd69906245565a910,
    0xf40e35855771202a,
    0x106aa07032bbd1b8,
    0x19a4c116b8d2d0c8,
    0x1e376c085141ab53,
    0x2748774cdf8eeb99,
    0x34b0bcb5e19b48a8,
    0x391c0cb3c5c95a63,
    0x4ed8aa4ae3418acb,
    0x5b9cca4f7763e373,
    0x682e6ff3d6b2b8a3,
    0x748f82ee5defb2fc,
    0x78a5636f43172f60,
    0x84c87814a1f0ab72,
    0x8cc702081a6439ec,
    0x90befffa23631e28,
    0xa4506cebde82bde9,
    0xbef9a3f7b2c67915,
    0xc67178f2e372532b,
    0xca273eceea26619c,
    0xd186b8c721c0c207,
    0xeada7dd6cde0eb1e,
    0xf57d4f7fee6ed178,
    0x06f067aa72176fba,
    0x0a637dc5a2c898a6,
    0x113f9804bef90dae,
    0x1b710b35131c471b,
    0x28db77f523047d84,
    0x32caab7b40c72493,
    0x3c9ebe0a15c9bebc,
    0x431d67c49c100d4c,
    0x4cc5d4becb3e42b6,
    0x597f299cfc657e2a,
    0x5fcb6fab3ad6faec,
    0x6c44198c4a475817
    ];

    bytes memory blocks = preprocess(data);
  unchecked {
    for (uint256 j = 0; j < blocks.length / 128; j++) {
      uint64[16] memory M = cutBlock(blocks, j);

      fvar.a = H[0];
      fvar.b = H[1];
      fvar.c = H[2];
      fvar.d = H[3];
      fvar.e = H[4];
      fvar.f = H[5];
      fvar.g = H[6];
      fvar.h = H[7];

      for (uint256 i = 0; i < 80; i++) {
        if (i < 16) {
          W[i] = M[i];
        } else {
          W[i] =
          gamma1(W[i - 2]) +
          W[i - 7] +
          gamma0(W[i - 15]) +
          W[i - 16];
        }

        T1 =
        fvar.h +
        sigma1(fvar.e) +
        Ch(fvar.e, fvar.f, fvar.g) +
        K[i] +
        W[i];
        T2 = sigma0(fvar.a) + Maj(fvar.a, fvar.b, fvar.c);

        fvar.h = fvar.g;
        fvar.g = fvar.f;
        fvar.f = fvar.e;
        fvar.e = fvar.d + T1;
        fvar.d = fvar.c;
        fvar.c = fvar.b;
        fvar.b = fvar.a;
        fvar.a = T1 + T2;
      }

      H[0] = H[0] + fvar.a;
      H[1] = H[1] + fvar.b;
      H[2] = H[2] + fvar.c;
      H[3] = H[3] + fvar.d;
      H[4] = H[4] + fvar.e;
      H[5] = H[5] + fvar.f;
      H[6] = H[6] + fvar.g;
      H[7] = H[7] + fvar.h;
    }
  }
    return H;
  }

  /*
   * Process a message by padded it before hash computation begins.
   * The purpose of this padding is to ensure that the padded message is a multiple of 1024 bits.
   * @param {bytes} message The raw input message as bytes.
   * @return Returns the padded message as bytes.
   */
  function preprocess(
    bytes memory message
  ) internal pure returns (bytes memory) {
    uint256 padding = 128 - (message.length % 128);
    if (message.length % 128 >= 112) {
      padding = 256 - (message.length % 128);
    }
    bytes memory result = new bytes(message.length + padding);
    for (uint256 i = 0; i < message.length; i++) {
      result[i] = message[i];
    }
    result[message.length] = 0x80;
    uint128 bitSize = uint128(message.length * 8);
    bytes memory bitlength = abi.encodePacked(bitSize);
    for (uint256 index = 0; index < bitlength.length; index++) {
      result[result.length - 1 - index] = bitlength[
      bitlength.length - 1 - index
      ];
    }
    return result;
  }

  /*
   * Helper function to extract 8 bytes.
   * @param {bytes} b Input bytes.
   * @param {uint256} offset The offset from where to start extracting.
   * @return Returns 8 extracted bytes.
   */
  function bytesToBytes8(
    bytes memory b,
    uint256 offset
  ) internal pure returns (bytes8) {
    bytes8 out;
    for (uint256 i = 0; i < 8; i++) {
      out |= bytes8(b[offset + i] & 0xFF) >> (i * 8);
    }
    return out;
  }

  /*
   * Helper function to extract a block of 16 64-bit unsigned integers.
   * @param {bytes} data Input bytes.
   * @param {uint256} blockIndex The block index from where to start extracting.
   * @return Returns an array with 16 64-bit unsigned integers.
   */
  function cutBlock(
    bytes memory data,
    uint256 blockIndex
  ) internal pure returns (uint64[16] memory)
  {
    uint64[16] memory result;
    for (uint8 r = 0; r < result.length; r++) {
      result[r] = uint64(bytesToBytes8(data, blockIndex * 128 + r * 8));
    }
    return result;
  }

  /* What follows implement the functions that are used by SHA-512 as described in https://csrc.nist.gov/csrc/media/publications/fips/180/2/archive/2002-08-01/documents/fips180-2.pdf#page=15 */

  /*
   * Thus, ROTR(x, n) is equivalent to a circular shift (rotation) of x by n positions to the right.
   * @param {x} Input number.
   * @param {n} The number of positions to circular shift.
   * @return Returns an 64-bit unsigned integer.
   */
  function ROTR(
    uint64 x,
    uint256 n
  ) internal pure returns (uint64) {
    return (x << (64 - n)) + (x >> n);
  }

  /*
   * The right shift operation SHR n(x), where x is a w-bit word and n is an integer with 0 <= n < w, is defined by SHR(x, n) = x >> n.
   * @param {x} The input number.
   * @param {n} The number of positions to shift.
   * @return Returns a 64-bit unsigned integer.
   */
  function SHR(
    uint64 x,
    uint256 n
  ) internal pure returns (uint64) {
    return uint64(x >> n);
  }

  /*
   * Computes Ch(x, y, z) = (x ^ y) ⊕ (﹁ x ^ z).
   * @param {x} The x value.
   * @param {y} The y value.
   * @param {z} The z value.
   * @return Returns a 64-bit unsigned integer.
   */
  function Ch(
    uint64 x,
    uint64 y,
    uint64 z
  ) internal pure returns (uint64) {
    return (x & y) ^ ((x ^ 0xffffffffffffffff) & z);
  }

  /*
   * Computes Maj(x, y, z) = (x ^ y) ⊕ (x ^ z) ⊕ (y ^ z).
   * @param {x} The x value.
   * @param {y} The y value.
   * @param {z} The z value.
   * @return Returns a 64-bit unsigned integer.
   */
  function Maj(
    uint64 x,
    uint64 y,
    uint64 z
  ) internal pure returns (uint64) {
    return (x & y) ^ (x & z) ^ (y & z);
  }

  /*
   * Computes sigma0(x) = ROTR(x, 28) ^ ROTR(x, 34) ^ ROTR(x, 39).
   * @param {x} The x value.
   * @return Returns a 64-bit unsigned integer.
   */
  function sigma0(
    uint64 x
  ) internal pure returns (uint64) {
    return ROTR(x, 28) ^ ROTR(x, 34) ^ ROTR(x, 39);
  }

  /*
   * Computes sigma1(x) = ROTR(x, 14) ^ ROTR(x, 18) ^ ROTR(x, 41).
   * @param {x} The x value.
   * @return Returns a 64-bit unsigned integer.
   */
  function sigma1(
    uint64 x
  ) internal pure returns (uint64) {
    return ROTR(x, 14) ^ ROTR(x, 18) ^ ROTR(x, 41);
  }

  /*
   * Computes gamma0(x) = OTR(x, 1) ^ ROTR(x, 8) ^ SHR(x, 7).
   * @param {x} The x value.
   * @return Returns a 64-bit unsigned integer.
   */
  function gamma0(
    uint64 x
  ) internal pure returns (uint64) {
    return ROTR(x, 1) ^ ROTR(x, 8) ^ SHR(x, 7);
  }

  /*
   * Computes gamma1(x) = ROTR(x, 19) ^ ROTR(x, 61) ^ SHR(x, 6).
   * @param {x} The x value.
   * @return Returns a 64-bit unsigned integer.
   */
  function gamma1(
    uint64 x
  ) internal pure returns (uint64) {
    return ROTR(x, 19) ^ ROTR(x, 61) ^ SHR(x, 6);
  }

  /* Structure to hold function variables */
  struct FuncVar {
    uint64 a;
    uint64 b;
    uint64 c;
    uint64 d;
    uint64 e;
    uint64 f;
    uint64 g;
    uint64 h;
  }
}
