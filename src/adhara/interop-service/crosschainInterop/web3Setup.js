const {setupNetwork, setupIntegration, updateConfig} = require("./web3Deploy")
const path = require('path')

const cordaRequestFollowLeg =
{
  signature: '0xc6755b7c',
  handlers: [{
    'fingerprint': 'net.corda:DldW9yS4tBOze6qv6U4QTA==',
    'componentIndex': '0x00',
    'describedSize': '0x08',
    'describedType': 'String',
    'describedPath': '0x06',
    'solidityType': 'string',
    'parser': 'PathParser',
  }, {
    'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
    'componentIndex': '0x01',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x00',
    'solidityType': 'string',
    'parser': 'PartyParser',
  }, {
    'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
    'componentIndex': '0x00',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x00',
    'solidityType': 'string',
    'parser': 'PartyParser',
  }, {
    'fingerprint': '',
    'componentIndex': '0x00',
    'describedSize': '0x00',
    'describedType': '',
    'describedPath': '0x00',
    'solidityType': 'address',
    'parser': 'NoParser',
  }, {
    'fingerprint': '',
    'componentIndex': '0x00',
    'describedSize': '0x00',
    'describedType': '',
    'describedPath': '0x00',
    'solidityType': 'uint256',
    'parser': 'NoParser',
  }, {
    'fingerprint': 'net.corda:DldW9yS4tBOze6qv6U4QTA==',
    'componentIndex': '0x00',
    'describedSize': '0x08',
    'describedType': 'String',
    'describedPath': '0x07',
    'solidityType': 'uint256',
    'parser': 'PathParser',
  }]
}
const cordaPerformCancellation =
{
  signature: '0xca2f0452',
  handlers: [{
    'fingerprint': 'net.corda:9GdANdKRptKFtq6zQDfG+A==',
    'componentIndex': '0x00',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x05',
    'solidityType': 'string',
    'parser': 'PathParser',
  }, {
    'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
    'componentIndex': '0x00',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x00',
    'solidityType': 'string',
    'parser': 'PartyParser',
  }, {
    'fingerprint': 'net.corda:ngdwbt6kRT0l5nn16uf87A==',
    'componentIndex': '0x01',
    'describedSize': '0x06',
    'describedType': 'String',
    'describedPath': '0x00',
    'solidityType': 'string',
    'parser': 'PartyParser',
  }]
}
const cordaParameterHandlers = [cordaRequestFollowLeg, cordaPerformCancellation];

// Specific test setup will set up an Ethereum chain to work with an Adhara Corda App and Corda transaction based proofs.
async function main() {
  let configGBP = {
    chainName: 'bc-local-gbp',
    chainId: 44844,
    ethProvider: 'http://localhost:8545',
    web3Provider: 'http://localhost:4545',
    logLevel: 'silent',
    deployerAddress: '0x049eb617fBa599E3D455Da70C6730ABc8Cc4221d',
    localSystemId: 1, // Local Ethereum ledger
    foreignSystems: [{
      cordaSystemId: 0, // Foreign Corda ledger
      cordaPartyAKey: '0xefa467877cc5342571d38157ffc7ddcd90a961b719f68416a65f1eac8073392c',
      cordaPartyALocalId: 'HTUSUS00GBP',
      cordaPartyAForeignId: 'Tz1QYXJ0eUEsIEw9TG9uZG9uLCBDPUdC', // O=PartyA, L=London, C=GB
      cordaPartyBKey: '0xad275912d4c90443cab3a70438c6c53e69938b310f7634b7e14df0c61248227a',
      cordaPartyBLocalId: 'HTGBGB00GBP',
      cordaPartyBForeignId: 'Tz1QYXJ0eUIsIEw9TmV3IFlvcmssIEM9VVM=', // O=PartyB, L=New York, C=US
      cordaNotaryKey: '0xdfe9207470bd2e8bd906e6f1a0a6dc3cba6305c3eb6d235cf380a68306fd678c',
      cordaParameterHandlers: cordaParameterHandlers
    }, {
      ethSystemId: 2, // Foreign Ethereum ledger
      ethValidatorAddresses: ['0xca31306798b41bc81c43094a1e0462890ce7a673'],
      ethPartyALocalId: 'HTGBGB00GBP',
      ethPartyAForeignId: 'HTGBGB00USD',
      ethPartyBLocalId: 'HTUSUS00GBP',
      ethPartyBForeignId: 'HTUSUS00USD'
    }],
    holdNotaryId: 'NOTARY00XVP',
    tokenAccount: 'HTGBGB00GBP',
    tokenAmount: '100000000000'
  }
  let configUSD = {
    chainName: 'bc-local-usd',
    chainId: 55755,
    ethProvider: 'http://localhost:7545',
    web3Provider: 'http://localhost:4545',
    logLevel: 'silent',
    deployerAddress: '0x06c3f482f18711be95adf106afa25cd13897fbe7',
    localSystemId: 2, // Local Ethereum ledger
    foreignSystems: [{
      cordaSystemId: 0, // Foreign Corda ledger
      cordaPartyAKey: '0xefa467877cc5342571d38157ffc7ddcd90a961b719f68416a65f1eac8073392c',
      cordaPartyALocalId: 'HTGBGB00USD',
      cordaPartyAForeignId: 'Tz1QYXJ0eUEsIEw9TG9uZG9uLCBDPUdC', // O=PartyA, L=London, C=GB
      cordaPartyBKey: '0xad275912d4c90443cab3a70438c6c53e69938b310f7634b7e14df0c61248227a',
      cordaPartyBLocalId: 'HTUSUS00USD',
      cordaPartyBForeignId: 'Tz1QYXJ0eUIsIEw9TmV3IFlvcmssIEM9VVM=', // O=PartyB, L=New York, C=US
      cordaNotaryKey: '0xdfe9207470bd2e8bd906e6f1a0a6dc3cba6305c3eb6d235cf380a68306fd678c',
      cordaParameterHandlers: cordaParameterHandlers
    }, {
      ethSystemId: 1, // Foreign Ethereum ledger
      ethValidatorAddresses: ['0xca31306798b41bc81c43094a1e0462890ce7a673'],
      ethPartyALocalId: 'HTGBGB00USD',
      ethPartyAForeignId: 'HTGBGB00GBP',
      ethPartyBLocalId: 'HTUSUS00USD',
      ethPartyBForeignId: 'HTUSUS00GBP'
    }],
    holdNotaryId: 'NOTARY00XVP',
    tokenAccount: 'HTUSUS00USD',
    tokenAmount: '100000000000'
  }

  let deployedGBP = await setupNetwork(configGBP)
  await updateConfig(path.resolve('./config/harmonia-config.json'), configGBP.localSystemId, deployedGBP)
  let deployedUSD = await setupNetwork(configUSD)
  await updateConfig(path.resolve('./config/harmonia-config.json'), configUSD.localSystemId, deployedUSD)

  configGBP.foreignSystems[1].authenticatedContract = deployedUSD.crosschainXvP.address
  //console.log(JSON.stringify(configGBP))
  await setupIntegration(configGBP, deployedGBP)

  configUSD.foreignSystems[1].authenticatedContract = deployedGBP.crosschainXvP.address
  //console.log(JSON.stringify(configUSD))
  await setupIntegration(configUSD, deployedUSD)
}

main()
