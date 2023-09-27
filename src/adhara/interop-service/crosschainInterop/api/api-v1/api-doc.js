const apiDoc = {
  swagger: "2.0",
  basePath: "/v1",
  info: {
    title: "Crosschain interop service API.",
    version: "0.0.1",
  },
  definitions: {
    RepoObligation: {
      type: "object",
      properties: {
        tradeId: {
          type: "string"
        },
        notional: {
          type: "number"
        },
        openingLeg: {
          type: "object",
          properties: {
            fromAccount: {
              type: "string"
            },
            toAccount: {
              type: "string"
            },
            amount: {
              type: "number"
            },
          },
        },
        closingLeg: {
          type: "object",
          properties: {
            fromAccount: {
              type: "string"
            },
            toAccount: {
              type: "string"
            },
            amount: {
              type: "number"
            },
            timestamp: {
              type: "number"
            },
          },
        },
      },
      required: ["tradeId"],
    },
    SettlementObligation: {
      type: "object",
      properties: {
        tradeId: {
          type: "string"
        },
        fromAccount: {
          type: "string"
        },
        toAccount: {
          type: "string"
        },
        currency: {
          type: "string"
        },
        amount: {
          type: "number"
        },
      },
      required: ["tradeId", "fromAccount", "toAccount", "amount"],
    },
    SettlementObligationResponse: {
      type: "object",
      properties: {
        operationId: {
          type: "string"
        },
      },
      required: [],
    },
    SettlementInstruction: {
      type: "object",
      properties: {
        foreignSystemId: {
          type: "number"
        },
        tradeId: {
          type: "string"
        },
        fromAccount: {
          type: "string"
        },
        toAccount: {
          type: "string"
        },
        currency: {
          type: "string"
        },
        amount: {
          type: "number"
        },
        callbackURL:{
          type: "string"
        },
        triggerLeadLeg: {
          type: "boolean"
        },
        useExistingEarmark: {
          type: "boolean"
        },
        useForCancellation: {
          type: "boolean"
        },
        signatureOrProof:{
          type: "object",
          properties:{
            sourceSystemId: {
              type: "number"
            },
            encodedEventData: {
              type: "string"
            },
            encodedKey: {
              type: "string"
            },
            encodedSignature: {
              type: "string"
            },
            partialMerkleRoot: {
              type: "string"
            },
            platformVersion: {
              type: "number"
            },
            schemaNumber: {
              type: "number"
            }
          }
        }
      },
      required: ['tradeId', 'fromAccount', 'toAccount', 'amount', 'triggerLeadLeg', 'useExistingEarmark'],
    },
    SettlementInstructionResponse: {
      type: "object",
      properties: {
        systemId: {
          type: "number"
        },
        foreignSystemId: {
          type: "number"
        },
        tradeId: {
          type: "string"
        },
        operationId: {
          type: "string"
        },
        fromAccount: {
          type: "string"
        },
        toAccount: {
          type: "string"
        },
        currency: {
          type: "string"
        },
        amount: {
          type: "number"
        },
        callbackURL:{
          type: "string"
        },
        triggerLeadLeg: {
          type: "boolean"
        },
        useExistingEarmark: {
          type: "boolean"
        },
        useForCancellation: {
          type: "boolean"
        },
        signatureOrProof:{
          type: "object",
          properties:{
            sourceSystemId: {
              type: "number"
            },
            encodedEventData: {
              type: "string"
            }
          }
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
    UpdateSettlementInstruction: {
      type: "object",
        properties: {
          state: {
            type: "string"
          },
        },
    },
    SettlementProof: {
      type: "object",
      properties: {
        tradeId: {
          type: "string"
        },
        systemId: {
          type: "number"
        },
        sourceSystemId: {
          type: "number"
        },
        encodedInfo: {
          type: "string"
        },
        signatureOrProof: {
          type: "string"
        },
      },
      required: [],
    },
  },
  paths: {},
};

module.exports = apiDoc;
