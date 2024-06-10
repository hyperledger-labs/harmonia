function init(config, crosschainApplicationSDK){
  const service = {
    getCordaNotary: async function getCordaNotary(networkId, remoteNetworkId, publicKey){
      return await crosschainApplicationSDK.isCordaNotary(networkId, remoteNetworkId, publicKey)
    },
    postCordaNotary: async function postCordaNotary(networkId, body){
      return await crosschainApplicationSDK.addCordaNotary(networkId, body.remoteNetworkId, body.publicKey)
    },
    deleteCordaNotary: async function deleteCordaNotary(networkId, remoteNetworkId, publicKey){
      return await crosschainApplicationSDK.removeCordaNotary(networkId, remoteNetworkId, publicKey)
    }
  }

  return service
}

module.exports = init
