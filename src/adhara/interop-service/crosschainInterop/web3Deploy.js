const fs = require("fs");
const assert = require('assert');
const {v4: uuidv4} = require("uuid")
const AssetTokenJson = require('./build/contracts/Token.sol/Token.json')
const CrosschainXvPJson = require('./build/contracts/XvP.sol/XvP.json')
const CrosschainMessagingJson = require('./build/contracts/CrosschainMessaging.sol/CrosschainMessaging.json')
const CrosschainFunctionCallJson = require('./build/contracts/CrosschainFunctionCall.sol/CrosschainFunctionCall.json')
const InteropManagerJson = require('./build/contracts/InteropManager.sol/InteropManager.json')
const ValidatorSetManagerJson = require('./build/contracts/ValidatorSetManager.sol/ValidatorSetManager.json')
const Web3 = require('web3')
const Logger = require("./src/CrosschainSDKUtils/logger")
const Client = require('./src/Infrastructure/EthereumClient')
const fetch = require("node-fetch");

async function deployContracts(context, contracts, deployerAddress) {
  let nonce = await context.web3.eth.getTransactionCount(deployerAddress)
  for (let c in contracts) {
    console.log('Deploying contract [' + c + '] via provider [' + context.web3.currentProvider.host + ']')
    const contract = new context.web3.eth.Contract(contracts[c].abi);
    const contractDeploy = contract.deploy({
      data: contracts[c].bin,
      arguments: []
    });
    let gas = 10000000 + await contractDeploy.estimateGas();
    try {
       let result = await sendTransaction(context,{ // Will deploy the contract. The promise will resolve with the new contract instance, instead of the receipt!
         from: deployerAddress,
         nonce: '0x' + Number(nonce++).toString(16),
         gasPrice: '0x' + Number(0).toString(16),
         chainId: '0x' + Number(context.networkId).toString(16),
         gas: '0x' + Number(gas).toString(16),
         data: contractDeploy._deployData,
         value: '0x' + Number(0).toString(16)
       });
      contracts[c].address = result.contractAddress
    } catch (err) {
      throw(err)
    }
  }
  return contracts
}

async function signTransaction(context, txData) {
  const headers = {
    'User-Agent': 'Super Agent/0.0.1',
    'Content-Type': 'application/json-rpc',
    'Accept': 'application/json-rpc'
  }
  const url = context.signer
  try {
    let par = []
    par.push(txData)
    const submitResponse = await fetch(url, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ jsonrpc: '2.0', method: 'eth_signTransaction',	params: par, id: 1 })
    })
    let submitResult = await submitResponse.json();
    if (submitResult.error) {
      return Promise.reject(Error(submitResult.error))
    } else {
      return submitResult.result
    }
  } catch (err) {
    return Promise.reject(Error(err))
  }
}

async function sendTransaction(context, txData) {
  const signedTransaction = await signTransaction(context, txData)
  try {
    return await context.web3.eth.sendSignedTransaction(signedTransaction)
  } catch (error) {
    let errorMsg = 'An unknown error occurred while sending transaction'
    if (!!error.reason) {
      errorMsg = error.reason
    } else if (!!error.receipt && error.receipt.status === false) {
      let revertReasonHex = error.receipt.revertReason
      if (revertReasonHex) {
        errorMsg = 'Transaction reverted without reason'
        revertReasonHex = revertReasonHex.startsWith('0x') ? revertReasonHex : '0x' + revertReasonHex
        if (revertReasonHex.substr(138)) {
          errorMsg = context.web3.utils.hexToAscii(revertReasonHex.substr(138))
        }
      }
    } else {
      errorMsg = error.toString()
    }
    console.log(errorMsg)
    return Promise.reject(Error(errorMsg))
  }
}

