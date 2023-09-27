const utils = require('./../CrosschainSDKUtils');

function init(config, dependencies) {

  const logger = dependencies.logger
  const crosschainXVPContract = dependencies.crosschainXVPContract
  const assetTokenContract = dependencies.assetTokenContract
  const settlementObligationsAdapter = dependencies.settlementObligationsAdapter
  const helpers = dependencies.helpers

  async function createSettlementObligation(systemId, body) {
    const chainName = config.chainIdToChainName[systemId]
    const tradeId = body.tradeId.toString()
    const foreignFromAccount = body.fromAccount
    const foreignToAccount = body.toAccount
    const amount = body.amount
    const timestamp = body.timestamp

    const fromAccountId = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, {foreignAccountId: foreignFromAccount})).localAccountId
    const toAccountId = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, {foreignAccountId: foreignToAccount})).localAccountId
    const localOperationId = await helpers.getOperationIdFromTradeId(chainName, tradeId, fromAccountId, toAccountId)

    await settlementObligationsAdapter.createAndMakeHoldPerpetual(chainName, tradeId, fromAccountId, toAccountId, amount, localOperationId, timestamp)

    let makeHoldPerpetualExecutedEvent = undefined
    while (!makeHoldPerpetualExecutedEvent) {
      makeHoldPerpetualExecutedEvent = await getMakeHoldPerpetualExecutedEventByOperationId(chainName, 0, localOperationId)
      await utils.sleep(3000)
    }
    return {
      operationId: localOperationId
    }
  }

  async function getMakeHoldPerpetualExecutedEventByOperationId(chainName, startingBlock, operationId) {

    const eventLogs = await assetTokenContract.findMakeHoldPerpetualExecutedEvent(chainName, startingBlock)

    for (let eventLog of eventLogs) {
      if (!!eventLog.decodedLog && !!eventLog.decodedLog.operationId && eventLog.decodedLog.operationId.toString() === operationId) {
        return {
          txHash: eventLog.txHash,
          operationId: eventLog.decodedLog.operationId.toString(),
          blockNumber: Number(eventLog.blockNumber),
          chainName
        }
      }
    }
    return undefined
  }

  async function getCounterpartyChainMakeHoldPerpetualExecutedEvent(chainName, tradeId, fromAccount, toAccount) {
    const holdPromiseArr = []
    // Add all other configured chains, except the lead leg's chain
    for (const chain of config.chains) {
      if (chain === chainName) {
        continue
      }
      if (config[chain].type !== 'ethereum') {
        continue
      }

      const foreignSystemId = config[chain].id
      const foreignFromAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(foreignSystemId, {foreignAccountId: fromAccount})).localAccountId
      const foreignToAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(foreignSystemId, {foreignAccountId: toAccount})).localAccountId
      // The from and to accounts are expected to be swapped on the foreign side vs the local side
      const foreignOperationId = await helpers.getOperationIdFromTradeId(chain, tradeId, foreignToAccount, foreignFromAccount)
      logger.log('silly', 'Finding counterparty chain perpetual hold for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + '] and foreignOperationId [' + foreignOperationId + ']')

      holdPromiseArr.push(getMakeHoldPerpetualExecutedEventByOperationId(chain, 0, foreignOperationId))
    }
    return await Promise.any(holdPromiseArr)
  }

  async function getExecuteHoldExecutedEventByOperationId(chainName, startingBlock, operationId) {

    const eventLogs = await assetTokenContract.findExecuteHoldExecutedEvent(chainName, startingBlock)

    for (let eventLog of eventLogs) {
      if (!!eventLog.decodedLog && !!eventLog.decodedLog.operationId && eventLog.decodedLog.operationId.toString() === operationId) {
        return {
          txHash: eventLog.txHash,
          operationId: eventLog.decodedLog.operationId.toString(),
          blockNumber: Number(eventLog.blockNumber),
          chainName
        }
      }
    }
    return undefined
  }

  async function getCancelHoldExecutedEventByOperationId(chainName, startingBlock, operationId) {

    const eventLogs = await assetTokenContract.findCancelHoldExecutedEvent(chainName, startingBlock)

    for (let eventLog of eventLogs) {
      if (!!eventLog.decodedLog && !!eventLog.decodedLog.operationId && eventLog.decodedLog.operationId.toString() === operationId) {
        return {
          txHash: eventLog.txHash,
          operationId: eventLog.decodedLog.operationId.toString(),
          blockNumber: Number(eventLog.blockNumber),
          chainName
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
