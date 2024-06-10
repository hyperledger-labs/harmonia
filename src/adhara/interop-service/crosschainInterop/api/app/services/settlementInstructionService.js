function init(config, crosschainApplicationSDK){
  const settlementInstructionService = {
    getSettlementInstruction: async function getSettlementInstruction(networkId, tradeId, fromAccount, toAccount, operationId){
      return await crosschainApplicationSDK.getSettlementInstruction(networkId, tradeId, fromAccount, toAccount, operationId)
    },
    postSettlementInstruction: async function postSettlementInstruction(networkId, body){
      return await crosschainApplicationSDK.submitSettlementInstruction(networkId, body)
    },
    deleteSettlementInstruction: async function deleteSettlementInstruction(networkId, tradeId, fromAccount, toAccount, operationId){
      return await crosschainApplicationSDK.deleteSettlementInstruction(networkId, tradeId, fromAccount, toAccount, operationId)
    },
    patchSettlementInstruction: async function patchSettlementInstruction(networkId, tradeId, fromAccount, toAccount, body){
      return await crosschainApplicationSDK.patchSettlementInstruction(networkId, tradeId, fromAccount, toAccount, body)
    }
  }
  return settlementInstructionService
}

module.exports = init


