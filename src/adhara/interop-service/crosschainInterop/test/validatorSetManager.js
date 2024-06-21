const InteropManager = artifacts.require('InteropManager');
const ValidatorSetManager = artifacts.require('ValidatorSetManager');
const assert = require('assert');
const { ethers } = require('ethers');
const abiCoder = new ethers.utils.AbiCoder

let vsm = null, im = null;
const destinationNetworkId = 2
const destinationNetworkId2 = 3
const eventSelector = web3.utils.sha3('CrosschainFunctionCall(uint256,address,bytes)')
const eventParams = ['uint256','address','bytes']
const funcSelector = web3.utils.sha3('setValidatorList(uint256,uint256,address[])').slice(0, 10)
const validators = ['0xca31306798b41bc81c43094a1e0462890ce7a673']

contract('ValidatorSetManager', async accounts => {
  beforeEach(async () => {
    im = await InteropManager.new();
    await im.addRemoteDestinationNetwork(destinationNetworkId, '0xc23cdfef6ec7b1b39c6cb898d7acc71437f167bd')
    await im.enableRemoteDestinationNetwork(destinationNetworkId)
    await im.addRemoteDestinationNetwork(destinationNetworkId2, '0xc23cdfef6ec7b1b39c6cb898d7acc71437f167be')
    await im.enableRemoteDestinationNetwork(destinationNetworkId2)

    vsm = await ValidatorSetManager.new();
    vsm.setInteropManager(im.address)
  })


  it('should be able to set validators and get events to sync remote destination chain interoperability config', async function () {
    const validatorsBefore = await vsm.getValidators()
    assert.equal(validatorsBefore.length, 0)
    let result = await vsm.setValidatorsAndSyncRemotes(validators)
    assert.equal(result.receipt.status, true)
    assert.equal(result.receipt.rawLogs.length, 5)
    let event1 = result.receipt.rawLogs.some(l => {
      if (l.topics[0] === eventSelector) {
        let decodedLog = abiCoder.decode(eventParams, l.data)
        return decodedLog[2].startsWith(funcSelector) && Number(decodedLog[0]._hex) === Number(destinationNetworkId)
      }
      return false
    });
    assert.ok(event1, 'CrosschainFunctionCall event with setValidatorList was not emitted for chain [' + destinationNetworkId + ']');
    let event2 = result.receipt.rawLogs.some(l => {
      if (l.topics[0] === eventSelector) {
        let decodedLog = abiCoder.decode(eventParams, l.data)
        return decodedLog[2].startsWith(funcSelector) && Number(decodedLog[0]._hex) === Number(destinationNetworkId2)
      }
      return false
    });
    assert.ok(event2, 'CrosschainFunctionCall event with setValidatorList was not emitted for chain ['+ destinationNetworkId2 + ']');
    let foundSetValidatorsExecuted = false, foundSetValidatorsAndSyncRemoteExecuted = false;
    for (let log of result.receipt.logs) {
      if (log.event === 'SetValidatorsExecuted') foundSetValidatorsExecuted++;
      if (log.event === 'SetValidatorsAndSyncRemoteExecuted') foundSetValidatorsAndSyncRemoteExecuted++;
    }
    assert.equal(foundSetValidatorsExecuted, 1)
    assert.equal(foundSetValidatorsAndSyncRemoteExecuted, 2)
    const validatorsAfter = await vsm.getValidators()
    assert.equal(validatorsAfter.length, 1)
    assert.equal(validatorsAfter[0].toLowerCase(), validators[0])
  });

  it('should be able to get validators and events to sync remote destination chain interoperability config', async function () {
    let setres = await vsm.setValidators(validators)
    assert.equal(setres.receipt.status, true)
    let result = await vsm.getValidatorsAndSyncRemotes()
    assert.equal(result.receipt.status, true)
    assert.equal(result.receipt.rawLogs.length, 4)
    let event1 = result.receipt.rawLogs.some(l => {
      if (l.topics[0] === eventSelector) {
        let decodedLog = abiCoder.decode(eventParams, l.data)
        return decodedLog[2].startsWith(funcSelector) && Number(decodedLog[0]._hex) === Number(destinationNetworkId)
      }
      return false
    });
    assert.ok(event1, 'CrosschainFunctionCall event with setValidatorList was not emitted for chain [' + destinationNetworkId + ']');
    let event2 = result.receipt.rawLogs.some(l => {
      if (l.topics[0] === eventSelector) {
        let decodedLog = abiCoder.decode(eventParams, l.data)
        return decodedLog[2].startsWith(funcSelector) && Number(decodedLog[0]._hex) === Number(destinationNetworkId2)
      }
      return false
    });
    assert.ok(event2, 'CrosschainFunctionCall event with setValidatorList was not emitted for chain ['+ destinationNetworkId2 + ']');
    let foundGetValidatorsAndSyncRemoteExecuted = false;
    for (let log of result.receipt.logs) {
      if (log.event === 'GetValidatorsAndSyncRemoteExecuted') foundGetValidatorsAndSyncRemoteExecuted++;
    }
    assert.equal(foundGetValidatorsAndSyncRemoteExecuted, 2)
  });
})
