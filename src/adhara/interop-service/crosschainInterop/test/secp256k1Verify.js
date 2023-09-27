const SECP256K1Verify = artifacts.require("SECP256K1Verify");
const createHash = require('create-hash')
const ecKey = require("ec-key");

contract("SECP256K1Verify", async accounts => {

  let instance = null;
  let messageHash;
  let message;
  let publicKey;
  let parity;
  let signature;

  beforeEach(async () => {
    instance = await SECP256K1Verify.new()

    let msg = Math.random().toString(36).replace(/[^a-z]+/g, '').substr(0, 10);
    let buf = Buffer.from(msg, 'utf8')
    message = '0x' + buf.toString('hex');
    // Generate key
    let priKey = ecKey.createECKey('secp256k1');
    let pubKey = priKey.asPublicECKey();
    publicKey = [
      '0x' + pubKey.x.toString('hex'),
      '0x' + pubKey.y.toString('hex')
    ];
    //console.log(publicKey[0])
    //console.log(publicKey[1])
    //console.log(parity)
    let asInt = BigInt(publicKey[1])
    let yBit = ((asInt >> 0n) & 1n) | 2n
    parity = '0x0'+ yBit.toString(16)
    let sigString = priKey.createSign('SHA256').update(msg).sign('hex').toString('hex');
    // Reformat signature / extract coordinates.
    let xlength = 2 * ('0x' + sigString.slice(6, 8));
    sigString = sigString.slice(8);
    let r = '0x' + sigString.slice(0, xlength)
    r = r.replace(/^(0x)0+((\w{4})+)$/, "$1$2")
    let s = '0x' + sigString.slice(xlength + 4)
    s = s.replace(/^(0x)0+((\w{4})+)$/, "$1$2")
    signature = [r, s];
    //console.log(signature[0])
    //console.log(signature[1])
  })

  it('confirm valid signature (#1)', async() => {
    let result = await instance.verifySignature(publicKey, signature, message);
    assert.equal(result, true);
  });

  it('confirm valid signature (#2)', async() => {
    let result = await instance.verifySignature(publicKey, signature, message);
    assert.equal(result, true);
  });

  it('confirm valid signature (#3)', async() => {
    let result = await instance.verify(publicKey[0], signature[0], signature[1], parity, message);
    assert.equal(result, true);
  });

  it('confirm valid signature (#4)', async() => {
    let result = await instance.verify(publicKey[0], signature[0], signature[1], parity, message);
    assert.equal(result, true);
  });

  it('confirm valid signature (#5)', async () =>  {
    let result = await instance.verify(publicKey[0], signature[0], signature[1], parity, message);
    assert.equal(result, true);
  });

  it('reject signature with flipped public key coordinates ([x,y] >> [y,x])', async() => {
    let flippedPublicKey = [publicKey[1], publicKey[0]];
    let result = await instance.verifySignature(flippedPublicKey, signature, message);
    assert.equal(result, false);
  });

  it('reject signature with flipped signature values ([r,s] >> [s,r])', async() => {
    let flippedSignature = [signature[1], signature[0]];
    let result = await instance.verifySignature(publicKey, flippedSignature, message);
    assert.equal(result, false);
  });

  it('reject signature with invalid message hash', async() => {
    let invalidMsg = Math.random().toString(36).replace(/[^a-z]+/g, '').substr(0, 10);
    let imBuf = Buffer.from(invalidMsg, 'utf8')
    let imHash = createHash('sha256').update(imBuf).digest()
    let imHasHex = imHash.toString('hex')
    let invalidMessageHash = '0x' + imHasHex
    let invalidMessage = '0x' + imBuf.toString('hex');
    let result = await instance.verifySignature(publicKey, signature, invalidMessage);
    assert.equal(result, false);
  });
})
