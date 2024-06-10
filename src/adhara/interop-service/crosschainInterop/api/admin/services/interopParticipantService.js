
function init(config, crosschainApplicationSDK){
  const interopParticipantService = {
    getLocalAccountId: async function getInteropParticipants(networkId, remoteAccountId){
      return await crosschainApplicationSDK.getRemoteAccountIdToLocalAccountId(networkId, {remoteAccountId})
    },
    postInteropParticipants: async function postInteropParticipants(networkId, body){
      return await crosschainApplicationSDK.setRemoteAccountIdToLocalAccountId(networkId, body)
    },
    deleteInteropParticipants: async function deleteInteropParticipants(networkId, body){
      // todo - add function to remove the mapping
      return await crosschainApplicationSDK.getRemoteAccountIdToLocalAccountId(networkId, {remoteAccountId})
    }
  }

  return interopParticipantService
}

module.exports = init