async function invokeMethod(context, contract, senderAddress, method, args) {
  console.log('Invoking method [' + method + '] via provider [' + context.web3.currentProvider.host + ']')
  let nonce = await context.web3.eth.getTransactionCount(senderAddress)
  args = Object.keys(args).map(k => args[k])
  let params = {
    from: senderAddress,
    nonce: nonce
  };
  params.gas = 1000 + await contract.methods[method](...args).estimateGas();
  if (args.length > 0)
    await contract.methods[method](...args).send(params)
      .once('transactionHash', (txHash) => {
        console.log('Invoking method [' + method + '] hash [' + txHash + ']')
      })
      .on('error', (err) => {
        console.log('Invoking method [' + method + '] error [' + err.message() + ']')
      })
      .then((receipt) => {
        console.log('Invoking method [' + method + '] receipt [' + receipt.status + ']')
      });
  else
    await contract.methods[method]().send(params)
      .once('transactionHash', (txHash) => {
        console.log('Invoking method [' + method + '] hash [' + txHash + ']')
      })
      .on('error', (err) => {
        console.log('Invoking method [' + method + '] error [' + err.message() + ']')
      })
      .then((receipt) => {
        console.log('Invoking method [' + method + '] receipt [' + receipt.status + ']')
      });
}

async function callMethod(context, contract, senderAddress, method, args) {
  console.log('Calling method ['+ method + '] via provider [' + context.web3.currentProvider.host + ']')
  args = Object.keys(args).map(k => args[k])
  let params = {
    from: senderAddress,
  };
  if (args.length > 0)
    return await contract.methods[method](...args).call(params)
  else
    return await contract.methods[method]().call(params)
}

async function invoke(context, contract, senderAddress, method, args) {
  console.log('Invoking method ['+ method + '] via client')
  return await context.client.buildAndSendTx(
    contract.abi,
    method,
    args,
    senderAddress,
    contract.address,
    context.networkName
  )
}

async function call(context, contract, senderAddress, method, args) {
  console.log('Calling method ['+ method + '] via client')
  return await context.client.buildAndCallTx(
    contract.abi,
    method,
    args,
    senderAddress,
    contract.address,
    context.networkName
  )
}

async function createTokens(context, tokenContract, senderAddress, accountId, amount) {
  let args = {
    operationId: uuidv4().substring(0,8),
    toAccount: accountId,
    amount: amount,
    metaData: ''
  }
  await invoke(context, tokenContract, senderAddress, 'create', args)
}

async function createHold(context, tokenContract, senderAddress, fromAccountId, toAccountId, notaryId, amount) {
  let args = {
    operationId: uuidv4().substring(0,8),
    fromAccount: fromAccountId,
    toAccount: toAccountId,
    notaryId: notaryId,
    amount: amount,
    duration: '30',
    metaData: '',
  }
  await invoke(context, tokenContract, senderAddress, 'createHold', args)
}

async function setMessagingContractAddress(context, fcContract, senderAddress, contractAddress) {
  let args = {
    address: contractAddress
  }
  await invoke(context, fcContract, senderAddress, 'setMessagingContractAddress', args)
}

async function setFunctionCallContractAddress(context, xvpContract, senderAddress, contractAddress) {
  let args = {
    address: contractAddress
  }
  await invoke(context, xvpContract, senderAddress, 'setFunctionCallContractAddress', args)
}

async function setTokenContractAddress(context, xvpContract, senderAddress, contractAddress) {
  let args = {
    address: contractAddress
  }
  await invoke(context, xvpContract, senderAddress, 'setTokenContractAddress', args)
}

async function setAppendAuthParams(context, fcContract, senderAddress, enable) {
  let args = {
    enable: enable
  }
  await invoke(context, fcContract, senderAddress, 'setAppendAuthParams', args)
}

async function addAuthParams(context, fcContract, senderAddress, networkId, contractAddress) {
  let args = {
    networkId: Number(networkId),
    contractAddress: contractAddress,
  }
  await invoke(context, fcContract, senderAddress, 'addAuthParams', args)
}

async function setNotaryId(context, xvpContract, senderAddress, notaryId) {
  let args = {
    notaryId: notaryId
  }
  await invoke(context, xvpContract, senderAddress, 'setNotaryId', args)
}

