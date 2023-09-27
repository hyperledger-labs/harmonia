const fs = require("fs");
const assert = require('assert');
const {v4: uuidv4} = require("uuid")
const AssetTokenJson = require('./build/contracts/Token.json')
const CrosschainXvPJson = require('./build/contracts/XvP.json')
const CrosschainMessagingJson = require('./build/contracts/CrosschainMessaging.json')
const CrosschainFunctionCallJson = require('./build/contracts/CrosschainFunctionCall.json')
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
      // let result = await contractDeploy.send({ // Will deploy the contract. The promise will resolve with the new contract instance, instead of the receipt!
      //   from: deployerAddress,
      //   gas: gas,
      //   nonce: nonce++
      // });
       let result = await sendTransaction(context,{ // Will deploy the contract. The promise will resolve with the new contract instance, instead of the receipt!
         from: deployerAddress,
         nonce: '0x' + Number(nonce++).toString(16),
         gasPrice: '0x' + Number(0).toString(16),
         chainId: '0x' + Number(context.chainId).toString(16),
         gas: '0x' + Number(gas).toString(16),
         data: contractDeploy._deployData,
         value: '0x' + Number(0).toString(16)
       });
      //contracts[c].address = result._address
      contracts[c].address = result.contractAddress
      //contracts[c].contract = new context.web3.eth.Contract(contracts[c].abi, contracts[c].address, {from: deployerAddress});
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
    context.chainName
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
    context.chainName
  )
}

async function createTokens(context, tokenContract, senderAddress, accountId, amount) {
  let args = {
    operationId: uuidv4().substring(0,8),
    toAccount: accountId,
    amount: amount,
    metaData: ''
  }
  //await invokeMethod(context, tokenContract.contract, senderAddress, 'create', args)
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
  //await invokeMethod(context, tokenContract.contract, senderAddress, 'createHold', args)
  await invoke(context, tokenContract, senderAddress, 'createHold', args)
}

async function setMessagingContractAddress(context, fcContract, senderAddress, contractAddress) {
  let args = {
    address: contractAddress
  }
  //await invokeMethod(context, fcContract.contract, senderAddress, 'setMessagingContractAddress', args)
  await invoke(context, fcContract, senderAddress, 'setMessagingContractAddress', args)
}

async function setFunctionCallContractAddress(context, xvpContract, senderAddress, contractAddress) {
  let args = {
    address: contractAddress
  }
  //await invokeMethod(context, xvpContract.contract, senderAddress, 'setFunctionCallContractAddress', args)
  await invoke(context, xvpContract, senderAddress, 'setFunctionCallContractAddress', args)
}

async function setTokenContractAddress(context, xvpContract, senderAddress, contractAddress) {
  let args = {
    address: contractAddress
  }
  //await invokeMethod(context, xvpContract.contract, senderAddress, 'setTokenContractAddress', args)
  await invoke(context, xvpContract, senderAddress, 'setTokenContractAddress', args)
}

async function setAppendAuthParams(context, fcContract, senderAddress, enable) {
  let args = {
    enable: enable
  }
  //await invokeMethod(context, fcContract.contract, senderAddress, 'setAppendAuthParams', args)
  await invoke(context, fcContract, senderAddress, 'setAppendAuthParams', args)
}

async function addAuthParams(context, fcContract, senderAddress, systemId, contractAddress) {
  let args = {
    blockchainId: Number(systemId),
    contractAddress: contractAddress,
  }
  //await invokeMethod(context, fcContract.contract, senderAddress, 'addAuthParams', args)
  await invoke(context, fcContract, senderAddress, 'addAuthParams', args)
}

async function setSystemId(context, fcContract, senderAddress, systemId) {
  let args = {
    blockchainId: systemId
  }
  //await invokeMethod(context, fcContract.contract, senderAddress, 'setSystemId', args)
  await invoke(context, fcContract, senderAddress, 'setSystemId', args)
}

async function setNotaryId(context, xvpContract, senderAddress, notaryId) {
  let args = {
    notaryId: notaryId
  }
  //await invokeMethod(context, xvpContract.contract, senderAddress, 'setNotaryId', args)
  await invoke(context, xvpContract, senderAddress, 'setNotaryId', args)
}

