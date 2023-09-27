function init(config, crosschainApplicationSDK){
  const service = {
    getCordaNotary: async function getCordaNotary(systemId, foreignSystemId, publicKey){
      return await crosschainApplicationSDK.isCordaNotary(systemId, foreignSystemId, publicKey)
    },
    postCordaNotary: async function postCordaNotary(systemId, body){
      return await crosschainApplicationSDK.addCordaNotary(systemId, body.foreignSystemId, body.publicKey)
    },
    deleteCordaNotary: async function deleteCordaNotary(systemId, foreignSystemId, publicKey){
      return await crosschainApplicationSDK.removeCordaNotary(systemId, foreignSystemId, publicKey)
    }
  }
  
  return service
}

module.exports = init
