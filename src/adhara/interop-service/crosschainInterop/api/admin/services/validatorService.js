function init(config, crosschainApplicationSDK) {
  const validatorService = {
    getValidators: async function getValidators(blockHash) {
      if (!!blockHash) {
        return await crosschainApplicationSDK.getValidatorsByBlockhash(blockHash)
      } else {
        return await crosschainApplicationSDK.getAllValidators()
      }
    }
  }

  return validatorService
}


module.exports = init
