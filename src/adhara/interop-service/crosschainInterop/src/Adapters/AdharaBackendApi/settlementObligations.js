
function init(config, dependencies){

  const logger = dependencies.logger

  async function createAndMakeHoldPerpetual(chainName, tradeId, fromAccount, toAccount, amount, localOperationId, timestamp) {
    let apiResponse = undefined
    if (!!config[chainName].obligationAPIURL) {
      const url = config[chainName].obligationAPIURL
      const x_userId = config[chainName].obligationAPIUser
      // TODO: switch obligation type based on source system id (as in HQLAX or DTCC)?
      const params = {
        obligationID: localOperationId,
        ledgerContractId: config[chainName].contracts.assetTokenContract.id,
        fromAccount,
        toAccount,
        amount: amount.toString(),
        sourceId: 'integration-obligation-source',
        obligationType: 'HOLD_INTEROP_HQLAX',
        metadata: '{"type":"holdForIntegration","version":"v1","data":{"holdNotaryId":"' + config.tradeDetails.notaryId + '"}}'
      }

      if (Number(timestamp) > 0) {
        params.metaData = '{"type":"holdForIntegration","version":"v1","data":{"holdNotaryId":"' + config.tradeDetails.notaryId + '","initialTimestamp":"' + Number(body.timestamp) + '"}}'
      }

      const headers = {
        'Content-Type': 'application/json',
        'X-USERID': x_userId
      }

      const postBody = JSON.stringify(params)

      logger.log('debug', 'Performing POST for creating obligation:'+ JSON.stringify(params, null, 2))
      logger.log('debug', 'POST Headers: '+ JSON.stringify(headers, null, 2))

      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: postBody
      });
      const apiResponse = await response.json()
      if (!!apiResponse.error) {
        logger.log('error', 'Error from creating obligation via obligation API: '+ apiResponse.error.details.message)
        return {
          tradeId: tradeId,
          error: apiResponse.error,
        }
      }
      logger.log('debug', 'Response from creating obligation via obligation API: '+ JSON.stringify(apiResponse, null, 2))
    } else {
      logger.log('error', 'Obligation API URL undefined')
      return {
        tradeId: tradeId
      }
    }
    return apiResponse
  }

  return {
    createAndMakeHoldPerpetual
  }
}

module.exports = init


