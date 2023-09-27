module.exports = function (app, cordaNotaryService) {

  app.get('/:systemId/cordaNotaries', async (req, res, next) => {
    /* #swagger.tags = ['CordaNotaries']
       #swagger.summary = 'Fetch if is a Corda Notary.' */
    // #swagger.operationId = "getCordaNotary"
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['foreignSystemId'] = {
        in: 'query',
        required: true,
        type: 'number'
       }
    */
    /* #swagger.parameters['publicKey'] = {
        in: 'query',
        required: true,
        type: 'string'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { isNotary: true }, description: "Fetched if is a Corda Notary." }
      */
      const responseToSend = await cordaNotaryService.getCordaNotary(req.params.systemId, req.query.foreignSystemId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get corda notary, please contact system admin")
    }
  })

  app.post('/:systemId/cordaNotaries', async (req, res, next) => {
    /* #swagger.tags = ['CordaNotaries']
       #swagger.summary = 'Create a corda notary.' */
    // #swagger.operationId = "postCordaNotary"
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['cordaNotary'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/CordaNotary" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/CordaNotary" }, description: "Created corda notary." }
      */
      const responseToSend = await cordaNotaryService.postCordaNotary(req.params.systemId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create corda notary, please contact system admin")
    }
  })

  app.delete('/:systemId/cordaNotaries', async (req, res, next) => {
    /* #swagger.tags = ['CordaNotaries']
       #swagger.summary = 'Remove a corda notary.' */
    // #swagger.operationId = "deleteCordaNotary"
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['foreignSystemId'] = {
         in: 'query',
         required: true,
         type: 'number'
       }
    */
    /* #swagger.parameters['publicKey'] = {
         in: 'query',
         required: true,
         type: 'string'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/CordaNotary" }, description: "Removed corda notary." }
      */
      const responseToSend = await cordaNotaryService.deleteCordaNotary(req.params.systemId, req.query.foreignSystemId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove corda notary, please contact system admin")
    }
  })

};



