const path = require('path');
const fs = require('fs')
const readFile = path => fs.readFileSync(path, 'utf8');
const abiDecoder = require('abi-decoder')
const utils = require('../CrosschainSDKUtils');
const fetch = require('node-fetch');
const ethers = require("ethers");
const ethJsUtil = require("ethereumjs-util");

function init(config, dependencies) {

  const logger = dependencies.logger
  const crosschainFunctionCallContract = dependencies.crosschainFunctionCallContract
  const crosschainXVPContract = dependencies.crosschainXVPContract

  if (dependencies.CrosschainXvPJson) abiDecoder.addABI(dependencies.CrosschainXvPJson.abi)
  if (dependencies.CrosschainFunctionCallJson) abiDecoder.addABI(dependencies.CrosschainFunctionCallJson.abi)

  async function handleCordaEvent(fromNetwork, toNetwork, path) {
    const raw = JSON.parse(readFile(path).toString())
    let contractAddress = await crosschainXVPContract.getContractAddress(config[toNetwork].id)
    const decoderService = config[fromNetwork].decoderService
    const param = {
      networkId: '' + config[toNetwork].id,
      contractAddress: '' + contractAddress,
      encodedInfo: '' + raw.raw
    }
    let constructedProof = await postJson(decoderService, param);
    logger.log('silly', JSON.stringify(constructedProof, null, 2))
    let cordaProof = constructedProof.proof;
    console.log('From: ', config[fromNetwork].id)
    console.log('To: ', config[toNetwork].id)

    const result = await crosschainFunctionCallContract.inboundCall(toNetwork, config[fromNetwork].id, cordaProof.encodedInfo, cordaProof.signatureOrProof)
    logger.log('silly', JSON.stringify(result, null, 2))
  }

  async function handleEthEvent(fromNetwork, toNetwork, path, host) {
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
          networkId: config[fromNetwork].id,
          sourceNetworkId: ethProof.proof.networkId,
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
          networkId: config[fromNetwork].id,
          sourceNetworkId: ethProof.proof.networkId,
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

  async function monitorForCordaEvents(fromNetwork, toNetwork, path, handler) {
    logger.log('debug', 'Watching path: ' + path);
    const folders = [path];
    if (!handler) {
      handler = handleCordaEvent
    }
    await startCordaFolderWatchers(fromNetwork, toNetwork, folders, handler);
  }

  async function monitorForEthEvents(fromNetwork, toNetwork, path, host) {
    logger.log('debug', 'Watching path: ' + path);
    const folders = [path];
    await startEthFolderWatchers(fromNetwork, toNetwork, folders, host);
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

  async function startCordaFolderWatchers(fromNetwork, toNetwork, folders, handler) {
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
                handler(fromNetwork, toNetwork, fullPath);
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

  async function startEthFolderWatchers(fromNetwork, toNetwork, folders, host) {
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
                handleEthEvent(fromNetwork, toNetwork, fullPath, host);
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

  async function handleCrosschainFunctionCallEvent(web3, block, txHash, fromNetwork, toNetwork, context) {
    let proof = await dependencies.crosschainMessagingSDK.createProof(fromNetwork, block, txHash)
    // Submit the proof, block hash and event to the other chain
    try {
      // Packing items as per decodeAndVerify EEA spec
      let fromNetworkId = config[fromNetwork].id
      let toNetworkId = config[toNetwork].id
      const eventSig = web3.utils.soliditySha3('CrosschainFunctionCall(uint256,address,bytes)')
      const blockHeaderObj = dependencies.crosschainMessagingSDK.getBlockHeaderObjFromBlock(config[fromNetwork].consensus, block)
      let validatorSignatures = ethers.utils.RLP.decode(blockHeaderObj.rlpValidatorSignatures)
      const signatures = []
      for (let i = 0; i < validatorSignatures.length; i++) {
        let res = ethJsUtil.fromRpcSig(validatorSignatures[i])
        let publicKey = ethJsUtil.ecrecover(ethJsUtil.toBuffer(ethers.utils.keccak256(blockHeaderObj.rlpBlockHeaderPreimage)), res.v, res.r, res.s);
        let address = ethJsUtil.pubToAddress(publicKey).toString("hex");
        let signature = {
          by: '0x'+address,
          sigR: '0x'+ res.r.toString('hex'),
          sigS: '0x' + res.s.toString('hex'),
          sigV: '0x000000000000000000000000000000000000000000000000000000000000000'+(res.v-27),
          meta: '0x'
        }
        signatures.push(signature);
      }
      let index = -1
      const encodedReceipt = ethers.utils.RLP.decode(proof.rlpEncodedReceipt)
      for (let i = 0; i < encodedReceipt[3].length; i++) {
        const encodedLog = encodedReceipt[3][i];
        const topic = encodedLog[1];
        if (topic[0] === eventSig) {
          const eventData = ''+encodedLog[2]
          const eventParameters = web3.eth.abi.decodeParameters(['uint256', 'address', 'bytes'], eventData);
          const destinationNetworkId = Number(eventParameters['0'])
          if (toNetworkId === destinationNetworkId) {
            index = i;
            break;
          }
        }
      }
      if (index < 0) {
        throw Error('No remote function call events were found for destination network ['+toNetwork+']')
      }
      const eventData = web3.eth.abi.encodeParameters([ eventDataStruct ], [
        {
          index: '0x' + index.toString(16),
          signature: eventSig,
          logs: proof.rlpEncodedReceipt
        }
      ])
      const blockHeaderMeta = web3.eth.abi.encodeParameters([ blockHeaderMetaStruct ], [
        {
          rlpBlockHeader: blockHeaderObj.rlpBlockHeaderExcludingSeals,
          rlpBlockHeaderPreimage: blockHeaderObj.rlpBlockHeaderPreimage
        }
      ])
      const encodedInfo = web3.eth.abi.encodeParameters(['uint256', 'address', 'bytes'], [config[toNetwork].id, '0x0000000000000000000000000000000000000000', eventData])
      const encodedProof = web3.eth.abi.encodeParameters([ encodedProofStruct ], [
        {
          typ: 0,
          ProofData: {
            witnesses: proof.witness,
            root: block.receiptsRoot,
            blockHash: block.hash,
            blockHeaderMeta: blockHeaderMeta,
          },
          Signature: signatures
        }
      ])
      if (config[toNetwork].type !== 'ethereum') {
        logger.log('silly', 'Constructed proof for call from remote chain [' + fromNetwork + '] on chain [' + toNetwork + '] with context [' + JSON.stringify(context) + ']: ' + JSON.stringify({ networkId: fromNetworkId, eventSig, encodedInfo, encodedProof }, null, '\t'))
      } else {
        logger.log('info', 'Performing call from remote chain [' + fromNetwork + '] on chain [' + toNetwork + '] with context [' + JSON.stringify(context) + ']')
        const result = await crosschainFunctionCallContract.inboundCall(toNetwork, fromNetworkId, encodedInfo, encodedProof)
        logger.log('silly', 'Perform call from remote chain result: ' + result)
      }
      return Promise.resolve({
        toNetwork,
        networkId: fromNetworkId,
        encodedInfo,
        encodedProof
      })
    } catch (error) {
      logger.log('error', error)
      return Promise.reject(Error(error))
    }
  }
  return {
    monitorForEthEvents,
    monitorForCordaEvents,
    handleCrosschainFunctionCallEvent,
    inboundCall: crosschainFunctionCallContract.inboundCall,
  }
}

const eventDataStruct = {
  EventData: {
    index: 'uint256',
    signature: 'bytes32',
    logs: 'bytes',
  },
}

const blockHeaderMetaStruct = {
  BlockHeaderMeta: {
    rlpBlockHeader: 'bytes',
    rlpBlockHeaderPreimage: 'bytes'
  }
}

const encodedProofStruct = {
  Proof: {
    typ: 'uint256',
    ProofData: {
      witnesses: 'bytes',
      root: 'bytes32',
      blockHash: 'bytes32',
      blockHeaderMeta: 'bytes',
    },
    'Signature[]': {
      by: 'uint256',
      sigR: 'uint256',
      sigS: 'uint256',
      sigV: 'uint256',
      meta: 'bytes',
    }
  }
}

module.exports = init
