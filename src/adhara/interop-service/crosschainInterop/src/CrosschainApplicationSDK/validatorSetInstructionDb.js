const path = require('path')
const fs = require('fs')
const validatorSetInstructionStates = require('./validatorSetInstructionStates.js')

function init(config, dependencies) {

  const logger = dependencies.logger
  const validatorSetInstructionStoreName = 'validatorSetInstructionStore.json'
  const validatorSetInstructionStorePath = path.join(__dirname, '../..', config.fileDBDirectory, validatorSetInstructionStoreName)

  fs.mkdirSync(path.dirname(validatorSetInstructionStorePath), {recursive: true})
  if(!fs.existsSync(validatorSetInstructionStorePath)){
    fs.writeFileSync(validatorSetInstructionStorePath, JSON.stringify([]), {flag: 'w'})
  }

  const validatorSetInstructionObjStore = JSON.parse(fs.readFileSync(validatorSetInstructionStorePath, {encoding: 'utf8', flag: 'a+'}))

  async function addValidatorSetInstruction(validatorSetInstructionObj) {
    let found = false
    for (let i in validatorSetInstructionObjStore) {
      const item = validatorSetInstructionObjStore[i]
      if (validatorSetInstructionObj.networkId === item.networkId && validatorSetInstructionObj.operationId === item.operationId) {
        found = true
        break
      }
    }
    if (!found) {
      validatorSetInstructionObjStore.push(validatorSetInstructionObj)
      fs.writeFileSync(validatorSetInstructionStorePath, JSON.stringify(validatorSetInstructionObjStore))
    } else {
      return Promise.reject(Error('Duplicate validator set instruction for operationId [' + validatorSetInstructionObj.operationId + ']'))
    }
  }

  async function updateValidatorSetInstruction(validatorSetInstructionObj) {
    let found = false
    for (let i in validatorSetInstructionObjStore) {
      const item = validatorSetInstructionObjStore[i]
      if (validatorSetInstructionObj.networkId === item.networkId && validatorSetInstructionObj.operationId === item.operationId) {
        found = true
        validatorSetInstructionObjStore[i] = validatorSetInstructionObj
        logger.log('debug', 'Updated validator set instruction object for networkId [' + validatorSetInstructionObj.networkId + '] with operationId ['+validatorSetInstructionObj.operationId+'] and state ['+validatorSetInstructionObj.state+']')
        break
      }
    }
      fs.writeFileSync(validatorSetInstructionStorePath, JSON.stringify(validatorSetInstructionObjStore))
    if (!found) {
      return Promise.reject(Error('Validator set instruction from networkId [' + validatorSetInstructionObj.networkId + '] with operationId [' + validatorSetInstructionObj.operationId + '] was not found'))
    }
  }

  async function removeValidatorSetInstruction(networkId, operationId) {
    let found = false
    for (let i in validatorSetInstructionObjStore) {
      const item = validatorSetInstructionObjStore[i]
      if (networkId === item.networkId && operationId === item.operationId) {
        found = true
        validatorSetInstructionObjStore.splice(i, 1)
        break
      }
    }
    fs.writeFileSync(validatorSetInstructionStorePath, JSON.stringify(validatorSetInstructionObjStore))
    if (!found) {
      return Promise.reject(Error('Validator update instruction from networkId [' + networkId + '] with operationId [' + operationId + '] was not found'))
    }
  }

  async function findValidatorSetInstructionByState(networkId, state) {
    const list = []
    for (let item of validatorSetInstructionObjStore) {
      if (item.networkId === networkId && item.state === state) {
        list.push(item)
      }
    }
    return list
  }

  async function findValidatorSetInstructionByOperationId(networkId, operationId) {
    for (let item of validatorSetInstructionObjStore) {
      if (item.networkId === networkId && item.operationId === operationId) {
        return item
      }
    }
  }

  // Return all validator set instructions that still require some processing
  async function findValidatorSetInstructionsToProcess(){
    const list = []
    for (let validatorSetInstructionObj of validatorSetInstructionObjStore) {
      if (validatorSetInstructionObj.state === validatorSetInstructionStates.confirmed || validatorSetInstructionObj.state === validatorSetInstructionStates.waitingForCrosschainFunctionCall) {
        list.push(validatorSetInstructionObj)
      }
    }
    return list
  }

  async function printAllValidatorSetInstructions() {
    for (let validatorSetInstructionObj of validatorSetInstructionObjStore) {
      logger.log('debug', validatorSetInstructionObj.toString())
    }
  }

  return {
    addValidatorSetInstruction,
    updateValidatorSetInstruction,
    removeValidatorSetInstruction,
    findValidatorSetInstructionByState,
    findValidatorSetInstructionByOperationId,
    findValidatorSetInstructionsToProcess,
    printAllValidatorSetInstructions
  }
}

module.exports = init;
