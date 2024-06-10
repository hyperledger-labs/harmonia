const ValidatorSetManagerJson = require('../../../build/contracts/ValidatorSetManager.sol/ValidatorSetManager.json')

function init(config, dependencies){

  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function setValidators(networkId, validators) {
    const functionName = 'setValidators'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService // Need a network administrator context
    const contractAddress = config[networkName].contracts.validatorSetManager.address

    const result = await ethClient.buildAndSendTx(
      ValidatorSetManagerJson.abi,
      functionName,
      {
        validators: validators,
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true){
      return {
        transactionState: 'SUCCESS',
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function setValidatorsAndSyncRemotes(networkId, validators) {
    const functionName = 'setValidatorsAndSyncRemotes'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService // Need a network administrator context
    const contractAddress = config[networkName].contracts.validatorSetManager.address

    const result = await ethClient.buildAndSendTx(
      ValidatorSetManagerJson.abi,
      functionName,
      {
        validators: validators,
      },
      fromAddress,
      contractAddress,
      networkName
    )
    if (result.status === true){
      return {
        transactionState: 'SUCCESS',
        blockNumber: result.blockNumber,
        processedAt: result.blockNumber
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getValidators(networkId) {
    const functionName = 'getValidators'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService // Need a network administrator context
    const contractAddress = config[networkName].contracts.validatorSetManager.address

    const result = await ethClient.buildAndCallTx(
      ValidatorSetManagerJson.abi,
      functionName,
      {},
      fromAddress,
      contractAddress,
      networkName
    )
    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['address[]'], result)
    return abiDecoded[0]
  }

  async function getLastUpdate(networkId) {
    const functionName = 'getLastUpdate'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.validatorSetManager.address

    const result = await ethClient.buildAndCallTx(
      ValidatorSetManagerJson.abi,
      functionName,
      {},
      fromAddress,
      contractAddress,
      networkName
    )
    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['uint256'], result)
    return abiDecoded[0]
  }

  async function getValidatorsAndSyncRemotes(networkId) {
    const functionName = 'getValidatorsAndSyncRemotes'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService // Need a network administrator context
    const contractAddress = config[networkName].contracts.validatorSetManager.address

    const result = await ethClient.buildAndSendTx(
      ValidatorSetManagerJson.abi,
      functionName,
      {},
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true){
      return {
        transactionState: 'SUCCESS',
        blockNumber: await getLastUpdate(networkId),
        processedAt: result.blockNumber
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
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
    return config[networkName].contracts.validatorSetManager.address
  }

  return {
    getContractAddress,
    getValidators,
    getValidatorsAndSyncRemotes,
    setValidators,
    setValidatorsAndSyncRemotes,
  }
}

module.exports = init


