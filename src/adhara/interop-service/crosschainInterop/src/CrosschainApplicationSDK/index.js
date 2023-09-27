const abiDecoder = require('abi-decoder')

function init(config, dependencies) {
  const logger = dependencies.logger
  const web3Store = dependencies.web3Store
  const crosschainMessagingContract = dependencies.crosschainMessagingContract
  const crosschainFunctionCallContract = dependencies.crosschainFunctionCallContract
  const crosschainXVPContract = dependencies.crosschainXVPContract
  const assetTokenContract = dependencies.assetTokenContract
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
    db: validatorUpdateInstructionDb,
    crosschainFunctionCallSDK: dependencies.crosschainFunctionCallSDK,
    CrosschainFunctionCallJson: dependencies.CrosschainFunctionCallJson,
    crosschainFunctionCallContract,
    crosschainMessagingContract
  });

  settlementInstructions.start()
  validatorUpdateInstructions.start()

  return {
    getAvailableBalanceOf: assetTokenContract.getAvailableBalanceOf,
    destroyTokens: helpers.destroyTokens,
    createTokens: helpers.createTokens,
    createHoldAndMakePerpetual: helpers.createHoldAndMakePerpetual,
    startCancellation: settlementInstructions.startCancellation,
    performCancellation: settlementInstructions.performCancellation,
    startLeadLeg: settlementInstructions.startLeadLeg,
    setForeignAccountIdToLocalAccountId: crosschainXVPContract.setForeignAccountIdToLocalAccountId,
    getForeignAccountIdToLocalAccountId: crosschainXVPContract.getForeignAccountIdToLocalAccountId,
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
    setSystemId: crosschainFunctionCallContract.setSystemId,
    getSystemId: crosschainFunctionCallContract.getSystemId,
    setAppendAuthParams: crosschainFunctionCallContract.setAppendAuthParams,
    getAppendAuthParams: crosschainFunctionCallContract.getAppendAuthParams,
    addAuthParams: crosschainFunctionCallContract.addAuthParams,
    isAuthParams: crosschainFunctionCallContract.isAuthParams,
    removeAuthParams: crosschainFunctionCallContract.removeAuthParams,
    getValidatorUpdateInstruction: validatorUpdateInstructions.getValidatorUpdateInstruction,
    submitValidatorUpdateInstruction: validatorUpdateInstructions.submitValidatorUpdateInstruction,
    stop: settlementInstructions.stop,
    crosschainXVPContract,
    crosschainFunctionCallContract,
    helpers,
  }
}

module.exports = init
