const X509Testbench = artifacts.require('X509Verify');
const truffleAssert = require('truffle-assertions');

contract('X509', async (accounts) => {
  let instance = null;
  const testData = [{
    name: 'SECP256K1 encoded key 1',
    input: '302A300506032B6570032100254B037FA1B0668AB137413E0AC99869D741BAC30D0E8C2AD73459633E352CA3',
    output: '0x254b037fa1b0668ab137413e0ac99869d741bac30d0e8c2ad73459633e352ca3',
  }, {
    name: 'SECP256K1 encoded key 2',
    input: '302A300506032B6570032100B3DC96880DDA9C36169D622B318E14CEA5B99408B7A30991D9822DC2B33D5F55',
    output: '0xb3dc96880dda9c36169d622b318e14cea5b99408b7a30991d9822dc2b33d5f55',
  }, {
    name: 'SECP256K1 encoded key 3',
    input: '302A300506032B6570032100E903C0654289F670B41120B4ABFA12045D22BE0D9640AD644D481AE5A6B923BD',
    output: '0xe903c0654289f670b41120b4abfa12045d22be0d9640ad644d481ae5a6b923bd',
  }, {
    name: 'SECP256R1 encoded key 1',
    input: '3059301306072A8648CE3D020106082A8648CE3D0301070342000448A450E7A340C810034F36C6CE78197CBEC20D123858813F7E27A881BDDFDCFACF21A4115FA93CD9C9A6CCEEFCF548DD3CD4A0EE6CAA7F57EB08AC23E54B6AAC',
    output: '0x48a450e7a340c810034f36c6ce78197cbec20d123858813f7e27a881bddfdcfa',
  }, {
    name: 'ED25519 encoded key 1',
    input: '302A300506032B6570032100031873061D16CE60F37A669297FC765DC335ABF82FBBCBCBF6ED7371125313B9',
    output: '0x031873061d16ce60f37a669297fc765dc335abf82fbbcbcbf6ed7371125313b9',
  }, {
    name: 'ED25519 encoded key 2',
    input: '302A300506032B6570032100E289B3CB7A867E086AD0F7DB191F07B60A6C856D99D73C54AFC86B141303AC78',
    output: '0xe289b3cb7a867e086ad0f7db191f07b60a6c856d99d73c54afc86b141303ac78',
  }, {
    name: 'ED25519 encoded key 3',
    input: '302A300506032B65700321000235296A9025C6F77FC5F2103E7C757988149932130237ECDC905AA3C292A7C8',
    output: '0x0235296a9025c6f77fc5f2103e7c757988149932130237ecdc905aa3c292a7c8',
  }];
  const incorrectData = [{
    name: 'given empty (pre-fixed) hex string',
    input: '',
    output: 'Hex-encoded ASCII string should be of even length greater than or equal to 2, was given string of length=[0]',
  }, {
    name: 'given any encoding with odd length',
    input: '003',
    output: 'Hex-encoded ASCII string should be of even length greater than or equal to 2, was given string of length=[3].',
  }, {
    name: 'given invalid compressed point encoding',
    input: '302A300506032B6570032100031873061D16CE60F37A669297FC765DC335ABF82FBBCBCBF6ED737112',
    output: 'X.509 public key contains an invalid uncompressed point encoding, no uncompressed point indicator',
  }, {
    name: 'given zero',
    input: '00',
    output: 'ASN1 encoding contains a zero value that can not be used as root node length',
  }, {
    name: 'asn1 constructor is absent',
    input: '402A300506032B6570032100254B037FA1B0668AB137413E0AC99869D741BAC30D0E8C2AD73459633E352CA3',
    output: 'ASN1 encoding contains a value that is not a constructed type',
  }, {
    name: 'asn1 bit string value is wrongly encoded',
    input: '302A300506032B6570022100031873061D16CE60F37A669297FC765DC335ABF82FBBCBCBF6ED7371125313B9',
    output: 'ASN1 encoding contains a value that is not of type bit string',
  }, {
    name: 'asn1 bit string is not zero-padded',
    input: '302A300506032B6570032101031873061D16CE60F37A669297FC765DC335ABF82FBBCBCBF6ED7371125313B9',
    output: 'ASN1 encoding only allows for zero-padded bit strings to be converted to byte strings',
  }];

  beforeEach(async () => {
    instance = await X509Testbench.new();
  });

  it('should be able to decode der-encoded X.509 public key', async () => {
    try {
      for (let i = 0; i < testData.length; i++) {
        const result = await instance.decodeKey(testData[i].input);
        assert.equal(testData[i].output, result);
      }
    } catch (err) {
      assert.fail('No revert expected:', err);
    }
  });

  for (let i = 0; i < incorrectData.length; i++) {
    it(`should fail to decode der-encoded x.509 public key when ${incorrectData[i].name}`, async () => {
      await truffleAssert.reverts(
        instance.decodeKeyPayable(incorrectData[i].input),
        incorrectData[i].output,
      );
    });
  }
});
