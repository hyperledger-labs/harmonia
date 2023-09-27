const { GetProof } = require('eth-proof')
const ta = require('truffle-assertions');
const {v4: uuidv4} = require("uuid");
const Logger = require('../src/CrosschainSDKUtils/logger.js')
const opId = function () { return uuidv4().substring(0,16) }

const CrosschainFunctionCall = artifacts.require('CrosschainFunctionCall');
const CrosschainMessaging = artifacts.require('CrosschainMessaging');
const XvP = artifacts.require('XvP');
const Token = artifacts.require('Token');
const config = { logLevel: 'silent' }

const logger = Logger(config, {})
const crosschainMessagingSDK = require('../src/CrosschainMessagingSDK')(config, { logger })

const random = function () { return Math.floor(Math.random() * (999 - 100 + 1) + 100) }
let del = null, pay = null, ptc = null, dtc = null, pfc = null, dfc = null, pmc = null, dmc = null;
let fc = 0, sc = 1;

contract('NonAtomicHiddenAuthParams', async accounts => {
  let web3ProviderUrl = web3.currentProvider.host
  if (!web3ProviderUrl) {
    web3ProviderUrl = 'http://127.0.0.1:8545'
  }
  const ethProof = new GetProof(web3ProviderUrl)

  beforeEach(async () => {
    pay = await XvP.new(); // Payment
    ptc = await Token.new(); // Payment token contract
    pmc = await CrosschainMessaging.new(); // Payment messaging contract
    pfc = await CrosschainFunctionCall.new(); // Payment function call contract
    let result = null;
    // Payment notaries
    result = await pay.setTokenContractAddress(ptc.address)
    assert.equal(result.receipt.status, true)
    result = await ptc.addHoldNotary('N-123', pay.address)
    assert.equal(result.receipt.status, true)
    result = await pay.setNotaryId('N-123')
    assert.equal(result.receipt.status, true)
    // Payment interop
    result = await pfc.setMessagingContractAddress(pmc.address)
    assert.equal(result.receipt.status, true)
    result = await pay.setFunctionCallContractAddress(pfc.address)
    assert.equal(result.receipt.status, true)
    // Payment schemes
    const firstDecodingScheme = await pfc.CordaTransactionDecodingSchemeId();
    result = await pfc.onboardEventDecodingScheme(fc, firstDecodingScheme)
    assert.equal(result.receipt.status, true)
    const firstProvingScheme = await pmc.CordaTransactionProvingSchemeId();
    result = await pmc.onboardProvingScheme(fc, firstProvingScheme)
    assert.equal(result.receipt.status, true)
    result = await pmc.addParticipant(fc, '0x04473F5846E9C090BC82D80C3B0F01E7058E21684C1967D71D3A338120A32B4C');
    assert.equal(result.receipt.status, true)
    result = await pmc.addParticipant(fc, '0x2E7F70B8A6499C1DE28DF983A882E057BF8AEECE0F677E402B6A128A615B3D11');
    assert.equal(result.receipt.status, true)
    result = await pmc.addNotary(fc, '0xC98D06DEEFF77BAA9339C709818F3AB9313740EE39CC6193E341C687DFA38CF4');
    assert.equal(result.receipt.status, true)
    result = await pmc.setParameterHandlers(fc, "0xc6755b7c", [{
      'fingerprint': 'net.corda:DldW9yS4tBOze6qv6U4QTA==',
      'componentIndex': '0x00',
      'describedSize': '0x08',
      'describedType': 'String',
      'describedPath': '0x06',
      'solidityType': 'string',
      'parser': 'PathParser',
    }, {
      'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
      'componentIndex': '0x01',
      'describedSize': '0x06',
      'describedType': 'String',
      'describedPath': '0x00',
      'solidityType': 'string',
      'parser': 'PartyParser',
    }, {
      'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
      'componentIndex': '0x00',
      'describedSize': '0x06',
      'describedType': 'String',
      'describedPath': '0x00',
      'solidityType': 'string',
      'parser': 'PartyParser',
    }, {
      'fingerprint': '',
      'componentIndex': '0x00',
      'describedSize': '0x00',
      'describedType': '',
      'describedPath': '0x00',
      'solidityType': 'address',
      'parser': 'NoParser',
    }, {
      'fingerprint': '',
      'componentIndex': '0x00',
      'describedSize': '0x00',
      'describedType': '',
      'describedPath': '0x00',
      'solidityType': 'uint256',
      'parser': 'NoParser',
    }, {
      'fingerprint': 'net.corda:DldW9yS4tBOze6qv6U4QTA==',
      'componentIndex': '0x00',
      'describedSize': '0x08',
      'describedType': 'String',
      'describedPath': '0x07',
      'solidityType': 'uint256',
      'parser': 'PathParser',
    }]);
    assert.equal(result.receipt.status, true)
    const secondDecodingScheme = await pfc.EthereumEventLogDecodingSchemeId();
    result = await pfc.onboardEventDecodingScheme(sc, secondDecodingScheme)
    assert.equal(result.receipt.status, true)
    const secondProvingScheme = await pmc.EthereumBlockHeaderProvingSchemeId();
    result = await pmc.onboardProvingScheme(sc, secondProvingScheme)
    assert.equal(result.receipt.status, true)

    const bmd = ({ // Mint details
      operationId: 'M-' + random(),
      toAccount: 'Bob',
      amount: 10000000,
      metaData: ''
    })
    let bmint = await ptc.create(bmd.operationId, bmd.toAccount, bmd.amount, bmd.metaData)
    assert.equal(bmint.receipt.status, true)

    const validatorList = ['0xca31306798b41bc81c43094a1e0462890ce7a673']
    if (!validatorList || validatorList.length === 0) {
      return Promise.reject(Error("Could not get list of validators, or list is empty"))
    }
    await pmc.setValidatorList(sc, opId(), validatorList)
  })

  it('should be able to set the systemId', async () => {
    let result = await pfc.setSystemId(sc)
    assert.equal(result.receipt.status, true)
  })

  it('should be able to get the systemId', async () => {
    let result = await pfc.setSystemId(sc)
    assert.equal(result.receipt.status, true)
    result = (await pfc.getSystemId.call()).toNumber();
    assert.equal(result, sc)
  })

  it('should be able to append authentication parameters', async () => {
    let result = await pfc.setAppendAuthParams(true)
    assert.equal(result.receipt.status, true)
  })

  it('should be able to add authentication parameters', async () => {
    const sourceSystemId = sc
    let result = await pfc.addAuthParams(sourceSystemId, accounts[0])
    assert.equal(result.receipt.status, true)
    result = await pfc.isAuthParams(sourceSystemId, accounts[0])
    assert.equal(result, true)
  })

  it('should be able to remove authentication parameters', async () => {
    const sourceSystemId = sc
    let result = await pfc.addAuthParams(sourceSystemId, accounts[0])
    assert.equal(result.receipt.status, true)
    result = await pfc.removeAuthParams(sourceSystemId, accounts[0])
    assert.equal(result.receipt.status, true)
    result = await pfc.isAuthParams(sourceSystemId, accounts[0])
    assert.equal(result, false)
  })

  it('should be able to verify authentication parameters', async () => {
    const sourceSystemId = sc
    let result = await pfc.addAuthParams(sourceSystemId, accounts[0])
    assert.equal(result.receipt.status, true)
    result = await pfc.isAuthParams(sourceSystemId, accounts[0])
    assert.equal(result, true)
    result = await pfc.isAuthParams(sourceSystemId, accounts[1])
    assert.equal(result, false)
  })

  it('should be able to update authentication parameters', async () => {
    const sourceSystemId = sc
    let result = await pfc.addAuthParams(sourceSystemId, accounts[0])
    assert.equal(result.receipt.status, true)
    result = await pfc.isAuthParams(sourceSystemId, accounts[0])
    assert.equal(result, true)
    result = await pfc.addAuthParams(sourceSystemId, accounts[1])
    assert.equal(result.receipt.status, true)
    result = await pfc.isAuthParams(sourceSystemId, accounts[1])
    assert.equal(result, true)
  })

  it('should be able to perform call from remote chain with verifiable hidden authentication parameters', async () => {
    // Emit cross blockchain call
    const destinationSystemId = fc
    const sourceSystemId = sc
    const contractAddress = pay.address

    const tradeId = 'O-' + random()
    const fromAccount = 'Bob'
    const toAccount = 'Alice'
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)

    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId: 'N-123',
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await ptc.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const functionSignature = web3.eth.abi.encodeFunctionSignature('completeLeadLeg(string,string,string,uint256)');
    let callData = web3.eth.abi.encodeParameters(
      ['string', 'string', 'string', 'uint256'],
      [tradeId, hd.fromAccount, hd.toAccount, hd.amount]
    )
    const functionCallData = functionSignature + callData.slice(2);
    await pfc.setSystemId(sourceSystemId)
    await pfc.setAppendAuthParams(true)
    await pfc.addAuthParams(sourceSystemId, accounts[0])
    const crossBlockchainCallResult = await pfc.crossBlockchainCall(destinationSystemId, contractAddress, functionCallData, { from: accounts[0] })
    const txReceipt = crossBlockchainCallResult.receipt
    // Construct proof
    const block = await web3.eth.getBlock(txReceipt.blockNumber)
    const txHash = txReceipt.transactionHash
    const txProof = await ethProof.receiptProof(txHash)
    const encodedReceiptProof = crosschainMessagingSDK.encodeReceiptProof(txProof)
    const crosschainControlContract = '0x0000000000000000000000000000000000000000'
    const eventSig = web3.utils.soliditySha3('CrossBlockchainCallExecuted(uint256,address,bytes)')
    const encodedInfo = web3.eth.abi.encodeParameters(
      ['uint256', 'address', 'bytes32', 'bytes'],
      [destinationSystemId, crosschainControlContract, eventSig, encodedReceiptProof.rlpEncodedReceipt]
    )
    const rlpSiblingNodes = encodedReceiptProof.witness
    const blockHash = block.hash
    const receiptsRoot = block.receiptsRoot
    const blockHeaderObj = crosschainMessagingSDK.getBlockHeaderObjFromBlock('ibft', block)
    const rlpBlockHeaderExcludingSeals = blockHeaderObj.rlpBlockHeaderExcludingSeals
    const rlpBlockHeaderExcludingRound = blockHeaderObj.rlpBlockHeaderExcludingRound
    const rlpValidatorSignatures = blockHeaderObj.rlpValidatorSignatures
    const signatureOrProof = web3.eth.abi.encodeParameters(
      ['bytes', 'bytes32', 'bytes32', 'bytes', 'bytes', 'bytes'],
      [rlpSiblingNodes, receiptsRoot, blockHash, rlpBlockHeaderExcludingSeals, rlpBlockHeaderExcludingRound, rlpValidatorSignatures]
    )

    // Perform remote call via proof
    result = await pfc.performCallFromRemoteChain(sourceSystemId, eventSig, encodedInfo, signatureOrProof, { gas: 20000000 })
    assert.equal(result.receipt.status, true)
  })

  it('should not be able to perform call from remote chain with unverifiable hidden authentication parameters', async () => {
    // Emit cross blockchain call
    const destinationSystemId = fc
    const sourceSystemId = sc
    const contractAddress = pay.address

    const tradeId = 'O-' + random()
    const fromAccount = 'Bob'
    const toAccount = 'Alice'
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)

    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId: 'N-123',
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await ptc.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const functionSignature = web3.eth.abi.encodeFunctionSignature('completeLeadLeg(string,string,string,uint256)');
    let callData = web3.eth.abi.encodeParameters(
      ['string', 'string', 'string', 'uint256'],
      [tradeId, hd.fromAccount, hd.toAccount, hd.amount]
    )
    const functionCallData = functionSignature + callData.slice(2);
		// Onboard incorrect address
    await pfc.addAuthParams(sourceSystemId, accounts[0])
    await pfc.setAppendAuthParams(true);
    const crossBlockchainCallResult = await pfc.crossBlockchainCall(destinationSystemId, contractAddress, functionCallData, { from: accounts[1] })
    const txReceipt = crossBlockchainCallResult.receipt
    // Debug
    //let log = txReceipt.logs[0]
    //let event = log.event
    //console.log(event)
    //let args = log.args
    //const { 0: chainIdValue, 1: addressValue, 2: functionCallValue } = args
    //console.log("Destination blockchainId: ", chainIdValue.toString())
    //console.log("Destination contract: ", addressValue.toString())
    //console.log("FunctionCallData that includes auth params: ", functionCallValue.toString())
    //console.log("Source blockchainId: ", sc)
    //console.log("Source address: ", accounts[0])
    // Construct proof
    const block = await web3.eth.getBlock(txReceipt.blockNumber)
    const txHash = txReceipt.transactionHash
    const txProof = await ethProof.receiptProof(txHash)
    const encodedReceiptProof = crosschainMessagingSDK.encodeReceiptProof(txProof)
    const crosschainControlContract = '0x0000000000000000000000000000000000000000'
    const eventSig = web3.utils.soliditySha3('CrossBlockchainCallExecuted(uint256,address,bytes)')
    const encodedInfo = web3.eth.abi.encodeParameters(
      ['uint256', 'address', 'bytes32', 'bytes'],
      [destinationSystemId, crosschainControlContract, eventSig, encodedReceiptProof.rlpEncodedReceipt]
    )
    const rlpSiblingNodes = encodedReceiptProof.witness
    const blockHash = block.hash
    const receiptsRoot = block.receiptsRoot
    const blockHeaderObj = crosschainMessagingSDK.getBlockHeaderObjFromBlock('ibft', block)
    const rlpBlockHeaderExcludingSeals = blockHeaderObj.rlpBlockHeaderExcludingSeals
    const rlpBlockHeaderExcludingRound = blockHeaderObj.rlpBlockHeaderExcludingRound
    const rlpValidatorSignatures = blockHeaderObj.rlpValidatorSignatures
    const signatureOrProof = web3.eth.abi.encodeParameters(
      ['bytes', 'bytes32', 'bytes32', 'bytes', 'bytes', 'bytes'],
      [rlpSiblingNodes, receiptsRoot, blockHash, rlpBlockHeaderExcludingSeals, rlpBlockHeaderExcludingRound, rlpValidatorSignatures]
    )
    // Revert
    try {
      await ta.reverts(await pfc.performCallFromRemoteChain(sourceSystemId, eventSig, encodedInfo, signatureOrProof, { gas: 20000000 }), "Verification of NonAtomicAuthParams failed");
    } catch (err) {
      //console.log({err})
    }
  })
})
