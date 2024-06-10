function init(config, crosschainApplicationSDK){
  const repoObligationService = {
    postRepoObligation: async function postRepoObligation(networkId, body){
      console.log("createRepoObligation to crosschainApplicationSDK")
      return await crosschainApplicationSDK.createRepoObligation(networkId, body)
    }
  }

  return repoObligationService
}

module.exports = init



