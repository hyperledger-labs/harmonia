const utils = require("../CrosschainSDKUtils");

function init(config, dependencies) {
  const logger = dependencies.logger

  async function getValidators(networkId) {
    const networkName = config.networkIdToNetworkName[networkId]
    if (!config[networkName]) {
      return Promise.reject(Error('No configuration found for ' + networkName + ', unable to set validators'))
    }
    if (config[networkName].type !== 'ethereum') {
      return Promise.reject(Error('Only possible to set validators on ethereum chains'))
    }
    const context = config[networkName].context.admin

    let validators = await utils.call(context, config[networkName].contracts.validatorSetManager.path, 'getValidators', [])

    if (!validators) {
      return Promise.reject(Error('Failed to retrieve validator set from validator set manager'))
    }
    logger.log('debug', 'Retrieved validators: ' + JSON.stringify(validators))
    return validators
  }

  return {
    getValidators
  }
}

module.exports = init