async function setRemoteAccountIdToLocalAccountId(context, xvpContract, senderAddress, remoteAccountId, localAccountId) {
  let args = {
    localAccountId: localAccountId,
    remoteAccountId: remoteAccountId,
  }
  await invoke(context, xvpContract, senderAddress, 'setRemoteAccountIdToLocalAccountId', args)
}

async function getRemoteAccountIdToLocalAccountId(context, tokenContract, senderAddress, remoteAccountId) {
  let args = {
    remoteAccountId: remoteAccountId
  }
  let result = await call(context, tokenContract, senderAddress, 'getRemoteAccountIdToLocalAccountId', args)
  let decoded = context.web3.eth.abi.decodeParameters(['string'], result)
  return decoded[0]
}

async function onboardProvingScheme(context, msgContract, senderAddress, networkId, schemeId) {
  let args = {
    networkId: networkId,
    scheme: schemeId
  }
  await invoke(context, msgContract, senderAddress, 'onboardProvingScheme', args)
}

async function onboardEventDecodingScheme(context, fcContract, senderAddress, networkId, schemeId) {
  let args = {
    networkId: networkId,
    scheme: schemeId
  }
  await invoke(context, fcContract, senderAddress, 'onboardEventDecodingScheme', args)
}

async function setParameterHandlers(context, msgContract, senderAddress, networkId, functionSignature, functionPrototype, functionCommand, paramHandlers) {
  let args = {
    networkId: networkId,
    functionSignature: functionSignature,
    functionPrototype: functionPrototype,
    functionCommand: functionCommand,
    paramHandlers: paramHandlers
  }
  await invoke(context, msgContract, senderAddress, 'setParameterHandlers', args)
}

async function getParameterHandler(context, msgContract, senderAddress, networkId, functionSignature, index) {
  let args = {
    networkId: networkId,
    functionSignature: functionSignature,
    index: index
  }
  let result = await call(context, msgContract, senderAddress, 'getParameterHandler', args)
  const decoded = context.web3.eth.abi.decodeParameters(['bytes'], result);
  let handler = context.web3.eth.abi.decodeParameters([{
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
  }], decoded[0])
  return {
    'fingerprint': handler['0'].fingerprint,
    'componentIndex': handler['0'].componentIndex,
    'describedSize': handler['0'].describedSize,
    'describedType': handler['0'].describedType,
    'describedPath': handler['0'].describedPath,
    'solidityType': handler['0'].solidityType,
    'calldataPath': handler['0'].calldataPath,
    'parser': handler['0'].parser,
  }
}

async function getFunctionCommand(context, msgContract, senderAddress, networkId, functionSignature) {
  let args = {
    networkId: networkId,
    functionSignature: functionSignature,
  }
  let result = await call(context, msgContract, senderAddress, 'getFunctionCommand', args)
  let decoded = context.web3.eth.abi.decodeParameters(['string'], result)
  return decoded[0]
}

async function getFunctionPrototype(context, msgContract, senderAddress, networkId, functionSignature) {
  let args = {
    networkId: networkId,
    functionSignature: functionSignature,
  }
  let result = await call(context, msgContract, senderAddress, 'getFunctionPrototype', args)
  let decoded = context.web3.eth.abi.decodeParameters(['string'], result)
  return decoded[0]
}

async function addNotary(context, msgContract, senderAddress, networkId, publicKey) {
  let args = {
    networkId: networkId,
    publicKey: publicKey
  }
  await invoke(context, msgContract, senderAddress, 'addNotary', args)
}

async function addParticipant(context, msgContract, senderAddress, networkId, publicKey) {
  let args = {
    networkId: networkId,
    publicKey: publicKey
  }
  await invoke(context, msgContract, senderAddress, 'addParticipant', args)
}

async function setValidatorList(context, msgContract, senderAddress, networkId, validatorList) {
  let args = {
    networkId: networkId,
    blockNumber: 1,
    validatorList: validatorList
  }
  await invoke(context, msgContract, senderAddress, 'setValidatorList', args)
}

