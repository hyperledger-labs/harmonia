const CrosschainMessagingJson = require('../../../build/contracts/CrosschainMessaging.sol/CrosschainMessaging.json')

function init(config, dependencies){

  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function setParameterHandlers(networkId, remoteNetworkId, functionSignature, functionPrototype, functionCommand, paramHandlers) {
    const functionName = 'setParameterHandlers'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        functionSignature,
        functionPrototype,
        functionCommand,
        paramHandlers
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        remoteNetworkId,
        functionSignature,
        paramHandlers
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function removeParameterHandlers(networkId, remoteNetworkId, functionSignature) {
    const functionName = 'removeParameterHandlers'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        functionSignature
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        remoteNetworkId,
        functionSignature
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getParameterHandler(networkId, remoteNetworkId, functionSignature, index) {
    const functionName = 'getParameterHandler'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address

    const result = await ethClient.buildAndCallTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        functionSignature,
        index
      },
      fromAddress,
      contractAddress,
      networkName
    )

    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['bytes'], result);

    const abiDecodedStruct = web3Store[networkName].eth.abi.decodeParameters([{
      'ParameterHandler': {
        'fingerprint': 'string',
        'componentIndex': 'uint8',
        'describedSize': 'uint8',
        'describedType': 'string',
        'describedPath': 'bytes',
        'solidityType': 'string',
        'calldataPath': 'bytes',
        'parser': 'string',
      },
    }], abiDecoded[0]);

    return {
      'fingerprint': abiDecodedStruct['0'].fingerprint,
      'componentIndex': abiDecodedStruct['0'].componentIndex,
      'describedSize': abiDecodedStruct['0'].describedSize,
      'describedType': abiDecodedStruct['0'].describedType,
      'describedPath': abiDecodedStruct['0'].describedPath,
      'solidityType': abiDecodedStruct['0'].solidityType,
      'calldataPath': abiDecodedStruct['0'].calldataPath,
      'parser': abiDecodedStruct['0'].parser,
    };
  }

  async function addParticipant(networkId, remoteNetworkId, publicKey) {
    const functionName = 'addParticipant'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address

    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        remoteNetworkId,
        publicKey
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function removeParticipant(networkId, remoteNetworkId, publicKey) {
    const functionName = 'removeParticipant'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address

    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        remoteNetworkId,
        publicKey
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function isParticipant(networkId, remoteNetworkId, publicKey) {
    const functionName = 'isParticipant'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address
    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndCallTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      networkName
    )
    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['bool'], result);

    return {
      isParticipant: abiDecoded[0]
    }
  }

  async function addNotary(networkId, remoteNetworkId, publicKey) {
    const functionName = 'addNotary'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address

    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        remoteNetworkId,
        publicKey
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function removeNotary(networkId, remoteNetworkId, publicKey) {
    const functionName = 'removeNotary'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address

    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      networkName
    )

    if (result.status === true) {
      return {
        transactionState: 'SUCCESS',
        remoteNetworkId,
        publicKey
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function isNotary(networkId, remoteNetworkId, publicKey) {
    const functionName = 'isNotary'
    const networkName = config.networkIdToNetworkName[networkId]
    await checkConfigForChain(networkName, functionName)

    const fromAddress = config[networkName].contexts.interopService
    const contractAddress = config[networkName].contracts.crosschainMessaging.address
    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndCallTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        networkId: remoteNetworkId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      networkName
    )
    const abiDecoded = web3Store[networkName].eth.abi.decodeParameters(['bool'], result);

    return {
      isNotary: abiDecoded[0]
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
    return config[networkName].contracts.crosschainMessaging.address
  }

  return {
    setParameterHandlers,
    removeParameterHandlers,
    getParameterHandler,
    getContractAddress,
    addParticipant,
    removeParticipant,
    isParticipant,
    addNotary,
    removeNotary,
    isNotary
  }
}

module.exports = init

