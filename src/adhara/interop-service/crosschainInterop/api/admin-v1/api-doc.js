const apiDoc = {
  swagger: "2.0",
  basePath: "/v1",
  info: {
    title: "Crosschain interop service admin API.",
    version: "0.0.1",
  },
  definitions: {
    Validators: {
      type: "object",
      properties: {
        chainId: {
          type: "number"
        },
        ethereumAddresses: {
          type: "array",
          items:{
            type: "string"
          }
        },
      },
      required: [],
    },
    InteropAuthParams: {
      type: "object",
      properties: {
        foreignSystemId: {
          type: "string",
        },
        foreignContractAddress: {
          type: "string",
        },
      },
      required: ["foreignSystemId", "foreignContractAddress"],
    },
    InteropParticipants: {
      type: "object",
      properties: {
        localAccountId: {
          type: "string",
        },
        foreignAccountId: {
          type: "string",
        },
      },
      required: ["localAccountId", "foreignAccountId"],
    },
    CordaNotary: {
      type: "object",
      properties: {
        foreignSystemId: {
          type: "number",
        },
        publicKey: {
          type: "string",
        },
      },
      required: ["publicKey", "foreignSystemId"],
    },
    CordaParticipant: {
      type: "object",
      properties: {
        foreignSystemId: {
          type: "number",
        },
        publicKey: {
          type: "string",
        },
      },
      required: ["publicKey", "foreignSystemId"],
    },
    CordaParameterHandler: {
      type: "object",
      properties: {
        fingerprint: {
          type: "string",
        },
        componentIndex: {
          type: "number",
        },
        describedSize: {
          type: "number",
        },
        describedType: {
          type: "string",
        },
        describedPath: {
          type: "string",
        },
        solidityType: {
          type: "string",
        },
        parser: {
          type: "string",
        },
      },
      required: [],
    },
    CordaParameterHandlers: {
      type: "array",
      items: {
        $ref: "#/definitions/CordaParameterHandler",
      },
    },
    CordaRegisteredFunction: {
      type: "object",
      properties: {
        systemId: {
          type: "number",
        },
        functionsSignature: {
          type: "number",
        },
        parameterHandlers: {
          type: "array",
          items: {
            $ref: "#/definitions/CordaParameterHandler",
          },
        }
      }
    },
    ValidatorUpdateInstructionRequest: {
      type: "object",
      properties: {
        foreignSystemId: {
          type: "string"
        },
        operationId: {
          type: "string"
        },
        blockHeader: {
          type: "string"
        },
        contractAddress: {
          type: "string"
        },
        callbackURL: {
          type: "string"
        },
      },
      required: ['foreignSystemId', 'operationId'],
    },
    ValidatorUpdateInstructionResponse: {
      type: "object",
      properties: {
        systemId: {
          type: "number"
        },
        foreignSystemId: {
          type: "string"
        },
        operationId: {
          type: "string"
        },
        state: {
          type: "string"
        },
        creationDate: {
          type: "number"
        },
        lastUpdate: {
          type: "number"
        },
        humanReadableTimestamp: {
          type: "string"
        },
      },
    },
  },
  paths: {},
};

module.exports = apiDoc;
