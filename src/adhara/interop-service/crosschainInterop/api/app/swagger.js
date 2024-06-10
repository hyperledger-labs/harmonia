const options = { swagger: "2.0", disableLogs: false }
const swaggerAutogen = require('swagger-autogen')(options)


const doc = {
  info: {
    version: "0.0.1",
    title: "Application API",
    description: "Crosschain interop service application API defines endpoints for SettlementObligations, SettlementInstructions, and RepoObligations."
  },
  host: "localhost:3030",
  basePath: "/",
  schemes: ['http', 'https'],
  consumes: ['application/json'],
  produces: ['application/json'],
  tags: [
    {
      "name": "SettlementObligations",
      "description": "Settlement Obligations Resource"
    },
    {
      "name": "SettlementInstructions",
      "description": "Settlement Instructions Resource"
    },
    {
      "name": "RepoObligations",
      "description": "Repo Obligations Resource"
    }
  ],
  definitions: {
    RepoObligation: {
      $tradeId: "string",
      notional: 0,
      openingLeg: {
        fromAccount: "string",
        toAccount: "string",
        amount: 0
      },
      closingLeg: {
        fromAccount: "string",
        toAccount: "string",
        amount: 0,
        timestamp: 0
      }
    },
    SettlementObligation: {
      $tradeId: "string",
      $fromAccount: "string",
      $toAccount: "string",
      currency: "string",
      $amount: 0
    },
    SettlementObligationResponse: {
      operationId: "string"
    },
    SettlementInstruction: {
      remoteNetworkId: 1,
      $tradeId: "string",
      $fromAccount: "string",
      $toAccount: "string",
      currency: "string",
      $amount: 0,
      callbackURL: "string",
      $triggerLeadLeg: true,
      $useExistingEarmark: true,
      useForCancellation: false,
      signatureOrProof: {
        sourceNetworkId: 0,
        encodedEventData: "string",
        encodedKey: "string",
        encodedSignature: "string",
        partialMerkleRoot: "string",
        platformVersion: 0,
        schemaNumber: 0,
      }
    },
    SettlementInstructionResponse: {
      networkId: 0,
      remoteNetworkId: 1,
      tradeId: "string",
      operationId: "string",
      fromAccount: "string",
      toAccount: "string",
      currency: "string",
      amount: 0,
      callbackURL: "string",
      triggerLeadLeg: true,
      useExistingEarmark: true,
      useForCancellation: false,
      signatureOrProof: {
        sourceNetworkId: 0,
        encodedEventData: "string"
      },
      state: "string",
      creationDate: 0,
      lastUpdate: 0,
      humanReadableTimestamp: "string"
    },
    UpdateSettlementInstruction: {
      state:  "string"
    },
    SettlementProof: {
      tradeId: "string",
      networkId: 0,
      sourceNetworkId: 0,
      encodedInfo: "string",
      signatureOrProof: "string"
    }
  }
}

const outputFile = './swagger-output.json'
const endpointsFiles = ['./paths/{networkId}/repoObligations.js', './paths/{networkId}/settlementInstructions.js', './paths/{networkId}/settlementObligations.js']

swaggerAutogen(outputFile, endpointsFiles, doc).then(() => {
  require('./index')           // project's root file
})
