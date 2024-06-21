module.exports = function (app, cordaRegisteredFunctionService) {

  app.get('/:networkId/cordaRegisteredFunctions', async (req, res, next) => {
    /* #swagger.tags = ['CordaRegisteredFunctions']
       #swagger.summary = 'Fetch the parameter handler for a registered function.' */
    // #swagger.operationId = "getRegisteredFunction"
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['remoteNetworkId'] = {
        in: 'query',
        required: true,
        type: 'number'
       }
    */
    /* #swagger.parameters['functionSignature'] = {
        in: 'query',
        required: true,
        type: 'number'
       }
    */
    /* #swagger.parameters['index'] = {
        in: 'query',
        required: true,
        type: 'number'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/CordaParameterHandler" }, description: "Fetched the parameter handler for a registered function." }
      */
      const responseToSend = await cordaRegisteredFunctionService.getRegisteredFunction(req.params.networkId, req.query.remoteNetworkId, req.query.functionSignature, req.query.index)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get corda registered function, please contact system admin")
    }
  })

  app.post('/:networkId/cordaRegisteredFunctions', async (req, res, next) => {
    /* #swagger.tags = ['CordaRegisteredFunctions']
       #swagger.summary = 'Create a Corda registered function.' */
    // #swagger.operationId = "postRegisteredFunction"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['remoteNetworkId'] = {
         in: 'query',
         required: true,
         type: 'number'
       }
    */
    /* #swagger.parameters['functionSignature'] = {
         in: 'query',
         required: true,
         type: 'number'
       }
    */
    /* #swagger.parameters['functionParameterHandlers'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/CordaParameterHandlers" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/CordaRegisteredFunction" }, description: "Created a Corda registered function." }
      */
      const responseToSend = await cordaRegisteredFunctionService.postRegisteredFunction(req.params.networkId, req.query.remoteNetworkId, req.query.functionSignature, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create corda registered function, please contact system admin")
    }
  })

  app.delete('/:networkId/cordaRegisteredFunctions', async (req, res, next) => {
    /* #swagger.tags = ['CordaRegisteredFunctions']
       #swagger.summary = 'Remove a Corda registered function.' */
    // #swagger.operationId = "deleteRegisteredFunction"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['remoteNetworkId'] = {
         in: 'query',
         required: true,
         type: 'number'
       }
    */
    /* #swagger.parameters['functionSignature'] = {
         in: 'query',
         required: true,
         type: 'number'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/CordaRegisteredFunction" }, description: "Removed a Corda registered function." }
      */
      const responseToSend = await cordaRegisteredFunctionService.deleteRegisteredFunction(req.params.networkId, req.query.remoteNetworkId, req.query.functionSignature)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove corda registered function, please contact system admin")
    }
  })

};



