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
    }
  ],
  definitions: {
    Validators: {
      chainId: 0,
      ethereumAddresses: ["string"]
    },
    InteropAuthParam: {
      $foreignSystemId: "string",
      $foreignContractAddress: "string"
    },
    InteropParticipant: {
      $localAccountId: "string",
      $foreignAccountId: "string"
    },
    CordaNotary: {
      $foreignSystemId: 0,
      $publicKey: "string"
    },
    CordaParticipant: {
      $foreignSystemId: 0,
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
      systemId: 0,
      functionsSignature:  0,
      parameterHandlers: [
        { $ref: "#/definitions/CordaParameterHandler" }
      ]
    },
    ValidatorUpdateInstructionRequest: {
      $foreignSystemId: 1,
      $operationId: "string",
      blockHeader: "string",
      contractAddress: "string",
      callbackURL: "string",
    },
    ValidatorUpdateInstructionResponse: {
      systemId: 0,
      foreignSystemId: 1,
      operationId: "string",
      state: "string",
      creationDate: 0,
      lastUpdate: 0,
      humanReadableTimestamp: "string"
    },
  }
}

const outputFile = './swagger-output.json'
const endpointsFiles = ['./paths/validators.js', './paths/{systemId}/cordaNotaries.js', './paths/{systemId}/cordaParticipants.js', './paths/{systemId}/cordaRegisteredFunctions.js', './paths/{systemId}/interopParticipants.js', './paths/{systemId}/interopAuthParams.js', './paths/{systemId}/validatorUpdateInstructions.js']

swaggerAutogen(outputFile, endpointsFiles, doc).then(() => {
  require('./index')           // project's root file
})
