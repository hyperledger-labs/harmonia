const fs = require('fs')
const readFile = path => fs.readFileSync(path, 'utf8');
const Logger = require('../src/CrosschainSDKUtils/logger.js')
const Graph = require('../src/RunGraph')
const AssetTokenJson = require('../build/contracts/IToken.json')
const assert = require('assert');
const fetch = require("node-fetch");
const { v4: uuidv4 } = require("uuid");
const path = require("path");
const config = require('../config/harmonia-config.json');

const chain0Name = 'bc-sec'
const chain1Name = 'bc-local-gbp'

const x500PartyA = 'O=PartyA, L=London, C=GB'
const bs64PartyA = 'Tz1QYXJ0eUEsIEw9TG9uZG9uLCBDPUdC'
const localPartyA = config[chain1Name].accountIds[1]
const x500PartyB = 'O=PartyB, L=New York, C=US'
const bs64PartyB = 'Tz1QYXJ0eUIsIEw9TmV3IFlvcmssIEM9VVM='
const localPartyB = config[chain1Name].accountIds[0]

config.logLevel = !!process.env.log_level && process.env.log_level.length > 0 ? process.env.log_level : 'silent'
const logger = Logger(config, {})

let helpers

describe('settlement instructions with corda transaction verification scheme', function () {

  const chain1Alice = config[chain1Name].accountIds[1]
  const chain1Bob = config[chain1Name].accountIds[0]
  const expath = process.cwd() + '/test-integration/monitoring-bc-sec'
  const testDBDirectory = 'test-integration/test-db-' + uuidv4().substring(0, 8)
  config.fileDBDirectory = testDBDirectory
  logger.log('debug', "Create test DB [" + testDBDirectory + "]")

  const graph = Graph(config, {
    logger
  })
  const ethClient = graph.ethClient
  const crosschainFunctionCallSDK = graph.crosschainFunctionCallSDK
  const crosschainApplicationSDK = graph.crosschainApplicationSDK
  helpers = crosschainApplicationSDK.helpers
  let amount = 100

  async function handleCordaEventForTransactionVerification(fromChain, toChain, cordaFile) {
    const jsonCordaFile = JSON.parse(readFile(cordaFile).toString())
    let sourceSystemId = config[fromChain].id
    let destinationSystemId = config[toChain].id
    let fromAccount = '' + jsonCordaFile.senderId;
    let toAccount = '' + jsonCordaFile.receiverId;
    let tradeId = '' + jsonCordaFile.tradeId;
    if (cordaFile.includes('earmark')) {
      const submitPromise = await crosschainApplicationSDK.submitSettlementInstruction(destinationSystemId, {
        tradeId: tradeId,
        fromAccount: fromAccount,
        toAccount: toAccount,
        currency: 'GBP',
        amount: amount,
        callbackURL: config[fromChain].providers[0] + '/confirm-dcr',
        triggerLeadLeg: false,
        useExistingEarmark: true,
        useForCancellation: false,
        signatureOrProof: {
          sourceSystemId: Number(sourceSystemId),
          encodedEventData: '' + jsonCordaFile.raw
        }
      })
    } else if (cordaFile.includes('cancel')) {
      const submitPromise = await crosschainApplicationSDK.submitSettlementInstruction(destinationSystemId, {
        tradeId: tradeId,
        fromAccount: fromAccount,
        toAccount: toAccount,
        currency: 'GBP',
        amount: amount,
        callbackURL: config[fromChain].providers[1] + '/resolve-xvp',
        triggerLeadLeg: false,
        useExistingEarmark: true,
        useForCancellation: true,
        signatureOrProof: {
          sourceSystemId: Number(sourceSystemId),
          encodedEventData: '' + jsonCordaFile.raw
        }
      })
    }
  }

  before(async function () {
    crosschainFunctionCallSDK.monitorForCordaEvents(chain0Name, chain1Name, expath, handleCordaEventForTransactionVerification);
  })

  after(async function () {
    const dbDirectory = path.resolve(testDBDirectory)
    fs.rmSync(dbDirectory, { force: true, recursive: true })
    logger.log('debug', 'Removed test DB [' + testDBDirectory + ']')
    await crosschainApplicationSDK.stop()
  })

  step('should complete settlement when both earmarks are in place', async function () {
    let tradeId = uuidv4().substring(0, 8);

    logger.log('debug', 'Check balances');
    const aliceStartEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Alice));
    logger.log('debug', 'Available ' + chain1Name + ' balance for Alice [' + aliceStartEth + ']')
    const bobStartEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Bob));
    logger.log('debug', 'Available ' + chain1Name + ' balance for Bob [' + bobStartEth + ']')

    logger.log('debug', 'Place hold on follow ledger');
    await placeHoldOnEthereum(config, ethClient, chain1Name, tradeId, localPartyB, localPartyA, amount, config.tradeDetails.notaryId);

    logger.log('debug', 'Place hold on lead ledger');
    await placeHoldOnCorda(config, chain0Name, tradeId, amount, expath);

    logger.log('debug', 'Waiting...')
    await sleep(15000)

    let dcrs = await queryDCRs(config, chain0Name, 0, tradeId);
    assert.equal(dcrs.length, 1)
    for (let i = 0; i < dcrs.length; i++) {
      let dcr = dcrs[i].state.data
      assert.equal(dcr.status, 'TRANSFERRED')
    }
    let xvps = await queryXVPs(config, chain0Name, 1, tradeId);
    assert.equal(xvps.length, 0)

    logger.log('debug', 'Check balances');
    const aliceEndEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Alice))
    logger.log('debug', 'Available ' + chain1Name + ' balance for Alice [' + aliceEndEth + ']')
    const bobEndEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Bob))
    logger.log('debug', 'Available ' + chain1Name + ' balance for Bob [' + bobEndEth + ']')
    assert.equal(aliceStartEth + amount, aliceEndEth)
    assert.equal(bobStartEth - amount, bobEndEth)

    logger.log('debug', 'tradeId [' + tradeId + ']: Getting settlement instruction for systemId [' + config[chain1Name].id + ']')
    let systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    while (!systemResponse) {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    while (systemResponse.state !== 'processed') {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    assert.equal(systemResponse.state, 'processed')
  })

  step('should wait for hold indefinitely when ethereum earmark is not in place', async function () {
    let tradeId = uuidv4().substring(0, 8);
    let amount = 1

    logger.log('debug', 'Place hold on lead ledger');
    await placeHoldOnCorda(config, chain0Name, tradeId, amount, expath);

    logger.log('debug', 'Waiting...')
    await sleep(15000)

    let dcrs = await queryDCRs(config, chain0Name, 1, tradeId);
    assert.equal(dcrs.length, 1)
    let xvps = await queryXVPs(config, chain0Name, 1, tradeId);
    assert.equal(xvps.length, 1)

    logger.log('debug', 'tradeId [' + tradeId + ']: Getting settlement instruction for systemId [' + config[chain1Name].id + ']')
    let systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    while (!systemResponse) {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    while (systemResponse.state !== 'waitingForHold') {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    assert.equal(systemResponse.state, 'waitingForHold')
  })

  step('should be able to cancel settlement when corda earmark is not in place', async function () {

    let tradeId = uuidv4().substring(0, 8);

    logger.log('debug', 'Check balances');
    const aliceStartEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Alice));
    logger.log('debug', 'Available ' + chain1Name + ' balance for Alice [', aliceStartEth + ']')
    const bobStartEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Bob));
    logger.log('debug', 'Available ' + chain1Name + ' balance for Bob [', bobStartEth + ']')


    logger.log('debug', 'Place hold on follow ledger');
    await placeHoldOnEthereum(config, ethClient, chain1Name, tradeId, localPartyB, localPartyA, amount, config.tradeDetails.notaryId);

    logger.log('debug', 'Cancel hold on lead ledger');
    await cancelHoldOnCorda(config, chain0Name, tradeId, amount, expath);

    logger.log('debug', 'Waiting...')
    await sleep(25000)

    let dcrs = await queryDCRs(config, chain0Name, 1, tradeId);
    assert.equal(dcrs.length, 0)
    let xvps = await queryXVPs(config, chain0Name, 1, tradeId);
    assert.equal(xvps.length, 0)

    logger.log('debug', 'Check balances');
    const aliceEndEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Alice))
    logger.log('debug', 'Available ' + chain1Name + ' balance for Alice [', aliceEndEth + ']')
    const bobEndEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Bob))
    logger.log('debug', 'Available ' + chain1Name + ' balance for Bob [', bobEndEth + ']')
    assert.equal(aliceStartEth, aliceEndEth)
    assert.equal(bobStartEth, bobEndEth)

    logger.log('debug', 'tradeId [' + tradeId + ']: Getting settlement instruction for systemId [' + config[chain1Name].id + ']')
    let systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    while (!systemResponse) {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    while (systemResponse.state !== 'cancelled') {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    assert.equal(systemResponse.state, 'cancelled')
  })

  step('should be able to cancel settlement when ethereum earmark is not in place', async function () {
    let tradeId = uuidv4().substring(0, 8);

    logger.log('debug', 'Check balances');
    const aliceStartEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Alice));
    logger.log('debug', 'Available ' + chain1Name + ' balance for Alice [' + aliceStartEth + ']')
    const bobStartEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Bob));
    logger.log('debug', 'Available ' + chain1Name + ' balance for Bob [' + bobStartEth + ']')

    logger.log('debug', 'Place hold on lead ledger');
    await placeHoldOnCorda(config, chain0Name, tradeId, amount, expath);

    // Wait a little for the settlement instruction to arrive yet. It can't go through as the ethereum hold does not exist
    await sleep(1000)

    logger.log('debug', 'Cancel hold on follow ledger');
    await cancelHoldOnEthereum(config, crosschainApplicationSDK, chain1Name, tradeId, config[chain0Name].id, config[chain0Name].providers[0]);

    logger.log('debug', 'Waiting...')
    await sleep(15000)

    let dcrs = await queryDCRs(config, chain0Name, 1, tradeId);
    assert.equal(dcrs.length, 0)
    let xvps = await queryXVPs(config, chain0Name, 1, tradeId);
    assert.equal(xvps.length, 0)

    logger.log('debug', 'Check balances');
    const aliceEndEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Alice))
    logger.log('debug', 'Available ' + chain1Name + ' balance for Alice [' + aliceEndEth + ']')
    const bobEndEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Bob))
    logger.log('debug', 'Available ' + chain1Name + ' balance for Bob [' + bobEndEth + ']')
    assert.equal(aliceStartEth, aliceEndEth)
    assert.equal(bobStartEth, bobEndEth)

    logger.log('debug', 'Getting settlement instruction for tradeId [' + tradeId + '] on systemId [' + config[chain1Name].id + ']')
    let systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    while (!systemResponse) {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    while (systemResponse.state !== 'cancelled') {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    assert.equal(systemResponse.state, 'cancelled')
  })

  step('should not be able to cancel settlement via patch when ethereum earmark is in place', async function () {
    let tradeId = uuidv4().substring(0, 8);

    logger.log('debug', 'Place hold on follow ledger');
    await placeHoldOnEthereum(config, ethClient, chain1Name, tradeId, localPartyB, localPartyA, amount, config.tradeDetails.notaryId);

    // The next step fails because no settlement instruction was submitted from the corda system
    logger.log('debug', 'Cancel hold on follow ledger');
    assert.rejects(async function () {
      await cancelHoldOnEthereum(config, crosschainApplicationSDK, chain1Name, tradeId, config[chain0Name].id, config[chain0Name].providers[0]);
    })
  })

  step('should not be able to cancel settlement via bypass when ethereum earmark is in place', async function () {
    logger.log('debug', 'Check balances');
    const bobStartEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Bob));
    logger.log('debug', 'Available ' + chain1Name + ' balance for Bob [' + bobStartEth + ']')

    let tradeId = uuidv4().substring(0, 8);

    logger.log('debug', 'Place hold on follow ledger');
    await placeHoldOnEthereum(config, ethClient, chain1Name, tradeId, localPartyB, localPartyA, amount, config.tradeDetails.notaryId);

    const submitPromise = crosschainApplicationSDK.submitSettlementInstruction(config[chain1Name].id, {
      foreignSystemId: config[chain0Name].id,
      tradeId: tradeId,
      fromAccount: bs64PartyB,
      toAccount: bs64PartyA,
      currency: 'GBP',
      amount: 1,
      callbackURL: '',
      triggerLeadLeg: false,
      useExistingEarmark: true,
      useForCancellation: false,
    })

    await sleep(2000)

    logger.log('debug', 'Cancel hold on follow ledger');
    await cancelHoldOnEthereum(config, crosschainApplicationSDK, chain1Name, tradeId, config[chain0Name].id, config[chain0Name].providers[0]);

    await sleep(15000)

    logger.log('debug', 'Check balances');
    const bobEndEth = Number(await crosschainApplicationSDK.getAvailableBalanceOf(chain1Name, chain1Bob))
    logger.log('debug', 'Available ' + chain1Name + ' balance for Bob [' + bobEndEth + ']')

    assert.equal(bobStartEth - amount, bobEndEth)
  })

  step('should not be able to cancel settlement when corda earmark is in place', async function () {
    let tradeId = uuidv4().substring(0, 8);

    logger.log('debug', 'Place hold on lead ledger');
    await placeHoldOnCorda(config, chain0Name, tradeId, amount, expath);

    logger.log('debug', 'Waiting...')
    await sleep(15000)

    logger.log('debug', 'Cancel hold on lead ledger');
    const canParams = {
      'tradeId': '' + tradeId
    }
    await postJson(config[chain0Name].providers[1] + '/cancel-xvp', canParams, expath)
      .then(function (data) {
        logger.log('debug', 'Cancel xvp transaction: Success');
        logger.log('debug', JSON.stringify(data, null, 2));
      })
      .catch(function (err) {
        logger.log('debug', 'Cancel xvp transaction: Error: ', err);
      });

    let dcrs = await queryDCRs(config, chain0Name, 1, tradeId);
    assert.equal(dcrs.length, 1)
    let xvps = await queryXVPs(config, chain0Name, 1, tradeId);
    assert.equal(xvps.length, 1)

    logger.log('debug', 'tradeId [' + tradeId + ']: Getting settlement instruction for systemId [' + config[chain1Name].id + ']')
    let systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    while (!systemResponse) {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    while (systemResponse.state !== 'waitingForHold') {
      await sleep(100)
      systemResponse = await crosschainApplicationSDK.getSettlementInstruction(config[chain1Name].id, tradeId, bs64PartyB, bs64PartyA, undefined)
    }
    assert.equal(systemResponse.state, 'waitingForHold')
  });
});