async function setForeignAccountIdToLocalAccountId(context, xvpContract, senderAddress, foreignAccountId, localAccountId) {
  let args = {
    localAccountId: localAccountId,
    foreignAccountId: foreignAccountId,
  }
  //await invokeMethod(context, xvpContract.contract, senderAddress, 'setForeignAccountIdToLocalAccountId', args)
  await invoke(context, xvpContract, senderAddress, 'setForeignAccountIdToLocalAccountId', args)
}

async function getForeignAccountIdToLocalAccountId(context, tokenContract, senderAddress, foreignAccountId) {
  // console.log('Invoke asset token layer method [getAvailableBalanceOf] via provider [' + web3.currentProvider.host + ']')
  let args = {
    foreignAccountId: foreignAccountId
  }
  //return await callMethod(context, tokenContract.contract, senderAddress, 'getAvailableBalanceOf', args)
  let result = await call(context, tokenContract, senderAddress, 'getForeignAccountIdToLocalAccountId', args)
  let decoded = context.web3.eth.abi.decodeParameters(['string'], result)
  return decoded[0]
}

async function onboardProvingScheme(context, msgContract, senderAddress, chainId, schemeId) {
  let args = {
    chainId: chainId,
    scheme: schemeId
  }
  //await invokeMethod(context, msgContract.contract, senderAddress, 'onboardProvingScheme', args)
  await invoke(context, msgContract, senderAddress, 'onboardProvingScheme', args)
}

async function onboardEventDecodingScheme(context, fcContract, senderAddress, chainId, schemeId) {
  let args = {
    chainId: chainId,
    scheme: schemeId
  }
  //await invokeMethod(context, fcContract.contract, senderAddress, 'onboardEventDecodingScheme', args)
  await invoke(context, fcContract, senderAddress, 'onboardEventDecodingScheme', args)
}

async function setParameterHandlers(context, msgContract, senderAddress, chainId, functionSignature, paramHandlers) {
  let args = {
    chainId: chainId,
    functionSignature: functionSignature,
    paramHandlers: paramHandlers
  }
  //await invokeMethod(context, msgContract.contract, senderAddress, 'setParameterHandlers', args)
  await invoke(context, msgContract, senderAddress, 'setParameterHandlers', args)
}

async function getParameterHandler(context, msgContract, senderAddress, chainId, functionSignature, index) {
  let args = {
    chainId: chainId,
    functionSignature: functionSignature,
    index: index
  }
  //await invokeCall(context, msgContract.contract, senderAddress, 'getParameterHandler', args)
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
    'parser': handler['0'].parser,
  }
}

async function addNotary(context, msgContract, senderAddress, chainId, publicKey) {
  let args = {
    chainId: chainId,
    publicKey: publicKey
  }
  //await invokeMethod(context, msgContract.contract, senderAddress, 'addNotary', args)
  await invoke(context, msgContract, senderAddress, 'addNotary', args)
}

async function addParticipant(context, msgContract, senderAddress, chainId, publicKey) {
  let args = {
    chainId: chainId,
    publicKey: publicKey
  }
  //await invokeMethod(context, msgContract.contract, senderAddress, 'addParticipant', args)
  await invoke(context, msgContract, senderAddress, 'addParticipant', args)
}

async function setValidatorList(context, msgContract, senderAddress, chainId, validatorList) {
  let args = {
    chainId: chainId,
    operationId: uuidv4(),
    validatorList: validatorList
  }
  //await invokeMethod(context, msgContract.contract, senderAddress, 'setValidatorList', args)
  await invoke(context, msgContract, senderAddress, 'setValidatorList', args)
}

async function addHoldNotary(context, tokenContract, senderAddress, notaryId, holdNotaryAdminAddress) {
  let args = {
    notaryId: notaryId,
    holdNotaryAdminAddress: holdNotaryAdminAddress
  }
  //await invokeMethod(context, tokenContract.contract, senderAddress, 'addHoldNotary', args)
  await invoke(context, tokenContract, senderAddress, 'addHoldNotary', args)
}

async function getBalanceOf(context, tokenContract, senderAddress, accountId) {
  // console.log('Invoke asset token layer method [getAvailableBalanceOf] via provider [' + web3.currentProvider.host + ']')
  let args = {
    account: accountId
  }
  //return await callMethod(context, tokenContract.contract, senderAddress, 'getAvailableBalanceOf', args)
  let result = await call(context, tokenContract, senderAddress, 'getAvailableBalanceOf', args)
  let decoded = context.web3.eth.abi.decodeParameters(['uint256'], result)
  return decoded[0]
}

