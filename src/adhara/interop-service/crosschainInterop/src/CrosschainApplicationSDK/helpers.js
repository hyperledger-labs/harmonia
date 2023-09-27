
function init(config, dependencies){

  const logger = dependencies.logger
  const web3Store = dependencies.web3Store

  async function getOperationIdFromTradeId(chainName, tradeId, fromAccount, toAccount) {
    const web3 = web3Store[chainName]
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId.toString() }, fromAccount, toAccount).substring(2)
    logger.log('silly', `Calculating operationId from tradeId: [${tradeId}], fromAccount: [${fromAccount}], toAccount [${toAccount}] => operationId: [${operationId}]`)
    return operationId
  }

  return {
    getOperationIdFromTradeId
  }
}

module.exports = init
