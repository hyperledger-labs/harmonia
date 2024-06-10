const utils = require('./../CrosschainSDKUtils');

function init(config, dependencies) {

  const logger = dependencies.logger
  const crosschainXVPContract = dependencies.crosschainXVPContract
  const assetTokenContract = dependencies.assetTokenContract
  const settlementObligationsAdapter = dependencies.settlementObligationsAdapter
  const helpers = dependencies.helpers

  async function createSettlementObligation(networkId, body) {
    const networkName = config.networkIdToNetworkName[networkId]
    const tradeId = body.tradeId.toString()
    const remoteFromAccount = body.fromAccount
    const remoteToAccount = body.toAccount
    const amount = body.amount
    const timestamp = body.timestamp

    const fromAccountId = (await crosschainXVPContract.getRemoteAccountIdToLocalAccountId(networkId, {remoteAccountId: remoteFromAccount})).localAccountId
    const toAccountId = (await crosschainXVPContract.getRemoteAccountIdToLocalAccountId(networkId, {remoteAccountId: remoteToAccount})).localAccountId
    const localOperationId = await helpers.getOperationIdFromTradeId(networkName, tradeId, fromAccountId, toAccountId)

    await settlementObligationsAdapter.createAndMakeHoldPerpetual(networkName, tradeId, fromAccountId, toAccountId, amount, localOperationId, timestamp)

    let makeHoldPerpetualExecutedEvent = undefined
    while (!makeHoldPerpetualExecutedEvent) {
      makeHoldPerpetualExecutedEvent = await getMakeHoldPerpetualExecutedEventByOperationId(networkName, 0, localOperationId)
      await utils.sleep(3000)
    }
    return {
      operationId: localOperationId
    }
  }

  async function getMakeHoldPerpetualExecutedEventByOperationId(networkName, startingBlock, operationId) {

    const eventLogs = await assetTokenContract.findMakeHoldPerpetualExecutedEvent(networkName, startingBlock)

    for (let eventLog of eventLogs) {
      if (!!eventLog.decodedLog && !!eventLog.decodedLog.operationId && eventLog.decodedLog.operationId.toString() === operationId) {
        return {
          txHash: eventLog.txHash,
          operationId: eventLog.decodedLog.operationId.toString(),
          blockNumber: Number(eventLog.blockNumber),
          networkName
        }
      }
    }
    return undefined
  }

  async function getCounterpartyChainMakeHoldPerpetualExecutedEvent(networkName, tradeId, fromAccount, toAccount) {
    const holdPromiseArr = []
    // Add all other configured chains, except the lead leg's chain
    for (const chain of config.chains) {
      if (chain === networkName) {
        continue
      }
      if (config[chain].type !== 'ethereum') {
        continue
      }

      const remoteNetworkId = config[chain].id
      const remoteFromAccount = (await crosschainXVPContract.getRemoteAccountIdToLocalAccountId(remoteNetworkId, {remoteAccountId: fromAccount})).localAccountId
      const remoteToAccount = (await crosschainXVPContract.getRemoteAccountIdToLocalAccountId(remoteNetworkId, {remoteAccountId: toAccount})).localAccountId
      // The from and to accounts are expected to be swapped on the remote side vs the local side
      const remoteOperationId = await helpers.getOperationIdFromTradeId(chain, tradeId, remoteToAccount, remoteFromAccount)
      logger.log('silly', 'Finding counterparty chain perpetual hold for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + '] and remoteOperationId [' + remoteOperationId + ']')

      holdPromiseArr.push(getMakeHoldPerpetualExecutedEventByOperationId(chain, 0, remoteOperationId))
    }
    return await Promise.any(holdPromiseArr)
  }

  async function getExecuteHoldExecutedEventByOperationId(networkName, startingBlock, operationId) {

    const eventLogs = await assetTokenContract.findExecuteHoldExecutedEvent(networkName, startingBlock)

    for (let eventLog of eventLogs) {
      if (!!eventLog.decodedLog && !!eventLog.decodedLog.operationId && eventLog.decodedLog.operationId.toString() === operationId) {
        return {
          txHash: eventLog.txHash,
          operationId: eventLog.decodedLog.operationId.toString(),
          blockNumber: Number(eventLog.blockNumber),
          networkName
        }
      }
    }
    return undefined
  }

  async function getCancelHoldExecutedEventByOperationId(networkName, startingBlock, operationId) {

    const eventLogs = await assetTokenContract.findCancelHoldExecutedEvent(networkName, startingBlock)

    for (let eventLog of eventLogs) {
      if (!!eventLog.decodedLog && !!eventLog.decodedLog.operationId && eventLog.decodedLog.operationId.toString() === operationId) {
        return {
          txHash: eventLog.txHash,
          operationId: eventLog.decodedLog.operationId.toString(),
          blockNumber: Number(eventLog.blockNumber),
          networkName
        }
      }
    }
    return undefined
  }

  return {
    createSettlementObligation,
    getMakeHoldPerpetualExecutedEventByOperationId,
    getCounterpartyChainMakeHoldPerpetualExecutedEvent,
    getExecuteHoldExecutedEventByOperationId,
    getCancelHoldExecutedEventByOperationId
  }

}

module.exports = init