async function addHoldNotary(context, tokenContract, senderAddress, notaryId, holdNotaryAdminAddress) {
  let args = {
    notaryId: notaryId,
    holdNotaryAdminAddress: holdNotaryAdminAddress
  }
  await invoke(context, tokenContract, senderAddress, 'addHoldNotary', args)
}

async function getBalanceOf(context, tokenContract, senderAddress, accountId) {
  let args = {
    account: accountId
  }
  let result = await call(context, tokenContract, senderAddress, 'getAvailableBalanceOf', args)
  let decoded = context.web3.eth.abi.decodeParameters(['uint256'], result)
  return decoded[0]
}

async function getOwner(context, ownedContract, senderAddress) {
  let result = await call(context, ownedContract, senderAddress, 'owner', {})
  result = result.slice(26, 66)
  return '0x'+result
}

async function setValidators(context, vsmContract, senderAddress, validatorList) {
  let args = {
    validators: validatorList
  }
  await invoke(context, vsmContract, senderAddress, 'setValidators', args)
}

async function getValidators(context, vsmContract, senderAddress) {
  let args = {}
  let result = await call(context, vsmContract, senderAddress, 'getValidators', args)
  let decoded = context.web3.eth.abi.decodeParameters(['address[]'], result)
  return decoded[0]
}

async function setLocalNetworkId(context, imContract, senderAddress, networkId) {
  let args = {
    networkId: networkId
  }
  await invoke(context, imContract, senderAddress, 'setLocalNetworkId', args)
}

async function addRemoteDestinationNetwork(context, imContract, senderAddress, networkId, connectorContract) {
  let args = {
    remoteNetworkId: networkId,
    chainConnectorAddress: connectorContract
  }
  await invoke(context, imContract, senderAddress, 'addRemoteDestinationNetwork', args)
}

async function enableRemoteDestinationNetwork(context, imContract, senderAddress, networkId) {
  let args = {
    remoteNetworkId: networkId,
  }
  await invoke(context, imContract, senderAddress, 'enableRemoteDestinationNetwork', args)
}

async function listRemoteDestinationNetworks(context, imContract, senderAddress) {
  let args = {
    startIndex: 0,
    limit: 50
  }
  let result = await call(context, imContract, senderAddress, 'listRemoteDestinationNetworks', args)
  let decoded = context.web3.eth.abi.decodeParameters(['uint256[]'], result)
  return decoded[0]
}

async function setInteropManager(context, vsmContract, senderAddress, contractAddress) {
  let args = {
    address: contractAddress
  }
  await invoke(context, vsmContract, senderAddress, 'setInteropManager', args)
}

async function setupLedger(context, config) {
  let from = config.deployerAddress
  let contracts = {
    crosschainXvP: {
      abi: CrosschainXvPJson.abi,
      bin: CrosschainXvPJson.bytecode,
    },
    crosschainFunctionCall: {
      abi: CrosschainFunctionCallJson.abi,
      bin: CrosschainFunctionCallJson.bytecode,
    },
    crosschainMessaging: {
      abi: CrosschainMessagingJson.abi,
      bin: CrosschainMessagingJson.bytecode,
    },
    assetTokenContract: {
      abi: AssetTokenJson.abi,
      bin: AssetTokenJson.bytecode,
    }
  }
  contracts.interopManager = {
    abi: InteropManagerJson.abi,
    bin: InteropManagerJson.bytecode,
    address: '0x0000000000000000000000000000000000008888'
  }
  contracts.validatorSetManager = {
    abi: ValidatorSetManagerJson.abi,
    bin: ValidatorSetManagerJson.bytecode,
    address: '0x0000000000000000000000000000000000007777'
  }
  await deployContracts(context, contracts, from)
  return contracts
}

