module.exports = function (app, interopParticipantService) {

  app.get('/:systemId/interopParticipants', async (req, res, next) => {
    /* #swagger.tags = ['InteropParticipants']
       #swagger.summary = 'Fetch the interop participant.' */
    // #swagger.operationId = "getLocalAccountId"
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['foreignAccountId'] = {
        in: 'query',
        required: true,
        type: 'number'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/InteropParticipant" }, description: "Fetched the interop participant." }
      */
      const responseToSend = await interopParticipantService.getLocalAccountId(req.params.systemId, req.query.foreignAccountId)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get interop participant, please contact system admin")
    }
  })

  app.post('/:systemId/interopParticipants', async (req, res, next) => {
    /* #swagger.tags = ['InteropParticipants']
       #swagger.summary = 'Create interop participant.' */
    // #swagger.operationId = "postInteropParticipants"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['interopParticipant'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/InteropParticipant" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/InteropParticipant" }, description: "Created interop participant." }
      */
      const responseToSend = await interopParticipantService.postInteropParticipants(req.params.systemId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create interop participant, please contact system admin")
    }
  })

  app.delete('/:systemId/interopParticipants', async (req, res, next) => {
    /* #swagger.tags = ['InteropParticipants']
       #swagger.summary = 'Remove interop participant.' */
    // #swagger.operationId = "deleteInteropParticipant"
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['interopParticipant'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/InteropParticipant" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/InteropParticipant" }, description: "Removed interop participant." }
      */
      const responseToSend = await interopParticipantService.deleteInteropParticipant(req.params.systemId, req.query.foreignAccountId)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove interop participant, please contact system admin")
    }
  })

};



