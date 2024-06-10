module.exports = function (app, validatorSetInstructionService) {

  app.get('/:networkId/validatorSetInstructions', async (req, res, next) => {
    /* #swagger.tags = ['ValidatorSetInstructions']
       #swagger.summary = 'Fetch the validator set instruction.' */
    // #swagger.operationId = "getValidatorSetInstruction"
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
         schema: { "$ref": "#/definitions/ValidatorSetInstructionResponse" }, description: "Fetched the validator set instruction." }
      */
      const responseToSend = await validatorSetInstructionService.getValidatorSetInstruction(req.params.networkId, req.query.operationId)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get validator update instruction, please contact system admin")
    }
  })

  app.post('/:networkId/validatorSetInstructions', async (req, res, next) => {
    /* #swagger.tags = ['ValidatorSetInstructions']
       #swagger.summary = 'Create validator set instruction.' */
    // #swagger.operationId = "postValidatorSetInstruction"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'number'
       }
    */
    /* #swagger.parameters['validatorSetInstruction'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/ValidatorSetInstructionRequest" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/ValidatorSetInstructionResponse" }, description: "Created validator update instruction." }
      */
      const responseToSend = await validatorSetInstructionService.postValidatorSetInstruction(req.params.networkId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create validator set instruction, please contact system admin")
    }
  })

  app.delete('/:networkId/validatorSetInstructions', async (req, res, next) => {
    /* #swagger.tags = ['ValidatorSetInstructions']
       #swagger.summary = 'Remove validator set instruction.' */
    // #swagger.operationId = "deleteValidatorSetInstruction"
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
         schema: { "$ref": "#/definitions/ValidatorSetInstructionResponse" }, description: "Removed validator set instruction." }
      */
      const responseToSend = await validatorSetInstructionService.deleteValidatorSetInstruction(req.params.networkId, req.query.operationId)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove validator set instruction, please contact system admin")
    }
  })

};