async function setupConfig(context, config, contracts) {
  const from = config.deployerAddress
  const msg = 'Only administrators can set up contract configuration'
  assert.strictEqual(await getOwner(context, contracts.crosschainFunctionCall, from), from.toLowerCase(), msg)
  assert.strictEqual(await getOwner(context, contracts.crosschainXvP, from), from.toLowerCase(), msg)
  assert.strictEqual(await getOwner(context, contracts.assetTokenContract, from), from.toLowerCase(), msg)

  await getValidators(context, contracts.validatorSetManager, from)
  await setLocalNetworkId(context, contracts.interopManager, from, config.localNetworkId) // Set the local network id
  await setLocalNetworkId(context, contracts.crosschainFunctionCall, from, config.localNetworkId) // Set the local system id
  await setAppendAuthParams(context, contracts.crosschainFunctionCall, from, true) // Enable contract authentication
  await setMessagingContractAddress(context, contracts.crosschainFunctionCall, from, contracts.crosschainMessaging.address)
  await setNotaryId(context, contracts.crosschainXvP, from, config.holdNotaryId)
  await setFunctionCallContractAddress(context, contracts.crosschainXvP, from, contracts.crosschainFunctionCall.address)
  await setTokenContractAddress(context, contracts.crosschainXvP, from, contracts.assetTokenContract.address)
  await addHoldNotary(context, contracts.assetTokenContract, from,  config.holdNotaryId, contracts.crosschainXvP.address)
  await createTokens(context, contracts.assetTokenContract, from, config.tokenAccount, config.tokenAmount)
  assert.strictEqual(await getBalanceOf(context, contracts.assetTokenContract, from, config.tokenAccount), config.tokenAmount, 'Balance does not match')
}

