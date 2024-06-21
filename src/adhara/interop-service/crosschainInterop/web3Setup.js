const {setupNetwork, setupIntegration, updateConfig} = require("./web3Deploy")
const path = require('path')
const w3 = require('web3')
const web3 = new w3()

const cordaRequestFollowLeg =
{
  prototype: 'requestFollowLeg(string,string,string,address,uint256,uint256)',
  command: 'net.corda.samples.example.contracts.DCRContract$Commands$Earmark',
  signature: web3.eth.abi.encodeFunctionSignature('requestFollowLeg(string,string,string,address,uint256,uint256)'),
  handlers: [{
    'fingerprint': 'net.corda:DldW9yS4tBOze6qv6U4QTA==',
    'componentIndex': '0x00',
    'describedSize': '0x08',
    'describedType': 'String',
    'describedPath': '0x06',
    'solidityType': 'string',
    'calldataPath': '0x00',
    'parser': 'PathParser',
  }, {
    'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
    'componentIndex': '0x01',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x00',
    'solidityType': 'string',
    'calldataPath': '0x01',
    'parser': 'PartyParser',
  }, {
    'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
    'componentIndex': '0x00',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x00',
    'solidityType': 'string',
    'calldataPath': '0x02',
    'parser': 'PartyParser',
  }, {
    'fingerprint': '',
    'componentIndex': '0x00',
    'describedSize': '0x00',
    'describedType': '',
    'describedPath': '0x00',
    'solidityType': 'address',
    'calldataPath': '0x03',
    'parser': 'NoParser',
  }, {
    'fingerprint': '',
    'componentIndex': '0x00',
    'describedSize': '0x00',
    'describedType': '',
    'describedPath': '0x00',
    'solidityType': 'uint256',
    'calldataPath': '0x04',
    'parser': 'NoParser',
  }, {
    'fingerprint': 'net.corda:DldW9yS4tBOze6qv6U4QTA==',
    'componentIndex': '0x00',
    'describedSize': '0x08',
    'describedType': 'String',
    'describedPath': '0x07',
    'solidityType': 'uint256',
    'calldataPath': '0x05',
    'parser': 'PathParser',
  }]
}
const cordaPerformCancellation =
{
  prototype: 'performCancellation(string,string,string)',
  command: 'net.corda.samples.example.contracts.XVPContract$Commands$Cancel',
  signature: web3.eth.abi.encodeFunctionSignature('performCancellation(string,string,string)'),
  handlers: [{
    'fingerprint': 'net.corda:9GdANdKRptKFtq6zQDfG+A==',
    'componentIndex': '0x00',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x05',
    'solidityType': 'string',
    'calldataPath': '0x00',
    'parser': 'PathParser',
  }, {
    'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
    'componentIndex': '0x00',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x00',
    'solidityType': 'string',
    'calldataPath': '0x01',
    'parser': 'PartyParser',
  }, {
    'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
    'componentIndex': '0x01',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x00',
    'solidityType': 'string',
    'calldataPath': '0x02',
    'parser': 'PartyParser',
  }]
}
const cordaParameterHandlers = [cordaRequestFollowLeg, cordaPerformCancellation];

