function init(config, crosschainApplicationSDK){
  const settlementObligationService = {
    postSettlementObligation: async function postSettlementObligation(networkId, body){
      console.log("createSettlementObligation to crosschainApplicationSDK")
      return await crosschainApplicationSDK.createSettlementObligation(networkId, body)
    }
  }
  
  return settlementObligationService
}

module.exports = init


