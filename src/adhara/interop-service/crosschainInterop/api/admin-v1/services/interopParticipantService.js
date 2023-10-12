
function init(config, crosschainApplicationSDK){
  const interopParticipantService = {
    getLocalAccountId: async function getInteropParticipants(systemId, foreignAccountId){
      return await crosschainApplicationSDK.getForeignAccountIdToLocalAccountId(systemId, {foreignAccountId})
    },
    postInteropParticipants: async function postInteropParticipants(systemId, body){
      return await crosschainApplicationSDK.setForeignAccountIdToLocalAccountId(systemId, body)
    }
  }
  
  return interopParticipantService
}

module.exports = init

