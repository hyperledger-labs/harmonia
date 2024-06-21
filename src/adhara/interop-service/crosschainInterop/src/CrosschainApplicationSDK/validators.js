const fetch = require('node-fetch');

function init(config, dependencies) {

  async function getBlockByBlockHash(networkName, blockHash) {
    try {
      const response = await fetch(config[networkName].httpProvider, {
        method: 'POST',
        body: '{"jsonrpc":"2.0", "method":"eth_getBlockByHash", "params":["' + blockHash + '", false], "id":"53"}'
      })
      return await response.json()
    } catch (error) {
      return Promise.reject(Error(error))
    }
  }

  async function getValidatorsByBlockNumber(networkName, blockNumber) {
    try {
      let consensusString = 'qbft'
      if (!!config[networkName] && config[networkName].consensus === 'ibft') {
        consensusString = 'ibft'
      }
      const response = await fetch(config[networkName].httpProvider, {
        method: 'POST',
        body: '{"jsonrpc":"2.0", "method":"' + consensusString + '_getValidatorsByBlockNumber", "params":["' + blockNumber + '", false], "id":"53"}'
      })
      const jsonResponse = await response.json()
      if (!!jsonResponse.error) {
        return Promise.reject(Error(JSON.stringify(jsonResponse)))
      }
      return jsonResponse
    } catch (error) {
      return Promise.reject(Error(error))
    }
  }

  // Returns the validators for a single chain, if the block hash belongs to a chain that has been configured
  async function getValidatorsByBlockHash(blockHash) {
    for (let networkName of config.chains) {
      // Only run this for ethereum chains
      if (config[networkName].type !== 'ethereum') {
        continue
      }
      // First lookup the block number, if null then this block hash doesn't exist on this chain
      const block = await getBlockByBlockHash(networkName, blockHash)
      if (!!block && !!block.result && !!block.result.number) {
        const validatorResult = await getValidatorsByBlockNumber(networkName, block.result.number)
        if (!validatorResult || !validatorResult.result) {
          return Promise.reject(Error(`Could not retrieve validators from chain ${networkName} for block number ${block.result.number}`))
        }
        // This ensures that only a single chain's validators will be returned
        const obj = {
          networkId: config[networkName].id,
          ethereumAddresses: validatorResult.result,
        }
        return obj
      }
    }
    // The expectation is that the block hash should exist on at least one chain
    return Promise.reject(Error(`Block hash ${blockHash} not found on any known chain: ${config.chains}`))
  }

  async function getAllValidators() {
    let list = []
    for (let networkName of config.chains) {
      // Only run this for ethereum chains
      if (config[networkName].type !== 'ethereum') {
        continue
      }
      const validatorResult = await getValidatorsByBlockNumber(networkName, 'latest')
      if (!validatorResult || !validatorResult.result) {
        return Promise.reject(Error(`Could not retrieve validators from chain ${networkName} for block number 'latest'`))
      }
      const obj = {
        networkId: config[networkName].id,
        ethereumAddresses: validatorResult.result,
      }
      list.push(obj)
    }
    return list
  }

  return {
    getValidatorsByBlockNumber,
    getValidatorsByBlockHash,
    getAllValidators
  }
}

module.exports = init
