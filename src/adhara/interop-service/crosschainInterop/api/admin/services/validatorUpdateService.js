
function init(config, crosschainApplicationSDK){
  const validatorUpdateService = {
    getValidatorUpdateInstruction: async function getValidatorUpdateInstruction(networkId, operationId) {
      return await crosschainApplicationSDK.getValidatorUpdateInstruction(networkId, operationId)
    },
    postValidatorUpdateInstruction: async function postValidatorUpdateInstruction(networkId, body) {
      return await crosschainApplicationSDK.submitValidatorUpdateInstruction(networkId, body)
    },
    deleteValidatorUpdateInstruction: async function deleteValidatorUpdateInstruction(networkId, operationId) {
      return await crosschainApplicationSDK.deleteValidatorUpdateInstruction(networkId, operationId)
    },
  }
  return validatorUpdateService
}

module.exports = init

