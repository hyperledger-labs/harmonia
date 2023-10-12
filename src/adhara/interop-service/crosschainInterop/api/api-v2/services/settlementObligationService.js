function init(config, crosschainApplicationSDK){
  const settlementObligationService = {
    postSettlementObligation: async function postSettlementObligation(systemId, body){
      console.log("createSettlementObligation to crosschainApplicationSDK")
      return await crosschainApplicationSDK.createSettlementObligation(systemId, body)
    }
  }
  
  return settlementObligationService
}

module.exports = init


