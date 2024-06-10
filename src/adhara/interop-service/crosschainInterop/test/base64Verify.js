const Base64Verify = artifacts.require('Base64Verify');

contract('Base64', async (accounts) => {
  let instance = null;

  beforeEach(async () => {
    instance = await Base64Verify.new();
  });

  for (const { title, input, expected } of [
    { title: 'should be able to convert to base64 encoded string with double padding', input: 'test', expected: 'dGVzdA==' },
    { title: 'should be able to convert to base64 encoded string with single padding', input: 'test1', expected: 'dGVzdDE=' },
    { title: 'should be able to convert to base64 encoded string without padding', input: 'test12', expected: 'dGVzdDEy' },
    { title: 'should be able to convert to base64 encoded string (/ case)', input: 'où', expected: 'b/k=' },
    { title: 'should be able to convert to base64 encoded string (+ case)', input: 'zs~1t8', expected: 'enN+MXQ4' },
    { title: 'should be able to convert empty bytes', input: '', expected: '' },
  ]) {
    it(title, async function () {
      const buffer = Buffer.from(input, 'ascii');
      assert.equal(await instance.encode(buffer), expected);
    });
  }
});
