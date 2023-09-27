const CrosschainXvPJson = require('../../../build/contracts/XvP.json')

function init(config, dependencies){

  const logger = dependencies.logger
  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function setForeignAccountIdToLocalAccountId(systemId, {localAccountId, foreignAccountId}){

    const functionName = 'setForeignAccountIdToLocalAccountId'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndSendTx(
      CrosschainXvPJson.abi,
      functionName,
      {localAccountId, foreignAccountId},
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        localAccountId,
        foreignAccountId
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getForeignAccountIdToLocalAccountId(systemId, {foreignAccountId}){

    const functionName = 'getForeignAccountIdToLocalAccountId'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    let _foreignAccountId = foreignAccountId
    if (_foreignAccountId.includes(" ")) {
      const foreignAccountIdBase64 = Buffer.from(_foreignAccountId).toString('base64')
      logger.log('debug', "Space detected in foreignAccountId: ["+_foreignAccountId+"], encoding to base 64 before performing lookup: "+foreignAccountIdBase64)
      _foreignAccountId = foreignAccountIdBase64
    }

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndCallTx(
      CrosschainXvPJson.abi,
      functionName,
      {_foreignAccountId},
      fromAddress,
      contractAddress,
      chainName
    )

    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['string'], result)

    // If there is not mapping set then default back to the foreignAccountId
    if (!!abiDecoded && !abiDecoded[0]) {
      return {
        localAccountId: foreignAccountId,
        foreignAccountId: foreignAccountId
      }
    }
    return {
      localAccountId: abiDecoded[0],
      foreignAccountId: foreignAccountId
    }
  }

  async function startLeadLeg(chainName, tradeId, fromAccountId, toAccountId, amount, counterpartyChainName) {

    const functionName = 'startLeadLeg'
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainXvP.address

    const tradeIdString = tradeId.toString()
    const sourceBlockchainId = config[chainName].id
    const destinationBlockchainId = config[counterpartyChainName].id
    const destinationContract = config[counterpartyChainName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndSendTx(
      CrosschainXvPJson.abi,
      functionName,
      [
        [
          tradeIdString,
          fromAccountId,
          toAccountId,
          amount
        ],
        sourceBlockchainId,
        destinationBlockchainId,
        destinationContract
      ],
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        tradeId: tradeId.toString(),
        sender: fromAccountId,
        receiver: toAccountId,
        amount: amount,
        sourceBlockchainId: config[chainName].id,
        destinationBlockchainId: config[counterpartyChainName].id,
        destinationContract: destinationContract
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function startCancellation(chainName, tradeId, sender, receiver, sourceBlockchainId, destinationBlockchainId, destinationContract) {
    const functionName = 'startCancellation'
    if (config[chainName].type !== 'ethereum') {
      let error = 'Invoking method [startCancellation] is only available on Ethereum ledgers';
      return Promise.reject(Error(error))
    }
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndSendTx(
      CrosschainXvPJson.abi,
      functionName,
      {
        tradeId: tradeId.toString(),
        sender: sender.toString(),
        receiver: receiver.toString(),
        sourceBlockchainId: sourceBlockchainId,
        destinationBlockchainId: destinationBlockchainId,
        destinationContract: destinationContract
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        tradeId: tradeId.toString(),
        sender: sender.toString(),
        receiver: receiver.toString(),
        sourceBlockchainId: sourceBlockchainId,
        destinationBlockchainId: destinationBlockchainId,
        destinationContract: destinationContract
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function performCancellation(chainName, tradeId, sender, receiver) {
    const functionName = 'performCancellation'
    if (config[chainName].type !== 'ethereum') {
      let error = 'Invoking method [performCancellation] is only available on Ethereum ledgers';
      return Promise.reject(Error(error))
    }
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndSendTx(
      CrosschainXvPJson.abi,
      functionName,
      {
        tradeId: tradeId.toString(),
        sender: sender.toString(),
        receiver: receiver.toString()
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        tradeId: tradeId.toString(),
        sender: sender.toString(),
        receiver: receiver.toString()
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getIsCancelled(chainName, operationId) {
    const functionName = 'getIsCancelled'
    if (config[chainName].type !== 'ethereum') {
      let error = 'Invoking method [getIsXvpCancelled] is only available on Ethereum ledgers';
      return Promise.reject(Error(error))
    }
    await checkConfigForChain(chainName, functionName)
    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndCallTx(
      CrosschainXvPJson.abi,
      functionName,
      {operationId},
      fromAddress,
      contractAddress,
      chainName
    )

    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['bool'], result)
    return abiDecoded[0]
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
    return config[chainName].contracts.crosschainXvP.address
  }

  return {
    setForeignAccountIdToLocalAccountId,
    getForeignAccountIdToLocalAccountId,
    startLeadLeg,
    startCancellation,
    performCancellation,
    getIsCancelled,
    getContractAddress
  }
}

module.exports = init
