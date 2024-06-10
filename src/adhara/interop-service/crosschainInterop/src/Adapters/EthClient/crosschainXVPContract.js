const CrosschainXvPJson = require('../../../build/contracts/XvP.sol/XvP.json')

function init(config, dependencies){

  const logger = dependencies.logger
  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function setRemoteAccountIdToLocalAccountId(networkId, {localAccountId, remoteAccountId}){

    const functionName = 'setRemoteAccountIdToLocalAccountId'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndSendTx(
      CrosschainXvPJson.abi,
      functionName,
      {
        localAccountId,
        remoteAccountId
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        localAccountId,
        remoteAccountId
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getRemoteAccountIdToLocalAccountId(networkId, {remoteAccountId}){

    const functionName = 'getRemoteAccountIdToLocalAccountId'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    let remoteId = remoteAccountId
    if (remoteId.includes(" ")) {
      const remoteAccountIdBase64 = Buffer.from(remoteId).toString('base64')
      logger.log('debug', "Space detected in remoteAccountId: ["+remoteId+"], encoding to base 64 before performing lookup: "+remoteAccountIdBase64)
      remoteId = remoteAccountIdBase64
    }
    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndCallTx(
      CrosschainXvPJson.abi,
      functionName,
      {
        remoteAccountId: remoteId
      },
      fromAddress,
      contractAddress,
      networkName
    )

    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['string'], result)

    // If there is not mapping set then default back to the remoteAccountId
    if (!!abiDecoded && !abiDecoded[0]) {
      return {
        localAccountId: remoteAccountId,
        remoteAccountId: remoteAccountId
      }
    }
    return {
      localAccountId: abiDecoded[0],
      remoteAccountId: remoteAccountId
    }
  }

  async function startLeadLeg(networkName, tradeId, fromAccountId, toAccountId, amount, counterpartyNetworkName) {

    const functionName = 'startLeadLeg'
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainXvP.address

    const tradeIdString = tradeId.toString()
    const sourceNetworkId = config[networkName].id
    const destinationNetworkId = config[counterpartyNetworkName].id
    const destinationContract = config[counterpartyNetworkName].contracts.crosschainXvP.address

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
        sourceNetworkId,
        destinationNetworkId,
        destinationContract
      ],
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true){
      return {
        transactionState: 'SUCCESS',
        processedAt: result.blockNumber,
        tradeId: tradeId.toString(),
        sender: fromAccountId,
        receiver: toAccountId,
        amount: amount,
        sourceNetworkId: config[networkName].id,
        destinationNetworkId: config[counterpartyNetworkName].id,
        destinationContract: destinationContract
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function startCancellation(networkName, tradeId, sender, receiver, sourceNetworkId, destinationNetworkId, destinationContract) {
    const functionName = 'startCancellation'
    if (config[networkName].type !== 'ethereum') {
      let error = 'Invoking method [startCancellation] is only available on Ethereum ledgers';
      return Promise.reject(Error(error))
    }
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndSendTx(
      CrosschainXvPJson.abi,
      functionName,
      {
        tradeId: tradeId.toString(),
        sender: sender.toString(),
        receiver: receiver.toString(),
        sourceNetworkId: sourceNetworkId,
        destinationNetworkId: destinationNetworkId,
        destinationContract: destinationContract
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true){
      return {
        transactionState: 'SUCCESS',
        processedAt: result.blockNumber,
        tradeId: tradeId.toString(),
        sender: sender.toString(),
        receiver: receiver.toString(),
        sourceNetworkId: sourceNetworkId,
        destinationNetworkId: destinationNetworkId,
        destinationContract: destinationContract
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function performCancellation(networkName, tradeId, sender, receiver) {
    const functionName = 'performCancellation'
    if (config[networkName].type !== 'ethereum') {
      let error = 'Invoking method [performCancellation] is only available on Ethereum ledgers';
      return Promise.reject(Error(error))
    }
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainXvP.address

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
      networkName
    )

    if (result.status === true) {
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

  async function getIsCancelled(networkName, operationId) {
    const functionName = 'getIsCancelled'
    if (config[networkName].type !== 'ethereum') {
      let error = 'Invoking method [getIsXvpCancelled] is only available on Ethereum ledgers';
      return Promise.reject(Error(error))
    }
    await checkConfigForChain(networkName, functionName)
    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainXvP.address

    const result = await ethClient.buildAndCallTx(
      CrosschainXvPJson.abi,
      functionName,
      {
        operationId: operationId
      },
      fromAddress,
      contractAddress,
      networkName
    )

    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['bool'], result)
    return abiDecoded[0]
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
    return config[networkName].contracts.crosschainXvP.address
  }

  return {
    setRemoteAccountIdToLocalAccountId,
    getRemoteAccountIdToLocalAccountId,
    startLeadLeg,
    startCancellation,
    performCancellation,
    getIsCancelled,
    getContractAddress
  }
}

module.exports = init
