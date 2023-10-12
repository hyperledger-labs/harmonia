const CrosschainFunctionCallJson = require('../../../build/contracts/CrosschainFunctionCall.json')

function init(config, dependencies){

  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function setAppendAuthParams(systemId, enable){
    const functionName = 'setAppendAuthParams'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {enable},
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS'
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getAppendAuthParams(systemId){
    const functionName = 'getAppendAuthParams'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndCallTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {},
      fromAddress,
      contractAddress,
      chainName
    )
    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['bool'], result)
    return abiDecoded[0]
  }

  async function addAuthParams(systemId, foreignSystemId, foreignContractAddress) {
    const functionName = 'addAuthParams'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        foreignSystemId: Number(foreignSystemId),
        foreignContractAddress
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        foreignSystemId,
        foreignContractAddress
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function isAuthParams(systemId, foreignSystemId, foreignContractAddress) {
    const functionName = 'isAuthParams'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndCallTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        blockchainId: Number(foreignSystemId),
        contractAddress: foreignContractAddress
      },
      fromAddress,
      contractAddress,
      chainName
    )
    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['bool'], result)
    return {
      isAuthParams: abiDecoded[0]
    }
  }

  async function removeAuthParams(systemId, foreignSystemId, foreignContractAddress) {
    const functionName = 'removeAuthParams'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        blockchainId: Number(foreignSystemId),
        contractAddress: foreignContractAddress
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        foreignSystemId,
        foreignContractAddress
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function setSystemId(systemId) {
    const functionName = 'setSystemId'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        systemId
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        systemId,
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getSystemId(systemId) {
    const functionName = 'getSystemId'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndCallTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      { },
      fromAddress,
      contractAddress,
      chainName
    )
    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['uint256'], result)
    return abiDecoded[0]
  }

  async function performCallFromRemoteChain(chainName, blockchainId, eventSig, encodedInfo, signatureOrProof) {
    const functionName = 'performCallFromRemoteChain'
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        blockchainId: Number(blockchainId),
        eventSig,
        encodedInfo,
        signatureOrProof
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        blockchainId: Number(blockchainId),
        eventSig,
        encodedInfo,
        signatureOrProof
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function startValidatorUpdate(chainName, operationId, bvsBlockHeader, cvsContractAddress, destinationContract, destinationBlockchainId) {
    const functionName = 'startValidatorUpdate'
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        id: operationId,
        destinationBlockchainId:  Number(destinationBlockchainId),
        destinationContract: destinationContract,
        cvsContractAddress: cvsContractAddress,
        bvsBlockHeader: bvsBlockHeader ? Buffer.from(bvsBlockHeader.substring(2)).toString('hex') : Buffer.from('').toString('hex'),
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        id: operationId,
        destinationBlockchainId:  Number(destinationBlockchainId),
        destinationContract: destinationContract,
        cvsContractAddress: cvsContractAddress,
        bvsBlockHeader: bvsBlockHeader ? Buffer.from(bvsBlockHeader.substring(2)).toString('hex') : Buffer.from('').toString('hex'),
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function findCrossBlockchainCallExecutedEvent(chainName, startingBlock){

    const eventName = 'CrossBlockchainCallExecuted'
    const contractAddress = config[chainName].contracts.crosschainFunctionCall.address

    let eventABI = {}
    let eventSignature = eventName+'('
    for(let item of CrosschainFunctionCallJson.abi){
      if(item.name === eventName){
        eventABI = item
        for(let i in item.inputs){
          const input = item.inputs[i]
          eventSignature += input.type
          if(i < item.inputs.length -1){
            eventSignature += ','
          } else {
            eventSignature += ')'
          }
        }
      }
    }

    const web3 = web3Store[chainName]

    const filterTopics = [web3.utils.keccak256(eventSignature)]
    const eventLogs = await ethClient.getPastLogs(startingBlock, 'latest', contractAddress, filterTopics, chainName)

    const decodedEventLogs = []
    for(let log of eventLogs){
      const decodedLog = web3.eth.abi.decodeLog(eventABI.inputs, log.data)
      decodedEventLogs.push({
        decodedLog, //TODO: clean up the decoded log to only contain the named parameters?
        blockNumber: log.blockNumber,
        txHash: log.transactionHash,
        data: log.data,
        logIndex: log.logIndex
      })
    }
    return decodedEventLogs
  }

  async function checkConfigForChain(chainName, functionName){
    if (!config[chainName]) {
      return Promise.reject(Error('No configuration found for chain [' + chainName + '], unable to call [' + functionName + ']'))
    }
    if (config[chainName].type !== 'ethereum') {
      return Promise.reject(Error('Only possible on ethereum chains, unable to call [' + functionName + ']'))
    }
  }

  async function getContractAddress(systemId){
    const chainName = config.chainIdToChainName[systemId]
    return config[chainName].contracts.crosschainFunctionCall.address
  }

  return {
    setAppendAuthParams,
    getAppendAuthParams,
    getContractAddress,
    addAuthParams,
    isAuthParams,
    removeAuthParams,
    setSystemId,
    getSystemId,
    performCallFromRemoteChain,
    startValidatorUpdate,
    findCrossBlockchainCallExecutedEvent
  }
}

module.exports = init
