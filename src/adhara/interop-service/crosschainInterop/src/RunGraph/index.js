const Web3 = require('web3')

const AssetTokenJson = require('../../build/contracts/Token.sol/Token.json')
const CrosschainXvPJson = require('../../build/contracts/XvP.sol/XvP.json')
const CrosschainMessagingJson = require('../../build/contracts/CrosschainMessaging.sol/CrosschainMessaging.json')
const CrosschainFunctionCallJson = require('../../build/contracts/CrosschainFunctionCall.sol/CrosschainFunctionCall.json')
const FeeManagerJson = require('../../build/contracts/interfaces/IFeeManager.sol/IFeeManager.json');
const InteropManagerJson = require('../../build/contracts/InteropManager.sol/InteropManager.json');

function init(config, dependencies){

  const logger = dependencies.logger

  const web3Store = {}
  for (const chain of config.chains) {
    if (config[chain].type !== 'ethereum') {
      continue
    }

    const web3 = new Web3(config[chain].httpProvider)
    web3.eth.handleRevert = true
    web3Store[chain] = web3
  }

  const ethClient = require('../Infrastructure/EthereumClient')(config, {
    logger,
    web3Store
  })
  // Smart Contract Adapters, using ethClient
  const crosschainFunctionCallContract = require('../Adapters/EthClient/crosschainFunctionCallContract.js')(config, {
    logger,
    web3Store,
    ethClient
  })
  const crosschainXVPContract = require('../Adapters/EthClient/crosschainXVPContract.js')(config, {
    logger,
    web3Store,
    ethClient
  })
  const crosschainMessagingContract = require('../Adapters/EthClient/crosschainMessagingContract.js')(config, {
    logger,
    web3Store,
    ethClient
  })
  const assetTokenContract = require('../Adapters/EthClient/assetTokenContract.js')(config, {
    logger,
    web3Store,
    ethClient
  })
  const validatorSetManagerContract = require('../Adapters/EthClient/validatorSetManagerContract.js')(config, {
    logger,
    web3Store,
    ethClient
  })
  const interopManagerContract = require('../Adapters/EthClient/interopManagerContract.js')(config, {
    logger,
    web3Store,
    ethClient
  })
  const settlementObligations = require('../Adapters/EthClient/settlementObligations.js')(config, {
    logger,
    web3Store,
    ethClient,
    assetTokenContract
  })
  // Main Application Usecases
  const crosschainMessagingSDK = require('../CrosschainMessagingSDK')(config, {
    logger,
    crosschainMessagingContract
  })
  const crosschainFunctionCallSDK = require('../CrosschainFunctionCallSDK')(config, {
    logger,
    web3Store,
    CrosschainXvPJson,
    CrosschainFunctionCallJson,
    crosschainMessagingSDK,
    crosschainFunctionCallContract,
    crosschainXVPContract
  })
  const crosschainApplicationSDK = require('../CrosschainApplicationSDK')(config, {
    logger,
    web3Store,
    AssetTokenJson,
    CrosschainXvPJson,
    CrosschainMessagingJson,
    CrosschainFunctionCallJson,
    FeeManagerJson,
    InteropManagerJson,
    crosschainFunctionCallSDK,
    crosschainMessagingSDK,
    crosschainMessagingContract,
    crosschainFunctionCallContract,
    crosschainXVPContract,
    assetTokenContract,
    validatorSetManagerContract,
    interopManagerContract,
    settlementObligations
  })

  return {
    web3Store,
    ethClient,
    crosschainFunctionCallContract,
    crosschainXVPContract,
    crosschainMessagingContract,
    assetTokenContract,
    crosschainMessagingSDK,
    crosschainFunctionCallSDK,
    crosschainApplicationSDK
  }
}

module.exports = init
