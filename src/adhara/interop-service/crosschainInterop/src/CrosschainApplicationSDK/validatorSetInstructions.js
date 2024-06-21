const fetch = require('node-fetch')
const https = require('https')
const abiDecoder = require('abi-decoder');
const utils = require('./../CrosschainSDKUtils');
const validatorSetInstructionStates = require("./validatorSetInstructionStates");

function init(config, dependencies) {

  const logger = dependencies.logger
  const web3Store = dependencies.web3Store
  const interopManagerContract = dependencies.interopManagerContract
  const validatorSetManagerContract = dependencies.validatorSetManagerContract
  const crosschainFunctionCallContract = dependencies.crosschainFunctionCallContract

  if (dependencies.CrosschainMessagingJson) abiDecoder.addABI(dependencies.CrosschainMessagingJson.abi)
  if (dependencies.InteropManagerJson) abiDecoder.addABI(dependencies.InteropManagerJson.abi)

  async function getValidatorSetInstruction(networkId, operationId) {
    let validatorSetInstructionObj
    if (!!networkId && !!operationId) {
      validatorSetInstructionObj = await dependencies.db.findValidatorSetInstructionByOperationId(networkId, operationId)
    }
    if (!validatorSetInstructionObj) {
      return Promise.reject(Error('Validator update instruction from networkId [' + networkId + '] with operationId [' + operationId +'] was not found'))
    }

    return {
      networkId: validatorSetInstructionObj.networkId,
      operationId: validatorSetInstructionObj.operationId,
      state: validatorSetInstructionObj.state,
      creationDate: validatorSetInstructionObj.creationDate,
      lastUpdate: validatorSetInstructionObj.lastUpdate,
      humanReadableTimestamp: new Date(validatorSetInstructionObj.creationDate),
      result: validatorSetInstructionObj.result
    }
  }

  async function submitValidatorSetInstruction(networkId, validatorSetInstruction) {
    const networkName = config.networkIdToNetworkName[networkId]
    if (config[networkName].type !== 'ethereum') {
      return Promise.reject(Error('Validator sets can only be updated from Ethereum ledgers'))
    }
    const operationId = validatorSetInstruction.operationId
    const validators = validatorSetInstruction.validators
    if (!validators || validators.length === 0) {
      return Promise.reject(Error('Validator list is empty'))
    }
    logger.log('info', 'Received new validator set instruction from networkId [' + networkId + '] with operationId [' + operationId + ']')

    const state = validatorSetInstructionStates.confirmed
    const creationDate = Date.now()
    const lastUpdate = Date.now()
    const filters = []
    let numCallbackURLs = 0;
    if (!!validatorSetInstruction.filters) {
      for (let f=0; f < validatorSetInstruction.filters.length; f++) {
        let callbackURL = validatorSetInstruction.filters[f].callbackURL
        let networkId = validatorSetInstruction.filters[f].remoteDestinationNetworkId
        if (!!callbackURL) {
          // Since the proxy is going to inject the mTLS cert, we need to do a callout using only http
          if (callbackURL.startsWith('https') && config.performCallbackHttpsRewrite === true) {
            callbackURL = callbackURL.replace('https', 'http')
            logger.log('info', 'Adapted new validator set instruction from networkId [' + networkId + '] with callbackURL [' + callbackURL + '] to support mTLS')
          }
          numCallbackURLs++
        }
        filters.push({
          remoteDestinationNetworkId: networkId,
          callbackURL: callbackURL
        })
      }
    }
    let validatorSetInstructionObj = {
      networkName,
      networkId,
      operationId,
      validators,
      filters,
      state,
      creationDate,
      lastUpdate
    }
    await dependencies.db.addValidatorSetInstruction(validatorSetInstructionObj)
    if (numCallbackURLs === 0 || numCallbackURLs !== filters.length) {
      while (validatorSetInstructionObj.state !== validatorSetInstructionStates.processed && validatorSetInstructionObj.state !== validatorSetInstructionStates.failed) {
        const timeElapsed = Date.now() - validatorSetInstructionObj.timestamp
        if (timeElapsed >= 5 * 60 * 1000) {
          logger.log('debug', 'Validator set instruction from networkId [' + validatorSetInstructionObj.networkId + '] for operationId [' + validatorSetInstructionObj.operationId + '] has timed out')
          return await handleValidatorSetInstructionTimeout(validatorSetInstructionObj)
        } else {
          validatorSetInstructionObj = await dependencies.db.findValidatorSetInstructionByOperationId(validatorSetInstructionObj.networkId, validatorSetInstructionObj.operationId)
          await utils.sleep(2000)
        }
      }
      if (!validatorSetInstructionObj || !validatorSetInstructionObj.result) {
        if (!validatorSetInstructionObj) {
          return Promise.reject(Error('Validator set instruction was deleted or does not exist'))
        } else if (!!validatorSetInstructionObj.error) {
          return Promise.reject(validatorSetInstructionObj.error)
        }
        logger.log('debug', 'Validator set instruction with state [' + validatorSetInstructionObj.state + '] does not contain a response')
        return Promise.reject(Error('Validator set instruction result does not contain a response'))
      }
      return await adaptResponse(validatorSetInstructionObj)
    } else {
     return {
       operationId: operationId,
       state: state
     }
    }
  }

  async function deleteValidatorSetInstruction(networkId, operationId) {
    let validatorSetInstructionObj
    if (!!networkId && !!operationId) {
      validatorSetInstructionObj = await dependencies.db.findValidatorSetInstructionByOperationId(networkId, operationId)
    }
    if (!validatorSetInstructionObj) {
      return Promise.reject(Error('Validator update instruction from networkId [' + networkId + '] with operationId [' + operationId + '] was not found'))
    }

    if (!!validatorSetInstructionObj.state) {
      if (validatorSetInstructionObj.state === validatorSetInstructionStates.confirmed
        || validatorSetInstructionObj.state === validatorSetInstructionStates.waitingForHold
        || validatorSetInstructionObj.state === validatorSetInstructionStates.failed) {
        await dependencies.db.removeValidatorSetInstruction(networkId, operationId)
      } else {
        return Promise.reject(Error('Validator update instruction from system [' + validatorSetInstructionObj.networkId + '] with state [' + validatorSetInstructionObj.state + '] cannot be deleted'))
      }
    }

    // Sanitise before sending back
    const response = {
      networkId: validatorSetInstructionObj.networkId,
      operationId: validatorSetInstructionObj.operationId,
      validators: validatorSetInstructionObj.validators,
      filters: validatorSetInstructionObj.filters,
      state: validatorSetInstructionObj.state,
      creationDate: validatorSetInstructionObj.creationDate,
      lastUpdate: validatorSetInstructionObj.lastUpdate,
      humanReadableTimestamp: new Date(validatorSetInstructionObj.creationDate)
    }

    return response
  }

  async function handleValidatorSetInstructionCallback(validatorSetInstructionObj) {
    for (let f=0; f < validatorSetInstructionObj.filters.length; f++) {
      let callbackURL = validatorSetInstructionObj.filters[f].callbackURL
      if (!!callbackURL) {
        try {
          const params = await adaptResponseForSource(validatorSetInstructionObj, validatorSetInstructionObj.filters[f].remoteDestinationNetworkId)
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
            callbackResponse.headers.get('Content-Type');
          } catch (err) {
            logger.log('error', 'Unable to get content type from callback response for validator set instruction from networkId [' + validatorSetInstructionObj.networkId + '] for operationId [' + validatorSetInstructionObj.operationId + ']: Error: ' + err)
          }
          const callbackResponseText = await callbackResponse.text()
          logger.log('debug', 'Handled callback for validator set instruction from networkId [' + validatorSetInstructionObj.networkId + '] for operationId [' + validatorSetInstructionObj.operationId + ']: Response:\n' + callbackResponseText)
        } catch (error) {
          logger.log('error', Error(error))
        }
      } else {
        logger.log('debug', 'No callback url provided for validator set instruction from networkId [' + validatorSetInstructionObj.networkId + '] for operationId [' + validatorSetInstructionObj.operationId + ']')
      }
    }
  }

  async function adaptResponseForSource(validatorSetInstructionObj, remoteDestinationNetworkId) {
    for (let r=0; r<validatorSetInstructionObj.result.length; r++) {
      if (validatorSetInstructionObj.result[r].networkId === remoteDestinationNetworkId) {
        logger.log('debug', 'Returning validator set instruction result for operationId [' + validatorSetInstructionObj.operationId + '] ' + 'and remote destination chain with networkId [' + remoteDestinationNetworkId + ']: ' + JSON.stringify(validatorSetInstructionObj.result[r]))
        return validatorSetInstructionObj.result[r]
      }
    }
    logger.log('warn', 'No validator set instruction result was found for operationId [' + validatorSetInstructionObj.operationId + '] ' + 'and remote destination chain with networkId [' + remoteDestinationNetworkId + ']')
  }

  async function handleValidatorSetInstructionTimeout(validatorSetInstructionObj) {
    logger.log('debug', 'Setting state [' + validatorSetInstructionObj.state + '] to state [timedOut]');
    validatorSetInstructionObj.state = validatorSetInstructionStates.timedOut
    validatorSetInstructionObj.lastUpdate = Date.now()
    await dependencies.db.updateValidatorSetInstruction(validatorSetInstructionObj)
    for (let f=0; f < validatorSetInstructionObj.filters.length; f++) {
      let callbackURL = validatorSetInstructionObj.filters[f].callbackURL
      if (!!callbackURL) {
        try {
          const params = {
            error: "Validator set instruction on networkId " + validatorSetInstructionObj.networkId + " with operationId: " + validatorSetInstructionObj.operationId + " has timed out"
          }
          const callbackResponse = await fetch(callbackURL, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(params)
          })
          const callbackResponseJson = await callbackResponse.json()
          logger.log('debug', 'Handled callback url for validator set instruction from networkId [' + validatorSetInstructionObj.networkId + '] for operationId [' + validatorSetInstructionObj.operationId + ']: Response:\n' + JSON.stringify(callbackResponseJson))
        } catch (error) {
          logger.log('error', Error(error))
        }
      } else {
        logger.log('debug', 'No callback url provided for validator set instruction from networkId [' + validatorSetInstructionObj.networkId + '] for operationId [' + validatorSetInstructionObj.operationId + ']')
      }
    }
  }

  async function transitionToWaitingForCrosschainFunctionCall(validatorSetInstructionObj) {
    try {
      logger.log('debug', 'Starting remote validator sync from networkId [' + validatorSetInstructionObj.networkId + ']');
      const sv = await validatorSetManagerContract.setValidatorsAndSyncRemotes(validatorSetInstructionObj.networkId, validatorSetInstructionObj.validators)
      if (!!sv) {
        logger.log('debug', 'Setting state [' + validatorSetInstructionObj.state + '] to state [waitingForCrosschainFunctionCall]');
        validatorSetInstructionObj.blockNumber = sv.blockNumber
        validatorSetInstructionObj.processedAt = sv.processedAt
        validatorSetInstructionObj.state = validatorSetInstructionStates.waitingForCrosschainFunctionCall
      } else {
        return Promise.reject(Error('Transaction starting validator update from networkId [' + validatorSetInstructionObj.networkId + '] to update remote destination chains failed'))
      }
      validatorSetInstructionObj.lastUpdate = Date.now()
      await dependencies.db.updateValidatorSetInstruction(validatorSetInstructionObj)
    } catch (error) {
      return Promise.reject(Error(error))
    }
  }

  async function transitionToProcessed(validatorSetInstructionObj) {
    try {
      const networkName = validatorSetInstructionObj.networkName
      if (config[networkName].type === 'ethereum' && validatorSetInstructionObj.state === validatorSetInstructionStates.waitingForCrosschainFunctionCall) {
        logger.log('debug', 'Finding event to do validator update from networkId [' + validatorSetInstructionObj.networkId + '] with operationId [' + validatorSetInstructionObj.operationId + ']');
        const startBlock = validatorSetInstructionObj.processedAt
        let filters = new Map([['networkId', validatorSetInstructionObj.networkId],
                               ['blockNumber', validatorSetInstructionObj.blockNumber]]);
        const executedEvents = await getCrosschainFunctionCallEvents(networkName, startBlock, startBlock, 'setValidatorList', filters)
        if (executedEvents.length === 0) {
          return
        }
        const executedResults = []
        for (let i=0; i<executedEvents.length; i++) {
          const executedEvent = executedEvents[i]
          const block = executedEvent.block
          const txHash = executedEvent.txHash
          const decodedLog = {
            functionName: executedEvent.functionName,
            functionParameters: executedEvent.decodedFunctionCallData.params
          }
          const destinationNetworkId = executedEvent.destinationNetworkId
          if (!destinationNetworkId) {
            return Promise.reject(Error('Destination system not found while processing validator update instruction from networkId [' + validatorSetInstructionObj.networkId + ']'))
          }
          if (validatorSetInstructionObj.filters.length > 0 && !validatorSetInstructionObj.filters.some(f => Number(f.remoteDestinationNetworkId) === Number(destinationNetworkId))) {
            continue
          }
          const counterpartyNetworkName = config.networkIdToNetworkName[destinationNetworkId]
          if (!config[counterpartyNetworkName]) {
            return Promise.reject(Error('No configuration found for destination system with networkId [' + destinationNetworkId + ']'))
          }
          // Check that authentication parameters for the source chain contract is set on the destination chain, if ethereum
          if (config[counterpartyNetworkName].type === 'ethereum') {
            let networkId = config[networkName].id
            let validatorSetManagerAddress = await validatorSetManagerContract.getContractAddress(networkId)
            let counterpartyChainAuthParams = (await crosschainFunctionCallContract.isAuthParams(destinationNetworkId, networkId, validatorSetManagerAddress)).isAuthParams
            if (!counterpartyChainAuthParams) {
              return Promise.reject(Error('Unable to process validator update instruction for destination system with networkId [' + destinationNetworkId + '] due to lack of authentication parameters mapping networkId [' + networkId + '] to contractAddress [' + validatorSetManagerAddress + ']'))
            } else {
              logger.log('debug', 'Authentication parameters exist for destination system with networkId [' + destinationNetworkId + '] mapping networkId [' + networkId + '] to contractAddress [' + validatorSetManagerAddress + ']');
            }
          }
          logger.log('debug', 'Handling event [CrosschainFunctionCall] from chain [' + networkName + '] with counter party chain [' + counterpartyNetworkName + ']');
          const proofObj = await dependencies.crosschainFunctionCallSDK.handleCrosschainFunctionCallEvent(web3Store[networkName], block, txHash, networkName, counterpartyNetworkName, decodedLog)
          if (executedEvent.decodedFunctionCallData.name === 'setValidatorList') {  // First 4 bytes of keccak256 hash of 'setValidatorList(uint256,uint256,address[])'
            executedResults.push({
              networkId: config[counterpartyNetworkName].id,
              sourceNetworkId: config[networkName].id,
              encodedInfo: proofObj.encodedInfo,
              signatureOrProof: proofObj.encodedProof
            })
          }
        }
        validatorSetInstructionObj.result = executedResults
        await handleValidatorSetInstructionCallback(validatorSetInstructionObj)
        logger.log('debug', 'Setting state [' + validatorSetInstructionObj.state + '] to state [processed] on chain [' + networkName + ']');
        validatorSetInstructionObj.state = validatorSetInstructionStates.processed
        validatorSetInstructionObj.lastUpdate = Date.now()
        await dependencies.db.updateValidatorSetInstruction(validatorSetInstructionObj)
      }
    } catch (error) {
      return Promise.reject(Error(error))
    }
  }

  async function adaptResponse(validatorSetInstructionObj) {
    return validatorSetInstructionObj.result
  }

  async function getCrosschainFunctionCallEvents(networkName, fromBlock, toBlock, functionName, paramsMap) {
    const events = []
    const eventLogs = await interopManagerContract.findCrosschainFunctionCallEvents(networkName, fromBlock, toBlock)
    for (let eventLog of eventLogs) {
      const decodedFunctionCallData = abiDecoder.decodeMethod(eventLog.decodedLog.functionCallData)
      if (!decodedFunctionCallData || !decodedFunctionCallData.params) {
        continue
      }
      if (functionName === decodedFunctionCallData.name) {
        let matchAll = true;
        for (let [key, value] of paramsMap.entries()) {
          let match = false;
          if (typeof key === 'string') {
            const eventParam = decodedFunctionCallData.params.find((item) => item.name === key)
            match = !!eventParam || value.toString() === eventParam.value.toString()
          }
          if (!match) {
            matchAll = false;
            break;
          }
        }
        if (matchAll) {
          const block = await web3Store[networkName].eth.getBlock(eventLog.blockNumber)
          events.push({
            block,
            txHash: eventLog.txHash,
            functionName: decodedFunctionCallData.name,
            destinationNetworkId: eventLog.decodedLog.networkId,
            decodedFunctionCallData,
            paramsMap,
          })
        }
      }
    }
    return events
  }

  async function handleError(item, err) {
    logger.log('debug', 'Setting remote validator set update instruction state [' + item.state + '] to state [failed]');
    item.state = validatorSetInstructionStates.failed
    item.error = err
    item.lastUpdate = Date.now()
    await dependencies.db.updateValidatorSetInstruction(item)
  }

  let stopProcessing = false
  let hasStoppedProcessing = false

  async function start() {
    try {
      const validatorSetInstructionsToProcessList = await dependencies.db.findValidatorSetInstructionsToProcess()
      const processListPromises = []
      for (let item of validatorSetInstructionsToProcessList) {
        try {
          if (!item.state) {
            logger.log('error', Error('Unable to process validator set instruction due to null or undefined state [' + JSON.stringify(item, null, 2)) + ']')
          } else if (item.state === validatorSetInstructionStates.waitingForCrosschainFunctionCall) {
            processListPromises.push(transitionToProcessed(item).catch(async function(err) { await handleError(item, err) }))
          } else if (item.state === validatorSetInstructionStates.confirmed) {
            processListPromises.push(transitionToWaitingForCrosschainFunctionCall(item).catch(async function(err) { await handleError(item, err) }))
          } else {
            logger.log('error', Error('Unable to process validator set instruction due to invalid state [' + JSON.stringify(item, null, 2)) + ']')
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
      logger.log('debug', 'Waiting for validator set instruction processor to stop');
    }
    return Promise.resolve(true)
  }

  return {
    getValidatorSetInstruction,
    submitValidatorSetInstruction,
    deleteValidatorSetInstruction,
    start,
    stop
  }
}

module.exports = init
