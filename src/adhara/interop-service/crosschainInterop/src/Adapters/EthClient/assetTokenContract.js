const AssetTokenJson = require('../../../build/contracts/IToken.json')

function init(config, dependencies){

  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function createHold(chainName, tradeId, fromAccountId, toAccountId, amount) {
    const functionName = 'createHold'
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.tokenAdmin
    const contractAddress = config[chainName].contracts.assetTokenContract.address
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
      chainName
    )

    if(result.status === true){
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

  async function makeHoldPerpetual(chainName, tradeId) {
    const functionName = 'makeHoldPerpetual'
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.tokenAdmin
    const contractAddress = config[chainName].contracts.assetTokenContract.address

    const result = await ethClient.buildAndSendTx(
      AssetTokenJson.abi,
      functionName,
      {
        operationId: tradeId.toString(),
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
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

  async function getAvailableBalanceOf(chainName, accountId) {
    const functionName = 'getAvailableBalanceOf'
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.assetTokenContract.address

    const result = await ethClient.buildAndCallTx(
      AssetTokenJson.abi,
      functionName,
      {
        account: accountId
      },
      fromAddress,
      contractAddress,
      chainName
    )
    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['uint256'], result)
    return abiDecoded[0]
  }

  async function findMakeHoldPerpetualExecutedEvent(chainName, startingBlock){

    const eventName = 'MakeHoldPerpetualExecuted'
    const contractAddress = config[chainName].contracts.assetTokenContract.address

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

  async function findExecuteHoldExecutedEvent(chainName, startingBlock) {

    const eventName = 'ExecuteHoldExecuted'
    const contractAddress = config[chainName].contracts.assetTokenContract.address

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

  async function findCancelHoldExecutedEvent(chainName, startingBlock) {

    const eventName = 'CancelHoldExecuted'
    const contractAddress = config[chainName].contracts.assetTokenContract.address

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
    await checkConfigForChain(chainName, 'getContractAddress')
    return config[chainName].contracts.assetTokenContract.address
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