async function getOwner(context, ownedContract, senderAddress) {
  //return await callMethod(context, ownedContract.contract, senderAddress, 'owner', {})
  let result = await call(context, ownedContract, senderAddress, 'owner', {})
  result = result.slice(26, 66)
  return '0x'+result
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
  await deployContracts(context, contracts, from)
  return contracts
}

async function setupConfig(context, config, contracts) {
  const from = config.deployerAddress
  const msg = 'Only administrators can set up contract configuration'
  assert.strictEqual(await getOwner(context, contracts.crosschainFunctionCall, from), from.toLowerCase(), msg)
  assert.strictEqual(await getOwner(context, contracts.crosschainXvP, from), from.toLowerCase(), msg)
  assert.strictEqual(await getOwner(context, contracts.assetTokenContract, from), from.toLowerCase(), msg)

  await setSystemId(context, contracts.crosschainFunctionCall, from, config.localSystemId) // Set the local system id
  await setAppendAuthParams(context, contracts.crosschainFunctionCall, from, true) // Enable contract authentication
  await setMessagingContractAddress(context, contracts.crosschainFunctionCall, from, contracts.crosschainMessaging.address)
  await setNotaryId(context, contracts.crosschainXvP, from, config.holdNotaryId)
  await setFunctionCallContractAddress(context, contracts.crosschainXvP, from, contracts.crosschainFunctionCall.address)
  await setTokenContractAddress(context, contracts.crosschainXvP, from, contracts.assetTokenContract.address)
  await addHoldNotary(context, contracts.assetTokenContract, from,  config.holdNotaryId, contracts.crosschainXvP.address)
  await createTokens(context, contracts.assetTokenContract, from, config.tokenAccount, config.tokenAmount)
  assert.strictEqual(await getBalanceOf(context, contracts.assetTokenContract, from, config.tokenAccount), config.tokenAmount, 'Balance does not match')
}

async function setupForeign(context, config, contracts) {
  let from = config.deployerAddress
  for (let foreignSystem of config.foreignSystems) {
    if (foreignSystem.cordaSystemId !== undefined) {
      //const msg = 'Only administrators can set up contract configuration'
      //assert.strictEqual(await getOwner(context, contracts.crosschainMessaging, from), from.toLowerCase(), msg)
      //assert.strictEqual(await getOwner(context, contracts.crosschainFunctionCall, from), from.toLowerCase(), msg)
      //assert.strictEqual(await getOwner(context, contracts.crosschainXvP, from), from.toLowerCase(), msg)
      //assert.strictEqual(await getOwner(context, contracts.assetTokenContract, from), from.toLowerCase(), msg)
      await addAuthParams(context, contracts.crosschainFunctionCall, from, foreignSystem.cordaSystemId, contracts.crosschainXvP.address)
      await onboardEventDecodingScheme(context, contracts.crosschainFunctionCall, from, foreignSystem.cordaSystemId, 1) // Corda decoding scheme
      await onboardProvingScheme(context, contracts.crosschainMessaging, from, foreignSystem.cordaSystemId, 1) // Corda transaction-based proving scheme
      await addParticipant(context, contracts.crosschainMessaging, from, foreignSystem.cordaSystemId, foreignSystem.cordaPartyAKey) // Corda issuing party
      await addParticipant(context, contracts.crosschainMessaging, from, foreignSystem.cordaSystemId, foreignSystem.cordaPartyBKey) // Corda receiving party
      await addNotary(context, contracts.crosschainMessaging, from, foreignSystem.cordaSystemId, foreignSystem.cordaNotaryKey) // Corda notary
      for (let h of foreignSystem.cordaParameterHandlers) {
        await setParameterHandlers(context, contracts.crosschainMessaging, from, foreignSystem.cordaSystemId, h.signature, h.handlers)
        for (let i=0; i<h.handlers.length; i++) await getParameterHandler(context, contracts.crosschainMessaging, from, foreignSystem.cordaSystemId, h.signature, i)
      }
      await setForeignAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, foreignSystem.cordaPartyAForeignId, foreignSystem.cordaPartyALocalId) // Register Party A id mapping
      assert.strictEqual(await getForeignAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, foreignSystem.cordaPartyAForeignId), foreignSystem.cordaPartyALocalId, 'Identity mapping failed')
      await setForeignAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, foreignSystem.cordaPartyBForeignId, foreignSystem.cordaPartyBLocalId) // Register Party B id mapping
      assert.strictEqual(await getForeignAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, foreignSystem.cordaPartyBForeignId), foreignSystem.cordaPartyBLocalId, 'Identity mapping failed')
    }
    if (foreignSystem.ethSystemId !== undefined) {
      await addAuthParams(context, contracts.crosschainFunctionCall, from, foreignSystem.ethSystemId, foreignSystem.authenticatedContract)
      await onboardEventDecodingScheme(context, contracts.crosschainFunctionCall, from, foreignSystem.ethSystemId, 2) // Block header decoding
      await onboardProvingScheme(context, contracts.crosschainMessaging, from, foreignSystem.ethSystemId, 2) // Block header proving scheme
      await setValidatorList(context, contracts.crosschainMessaging, from, foreignSystem.ethSystemId, foreignSystem.ethValidatorAddresses) // Register validator list
      await setForeignAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, foreignSystem.ethPartyAForeignId, foreignSystem.ethPartyALocalId) // Register Party A id mapping
      assert.strictEqual(await getForeignAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, foreignSystem.ethPartyAForeignId), foreignSystem.ethPartyALocalId, 'Identity mapping failed')
      await setForeignAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, foreignSystem.ethPartyBForeignId, foreignSystem.ethPartyBLocalId) // Register Party B id mapping
      assert.strictEqual(await getForeignAccountIdToLocalAccountId(context, contracts.crosschainXvP, from, foreignSystem.ethPartyBForeignId), foreignSystem.ethPartyBLocalId, 'Identity mapping failed')
    }
  }
}

