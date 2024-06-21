function init(config, crosschainApplicationSDK){
  const service = {
    getRegisteredFunction: async function getRegisteredFunction(networkId, remoteNetworkId, functionSignature, index){
      return await crosschainApplicationSDK.getCordaParameterHandler(networkId, remoteNetworkId, functionSignature, index)
    },
    postRegisteredFunction: async function postRegisteredFunction(networkId, remoteNetworkId, functionSignature, body){
      return await crosschainApplicationSDK.setCordaParameterHandlers(networkId, remoteNetworkId, functionSignature, body)
    },
    deleteRegisteredFunction: async function deleteRegisteredFunction(networkId, remoteNetworkId, functionSignature){
      return await crosschainApplicationSDK.removeCordaParameterHandlers(networkId, remoteNetworkId, functionSignature)
    }
  }

  return service
}

module.exports = init
