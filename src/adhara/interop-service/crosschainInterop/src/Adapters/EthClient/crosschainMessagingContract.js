const CrosschainMessagingJson = require('../../../build/contracts/CrosschainMessaging.json')

function init(config, dependencies){

  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function setParameterHandlers(systemId, foreignSystemId, functionSignature, paramHandlers) {
    const functionName = 'setParameterHandlers'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        functionSignature,
        paramHandlers
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        foreignSystemId,
        functionSignature,
        paramHandlers
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function removeParameterHandlers(systemId, foreignSystemId, functionSignature) {
    const functionName = 'removeParameterHandlers'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        functionSignature
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        foreignSystemId,
        functionSignature
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function getParameterHandler(systemId, foreignSystemId, functionSignature, index) {
    const functionName = 'getParameterHandler'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address

    const result = await ethClient.buildAndCallTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        functionSignature,
        index
      },
      fromAddress,
      contractAddress,
      chainName
    )

    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['bytes'], result);

    const abiDecodedStruct = web3Store[chainName].eth.abi.decodeParameters([{
      'ParameterHandler': {
        'fingerprint': 'string',
        'componentIndex': 'uint8',
        'describedSize': 'uint8',
        'describedType': 'string',
        'describedPath': 'bytes',
        'solidityType': 'string',
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
      'parser': abiDecodedStruct['0'].parser,
    };
  }

  async function addParticipant(systemId, foreignSystemId, publicKey) {
    const functionName = 'addParticipant'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address

    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        foreignSystemId,
        publicKey
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function removeParticipant(systemId, foreignSystemId, publicKey) {
    const functionName = 'removeParticipant'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address

    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        foreignSystemId,
        publicKey
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function isParticipant(systemId, foreignSystemId, publicKey) {
    const functionName = 'isParticipant'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address
    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndCallTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      chainName
    )
    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['bool'], result);

    return {
      isParticipant: abiDecoded[0]
    }
  }

  async function addNotary(systemId, foreignSystemId, publicKey) {
    const functionName = 'addNotary'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address

    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        foreignSystemId,
        publicKey
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function removeNotary(systemId, foreignSystemId, publicKey) {
    const functionName = 'removeNotary'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address

    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndSendTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      chainName
    )

    if(result.status === true){
      return {
        transactionState: 'SUCCESS',
        foreignSystemId,
        publicKey
      }
    } else {
      return {
        transactionState: 'FAILURE'
      }
    }
  }

  async function isNotary(systemId, foreignSystemId, publicKey) {
    const functionName = 'isNotary'
    const chainName = config.chainIdToChainName[systemId]
    await checkConfigForChain(chainName, functionName)

    const fromAddress = config[chainName].contexts.interopService
    const contractAddress = config[chainName].contracts.crosschainMessaging.address
    const decimalPublicKey = BigInt(publicKey).toString(10)

    const result = await ethClient.buildAndCallTx(
      CrosschainMessagingJson.abi,
      functionName,
      {
        chainId: foreignSystemId,
        publicKey: decimalPublicKey
      },
      fromAddress,
      contractAddress,
      chainName
    )
    const abiDecoded = web3Store[chainName].eth.abi.decodeParameters(['bool'], result);

    return {
      isNotary: abiDecoded[0]
    }
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
    return config[chainName].contracts.crosschainMessaging.address
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

