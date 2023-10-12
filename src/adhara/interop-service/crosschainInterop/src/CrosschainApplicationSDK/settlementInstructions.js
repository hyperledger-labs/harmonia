const fetch = require('node-fetch')
const https = require('https')
const abiDecoder = require('abi-decoder');
const utils = require('./../CrosschainSDKUtils');
const settlementInstructionStates = require('./settlementInstructionStates.js')

function init(config, dependencies) {

  const logger = dependencies.logger
  const web3Store = dependencies.web3Store
  const helpers = dependencies.helpers
  const crosschainXVPContract = dependencies.crosschainXVPContract
  const crosschainFunctionCallContract = dependencies.crosschainFunctionCallContract
  const settlementObligations = dependencies.settlementObligations

  if (dependencies.CrosschainXvPJson) abiDecoder.addABI(dependencies.CrosschainXvPJson.abi)
  if (dependencies.CrosschainFunctionCallJson) abiDecoder.addABI(dependencies.CrosschainFunctionCallJson.abi)
  if (dependencies.AssetTokenJson) abiDecoder.addABI(dependencies.AssetTokenJson.abi)

  async function getSettlementInstruction(systemId, tradeId, foreignFromAccount, foreignToAccount, operationId) {
    let settlementInstructionObj
    if (!!systemId && !!operationId) {
      settlementInstructionObj = await dependencies.db.findSettlementInstructionByOperationId(systemId, operationId)
    } else if (!!systemId && !!tradeId && !!foreignFromAccount && !!foreignToAccount) {
      const chainName = config.chainIdToChainName[systemId]
      const fromAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, { foreignAccountId: foreignFromAccount })).localAccountId
      const toAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, { foreignAccountId: foreignToAccount })).localAccountId
      const localOperationId = await helpers.getOperationIdFromTradeId(chainName, tradeId, fromAccount, toAccount)
      settlementInstructionObj = await dependencies.db.findSettlementInstructionByOperationId(systemId, localOperationId)
    }

    // TODO: finalise what the return value should be if not found
    if (!settlementInstructionObj) {
      return undefined
    }

    // Sanitise before sending back
    const response = {
      systemId: settlementInstructionObj.systemId,
      foreignSystemId: settlementInstructionObj.foreignSystemId,
      tradeId: settlementInstructionObj.tradeId,
      operationId: settlementInstructionObj.operationId,
      fromAccount: settlementInstructionObj.fromAccount,
      toAccount: settlementInstructionObj.toAccount,
      currency: settlementInstructionObj.currency,
      amount: settlementInstructionObj.amount,
      callbackURL: settlementInstructionObj.callbackURL,
      triggerLeadLeg: settlementInstructionObj.triggerLeadLeg,
      useExistingEarmark: settlementInstructionObj.useExistingEarmark,
      useForCancellation: settlementInstructionObj.useForCancellation,
      signatureOrProof: settlementInstructionObj.signatureOrProof,
      state: settlementInstructionObj.state,
      // TODO: this needs to be sanitised further, should an error even be included?
      //error: settlementInstructionObj.error,
      creationDate: settlementInstructionObj.creationDate,
      lastUpdate: settlementInstructionObj.lastUpdate,
      humanReadableTimestamp: new Date(settlementInstructionObj.creationDate)
    }

    return response
  }

  async function submitSettlementInstruction(systemId, settlementInstruction) {
    // TODO: add validation on required parameters
    const chainName = config.chainIdToChainName[systemId]
    if (!config[chainName]) {
      return Promise.reject(Error('No configuration found for system with id [' + systemId + '], unable to submit settlement instruction'))
    }
    const tradeId = settlementInstruction.tradeId.toString()
    const metaData = '' // Should we add a meta data field to the API for settlement instructions
    const foreignFromAccount = settlementInstruction.fromAccount
    const foreignToAccount = settlementInstruction.toAccount
    const currency = settlementInstruction.currency
    const amount = settlementInstruction.amount
    const triggerLeadLeg = settlementInstruction.triggerLeadLeg
    const useExistingEarmark = settlementInstruction.useExistingEarmark
    const useForCancellation = settlementInstruction.useForCancellation === true
    const notaryId = config.tradeDetails.notaryId
    const state = settlementInstructionStates.confirmed
    const signatureOrProof = settlementInstruction.signatureOrProof
    const creationDate = Date.now() // timestamp in server time
    const lastUpdate = Date.now()
    const fromAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, { foreignAccountId: foreignFromAccount })).localAccountId
    const toAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, { foreignAccountId: foreignToAccount })).localAccountId
    const localOperationId = await helpers.getOperationIdFromTradeId(chainName, tradeId, fromAccount, toAccount)

    // TODO: add settlement instruction request to DB!
    logger.log('info', 'Received new settlement instruction from systemId [' + systemId + '] with tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + '], operationId [' + localOperationId + ']')

    // Do checks on foreign system id
    let foreignSystemId = settlementInstruction.foreignSystemId
    if (!!signatureOrProof && foreignSystemId !== signatureOrProof.sourceSystemId) {
      logger.log('warn', 'Received new settlement instruction with foreignSystemId [' + foreignSystemId + '] and proof originating from sourceSystemId [' + signatureOrProof.sourceSystemId + ']')
      foreignSystemId = signatureOrProof.sourceSystemId
    }
    let foreignChainName = config.chainIdToChainName[foreignSystemId]
    if (!config[foreignChainName]) {
      return Promise.reject(Error('No configuration found for foreign system with id [' + foreignSystemId + '], unable to submit settlement instruction'))
    }

    // Since the proxy is going to inject the mTLS cert, we need to do a call out using only http
    let callbackURL = settlementInstruction.callbackURL
    if (!!callbackURL && callbackURL.startsWith('https') && config.performCallbackHttpsRewrite === true) {
      callbackURL = callbackURL.replace('https', 'http')
    }

    let settlementInstructionObj = {
      chainName,
      systemId,
      foreignSystemId,
      tradeId,
      localOperationId,
      fromAccount,
      toAccount,
      amount,
      triggerLeadLeg,
      useExistingEarmark,
      useForCancellation,
      notaryId,
      callbackURL,
      signatureOrProof,
      state,
      creationDate,
      lastUpdate
    }

    await dependencies.db.addSettlementInstructionToStore(settlementInstructionObj)

    if (!callbackURL) {
      logger.log('debug', 'Settlement instruction from systemId [' + systemId + '] with tradeId [' + settlementInstructionObj.tradeId.toString() + '] does not contain a callback url')
      while ((settlementInstructionObj.state !== settlementInstructionStates.processed && settlementInstructionObj.state !== settlementInstructionStates.cancelled) && settlementInstructionObj.state !== settlementInstructionStates.failed) {
        settlementInstructionObj = await dependencies.db.findSettlementInstruction(
          settlementInstructionObj.systemId,
          settlementInstructionObj.tradeId,
          settlementInstructionObj.fromAccount,
          settlementInstructionObj.toAccount
        )
        await utils.sleep(2000)
      }
      if (!settlementInstructionObj || !settlementInstructionObj.settlementInstructionResult) {
        if (!settlementInstructionObj) {
          return Promise.reject(Error('Settlement instruction was deleted or does not exist'))
        } else if (!!settlementInstructionObj.error) {
          return Promise.reject(settlementInstructionObj.error)
        }
        return Promise.reject(Error('Settlement instruction with state [' + settlementInstructionObj.state + '] does not contain a response'))
      }
      logger.log('debug', 'Handling settlement instruction response from systemId [' + systemId + '] for tradeId: [' + settlementInstructionObj.tradeId.toString() + ']')
      return await adaptResponseForSource(settlementInstructionObj)
    } else {
      return {
        "operationId": localOperationId,
        "tradeId": tradeId,
        "fromAccount": fromAccount,
        "toAccount": toAccount,
        "amount": amount,
        "state": state
      }
    }
  }

  async function deleteSettlementInstruction(systemId, tradeId, foreignFromAccount, foreignToAccount, operationId) {
    let settlementInstructionObj
    if (!!systemId && !!operationId) {
      settlementInstructionObj = await dependencies.db.findSettlementInstructionByOperationId(systemId, operationId)
    } else if (!!systemId && !!tradeId && !!foreignFromAccount && !!foreignToAccount) {
      const chainName = config.chainIdToChainName[systemId]
      const fromAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, { foreignAccountId: foreignFromAccount })).localAccountId
      const toAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, { foreignAccountId: foreignToAccount })).localAccountId
      const localOperationId = await helpers.getOperationIdFromTradeId(chainName, tradeId, fromAccount, toAccount)
      settlementInstructionObj = await dependencies.db.findSettlementInstructionByOperationId(systemId, localOperationId)
    }

    // TODO: finalise what the return value should be if not found
    if (!settlementInstructionObj) {
      return undefined
    }

    if (!!settlementInstructionObj.state) {
      if (settlementInstructionObj.state === settlementInstructionStates.confirmed
        || settlementInstructionObj.state === settlementInstructionStates.waitingForHold
        || settlementInstructionObj.state === settlementInstructionStates.failed) {
        await dependencies.db.removeSettlementInstructionFromStore(tradeId, foreignFromAccount, foreignToAccount)
      } else {
        return Promise.reject(Error('Settlement instruction for tradeId: [' + settlementInstructionObj.tradeId.toString() + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + '] with state [' + settlementInstructionObj.state + '] cannot be deleted'))
      }
    }

    // Sanitise before sending back
    const response = {
      systemId: settlementInstructionObj.systemId,
      foreignSystemId: settlementInstructionObj.foreignSystemId,
      tradeId: settlementInstructionObj.tradeId,
      operationId: settlementInstructionObj.operationId,
      fromAccount: settlementInstructionObj.fromAccount,
      toAccount: settlementInstructionObj.toAccount,
      currency: settlementInstructionObj.currency,
      amount: settlementInstructionObj.amount,
      callbackURL: settlementInstructionObj.callbackURL,
      triggerLeadLeg: settlementInstructionObj.triggerLeadLeg,
      useExistingEarmark: settlementInstructionObj.useExistingEarmark,
      useForCancellation: settlementInstructionObj.useForCancellation,
      signatureOrProof: settlementInstructionObj.signatureOrProof,
      state: settlementInstructionObj.state,
      // TODO: this needs to be sanitised further, should an error even be included?
      //error: settlementInstructionObj.error,
      creationDate: settlementInstructionObj.creationDate,
      lastUpdate: settlementInstructionObj.lastUpdate,
      humanReadableTimestamp: new Date(settlementInstructionObj.creationDate)
    }

    return response
  }

  function isTransitionToCancelAllowed(settlementInstructionObj) {
    if (settlementInstructionObj.state === settlementInstructionStates.confirmed) {
      return true
    }
    if (settlementInstructionObj.state === settlementInstructionStates.waitingForHold) {
      return true
    }
    if (settlementInstructionObj.state === settlementInstructionStates.waitingForCrossBlockchainCallExecuted) {
      return true
    }
    if (settlementInstructionObj.state === settlementInstructionStates.failed) {
      return true
    }
    return false
  }

  function isTransitionToCancellingAllowed(settlementInstructionObj) {
    if (settlementInstructionObj.state === settlementInstructionStates.waitingForForeignSystemCancellation) {
      return true
    }
    if (settlementInstructionObj.state === settlementInstructionStates.waitingForHold) {
      return true
    }
    return false
  }

  function isTransitionToWaitingForHoldAllowed(settlementInstructionObj) {
    if (settlementInstructionObj.state === settlementInstructionStates.timedOut) {
      return true
    }
    if (settlementInstructionObj.state === settlementInstructionStates.failed) {
      return true
    }
    return false
  }

  async function handleSettlementInstructionUpdate(settlementInstructionRequest) {
    const settlementInstructionObj = await dependencies.db.findSettlementInstructionByOperationId(settlementInstructionRequest.systemId, settlementInstructionRequest.localOperationId)
    if (!settlementInstructionObj) {
      return Promise.reject(Error('Settlement instruction (systemId [' + settlementInstructionRequest.systemId + '], operationId [' + settlementInstructionRequest.localOperationId + ']) not found'))
    }
    if (!settlementInstructionObj.state) {
      return Promise.reject(Error('No state found on settlement instruction for systemId [' + settlementInstructionObj.systemId + '], tradeId [' + settlementInstructionObj.tradeId + '], fromAccount [' + settlementInstructionObj.fromAccount + '], toAccount [' + settlementInstructionObj.toAccount + ']'))
    }
    if (settlementInstructionObj.state !== settlementInstructionRequest.state) {
      logger.log('debug', 'Updating state [' + settlementInstructionObj.state + '] to [' + settlementInstructionRequest.state + ']');
      try {
        await isStateUpdateAllowed(settlementInstructionObj, settlementInstructionRequest)
        settlementInstructionObj.state = settlementInstructionRequest.state
      } catch (error) {
        return Promise.reject(error)
      }
    }
    if (settlementInstructionObj.callbackURL !== settlementInstructionRequest.callbackURL) {
      logger.log('debug', 'Updating callbackURL [' + settlementInstructionObj.callbackURL + '] to [' + settlementInstructionRequest.callbackURL + ']');
      settlementInstructionObj.callbackURL = settlementInstructionRequest.callbackURL
    }
    settlementInstructionObj.lastUpdate = Date.now()
    await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
    await dependencies.db.findSettlementInstruction(settlementInstructionRequest.systemId, settlementInstructionObj.tradeId, settlementInstructionObj.fromAccount, settlementInstructionObj.toAccount)
  }

  async function isStateUpdateAllowed(settlementInstructionObj, settlementInstructionRequest) {
    if (settlementInstructionRequest.state === settlementInstructionStates.failed) {
      return Promise.resolve()
    } else if (settlementInstructionRequest.state === settlementInstructionStates.waitingForHold) {
      if (isTransitionToWaitingForHoldAllowed(settlementInstructionObj)) {
        return Promise.resolve()
      } else {
        return Promise.reject(Error('Settlement instruction for trade id [' + settlementInstructionObj.tradeId + '] with state [' + settlementInstructionObj.state + '] cannot be updated to state [waitingForHold]. It has to either be in state [timedOut] or [failed].'))
      }
    } else if (settlementInstructionRequest.state === settlementInstructionStates.cancel) {
      if (settlementInstructionObj.foreignSystemId !== undefined) {
        if (isTransitionToCancelAllowed(settlementInstructionObj)) {
          return Promise.resolve()
        } else {
          return Promise.reject(Error('Settlement instruction for trade id [' + settlementInstructionObj.tradeId + '] with state [' + settlementInstructionObj.state + '] is not cancellable'))
        }
      } else {
        return Promise.reject(Error('Settlement instruction for trade id [' + settlementInstructionObj.tradeId + '] with state [' + settlementInstructionObj.state + '] on chain [' + settlementInstructionObj.chainName + '] can only be cancelled from a foreign ledger'))
      }
    } else if (settlementInstructionRequest.state === settlementInstructionStates.cancelling) {
      if (settlementInstructionObj.foreignSystemId !== undefined) {
        if (isTransitionToCancellingAllowed(settlementInstructionObj)) {
          return Promise.resolve()
        } else {
          return Promise.reject(Error('Settlement instruction for trade id [' + settlementInstructionObj.tradeId + '] with state [' + settlementInstructionObj.state + '] is not transitionable to state [cancelling]'))
        }
      } else {
        return Promise.reject(Error('Settlement instruction for trade id [' + settlementInstructionObj.tradeId + '] with state [' + settlementInstructionObj.state + '] on chain [' + settlementInstructionObj.chainName + '] can only be cancelled from a foreign ledger'))
      }
    } else {
      return Promise.reject(Error('Settlement instructions cannot be updated to [' + settlementInstructionRequest.state + ']'))
    }
  }

  async function patchSettlementInstruction(systemId, tradeId, foreignFromAccount, foreignToAccount, updateSettlementInstruction) {
    logger.log('debug', 'Received new request to patch settlement instruction from systemId [' + systemId + '] for tradeId [' + tradeId + '], fromAccount [' + foreignFromAccount + '], toAccount [' + foreignToAccount + ']')
    let settlementInstructionObj
    if (!!systemId && !!tradeId) {
      const chainName = config.chainIdToChainName[systemId]
      if (config[chainName].type === 'ethereum') {
        if (!foreignFromAccount || !foreignToAccount) {
          logger.log('debug', 'Request to patch settlement instruction from systemId [' + systemId + '] for tradeId [' + tradeId + '] does not contain a fromAccount or toAccount: The last instruction will be patched')
          settlementInstructionObj = await dependencies.db.findSettlementInstructionByTradeId(systemId, tradeId)
        } else {
          const fromAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, { foreignAccountId: foreignFromAccount })).localAccountId
          const toAccount = (await crosschainXVPContract.getForeignAccountIdToLocalAccountId(systemId, { foreignAccountId: foreignToAccount })).localAccountId
          const localOperationId = await helpers.getOperationIdFromTradeId(chainName, tradeId, fromAccount, toAccount)
          settlementInstructionObj = await dependencies.db.findSettlementInstructionByOperationId(systemId, localOperationId)
        }
      }
    } else {
      return Promise.reject(Error('Please specify a systemId, tradeId, fromAccount and toAccount when updating a settlement instruction\'s state'))
    }
    if (!settlementInstructionObj) {
      return Promise.reject(Error('Settlement instruction for systemId [' + systemId + '], tradeId [' + tradeId + '], fromAccount [' + foreignFromAccount + '], toAccount [' + foreignToAccount + '] not found'));
    }
    logger.log('debug', 'Request to patch settlement instruction with from systemId [' + systemId + '] for tradeId [' + tradeId + '] with new state [' + updateSettlementInstruction.state + ']')
    // Create a deep copy of the settlementInstructionObj to use as the settlementInstructionRequest
    const settlementInstructionRequest = JSON.parse(JSON.stringify(settlementInstructionObj))
    settlementInstructionRequest.systemId = systemId
    if (!!updateSettlementInstruction.state) {
      settlementInstructionRequest.state = updateSettlementInstruction.state
    }
    if (!!updateSettlementInstruction.callbackURL) {
      settlementInstructionRequest.callbackURL = updateSettlementInstruction.callbackURL
    }
    await dependencies.db.addSettlementInstructionRequestToStore(settlementInstructionRequest)
    const response = {
      success: true
    }
    return response
  }

  async function adaptResponseForSource(settlementInstructionObj) {
    if (!!settlementInstructionObj.signatureOrProof && config.chainIdToChainName[settlementInstructionObj.signatureOrProof.sourceSystemId].endsWith('dtcc')) {
      return {
        partyId: settlementInstructionObj.toAccount,
        dtccTransactionIdHash: settlementInstructionObj.tradeId,
        quantity: settlementInstructionObj.amount // TODO: The amount should be retrieved from the hold, especially once we have partial holds
      }
    } else if ((!settlementInstructionObj.signatureOrProof && !settlementInstructionObj.encodedInfo) || (settlementInstructionObj.useForCancellation)) {
      return settlementInstructionObj.settlementInstructionResult
    } else {
      return {
        tradeId: settlementInstructionObj.settlementInstructionResult.tradeId.toString(),
        systemId: settlementInstructionObj.settlementInstructionResult.systemId,
        sourceSystemId: settlementInstructionObj.settlementInstructionResult.sourceSystemId,
        encodedInfo: settlementInstructionObj.settlementInstructionResult.encodedInfo,
        signatureOrProof: settlementInstructionObj.settlementInstructionResult.signatureOrProof
      }
    }
  }

  async function handleSettlementInstructionCallback(callbackURL, settlementInstructionObj) {
    const params = await adaptResponseForSource(settlementInstructionObj)
    const headers = {
      'Content-Type': 'application/json'
    }
    if (!!config.callbackAuthorizationToken) {
      headers['Authorization'] = config.callbackAuthorizationToken
    }

    const options = {
      method: 'POST',
      headers,
      body: JSON.stringify(params)
    }
    if (callbackURL.startsWith('https') && config.rejectUnauthorizedSSL === false) {
      options.agent = new https.Agent({
        rejectUnauthorized: false
      })
    }
    const callbackResponse = await fetch(callbackURL, options)
    try {
      const contentType = callbackResponse.headers.get('content-type');
    } catch (err) {
      logger.log('error', 'Unable to get content type from callback response for settlement instruction with tradeId: [' + settlementInstructionObj.tradeId.toString() + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + ']: Error: ' + err)
    }
    const callbackResponseText = await callbackResponse.text()
    logger.log('debug', 'Handled settlement instruction callback url for tradeId: [' + settlementInstructionObj.tradeId.toString() + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + ']: Response:\n' + callbackResponseText)
  }

  async function transitionToWaitingForHold(settlementInstructionObj) {
    try {
      const useExistingEarmark = settlementInstructionObj.useExistingEarmark

      const tradeId = settlementInstructionObj.tradeId
      const fromAccount = settlementInstructionObj.fromAccount
      const toAccount = settlementInstructionObj.toAccount

      if (!useExistingEarmark) {
        logger.log('debug', 'Creating settlement obligation for tradeId: [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + ']')
        const systemId = settlementInstructionObj.systemId
        const amount = settlementInstructionObj.amount
        let settlementObligation = await dependencies.settlementObligations.createSettlementObligation(systemId, {
          tradeId,
          fromAccount,
          toAccount,
          amount
        })
        if (!!settlementObligation.error) {
          return Promise.reject(Error(settlementObligation.error.details.message))
        }
      } else {
        logger.log('debug', 'Will look for existing earmark to use for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + ']')
      }

      const settlementInstructionObjStateCheck = await dependencies.db.findSettlementInstructionByOperationId(settlementInstructionObj.systemId, settlementInstructionObj.localOperationId)
      if (settlementInstructionObjStateCheck.state !== settlementInstructionStates.confirmed) {
        logger.log('error', 'Settlement instruction no longer in [confirmed] state, cannot transition to [waitingForHold] for tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + ']')
        return
      }
      logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForHold]');
      settlementInstructionObj.state = settlementInstructionStates.waitingForHold
      settlementInstructionObj.lastUpdate = Date.now()
      await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
    } catch (error) {
      return Promise.reject(error)
    }
  }

  async function handleSettlementInstructionTimeout(settlementInstructionObj) {
    logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [timedOut]');
    settlementInstructionObj.state = settlementInstructionStates.timedOut
    settlementInstructionObj.lastUpdate = Date.now()
    await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)

    const callbackURL = settlementInstructionObj.callbackURL

    if (!!callbackURL) {
      try {
        const params = {
          error: "Settlement Instruction on systemId " + settlementInstructionObj.systemId
            + " with tradeId: " + settlementInstructionObj.tradeId
            + " and operationId: " + settlementInstructionObj.localOperationId
            + " has timed out while waiting for earmark to be created"
        }
        const callbackResponse = await fetch(callbackURL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(params)
        })
        const callbackResponseJson = await callbackResponse.json()
        logger.log('debug', 'Handled timeout settlement instruction callback url for tradeId [' + settlementInstructionObj.tradeId.toString() + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + ']: Response:\n' + JSON.stringify(callbackResponseJson))
      } catch (error) {
        logger.log('error', Error(error))
      }
    } else {
      logger.log('info', 'No callback url provided for tradeId [' + settlementInstructionObj.tradeId + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + ']')
    }
  }

  async function transitionToCancelledState(settlementInstructionObj) {
    try {
      // CrossBlockchain calls for cancellation will come from the foreign system/chain
      const tradeId = settlementInstructionObj.tradeId.toString()
      const callbackURL = settlementInstructionObj.callbackURL
      const chainName = config.chainIdToChainName[settlementInstructionObj.systemId]
      const foreignChainName = config.chainIdToChainName[settlementInstructionObj.foreignSystemId]

      if (config[foreignChainName].type === 'ethereum') {
        const web3Foreign = web3Store[foreignChainName]
        const startBlock = 0 // If set to zero, the search will start X blocks back from the latest block

        const eventLog = await getCrossBlockchainCallExecutedEventByTradeDetails(foreignChainName, startBlock, tradeId)
        if(!eventLog){
          return
        }
        logger.log('debug', 'Found CrossBlockchainCallExecuted for function: ['+eventLog.functionName+'] and function call data:['+JSON.stringify(eventLog.decodedFunctionCallData.params, null, 2)+']')

        const block = eventLog.block
        const txHash = eventLog.txHash
        const destinationBlockchainId = eventLog.destinationBlockchainId
        if (!destinationBlockchainId) {
          return Promise.reject(Error('Destination chain not found while processing trade id [' + tradeId + ']: Unable to transition to a cancelled state'))
        }
        const counterpartyChainName = config.chainIdToChainName[destinationBlockchainId]
        if (!config[counterpartyChainName]) {
          return Promise.reject(Error('No configuration found for chain [' + destinationBlockchainId + ']: Unable to transition to a cancelled state'))
        }
        // Check that authentication parameters for the source chain function call contract is set in the destination eth chain
        if (config[counterpartyChainName].type === 'ethereum') {
          let systemId = settlementInstructionObj.systemId
          let counterpartySystemId = settlementInstructionObj.foreignSystemId
          let crosschainXvPAddress = await crosschainXVPContract.getContractAddress(systemId)
          logger.log('debug', 'Checking that authentication parameters exist in counterparty system [' + counterpartySystemId + '] mapping systemId [' + systemId + '] to XvP contractAddress [' + crosschainXvPAddress + ']');
          let counterpartyChainAuthParams = (await crosschainFunctionCallContract.isAuthParams(counterpartySystemId, systemId, crosschainXvPAddress)).isAuthParams
          if (!counterpartyChainAuthParams) {
            const authParamErr = new Error('Unable to process settlement instruction object for tradeId [' + settlementInstructionObj.tradeId.toString() + '] due to authentication parameters missing from counterparty system [' + counterpartySystemId + '] mapping systemId [' + systemId + '] to XvP contractAddress [' + crosschainXvPAddress + ']');
            logger.log('error', authParamErr)
            throw authParamErr;
          } else {
            logger.log('debug', 'Authentication parameters exist in counterparty system [' + counterpartySystemId + '] mapping systemId [' + systemId + '] to XvP contractAddress [' + crosschainXvPAddress + ']');
          }
        }
        logger.log('debug', 'Creating proof of startCancellation from system [' + foreignChainName + '] in order to performCancellation on [' + counterpartyChainName + ']')
        const context = {
          tradeId,
          functionName: eventLog.functionName
        }
        const proofObj = await dependencies.crosschainFunctionCallSDK.handleCrossBlockchainCallExecutedEvent(web3Foreign, block, txHash, foreignChainName, counterpartyChainName, context)
        settlementInstructionObj.settlementInstructionResult = {
          tradeId,
          systemId: config[counterpartyChainName].id,
          sourceSystemId: config[foreignChainName].id,
          encodedInfo: proofObj.encodedInfo,
          signatureOrProof: proofObj.signatureOrProof
        }
        if (!!callbackURL) {
          logger.log('debug', 'Handling settlement instruction callback url for tradeId: [' + settlementInstructionObj.tradeId.toString() + ']')
          try {
            await handleSettlementInstructionCallback(callbackURL, settlementInstructionObj)
          } catch (error) {
            logger.log('error', Error(error))
          }
        } else {
          logger.log('debug', 'No callback url provided for settlement instruction with tradeId: [' + settlementInstructionObj.tradeId.toString() + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + ']')
        }
        if (eventLog.functionName === 'performCancellation') {
          logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [cancelled] after handling event [CrossBlockchainCallExecuted]');
          settlementInstructionObj.state = settlementInstructionStates.cancelled
        } else {
          return Promise.reject(Error('Unsupported function name received: ' + eventLog.functionName))
        }
        settlementInstructionObj.lastUpdate = Date.now()
        await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
      } else if (config[foreignChainName].type === 'corda') {
        if (config[chainName].type === 'ethereum') {
          const web3 = web3Store[chainName]
          const holdCancelled = await settlementObligations.getCancelHoldExecutedEventByOperationId(chainName, 0, settlementInstructionObj.localOperationId)
          if (holdCancelled === undefined) {
            // If more than 5 minutes have passed, call callback with timeout error message
            const timeElapsed = Date.now() - settlementInstructionObj.timestamp
            if (timeElapsed >= 5 * 60 * 1000) {
              logger.log('debug', 'Finding holds for settlement instruction with tradeId [' + tradeId + '] has timed out: calling handleSettlementInstructionTimeout')
              return await handleSettlementInstructionTimeout(settlementInstructionObj)
            } else {
              return
            }
          }
          if (!!holdCancelled) {
            settlementInstructionObj.settlementInstructionResult = {
              tradeId: tradeId,
              sourceSystemId: config[chainName].id,
            }
            if (!!callbackURL) {
              logger.log('debug', 'Handling settlement instruction callback url for tradeId: [' + settlementInstructionObj.tradeId.toString() + ']')
              try {
                await handleSettlementInstructionCallback(callbackURL, settlementInstructionObj)
              } catch (error) {
                logger.log('error', Error(error))
              }
            } else {
              logger.log('debug', 'No callback url provided for settlement instruction with tradeId: [' + settlementInstructionObj.tradeId.toString() + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + ']')
            }
            logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [cancelled] after handling event [CancelHoldExecuted]');
            settlementInstructionObj.state = settlementInstructionStates.cancelled
            settlementInstructionObj.lastUpdate = Date.now()
            await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
          }
        }
      }
    } catch (error) {
      return Promise.reject(error)
    }
  }

  async function getCrossBlockchainCallExecutedEventByTradeDetails(chainName, startBlock, tradeId, sender, receiver, destinationBlockchainId){

    const eventLogs = await crosschainFunctionCallContract.findCrossBlockchainCallExecutedEvent(chainName, startBlock)

    for(let eventLog of eventLogs){
      const decodedFunctionCallData = abiDecoder.decodeMethod(eventLog.decodedLog.functionCallData)
      if(!decodedFunctionCallData || !decodedFunctionCallData.params){
        continue
      }
      const eventTradeId = decodedFunctionCallData.params.find((item) => item.name === 'tradeId')
      const eventSender = decodedFunctionCallData.params.find((item) => item.name === 'sender')
      const eventReceiver = decodedFunctionCallData.params.find((item) => item.name === 'receiver')
      if((!!eventTradeId && eventTradeId.value.toString() === tradeId.toString())
        && ((!sender) || (!!sender && !!eventSender && eventSender.value.toString() === sender.toString())) // if sender is passed in and eventSender is found, do they match?
        && ((!receiver) || (!!receiver && !!eventReceiver && eventReceiver.value.toString() === receiver.toString())) // if receiver is passed in and eventReceiver is found, do they match?
        && ((!destinationBlockchainId) || (!!destinationBlockchainId && !!eventLog.decodedLog && eventLog.decodedLog.destinationBlockchainId.toString() === destinationBlockchainId.toString()))
      ){
        const block = await web3Store[chainName].eth.getBlock(eventLog.blockNumber)
        return {
          block,
          decodedFunctionCallData,
          txHash: eventLog.txHash,
          functionName: decodedFunctionCallData.name,
          destinationBlockchainId: eventLog.decodedLog.destinationBlockchainId,
        }
      }
    }
    return undefined
  }

  async function transitionToEndState(settlementInstructionObj) {
    try {
      const chainName = settlementInstructionObj.chainName
      const tradeId = settlementInstructionObj.tradeId.toString()
      const callbackURL = settlementInstructionObj.callbackURL
      if (config[chainName].type === 'ethereum' && settlementInstructionObj.state === settlementInstructionStates.waitingForCrossBlockchainCallExecuted) {
        const startBlock = 0 // If set to zero, the search will start X blocks back from the latest block
        const web3 = web3Store[chainName]

        const eventLog = await getCrossBlockchainCallExecutedEventByTradeDetails(
          chainName,
          startBlock,
          tradeId,
          settlementInstructionObj.toAccount,
          settlementInstructionObj.fromAccount,
          settlementInstructionObj.foreignSystemId
        )
        if (!eventLog) {
          return
        }

        logger.log('debug', 'Found CrossBlockchainCallExecuted for function: ['+eventLog.functionName+'] and function call data:['+JSON.stringify(eventLog.decodedFunctionCallData.params, null, 2)+']')

        const block = eventLog.block
        const txHash = eventLog.txHash
        const destinationBlockchainId = eventLog.destinationBlockchainId
        if (!destinationBlockchainId) {
          return Promise.reject(Error('Destination chain not found while processing trade id [' + tradeId + ']: Unable to complete the lead leg'))
        }
        const counterpartyChainName = config.chainIdToChainName[destinationBlockchainId]
        if (!config[counterpartyChainName]) {
          return Promise.reject(Error('No configuration found for chain [' + destinationBlockchainId + ']: Unable to complete the lead leg'))
        }
        // Check that authentication parameters for the source chain xvp contract is set on the destination chain, if ethereum
        if (config[counterpartyChainName].type === 'ethereum') {
          let systemId = config[chainName].id
          let crosschainXvPAddress = await crosschainXVPContract.getContractAddress(systemId)
          logger.log('debug', 'Checking that authentication parameters exist in counterparty system [' + destinationBlockchainId + '] mapping systemId [' + systemId + '] to XvP contractAddress [' + crosschainXvPAddress + ']');
          let counterpartyChainAuthParams = (await crosschainFunctionCallContract.isAuthParams(destinationBlockchainId, systemId, crosschainXvPAddress)).isAuthParams
          if (!counterpartyChainAuthParams) {
            const authParamErr = new Error('Unable to process settlement instruction object for tradeId [' + settlementInstructionObj.tradeId.toString() + '] due to authentication parameters missing from counterparty system [' + destinationBlockchainId + '] mapping systemId [' + systemId + '] to XvP contractAddress [' + crosschainXvPAddress + ']');
            logger.log('error', authParamErr)
            throw authParamErr;
          } else {
            logger.log('debug', 'Authentication parameters exist in counterparty system [' + destinationBlockchainId + '] mapping systemId [' + systemId + '] to XvP contractAddress [' + crosschainXvPAddress + ']');
          }
        }
        logger.log('debug', 'Handling event [CrossBlockchainCallExecuted] from chain [' + chainName + '] with counter party chain [' + counterpartyChainName + ']');
        const context = {
          tradeId,
          functionName: eventLog.functionName
        }
        const proofObj = await dependencies.crosschainFunctionCallSDK.handleCrossBlockchainCallExecutedEvent(web3, block, txHash, chainName, counterpartyChainName, context)
        if (eventLog.functionName === 'requestFollowLeg') {
          logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForExecuteHoldExecuted] on chain [' + chainName + ']');
          settlementInstructionObj.state = settlementInstructionStates.waitingForExecuteHoldExecuted
          settlementInstructionObj.lastUpdate = Date.now()
          await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
        } else if (eventLog.functionName === 'completeLeadLeg') {
          settlementInstructionObj.settlementInstructionResult = {
            tradeId,
            systemId: config[counterpartyChainName].id,
            sourceSystemId: config[chainName].id,
            encodedInfo: proofObj.encodedInfo,
            signatureOrProof: proofObj.signatureOrProof
          }
          if (!!callbackURL) {
            logger.log('debug', 'Handling settlement instruction callback url for tradeId: [' + settlementInstructionObj.tradeId.toString() + ']')
            try {
              await handleSettlementInstructionCallback(callbackURL, settlementInstructionObj)
              logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [processed] on chain [' + chainName + ']');
              settlementInstructionObj.state = settlementInstructionStates.processed
              settlementInstructionObj.lastUpdate = Date.now()
              await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
            } catch (error) {
              logger.log('error', Error(error))
              logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForCommunication] on chain [' + chainName + ']');
              settlementInstructionObj.state = settlementInstructionStates.waitingForCommunication
              settlementInstructionObj.lastUpdate = Date.now()
              await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
            }
          } else {
            logger.log('debug', 'No callback url provided for settlement instruction with tradeId: [' + settlementInstructionObj.tradeId.toString() + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + ']')
            logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [processed] on chain [' + chainName + ']');
            settlementInstructionObj.state = settlementInstructionStates.processed
            settlementInstructionObj.lastUpdate = Date.now()
            await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
          }
        } else if (eventLog.functionName === 'performCancellation') {
          if (config[counterpartyChainName].type === 'corda') {
            logger.log('debug', 'Starting cancellation on Corda ledger [' + counterpartyChainName + '] using callback [' + settlementInstructionObj.callbackURL + '] for tradeId: [' + settlementInstructionObj.tradeId.toString() + ']');
            const params = {
              'tradeId': '' + tradeId,
              'encodedInfo': proofObj.encodedInfo,
              'signatureOrProof': proofObj.signatureOrProof
            }
            const path = process.cwd() + '/../dvp/corda2EthDemo/monitored';
            const response = await fetch(callbackURL, {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                'Export': path,
              },
              body: JSON.stringify(params)
            });
            let callbackResponseText = await response.json();
            logger.log('debug', 'Handled settlement instruction callback url: Response:\n' + JSON.stringify(callbackResponseText))
            logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [cancelled] on chain [' + chainName + ']');
            settlementInstructionObj.state = settlementInstructionStates.cancelled
            settlementInstructionObj.lastUpdate = Date.now()
            await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
          }
        }
      } else if (config[chainName].type === 'ethereum' && settlementInstructionObj.state === settlementInstructionStates.waitingForExecuteHoldExecuted) {
        const startBlock = 0 // If set to zero, the search will start X blocks back from the latest block
        const web3 = web3Store[chainName]
        const executedEvent = await settlementObligations.getExecuteHoldExecutedEventByOperationId(chainName, startBlock, settlementInstructionObj.localOperationId)
        if (!!executedEvent) {
          settlementInstructionObj.settlementInstructionResult = {
            tradeId: tradeId,
            sourceSystemId: config[chainName].id,
          }
          if (!!callbackURL) {
            logger.log('debug', 'Handling settlement instruction callback url for tradeId: [' + settlementInstructionObj.tradeId.toString() + ']')
            try {
              await handleSettlementInstructionCallback(callbackURL, settlementInstructionObj)
              logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [processed] on chain [' + chainName + ']');
              settlementInstructionObj.state = settlementInstructionStates.processed
              settlementInstructionObj.lastUpdate = Date.now()
              await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
            } catch (error) {
              logger.log('error', Error(error))
              logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForCommunication] on chain [' + chainName + ']');
              settlementInstructionObj.state = settlementInstructionStates.waitingForCommunication
              settlementInstructionObj.lastUpdate = Date.now()
              await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
            }
          } else {
            logger.log('debug', 'No callback url provided for settlement instruction with tradeId: [' + settlementInstructionObj.tradeId.toString() + '], fromAccount: [' + settlementInstructionObj.fromAccount + '], toAccount: [' + settlementInstructionObj.toAccount + ']')
            logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [processed] on chain [' + chainName + ']');
            settlementInstructionObj.state = settlementInstructionStates.processed
            settlementInstructionObj.lastUpdate = Date.now()
            await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
          }
        }
      }
    } catch (error) {
      return Promise.reject(error)
    }
  }

  async function transitionToTimedOutCommunication(settlementInstructionObj) {
    const chainName = settlementInstructionObj.chainName
    const tradeId = settlementInstructionObj.tradeId.toString()
    const callbackURL = settlementInstructionObj.callbackURL
    logger.log('debug', 'transitionToTimedOutCommunication: Handling settlement instruction callback url for tradeId: [' + tradeId + ']')
    try {
      await handleSettlementInstructionCallback(callbackURL, settlementInstructionObj)
      logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [processed] on chain [' + chainName + ']');
      settlementInstructionObj.state = settlementInstructionStates.processed
      settlementInstructionObj.lastUpdate = Date.now()
      await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
    } catch (error) {
      // If more than 5 minutes have passed, call callback with timeout error message
      const timeElapsed = Date.now() - settlementInstructionObj.lastUpdate
      if (timeElapsed >= 5 * 60 * 1000) {
        logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] as state [timedOutCommunication] on chain [' + chainName + ']');
        settlementInstructionObj.state = settlementInstructionStates.timedOutCommunication
        settlementInstructionObj.lastUpdate = Date.now()
        await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
      } else {
        logger.log('debug', 'Callback failed again. Keeping state [' + settlementInstructionObj.state + '] as state [waitingForCommunication] on chain [' + chainName + ']');
      }
    }
  }

  async function transitionToWaitingForCrossBlockchainCallExecuted(settlementInstructionObj) {
    try {
      const triggerLeadLeg = settlementInstructionObj.triggerLeadLeg
      const useForCancellation = settlementInstructionObj.useForCancellation
      const signatureOrProof = settlementInstructionObj.signatureOrProof
      const chainName = settlementInstructionObj.chainName
      const tradeId = settlementInstructionObj.tradeId.toString()
      const fromAccount = settlementInstructionObj.fromAccount
      const toAccount = settlementInstructionObj.toAccount
      const amount = settlementInstructionObj.amount
      const localOperationId = settlementInstructionObj.localOperationId

      if (!!triggerLeadLeg) {
        const web3 = web3Store[chainName]

        const holdsCreated = await Promise.all([
          settlementObligations.getMakeHoldPerpetualExecutedEventByOperationId(chainName, 0, localOperationId),
          settlementObligations.getCounterpartyChainMakeHoldPerpetualExecutedEvent(chainName, tradeId, fromAccount, toAccount)
        ])

        if (holdsCreated[0] === undefined || holdsCreated[1] === undefined) {
          // If more than 5 minutes have passed, call callback with timeout error message
          const timeElapsed = Date.now() - settlementInstructionObj.timestamp
          if (timeElapsed >= 5 * 60 * 1000) {
            logger.log('debug', 'Finding holds for settlement instruction with tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + '] has timed out, calling handleSettlementInstructionTimeout')
            return await handleSettlementInstructionTimeout(settlementInstructionObj)
          } else {
            if (holdsCreated[0] === undefined) {
              logger.log('debug', 'Hold for tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + '] not yet found on system [' + chainName + '], continuing the search')
            }
            if (holdsCreated[1] === undefined) {
              logger.log('debug', 'Hold for tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + '] not yet found on counterparty system, continuing the search')
            }
            return
          }
        }
        // The second hold will belong to the counterparty chain
        const counterpartyChainName = holdsCreated[1].chainName
        logger.log('debug', 'Found event [MakeHoldPerpetualExecuted] for tradeId [' + tradeId + '] on chain [' + chainName + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + '] and counter party chain: Triggering lead leg')
        const sll = await crosschainXVPContract.startLeadLeg(chainName, tradeId, fromAccount, toAccount, amount, counterpartyChainName)
        if (!!sll) {
          const settlementInstructionObjStateCheck = await dependencies.db.findSettlementInstructionByOperationId(settlementInstructionObj.systemId, settlementInstructionObj.localOperationId)
          if (settlementInstructionObjStateCheck.state !== settlementInstructionStates.waitingForHold) {
            logger.log('error', 'Settlement instruction no longer in [waitingForHold] state, cannot transition to [waitingForCrossBlockchainCallExecuted] for tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + ']')
            return
          }
          logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForCrossBlockchainCallExecuted]');
          settlementInstructionObj.state = settlementInstructionStates.waitingForCrossBlockchainCallExecuted
        } else {
          return Promise.reject(Error('Transaction starting lead leg for tradeId [' + tradeId + '] failed'))
        }
      } else if (!!signatureOrProof) { // This is only used for non-ethereum ledgers, where the cross-chain application API is the first point of entry into the system, and the call contains the necessary data to construct a proof.
        //const web3 = web3Store[chainName] // This uses the settlement instruction's ethereum system id, which marks the target system id for instructions coming from non-ethereum ledgers.
        if (!useForCancellation) {
          const holdMadePerpetual = await settlementObligations.getMakeHoldPerpetualExecutedEventByOperationId(chainName, 0, localOperationId) // Checks if an earmark exists on the target system.
          if (holdMadePerpetual === undefined) {
            const timeElapsed = Date.now() - settlementInstructionObj.timestamp
            if (timeElapsed >= 5 * 60 * 1000) {
              logger.log('debug', 'Finding hold for settlement instruction with tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + '] has timed out, calling function [handleSettlementInstructionTimeout]')
              return await handleSettlementInstructionTimeout(settlementInstructionObj)
            } else {
              logger.log('debug', 'Hold for tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + '] not yet found on system [' + chainName + '], continuing the search')
              return
            }
          }
        }
        const sourceSystemName = config.chainIdToChainName[signatureOrProof.sourceSystemId] // This uses the non-ethereum source system's id as contained in the included proof.
        let contractAddress = await crosschainXVPContract.getContractAddress(config[chainName].id)
        const decoderService = config[sourceSystemName].decoderService // For a non-ethereum system, we have to call an external service to build a proof.
        let functionName = useForCancellation ? 'performCancellation' : 'requestFollowLeg'
        let param = {
          blockchainId: signatureOrProof.sourceSystemId,
          contractAddress: contractAddress,
          functionName: functionName,
          withHiddenAuthParams: 'true',
          authBlockchainId: signatureOrProof.sourceSystemId,
          authContractAddress: contractAddress // TODO: This should be representative of the CorDapp that wants to perform a cross-chain function call to the xvp contract
        }
        logger.log('debug', 'Constructing call to decoder service for trade id [' + tradeId + ']')
        if (signatureOrProof.encodedEventData) {
          param.encodedInfo = signatureOrProof.encodedEventData;
        } else if (signatureOrProof.encodedSignature) {
          param.senderId = fromAccount;
          param.receiverId = toAccount;
          param.encodedId = tradeId;
          param.encodedKey = signatureOrProof.encodedKey;
          param.encodedSignature = signatureOrProof.encodedSignature;
          param.partialMerkleRoot = signatureOrProof.partialMerkleRoot;
          param.platformVersion = signatureOrProof.platformVersion;
          param.schemaNumber = signatureOrProof.schemaNumber;
        }
        logger.log('info', 'Performing call to decoder service for tradeId [' + tradeId + '], function [' + functionName + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + ']')
        let constructedProof = await utils.postData(decoderService, param)
        if (!!constructedProof.proof) { // handle provided proof
          const sourceSystemId = signatureOrProof.sourceSystemId
          const eventSig = constructedProof.proof.eventSig
          const encodedInfo = constructedProof.proof.encodedInfo // This looks different
          const encodedSignatureOrProof = constructedProof.proof.signatureOrProof // This looks different
          logger.log('info', 'Performing call from remote chain for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + ']')
          const pc = await dependencies.crosschainFunctionCallSDK.performCallFromRemoteChain(
            chainName,
            sourceSystemId,
            eventSig,
            encodedInfo,
            encodedSignatureOrProof
          )
          if (!!pc) {
            const settlementInstructionObjStateCheck = await dependencies.db.findSettlementInstructionByOperationId(settlementInstructionObj.systemId, settlementInstructionObj.localOperationId)
            if (settlementInstructionObjStateCheck.state !== settlementInstructionStates.waitingForHold) {
              logger.log('error', 'Settlement instruction no longer in [waitingForHold] state, cannot transition to [waitingForCrossBlockchainCallExecuted] for tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + ']')
              return
            }
            if (useForCancellation) {
              logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForCancelHoldExecuted]');
              settlementInstructionObj.state = settlementInstructionStates.waitingForCancelHoldExecuted
            } else {
              logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForCrossBlockchainCallExecuted]');
              settlementInstructionObj.state = settlementInstructionStates.waitingForCrossBlockchainCallExecuted
            }
          } else {
            return Promise.reject(Error('Transaction performing call from remote chain for tradeId [' + tradeId + '] and function [' + functionName + '] failed'))
          }
        } else {
          return Promise.reject(Error('Could not decode additional data fields from decoder output: Unable to perform call from remote chain for tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + ']'))
        }
      } else {
        const web3 = web3Store[chainName]
        const holdCreated = await settlementObligations.getMakeHoldPerpetualExecutedEventByOperationId(chainName, 0, localOperationId)
        if (holdCreated === undefined) {
          // If more than 5 minutes have passed, call callback with timeout error message
          const timeElapsed = Date.now() - settlementInstructionObj.timestamp
          if (timeElapsed >= 5 * 60 * 1000) {
            logger.log('debug', 'Finding hold for settlement instruction with tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + '] has timed out')
            return await handleSettlementInstructionTimeout(settlementInstructionObj)
          } else {
            logger.log('debug', 'Hold for tradeId [' + tradeId + '], fromAccount: [' + fromAccount + '], toAccount: [' + toAccount + '] not yet found on system [' + chainName + '], continuing the search')
            return
          }
        }
        logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForCrossBlockchainCallExecuted]');
        settlementInstructionObj.state = settlementInstructionStates.waitingForCrossBlockchainCallExecuted
      }
      settlementInstructionObj.lastUpdate = Date.now()
      await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
    } catch (error) {
      return Promise.reject(error)
    }
  }

  async function handleCancellation(settlementInstructionObj) {
    try {
      const chainName = settlementInstructionObj.chainName
      const tradeId = settlementInstructionObj.tradeId.toString()
      const fromAccount = settlementInstructionObj.fromAccount
      const toAccount = settlementInstructionObj.toAccount
      if (config[chainName].type === 'ethereum') {
        logger.log('debug', 'Settlement instruction cancellation for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + ']')
        const isCancelled = await crosschainXVPContract.getIsCancelled(chainName, settlementInstructionObj.localOperationId)
        if (!!isCancelled) {
          logger.log('debug', 'Settlement instruction cancellation: calling performCancellation for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + ']')
          const pc = await crosschainXVPContract.performCancellation(chainName, tradeId, fromAccount, toAccount)
          if (!!pc) {
            logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [cancelled] after handling cancellation');
            settlementInstructionObj.state = settlementInstructionStates.cancelled
          } else {
            return Promise.reject('Transaction performing cancellation for tradeId [' + tradeId + '] failed')
          }
          settlementInstructionObj.lastUpdate = Date.now()
          await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
        } else {
          const counterpartyChainName = config.chainIdToChainName[settlementInstructionObj.foreignSystemId]
          if (config[counterpartyChainName].type === 'ethereum') {
            const crosschainXvPAddress = await crosschainXVPContract.getContractAddress(config[chainName].id)
            const sourceBlockchainId = config[counterpartyChainName].id
            const destinationBlockchainId = config[chainName].id
            const destinationContract = crosschainXvPAddress
            logger.log('debug', 'Starting cancellation on Ethereum ledger [' + counterpartyChainName + ']: calling startCancellation directly for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + ']')
            const sc = await crosschainXVPContract.startCancellation(counterpartyChainName, tradeId, toAccount, fromAccount, sourceBlockchainId, destinationBlockchainId, destinationContract)
            if (!!sc) {
              logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForForeignSystemCancellation]');
              settlementInstructionObj.state = settlementInstructionStates.waitingForForeignSystemCancellation
            } else {
              return Promise.reject(Error('Transaction starting cancellation for tradeId [' + tradeId + '] failed'))
            }
            settlementInstructionObj.lastUpdate = Date.now()
            await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
          } else if (config[counterpartyChainName].type === 'corda') {
            const destinationContract = await crosschainXVPContract.getContractAddress(config[chainName].id)
            if (settlementInstructionObj.state === settlementInstructionStates.cancel) {
              logger.log('debug', 'Starting cancellation on Ethereum ledger [' + chainName + ']: calling startCancellation directly for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + ']')
              const sc = await crosschainXVPContract.startCancellation(chainName, tradeId, fromAccount, toAccount, config[chainName].id, settlementInstructionObj.foreignSystemId, destinationContract)
              if (!!sc) {
                logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForCrossBlockchainCallExecuted]');
                settlementInstructionObj.state = settlementInstructionStates.waitingForCrossBlockchainCallExecuted
              } else {
                return Promise.reject(Error('Transaction starting cancellation for tradeId [' + tradeId + '] failed'))
              }
              settlementInstructionObj.lastUpdate = Date.now()
              await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
            } else if (settlementInstructionObj.state === settlementInstructionStates.cancelling) {
              logger.log('debug', 'Performing cancellation on Ethereum ledger [' + chainName + ']: calling startCancellation via interop layer for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + ']');
              const sourceSystemName = config.chainIdToChainName[settlementInstructionObj.signatureOrProof.sourceSystemId]
              let contractAddress = await crosschainXVPContract.getContractAddress(config[chainName].id)
              const decoderService = config[sourceSystemName].decoderService
              let param = {
                blockchainId: settlementInstructionObj.signatureOrProof.sourceSystemId,
                contractAddress: contractAddress,
                functionName: 'performCancellation',
                withHiddenAuthParams: 'true',
                authBlockchainId: settlementInstructionObj.signatureOrProof.sourceSystemId,
                authContractAddress: contractAddress // TODO: This should be representative of the CorDapp that wants to perform a cross-chain function call to the xvp contract
              }
              logger.log('debug', 'Constructing call to decoder service for trade id [' + tradeId + ']')
              if (settlementInstructionObj.signatureOrProof.encodedEventData) {
                param.encodedInfo = settlementInstructionObj.signatureOrProof.encodedEventData;
              } else if (settlementInstructionObj.signatureOrProof.encodedSignature) {
                param.senderId = fromAccount;
                param.receiverId = toAccount;
                param.encodedId = tradeId;
                param.encodedKey = settlementInstructionObj.signatureOrProof.encodedKey;
                param.encodedSignature = settlementInstructionObj.signatureOrProof.encodedSignature;
                param.partialMerkleRoot = settlementInstructionObj.signatureOrProof.partialMerkleRoot;
                param.platformVersion = settlementInstructionObj.signatureOrProof.platformVersion;
                param.schemaNumber = settlementInstructionObj.signatureOrProof.schemaNumber;
              }
              logger.log('debug', 'Performing call to decoder service for trade id [' + tradeId + '] and function [performCancellation]')
              let constructedProof = await utils.postData(decoderService, param)
              if (!!constructedProof.proof) { // handle provided proof
                const sourceSystemId = settlementInstructionObj.signatureOrProof.sourceSystemId
                const eventSig = constructedProof.proof.eventSig
                const encodedInfo = constructedProof.proof.encodedInfo
                const encodedSignatureOrProof = constructedProof.proof.signatureOrProof
                logger.log('info', 'Performing call from remote chain for trade id [' + tradeId + '] and source system id [' + sourceSystemId + ']')
                const pc = await dependencies.crosschainFunctionCallSDK.performCallFromRemoteChain(
                  chainName,
                  sourceSystemId,
                  eventSig,
                  encodedInfo,
                  encodedSignatureOrProof
                )
                if (!!pc) {
                  logger.log('debug', 'Setting state [' + settlementInstructionObj.state + '] to state [waitingForCancelHoldExecuted]');
                  settlementInstructionObj.state = settlementInstructionStates.waitingForCancelHoldExecuted
                } else {
                  return Promise.reject(Error('Transaction performing call from remote chain for tradeId [' + tradeId + '] failed'))
                }
                settlementInstructionObj.lastUpdate = Date.now()
                await dependencies.db.updateSettlementInstructionStore(settlementInstructionObj)
              } else {
                return Promise.reject(Error('Could not decode additional data fields from decoder output: Unable to perform call from remote chain'))
              }
            }
          } else {
            logger.log('debug', Error('Starting cancellation on counter party ledger [' + counterpartyChainName + '] requires a ledger of known type [ethereum,corda]'));
          }
        }
      }
    } catch (error) {
      return Promise.reject(error)
    }
  }

  async function handleError(item, err) {
    logger.log('error', 'Error while processing settlement instruction: '+err);
    logger.log('debug', 'Setting settlement instruction state [' + item.state + '] to state [failed]');
    item.state = settlementInstructionStates.failed
    item.error = err
    item.lastUpdate = Date.now()
    await dependencies.db.updateSettlementInstructionStore(item)
  }

  let stopProcessing = false
  let hasStoppedProcessing = false

  async function start() {
    try {
      // First perform all state update requests
      const settlementInstructionsToUpdateList = await dependencies.db.findSettlementInstructionRequestsToProcess()
      for (let i in settlementInstructionsToUpdateList) {
        const item = settlementInstructionsToUpdateList[i]
        try {
          await handleSettlementInstructionUpdate(item)
          await dependencies.db.removeSettlementInstructionRequestFromStore(item)
        } catch (error) {
          logger.log('error', error)
          await dependencies.db.removeSettlementInstructionRequestFromStore(item)
          logger.log('debug', 'Unable to process settlement instruction update to transition to state [' + item.state + ']');
        }
      }

      // Next, process all state transitions
      const settlementInstructionsToProcessList = await dependencies.db.findSettlementInstructionsToProcess()
      const processListPromises = []
      for (let item of settlementInstructionsToProcessList) {
        try {
          if (!item.state) {
            logger.log('error', Error('Unable to process settlement instruction due to null or undefined state:' + JSON.stringify(item, null, 2)))
          } else if (item.state === settlementInstructionStates.waitingForForeignSystemCancellation || item.state === settlementInstructionStates.waitingForCancelHoldExecuted) {
            processListPromises.push(transitionToCancelledState(item).catch(async function (err) { await handleError(item, err) }))
          } else if (item.state === settlementInstructionStates.cancel || item.state === settlementInstructionStates.cancelling) {
            processListPromises.push(handleCancellation(item).catch(async function (err) { await handleError(item, err) }))
          } else if (item.state === settlementInstructionStates.waitingForCrossBlockchainCallExecuted || item.state === settlementInstructionStates.waitingForExecuteHoldExecuted) {
            processListPromises.push(transitionToEndState(item).catch(async function (err) { await handleError(item, err) }))
          } else if (item.state === settlementInstructionStates.waitingForHold) {
            processListPromises.push(transitionToWaitingForCrossBlockchainCallExecuted(item).catch(async function (err) { await handleError(item, err) }))
          } else if (item.state === settlementInstructionStates.confirmed) {
            processListPromises.push(transitionToWaitingForHold(item).catch(async function (err) { await handleError(item, err) }))
          } else if (item.state === settlementInstructionStates.waitingForCommunication) {
            processListPromises.push(transitionToTimedOutCommunication(item).catch(async function (err) { await handleError(item, err) }))
          } else {
            logger.log('error', Error('Unable to process settlement instruction due to invalid state:' + JSON.stringify(item, null, 2)))
          }
          await Promise.all(processListPromises)
        } catch (error) {
          await handleError(item, error)
        }
      }
    } catch (error) {
      logger.log('error', error)
    }

    await utils.sleep(1000)
    if (!stopProcessing) {
      start()
    } else {
      hasStoppedProcessing = true
    }
  }

  async function stop() {
    stopProcessing = true
    while (!hasStoppedProcessing) {
      await utils.sleep(1000)
      logger.log('debug', 'waiting for settlement instruction process to stop');
    }
    return Promise.resolve(true)
  }

  return {
    getSettlementInstruction,
    submitSettlementInstruction,
    deleteSettlementInstruction,
    patchSettlementInstruction,
    handleSettlementInstructionCallback,
    transitionToWaitingForHold,
    handleSettlementInstructionTimeout,
    transitionToEndState,
    startLeadLeg: crosschainXVPContract.startLeadLeg,
    transitionToWaitingForCrossBlockchainCallExecuted,
    startCancellation: crosschainXVPContract.startCancellation,
    performCancellation: crosschainXVPContract.performCancellation,
    start,
    stop
  }
}

module.exports = init
