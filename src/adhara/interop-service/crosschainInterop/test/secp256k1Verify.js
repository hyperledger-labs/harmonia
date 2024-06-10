const SECP256K1Verify = artifacts.require("SECP256K1Verify");
const createHash = require('create-hash')
const ecKey = require("ec-key");
const { keccak256 } = require('hardhat/internal/util/keccak');
const truffleAssert = require('truffle-assertions');

contract("SECP256K1Verify", async accounts => {

  let instance = null;
  let messageHash;
  let recoverHash;
  let message;
  let publicKey;
  let parity;
  let signature;
  let address;

  beforeEach(async () => {
    instance = await SECP256K1Verify.new()
    // Generate message
    let msg = Math.random()
      .toString(36)
      .replace(/[^a-z]+/g, '')
      .substring(0, 10);
    let buf = Buffer.from(msg, 'utf8')
    message = '0x' + buf.toString('hex');
    // Generate hash
    let sha256Hash = createHash('sha256')
      .update(buf)
      .digest()
    let keccakHash = createHash('sha3-256')
      .update(buf)
      .digest()
    messageHash = '0x' + sha256Hash.toString('hex')
    recoverHash = '0x' + keccakHash.toString('hex')
    // Generate key
    let priKey = ecKey.createECKey('secp256k1');
    let pubKey = priKey.asPublicECKey();
    publicKey = [
      '0x' + pubKey.x.toString('hex'),
      '0x' + pubKey.y.toString('hex')
    ];
    // Compute parity
    let asInt = BigInt(publicKey[1]);
    let yBit = ((asInt >> 0n) & 1n) | 2n
    parity = '0x0' + yBit.toString(16)
    // Derive address from raw public key
    let rawXY = Buffer.concat([pubKey.x, pubKey.y]);
    //console.log('Raw key: 0x' + rawXY.toString('hex'));
    let hashXY = keccak256(rawXY);
    let calcAddress = hashXY.subarray(-20)
      .toString('hex')
      .toLowerCase();
    // Calculate checksum (expressed as upper/lower case in the address)
    let addressHash = keccak256(calcAddress)
      .toString('hex');
    address = '0x';
    for (let i = 0; i < calcAddress.length; i++) {
      if (parseInt(addressHash[i], 16) > 7) {
        address += calcAddress[i].toUpperCase();
      } else {
        address += calcAddress[i];
      }
    }
    //console.log('Derived address: 0x' + address);  // Test with https://www.rfctools.com/ethereum-address-test-tool using the raw key
    // Generate Signature
    let sigString = priKey.createSign('SHA256')
      .update(msg)
      .sign('hex')
      .toString('hex');
    // Reformat signature / extract coordinates.
    let xlength = 2 * ('0x' + sigString.slice(6, 8));
    sigString = sigString.slice(8);
    let r = '0x' + sigString.slice(0, xlength)
    r = r.replace(/^(0x)0+((\w{4})+)$/, "$1$2")
    let s = '0x' + sigString.slice(xlength + 4)
    s = s.replace(/^(0x)0+((\w{4})+)$/, "$1$2")
    signature = [r, s];
  })

  it('should be able to verify valid signature', async () => {
    let result = await instance.verifySignature(publicKey, signature, message);
    assert.equal(result, true);
  });

  it('should NOT be able to verify signature with zero signature value', async () => {
    let result = await instance.verify(publicKey[0], '0x0', signature[1], parity, message);
    assert.equal(result, false);
  });

  it('should NOT be able to verify signature with incorrect signature values', async () => {
    let result = await instance.verify(publicKey[0], '0xFFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551', signature[1], parity, message);
    assert.equal(result, false);
  });

  it('should NOT be able to verify signature with malformed parity', async () => {
    await truffleAssert.reverts(
      instance.verify(publicKey[0], signature[0], signature[1], '0x09', message),
      'Invalid compressed EC point prefix'
    );
  });

  it('should NOT be able to verify signature with incorrect message', async () => {
    let result = await instance.verify(publicKey[0], signature[0], signature[1], parity, '0x1234');
    assert.equal(result, false);
  });

  it('should NOT be able to verify signature with flipped public key coordinates ([x,y] >> [y,x])', async () => {
    let flippedPublicKey = [publicKey[1], publicKey[0]];
    let result = await instance.verifySignature(flippedPublicKey, signature, message);
    assert.equal(result, false);
  });

  it('should NOT be able to verify signature with flipped signature values ([r,s] >> [s,r])', async () => {
    let flippedSignature = [signature[1], signature[0]];
    let result = await instance.verifySignature(publicKey, flippedSignature, message);
    assert.equal(result, false);
  });

  it('should NOT be able to verify signature with invalid message hash', async () => {
    let invalidMsg = Math.random()
      .toString(36)
      .replace(/[^a-z]+/g, '')
      .substring(0, 10);
    let imBuf = Buffer.from(invalidMsg, 'utf8')
    let invalidMessage = '0x' + imBuf.toString('hex');
    let result = await instance.verifySignature(publicKey, signature, invalidMessage);
    assert.equal(result, false);
  });
});
