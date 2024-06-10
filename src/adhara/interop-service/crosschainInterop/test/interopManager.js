const ta = require('truffle-assertions');
const {v4: uuidv4} = require("uuid");

const InteropManager = artifacts.require('InteropManager');

const connectorAddress = '0xb93FA4E9Cd33aE12dF0E4848D972169c5d2F5b8f'
const remoteChainStatusNotExistent = '0x0000000000000000000000000000000000000000000000000000000000000000'
const remoteChainStatusCreated = '0x4352454154454400000000000000000000000000000000000000000000000000'
const remoteChainStatusEnabled = '0x454e41424c454400000000000000000000000000000000000000000000000000'
const remoteChainStatusDisabled = '0x44495341424c4544000000000000000000000000000000000000000000000000'

let im = null;

contract('InteropManager', async accounts => {
  beforeEach(async () => {
    im = await InteropManager.new();
  })

  it('should be able to add and/or remove a remote source network', async () => {
    let requestToAdd = await im.addRemoteSourceNetwork(0, connectorAddress)
    assert.equal(requestToAdd.receipt.status, true)
    let responseAfterAdd = await im.getRemoteSourceNetworkData(0);
    assert.equal(responseAfterAdd.connectorAddress, connectorAddress)
    assert.equal(responseAfterAdd.status, remoteChainStatusCreated)
    let requestToRemove = await im.removeRemoteSourceNetwork(0)
    assert.equal(requestToRemove.receipt.status, true)
    let responseAfterRemove = await im.getRemoteSourceNetworkData(0)
    assert.equal(responseAfterRemove.status, remoteChainStatusNotExistent)
  })

  it('should be able to enable and/or disable a remote source networks', async () => {
    let requestToAdd = await im.addRemoteSourceNetwork(0, connectorAddress)
    assert.equal(requestToAdd.receipt.status, true)
    let requestToEnable = await im.enableRemoteSourceNetwork(0)
    assert.equal(requestToEnable.receipt.status, true)
    let responseAfterEnable = await im.getRemoteSourceNetworkData(0);
    assert.equal(responseAfterEnable.connectorAddress, connectorAddress)
    assert.equal(responseAfterEnable.status, remoteChainStatusEnabled)
    let requestToDisable = await im.disableRemoteSourceNetwork(0)
    assert.equal(requestToDisable.receipt.status, true)
    let responseAfterDisable = await im.getRemoteSourceNetworkData(0);
    assert.equal(responseAfterDisable.connectorAddress, connectorAddress)
    assert.equal(responseAfterDisable.status, remoteChainStatusDisabled)
    let requestToRemove = await im.removeRemoteSourceNetwork(0)
    assert.equal(requestToRemove.receipt.status, true)
  })

  it('should be able to list remote source networks', async () => {
    let requestToListNone = await im.listRemoteSourceNetworks(0, 10)
    assert.equal(requestToListNone.moreItems, false)
    assert.equal(requestToListNone.items.length, 0)
    for (let i = 0; i < 4; i++) {
      let requestToAdd = await im.addRemoteSourceNetwork(i, connectorAddress)
      assert.equal(requestToAdd.receipt.status, true)
    }
    let requestToListNoLimit = await im.listRemoteSourceNetworks(0, 0)
    assert.equal(requestToListNoLimit.moreItems, false)
    assert.equal(requestToListNoLimit.providedStartIndex, 0)
    assert.equal(requestToListNoLimit.providedLimit, 0)
    assert.equal(requestToListNoLimit.items.length, 4)
    for (let i = 0; i < requestToListNoLimit.items.length; i++) {
      assert.equal(requestToListNoLimit.items[i], i)
    }
    let requestToListWithLimit = await im.listRemoteSourceNetworks(0, 2)
    assert.equal(requestToListWithLimit.moreItems, true)
    assert.equal(requestToListWithLimit.providedStartIndex, 0)
    assert.equal(requestToListWithLimit.providedLimit, 2)
    assert.equal(requestToListWithLimit.items.length, 2)
    for (let i = 0; i < requestToListWithLimit.items.length; i++) {
      assert.equal(requestToListWithLimit.items[i], i)
    }
    let requestToListWithStartIndex = await im.listRemoteSourceNetworks(2, 2)
    assert.equal(requestToListWithStartIndex.moreItems, false)
    assert.equal(requestToListWithStartIndex.providedStartIndex, 2)
    assert.equal(requestToListWithStartIndex.providedLimit, 2)
    assert.equal(requestToListWithStartIndex.items.length, 2)
    for (let i = 0; i < requestToListWithStartIndex.items.length; i++) {
      assert.equal(requestToListWithStartIndex.items[i], i+2)
    }
    let requestToRemove = await im.removeRemoteSourceNetwork(1)
    assert.equal(requestToRemove.receipt.status, true)
    let requestToListAfterRemoval = await im.listRemoteSourceNetworks(0, 0)
    assert.equal(requestToListAfterRemoval.moreItems, false)
    assert.equal(requestToListAfterRemoval.providedStartIndex, 0)
    assert.equal(requestToListAfterRemoval.providedLimit, 0)
    assert.equal(requestToListAfterRemoval.items.length, 3)
    assert.equal(requestToListAfterRemoval.items[0], 0)
    await im.removeRemoteSourceNetwork(0)
    for (let i = 1; i < requestToListAfterRemoval.items.length; i++) {
      assert.equal(requestToListAfterRemoval.items[i], i+1)
      await im.removeRemoteSourceNetwork(i+1)
    }
  })

  it('should fail to list remote source networks with invalid start index', async () => {
    for (let i = 0; i < 2; i++) {
      let requestToAdd = await im.addRemoteSourceNetwork(i, connectorAddress)
      assert.equal(requestToAdd.receipt.status, true)
    }
    await ta.reverts(im.listRemoteSourceNetworks(2, 0))
    for (let i = 0; i < 2; i++) {
      await im.removeRemoteSourceNetwork(i)
    }
  })

  it('should be able to add and/or remove a remote destination network', async () => {
    let requestToAdd = await im.addRemoteDestinationNetwork(0, connectorAddress)
    assert.equal(requestToAdd.receipt.status, true)
    let responseAfterAdd = await im.getRemoteDestinationNetworkData(0);
    assert.equal(responseAfterAdd.connectorAddress, connectorAddress)
    assert.equal(responseAfterAdd.status, remoteChainStatusCreated)
    let requestToRemove = await im.removeRemoteDestinationNetwork(0)
    assert.equal(requestToRemove.receipt.status, true)
    let responseAfterRemove = await im.getRemoteSourceNetworkData(0)
    assert.equal(responseAfterRemove.status, remoteChainStatusNotExistent)
  })

  it('should be able to enable and/or disable a remote destination network', async () => {
    let requestToAdd = await im.addRemoteDestinationNetwork(0, connectorAddress)
    assert.equal(requestToAdd.receipt.status, true)
    let requestToEnable = await im.enableRemoteDestinationNetwork(0)
    assert.equal(requestToEnable.receipt.status, true)
    let responseAfterEnable = await im.getRemoteDestinationNetworkData(0);
    assert.equal(responseAfterEnable.connectorAddress, connectorAddress)
    assert.equal(responseAfterEnable.status, remoteChainStatusEnabled)
    let requestToDisable = await im.disableRemoteDestinationNetwork(0)
    assert.equal(requestToDisable.receipt.status, true)
    let responseAfterDisable = await im.getRemoteDestinationNetworkData(0);
    assert.equal(responseAfterDisable.connectorAddress, connectorAddress)
    assert.equal(responseAfterDisable.status, remoteChainStatusDisabled)
    let requestToRemove = await im.removeRemoteDestinationNetwork(0)
    assert.equal(requestToRemove.receipt.status, true)
  })

  it('should be able to list remote destination networks', async () => {
    let requestToListNone = await im.listRemoteDestinationNetworks(0, 10)
    assert.equal(requestToListNone.moreItems, false)
    assert.equal(requestToListNone.items.length, 0)
    for (let i = 0; i < 4; i++) {
      let requestToAdd = await im.addRemoteDestinationNetwork(i, connectorAddress)
      assert.equal(requestToAdd.receipt.status, true)
    }
    let requestToListNoLimit = await im.listRemoteDestinationNetworks(0, 0)
    assert.equal(requestToListNoLimit.moreItems, false)
    assert.equal(requestToListNoLimit.providedStartIndex, 0)
    assert.equal(requestToListNoLimit.providedLimit, 0)
    assert.equal(requestToListNoLimit.items.length, 4)
    for (let i = 0; i < requestToListNoLimit.items.length; i++) {
      assert.equal(requestToListNoLimit.items[i], i)
    }
    let requestToListWithLimit = await im.listRemoteDestinationNetworks(0, 2)
    assert.equal(requestToListWithLimit.moreItems, true)
    assert.equal(requestToListWithLimit.providedStartIndex, 0)
    assert.equal(requestToListWithLimit.providedLimit, 2)
    assert.equal(requestToListWithLimit.items.length, 2)
    for (let i = 0; i < requestToListWithLimit.items.length; i++) {
      assert.equal(requestToListWithLimit.items[i], i)
    }
    let requestToListWithStartIndex = await im.listRemoteDestinationNetworks(2, 2)
    assert.equal(requestToListWithStartIndex.moreItems, false)
    assert.equal(requestToListWithStartIndex.providedStartIndex, 2)
    assert.equal(requestToListWithStartIndex.providedLimit, 2)
    assert.equal(requestToListWithStartIndex.items.length, 2)
    for (let i = 0; i < requestToListWithStartIndex.items.length; i++) {
      assert.equal(requestToListWithStartIndex.items[i], i+2)
    }
    let requestToRemove = await im.removeRemoteDestinationNetwork(1)
    assert.equal(requestToRemove.receipt.status, true)
    let requestToListAfterRemoval = await im.listRemoteDestinationNetworks(0, 0)
    assert.equal(requestToListAfterRemoval.moreItems, false)
    assert.equal(requestToListAfterRemoval.providedStartIndex, 0)
    assert.equal(requestToListAfterRemoval.providedLimit, 0)
    assert.equal(requestToListAfterRemoval.items.length, 3)
    assert.equal(requestToListAfterRemoval.items[0], 0)
    await im.removeRemoteDestinationNetwork(0)
    for (let i = 1; i < requestToListAfterRemoval.items.length; i++) {
      assert.equal(requestToListAfterRemoval.items[i], i+1)
      await im.removeRemoteDestinationNetwork(i+1)
    }
  })

  it('should fail to list remote destination chains with invalid start index', async () => {
    for (let i = 0; i < 2; i++) {
      let requestToAdd = await im.addRemoteDestinationNetwork(i, connectorAddress)
      assert.equal(requestToAdd.receipt.status, true)
    }
    await ta.reverts(im.listRemoteDestinationNetworks(2, 0))
    for (let i = 0; i < 2; i++) {
      await im.removeRemoteDestinationNetwork(i)
    }
  })

  it('should be able to emit cross blockchain call event', async () => {
    let requestToAdd = await im.addRemoteDestinationNetwork(0, connectorAddress)
    assert.equal(requestToAdd.receipt.status, true)
    let result = await im.outboundCall(0, '0xE903048bBb91310cE915ce08b40e79BFF13A78f0', '0x123')
    assert.equal(result.receipt.status, true)
    let found = false;
    for (let log of result.receipt.logs) {
      if (log.event === 'CrosschainFunctionCall')
        found = true;
    }
    assert.equal(found, true)
    await im.removeRemoteDestinationNetwork(0)
  })

})
