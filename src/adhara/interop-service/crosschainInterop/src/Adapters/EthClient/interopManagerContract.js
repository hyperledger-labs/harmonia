const InteropManagerJson = require('../../../build/contracts/InteropManager.sol/InteropManager.json')

function init(config, dependencies) {

  const ethClient = dependencies.ethClient
  const web3Store = dependencies.web3Store

  async function findCrosschainFunctionCallEvents(networkName, fromBlock, toBlock) {
    const eventName = 'CrosschainFunctionCall'
    const contractAddress = config[networkName].contracts.interopManager.address
    let eventABI = {}
    let eventSignature = eventName + '('
    for (let item of InteropManagerJson.abi) {
      if (item.name === eventName) {
        eventABI = item
        for (let i in item.inputs) {
          const input = item.inputs[i]
          eventSignature += input.type
          eventSignature += (i < item.inputs.length - 1) ? ',' : ')'
        }
      }
    }
    const web3 = web3Store[networkName]
    const filterTopics = [web3.utils.keccak256(eventSignature)]
    const eventLogs = await ethClient.getPastLogs(fromBlock, toBlock, contractAddress, filterTopics, networkName)
    const decodedEventLogs = []
    for (let log of eventLogs) {
      const decodedLog = web3.eth.abi.decodeLog(eventABI.inputs, log.data)
      decodedEventLogs.push({
        decodedLog,
        blockNumber: log.blockNumber,
        txHash: log.transactionHash,
        data: log.data,
        logIndex: log.logIndex
      })
    }
    return decodedEventLogs
  }

  async function checkConfigForChain(networkName, functionName) {
    if (!config[networkName]) {
      return Promise.reject(Error('No configuration found for chain [' + networkName + '], unable to call [' + functionName + ']'))
    }
    if (config[networkName].type !== 'ethereum') {
      return Promise.reject(Error('Only possible on ethereum chains, unable to call [' + functionName + ']'))
    }
  }

  async function getContractAddress(networkId) {
    const networkName = config.networkIdToNetworkName[networkId]
    return config[networkName].contracts.interopManager.address
  }

  return {
    getContractAddress,
    findCrosschainFunctionCallEvents
  }
}

module.exports = init
