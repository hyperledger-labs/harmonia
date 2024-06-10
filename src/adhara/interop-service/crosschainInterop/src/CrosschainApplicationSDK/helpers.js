
function init(config, dependencies) {

  const logger = dependencies.logger
  const web3Store = dependencies.web3Store

  async function getOperationIdFromTradeId(networkName, tradeId, fromAccount, toAccount) {
    const web3 = web3Store[networkName]
    const operationId = web3.utils.soliditySha3({
      type: 'string',
      value: tradeId.toString()
    }, fromAccount, toAccount).substring(2)
    logger.log('silly', `Calculating operationId from tradeId: [${tradeId}], fromAccount: [${fromAccount}], toAccount [${toAccount}] => operationId: [${operationId}]`)
    return operationId
  }

  async function getCurrentBlock(networkName) {
    const latestBlock = await web3Store[networkName].eth.getBlock('latest')
    return latestBlock.number
  }

  return {
    getOperationIdFromTradeId,
    getCurrentBlock
  }
}

module.exports = init

