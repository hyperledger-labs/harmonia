
function init(config, crosschainApplicationSDK){
  const validatorUpdateService = {
    getValidatorUpdateInstruction: async function getValidatorUpdateInstruction(systemId, operationId) {
      return await crosschainApplicationSDK.getValidatorUpdateInstruction(systemId, operationId)
    },
    postValidatorUpdateInstruction: async function postValidatorUpdateInstruction(systemId, body) {
      return await crosschainApplicationSDK.submitValidatorUpdateInstruction(systemId, body)
    },
    deleteValidatorUpdateInstruction: async function deleteValidatorUpdateInstruction(systemId, operationId) {
      return await crosschainApplicationSDK.deleteValidatorUpdateInstruction(systemId, operationId)
    },
  }
  return validatorUpdateService
}

module.exports = init

