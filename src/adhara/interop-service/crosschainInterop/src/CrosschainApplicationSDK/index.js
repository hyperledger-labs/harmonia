const abiDecoder = require('abi-decoder')

function init(config, dependencies) {
  const logger = dependencies.logger
  const web3Store = dependencies.web3Store
  const crosschainMessagingContract = dependencies.crosschainMessagingContract
  const crosschainFunctionCallContract = dependencies.crosschainFunctionCallContract
  const crosschainXVPContract = dependencies.crosschainXVPContract
  const assetTokenContract = dependencies.assetTokenContract
  const validatorSetManagerContract = dependencies.validatorSetManagerContract
  const interopManagerContract = dependencies.interopManagerContract
  const settlementObligationsAdapter = dependencies.settlementObligations

  if (dependencies.CrosschainXvPJson) abiDecoder.addABI(dependencies.CrosschainXvPJson.abi)
  if (dependencies.CrosschainFunctionCallJson) abiDecoder.addABI(dependencies.CrosschainFunctionCallJson.abi)
  if (dependencies.AssetTokenJson) abiDecoder.addABI(dependencies.AssetTokenJson.abi)

  const helpers = require('./helpers.js')(config, {
    logger,
    web3Store
  })
  const validators = require('./validators.js')(config, {
    logger,
    web3Store
  })

  // Settlement instruction database
  const settlementInstructionDb = require('./settlementInstructionDb')(config, {
    logger
  });
  const settlementObligations = require('./settlementObligations.js')(config,{
    logger,
    web3Store,
    helpers,
    crosschainXVPContract,
    assetTokenContract,
    settlementObligationsAdapter
  });
  const settlementInstructions = require('./settlementInstructions')(config, {
    logger,
    web3Store,
    helpers,
    crosschainXVPContract,
    crosschainFunctionCallContract,
    assetTokenContract,
    db: settlementInstructionDb,
    settlementObligations,
    crosschainFunctionCallSDK: dependencies.crosschainFunctionCallSDK,
    CrosschainXvPJson: dependencies.CrosschainXvPJson,
    CrosschainFunctionCallJson: dependencies.CrosschainFunctionCallJson,
    AssetTokenJson: dependencies.AssetTokenJson,
  });
  const repoObligations = require('./repoObligations')({
    settlementInstructions
  });

  // Validator update database
  const validatorUpdateInstructionDb = require('./validatorUpdateInstructionDb')(config, {
    logger
  });
  const validatorUpdateInstructions = require('./validatorUpdateInstructions')(config, {
    logger,
    web3Store,
    db: validatorUpdateInstructionDb,
    crosschainFunctionCallSDK: dependencies.crosschainFunctionCallSDK,
    CrosschainMessagingJson: dependencies.CrosschainMessagingJson,
    InteropManagerJson: dependencies.InteropManagerJson,
    crosschainFunctionCallContract,
    crosschainMessagingContract,
    validatorSetManagerContract,
    interopManagerContract,
  });

  const validatorSetInstructionDb = require('./validatorSetInstructionDb')(config, {
    logger
  });
  const validatorSetInstructions = require('./validatorSetInstructions')(config, {
    logger,
    web3Store,
    db: validatorSetInstructionDb,
    crosschainFunctionCallSDK: dependencies.crosschainFunctionCallSDK,
    CrosschainMessagingJson: dependencies.CrosschainMessagingJson,
    InteropManagerJson: dependencies.InteropManagerJson,
    crosschainFunctionCallContract,
    crosschainMessagingContract,
    validatorSetManagerContract,
    interopManagerContract,
  });

  settlementInstructions.start()
  validatorUpdateInstructions.start()
  validatorSetInstructions.start()

  async function stop() {
    await settlementInstructions.stop()
    await validatorUpdateInstructions.stop()
    await validatorSetInstructions.stop()
  }

  return {
    getAvailableBalanceOf: assetTokenContract.getAvailableBalanceOf,
    destroyTokens: helpers.destroyTokens,
    createTokens: helpers.createTokens,
    createHoldAndMakePerpetual: helpers.createHoldAndMakePerpetual,
    startCancellation: settlementInstructions.startCancellation,
    performCancellation: settlementInstructions.performCancellation,
    startLeadLeg: settlementInstructions.startLeadLeg,
    setRemoteAccountIdToLocalAccountId: crosschainXVPContract.setRemoteAccountIdToLocalAccountId,
    getRemoteAccountIdToLocalAccountId: crosschainXVPContract.getRemoteAccountIdToLocalAccountId,
    getSettlementInstruction: settlementInstructions.getSettlementInstruction,
    submitSettlementInstruction: settlementInstructions.submitSettlementInstruction,
    deleteSettlementInstruction: settlementInstructions.deleteSettlementInstruction,
    patchSettlementInstruction: settlementInstructions.patchSettlementInstruction,
    createSettlementObligation: settlementObligations.createSettlementObligation,
    createRepoObligation: repoObligations.createRepoObligation,
    getValidatorsByBlockhash: validators.getValidatorsByBlockHash,
    getAllValidators: validators.getAllValidators,
    addCordaNotary: crosschainMessagingContract.addNotary,
    removeCordaNotary: crosschainMessagingContract.removeNotary,
    isCordaNotary: crosschainMessagingContract.isNotary,
    addCordaParticipant: crosschainMessagingContract.addParticipant,
    removeCordaParticipant: crosschainMessagingContract.removeParticipant,
    isCordaParticipant: crosschainMessagingContract.isParticipant,
    setCordaParameterHandlers: crosschainMessagingContract.setParameterHandlers,
    removeCordaParameterHandlers: crosschainMessagingContract.removeParameterHandlers,
    getCordaParameterHandler: crosschainMessagingContract.getParameterHandler,
    setLocalNetworkId: crosschainFunctionCallContract.setLocalNetworkId,
    getLocalNetworkId: crosschainFunctionCallContract.getLocalNetworkId,
    setAppendAuthParams: crosschainFunctionCallContract.setAppendAuthParams,
    getAppendAuthParams: crosschainFunctionCallContract.getAppendAuthParams,
    addAuthParams: crosschainFunctionCallContract.addAuthParams,
    isAuthParams: crosschainFunctionCallContract.isAuthParams,
    removeAuthParams: crosschainFunctionCallContract.removeAuthParams,
    getValidators: validatorSetManagerContract.getValidators,
    getValidatorsAndSyncRemotes: validatorSetManagerContract.getValidatorsAndSyncRemotes,
    setValidators: validatorSetManagerContract.setValidators,
    setValidatorsAndSyncRemotes: validatorSetManagerContract.setValidatorsAndSyncRemotes,
    getValidatorSetInstruction: validatorSetInstructions.getValidatorSetInstruction,
    submitValidatorSetInstruction: validatorSetInstructions.submitValidatorSetInstruction,
    deleteValidatorSetInstruction: validatorSetInstructions.deleteValidatorSetInstruction,
    getValidatorUpdateInstruction: validatorUpdateInstructions.getValidatorUpdateInstruction,
    submitValidatorUpdateInstruction: validatorUpdateInstructions.submitValidatorUpdateInstruction,
    deleteValidatorUpdateInstruction: validatorUpdateInstructions.deleteValidatorUpdateInstruction,
    stop: stop,
    crosschainXVPContract,
    crosschainFunctionCallContract,
    helpers,
  }
}

module.exports = init