async function placeHoldOnEthereum(config, ethClient, chainName, tradeId, fromAccount, toAccount, amount, notaryId) {
  const operationId = await helpers.getOperationIdFromTradeId(chainName, tradeId, fromAccount, toAccount)
  const createHoldResult = await ethClient.buildAndSendTx(
    AssetTokenJson.abi,
    'createHold',
    {
      operationId: operationId,
      fromAccount: localPartyB,
      toAccount: localPartyA,
      notaryId: notaryId,
      amount: ''+amount,
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

async function cancelHoldOnEthereum(config, sdk, chainName, tradeId, foreignSystemId, foreignSystemLocation) {
  const params = {
    systemId: config[chainName].id,
    state: 'cancel',
    foreignSystemId: foreignSystemId,
    callbackURL: foreignSystemLocation + '/cancel-dcr',
  }
  return await sdk.patchSettlementInstruction(config[chainName].id, tradeId, bs64PartyB, bs64PartyA, params)
}

async function placeHoldOnCorda(config, chainName, tradeId, amount, path) {
  const dcrParams = {
    'value': '' + amount,
    'currency': 'GBP'
  }
  let linearId = '', earmarkId = '';
  await postJson(config[chainName].providers[0] + '/create-dcr', dcrParams, path)
    .then(function (data) {
      logger.log('debug', 'Create dcr transaction: Success');
      logger.log('debug', JSON.stringify(data, null, 2));
      linearId = data.output.linearId.id;
    })
    .catch(function (err) {
      logger.log('debug', 'Create dcr transaction: Error: ', err);
    });

  const xvpParams = {
    'tradeId': tradeId,
    'assetId': linearId,
    'from': x500PartyA,
    'to': x500PartyB,
  }
  await postJson(config[chainName].providers[0] + '/create-xvp', xvpParams, path)
    .then(function (data) {
      logger.log('debug', 'Create xvp transaction: Success');
      logger.log('debug', JSON.stringify(data, null, 2));
    })
    .catch(function (err) {
      logger.log('debug', 'Create xvp transaction: Error: ', err);
    });

  const earParams = {
    'linearId': linearId,
    'partyName': x500PartyB,
    'tradeId': tradeId
  }
  await postJson(config[chainName].providers[0] + '/earmark-dcr', earParams, path)
    .then(function (data) {
      logger.log('debug', 'Earmark dcr transaction: Success');
      logger.log('debug', JSON.stringify(data, null, 2));
      earmarkId = data.id;
    })
    .catch(function (err) {
      logger.log('debug', 'Earmark dcr transaction: Error: ', err);
    });
  return earmarkId
}

async function cancelHoldOnCorda(config, chainName, tradeId, amount, path) {
  const dcrParams = {
    'value': '' + amount,
    'currency': 'GBP'
  }
  let linearId = '';
  await postJson(config[chainName].providers[0] + '/create-dcr', dcrParams, path)
    .then(function (data) {
      logger.log('debug', 'Create dcr transaction: Success');
      logger.log('debug', JSON.stringify(data, null, 2));
      linearId = data.output.linearId.id;
    })
    .catch(function (err) {
      logger.log('debug', 'Create dcr transaction: Error: ', err);
    });

  const xvpParams = {
    'tradeId': '' + tradeId,
    'assetId': '' + linearId,
    'from': 'O=PartyA,L=London,C=GB',
    'to': 'O=PartyB,L=New York,C=US',
  }
  await postJson(config[chainName].providers[0] + '/create-xvp', xvpParams, path)
    .then(function (data) {
      logger.log('debug', 'Create xvp transaction: Success');
      logger.log('debug', JSON.stringify(data, null, 2));
    })
    .catch(function (err) {
      logger.log('debug', 'Create xvp transaction: Error: ', err);
    });

  const canParams = {
    'tradeId': '' + tradeId
  }
  await postJson(config[chainName].providers[1] + '/cancel-xvp', canParams, path)
    .then(function (data) {
      logger.log('debug', 'Cancel xvp transaction: Success');
      logger.log('debug', JSON.stringify(data, null, 2));
    })
    .catch(function (err) {
      logger.log('debug', 'Cancel xvp transaction: Error: ', err);
    });
}

async function queryDCRs(config, chainName, partyIndex, tradeId) {
  const response = await fetch(config[chainName].providers[partyIndex] + '/dcrs?tradeId=' + tradeId, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  })
  return await response.json()
}

async function queryXVPs(config, chainName, partyIndex, tradeId) {
  const response = await fetch(config[chainName].providers[partyIndex] + '/xvps?tradeId=' + tradeId, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  })
  return await response.json()
}

async function postJson(url = '', params = {}, expath) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Export': expath,
    },
    body: JSON.stringify(params)
  });
  return await response.json();
}

async function sleep(ms) { return new Promise((resolve) => { setTimeout(resolve, ms); }); }
