const { v4: uuidv4 } = require('uuid');
const fs = require('fs')
const path = require('path')
const assert = require('assert');
const Logger = require('../src/CrosschainSDKUtils/logger.js')
const Graph = require('../src/RunGraph')
const opId = function () { return uuidv4().substring(0,16) }

const config = require('../config/config.json');
config.logLevel = !!process.env.log_level && process.env.log_level.length > 0 ? process.env.log_level : 'silent'
const logger = Logger(config, {})

describe('validator set updates', function () {

	const testDBDirectory = 'test-db-' + uuidv4().substring(0, 8)
	config.fileDBDirectory = testDBDirectory
	logger.log('debug', 'Create test DB [' + testDBDirectory + ']')

	const graph = Graph(config, { logger })
	const crosschainApplicationSDK = graph.crosschainApplicationSDK

	const system0Id = 0
	const system1Id = 1
	const system2Id = 2

	before(async function () {
	})

	after(async function () {
		const dbDirectory = path.resolve(testDBDirectory)
		fs.rmSync(dbDirectory, { force: true, recursive: true })
		logger.log('debug', 'Removed test DB [' + testDBDirectory + ']')
		await crosschainApplicationSDK.stop()
	})

	step('should be able to set local validator set', async function () {
		let validators = ['0xca31306798b41bc81c43094a1e0462890ce7a673']
		let result = await crosschainApplicationSDK.setValidators(system1Id, validators, opId())
		assert.equal(result.transactionState, 'SUCCESS')
		validators = await crosschainApplicationSDK.getValidators(system1Id)
		assert.ok(validators.length > 0, "Failed to set validators in validator set manager")
	});

	step('should be able to get the local validator set and sync remote validator sets', async function () {
		let submission = await crosschainApplicationSDK.submitValidatorUpdateInstruction(system1Id, {
			operationId: opId(),
		})
		assert.equal(submission[0].networkId, system0Id)
		assert.equal(submission[0].sourceNetworkId, system1Id)
		assert.equal(submission[0].encodedInfo.length > 0, true)
		assert.equal(submission[0].signatureOrProof.length > 0, true)
		assert.equal(submission[1].networkId, system2Id)
		assert.equal(submission[1].sourceNetworkId, system1Id)
		assert.equal(submission[1].encodedInfo.length > 0, true)
		assert.equal(submission[1].signatureOrProof.length > 0, true)
	});

	step('should be able to get the local validator set and sync remote validator sets with filters', async function () {
		let submission = await crosschainApplicationSDK.submitValidatorUpdateInstruction(system1Id, {
			operationId: opId(),
			filters: [{
				remoteDestinationNetworkId: system2Id
			}]
		})
		assert.equal(submission[0].networkId, system2Id)
		assert.equal(submission[0].sourceNetworkId, system1Id)
		assert.equal(submission[0].encodedInfo.length > 0, true)
		assert.equal(submission[0].signatureOrProof.length > 0, true)
	});

	step('should be able to set the local validator set and sync remote validator sets', async function () {
		const validators = await crosschainApplicationSDK.getValidators(system1Id)
		let submission = await crosschainApplicationSDK.submitValidatorSetInstruction(system1Id, {
			operationId: opId(),
			validators: validators
		})
		assert.equal(submission[0].networkId, system0Id)
		assert.equal(submission[0].sourceNetworkId, system1Id)
		assert.equal(submission[0].encodedInfo.length > 0, true)
		assert.equal(submission[0].signatureOrProof.length > 0, true)
		assert.equal(submission[1].networkId, system2Id)
		assert.equal(submission[1].sourceNetworkId, system1Id)
		assert.equal(submission[1].encodedInfo.length > 0, true)
		assert.equal(submission[1].signatureOrProof.length > 0, true)
	});

	step('should be able to set the local validator set and sync remote validator sets with filters', async function () {
		const validators = await crosschainApplicationSDK.getValidators(system1Id)
		let submission = await crosschainApplicationSDK.submitValidatorSetInstruction(system1Id, {
			operationId: opId(),
			validators: validators,
			filters: [{
				remoteDestinationNetworkId: system0Id
			}]
		})
		assert.equal(submission[0].networkId, system0Id)
		assert.equal(submission[0].sourceNetworkId, system1Id)
		assert.equal(submission[0].encodedInfo.length > 0, true)
		assert.equal(submission[0].signatureOrProof.length > 0, true)
	});

	step('should not be able to set the local validator set with an empty set', async function () {
		assert.rejects(async function () {
			await crosschainApplicationSDK.submitValidatorSetInstruction(system1Id, {
				operationId: opId(),
				validators: []
			})
		})
	});

});



