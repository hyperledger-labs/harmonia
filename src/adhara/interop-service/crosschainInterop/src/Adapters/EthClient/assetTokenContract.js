const AssetTokenJson = require('../../../build/contracts/Token.sol/Token.json')

function init(config, dependencies){

  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function createHold(networkName, tradeId, fromAccountId, toAccountId, amount) {
    const functionName = 'createHold'
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.tokenAdmin
    const contractAddress = config[networkName].contracts.assetTokenContract.address
    const notaryId = config.tradeDetails.notaryId

    const result = await ethClient.buildAndSendTx(
      AssetTokenJson.abi,
      functionName,
      {
        operationId: tradeId.toString(),
        fromAccount: fromAccountId,
        toAccount: toAccountId,
        notaryId: notaryId.toString(),
        amount: amount,
        duration: 1000,
        metaData: '',
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        tradeId,
        fromAccountId,
        toAccountId,
        notaryId,
        amount
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function makeHoldPerpetual(networkName, tradeId) {
    const functionName = 'makeHoldPerpetual'
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.tokenAdmin
    const contractAddress = config[networkName].contracts.assetTokenContract.address

    const result = await ethClient.buildAndSendTx(
      AssetTokenJson.abi,
      functionName,
      {
        operationId: tradeId.toString(),
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        tradeId
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getAvailableBalanceOf(networkName, accountId) {
    const functionName = 'getAvailableBalanceOf'
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.assetTokenContract.address

    const result = await ethClient.buildAndCallTx(
      AssetTokenJson.abi,
      functionName,
      {
        account: accountId
      },
      fromAddress,
      contractAddress,
      networkName
    )
    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['uint256'], result)
    return abiDecoded[0]
  }

  async function findMakeHoldPerpetualExecutedEvent(networkName, startingBlock){

    const eventName = 'MakeHoldPerpetualExecuted'
    const contractAddress = config[networkName].contracts.assetTokenContract.address

    let eventABI = {}
    let eventSignature = eventName+'('
    for(let item of AssetTokenJson.abi){
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
    const eventLogs = await ethClient.getPastLogs(startingBlock, 'latest', contractAddress, filterTopics, networkName)

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

  async function findExecuteHoldExecutedEvent(networkName, startingBlock) {

    const eventName = 'ExecuteHoldExecuted'
    const contractAddress = config[networkName].contracts.assetTokenContract.address

    let eventABI = {}
    let eventSignature = eventName+'('
    for(let item of AssetTokenJson.abi){
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
    const eventLogs = await ethClient.getPastLogs(startingBlock, 'latest', contractAddress, filterTopics, networkName)

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

  async function findCancelHoldExecutedEvent(networkName, startingBlock) {

    const eventName = 'CancelHoldExecuted'
    const contractAddress = config[networkName].contracts.assetTokenContract.address

    let eventABI = {}
    let eventSignature = eventName+'('
    for(let item of AssetTokenJson.abi){
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
    const eventLogs = await ethClient.getPastLogs(startingBlock, 'latest', contractAddress, filterTopics, networkName)

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
    await checkConfigForChain(networkName, 'getContractAddress')
    return config[networkName].contracts.assetTokenContract.address
  }

  return {
    createHold,
    makeHoldPerpetual,
    getContractAddress,
    getAvailableBalanceOf,
    findMakeHoldPerpetualExecutedEvent,
    findExecuteHoldExecutedEvent,
    findCancelHoldExecutedEvent,
  }
}

module.exports = init


