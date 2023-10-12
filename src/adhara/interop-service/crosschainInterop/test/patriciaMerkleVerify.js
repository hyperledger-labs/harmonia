const { GetProof } = require('eth-proof')
const {v4: uuidv4} = require("uuid");
const opId = function () { return uuidv4().substring(0,16) }

const CrosschainFunctionCall = artifacts.require('CrosschainFunctionCall');
const CrosschainMessaging = artifacts.require('CrosschainMessaging');
const XvP = artifacts.require('XvP');
const Token = artifacts.require('Token');
const config = { logLevel: 'silent' }

const crosschainMessagingSDK = require('../src/CrosschainMessagingSDK')(config, {})

const random = function () { return Math.floor(Math.random() * (999 - 100 + 1) + 100) }
let del = null, pay = null, ptc = null, dtc = null, pfc = null, dfc = null, pmc = null, dmc = null;
let fc = 0, sc = 1;

contract.skip('PatriciaMerkleVerify', async accounts => {
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

  it('should verify a Patricia Merkle tree proof including an extension node of an event log containing CrossBlockchainCallExecuted', async () => {
    // Emit cross blockchain call
    const destinationBlockchainId = sc
    const sourceBlockchainId = sc
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

    let nonce = await web3.eth.getTransactionCount(accounts[28]) - 1
    for (let i = 0; i < 129; i++) {
      web3.eth.sendTransaction({ from: accounts[28], to: accounts[0], value: 1, nonce: ++nonce, gasPrice: 3 })
    }

    const crossBlockchainCallResult = await pfc.crossBlockchainCall(destinationBlockchainId, contractAddress, functionCallData)
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
      [destinationBlockchainId, crosschainControlContract, eventSig, encodedReceiptProof.rlpEncodedReceipt]
    )
    const rlpSiblingNodes = encodedReceiptProof.witness
    const blockHash = block.hash
    const receiptsRoot = block.receiptsRoot
    const blockHeaderObj = crosschainMessagingSDK.getBlockHeaderObjFromBlock('ibft', block)
    const rlpBlockHeader = blockHeaderObj.rlpBlockHeader
    const rlpBlockHeaderExcludingRound = blockHeaderObj.rlpBlockHeaderExcludingRound
    const rlpValidatorSignatures = blockHeaderObj.rlpValidatorSignatures
    const signatureOrProof = web3.eth.abi.encodeParameters(
      ['bytes', 'bytes32', 'bytes32', 'bytes', 'bytes', 'bytes'],
      [rlpSiblingNodes, receiptsRoot, blockHash, rlpBlockHeader, rlpBlockHeaderExcludingRound, rlpValidatorSignatures]
    )

    // Perform remote call via proof
    result = await pfc.performCallFromRemoteChain(sourceBlockchainId, eventSig, encodedInfo, signatureOrProof, { gas: 20000000 })
    assert.equal(result.receipt.status, true)
  })

})
