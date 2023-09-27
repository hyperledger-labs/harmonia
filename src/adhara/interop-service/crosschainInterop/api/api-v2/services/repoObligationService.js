function init(config, crosschainApplicationSDK){
  const repoObligationService = {
    postRepoObligation: async function postRepoObligation(systemId, body){
      console.log("createRepoObligation to crosschainApplicationSDK")
      return await crosschainApplicationSDK.createRepoObligation(systemId, body)
    }
  }
  
  return repoObligationService
}

module.exports = init



