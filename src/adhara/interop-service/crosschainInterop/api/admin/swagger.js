const options = { swagger: "2.0", disableLogs: false }
const swaggerAutogen = require('swagger-autogen')(options)


const doc = {
  info: {
    version: "0.0.1",
    title: "Administration API",
    description: "Crosschain interop service Administration API defines endpoints for maintaining the Corda Notaries, Corda Participants, Corda Registered Functions, Interop Authentication Parameters, Interop Participants, and Validators resources."
  },
  host: "localhost:3031",
  basePath: "/",
  schemes: ['http', 'https'],
  consumes: ['application/json'],
  produces: ['application/json'],
  tags: [
    {
      "name": "Validators",
      "description": "Validators Resource"
    },
    {
      "name": "InteropAuthParams",
      "description": "Interop Authentication Parameters Resource"
    },
    {
      "name": "InteropParticipants",
      "description": "Interop Participants Resource"
    },
    {
      "name": "CordaNotaries",
      "description": "Corda Notaries Resource"
    },
    {
      "name": "CordaParticipants",
      "description": "Corda Participants Resource"
    },
    {
      "name": "CordaRegisteredFunctions",
      "description": "Corda Registered Functions Resource"
    },
    {
      "name": "ValidatorUpdateInstructions",
      "description": "Validator Update Instruction Resource"
    },
    {
      "name": "ValidatorSetInstructions",
      "description": "Validator Set Instruction Resource"
    }
  ],
  definitions: {
    Validators: {
      networkId: 0,
      ethereumAddresses: ["string"]
    },
    InteropAuthParam: {
      $remoteNetworkId: "string",
      $remoteContractAddress: "string"
    },
    InteropParticipant: {
      $localAccountId: "string",
      $remoteAccountId: "string"
    },
    CordaNotary: {
      $remoteNetworkId: 0,
      $publicKey: "string"
    },
    CordaParticipant: {
      $remoteNetworkId: 0,
      $publicKey: "string"
    },
    CordaParameterHandler: {
      fingerprint: "string",
      componentIndex: 0,
      describedSize: 0,
      describedType: "string",
      describedPath: "string",
      solidityType: "string",
      parser: "string"
    },
    CordaParameterHandlers: [
      { $ref: "#/definitions/CordaParameterHandler" }
    ],
    CordaRegisteredFunction: {
      networkId: 0,
      functionsSignature:  0,
      parameterHandlers: [
        { $ref: "#/definitions/CordaParameterHandler" }
      ]
    },
    EthereumProof: {
      networkId: "string",
      sourceNetworkId: 0,
      encodedInfo: 0,
      signatureOrProof: "string",
    },
    ValidatorUpdateInstructionFilter: {
      remoteDestinationNetworkId: 0,
      callbackURL: "string"
    },
    ValidatorUpdateInstructionRequest: {
      $operationId: "string",
      filters:  [
        { $ref: "#/definitions/ValidatorUpdateInstructionFilter" }
      ]
    },
    ValidatorUpdateInstructionResponse: {
      networkId: 0,
      operationId: "string",
      state: "string",
      creationDate: 0,
      lastUpdate: 0,
      humanReadableTimestamp: "string",
      result: [
        { $ref: "#/definitions/EthereumProof" }
      ]
    },
    ValidatorSetInstructionFilter: {
      remoteDestinationNetworkId: 0,
      callbackURL: "string"
    },
    ValidatorSetInstructionRequest: {
      $operationId: "string",
      $validators: ["string"],
      filters:  [
        { $ref: "#/definitions/ValidatorSetInstructionFilter" }
      ]
    },
    ValidatorSetInstructionResponse: {
      networkId: 0,
      operationId: "string",
      state: "string",
      creationDate: 0,
      lastUpdate: 0,
      humanReadableTimestamp: "string",
      result: [
        { $ref: "#/definitions/EthereumProof" }
      ]
    },
  }
}

const outputFile = './swagger-output.json'
const endpointsFiles = ['./paths/validators.js', './paths/{networkId}/cordaNotaries.js', './paths/{networkId}/cordaParticipants.js', './paths/{networkId}/cordaRegisteredFunctions.js', './paths/{networkId}/interopParticipants.js', './paths/{networkId}/interopAuthParams.js', './paths/{networkId}/validatorUpdateInstructions.js', './paths/{networkId}/validatorSetInstructions.js']

swaggerAutogen(outputFile, endpointsFiles, doc).then(() => {
  require('./index')           // project's root file
})
