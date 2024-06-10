function init(config, crosschainApplicationSDK){
  const service = {
    getCordaParticipant: async function getCordaParticipant(networkId, remoteNetworkId, publicKey){
      return await crosschainApplicationSDK.isCordaParticipant(networkId, remoteNetworkId, publicKey)
    },
    postCordaParticipant: async function postCordaParticipant(networkId, body){
      return await crosschainApplicationSDK.addCordaParticipant(networkId, body.remoteNetworkId, body.publicKey)
    },
    deleteCordaParticipant: async function deleteCordaParticipant(networkId, remoteNetworkId, publicKey){
      return await crosschainApplicationSDK.removeCordaParticipant(networkId, remoteNetworkId, publicKey)
    }
  }

  return service
}

module.exports = init