async function setupRemote(context, config, contracts) {
  let from = config.deployerAddress
  await setInteropManager(context, contracts.validatorSetManager, from, contracts.interopManager.address)
  for (let remoteNetwork of config.remoteNetworks) {
    if (remoteNetwork.cordaNetworkId !== undefined) {
      await addRemoteDestinationNetwork(context, contracts.interopManager, from, remoteNetwork.cordaNetworkId, remoteNetwork.connectorContract) // This is the messaging contract currently
      await enableRemoteDestinationNetwork(context, contracts.interopManager, from, remoteNetwork.cordaNetworkId) // Misuse of enable/disable of connector contract to enable/disable auth parameters
      await listRemoteDestinationNetworks(context, contracts.interopManager, from)
      await addAuthParams(context, contracts.crosschainFunctionCall, from, remoteNetwork.cordaNetworkId, contracts.crosschainXvP.address)
      await onboardEventDecodingScheme(context, contracts.crosschainFunctionCall, from, remoteNetwork.cordaNetworkId, 1) // Corda decoding scheme
      await onboardProvingScheme(context, contracts.crosschainMessaging, from, remoteNetwork.cordaNetworkId, 1) // Corda transaction-based proving scheme
      await addParticipant(context, contracts.crosschainMessaging, from, remoteNetwork.cordaNetworkId, remoteNetwork.cordaPartyAKey) // Corda issuing party
      await addParticipant(context, contracts.crosschainMessaging, from, remoteNetwork.cordaNetworkId, remoteNetwork.cordaPartyBKey) // Corda receiving party
      await addNotary(context, contracts.crosschainMessaging, from, remoteNetwork.cordaNetworkId, remoteNetwork.cordaNotaryKey) // Corda notary
      for (let h of remoteNetwork.cordaParameterHandlers) {
        await setParameterHandlers(context, contracts.crosschainMessaging, from, remoteNetwork.cordaNetworkId, h.signature, h.prototype, h.command, h.handlers)
        for (let i=0; i<h.handlers.length; i++) {
          await getParameterHandler(context, contracts.crosschainMessaging, from, remoteNetwork.cordaNetworkId, h.signature, i)
        }
        await getFunctionPrototype(context, contracts.crosschainMessaging, from, remoteNetwork.cordaNetworkId, h.signature)
        await getFunctionCommand(context, contracts.crosschainMessaging, from, remoteNetwork.cordaNetworkId, h.signature)
      }
      await setRemoteAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, remoteNetwork.cordaPartyARemoteId, remoteNetwork.cordaPartyALocalId) // Register Party A id mapping
      assert.strictEqual(await getRemoteAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, remoteNetwork.cordaPartyARemoteId), remoteNetwork.cordaPartyALocalId, 'Identity mapping failed')
      await setRemoteAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, remoteNetwork.cordaPartyBRemoteId, remoteNetwork.cordaPartyBLocalId) // Register Party B id mapping
      assert.strictEqual(await getRemoteAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, remoteNetwork.cordaPartyBRemoteId), remoteNetwork.cordaPartyBLocalId, 'Identity mapping failed')
    }
    if (remoteNetwork.ethNetworkId !== undefined) {
      await addRemoteDestinationNetwork(context, contracts.interopManager, from, remoteNetwork.ethNetworkId, remoteNetwork.connectorContract) // This is the messaging contract currently
      await enableRemoteDestinationNetwork(context, contracts.interopManager, from, remoteNetwork.ethNetworkId) // Misuse of enable/disable of connector contract to enable/disable auth parameters
      await listRemoteDestinationNetworks(context, contracts.interopManager, from)
      for (let i=0; i<remoteNetwork.authenticatedContracts.length; i++) {
        await addAuthParams(context, contracts.crosschainFunctionCall, from, remoteNetwork.ethNetworkId, remoteNetwork.authenticatedContracts[i])
      }
      await onboardEventDecodingScheme(context, contracts.crosschainFunctionCall, from, remoteNetwork.ethNetworkId, 2) // Block header decoding
      await onboardProvingScheme(context, contracts.crosschainMessaging, from, remoteNetwork.ethNetworkId, 2) // Block header proving scheme
      await setValidatorList(context, contracts.crosschainMessaging, from, remoteNetwork.ethNetworkId, remoteNetwork.ethValidatorAddresses) // Register validator list
      await setRemoteAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, remoteNetwork.ethPartyARemoteId, remoteNetwork.ethPartyALocalId) // Register Party A id mapping
      assert.strictEqual(await getRemoteAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, remoteNetwork.ethPartyARemoteId), remoteNetwork.ethPartyALocalId, 'Identity mapping failed')
      await setRemoteAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, remoteNetwork.ethPartyBRemoteId, remoteNetwork.ethPartyBLocalId) // Register Party B id mapping
      assert.strictEqual(await getRemoteAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, remoteNetwork.ethPartyBRemoteId), remoteNetwork.ethPartyBLocalId, 'Identity mapping failed')
    }
  }
}

async function createContext(config) {
  const logger = Logger(config, {})
  const context = {}
  context.networkName = config.networkName
  context.web3 = new Web3(config.ethProvider)
  context.web3.eth.handleRevert = true
  context.networkId = config.networkId
  context.signer = config.web3Provider
  const web3Store = {}
  web3Store[config.networkName] = context.web3
  const clientConfig = {}
  clientConfig[config.networkName] = {
    networkId: context.networkId,
    signerBaseURL: context.signer
  }
  context.client = Client(clientConfig, { logger, web3Store })
  return context
}

async function setupNetwork(config) {
  let context = await createContext(config)
  let contracts = await setupLedger(context, config)
  await setupConfig(context, config, contracts)
  return contracts
}

async function setupIntegration(config, deployed) {
  let context = await createContext(config)
  await setupRemote(context, config, deployed)
}

async function updateConfig(pathToConfig, networkId, deployed) {
  const config = require(pathToConfig)
  const systemName = config.networkIdToNetworkName[networkId]
  for (let key in deployed) {
    config[systemName].contracts[key].address = deployed[key].address
  }
  fs.writeFileSync(pathToConfig, JSON.stringify(config, null, 2))
  console.log('Updated system [' + systemName + '] config in ['+ pathToConfig +']')
}

module.exports = {
  setupNetwork,
  setupIntegration,
  updateConfig
};
