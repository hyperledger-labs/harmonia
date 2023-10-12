const fetch = require('node-fetch')
const https = require('https')
const abiDecoder = require('abi-decoder');
const utils = require('./../CrosschainSDKUtils');
const validatorUpdateInstructionStates = require("./validatorUpdateInstructionStates");

function init(config, dependencies) {

  const logger = dependencies.logger
  const web3Store = dependencies.web3Store
  const crosschainFunctionCallContract = dependencies.crosschainFunctionCallContract
  const crosschainMessagingContract = dependencies.crosschainMessagingContract

  if (dependencies.CrosschainFunctionCallJson) abiDecoder.addABI(dependencies.CrosschainFunctionCallJson.abi)

  async function getValidatorUpdateInstruction(systemId, operationId) {
    let validatorUpdateInstructionObj
    if (!!systemId && !!operationId) {
      validatorUpdateInstructionObj = await dependencies.db.findValidatorUpdateInstructionByOperationId(systemId, operationId)
    }
    if (!validatorUpdateInstructionObj) {
      return Promise.reject(Error('Validator update instruction from systemId [' + systemId + '] with operationId [' + operationId +'] was not found'))
    }

    return {
      systemId: validatorUpdateInstructionObj.systemId,
      foreignSystemId: validatorUpdateInstructionObj.foreignSystemId,
      operationId: validatorUpdateInstructionObj.operationId,
      state: validatorUpdateInstructionObj.state,
      creationDate: validatorUpdateInstructionObj.creationDate,
      lastUpdate: validatorUpdateInstructionObj.lastUpdate,
      humanReadableTimestamp: new Date(validatorUpdateInstructionObj.creationDate)
    }
  }

  async function submitValidatorUpdateInstruction(systemId, validatorUpdateInstruction) {
    const chainName = config.chainIdToChainName[systemId]
    if (config[chainName].type !== 'ethereum') {
      return Promise.reject(Error('Validator updates can only be done from Ethereum ledgers'))
    }
    const foreignSystemId = validatorUpdateInstruction.foreignSystemId
    const foreignChainName = config.chainIdToChainName[foreignSystemId]
    if (!foreignChainName) {
      return Promise.reject(Error('Unknown foreign system with systemId [' + foreignSystemId + ']'))
    }
    if (config[foreignChainName].type !== 'ethereum') {
      return Promise.reject(Error('Validator updates can only be done for Ethereum ledgers'))
    }
    const operationId = validatorUpdateInstruction.operationId
    const blockHeader = validatorUpdateInstruction.blockHeader
    const contractAddress = validatorUpdateInstruction.contractAddress
    if (!blockHeader && !contractAddress) {
      return Promise.reject(Error('Validator updates require either a block header or a validator selection contract'))
    }
    logger.log('info', 'Received new validator update instruction from systemId [' + systemId + '] to update systemId [' + foreignSystemId + '] with operationId [' + operationId + ']')

    const state = validatorUpdateInstructionStates.confirmed
    const creationDate = Date.now()
    const lastUpdate = Date.now()

    // Since the proxy is going to inject the mTLS cert, we need to do a callout using only http
    let callbackURL = validatorUpdateInstruction.callbackURL
    if (!!callbackURL && callbackURL.startsWith('https') && config.performCallbackHttpsRewrite === true) {
      callbackURL = callbackURL.replace('https', 'http')
      logger.log('info', 'Adapted new validator update instruction from systemId [' + systemId + '] with callbackURL [' + callbackURL + '] to support mTLS')
    }

    let validatorUpdateInstructionObj = {
      chainName,
      systemId,
      foreignSystemId,
      operationId,
      blockHeader,
      contractAddress,
      callbackURL,
      state,
      creationDate,
      lastUpdate
    }

    await dependencies.db.addValidatorUpdateInstruction(validatorUpdateInstructionObj)

    if (!callbackURL) {
      while (validatorUpdateInstructionObj.state !== validatorUpdateInstructionStates.processed && validatorUpdateInstructionObj.state !== validatorUpdateInstructionStates.failed) {
        const timeElapsed = Date.now() - validatorUpdateInstructionObj.timestamp
        if (timeElapsed >= 5 * 60 * 1000) {
          logger.log('debug', 'Validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + '] for operationId [' + validatorUpdateInstructionObj.operationId + '] has timed out')
          return await handleValidatorUpdateInstructionTimeout(validatorUpdateInstructionObj)
        } else {
          validatorUpdateInstructionObj = await dependencies.db.findValidatorUpdateInstructionByOperationId(validatorUpdateInstructionObj.systemId, validatorUpdateInstructionObj.operationId)
          await utils.sleep(2000)
        }
      }
      if (!validatorUpdateInstructionObj || !validatorUpdateInstructionObj.result) {
        if (!validatorUpdateInstructionObj) {
          return Promise.reject(Error('Validator update instruction was deleted or does not exist'))
        } else if (!!validatorUpdateInstructionObj.error) {
          return Promise.reject(validatorUpdateInstructionObj.error)
        }
        logger.log('debug', 'Validator update instruction with state [' + validatorUpdateInstructionObj.state + '] does not contain a response')
        return Promise.reject(Error('Validator update instruction result does not contain a response'))
      }
      return await adaptResponse(validatorUpdateInstructionObj)
    } else {
      return {
        "operationId": operationId,
        "state": state
      }
    }
  }

  async function handleValidatorUpdateInstructionStateUpdate(validatorUpdateInstructionRequest) {
    const validatorUpdateInstructionObj = await dependencies.db.findValidatorUpdateInstructionByOperationId(validatorUpdateInstructionRequest.systemId, validatorUpdateInstructionRequest.operationId)
    if (!validatorUpdateInstructionObj) {
      return Promise.reject(Error('Validator update instruction from systemId [' + validatorUpdateInstructionRequest.systemId + '] with operationId [' + validatorUpdateInstructionRequest.operationId + ']) was not found'))
    }
    if (!!validatorUpdateInstructionObj.state) {
      if (validatorUpdateInstructionRequest.state === validatorUpdateInstructionStates.failed) {
        validatorUpdateInstructionObj.state = validatorUpdateInstructionRequest.failed
        validatorUpdateInstructionObj.lastUpdate = Date.now()
        await dependencies.db.updateValidatorUpdateInstruction(validatorUpdateInstructionObj)
      } else {
        return Promise.reject(Error('Validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + '] for operationId [' + validatorUpdateInstructionObj.operationId + '] with state [' + validatorUpdateInstructionObj.state + '] is not transitionable to state [' + validatorUpdateInstructionRequest.state + ']'))
      }
    } else {
      logger.log('error', 'No state found on validator update instruction for validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']')
    }
  }

  async function handleValidatorUpdateInstructionCallback(callbackURL, validatorUpdateInstructionObj) {
    if (!!callbackURL) {
      try {
        const params = await adaptResponseForSource(validatorUpdateInstructionObj)
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
          logger.log('error', 'Unable to get content type from callback response for validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']: Error: ' + err)
        }
        const callbackResponseText = await callbackResponse.text()
        logger.log('debug', 'Handled callback for validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']: Response:\n' + callbackResponseText)
      } catch (error) {
        logger.log('error', Error(error))
      }
    } else {
      logger.log('debug', 'No callback url provided for validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']')
    }
  }

  async function handleValidatorUpdateInstructionTimeout(validatorUpdateInstructionObj) {
    logger.log('debug', 'Setting state [' + validatorUpdateInstructionObj.state + '] to state [timedOut]');
    validatorUpdateInstructionObj.state = validatorUpdateInstructionStates.timedOut
    validatorUpdateInstructionObj.lastUpdate = Date.now()
    await dependencies.db.updateValidatorUpdateInstruction(validatorUpdateInstructionObj)

    const callbackURL = validatorUpdateInstructionObj.callbackURL
    if (!!callbackURL) {
      try {
        const params = {
          error: "validator update instruction on systemId " + validatorUpdateInstructionObj.systemId + " with operationId: " + validatorUpdateInstructionObj.operationId + " has timed out"
        }
        const callbackResponse = await fetch(callbackURL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(params)
        })
        const callbackResponseJson = await callbackResponse.json()
        logger.log('debug', 'Handled callback url for validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']: Response:\n' + JSON.stringify(callbackResponseJson))
      } catch (error) {
        logger.log('error', Error(error))
      }
    } else {
      logger.log('debug', 'No callback url provided for validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']')
    }
  }

  async function transitionToWaitingForCrossBlockchainCallExecuted(validatorUpdateInstructionObj) {
    try {
      let msgContractAddress = await crosschainMessagingContract.getContractAddress(validatorUpdateInstructionObj.foreignSystemId)
      logger.log('debug', 'Starting validator update from systemId [' + validatorUpdateInstructionObj.systemId + '] to update systemId [' + validatorUpdateInstructionObj.foreignSystemId + '] with messaging contract address [' + msgContractAddress + ']');
      const sv = await dependencies.crosschainFunctionCallSDK.startValidatorUpdate(validatorUpdateInstructionObj.chainName,
        validatorUpdateInstructionObj.operationId,
        validatorUpdateInstructionObj.blockHeader,     // RLP-encoded block header with no round number, use only when block header validator selection is used
        validatorUpdateInstructionObj.contractAddress, // Address of validator set management contract, use only when contract validator selection is used
        msgContractAddress,                            // Contains the foreign ledger (messaging) contract's addres
        validatorUpdateInstructionObj.foreignSystemId, // Contains the foreign ledger's chain id

      )
      if (!!sv) {
        logger.log('debug', 'Setting state [' + validatorUpdateInstructionObj.state + '] to state [waitingForCrossBlockchainCallExecuted]');
        validatorUpdateInstructionObj.state = validatorUpdateInstructionStates.waitingForCrossBlockchainCallExecuted
      } else {
        return Promise.reject(Error('Transaction starting validator update from systemId [' + validatorUpdateInstructionObj.systemId + '] to update systemId [' + validatorUpdateInstructionObj.foreignSystemId + '] failed'))
      }
      validatorUpdateInstructionObj.lastUpdate = Date.now()
      await dependencies.db.updateValidatorUpdateInstruction(validatorUpdateInstructionObj)
    } catch (error) {
      return Promise.reject(Error(error))
    }
  }

  async function transitionToProcessed(validatorUpdateInstructionObj) {
    try {
      const chainName = validatorUpdateInstructionObj.chainName
      const callbackURL = validatorUpdateInstructionObj.callbackURL
      if (config[chainName].type === 'ethereum' && validatorUpdateInstructionObj.state === validatorUpdateInstructionStates.waitingForCrossBlockchainCallExecuted) {
        logger.log('debug', 'Finding event to do validator update from systemId [' + validatorUpdateInstructionObj.systemId + '] to update systemId [' + validatorUpdateInstructionObj.foreignSystemId + '] with operationId [' + validatorUpdateInstructionObj.operationId + ']');
        const startBlock = 0 // If set to zero, the search will start X blocks back from the latest block
        const web3 = web3Store[chainName]
        const executedEvent = await getCrossBlockchainCallExecutedEventByOperationId(chainName, startBlock, validatorUpdateInstructionObj.foreignSystemId, validatorUpdateInstructionObj.operationId/*'0x5029d045'*/)
        if (!executedEvent) {
          return
        }
        const block = executedEvent.block
        const txHash = executedEvent.txHash
        const decodedLog = executedEvent.decodedLog
        const destinationBlockchainId = executedEvent.destinationBlockchainId
        if (!destinationBlockchainId) {
          return Promise.reject(Error('Destination system not found while processing validator update instruction from systemId [' + validatorUpdateInstructionObj.systemId + ']'))
        }
        const counterpartyChainName = config.chainIdToChainName[destinationBlockchainId]
        if (!config[counterpartyChainName]) {
          return Promise.reject(Error('No configuration found for destination system with systemId [' + destinationBlockchainId + ']'))
        }
        // Check that authentication parameters for the source chain xvp contract is set on the destination chain, if ethereum
        if (config[counterpartyChainName].type === 'ethereum') {
          let systemId = config[chainName].id
          let crosschainFCAddress = await crosschainFunctionCallContract.getContractAddress(systemId)
          let counterpartyChainAuthParams = (await crosschainFunctionCallContract.isAuthParams(destinationBlockchainId, systemId, crosschainFCAddress)).isAuthParams
          if (!counterpartyChainAuthParams) {
            const authParamErr = new Error('Unable to process validator update instruction for destination system with systemId [' + destinationBlockchainId + '] due to lack of authentication parameters mapping systemId [' + systemId + '] to contractAddress [' + crosschainFCAddress + ']');
            throw authParamErr;
          } else {
            logger.log('debug', 'Authentication parameters exist for destination system with systemId [' + destinationBlockchainId + '] mapping systemId [' + systemId + '] to contractAddress [' + crosschainFCAddress + ']');
          }
        }
        logger.log('debug', 'Handling event [CrossBlockchainCallExecuted] from chain [' + chainName + '] with counter party chain [' + counterpartyChainName + ']');
        const proofObj = await dependencies.crosschainFunctionCallSDK.handleCrossBlockchainCallExecutedEvent(web3, block, txHash, chainName, counterpartyChainName, decodedLog)
        if (proofObj.event === 'setValidatorList') {
          validatorUpdateInstructionObj.result = {
            systemId: config[counterpartyChainName].id,
            sourceSystemId: config[chainName].id,
            encodedInfo: proofObj.encodedInfo,
            signatureOrProof: proofObj.signatureOrProof
          }
          await handleValidatorUpdateInstructionCallback(callbackURL, validatorUpdateInstructionObj)
          logger.log('debug', 'Setting state [' + validatorUpdateInstructionObj.state + '] to state [processed] on chain [' + chainName + ']');
          validatorUpdateInstructionObj.state = validatorUpdateInstructionStates.processed
          validatorUpdateInstructionObj.lastUpdate = Date.now()
          await dependencies.db.updateValidatorUpdateInstruction(validatorUpdateInstructionObj)
        }
      }
    } catch (error) {
      return Promise.reject(Error(error))
    }
  }
  async function adaptResponse(validatorUpdateInstructionObj) {
    return validatorUpdateInstructionObj.result
  }

  async function getCrossBlockchainCallExecutedEventByOperationId(chainName, startBlock, destinationBlockchainId, operationId){

    const eventLogs = await crosschainFunctionCallContract.findCrossBlockchainCallExecutedEvent(chainName, startBlock)

    for(let eventLog of eventLogs){
      const decodedFunctionCallData = abiDecoder.decodeMethod(eventLog.decodedLog.functionCallData)
      if(!decodedFunctionCallData || !decodedFunctionCallData.params){
        continue
      }
      const eventOperationId = decodedFunctionCallData.params.find((item) => item.name === 'operationId')
      if(((!operationId) || (!!operationId && !!eventOperationId && eventOperationId.value.toString() === operationId.toString()))
        && ((!destinationBlockchainId) || (!!destinationBlockchainId && !!eventLog.decodedLog && eventLog.decodedLog.destinationBlockchainId.toString() === destinationBlockchainId.toString()))
      ){
        const block = await web3Store[chainName].eth.getBlock(eventLog.blockNumber)
        return {
          block,
          txHash: eventLog.txHash,
          functionName: decodedFunctionCallData.name,
          destinationBlockchainId: eventLog.decodedLog.destinationBlockchainId,
          decodedFunctionCallData,
          operationId,
        }
      }
    }
    return undefined
  }

  async function handleError(item, err) {
    logger.log('debug', 'Setting validator update instruction state [' + item.state + '] to state [failed]');
    item.state = validatorUpdateInstructionStates.failed
    item.error = err
    item.lastUpdate = Date.now()
    await dependencies.db.updateValidatorUpdateInstruction(item)
  }

  async function start() {
    try {
      const validatorUpdateInstructionsToProcessList = await dependencies.db.findValidatorUpdateInstructionsToProcess()
      const processListPromises = []
      for (let item of validatorUpdateInstructionsToProcessList) {
        try {
          if (!item.state) {
            logger.log('error', Error('Unable to process validator update instruction due to null or undefined state [' + JSON.stringify(item, null, 2)) + ']')
          } else if (item.state === validatorUpdateInstructionStates.waitingForCrossBlockchainCallExecuted) {
            processListPromises.push(transitionToProcessed(item).catch(async function(err) { await handleError(item, err) }))
          } else if (item.state === validatorUpdateInstructionStates.confirmed) {
            processListPromises.push(transitionToWaitingForCrossBlockchainCallExecuted(item).catch(async function(err) { await handleError(item, err) }))
          } else {
            logger.log('error', Error('Unable to process validator update instruction due to invalid state [' + JSON.stringify(item, null, 2)) + ']')
          }
        } catch (error) {
          await handleError(item, error)
        }
      }
      await Promise.all(processListPromises)
    } catch (error) {
      logger.log('error', error)
    }

    await utils.sleep(1000)
    start()
  }

  return {
    getValidatorUpdateInstruction,
    submitValidatorUpdateInstruction,
    start
  }
}

module.exports = init
