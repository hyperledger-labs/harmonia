module.exports = function (app, cordaNotaryService) {

  app.get('/:networkId/cordaNotaries', async (req, res, next) => {
    /* #swagger.tags = ['CordaNotaries']
       #swagger.summary = 'Fetch if is a Corda Notary.' */
    // #swagger.operationId = "getCordaNotary"
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
      const responseToSend = await cordaNotaryService.getCordaNotary(req.params.networkId, req.query.remoteNetworkId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get corda notary, please contact system admin")
    }
  })

  app.post('/:networkId/cordaNotaries', async (req, res, next) => {
    /* #swagger.tags = ['CordaNotaries']
       #swagger.summary = 'Create a corda notary.' */
    // #swagger.operationId = "postCordaNotary"
    /* #swagger.parameters['networkId'] = {
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
      const responseToSend = await cordaNotaryService.postCordaNotary(req.params.networkId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create corda notary, please contact system admin")
    }
  })

  app.delete('/:networkId/cordaNotaries', async (req, res, next) => {
    /* #swagger.tags = ['CordaNotaries']
       #swagger.summary = 'Remove a corda notary.' */
    // #swagger.operationId = "deleteCordaNotary"
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
      const responseToSend = await cordaNotaryService.deleteCordaNotary(req.params.networkId, req.query.remoteNetworkId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove corda notary, please contact system admin")
    }
  })

};



