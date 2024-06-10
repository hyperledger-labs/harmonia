const CrosschainFunctionCallJson = require('../../../build/contracts/CrosschainFunctionCall.sol/CrosschainFunctionCall.json')

function init(config, dependencies){

  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function setAppendAuthParams(networkId, enable){
    const functionName = 'setAppendAuthParams'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {enable},
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS'
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getAppendAuthParams(networkId){
    const functionName = 'getAppendAuthParams'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndCallTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {},
      fromAddress,
      contractAddress,
      networkName
    )
    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['bool'], result)
    return abiDecoded[0]
  }

  async function addAuthParams(networkId, remoteNetworkId, remoteContractAddress) {
    const functionName = 'addAuthParams'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        networkId: Number(remoteNetworkId),
        contractAddress: remoteContractAddress
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        remoteNetworkId,
        remoteContractAddress
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function isAuthParams(networkId, remoteNetworkId, remoteContractAddress) {
    const functionName = 'isAuthParams'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndCallTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        networkId: Number(remoteNetworkId),
        contractAddress: remoteContractAddress
      },
      fromAddress,
      contractAddress,
      networkName
    )
    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['bool'], result)
    return {
      isAuthParams: abiDecoded[0]
    }
  }

  async function removeAuthParams(networkId, remoteNetworkId, remoteContractAddress) {
    const functionName = 'removeAuthParams'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        networkId: Number(remoteNetworkId),
        contractAddress: remoteContractAddress
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        remoteNetworkId,
        remoteContractAddress
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function setLocalNetworkId(networkId) {
    const functionName = 'setLocalNetworkId'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        networkId
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        networkId,
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getLocalNetworkId(networkId) {
    const functionName = 'getLocalNetworkId'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndCallTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      { },
      fromAddress,
      contractAddress,
      networkName
    )
    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['uint256'], result)
    return abiDecoded[0]
  }

  async function inboundCall(networkName, networkId, encodedInfo, signatureOrProof) {
    const functionName = 'inboundCall'
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

    const result = await ethClient.buildAndSendTx(
      CrosschainFunctionCallJson.abi,
      functionName,
      {
        networkId: Number(networkId),
        encodedInfo,
        signatureOrProof
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        processedAt: result.blockNumber,
        networkId: Number(networkId),
        encodedInfo,
        signatureOrProof
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function findCrosschainFunctionCallEvents(networkName, fromBlock, toBlock){
    const eventName = 'CrosschainFunctionCall'
    const contractAddress = config[networkName].contracts.crosschainFunctionCall.address

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

    const web3 = web3Store[networkName]

    const filterTopics = [web3.utils.keccak256(eventSignature)]
    const eventLogs = await ethClient.getPastLogs(fromBlock, toBlock, contractAddress, filterTopics, networkName)

    const decodedEventLogs = []
    for(let log of eventLogs){
      const decodedLog = web3.eth.abi.decodeLog(eventABI.inputs, log.data)
      decodedEventLogs.push({
        decodedLog, // TODO: clean up the decoded log to only contain the named parameters?
        blockNumber: log.blockNumber,
        txHash: log.transactionHash,
        data: log.data,
        logIndex: log.logIndex
      })
    }
    return decodedEventLogs
  }

  async function checkConfigForChain(networkName, functionName){
    if (!config[networkName]) {
      return Promise.reject(Error('No configuration found for chain [' + networkName + '], unable to call [' + functionName + ']'))
    }
    if (config[networkName].type !== 'ethereum') {
      return Promise.reject(Error('Only possible on ethereum chains, unable to call [' + functionName + ']'))
    }
  }

  async function getContractAddress(networkId){
    const networkName = config.networkIdToNetworkName[networkId]
    return config[networkName].contracts.crosschainFunctionCall.address
  }

  return {
    setAppendAuthParams,
    getAppendAuthParams,
    getContractAddress,
    addAuthParams,
    isAuthParams,
    removeAuthParams,
    setLocalNetworkId,
    getLocalNetworkId,
    inboundCall,
    findCrosschainFunctionCallEvents
  }
}

module.exports = init
