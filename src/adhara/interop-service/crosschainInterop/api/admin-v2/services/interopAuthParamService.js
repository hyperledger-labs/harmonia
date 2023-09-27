
function init(config, crosschainApplicationSDK) {
  const interopAuthParamService = {
    getInteropAuthParams: async function getInteropAuthParams(systemId, foreignSystemId, foreignContractAddress) {
      return await crosschainApplicationSDK.isAuthParams(systemId, foreignSystemId, foreignContractAddress)
    },
    postInteropAuthParams: async function postInteropAuthParams(systemId, body) {
      return await crosschainApplicationSDK.addAuthParams(systemId, body.foreignSystemId, body.foreignContractAddress)
    },
    deleteInteropAuthParams: async function deleteInteropAuthParams(systemId, body) {
      return await crosschainApplicationSDK.removeAuthParams(systemId, body.foreignSystemId, body.foreignContractAddress)
    }
  }

  return interopAuthParamService
}

module.exports = init

