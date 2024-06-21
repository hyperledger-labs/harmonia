
function init(config, dependencies){

  const logger = dependencies.logger
  const assetTokenContract = dependencies.assetTokenContract

  const decimals = 2

  async function createAndMakeHoldPerpetual(networkName, tradeId, fromAccountId, toAccountId, amount, localOperationId) {
    let createHoldResponse = await assetTokenContract.createHold(networkName, localOperationId, fromAccountId, toAccountId, amount * Math.pow(10, decimals))
    logger.log('debug', 'Response from asset token contract create hold: '+ JSON.stringify(createHoldResponse, null, 2))
    let makeHoldPerpetualResponse = await assetTokenContract.makeHoldPerpetual(networkName, localOperationId)
    logger.log('debug', 'Response from asset token contract make hold perpetual: '+ JSON.stringify(makeHoldPerpetualResponse, null, 2))
    return [createHoldResponse, makeHoldPerpetualResponse]
  }

  return {
    createAndMakeHoldPerpetual
  }
}

module.exports = init


