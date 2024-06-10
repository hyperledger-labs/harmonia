const path = require('path')
const fs = require('fs')
const validatorUpdateInstructionStates = require('./validatorUpdateInstructionStates.js')

function init(config, dependencies) {

  const logger = dependencies.logger
  const validatorUpdateInstructionStoreName = 'validatorUpdateInstructionStore.json'
  const validatorUpdateInstructionStorePath = path.join(__dirname, '../..', config.fileDBDirectory, validatorUpdateInstructionStoreName)

  fs.mkdirSync(path.dirname(validatorUpdateInstructionStorePath), {recursive: true})
  if(!fs.existsSync(validatorUpdateInstructionStorePath)){
    fs.writeFileSync(validatorUpdateInstructionStorePath, JSON.stringify([]), {flag: 'w'})
  }

  const validatorUpdateInstructionObjStore = JSON.parse(fs.readFileSync(validatorUpdateInstructionStorePath, {encoding: 'utf8', flag: 'a+'}))

  async function addValidatorUpdateInstruction(validatorUpdateInstructionObj) {
    let found = false
    for (let i in validatorUpdateInstructionObjStore) {
      const item = validatorUpdateInstructionObjStore[i]
      if (validatorUpdateInstructionObj.networkId === item.networkId && validatorUpdateInstructionObj.operationId === item.operationId) {
        found = true
        break
      }
    }
    if (!found) {
      validatorUpdateInstructionObjStore.push(validatorUpdateInstructionObj)
      fs.writeFileSync(validatorUpdateInstructionStorePath, JSON.stringify(validatorUpdateInstructionObjStore))
    } else {
      return Promise.reject(Error('Duplicate validator update instruction for operationId [' + validatorUpdateInstructionObj.operationId + ']'))
    }
  }

  async function updateValidatorUpdateInstruction(validatorUpdateInstructionObj) {
    let found = false
    for (let i in validatorUpdateInstructionObjStore) {
      const item = validatorUpdateInstructionObjStore[i]
      if (validatorUpdateInstructionObj.networkId === item.networkId && validatorUpdateInstructionObj.operationId === item.operationId) {
        found = true
        validatorUpdateInstructionObjStore[i] = validatorUpdateInstructionObj
        logger.log('debug', 'Updated validator update instruction object for networkId [' + validatorUpdateInstructionObj.networkId + '] with operationId ['+validatorUpdateInstructionObj.operationId+'] and state ['+validatorUpdateInstructionObj.state+']')
        break
      }
    }
      fs.writeFileSync(validatorUpdateInstructionStorePath, JSON.stringify(validatorUpdateInstructionObjStore))
    if (!found) {
      return Promise.reject(Error('Validator update instruction from networkId [' + validatorUpdateInstructionObj.networkId + '] with operationId [' + validatorUpdateInstructionObj.operationId + '] was not found'))
    }
  }

  async function removeValidatorUpdateInstruction(networkId, operationId) {
    let found = false
    for (let i in validatorUpdateInstructionObjStore) {
      const item = validatorUpdateInstructionObjStore[i]
      if (networkId === item.networkId && operationId === item.operationId) {
        found = true
        validatorUpdateInstructionObjStore.splice(i, 1)
        break
      }
    }
    fs.writeFileSync(validatorUpdateInstructionStorePath, JSON.stringify(validatorUpdateInstructionObjStore))
    if (!found) {
      return Promise.reject(Error('Validator update instruction from networkId [' + networkId + '] with operationId [' + operationId + '] was not found'))
    }
  }

  async function findValidatorUpdateInstructionByState(networkId, state) {
    const list = []
    for (let item of validatorUpdateInstructionObjStore) {
      if (item.networkId === networkId && item.state === state) {
        list.push(item)
      }
    }
    return list
  }

  async function findValidatorUpdateInstructionByOperationId(networkId, operationId) {
    for (let item of validatorUpdateInstructionObjStore) {
      if (item.networkId === networkId && item.operationId === operationId) {
        return item
      }
    }
  }

  // Return all validator update instructions that still require some processing
  async function findValidatorUpdateInstructionsToProcess(){
    const list = []
    for (let validatorUpdateInstructionObj of validatorUpdateInstructionObjStore) {
      if (validatorUpdateInstructionObj.state === validatorUpdateInstructionStates.confirmed || validatorUpdateInstructionObj.state === validatorUpdateInstructionStates.waitingForCrosschainFunctionCall) {
        list.push(validatorUpdateInstructionObj)
      }
    }
    return list
  }

  async function printAll() {
    for (let validatorUpdateInstructionObj of validatorUpdateInstructionObjStore) {
      logger.log('debug', validatorUpdateInstructionObj.toString())
    }
  }

  return {
    addValidatorUpdateInstruction,
    updateValidatorUpdateInstruction,
    removeValidatorUpdateInstruction,
    findValidatorUpdateInstructionByState,
    findValidatorUpdateInstructionByOperationId,
    findValidatorUpdateInstructionsToProcess,
    printAll
  }
}

module.exports = init;