// Specific test setup will set up an Ethereum network to work with an Adhara Corda App and Corda transaction based proofs.
async function main() {
  let configGBP = {
    networkName: 'bc-local-gbp',
    networkId: 44844,
    ethProvider: 'http://localhost:8545',
    web3Provider: 'http://localhost:4545',
    logLevel: 'silent',
    deployerAddress: '0x049eb617fBa599E3D455Da70C6730ABc8Cc4221d',
    localNetworkId: 1, // Local Ethereum ledger
    remoteNetworks: [{
      cordaNetworkId: 0, // Remote Corda ledger
      cordaPartyAKey: '0x4af61fb05f9e463bcef152ef95fe4fba92421af37b242f4cbfeb4f7c1c7ad9eb',
      cordaPartyALocalId: 'HTUSUS00GBP',
      cordaPartyARemoteId: 'Tz1QYXJ0eUEsIEw9TG9uZG9uLCBDPUdC', // O=PartyA, L=London, C=GB
      cordaPartyBKey: '0x1f782a61439579aa0fadcca18e8874df875a13ef4678d6c6797abf2671473d05',
      cordaPartyBLocalId: 'HTGBGB00GBP',
      cordaPartyBRemoteId: 'Tz1QYXJ0eUIsIEw9TmV3IFlvcmssIEM9VVM=', // O=PartyB, L=New York, C=US
      cordaNotaryKey: '0x3b05648573efb5a73a35295b0946806615b53d8804c63cc58c7f12f60994f8f0',
      cordaParameterHandlers: cordaParameterHandlers
    }, {
      ethNetworkId: 2, // Remote Ethereum ledger
      ethValidatorAddresses: ['0xca31306798b41bc81c43094a1e0462890ce7a673'],
      ethPartyALocalId: 'HTGBGB00GBP',
      ethPartyARemoteId: 'HTGBGB00USD',
      ethPartyBLocalId: 'HTUSUS00GBP',
      ethPartyBRemoteId: 'HTUSUS00USD'
    }],
    holdNotaryId: 'NOTARY00XVP',
    tokenAccount: 'HTGBGB00GBP',
    tokenAmount: '100000000000'
  }
  let configUSD = {
    networkName: 'bc-local-usd',
    networkId: 55755,
    ethProvider: 'http://localhost:7545',
    web3Provider: 'http://localhost:4545',
    logLevel: 'silent',
    deployerAddress: '0x06c3f482f18711be95adf106afa25cd13897fbe7',
    localNetworkId: 2, // Local Ethereum ledger
    remoteNetworks: [{
      cordaNetworkId: 0, // Remote Corda ledger
      cordaPartyAKey: '0x4af61fb05f9e463bcef152ef95fe4fba92421af37b242f4cbfeb4f7c1c7ad9eb',
      cordaPartyALocalId: 'HTGBGB00USD',
      cordaPartyARemoteId: 'Tz1QYXJ0eUEsIEw9TG9uZG9uLCBDPUdC', // O=PartyA, L=London, C=GB
      cordaPartyBKey: '0x1f782a61439579aa0fadcca18e8874df875a13ef4678d6c6797abf2671473d05',
      cordaPartyBLocalId: 'HTUSUS00USD',
      cordaPartyBRemoteId: 'Tz1QYXJ0eUIsIEw9TmV3IFlvcmssIEM9VVM=', // O=PartyB, L=New York, C=US
      cordaNotaryKey: '0x3b05648573efb5a73a35295b0946806615b53d8804c63cc58c7f12f60994f8f0',
      cordaParameterHandlers: cordaParameterHandlers
    }, {
      ethNetworkId: 1, // Remote Ethereum ledger
      ethValidatorAddresses: ['0xca31306798b41bc81c43094a1e0462890ce7a673'],
      ethPartyALocalId: 'HTGBGB00USD',
      ethPartyARemoteId: 'HTGBGB00GBP',
      ethPartyBLocalId: 'HTUSUS00USD',
      ethPartyBRemoteId: 'HTUSUS00GBP'
    }],
    holdNotaryId: 'NOTARY00XVP',
    tokenAccount: 'HTUSUS00USD',
    tokenAmount: '100000000000'
  }

  let deployedGBP = await setupNetwork(configGBP)
  await updateConfig(path.resolve('./config/config.json'), configGBP.localNetworkId, deployedGBP)
  let deployedUSD = await setupNetwork(configUSD)
  await updateConfig(path.resolve('./config/config.json'), configUSD.localNetworkId, deployedUSD)

  configGBP.remoteNetworks[0].connectorContract = '0x0000000000000000000000000000000000000000'
  configGBP.remoteNetworks[1].authenticatedContracts = [deployedUSD.crosschainXvP.address, deployedUSD.validatorSetManager.address]
  configGBP.remoteNetworks[1].connectorContract = deployedUSD.crosschainMessaging.address
  //console.log(JSON.stringify(configGBP))
  await setupIntegration(configGBP, deployedGBP)

  configUSD.remoteNetworks[0].connectorContract = '0x0000000000000000000000000000000000000000'
  configUSD.remoteNetworks[1].authenticatedContracts = [deployedGBP.crosschainXvP.address, deployedGBP.validatorSetManager.address]
  configUSD.remoteNetworks[1].connectorContract = deployedGBP.crosschainMessaging.address
  //console.log(JSON.stringify(configUSD))
  await setupIntegration(configUSD, deployedUSD)
}

main()
