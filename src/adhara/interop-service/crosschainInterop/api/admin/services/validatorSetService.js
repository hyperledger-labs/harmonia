
function init(config, crosschainApplicationSDK){
  const validatorSetService = {
    getValidatorSetInstruction: async function getValidatorSetInstruction(networkId, operationId) {
      return await crosschainApplicationSDK.getValidatorSetInstruction(networkId, operationId)
    },
    postValidatorSetInstruction: async function postValidatorSetInstruction(networkId, body) {
      return await crosschainApplicationSDK.submitValidatorSetInstruction(networkId, body)
    },
    deleteValidatorSetInstruction: async function deleteValidatorSetInstruction(networkId, operationId) {
      return await crosschainApplicationSDK.deleteValidatorSetInstruction(networkId, operationId)
    },
  }
  return validatorSetService
}

module.exports = init

