const fetch = require('node-fetch');
const nonceMap = {}

function init(config, dependencies) {

  const logger = dependencies.logger
  const web3Store = dependencies.web3Store

  async function signTransaction(txData, networkName) {

    const headers = {
      'User-Agent': 'Super Agent/0.0.1',
      'Content-Type': 'application/json-rpc',
      'Accept': 'application/json-rpc'
    }

    const url = config[networkName].signerBaseURL
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
        logger.log('error', submitResult.error);
      } else {
        return submitResult.result
      }
    } catch (err) {
      logger.log('error', Error(err));
    }
  }

  async function signAndSendTx(txData, networkName, web3) {

    const signedTransaction = await signTransaction(txData, networkName)

    try {
      const txReceipt = await web3.eth.sendSignedTransaction(signedTransaction)
      return txReceipt
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
            errorMsg = web3.utils.hexToAscii(revertReasonHex.substr(138))
          }
        }
      } else {
        errorMsg = error.toString()
      }
      logger.log('error', Error(errorMsg))
      return Promise.reject(Error(errorMsg))
    }
  }

  async function buildAndSendTx(abi, functionName, args, fromAddress, contractAddress, networkName) {

    const web3 = web3Store[networkName]

    const nonce = await getNextNonce(fromAddress, web3, networkName)
    const functionCallData = buildFunctionCallData(abi, functionName, args, web3)

    let gasEstimation = 70000000
    try {
      const gasResult = await web3.eth.estimateGas({
        from: fromAddress,
        to: contractAddress,
        data: functionCallData
      })
      gasEstimation = Math.ceil((gasResult) * 1.1)
    } catch (error) {
      logger.log('warn', 'Unable to estimate gas for function call [' + functionName + ']: ' + JSON.stringify(error))
    }

    const txData = {
      from: fromAddress,
      to: contractAddress,
      nonce: '0x' + Number(nonce).toString(16),
      gasPrice: '0x' + Number(0).toString(16), // TODO: should this come from config rather?
      chainId: '0x' + Number(config[networkName].networkId).toString(16),
      gas: '0x' + Number(gasEstimation).toString(16),
      data: functionCallData,
      value: '0x' + Number(0).toString(16)
    }

    logger.log('debug', 'Signing and sending transaction [' + JSON.stringify(txData) + '] for function [' + functionName + '] to networkName [' + networkName + ']')

    const txReceipt = await signAndSendTx(txData, networkName, web3)
    logger.log('debug', 'Obtained receipt [' + JSON.stringify(txReceipt) + '] for function [' + functionName + ']')
    return txReceipt
  }

  async function buildAndCallTx(abi, functionName, args, fromAddress, contractAddress, networkName) {

    const web3 = web3Store[networkName]

    const functionCallData = buildFunctionCallData(abi, functionName, args, web3)
    const txData = {
      from: fromAddress,
      to: contractAddress,
      data: functionCallData
    }

    try {
      const txResult = await web3.eth.call(txData)
      return txResult
    } catch (error) {
      let errorMsg = 'An unknown error occurred while sending transaction'
      if (!!error.reason) {
        errorMsg = error.reason
      } else if (!!error.receipt && error.receipt.status === false) {
        let revertReasonHex = error.receipt.revertReason
        if (revertReasonHex) {
          errorMsg = 'Call reverted without reason'
          revertReasonHex = revertReasonHex.startsWith('0x') ? revertReasonHex : '0x' + revertReasonHex
          if (revertReasonHex.substr(138)) {
            errorMsg = web3.utils.hexToAscii(revertReasonHex.substr(138))
          }
        }
      } else {
        errorMsg = error.toString()
      }
      logger.log('error', Error(errorMsg))
      return Promise.reject(Error(errorMsg))
    }
  }

  async function getNextNonce(address, web3, networkName) {
    if (!!nonceMap[address] && !!nonceMap[address][networkName]) {
      const nonce = nonceMap[address][networkName]
      if (nonce === -1) { // Nonce is busy being fetched
        await sleep(100)
        return await getNextNonce(address, web3, networkName)
      }

      nonceMap[address][networkName]++
      return nonce
    } else {
      try {
        if (!nonceMap[address]) {
          nonceMap[address] = {}
          nonceMap[address][networkName] = -1
        } else if (!nonceMap[address][networkName]) {
          nonceMap[address][networkName] = -1
        }

        const count = await web3.eth.getTransactionCount(address)
        nonceMap[address][networkName] = count + 1
        return count
      } catch (e) {
        logger.log('error', Error(e));
      }
    }
  }

  function buildFunctionCallData(abi, functionName, args, web3) {
    try {
      const jsonInterface = getFunctionJSONInterface(abi, functionName)
      let functionCallArgs
      if (Array.isArray(args)) {
        functionCallArgs = args
      } else if (args instanceof Object) {
        functionCallArgs = Object.keys(args).map(k => args[k])
      } else {
        const e = Error('Unsupported format for function call arguments, please format correctly')
        logger.log('error', e)
        return Promise.reject(e)
      }
      const functionCallData = web3.eth.abi.encodeFunctionCall(jsonInterface, functionCallArgs)
      return functionCallData
    } catch (error) {
      logger.log('error', 'Unable to buildFunctionCallData: ', error)
      return Promise.reject(Error(error))
    }
  }

  function getFunctionJSONInterface(abi, functionName) {
    for (let jsonInterface of abi) {
      if (jsonInterface.name === functionName) {
        return {
          name: jsonInterface.name,
          type: jsonInterface.type,
          inputs: jsonInterface.inputs
        }
      }
    }
  }

  async function getPastLogs(fromBlock, toBlock, address, topics, networkName){
    const web3 = web3Store[networkName]
    const pastLogs = await web3.eth.getPastLogs({fromBlock, toBlock, address, topics})
    return pastLogs
  }

  function sleep(ms) {
    return new Promise((resolve) => {
      setTimeout(resolve, ms);
    });
  }

  return {
    buildAndSendTx,
    buildAndCallTx,
    getPastLogs
  }
}

module.exports = init
