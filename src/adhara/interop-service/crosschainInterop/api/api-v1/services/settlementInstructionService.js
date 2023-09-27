function init(config, crosschainApplicationSDK){
  const settlementInstructionService = {
    getSettlementInstruction: async function getSettlementInstruction(systemId, tradeId, fromAccount, toAccount, operationId){
      return await crosschainApplicationSDK.getSettlementInstruction(systemId, tradeId, fromAccount, toAccount, operationId)
    },
    postSettlementInstruction: async function postSettlementInstruction(systemId, body){
      return await crosschainApplicationSDK.submitSettlementInstruction(systemId, body)
    },
    deleteSettlementInstruction: async function deleteSettlementInstruction(systemId, tradeId, fromAccount, toAccount, operationId){
      return await crosschainApplicationSDK.deleteSettlementInstruction(systemId, tradeId, fromAccount, toAccount, operationId)
    },
    patchSettlementInstruction: async function patchSettlementInstruction(systemId, tradeId, fromAccount, toAccount, body){
      return await crosschainApplicationSDK.patchSettlementInstruction(systemId, tradeId, fromAccount, toAccount, body)
    }
  }
  return settlementInstructionService
}

module.exports = init


