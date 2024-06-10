module.exports = function (app, cordaParticipantService) {

  app.get('/:networkId/cordaParticipants', async (req, res, next) => {
    /* #swagger.tags = ['CordaParticipants']
       #swagger.summary = 'Fetched if is a Corda participant.' */
    // #swagger.operationId = "getCordaParticipant"
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
         schema: { isParticipant: true }, description: "Fetched if is a Corda participant." }
      */
      const responseToSend = await cordaParticipantService.getCordaParticipant(req.params.networkId, req.query.remoteNetworkId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get corda participant, please contact system admin")
    }
  })

  app.post('/:networkId/cordaParticipants', async (req, res, next) => {
    /* #swagger.tags = ['CordaParticipants']
       #swagger.summary = 'Create a corda participant.' */
    // #swagger.operationId = "postCordaParticipant"
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['cordaParticipant'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/CordaParticipant" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/CordaParticipant" }, description: "Created corda participant." }
      */
      const responseToSend = await cordaParticipantService.postCordaParticipant(req.params.networkId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create corda participant, please contact system admin")
    }
  })

  app.delete('/:networkId/cordaParticipants', async (req, res, next) => {
    /* #swagger.tags = ['CordaParticipants']
       #swagger.summary = 'Remove a corda participant.' */
    // #swagger.operationId = "deleteCordaParticipant"
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
         schema: { "$ref": "#/definitions/CordaParticipant" }, description: "Removed corda participant." }
      */
      const responseToSend = await cordaParticipantService.deleteCordaParticipant(req.params.networkId, req.query.remoteNetworkId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove corda participant, please contact system admin")
    }
  })

};



