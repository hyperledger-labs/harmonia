/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.13;

import "contracts/libraries/EllipticCurve.sol";

library SECP256R1 {

  uint constant a = 0xFFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC;
  uint constant b = 0x5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B;
  uint constant gx = 0x6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296;
  uint constant gy = 0x4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5;
  uint constant p = 0xFFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF;
  uint constant n = 0xFFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551;

  uint constant lowSmax = 0x7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0;

  /*
   * Verification of an ECDSA-SECP256R1-SHA256 signature.
   * @param {bytes32} k The ECDSA public key x coordinate to be used in verification.
   * @param {bytes32} r The r value of the ECDSA signature.
   * @param {bytes32} s The s value of the ECDSA signature.
   * @param {bytes1} v The public key parity bit of 0x02 if even and 0x03 if odd.
   * @param {bytes} m The message, to be hashed with SHA-256, that was signed.
   * @return {bool} Returns true if the signature is valid.
   */
  function verify(
    bytes32 k,
    bytes32 r,
    bytes32 s,
    bytes1 v,
    bytes memory m
  ) internal pure returns (bool) {
    bytes32 messageHash = sha256(m);
    uint256 y = EllipticCurve.deriveY(uint8(v), uint256(k), a, b, p);
    return verifySignature(messageHash, [uint256(r), uint256(s)], [uint256(k), y]);
  }

  /* Verify combination of message, signature, and public key. */
  function verifySignature(
    bytes32 message,
    uint[2] memory rs,
    uint[2] memory publicKey
  ) internal pure returns (bool)
  {
    // To disambiguate between public key solutions, include comment below.
    if (rs[0] == 0 || rs[0] >= n || rs[1] == 0) {// || rs[1] > lowSmax)
      return false;
    }
    if (!EllipticCurve.isOnCurve(publicKey[0], publicKey[1], a, b, p)) {
      return false;
    }
    uint x1;
    uint x2;
    uint y1;
    uint y2;
    uint sInv = EllipticCurve.invMod(rs[1], n);
    // s^(-1)
    (x1, y1) = EllipticCurve.ecMul(mulmod(uint(message), sInv, n), gx, gy, a, p);
    // s^(-1)zG
    (x2, y2) = EllipticCurve.ecMul(mulmod(rs[0], sInv, n), publicKey[0], publicKey[1], a, p);
    // s^(-1)rQ
    uint[3] memory P = addAndReturnProjectivePoint(x1, y1, x2, y2);
    // s^(-1)zG + s^(-1)rQ
    if (P[2] == 0) {
      return false;
    }
    uint Px = EllipticCurve.invMod(P[2], p);
    Px = mulmod(P[0], mulmod(Px, Px, p), p);
    return Px % n == rs[0];
  }

  /* Transform affine coordinates into projective coordinates. */
  function toProjectivePoint(
    uint x0,
    uint y0
  ) internal pure returns (uint[3] memory P)
  {
    P[2] = addmod(0, 1, p);
    P[0] = mulmod(x0, P[2], p);
    P[1] = mulmod(y0, P[2], p);
  }

  /* Add two points in affine coordinates and return projective point. */
  function addAndReturnProjectivePoint(
    uint x1,
    uint y1,
    uint x2,
    uint y2
  ) internal pure returns (uint[3] memory P)
  {
    uint x;
    uint y;
    (x, y) = add(x1, y1, x2, y2);
    P = toProjectivePoint(x, y);
  }

  /* Transform from projective to affine coordinates. */
  function toAffinePoint(
    uint x0,
    uint y0,
    uint z0
  ) internal pure returns (uint x1, uint y1)
  {
    uint z0Inv;
    z0Inv = EllipticCurve.invMod(z0, p);
    x1 = mulmod(x0, z0Inv, p);
    y1 = mulmod(y0, z0Inv, p);
    //return EllipticCurve.toAffine(x0, y0, z0, p);
  }

  /* Return the zero curve in projective coordinates. */
  function zeroProj() internal pure returns (uint x, uint y, uint z)
  {
    return (0, 1, 0);
  }

  /* Return the zero curve in affine coordinates. */
  function zeroAffine() internal pure returns (uint x, uint y)
  {
    return (0, 0);
  }

  /* Check if the curve is the zero curve. */
  function isZeroCurve(
    uint x0,
    uint y0
  ) internal pure returns (bool isZero)
  {
    if (x0 == 0 && y0 == 0) {
      return true;
    }
    return false;
  }

  /* Double an elliptic curve point in projective coordinates. Source: https://www.nayuki.io/page/elliptic-curve-point-addition-in-projective-coordinates  */
  function twiceProj(
    uint x0,
    uint y0,
    uint z0
  ) internal pure returns (uint x1, uint y1, uint z1)
  {
    uint t;
    uint u;
    uint v;
    uint w;
    if (isZeroCurve(x0, y0)) {
      return zeroProj();
    }
    u = mulmod(y0, z0, p);
    u = mulmod(u, 2, p);
    v = mulmod(u, x0, p);
    v = mulmod(v, y0, p);
    v = mulmod(v, 2, p);
    x0 = mulmod(x0, x0, p);
    t = mulmod(x0, 3, p);
    z0 = mulmod(z0, z0, p);
    z0 = mulmod(z0, a, p);
    t = addmod(t, z0, p);
    w = mulmod(t, t, p);
    x0 = mulmod(2, v, p);
    w = addmod(w, p - x0, p);
    x0 = addmod(v, p - w, p);
    x0 = mulmod(t, x0, p);
    y0 = mulmod(y0, u, p);
    y0 = mulmod(y0, y0, p);
    y0 = mulmod(2, y0, p);
    y1 = addmod(x0, p - y0, p);
    x1 = mulmod(u, w, p);
    z1 = mulmod(u, u, p);
    z1 = mulmod(z1, u, p);
  }

  /* Add two elliptic curve points in projective coordinates. Source https://www.nayuki.io/page/elliptic-curve-point-addition-in-projective-coordinates */
  function addProj(
    uint x0,
    uint y0,
    uint z0,
    uint x1,
    uint y1,
    uint z1
  ) internal pure returns (uint x2, uint y2, uint z2)
  {
    uint t0;
    uint t1;
    uint u0;
    uint u1;
    if (isZeroCurve(x0, y0)) {
      return (x1, y1, z1);
    }
    else if (isZeroCurve(x1, y1)) {
      return (x0, y0, z0);
    }
    t0 = mulmod(y0, z1, p);
    t1 = mulmod(y1, z0, p);
    u0 = mulmod(x0, z1, p);
    u1 = mulmod(x1, z0, p);
    if (u0 == u1) {
      if (t0 == t1) {
        return twiceProj(x0, y0, z0);
      }
      else {
        return zeroProj();
      }
    }
    (x2, y2, z2) = addProj2(mulmod(z0, z1, p), u0, u1, t1, t0);
    //return EllipticCurve.jacAdd(x0, y0, z0, x1, y1, z1, p);
  }

  /* Helper function that splits addProj to avoid too many local variables. */
  function addProj2(
    uint v,
    uint u0,
    uint u1,
    uint t1,
    uint t0
  ) private pure returns (uint x2, uint y2, uint z2)
  {
    uint u;
    uint u2;
    uint u3;
    uint w;
    uint t;
    t = addmod(t0, p - t1, p);
    u = addmod(u0, p - u1, p);
    u2 = mulmod(u, u, p);
    w = mulmod(t, t, p);
    w = mulmod(w, v, p);
    u1 = addmod(u1, u0, p);
    u1 = mulmod(u1, u2, p);
    w = addmod(w, p - u1, p);
    x2 = mulmod(u, w, p);
    u3 = mulmod(u2, u, p);
    u0 = mulmod(u0, u2, p);
    u0 = addmod(u0, p - w, p);
    t = mulmod(t, u0, p);
    t0 = mulmod(t0, u3, p);
    y2 = addmod(t, p - t0, p);
    z2 = mulmod(u3, v, p);
  }

  /* Add two elliptic curve points in affine coordinates. */
  function add(
    uint x0,
    uint y0,
    uint x1,
    uint y1
  ) internal pure returns (uint, uint)
  {
    uint z0;
    (x0, y0, z0) = addProj(x0, y0, 1, x1, y1, 1);
    return toAffinePoint(x0, y0, z0);
  }

  /* Double an elliptic curve point in affine coordinates. */
  function twice(
    uint x0,
    uint y0
  ) internal pure returns (uint, uint)
  {
    uint z0;
    (x0, y0, z0) = twiceProj(x0, y0, 1);
    return toAffinePoint(x0, y0, z0);
  }

  /* Multiply an elliptic curve point by a 2 power base (i.e., (2^exp)*P)). */
  function multiplyPowerBase2(
    uint x0,
    uint y0,
    uint exp
  ) internal pure returns (uint, uint)
  {
    uint base2X = x0;
    uint base2Y = y0;
    uint base2Z = 1;
    for (uint i = 0; i < exp; i++) {
      (base2X, base2Y, base2Z) = twiceProj(base2X, base2Y, base2Z);
    }
    return toAffinePoint(base2X, base2Y, base2Z);
  }

  /* Multiply the curve's generator point by a scalar. */
  function multipleGeneratorByScalar(
    uint scalar
  ) internal pure returns (uint, uint)
  {
    return EllipticCurve.ecMul(scalar, gx, gy, a, p);
  }
}
