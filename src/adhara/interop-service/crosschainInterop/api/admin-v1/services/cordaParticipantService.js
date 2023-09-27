function init(config, crosschainApplicationSDK){
  const service = {
    getCordaParticipant: async function getCordaParticipant(systemId, foreignSystemId, publicKey){
      return await crosschainApplicationSDK.isCordaParticipant(systemId, foreignSystemId, publicKey)
    },
    postCordaParticipant: async function postCordaParticipant(systemId, body){
      return await crosschainApplicationSDK.addCordaParticipant(systemId, body.foreignSystemId, body.publicKey)
    },
    deleteCordaParticipant: async function deleteCordaParticipant(systemId, foreignSystemId, publicKey){
      return await crosschainApplicationSDK.removeCordaParticipant(systemId, foreignSystemId, publicKey)
    }
  }
  
  return service
}

module.exports = init
