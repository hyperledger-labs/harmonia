const path = require('path')
const fs = require('fs')
const settlementInstructionStates = require('./settlementInstructionStates.js')

function init(config, dependencies) {

  const logger = dependencies.logger

  const settlementInstructionStoreName = 'settlementInstructionStore.json'
  const settlementInstructionStorePath = path.join(__dirname, '../..', config.fileDBDirectory, settlementInstructionStoreName)

  fs.mkdirSync(path.dirname(settlementInstructionStorePath), {recursive: true})
  if(!fs.existsSync(settlementInstructionStorePath)){
    fs.writeFileSync(settlementInstructionStorePath, JSON.stringify([]), {flag: 'w'})
  }

  const settlementInstructionObjStore = JSON.parse(fs.readFileSync(settlementInstructionStorePath, {encoding: 'utf8', flag: 'a+'}))

  const settlementInstructionRequestStore = []

  async function addSettlementInstructionRequestToStore(settlementInstructionObj) {
    settlementInstructionRequestStore.push(settlementInstructionObj)
  }

  async function removeSettlementInstructionRequestFromStore(settlementInstructionObj) {
    for (let i in settlementInstructionRequestStore) {
      const settlementInstructionRequest = settlementInstructionRequestStore[i]
      if(settlementInstructionRequest.tradeId === settlementInstructionObj.tradeId
        || settlementInstructionRequest.fromAccount === settlementInstructionObj.fromAccount
        || settlementInstructionRequest.toAccount === settlementInstructionObj.toAccount
        || settlementInstructionRequest.networkId === settlementInstructionObj.networkId
        || settlementInstructionRequest.amount === settlementInstructionObj.amount
        || settlementInstructionRequest.state === settlementInstructionObj.state
      ){
        settlementInstructionRequestStore.splice(i, 1)
      }
    }
  }

  async function findSettlementInstructionRequestsToProcess(){
    return settlementInstructionRequestStore
  }


  // Return all settlement instructions that still require some processing
  async function findSettlementInstructionsToProcess(){
    const list = []
    for (let settlementInstructionObj of settlementInstructionObjStore) {
      if(settlementInstructionObj.state === settlementInstructionStates.confirmed
        || settlementInstructionObj.state === settlementInstructionStates.waitingForHold
        || settlementInstructionObj.state === settlementInstructionStates.waitingForCrosschainFunctionCall
        || settlementInstructionObj.state === settlementInstructionStates.waitingForExecuteHoldExecuted
        || settlementInstructionObj.state === settlementInstructionStates.waitingForCommunication
        || settlementInstructionObj.state === settlementInstructionStates.waitingForCancelHoldExecuted
        || settlementInstructionObj.state === settlementInstructionStates.waitingForRemoteNetworkCancellation
        || settlementInstructionObj.state === settlementInstructionStates.cancel
        || settlementInstructionObj.state === settlementInstructionStates.cancelling
      ){
        list.push(settlementInstructionObj)
      }
    }
    return list
  }

  async function addSettlementInstructionToStore(settlementInstructionObj) {
    let found = false
    for (let i in settlementInstructionObjStore) {
      const item = settlementInstructionObjStore[i]
      if (settlementInstructionObj.tradeId === item.tradeId && settlementInstructionObj.fromAccount === item.fromAccount && settlementInstructionObj.toAccount === item.toAccount) {
        found = true
        break
      }
    }
    if (!found) {
      settlementInstructionObjStore.push(settlementInstructionObj)
      fs.writeFileSync(settlementInstructionStorePath, JSON.stringify(settlementInstructionObjStore))
    } else {
      return Promise.reject(Error('Duplicate settlement instruction for tradeId [' + settlementInstructionObj.tradeId + ']'))
    }
  }

  async function updateSettlementInstructionStore(settlementInstructionObj) {
    let found = false
    for (let i in settlementInstructionObjStore) {
      const item = settlementInstructionObjStore[i]
      if (settlementInstructionObj.tradeId === item.tradeId && settlementInstructionObj.fromAccount === item.fromAccount && settlementInstructionObj.toAccount === item.toAccount) {
        found = true
        settlementInstructionObjStore[i] = settlementInstructionObj
        logger.log('debug', 'Updated settlement instruction object for tradeId [' + settlementInstructionObj.tradeId + '], fromAccount ['+settlementInstructionObj.fromAccount+'], toAccount ['+settlementInstructionObj.toAccount+'] and state ['+settlementInstructionObj.state+']')
        break
      }
    }
    fs.writeFileSync(settlementInstructionStorePath, JSON.stringify(settlementInstructionObjStore))
    if (!found) {
      return Promise.reject(Error('Settlement instruction object for tradeId [' + settlementInstructionObj.tradeId + '], fromAccount ['+settlementInstructionObj.fromAccount+'], toAccount ['+settlementInstructionObj.toAccount+'] was not found in store'))
    }
  }

  async function findSettlementInstructionsByState(state) {
    const list = []
    for (let item of settlementInstructionObjStore) {
      if (item.state === state) {
        list.push(item)
      }
    }
    return list
  }

  async function findSettlementInstruction(networkId, tradeId, fromAccount, toAccount) {
    for (let item of settlementInstructionObjStore) {
      if (item.networkId === networkId && item.tradeId === tradeId && item.fromAccount === fromAccount && item.toAccount === toAccount) {
        return item
      }
    }
  }

  async function findSettlementInstructionByOperationId(networkId, operationId) {
    for (let item of settlementInstructionObjStore) {
      if (item.networkId === networkId && item.localOperationId === operationId) {
        return item
      }
    }
  }

  async function findSettlementInstructionByTradeId(networkId, tradeId) {
    let it
    for (let item of settlementInstructionObjStore) {
      if (item.networkId === networkId && item.tradeId === tradeId) {
        it = item
      }
    }
    return it
  }

  async function removeSettlementInstructionFromStore(tradeId, fromAccount, toAccount) {
    let found = false
    for (let i in settlementInstructionObjStore) {
      const item = settlementInstructionObjStore[i]
      if (tradeId === item.tradeId &&
        fromAccount === item.fromAccount &&
        toAccount === item.toAccount) {
        found = true
        settlementInstructionObjStore.splice(i, 1)
        break
      }
    }
      fs.writeFileSync(settlementInstructionStorePath, JSON.stringify(settlementInstructionObjStore))
    if (!found) {
      return Promise.reject(Error('Settlement instruction for tradeId [' + tradeId + '], fromAccount [' + fromAccount + '], toAccount [' + toAccount + '] not found'))
    }
  }

  async function printAllSettlementInstructions() {
    for (let settlementInstructionObj of settlementInstructionObjStore) {
      logger.log('debug', settlementInstructionObj.toString())
    }
  }

  return {
    addSettlementInstructionRequestToStore,
    removeSettlementInstructionRequestFromStore,
    findSettlementInstructionRequestsToProcess,
    addSettlementInstructionToStore,
    updateSettlementInstructionStore,
    findSettlementInstructionsByState,
    findSettlementInstruction,
    findSettlementInstructionByOperationId,
    findSettlementInstructionByTradeId,
    removeSettlementInstructionFromStore,
    printAllSettlementInstructions,
    findSettlementInstructionsToProcess
  }
}

module.exports = init;
