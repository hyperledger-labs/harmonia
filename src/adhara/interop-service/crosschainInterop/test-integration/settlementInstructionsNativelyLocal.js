const config = require('../config/config.json');
const { v4: uuidv4 } = require('uuid');
const fs = require('fs')
const path = require('path')
const assert = require('assert');
const Logger = require('../src/CrosschainSDKUtils/logger.js')
const Graph = require('../src/RunGraph')
const AssetTokenJson = require("../build/contracts/Token.sol/Token.json");

config.logLevel = !!process.env.log_level && process.env.log_level.length > 0 ? process.env.log_level : 'silent'
const logger = Logger(config, {})
let helpers

describe('settlement obligations and instructions', function () {

  const testDBDirectory = 'test-db-' + uuidv4().substring(0, 8)
  config.fileDBDirectory = testDBDirectory
  logger.log('debug', 'Create test DB [' + testDBDirectory + ']')

  const graph = Graph(config, { logger })
  const ethClient = graph.ethClient
  const crosschainApplicationSDK = graph.crosschainApplicationSDK
  helpers = crosschainApplicationSDK.helpers

  const networkId = 1
  const counterPartyNetworkId = 2
  const system1Name = config.networkIdToNetworkName[networkId]
  const system2Name = config.networkIdToNetworkName[counterPartyNetworkId]
  const system1FromAccount = config[system1Name].accountIds[0]
  const system1ToAccount = config[system1Name].accountIds[1]
  const system2FromAccount = config[system2Name].accountIds[0]
  const system2ToAccount = config[system2Name].accountIds[1]
  const amount = 100
  const decimals = 2

  before(async function () {
  })

  after(async function () {
    const dbDirectory = path.resolve(testDBDirectory)
    fs.rmSync(dbDirectory, { force: true, recursive: true })
    logger.log('debug', 'Removed test DB [' + testDBDirectory + ']')
    await crosschainApplicationSDK.stop()
  })

  step('should complete a settlement instruction where obligations are placed separately', async function () {
    let tradeId = uuidv4().substring(0, 8);

    // Initial balances
    const system1FromAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1FromAccount))
    const system1ToAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1ToAccount))
    const system2FromAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2FromAccount))
    const system2ToAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2ToAccount))

    await placeHoldOnEthereum(config, ethClient, system1Name, tradeId, amount, decimals, system1FromAccount, system1ToAccount, config.tradeDetails.notaryId);
    await placeHoldOnEthereum(config, ethClient, system2Name, tradeId, amount, decimals, system2FromAccount, system2ToAccount, config.tradeDetails.notaryId);

    const system2Promise = crosschainApplicationSDK.submitSettlementInstruction(counterPartyNetworkId, { remoteNetworkId: networkId, tradeId: tradeId, fromAccount: system2FromAccount, toAccount: system2ToAccount, currency: 'USD', amount, callbackURL: '', triggerLeadLeg: false, useExistingEarmark: true, useForCancellation: false })
    const system1Promise = crosschainApplicationSDK.submitSettlementInstruction(networkId, { remoteNetworkId: counterPartyNetworkId, tradeId: tradeId, fromAccount: system1FromAccount, toAccount: system1ToAccount, currency: 'GBP', amount, callbackURL: '', triggerLeadLeg: true, useExistingEarmark: true, useForCancellation: false })

    const responses = await Promise.all([system1Promise, system2Promise])

    assert.equal(responses[0].tradeId, tradeId);
    assert.equal(responses[0].sourceNetworkId, networkId);
    assert.equal(responses[1].tradeId, tradeId);
    assert.equal(responses[1].sourceNetworkId, counterPartyNetworkId);
    assert.equal(responses[1].networkId, networkId);
    assert.equal(typeof responses[1].encodedInfo, 'string');
    assert.equal(typeof responses[1].signatureOrProof, 'string');

    await sleep(200)
    const system1FromAccountBalanceEnd = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1FromAccount))
    const system1ToAccountBalanceEnd = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1ToAccount))
    const system2FromAccountBalanceEnd = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2FromAccount))
    const system2ToAccountBalanceEnd = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2ToAccount))
    assert.equal(system1FromAccountBalance - amount * Math.pow(10, decimals), system1FromAccountBalanceEnd)
    assert.equal(system1ToAccountBalance + amount * Math.pow(10, decimals), system1ToAccountBalanceEnd)
    assert.equal(system2FromAccountBalance - amount * Math.pow(10, decimals), system2FromAccountBalanceEnd)
    assert.equal(system2ToAccountBalance + amount * Math.pow(10, decimals), system2ToAccountBalanceEnd)
  });

  step('should be able to cancel a settlement instruction on the lead ledger', async function () {
    const tradeId = uuidv4().substring(0, 8);

    // Initial balances
    const system1FromAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1FromAccount))

    await crosschainApplicationSDK.createSettlementObligation(networkId, { tradeId, fromAccount: system1FromAccount, toAccount: system1ToAccount, currency: 'GBP', amount })
    const system1FromAccountBalanceCheck = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1FromAccount))
    assert.equal(system1FromAccountBalance - amount * Math.pow(10, decimals), system1FromAccountBalanceCheck)

    crosschainApplicationSDK.submitSettlementInstruction(networkId, { remoteNetworkId: counterPartyNetworkId, tradeId: tradeId, fromAccount: system1FromAccount, toAccount: system1ToAccount, currency: 'GBP', amount, callbackURL: '', triggerLeadLeg: true, useExistingEarmark: true, useForCancellation: false })
    let response = await crosschainApplicationSDK.getSettlementInstruction(networkId, tradeId, system1FromAccount, system1ToAccount, undefined)
    while (!response) {
      await sleep(100)
      response = await crosschainApplicationSDK.getSettlementInstruction(networkId, tradeId, system1FromAccount, system1ToAccount, undefined)
    }
    while (response.state !== 'waitingForHold') {
      await sleep(100)
      response = await crosschainApplicationSDK.getSettlementInstruction(networkId, tradeId, system1FromAccount, system1ToAccount, undefined)
    }
    assert.equal(response.state, 'waitingForHold');
    await crosschainApplicationSDK.patchSettlementInstruction(networkId, tradeId, system1FromAccount, system1ToAccount, { state: 'cancel' })
    response = await crosschainApplicationSDK.getSettlementInstruction(networkId, tradeId, system1FromAccount, system1ToAccount, undefined)
    while (response.state !== 'cancelled') {
      await sleep(100)
      response = await crosschainApplicationSDK.getSettlementInstruction(networkId, tradeId, system1FromAccount, system1ToAccount, undefined)
    }
    assert.equal(response.tradeId, tradeId);
    assert.equal(response.fromAccount, system1FromAccount);
    assert.equal(response.toAccount, system1ToAccount);
    assert.equal(response.state, 'cancelled');

    await sleep(2000)

    const system1FromAccountBalanceEnd = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1FromAccount))
    assert.equal(system1FromAccountBalanceEnd, system1FromAccountBalance)
  });

  step('should be able to cancel a settlement instruction on the follow ledger', async function () {
    const tradeId = uuidv4().substring(0, 8);

    // Initial balances
    const system2FromAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2FromAccount))

    await crosschainApplicationSDK.createSettlementObligation(counterPartyNetworkId, { tradeId, fromAccount: system2FromAccount, toAccount: system2ToAccount, currency: 'USD', amount })
    const system2FromAccountBalanceCheck = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2FromAccount))
    assert.equal(system2FromAccountBalance - amount * Math.pow(10, decimals), system2FromAccountBalanceCheck)
    crosschainApplicationSDK.submitSettlementInstruction(counterPartyNetworkId, { remoteNetworkId: networkId, tradeId, fromAccount: system2FromAccount, toAccount: system2ToAccount, currency: 'USD', amount, callbackURL: '', triggerLeadLeg: false, useExistingEarmark: true, useForCancellation: false })

    let response = await crosschainApplicationSDK.getSettlementInstruction(counterPartyNetworkId, tradeId, system2FromAccount, system2ToAccount, undefined)
    while (!response) {
      await sleep(100)
      response = await crosschainApplicationSDK.getSettlementInstruction(counterPartyNetworkId, tradeId, system2FromAccount, system2ToAccount, undefined)
    }
    while (response.state !== 'waitingForCrosschainFunctionCall') {
      await sleep(100)
      response = await crosschainApplicationSDK.getSettlementInstruction(counterPartyNetworkId, tradeId, system2FromAccount, system2ToAccount, undefined)
    }
    assert.equal(response.state, 'waitingForCrosschainFunctionCall');

    await crosschainApplicationSDK.patchSettlementInstruction(counterPartyNetworkId, tradeId, system2FromAccount, system2ToAccount, { state: 'cancel' })
    response = await crosschainApplicationSDK.getSettlementInstruction(counterPartyNetworkId, tradeId, system2FromAccount, system2ToAccount, undefined)
    while (response.state !== 'cancelled') {
      await sleep(100)
      response = await crosschainApplicationSDK.getSettlementInstruction(counterPartyNetworkId, tradeId, system2FromAccount, system2ToAccount, undefined)
    }
    assert.equal(response.tradeId, tradeId);
    assert.equal(response.fromAccount, system2FromAccount);
    assert.equal(response.toAccount, system2ToAccount);
    assert.equal(response.state, 'cancelled');

    const system2FromAccountBalanceEnd = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2FromAccount))
    assert.equal(system2FromAccountBalance, system2FromAccountBalanceEnd)
  });

});

async function placeHoldOnEthereum(config, ethClient, networkName, tradeId, amount, decimals, from, to, notaryId) {
  const operationId = await helpers.getOperationIdFromTradeId(networkName, tradeId, from, to)
  const createHoldResult = await ethClient.buildAndSendTx(
    AssetTokenJson.abi,
    'createHold',
    {
      operationId: operationId,
      fromAccount: from,
      toAccount: to,
      notaryId: notaryId,
      amount: ''+amount*Math.pow(10, decimals),
      duration: '30',
      metaData: '',
    },
    config[networkName].contexts.interopService,
    config[networkName].contracts.assetTokenContract.address,
    networkName
  )
  if (createHoldResult.status !== true) {
    return Promise.reject(createHoldResult.error)
  }
  const perpetualHoldResult = await ethClient.buildAndSendTx(
    AssetTokenJson.abi,
    'makeHoldPerpetual',
    {
      operationId: operationId,
    },
    config[networkName].contexts.interopService,
    config[networkName].contracts.assetTokenContract.address,
    networkName
  )
  if (perpetualHoldResult.status !== true) {
    return Promise.reject(perpetualHoldResult.error)
  }
}

async function sleep(ms) { return new Promise((resolve) => { setTimeout(resolve, ms); }); }
