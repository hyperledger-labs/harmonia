/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.13;

/*
 * Elliptic curve library providing arithmetic operations over elliptic curves.
 * Modified from source: https://github.com/witnet/elliptic-curve-solidity/blob/master/contracts/EllipticCurve.sol
 */
library EllipticCurve {

  /* Pre-computed constant for 2 ** 255 */
  uint256 constant private U255_MAX_PLUS_1 = 57896044618658097711785492504343953926634992332820282019728792003956564819968;

  /*
   * Computes the modular euclidean inverse of a number (mod p).
   * @param {x} The number.
   * @param {p} The modulus p.
   * @return Returns q such that x*q = 1 (mod p).
   */
  function invMod(
    uint256 x,
    uint256 p
  ) internal pure returns (uint256) {
    require(x != 0 && x != p && p != 0, "Invalid number");
    uint256 q = 0;
    uint256 newT = 1;
    uint256 r = p;
    uint256 t;
    while (x != 0) {
      t = r / x;
      (q, newT) = (newT, addmod(q, (p - mulmod(t, newT, p)), p));
      (r, x) = (x, r - t * x);
    }
    return q;
  }

  /*
   * Computes the modular exponentiation, b^e % p.
   * Source: https://github.com/androlo/standard-contracts/blob/master/contracts/src/crypto/ECCMath.sol
   * @param {base} The base b.
   * @param {expo} The exponent e.
   * @param {pp} The modulus p.
   * @return Returns r such that r = b**e (mod p).
   */
  function expMod(
    uint256 base,
    uint256 expo,
    uint256 pp
  ) internal pure returns (uint256) {
    require(pp != 0, "Modulus is zero");
    if (base == 0)
      return 0;
    if (expo == 0)
      return 1;
    uint256 r = 1;
    uint256 bit = U255_MAX_PLUS_1;
    assembly ("memory-safe")  {
      for {} gt(bit, 0) {}{
        r := mulmod(mulmod(r, r, pp), exp(base, iszero(iszero(and(expo, bit)))), pp)
        r := mulmod(mulmod(r, r, pp), exp(base, iszero(iszero(and(expo, div(bit, 2))))), pp)
        r := mulmod(mulmod(r, r, pp), exp(base, iszero(iszero(and(expo, div(bit, 4))))), pp)
        r := mulmod(mulmod(r, r, pp), exp(base, iszero(iszero(and(expo, div(bit, 8))))), pp)
        bit := div(bit, 16)
      }
    }
    return r;
  }

  /*
   * Converts a point (x,y,z) expressed in Jacobian coordinates to affine coordinates (x',y',1).
   * @param {x} The x coordinate.
   * @param {y} The y coordinate.
   * @param {z} The z coordinate.
   * @param {p} The modulus p.
   * @return Returns the (x', y') affine coordinates.
   */
  function toAffine(
    uint256 x,
    uint256 y,
    uint256 z,
    uint256 p)
  internal pure returns (uint256, uint256)
  {
    uint256 zInv = invMod(z, p);
    uint256 zInv2 = mulmod(zInv, zInv, p);
    uint256 x2 = mulmod(x, zInv2, p);
    uint256 y2 = mulmod(y, mulmod(zInv, zInv2, p), p);
    return (x2, y2);
  }

  /*
   * Derives the y coordinate from a compressed-format point x as in SEC-1.
   * Source: https://www.secg.org/SEC1-Ver-1.0.pdf.
   * @param {prefix} The parity byte (0x02 even, 0x03 odd).
   * @param {x} The x coordinate.
   * @param {aa} The constant a of curve.
   * @param {bb} The constant b of curve.
   * @param {pp} The modulus p.
   * @return Returns the y coordinate.
   */
  function deriveY(
    uint8 prefix,
    uint256 x,
    uint256 aa,
    uint256 bb,
    uint256 pp)
  internal pure returns (uint256)
  {
    require(prefix == 0x02 || prefix == 0x03, "Invalid compressed EC point prefix");
    // x^3 + ax + b
    uint256 y2 = addmod(mulmod(x, mulmod(x, x, pp), pp), addmod(mulmod(x, aa, pp), bb, pp), pp);
    y2 = expMod(y2, (pp + 1) / 4, pp);
    // uint256 cmp = yBit ^ y & 1;
    uint256 y = (y2 + prefix) % 2 == 0 ? y2 : pp - y2;
    return y;
  }

  /*
   * Check whether point (x, y) is on curve defined by a, b, and p.
   * @param {x} The x coordinate of P1.
   * @param {y} The y coordinate of P1.
   * @param {aa} The constant a of the curve.
   * @param {bb} The constant b of the curve.
   * @param {pp} The modulus p.
   * @return Returns true if (x, y) is on the curve, false otherwise.
   */
  function isOnCurve(
    uint x,
    uint y,
    uint aa,
    uint bb,
    uint pp)
  internal pure returns (bool)
  {
    if (0 == x || x >= pp || 0 == y || y >= pp) {
      return false;
    }
    // y^2
    uint lhs = mulmod(y, y, pp);
    // x^3
    uint rhs = mulmod(mulmod(x, x, pp), x, pp);
    if (aa != 0) {
      // x^3 + a*x
      rhs = addmod(rhs, mulmod(x, aa, pp), pp);
    }
    if (bb != 0) {
      // x^3 + a*x + b
      rhs = addmod(rhs, bb, pp);
    }
    return lhs == rhs;
  }

  /*
   * Calculate inverse (x, -y) of point (x, y).
   * @param {x} The x coordinate of P1.
   * @param {y} The y coordinate of P1.
   * @param {p} The modulus p.
   * @return Return (x, -y).
   */
  function ecInv(
    uint256 x,
    uint256 y,
    uint256 p)
  internal pure returns (uint256, uint256)
  {
    return (x, (p - y) % p);
  }

  /*
  * Add two points (x1, y1) and (x2, y2) in affine coordinates.
  * @param {x1} The x coordinate x of P1.
  * @param {y1} The y coordinate y of P1.
  * @param {x2} The x coordinate x of P2.
  * @param {y2} The y coordinate y of P2.
  * @param {aa} constant a of the curve.
  * @param {pp} The modulus p.
  * @return Returns (qx, qy) = P1+P2 in affine coordinates.
  */
  function ecAdd(
    uint256 x1,
    uint256 y1,
    uint256 x2,
    uint256 y2,
    uint256 aa,
    uint256 pp)
  internal pure returns (uint256, uint256)
  {
    uint x = 0;
    uint y = 0;
    uint z = 0;
    // Double if x1==x2 else add
    if (x1 == x2) {
      // y1 = -y2 mod p
      if (addmod(y1, y2, pp) == 0) {
        return (0, 0);
      } else {
        // P1 = P2
        (x, y, z) = jacDouble(
          x1,
          y1,
          1,
          aa,
          pp);
      }
    } else {
      (x, y, z) = jacAdd(
        x1,
        y1,
        1,
        x2,
        y2,
        1,
        pp);
    }
    // Get back to affine
    return toAffine(
      x,
      y,
      z,
      pp);
  }

  /*
   * Substract two points (x1, y1) and (x2, y2) in affine coordinates.
   * @param {x1} The x coordinate of P1.
   * @param {y1} The y coordinate of P1.
   * @param {x2} The x coordinate of P2.
   * @param {y2} The y coordinate of P2.
   * @param {aa} The constant a of the curve.
   * @param {pp} The modulus p.
   * @return Returns (qx, qy) = P1-P2 in affine coordinates.
   */
  function ecSub(
    uint256 x1,
    uint256 y1,
    uint256 x2,
    uint256 y2,
    uint256 aa,
    uint256 pp)
  internal pure returns (uint256, uint256)
  {
    // Invert square
    (uint256 x, uint256 y) = ecInv(x2, y2, pp);
    // P1-square
    return ecAdd(
      x1,
      y1,
      x,
      y,
      aa,
      pp);
  }

  /*
   * Multiplies a point (x1, y1, z1) by a scalar d in affine coordinates.
   * @param {k} The scalar d to multiply by.
   * @param {x} The x coordinate of P1.
   * @param {y} The y coordinate of P1.
   * @param {aa} The constant a of the curve.
   * @param {pp} The modulus p.
   * @return (qx, qy) = d*P in affine coordinates.
   */
  function ecMul(
    uint256 k,
    uint256 x,
    uint256 y,
    uint256 aa,
    uint256 pp)
  internal pure returns (uint256, uint256)
  {
    // Jacobian multiplication
    (uint256 x1, uint256 y1, uint256 z1) = jacMul(
      k,
      x,
      y,
      1,
      aa,
      pp);
    // Get back to affine
    return toAffine(
      x1,
      y1,
      z1,
      pp);
  }

  /*
   * Adds two points (x1, y1, z1) and (x2 y2, z2).
   * @param {x1} The coordinate x of P1.
   * @param {y1} The coordinate y of P1.
   * @param {z1} The coordinate z of P1.
   * @param {x2} The coordinate x of square.
   * @param {y2} The coordinate y of square.
   * @param {z2} The coordinate z of square.
   * @param {pp} The modulus p.
   * @return Returns (qx, qy, qz) as P1 + square in Jacobian.
   */
  function jacAdd(
    uint256 x1,
    uint256 y1,
    uint256 z1,
    uint256 x2,
    uint256 y2,
    uint256 z2,
    uint256 pp)
  internal pure returns (uint256, uint256, uint256)
  {
    if (x1 == 0 && y1 == 0)
      return (x2, y2, z2);
    if (x2 == 0 && y2 == 0)
      return (x1, y1, z1);
    // We follow the equations described in https://pdfs.semanticscholar.org/5c64/29952e08025a9649c2b0ba32518e9a7fb5c2.pdf Section 5
    uint[4] memory zs;
    // z1^2, z1^3, z2^2, z2^3
    zs[0] = mulmod(z1, z1, pp);
    zs[1] = mulmod(z1, zs[0], pp);
    zs[2] = mulmod(z2, z2, pp);
    zs[3] = mulmod(z2, zs[2], pp);
    // u1, s1, u2, s2
    zs = [
    mulmod(x1, zs[2], pp),
    mulmod(y1, zs[3], pp),
    mulmod(x2, zs[0], pp),
    mulmod(y2, zs[1], pp)
    ];
    // In case of zs[0] == zs[2] && zs[1] == zs[3], double function should be used
    require(zs[0] != zs[2] || zs[1] != zs[3], "Use jacDouble function instead");
    uint[4] memory hr;
    //h
    hr[0] = addmod(zs[2], pp - zs[0], pp);
    //r
    hr[1] = addmod(zs[3], pp - zs[1], pp);
    //h^2
    hr[2] = mulmod(hr[0], hr[0], pp);
    // h^3
    hr[3] = mulmod(hr[2], hr[0], pp);
    // qx = -h^3  -2u1h^2+r^2
    uint256 qx = addmod(mulmod(hr[1], hr[1], pp), pp - hr[3], pp);
    qx = addmod(qx, pp - mulmod(2, mulmod(zs[0], hr[2], pp), pp), pp);
    // qy = -s1*z1*h^3+r(u1*h^2 -x^3)
    uint256 qy = mulmod(hr[1], addmod(mulmod(zs[0], hr[2], pp), pp - qx, pp), pp);
    qy = addmod(qy, pp - mulmod(zs[1], hr[3], pp), pp);
    // qz = h*z1*z2
    uint256 qz = mulmod(hr[0], mulmod(z1, z2, pp), pp);
    return (qx, qy, qz);
  }

  /*
   * Doubles a points (x, y, z).
   * @param {xx} The x coordinate of P1.
   * @param {yy} The y coordinate of P1.
   * @param {zz} The z coordinate of P1.
   * @param {aa} The scalar a in the curve equation.
   * @param {pp} The modulus p.
   * @return Returns (qx, qy, qz) as 2P in Jacobian.
   */
  function jacDouble(
    uint256 xx,
    uint256 yy,
    uint256 zz,
    uint256 aa,
    uint256 pp)
  internal pure returns (uint256, uint256, uint256)
  {
    if (zz == 0)
      return (xx, yy, zz);
    // We follow the equations described in https://pdfs.semanticscholar.org/5c64/29952e08025a9649c2b0ba32518e9a7fb5c2.pdf Section 5
    // Note: there is a bug in the paper regarding the m parameter, M=3*(x1^2)+a*(z1^4)
    // x, y, z at this point represent the squares of xx, yy, zz
    uint256 x = mulmod(xx, xx, pp);
    //x1^2
    uint256 y = mulmod(yy, yy, pp);
    //y1^2
    uint256 z = mulmod(zz, zz, pp);
    //z1^2
    // s
    uint s = mulmod(4, mulmod(xx, y, pp), pp);
    // m
    uint m = addmod(mulmod(3, x, pp), mulmod(aa, mulmod(z, z, pp), pp), pp);
    // x, y, z at this point will be reassigned and rather represent qx, qy, qz from the paper
    // This allows to reduce the gas cost and stack footprint of the algorithm
    // qx
    x = addmod(mulmod(m, m, pp), pp - addmod(s, s, pp), pp);
    // qy = -8*y1^4 + M(S-T)
    y = addmod(mulmod(m, addmod(s, pp - x, pp), pp), pp - mulmod(8, mulmod(y, y, pp), pp), pp);
    // qz = 2*y1*z1
    z = mulmod(2, mulmod(yy, zz, pp), pp);
    return (x, y, z);
  }

  /*
   * Multiplies a point (x, y, z) by a scalar d.
   * @param {d} Scalar to multiply with.
   * @param {x} The x coordinate of P1.
   * @param {y} coordinate y of P1.
   * @param {z} coordinate z of P1.
   * @param {aa} constant of curve.
   * @param {pp} the modulus.
   * @return Returns (qx, qy, qz) as d*P1 in Jacobian.
   */
  function jacMul(
    uint256 d,
    uint256 x,
    uint256 y,
    uint256 z,
    uint256 aa,
    uint256 pp)
  internal pure returns (uint256, uint256, uint256)
  {
    // Early return in case that d == 0
    if (d == 0) {
      return (x, y, z);
    }
    uint256 remaining = d;
    uint256 qx = 0;
    uint256 qy = 0;
    uint256 qz = 1;
    // Double and add algorithm
    while (remaining != 0) {
      if ((remaining & 1) != 0) {
        (qx, qy, qz) = jacAdd(
          qx,
          qy,
          qz,
          x,
          y,
          z,
          pp);
      }
      remaining = remaining / 2;
      (x, y, z) = jacDouble(
        x,
        y,
        z,
        aa,
        pp);
    }
    return (qx, qy, qz);
  }
}