async function createContext(config) {
  const logger = Logger(config, {})
  const context = {}
  context.chainName = config.chainName
  context.web3 = new Web3(config.ethProvider)
  context.web3.eth.handleRevert = true
  context.chainId = config.chainId
  context.signer = config.web3Provider
  const web3Store = {}
  web3Store[config.chainName] = context.web3
  const clientConfig = {}
  clientConfig[config.chainName] = {
    chainId: context.chainId,
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
  await setupForeign(context, config, deployed)
}

async function updateConfig(pathToConfig, systemId, deployed) {
  const config = require(pathToConfig)
  const systemName = config.chainIdToChainName[systemId]
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


/*
 * Network setup configuration for integration with Corda.
 * @property {string} web3Provider Web3 provider to use. Example: http://localhost:4545
 * @property {string} deployerAddress Ethereum address from which to deploy contracts. Example: 0x049eb617fBa599E3D455Da70C6730ABc8Cc4221d
 * @property {string} cordaPartyAKey Corda issuing party public key. Example: 0x2247d1d382c4f4d7273742b0a8020bedd64c2f1f0bb908c0746706dd79682db3
 * @property {string} cordaPartyALocalId Corda issuing party local identification. Example: FNUSUS00GBP
 * @property {string} cordaPartyAForeignId Corda issuing party foreign identification encoded as base64. Example: Tz1QYXJ0eUEsIEw9TG9uZG9uLCBDPUdC
 * @property {string} cordaPartyBKey Corda receiving party public key. Example: 0xaffbc60356739f6a14aca6c394224af7dae8e443c97e8a10b5a630cfeb0072fd
 * @property {string} cordaPartyBLocalId Corda receiving party local identification. Example: FNGBGB00GBP
 * @property {string} cordaPartyBForeignId Corda receiving party identification encoded as base64. Example: Tz1QYXJ0eUIsIEw9TmV3IFlvcmssIEM9VVM=
 * @property {string} cordaNotaryKey Corda notary public key. Example: 0xc9b765141a1686f1344469443e22a9bd9a839755256788e7dcb0592cd56ec2c3
 * @property {number} cordaSystemId Corda ledger identification. Example: 3
 * @property {number} localSystemId Local Ethereum ledger identification. Example: 1
 * @property {string} holdNotaryId Ethereum hold notary identification. Example: NOTARY00XVP
 * @property {string} tokenAmount Amount of tokens to create for the Ethereum issuing party. Example: 100000000000
 */
