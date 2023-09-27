const path = require('path');
const fs = require('fs')
const readFile = path => fs.readFileSync(path, 'utf8');
const abiDecoder = require('abi-decoder')
const utils = require('../CrosschainSDKUtils');
const fetch = require('node-fetch');

function init(config, dependencies) {

  const logger = dependencies.logger
  const crosschainFunctionCallContract = dependencies.crosschainFunctionCallContract
  const crosschainXVPContract = dependencies.crosschainXVPContract

  if (dependencies.CrosschainXvPJson) abiDecoder.addABI(dependencies.CrosschainXvPJson.abi)
  if (dependencies.CrosschainFunctionCallJson) abiDecoder.addABI(dependencies.CrosschainFunctionCallJson.abi)

  async function handleCordaEvent(fromChain, toChain, path) {
    const raw = JSON.parse(readFile(path).toString())
    let contractAddress = await crosschainXVPContract.getContractAddress(config[toChain].id)
    const decoderService = config[fromChain].decoderService
    const param = {
      blockchainId: '' + config[fromChain].id,
      contractAddress: '' + contractAddress,
      encodedInfo: '' + raw.raw
    }
    let constructedProof = await postJson(decoderService, param);
    logger.log('silly', JSON.stringify(constructedProof, null, 2))
    let cordaProof = constructedProof.proof;
    const result = await crosschainFunctionCallContract.performCallFromRemoteChain(toChain, cordaProof.blockchainId, cordaProof.eventSig, cordaProof.encodedInfo, cordaProof.signatureOrProof)
    logger.log('silly', JSON.stringify(result, null, 2))
  }

  async function handleEthEvent(fromChain, toChain, path, host) {
    let raw = readFile(path).toString();
    let ethProof = JSON.parse(raw);
    logger.log('silly', JSON.stringify(ethProof, null, 2))
    try {
      let buff = new Buffer.from((JSON.stringify(ethProof.proof)));
      let proof = '0x' + buff.toString('hex');
      logger.log('silly', 'Loading Ethereum transaction proof: \n' + JSON.stringify(proof, null, 2))
      let tradeId = ethProof.tradeId;
      let event = ethProof.event;
      if (event === 'completeLeadLeg') {
        const params = {
          tradeId: '' + tradeId,
          systemId: config[fromChain].id,
          sourceSystemId: ethProof.proof.blockchainId,
          eventSig: ethProof.proof.eventSig,
          encodedInfo: ethProof.proof.encodedInfo,
          signatureOrProof: ethProof.proof.signatureOrProof
        }
        postJson(host + '/confirm-dcr', params)
          .then(function (data) {
            logger.log('info', 'Confirming DCR transaction: Success');
            logger.log('debug', JSON.stringify(data, null, 2));
          })
          .catch(function (err) {
            logger.log('error', 'Confirming DCR transaction: Error: ' + Error(err));
          });
      } else if (event === 'performCancellation') {
        const params = {
          tradeId: '' + tradeId,
          systemId: config[fromChain].id,
          sourceSystemId: ethProof.proof.blockchainId,
          eventSig: ethProof.proof.eventSig,
          encodedInfo: ethProof.proof.encodedInfo,
          signatureOrProof: ethProof.proof.signatureOrProof
        }
        postJson(host + '/cancel-dcr', params)
          .then(function (data) {
            logger.log('info', 'Cancelling DCR transaction: Success');
            logger.log('debug', JSON.stringify(data, null, 2));
          })
          .catch(function (err) {
            logger.log('error', 'Cancelling DCR transaction: Error: ' + Error(err));
          });
      }
    } catch (err) {
      logger.log('error', Error(err))
    }
  }

  async function postJson(url = '', params = {}) {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(params)
    });
    return await response.json();
  }

  async function monitorForCordaEvents(fromChain, toChain, path, handler) {
    logger.log('debug', 'Watching path: ' + path);
    const folders = [path];
    if (!handler) {
      handler = handleCordaEvent
    }
    await startCordaFolderWatchers(fromChain, toChain, folders, handler);
  }

  async function monitorForEthEvents(fromChain, toChain, path, host) {
    logger.log('debug', 'Watching path: ' + path);
    const folders = [path];
    await startEthFolderWatchers(fromChain, toChain, folders, host);
  }

  function makeDirs(p) {
    p.split(path.sep)
      .reduce((prevPath, folder) => {
        const currentPath = path.join(prevPath, folder, path.sep);
        if (!fs.existsSync(currentPath)) {
          fs.mkdirSync(currentPath, { recursive: true });
        }
        return currentPath;
      }, '');
  }

  const cordaFolderWatchers = [];

  async function startCordaFolderWatchers(fromChain, toChain, folders, handler) {
    if (folders.length > 0) {
      cordaFolderWatchers.length = 0;
      folders.forEach(folder => {
        makeDirs(folder);
        logger.log('debug', `Relative path: '${path.relative(process.cwd(), folder)}/'`);
        cordaFolderWatchers.push({
          path: folder,
          watcher: fs.watch(folder, (eventType, filename) => {
            const fullPath = path.join(folder, filename);
            if (eventType === 'rename') {
              if (fs.existsSync(fullPath)) {
                utils.sleep(2000);
                logger.log('debug', 'Loading data from new file: ' + filename);
                handler(fromChain, toChain, fullPath);
              }
            } else if (eventType === 'change') {
            }
          })
        });
      });
    }
  }

  async function stopCordaFolderWatchers() {
    cordaFolderWatchers.forEach(item => item.watcher && item.watcher.close());
  }

  const ethFolderWatchers = [];

  async function startEthFolderWatchers(fromChain, toChain, folders, host) {
    if (folders.length > 0) {
      ethFolderWatchers.length = 0;
      folders.forEach(folder => {
        makeDirs(folder);
        logger.log('debug', `Relative path: '${path.relative(process.cwd(), folder)}/'`);
        ethFolderWatchers.push({
          path: folder,
          watcher: fs.watch(folder, (eventType, filename) => {
            const fullPath = path.join(folder, filename);
            if (eventType === 'rename') {
              if (fs.existsSync(fullPath)) {
                utils.sleep(2000);
                logger.log('debug', 'Loading data from new file: ' + filename);
                handleEthEvent(fromChain, toChain, fullPath, host);
              }
            } else if (eventType === 'change') {
            }
          })
        });
      });
    }
  }

  async function stopEthFolderWatchers() {
    ethFolderWatchers.forEach(item => item.watcher && item.watcher.close());
  }

  async function handleCrossBlockchainCallExecutedEvent(web3, block, txHash, fromChain, toChain, context) {

    const tradeId = context.tradeId
    const functionName = context.functionName
    const operationId = context.operationId

    let proof = await dependencies.crosschainMessagingSDK.createProof(fromChain, block, txHash)
    // submit the proof, block hash and event to the other chain
    try {
      // packing items as per decodeAndVerify EEA spec
      let blockchainId = config[fromChain].id
      let crosschainControlContract = '0x0000000000000000000000000000000000000000'
      const eventSig = web3.utils.soliditySha3('CrossBlockchainCallExecuted(uint256,address,bytes)')
      const encodedInfo = web3.eth.abi.encodeParameters(
        ['uint256', 'address', 'bytes32', 'bytes'],
        [blockchainId, crosschainControlContract, eventSig, proof.rlpEncodedReceipt]
      )
      const rlpSiblingNodes = proof.witness
      const blockHash = block.hash
      const receiptsRoot = block.receiptsRoot
      const blockHeaderObj = dependencies.crosschainMessagingSDK.getBlockHeaderObjFromBlock(config[fromChain].consensus, block)
      const rlpBlockHeaderExcludingSeals = blockHeaderObj.rlpBlockHeaderExcludingSeals
      const rlpBlockHeaderExcludingRound = blockHeaderObj.rlpBlockHeaderExcludingRound
      const rlpValidatorSignatures = blockHeaderObj.rlpValidatorSignatures
      const signatureOrProof = web3.eth.abi.encodeParameters(
        ['bytes', 'bytes32', 'bytes32', 'bytes', 'bytes', 'bytes'],
        [rlpSiblingNodes, receiptsRoot, blockHash, rlpBlockHeaderExcludingSeals, rlpBlockHeaderExcludingRound, rlpValidatorSignatures]
      )
      if (config[toChain].type !== 'ethereum') {
        logger.log('silly', 'Constructed proof for chain [' + toChain + ']: ' + JSON.stringify({ 'tradeId': context.tradeId, 'operationId': context.operationId, 'event': context.functionName, 'proof': { blockchainId, eventSig, encodedInfo, signatureOrProof } }, null, '\t'))
      } else {
        logger.log('info', 'Performing call from remote chain for ' + (!!tradeId ? 'tradeId [' + tradeId + ']' : !!operationId ? ', operationId [' + operationId + ']' : '') + ', source system [' + fromChain + '], destination system [' + toChain + '] with proof of functionName to call [' + functionName + ']')
        const result = await crosschainFunctionCallContract.performCallFromRemoteChain(toChain, blockchainId, eventSig, encodedInfo, signatureOrProof)
        logger.log('silly', 'Perform call from remote chain result: ' + result)
      }

      return Promise.resolve({
        toChain,
        blockchainId,
        eventSig,
        encodedInfo,
        signatureOrProof
      })
    } catch (error) {
      logger.log('error', error)
      return Promise.reject(Error(error))
    }
  }
  return {
    monitorForEthEvents,
    monitorForCordaEvents,
    handleCrossBlockchainCallExecutedEvent,
    performCallFromRemoteChain: crosschainFunctionCallContract.performCallFromRemoteChain,
    startValidatorUpdate: crosschainFunctionCallContract.startValidatorUpdate,
  }
}

module.exports = init
