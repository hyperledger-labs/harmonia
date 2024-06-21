module.exports = function (app, validatorUpdateInstructionService) {

  app.get('/:networkId/validatorUpdateInstructions', async (req, res, next) => {
    /* #swagger.tags = ['ValidatorUpdateInstructions']
       #swagger.summary = 'Fetch the validator update instruction.' */
    // #swagger.operationId = "getValidatorUpdateInstruction"
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'number'
       }
    */
    /* #swagger.parameters['operationId'] = {
        in: 'query',
        required: true,
        type: 'string'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/ValidatorUpdateInstructionResponse" }, description: "Fetched the validator update instruction." }
      */
      const responseToSend = await validatorUpdateInstructionService.getValidatorUpdateInstruction(req.params.networkId, req.query.operationId)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get validator update instruction, please contact system admin")
    }
  })

  app.post('/:networkId/validatorUpdateInstructions', async (req, res, next) => {
    /* #swagger.tags = ['ValidatorUpdateInstructions']
       #swagger.summary = 'Create validator update instruction.' */
    // #swagger.operationId = "postValidatorUpdateInstruction"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'number'
       }
    */
    /* #swagger.parameters['validatorUpdateInstruction'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/ValidatorUpdateInstructionRequest" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/ValidatorUpdateInstructionResponse" }, description: "Created validator update instruction." }
      */
      const responseToSend = await validatorUpdateInstructionService.postValidatorUpdateInstruction(req.params.networkId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create validator update instruction, please contact system admin")
    }
  })

  app.delete('/:networkId/validatorUpdateInstructions', async (req, res, next) => {
    /* #swagger.tags = ['ValidatorUpdateInstructions']
       #swagger.summary = 'Remove validator update instruction.' */
    // #swagger.operationId = "deleteValidatorUpdateInstruction"
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'number'
       }
    */
    /* #swagger.parameters['operationId'] = {
        in: 'query',
        required: true,
        type: 'string'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/ValidatorUpdateInstructionResponse" }, description: "Removed validator update instruction." }
      */
      const responseToSend = await validatorUpdateInstructionService.deleteValidatorUpdateInstruction(req.params.networkId, req.query.operationId)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove validator update instruction, please contact system admin")
    }
  })

};



