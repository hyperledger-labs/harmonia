
function init(config, crosschainApplicationSDK) {
  const interopAuthParamService = {
    getInteropAuthParams: async function getInteropAuthParams(networkId, remoteNetworkId, remoteContractAddress) {
      return await crosschainApplicationSDK.isAuthParams(networkId, remoteNetworkId, remoteContractAddress)
    },
    postInteropAuthParams: async function postInteropAuthParams(networkId, body) {
      return await crosschainApplicationSDK.addAuthParams(networkId, body.remoteNetworkId, body.remoteContractAddress)
    },
    deleteInteropAuthParams: async function deleteInteropAuthParams(networkId, body) {
      return await crosschainApplicationSDK.removeAuthParams(networkId, body.remoteNetworkId, body.remoteContractAddress)
    }
  }

  return interopAuthParamService
}

module.exports = init

