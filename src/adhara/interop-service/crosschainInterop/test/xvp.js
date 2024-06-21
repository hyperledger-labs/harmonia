const XvP = artifacts.require("XvP");
const Token = artifacts.require("Token");
const CrosschainFunctionCall = artifacts.require("CrosschainFunctionCall");
const CrosschainMessaging = artifacts.require('CrosschainMessaging');

const { ethers } = require("ethers");
let abiCoder = new ethers.utils.AbiCoder

const ta = require('truffle-assertions');
const random = function () { return Math.floor(Math.random() * (999 - 100 + 1) + 100) }

let xvp = null, token = null, functionCall = null, messaging = null;
let fc = 0, sc = 1;
const fromAccount = 'Bob'
const toAccount = 'Alice'
const notaryId = 'N-123'

contract("XvP", async accounts => {
  beforeEach(async () => {
    xvp = await XvP.new();
    token = await Token.new();
    messaging = await CrosschainMessaging.new();
    functionCall = await CrosschainFunctionCall.new(); // Payment function call contract
    let result = null;
    result = await functionCall.setMessagingContractAddress(messaging.address)
    assert.equal(result.receipt.status, true)
    result = await xvp.setTokenContractAddress(token.address)
    assert.equal(result.receipt.status, true)
    result = await token.addHoldNotary(notaryId, xvp.address)
    assert.equal(result.receipt.status, true)
    result = await xvp.setNotaryId(notaryId)
    assert.equal(result.receipt.status, true)
    // Delivery interop
    result = await xvp.setFunctionCallContractAddress(functionCall.address)
    assert.equal(result.receipt.status, true)

    const amd = ({ // Mint details
      operationId: 'M-' + random(),
      toAccount: 'Alice',
      amount: 1000000,
      metaData: ''
    })
    let amint = await token.create(amd.operationId, amd.toAccount, amd.amount, amd.metaData)
    assert.equal(amint.receipt.status, true)
    const bmd = ({ // Mint details
      operationId: 'M-' + random(),
      toAccount: 'Bob',
      amount: 1000000,
      metaData: ''
    })
    let bmint = await token.create(bmd.operationId, bmd.toAccount, bmd.amount, bmd.metaData)
    assert.equal(bmint.receipt.status, true)
  })
  it("should be able to create an operation id on-chain that matches the off-chain creation", async () => {
    const tradeId = 'a'
    const fromAccount = 'FNGBGB00GBP'
    const toAccount = 'FNUSUS00GBP'
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)

    const smartContractOperationId = await xvp.getOperationIdFromTradeId(tradeId, fromAccount, toAccount)

    assert.equal(smartContractOperationId, operationId)
  })
  it("should be able to set remote account id to local account id", async () => {
    const result = await xvp.setRemoteAccountIdToLocalAccountId("F-007", "L-007")
    assert.equal(result.receipt.status, true)
  })
  it("should be able to check a hold when it does not exist", async () => {
    const td = ({
      tradeId: 'T-000',
      sender: 'P-001',
      receiver: 'P-002',
      amount: 1
    });
    const hd = ({ // Hold details
      operationId: 'c0399780e14b48286716ef379c7e0366bdb348e23ac6290b68d14e9a6e3a6cc0',
      fromAccount: 'P-001',
      toAccount: 'P-002',
      notaryId,
      amount: '1',
      status: '0x6e6f6e4578697374656e74000000000000000000000000000000000000000000'
    });
    try {
      await ta.reverts(
        xvp.checkHold(hd, td, token.address),
        "Hold does not exist"
      );
    } catch (err) {
      //console.log({ err })
    }
  })
  it("should be able to check a hold including amount", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let hold = await xvp.getHold(td, token.address)
    let check = await xvp.checkHold(hold, td, token.address)
    assert.equal(check.receipt.status, true)
  })
  it("should be able to check a hold without amount", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 0
    });
    let hold = await xvp.getHold(td, token.address)
    let check = await xvp.checkHold(hold, td, token.address)
    assert.equal(check.receipt.status, true)
  })
  it("should be able to start the lead leg", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let check = await xvp.startLeadLeg(td, fc, sc, xvp.address);
    assert.equal(check.receipt.status, true)
  })
  it("should fail to start the lead leg when the hold is not in place", async () => {
    const td = ({ // Trade details
      tradeId: 'O-' + random(),
      sender: toAccount,
      receiver: fromAccount,
      amount: 1
    });
    try {
      await ta.reverts(
        xvp.startLeadLeg(td, fc, sc, xvp.address),
        "Hold does not exist"
      );
    } catch (err) {
      //console.log({ err })
    }
  })
  it("should be able to request the follow leg", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let check = await xvp.requestFollowLeg(td.tradeId, td.sender, td.receiver, xvp.address, fc, td.amount)
    assert.equal(check.receipt.status, true)
  })
  it("should fail to request the follow leg when the hold is not in place", async () => {
    const td = ({ // Trade details
      tradeId: 'O-' + random(),
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    try {
      await ta.reverts(
        xvp.requestFollowLeg(td.tradeId, td.sender, td.receiver, xvp.address, fc, td.amount),
        "Hold does not exist"
      );
    } catch (err) {
      //console.log({ err })
    }
  })
  it("should be able to request the follow leg using remote identities", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: 'BobX500',
      receiver: 'AliceX500',
      amount: 1
    });
    result = await xvp.setRemoteAccountIdToLocalAccountId(hd.fromAccount, "BobX500")
    assert.equal(result.receipt.status, true)
    result = await xvp.setRemoteAccountIdToLocalAccountId(hd.toAccount, "AliceX500")
    assert.equal(result.receipt.status, true)
    result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    let check = await xvp.requestFollowLeg(td.tradeId, td.sender, td.receiver, xvp.address, fc, td.amount)
    assert.equal(check.receipt.status, true)
  })
  it("should be able to complete the lead leg", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let check = await xvp.completeLeadLeg(td.tradeId, td.sender, td.receiver, td.amount);
    assert.equal(check.receipt.status, true)
  })
  it("should fail to complete the lead leg when the hold is not in place", async () => {
    const td = ({ // Trade details
      tradeId: 'O-' + random(),
      sender: toAccount,
      receiver: fromAccount,
      amount: 1
    });
    try {
      await ta.reverts(
        xvp.completeLeadLeg(td.tradeId, td.sender, td.receiver, td.amount),
        "Hold was not executable, or does not exist"
      );
    } catch (err) {
      //console.log({ err })
    }
  })
  it("should fail to complete the lead leg on the follower chain", async () => {
    const td = ({ // Trade details
      tradeId: 'O-' + random(),
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    })
    await ta.reverts(xvp.completeLeadLeg(td.tradeId, td.sender, td.receiver, td.amount))
  })
  it("should not be able to perform a cancellation if the operation id is not marked as cancelled or the msg.sender is not the function call contract", async () => {
    const tradeId = 'O-' + random()
    try {
      await ta.reverts(xvp.performCancellation(tradeId, fromAccount, toAccount), "Operation id must be marked as cancelled when the msg.sender is not the function call contract");
    } catch (err) {
      //console.log({ err })
    }
  })
  it("should be able to cancel the lead leg if isCancelled==false", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });

    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let tx = await xvp.startCancellation(tradeId, fromAccount, toAccount, fc, sc, xvp.address)
    let selector = web3.utils.sha3("performCancellation(string,string,string)").slice(0, 10)
    let event = tx.receipt.rawLogs.some(l => { return l.topics[0] == web3.utils.sha3("CrosschainFunctionCall(uint256,address,bytes)") && abiCoder.decode(["uint256", "address", "bytes"], l.data)[2].startsWith(selector) });
    assert.ok(event, "CrosschainFunctionCall event with performCancellation not emitted");
    assert.equal(tx.receipt.status, true)

    let result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)

    let cancelledOperationId = await xvp.getIsCancelled(hd.operationId)
    assert.equal(cancelledOperationId, true)
    let check = await xvp.performCancellation(td.tradeId, td.sender, td.receiver)
    assert.equal(check.receipt.status, true)
  })
  it("should not be able to start a cancellation if the hold already exists", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    let result = await token.createHold(hd.operationId, hd.fromAccount, hd.toAccount, hd.notaryId, hd.amount, hd.duration, hd.metaData);
    assert.equal(result.receipt.status, true)
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    try {
      await ta.reverts(xvp.startCancellation(tradeId, fromAccount, toAccount, fc, sc, xvp.address), "Hold already exists, cannot start cancellation");
    } catch (err) {
      //console.log({ err })
    }
  })
  it("should be able to start a cancellation if the hold does not exist", async () => {
    const tradeId = 'O-' + random()
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let tx = await xvp.startCancellation(tradeId, fromAccount, toAccount, fc, sc, xvp.address)
    let selector = web3.utils.sha3("performCancellation(string,string,string)").slice(0, 10)
    let event = tx.receipt.rawLogs.some(l => { return l.topics[0] == web3.utils.sha3("CrosschainFunctionCall(uint256,address,bytes)") && abiCoder.decode(["uint256", "address", "bytes"], l.data)[2].startsWith(selector) });
    assert.ok(event, "CrosschainFunctionCall event with performCancellation not emitted");
    assert.equal(tx.receipt.status, true)
  })
  it("should not be able to request the follow leg if the operation id is cancelled", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let tx = await xvp.startCancellation(tradeId, fromAccount, toAccount, fc, sc, xvp.address)
    let selector = web3.utils.sha3("performCancellation(string,string,string)").slice(0, 10)
    let event = tx.receipt.rawLogs.some(l => { return l.topics[0] == web3.utils.sha3("CrosschainFunctionCall(uint256,address,bytes)") && abiCoder.decode(["uint256", "address", "bytes"], l.data)[2].startsWith(selector) });
    assert.ok(event, "CrosschainFunctionCall event with performCancellation not emitted");
    assert.equal(tx.receipt.status, true)

    let isCancelled = await xvp.getIsCancelled(hd.operationId)
    assert.equal(isCancelled, true)
    try {
      await ta.reverts(xvp.requestFollowLeg(td.tradeId, td.sender, td.receiver, xvp.address, fc, td.amount), "OperationId for hold is marked as cancelled");
    } catch (err) {
      //console.log({ err })
    }
  })
  it("should not be able to start the lead leg if the operation id is cancelled", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let tx = await xvp.startCancellation(tradeId, fromAccount, toAccount, fc, sc, xvp.address)
    let selector = web3.utils.sha3("performCancellation(string,string,string)").slice(0, 10)
    let event = tx.receipt.rawLogs.some(l => { return l.topics[0] == web3.utils.sha3("CrosschainFunctionCall(uint256,address,bytes)") && abiCoder.decode(["uint256", "address", "bytes"], l.data)[2].startsWith(selector) });
    assert.ok(event, "CrosschainFunctionCall event with performCancellation not emitted");
    assert.equal(tx.receipt.status, true)

    let isCancelled = await xvp.getIsCancelled(hd.operationId)
    assert.equal(isCancelled, true)
    try {
      await ta.reverts(xvp.startLeadLeg(td, fc, sc, xvp.address), "OperationId for hold is marked as cancelled");
    } catch (err) {
      //console.log({ err })
    }
  })
  it("should not be able to complete the lead leg if the operation id is cancelled", async () => {
    const tradeId = 'O-' + random()
    const operationId = web3.utils.soliditySha3({ type: 'string', value: tradeId }, fromAccount, toAccount).substring(2)
    const hd = ({ // Hold details
      operationId,
      fromAccount,
      toAccount,
      notaryId,
      amount: 1,
      duration: 0,
      metaData: ''
    });
    const td = ({ // Trade details
      tradeId: tradeId,
      sender: fromAccount,
      receiver: toAccount,
      amount: 1
    });
    let tx = await xvp.startCancellation(tradeId, fromAccount, toAccount, fc, sc, xvp.address)
    let selector = web3.utils.sha3("performCancellation(string,string,string)").slice(0, 10)
    let event = tx.receipt.rawLogs.some(l => { return l.topics[0] == web3.utils.sha3("CrosschainFunctionCall(uint256,address,bytes)") && abiCoder.decode(["uint256", "address", "bytes"], l.data)[2].startsWith(selector) });
    assert.ok(event, "CrosschainFunctionCall event with performCancellation not emitted");
    assert.equal(tx.receipt.status, true)

    let isCancelled = await xvp.getIsCancelled(hd.operationId)
    assert.equal(isCancelled, true)
    try {
      await ta.reverts(xvp.completeLeadLeg(td.tradeId, td.sender, td.receiver, td.amount), "OperationId for hold is marked as cancelled");
    } catch (err) {
      //console.log({ err })
    }
  })
})
