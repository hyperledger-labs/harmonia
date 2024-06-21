module.exports = function (app, settlementInstructionService) {

  app.get('/:networkId/settlementInstructions', async (req, res, next) => {
    /* #swagger.tags = ['SettlementInstructions']
       #swagger.summary = 'Fetch a settlement instruction by operationId, or by tradeId, fromAccount and toAccount.' */
    // #swagger.operationId = "getSettlementInstruction"
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['operationId'] = {
        in: 'query',
        required: false,
        type: 'string'
       }
    */
    /* #swagger.parameters['tradeId'] = {
        in: 'query',
        required: false,
        type: 'string'
       }
    */
    /* #swagger.parameters['fromAccount'] = {
         in: 'query',
         required: false,
         type: 'string'
       }
    */
    /* #swagger.parameters['toAccount'] = {
         in: 'query',
         required: false,
         type: 'string'
       }
    */
    try {
      const networkId = req.params.networkId
      const tradeId = req.query.tradeId
      const fromAccount = req.query.fromAccount
      const toAccount = req.query.toAccount
      const operationId = req.query.operationId

      if (!operationId && (!tradeId || !fromAccount || !toAccount)) {
        // #swagger.responses[400] = { description: "Bad request." }
        return res.status(400).json({
          "error": "Please provide either the [operationId] property, or [tradeId, fromAccount, toAccount] properties"
        })
      }
      const responseToSend = await settlementInstructionService.getSettlementInstruction(networkId, tradeId, fromAccount, toAccount, operationId)
      if (!responseToSend) {
        console.log(`Settlement instruction with properties [networkId: ${networkId}, tradeId: ${tradeId}, fromAccount: ${fromAccount}, toAccount: ${toAccount}, operationId: ${operationId}] was not found`)
        return res.status(400).json({
          "error": "Settlement instruction not found"
        })
      }
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/SettlementInstructionResponse" }, description: "Settlement instruction response." }
      */
      return res.status(200).json(responseToSend)
    } catch (e) {
      console.log(e)
      // #swagger.responses[500] = { description: "An error occurred." }
      return res.status(500).json({
        "error": "Internal Server error, could not get settlement instruction, please contact system admin"
      })
    }
  })

  app.post('/:networkId/settlementInstructions', async (req, res, next) => {
    /* #swagger.tags = ['SettlementInstructions']
       #swagger.summary = 'Add a settlement instruction.' */
    // #swagger.operationId = "postSettlementInstruction"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['settlementInstruction'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/SettlementInstruction" }
       }
    */
    try {
      const signatureOrProof = req.body.signatureOrProof
      // #swagger.responses[400] = { description: "Bad request." }
      if (!!signatureOrProof) {
        if (signatureOrProof.encodedEventData !== undefined) {
          if (signatureOrProof.encodedEventData.length === 0 || !/^[0-9A-Fa-f]+$/.test(signatureOrProof.encodedEventData)) {
            return res.status(400).json({
              "error": "Please set the [signatureOrProof.encodedEventData] property to a valid hex-encoded value"
            })
          }
        } else if (signatureOrProof.encodedSignature !== undefined) {
          if (signatureOrProof.encodedSignature.length === 0 || !/^[0-9A-Fa-f]+$/.test(signatureOrProof.encodedSignature)) {
            return res.status(400).json({
              "error": "Please set the [signatureOrProof.encodedSignature] property to a valid hex-encoded signature"
            })
          }
          if (signatureOrProof.encodedKey !== undefined) {
            if (signatureOrProof.encodedKey.length === 0 || !/^[0-9A-Fa-f]+$/.test(signatureOrProof.encodedKey)) {
              return res.status(400).json({
                "error": "Please set the [signatureOrProof.encodedKey] property to a valid hex-encoded public key"
              })
            }
          }
          if (signatureOrProof.partialMerkleRoot !== undefined) {
            if (!/^[0-9A-Fa-f]+$/.test(signatureOrProof.partialMerkleRoot)) {
              return res.status(400).json({
                "error": "Please clear or set the [signatureOrProof.partialMerkleRoot] property to a valid hex-encoded hash"
              })
            }
          }
        }
      }
      const responseToSend = await settlementInstructionService.postSettlementInstruction(req.params.networkId, req.body)
      /* #swagger.responses[201] = {
         schema: { "$ref": "#/definitions/SettlementProof" }, description: "Created settlement instruction." }
      */
      return res.status(201).json(responseToSend)
    } catch (e) {
      console.log(e)
      // #swagger.responses[500] = { description: "An error occurred." }
      return res.status(500).json({
        "error": "Internal Server error, could not create settlement instruction. please contact system admin"
      })
    }
  })

  app.patch('/:networkId/settlementInstructions', async (req, res, next) => {
    /* #swagger.tags = ['SettlementInstructions']
       #swagger.summary = 'Update a settlement instruction by tradeId, fromAccount and toAccount.' */
    // #swagger.operationId = "patchSettlementInstruction"
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['tradeId'] = {
         in: 'query',
         required: false,
         type: 'string'
       }
    */
    /* #swagger.parameters['fromAccount'] = {
         in: 'query',
         required: false,
         type: 'string'
       }
    */
    /* #swagger.parameters['toAccount'] = {
         in: 'query',
         required: false,
         type: 'string'
       }
    */
    /* #swagger.parameters['updateSettlementInstruction'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/UpdateSettlementInstruction" }
       }
    */
    try {
      const networkId = req.params.networkId
      const tradeId = req.query.tradeId
      const fromAccount = req.query.fromAccount
      const toAccount = req.query.toAccount

			// #swagger.responses[400] = { description: "Bad request." }
      if (!tradeId) {
        return res.status(400).json({
          "error": "Please provide tradeId"
        })
      }

      const responseToSend = await settlementInstructionService.patchSettlementInstruction(networkId, tradeId, fromAccount, toAccount, req.body)
      if (!responseToSend) {
        console.log(`Settlement instruction not found with networkId: ${networkId}, tradeId: ${tradeId}, fromAccount: ${fromAccount}, toAccount: ${toAccount}`)
        return res.status(400).json({
          "error:": "Settlement instruction not found"
        })
      }
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/SettlementInstructionResponse" }, description: "Settlement instruction response." }
      */
      return res.status(200).json(responseToSend)
    } catch (e) {
      console.log(e)
      // #swagger.responses[500] = { description: "An error occurred." }
      return res.status(500).json({
        "error": "Internal Server error, could not update settlement instruction, please contact system admin"
      })
    }
  })

  app.delete('/:networkId/settlementInstructions', async (req, res, next) => {
    /* #swagger.tags = ['SettlementInstructions']
       #swagger.summary = 'Delete a settlement instruction by operationId, or by tradeId, fromAccount and toAccount.' */
    // #swagger.operationId = "deleteSettlementInstruction"
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['operationId'] = {
         in: 'query',
         required: false,
         type: 'string'
        }
    */
    /* #swagger.parameters['tradeId'] = {
         in: 'query',
         required: false,
         type: 'string'
        }
    */
    /* #swagger.parameters['fromAccount'] = {
         in: 'query',
         required: false,
         type: 'string'
        }
    */
    /* #swagger.parameters['toAccount'] = {
         in: 'query',
         required: false,
         type: 'string'
       }
    */
    try {
      const networkId = req.params.networkId
      const tradeId = req.query.tradeId
      const fromAccount = req.query.fromAccount
      const toAccount = req.query.toAccount
      const operationId = req.query.operationId

			// #swagger.responses[400] = { description: "Bad request." }
      if (!operationId && (!tradeId || !fromAccount || !toAccount)) {
        return res.status(400).json({
          "error": "Please provide either the operationId, or tradeId, fromAccount and toAccount"
        })
      }

      const responseToSend = await settlementInstructionService.deleteSettlementInstruction(networkId, tradeId, fromAccount, toAccount, operationId)
      if (!responseToSend) {
        console.log(`Settlement instruction not found with networkId: ${networkId}, tradeId: ${tradeId}, fromAccount: ${fromAccount}, toAccount: ${toAccount}, operationId: ${operationId}`)
        return res.status(400).json({
          "error:": "Settlement instruction not found"
        })
      }
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/SettlementInstructionResponse" }, description: "Settlement instruction response." }
      */
      return res.status(200).json(responseToSend)
    } catch (e) {
      console.log(e)
      // #swagger.responses[500] = { description: "An error occurred." }
      return res.status(500).json({
        "error": "Internal Server error, could not remove settlement instruction, please contact system admin"
      })
    }
  })

};



