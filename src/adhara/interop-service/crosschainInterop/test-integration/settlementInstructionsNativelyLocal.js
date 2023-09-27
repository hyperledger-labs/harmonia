const config = require('../config/harmonia-config.json');
const { v4: uuidv4 } = require('uuid');
const fs = require('fs')
const path = require('path')
const assert = require('assert');
const Logger = require('../src/CrosschainSDKUtils/logger.js')
const Graph = require('../src/RunGraph')
const AssetTokenJson = require("../build/contracts/IToken.json");

config.logLevel = !!process.env.log_level && process.env.log_level.length > 0 ? process.env.log_level : 'silent'
const logger = Logger(config, {})
let helpers

describe('settlement obligations and instructions', function () {

  const testDBDirectory = 'test-integration/test-db-' + uuidv4().substring(0, 8)
  config.fileDBDirectory = testDBDirectory
  logger.log('debug', 'Create test DB [' + testDBDirectory + ']')

  const graph = Graph(config, { logger })
  const ethClient = graph.ethClient
  const crosschainApplicationSDK = graph.crosschainApplicationSDK
  helpers = crosschainApplicationSDK.helpers

  const systemId = 1
  const counterPartySystemId = 2
  const system1Name = config.chainIdToChainName[systemId]
  const system2Name = config.chainIdToChainName[counterPartySystemId]
  const system1FromAccount = config[system1Name].accountIds[0]
  const system1ToAccount = config[system1Name].accountIds[1]
  const system2FromAccount = config[system2Name].accountIds[0]
  const system2ToAccount = config[system2Name].accountIds[1]

  before(async function () {
  })

  after(async function () {
    const dbDirectory = path.resolve(testDBDirectory)
    fs.rmSync(dbDirectory, { force: true, recursive: true })
    logger.log('debug', 'Removed test DB [' + testDBDirectory + ']')
    await crosschainApplicationSDK.stop()
  })

  step('should complete a settlement instruction where an obligation is placed separately', async function () {
    let tradeId = uuidv4().substring(0, 8);
    const amount = 100
    const decimals = 2

    // Initial balances
    const system1FromAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1FromAccount))
    const system1ToAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system1Name, system1ToAccount))
    const system2FromAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2FromAccount))
    const system2ToAccountBalance = Number(await crosschainApplicationSDK.getAvailableBalanceOf(system2Name, system2ToAccount))

    await placeHoldOnEthereum(config, ethClient, system1Name, tradeId, amount, decimals, system1FromAccount, system1ToAccount, config.tradeDetails.notaryId);
    await placeHoldOnEthereum(config, ethClient, system2Name, tradeId, amount, decimals, system2FromAccount, system2ToAccount, config.tradeDetails.notaryId);

    const system2Promise = crosschainApplicationSDK.submitSettlementInstruction(counterPartySystemId, { foreignSystemId: systemId, tradeId: tradeId, fromAccount: system2FromAccount, toAccount: system2ToAccount, currency: 'USD', amount, callbackURL: '', triggerLeadLeg: false, useExistingEarmark: true, useForCancellation: false })
    const system1Promise = crosschainApplicationSDK.submitSettlementInstruction(systemId, { foreignSystemId: counterPartySystemId, tradeId: tradeId, fromAccount: system1FromAccount, toAccount: system1ToAccount, currency: 'GBP', amount, callbackURL: '', triggerLeadLeg: true, useExistingEarmark: true, useForCancellation: false })

    const responses = await Promise.all([system1Promise, system2Promise])

    assert.equal(responses[0].tradeId, tradeId);
    assert.equal(responses[0].sourceSystemId, systemId);
    assert.equal(responses[1].tradeId, tradeId);
    assert.equal(responses[1].sourceSystemId, counterPartySystemId);
    assert.equal(responses[1].systemId, systemId);
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

});

async function placeHoldOnEthereum(config, ethClient, chainName, tradeId, amount, decimals, from, to, notaryId) {
  const operationId = await helpers.getOperationIdFromTradeId(chainName, tradeId, from, to)
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
    config[chainName].contexts.interopService,
    config[chainName].contracts.assetTokenContract.address,
    chainName
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
    config[chainName].contexts.interopService,
    config[chainName].contracts.assetTokenContract.address,
    chainName
  )
  if (perpetualHoldResult.status !== true) {
    return Promise.reject(perpetualHoldResult.error)
  }
}

async function sleep(ms) { return new Promise((resolve) => { setTimeout(resolve, ms); }); }
