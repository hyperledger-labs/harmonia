const fetch = require('node-fetch')
const https = require('https')
const abiDecoder = require('abi-decoder');
const utils = require('./../CrosschainSDKUtils');
const validatorUpdateInstructionStates = require('./validatorUpdateInstructionStates');

function init(config, dependencies) {

  const logger = dependencies.logger
  const web3Store = dependencies.web3Store
  const crosschainFunctionCallContract = dependencies.crosschainFunctionCallContract
  const interopManagerContract = dependencies.interopManagerContract
  const validatorSetManagerContract = dependencies.validatorSetManagerContract

  if (dependencies.CrosschainMessagingJson) abiDecoder.addABI(dependencies.CrosschainMessagingJson.abi)
  if (dependencies.InteropManagerJson) abiDecoder.addABI(dependencies.InteropManagerJson.abi)

  async function getValidatorUpdateInstruction(networkId, operationId) {
    let validatorUpdateInstructionObj
    if (!!networkId && !!operationId) {
      validatorUpdateInstructionObj = await dependencies.db.findValidatorUpdateInstructionByOperationId(networkId, operationId)
    }
    if (!validatorUpdateInstructionObj) {
      return Promise.reject(Error('Validator update instruction from networkId [' + networkId + '] with operationId [' + operationId + '] was not found'))
    }

    return {
      networkId: validatorUpdateInstructionObj.networkId,
      operationId: validatorUpdateInstructionObj.operationId,
      state: validatorUpdateInstructionObj.state,
      creationDate: validatorUpdateInstructionObj.creationDate,
      lastUpdate: validatorUpdateInstructionObj.lastUpdate,
      humanReadableTimestamp: new Date(validatorUpdateInstructionObj.creationDate),
      result: validatorUpdateInstructionObj.result
    }
  }

  async function submitValidatorUpdateInstruction(networkId, validatorUpdateInstruction) {
    const networkName = config.networkIdToNetworkName[networkId]
    if (config[networkName].type !== 'ethereum') {
      return Promise.reject(Error('Validator updates can only be done from Ethereum ledgers'))
    }
    const operationId = validatorUpdateInstruction.operationId
    logger.log('info', 'Received new validator update instruction from networkId [' + networkId + '] with operationId [' + operationId + ']')

    const state = validatorUpdateInstructionStates.confirmed
    const creationDate = Date.now()
    const lastUpdate = Date.now()
    const filters = []
    let numCallbackURLs = 0;
    if (!!validatorUpdateInstruction.filters) {
      for (let f=0; f<validatorUpdateInstruction.filters.length; f++) {
        let callbackURL = validatorUpdateInstruction.filters[f].callbackURL
        let networkId = validatorUpdateInstruction.filters[f].remoteDestinationNetworkId
        if (!!callbackURL) {
          // Since the proxy is going to inject the mTLS cert, we need to do a callout using only http
          if (callbackURL.startsWith('https') && config.performCallbackHttpsRewrite === true) {
            callbackURL = callbackURL.replace('https', 'http')
            logger.log('info', 'Adapted new validator update instruction from networkId [' + networkId + '] with callbackURL [' + callbackURL + '] to support mTLS')
          }
          numCallbackURLs++
        }
        filters.push({
          remoteDestinationNetworkId: networkId,
          callbackURL: callbackURL
        })
      }
    }
    let validatorUpdateInstructionObj = {
      networkName,
      networkId,
      operationId,
      filters,
      state,
      creationDate,
      lastUpdate
    }
    await dependencies.db.addValidatorUpdateInstruction(validatorUpdateInstructionObj)
    if (numCallbackURLs === 0 || numCallbackURLs !== filters.length) {
      while (validatorUpdateInstructionObj.state !== validatorUpdateInstructionStates.processed && validatorUpdateInstructionObj.state !== validatorUpdateInstructionStates.failed) {
        const timeElapsed = Date.now() - validatorUpdateInstructionObj.timestamp
        if (timeElapsed >= 5 * 60 * 1000) {
          logger.log('debug', 'Validator update instruction from networkId [' + validatorUpdateInstructionObj.networkId + '] for operationId [' + validatorUpdateInstructionObj.operationId + '] has timed out')
          return await handleValidatorUpdateInstructionTimeout(validatorUpdateInstructionObj)
        } else {
          validatorUpdateInstructionObj = await dependencies.db.findValidatorUpdateInstructionByOperationId(validatorUpdateInstructionObj.networkId, validatorUpdateInstructionObj.operationId)
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
        operationId: operationId,
        state: state
      }
    }
  }

  async function deleteValidatorUpdateInstruction(networkId, operationId) {
    let validatorUpdateInstructionObj
    if (!!networkId && !!operationId) {
      validatorUpdateInstructionObj = await dependencies.db.findValidatorUpdateInstructionByOperationId(networkId, operationId)
    }
    if (!validatorUpdateInstructionObj) {
      return Promise.reject(Error('Validator update instruction from networkId [' + networkId + '] with operationId [' + operationId + '] was not found'))
    }

    if (!!validatorUpdateInstructionObj.state) {
      if (validatorUpdateInstructionObj.state === validatorUpdateInstructionStates.confirmed
       || validatorUpdateInstructionObj.state === validatorUpdateInstructionStates.waitingForHold
       || validatorUpdateInstructionObj.state === validatorUpdateInstructionStates.failed) {
        await dependencies.db.removeValidatorUpdateInstruction(networkId, operationId)
      } else {
        return Promise.reject(Error('Validator update instruction from system [' + validatorUpdateInstructionObj.networkId + '] with state [' + validatorUpdateInstructionObj.state + '] cannot be deleted'))
      }
    }

    // Sanitise before sending back
    const response = {
      networkId: validatorUpdateInstructionObj.networkId,
      operationId: validatorUpdateInstructionObj.operationId,
      filters: validatorUpdateInstructionObj.filters,
      state: validatorUpdateInstructionObj.state,
      creationDate: validatorUpdateInstructionObj.creationDate,
      lastUpdate: validatorUpdateInstructionObj.lastUpdate,
      humanReadableTimestamp: new Date(validatorUpdateInstructionObj.creationDate)
    }
    return response
  }


  async function handleValidatorUpdateInstructionCallback(validatorUpdateInstructionObj) {
    for (let f=0; f < validatorUpdateInstructionObj.filters.length; f++) {
      let callbackURL = validatorUpdateInstructionObj.filters[f].callbackURL
      if (!!callbackURL) {
        try {
          const params = await adaptResponseForSource(validatorUpdateInstructionObj, validatorUpdateInstructionObj.filters[f].remoteDestinationNetworkId)
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
            logger.log('error', 'Unable to get content type from callback response for validator update instruction from networkId [' + validatorUpdateInstructionObj.networkId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']: Error: ' + err)
          }
          const callbackResponseText = await callbackResponse.text()
          logger.log('debug', 'Handled callback for validator update instruction from networkId [' + validatorUpdateInstructionObj.networkId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']: Response:\n' + callbackResponseText)
        } catch (error) {
          logger.log('error', Error(error))
        }
      } else {
        logger.log('debug', 'No callback url provided for validator update instruction from networkId [' + validatorUpdateInstructionObj.networkId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']')
      }
    }
  }

  async function adaptResponseForSource(validatorUpdateInstructionObj, remoteDestinationNetworkId) {
    for (let r=0; r<validatorUpdateInstructionObj.result.length; r++) {
      if (Number(validatorUpdateInstructionObj.result[r].networkId) === Number(remoteDestinationNetworkId)) {
        logger.log('debug', 'Returning validator set instruction result for operationId [' + validatorUpdateInstructionObj.operationId + '] ' + 'and remote destination chain with networkId [' + remoteDestinationNetworkId + ']: ' + JSON.stringify(validatorUpdateInstructionObj.result[r]))
        return validatorUpdateInstructionObj.result[r]
      }
    }
    logger.log('warn', 'No validator update instruction result was found for operationId [' + validatorUpdateInstructionObj.operationId + '] ' + 'and remote destination chain with networkId [' + remoteDestinationNetworkId + ']')
  }

  async function handleValidatorUpdateInstructionTimeout(validatorUpdateInstructionObj) {
    logger.log('debug', 'Setting state [' + validatorUpdateInstructionObj.state + '] to state [timedOut]');
    validatorUpdateInstructionObj.state = validatorUpdateInstructionStates.timedOut
    validatorUpdateInstructionObj.lastUpdate = Date.now()
    await dependencies.db.updateValidatorUpdateInstruction(validatorUpdateInstructionObj)
    for (let f=0; f < validatorUpdateInstructionObj.filters.length; f++) {
      let callbackURL = validatorUpdateInstructionObj.filters[f].callbackURL
      if (!!callbackURL) {
        try {
          const params = {
            error: 'Validator update instruction on networkId ' + validatorUpdateInstructionObj.networkId + ' with operationId: ' + validatorUpdateInstructionObj.operationId + ' has timed out'
          }
          const callbackResponse = await fetch(callbackURL, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(params)
          })
          const callbackResponseJson = await callbackResponse.json()
          logger.log('debug', 'Handled callback url for validator update instruction from networkId [' + validatorUpdateInstructionObj.networkId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']: Response:\n' + JSON.stringify(callbackResponseJson))
        } catch (error) {
          logger.log('error', Error(error))
        }
      } else {
        logger.log('debug', 'No callback url provided for validator update instruction from networkId [' + validatorUpdateInstructionObj.networkId + '] for operationId [' + validatorUpdateInstructionObj.operationId + ']')
      }
    }
  }

  async function transitionToWaitingForCrosschainFunctionCall(validatorUpdateInstructionObj) {
    try {
      logger.log('debug', 'Starting remote validator sync from networkId [' + validatorUpdateInstructionObj.networkId + ']');
      const sv = await validatorSetManagerContract.getValidatorsAndSyncRemotes(validatorUpdateInstructionObj.networkId)
      if (!!sv) {
        logger.log('debug', 'Setting state [' + validatorUpdateInstructionObj.state + '] to state [waitingForCrosschainFunctionCall]');
        validatorUpdateInstructionObj.blockNumber = sv.blockNumber
        validatorUpdateInstructionObj.processedAt = sv.processedAt
        validatorUpdateInstructionObj.state = validatorUpdateInstructionStates.waitingForCrosschainFunctionCall
      } else {
        return Promise.reject(Error('Transaction starting validator update from networkId [' + validatorUpdateInstructionObj.networkId + '] to update remote destination chains failed'))
      }
      validatorUpdateInstructionObj.lastUpdate = Date.now()
      await dependencies.db.updateValidatorUpdateInstruction(validatorUpdateInstructionObj)
    } catch (error) {
      return Promise.reject(Error(error))
    }
  }

  async function transitionToProcessed(validatorUpdateInstructionObj) {
    try {
      const networkName = validatorUpdateInstructionObj.networkName
      if (config[networkName].type === 'ethereum' && validatorUpdateInstructionObj.state === validatorUpdateInstructionStates.waitingForCrosschainFunctionCall) {
        logger.log('debug', 'Finding event to do validator update from networkId [' + validatorUpdateInstructionObj.networkId + '] with operationId [' + validatorUpdateInstructionObj.operationId + ']');
        const startBlock = validatorUpdateInstructionObj.processedAt
        let filters = new Map([['networkId', validatorUpdateInstructionObj.networkId],
                               ['blockNumber', validatorUpdateInstructionObj.blockNumber]]);
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
            return Promise.reject(Error('Destination system not found while processing validator update instruction from networkId [' + validatorUpdateInstructionObj.networkId + ']'))
          }
          if (validatorUpdateInstructionObj.filters.length > 0 && !validatorUpdateInstructionObj.filters.some(f => Number(f.remoteDestinationNetworkId) === Number(destinationNetworkId))) {
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
          if (executedEvent.decodedFunctionCallData.name === 'setValidatorList') { // First 4 bytes of keccak256 hash of 'setValidatorList(uint256,uint256,address[])'
            executedResults.push({
              networkId: destinationNetworkId,
              sourceNetworkId: config[networkName].id,
              encodedInfo: proofObj.encodedInfo,
              signatureOrProof: proofObj.encodedProof
            })
          }
        }
        validatorUpdateInstructionObj.result = executedResults
        await handleValidatorUpdateInstructionCallback(validatorUpdateInstructionObj)
        logger.log('debug', 'Setting state [' + validatorUpdateInstructionObj.state + '] to state [processed] on chain [' + networkName + ']');
        validatorUpdateInstructionObj.state = validatorUpdateInstructionStates.processed
        validatorUpdateInstructionObj.lastUpdate = Date.now()
        await dependencies.db.updateValidatorUpdateInstruction(validatorUpdateInstructionObj)
      }
    } catch (error) {
      return Promise.reject(Error(error))
    }
  }

  async function adaptResponse(validatorUpdateInstructionObj) {
    return validatorUpdateInstructionObj.result
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
    logger.log('debug', 'Setting validator update instruction state [' + item.state + '] to state [failed]');
    item.state = validatorUpdateInstructionStates.failed
    item.error = err
    item.lastUpdate = Date.now()
    await dependencies.db.updateValidatorUpdateInstruction(item)
  }

  let stopProcessing = false
  let hasStoppedProcessing = false

  async function start() {
    try {
      const validatorUpdateInstructionsToProcessList = await dependencies.db.findValidatorUpdateInstructionsToProcess()
      const processListPromises = []
      for (let item of validatorUpdateInstructionsToProcessList) {
        try {
          if (!item.state) {
            logger.log('error', Error('Unable to process validator update instruction due to null or undefined state [' + JSON.stringify(item, null, 2)) + ']')
          } else if (item.state === validatorUpdateInstructionStates.waitingForCrosschainFunctionCall) {
            processListPromises.push(transitionToProcessed(item).catch(async function (err) {	await handleError(item, err) }))
          } else if (item.state === validatorUpdateInstructionStates.confirmed) {
            processListPromises.push(transitionToWaitingForCrosschainFunctionCall(item).catch(async function (err) {	await handleError(item, err) }))
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
      logger.log('debug', 'Waiting for validator update instruction processor to stop');
    }
    return Promise.resolve(true)
  }

  return {
    getValidatorUpdateInstruction,
    submitValidatorUpdateInstruction,
    deleteValidatorUpdateInstruction,
    start,
    stop
  }
}

module.exports = init
