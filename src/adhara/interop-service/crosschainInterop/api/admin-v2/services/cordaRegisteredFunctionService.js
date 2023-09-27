function init(config, crosschainApplicationSDK){
  const service = {
    getRegisteredFunction: async function getRegisteredFunction(systemId, foreignSystemId, functionSignature, index){
      return await crosschainApplicationSDK.getCordaParameterHandler(systemId, foreignSystemId, functionSignature, index)
    },
    postRegisteredFunction: async function postRegisteredFunction(systemId, foreignSystemId, functionSignature, body){
      return await crosschainApplicationSDK.setCordaParameterHandlers(systemId, foreignSystemId, functionSignature, body)
    },
    deleteRegisteredFunction: async function deleteRegisteredFunction(systemId, foreignSystemId, functionSignature){
      return await crosschainApplicationSDK.removeCordaParameterHandlers(systemId, foreignSystemId, functionSignature)
    }
  }

  return service
}

module.exports = init
