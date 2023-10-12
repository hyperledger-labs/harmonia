module.exports = function (settlementInstructionService) {
  const operations = {
    GET,
    POST,
    DELETE,
    PATCH
  };

  async function GET(req, res, next) {
    try{
      const systemId = req.params.systemId
      const tradeId = req.query.tradeId
      const fromAccount = req.query.fromAccount
      const toAccount = req.query.toAccount
      const operationId = req.query.operationId

      if(!operationId && (!tradeId || !fromAccount || !toAccount)){
        return res.status(400).json({
          "error":"Please provide either the [operationId] property, or [tradeId, fromAccount, toAccount] properties"
        })
      }
      const responseToSend = await settlementInstructionService.getSettlementInstruction(systemId, tradeId, fromAccount, toAccount, operationId)
      if(!responseToSend){
        console.log(`Settlement instruction with properties [systemId: ${systemId}, tradeId: ${tradeId}, fromAccount: ${fromAccount}, toAccount: ${toAccount}, operationId: ${operationId}] was not found`)
        return res.status(400).json({
          "error:":"Settlement instruction not found"
        })
      }
      return res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      return res.status(500).json({
        "error":"Internal Server error, please contact system admin"
      })
    }
  }

  async function POST(req, res, next) {
    try{
      const signatureOrProof = req.body.signatureOrProof
      if(!!signatureOrProof){
        if(signatureOrProof.encodedEventData !== undefined){
          if(signatureOrProof.encodedEventData.length === 0 || !/^[0-9A-Fa-f]+$/.test(signatureOrProof.encodedEventData)) {
            return res.status(400).json({
              "error": "Please set the [signatureOrProof.encodedEventData] property to a valid hex-encoded value"
            })
          }
        } else if(signatureOrProof.encodedSignature !== undefined){
          if(signatureOrProof.encodedSignature.length === 0 || !/^[0-9A-Fa-f]+$/.test(signatureOrProof.encodedSignature)){
            return res.status(400).json({
              "error": "Please set the [signatureOrProof.encodedSignature] property to a valid hex-encoded signature"
            })
          }
          if(signatureOrProof.encodedKey !== undefined){
            if (signatureOrProof.encodedKey.length === 0 || !/^[0-9A-Fa-f]+$/.test(signatureOrProof.encodedKey)){
              return res.status(400).json({
                "error": "Please set the [signatureOrProof.encodedKey] property to a valid hex-encoded public key"
              })
            }
          }
          if(signatureOrProof.partialMerkleRoot !== undefined) {
            if (!/^[0-9A-Fa-f]+$/.test(signatureOrProof.partialMerkleRoot)) {
              return res.status(400).json({
                "error": "Please clear or set the [signatureOrProof.partialMerkleRoot] property to a valid hex-encoded hash"
              })
            }
          }
        }
      }
      const responseToSend = await settlementInstructionService.postSettlementInstruction(req.params.systemId, req.body)
      return res.status(201).json(responseToSend)
    } catch (e){
      console.log(e)
      return res.status(500).json({
        "error":"Internal Server error, please contact system admin"
      })
    }
  }

  async function DELETE(req, res, next) {
    try{
      const systemId = req.params.systemId
      const tradeId = req.query.tradeId
      const fromAccount = req.query.fromAccount
      const toAccount = req.query.toAccount
      const operationId = req.query.operationId

      if(!operationId && (!tradeId || !fromAccount || !toAccount)){
        return res.status(400).json({
          "error":"Please provide either the operationId, or tradeId, fromAccount and toAccount"
        })
      }

      const responseToSend = await settlementInstructionService.deleteSettlementInstruction(systemId, tradeId, fromAccount, toAccount, operationId)
      if(!responseToSend){
        console.log(`Settlement instruction not found with systemId: ${systemId}, tradeId: ${tradeId}, fromAccount: ${fromAccount}, toAccount: ${toAccount}, operationId: ${operationId}`)
        return res.status(400).json({
          "error:":"Settlement instruction not found"
        })
      }
      return res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      return res.status(500).json({
        "error":"Internal Server error, please contact system admin"
      })
    }
  }

  async function PATCH(req, res, next) {
    try{
      const systemId = req.params.systemId
      const tradeId = req.query.tradeId
      const fromAccount = req.query.fromAccount
      const toAccount = req.query.toAccount

      if (!tradeId) {
        return res.status(400).json({
          "error":"Please provide tradeId"
        })
      }

      const responseToSend = await settlementInstructionService.patchSettlementInstruction(systemId, tradeId, fromAccount, toAccount, req.body)
      if(!responseToSend){
        console.log(`Settlement instruction not found with systemId: ${systemId}, tradeId: ${tradeId}, fromAccount: ${fromAccount}, toAccount: ${toAccount}`)
        return res.status(400).json({
          "error:":"Settlement instruction not found"
        })
      }
      return res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      return res.status(500).json({
        "error":"Internal Server error, please contact system admin"
      })
    }
  }

  GET.apiDoc = {
    summary: "Fetch a settlement instruction by operationId, or by tradeId, fromAccount and toAccount",
    operationId: "getSettlementInstruction",
    parameters: [
      {
        in: "path",
        name: "systemId",
        required: true,
        type: "number",
      },
      {
        in: 'query',
        name: 'operationId',
        required: false,
        type: 'string'
      },
      {
        in: 'query',
        name: 'tradeId',
        required: false,
        type: 'string'
      },
      {
        in: 'query',
        name: 'fromAccount',
        required: false,
        type: 'string'
      },
      {
        in: 'query',
        name: 'toAccount',
        required: false,
        type: 'string'
      }
    ],
    responses: {
      200: {
        description: "Settlement instruction response",
        schema: {
          $ref: "#/definitions/SettlementInstructionResponse",
        },
      },
      400: {
        description: "Bad request",
      },
      500: {
        description: "Internal server error",
      },
      default: {
        description: 'An error occurred.',
        schema: {
          additionalProperties: false
        }
      }
    },
  }

  POST.apiDoc = {
    summary: "Add a settlement instruction",
    operationId: "postSettlementInstruction",
    consumes: ["application/json"],
    parameters: [
      {
        in: "path",
        name: "systemId",
        required: true,
        type: "number",
      },
      {
        in: "body",
        name: "settlementInstruction",
        schema: {
          $ref: "#/definitions/SettlementInstruction",
        },
      }
    ],
    responses: {
      201: {
        description: "Created settlement instruction.",
        schema: {
          $ref: "#/definitions/SettlementProof",
        },
      },
      400: {
        description: "Bad request",
      },
      500: {
        description: "Internal server error",
      },
      default: {
        description: 'An error occurred.',
        schema: {
          additionalProperties: false
        }
      }
    },
  };

  DELETE.apiDoc = {
    summary: "Delete a settlement instruction by operationId, or by tradeId, fromAccount and toAccount",
    operationId: "deleteSettlementInstruction",
    parameters: [
      {
        in: "path",
        name: "systemId",
        required: true,
        type: "number",
      },
      {
        in: 'query',
        name: 'operationId',
        required: false,
        type: 'string'
      },
      {
        in: 'query',
        name: 'tradeId',
        required: false,
        type: 'string'
      },
      {
        in: 'query',
        name: 'fromAccount',
        required: false,
        type: 'string'
      },
      {
        in: 'query',
        name: 'toAccount',
        required: false,
        type: 'string'
      }
    ],
    responses: {
      200: {
        description: "Settlement instruction response",
        schema: {
          $ref: "#/definitions/SettlementInstructionResponse",
        },
      },
      400: {
        description: "Bad request",
      },
      500: {
        description: "Internal server error",
      },
      default: {
        description: 'An error occurred.',
        schema: {
          additionalProperties: false
        }
      }
    },
  };

  PATCH.apiDoc = {
      summary: "Update a settlement instruction by tradeId, fromAccount and toAccount",
      operationId: "patchSettlementInstruction",
      parameters: [
        {
          in: "path",
          name: "systemId",
          required: true,
          type: "number",
        },
        {
          in: 'query',
          name: 'tradeId',
          required: false,
          type: 'string'
        },
        {
          in: 'query',
          name: 'fromAccount',
          required: false,
          type: 'string'
        },
        {
          in: 'query',
          name: 'toAccount',
          required: false,
          type: 'string'
        },
        {
          in: "body",
          name: "updateSettlementInstruction",
          schema: {
            $ref: "#/definitions/UpdateSettlementInstruction",
          },
        }
      ],
      responses: {
        200: {
          description: "Settlement instruction update response",
          schema: {
            type: "object",
            properties: {
              success: {
                type: "boolean"
              },
            },
          },
        },
        400: {
          description: "Bad request",
        },
        500: {
          description: "Internal server error",
        },
        default: {
          description: 'An error occurred.',
          schema: {
            additionalProperties: false
          }
        }
      },
  };


  return operations;
};


