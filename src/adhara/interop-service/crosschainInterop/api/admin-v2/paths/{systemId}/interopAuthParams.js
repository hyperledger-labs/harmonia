module.exports = function (app, interopAuthParamService) {

  app.get('/:systemId/interopAuthParams', async (req, res, next) => {
    /* #swagger.tags = ['InteropAuthParams']
       #swagger.summary = 'Fetch if is interop authentication parameters.' */
    // #swagger.operationId = "getInteropAuthParams"
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
    /* #swagger.parameters['foreignContractAddress'] = {
         in: 'query',
         required: true,
         type: 'string'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { isAuthParam: true }, description: "Fetched if is interop authentication parameters." }
      */
      const responseToSend = await interopAuthParamService.getInteropAuthParams(req.params.systemId, req.query.foreignSystemId, req.query.foreignContractAddress)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get interop authentication parameter, please contact system admin")
    }
  })

  app.post('/:systemId/interopAuthParams', async (req, res, next) => {
    /* #swagger.tags = ['InteropAuthParams']
       #swagger.summary = 'Create interop authentication parameters.' */
    // #swagger.operationId = "postInteropAuthParams"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['interopAuthParam'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/InteropAuthParam" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/InteropAuthParam" }, description: "Created interop authentication parameters." }
      */
      const responseToSend = await interopAuthParamService.postInteropAuthParams(req.params.systemId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create interop authentication parameter, please contact system admin")
    }
  })

  app.delete('/:systemId/interopAuthParams', async (req, res, next) => {
    /* #swagger.tags = ['InteropAuthParams']
       #swagger.summary = 'Remove interop authentication parameters.' */
    // #swagger.operationId = "deleteInteropAuthParams"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['interopAuthParam'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/InteropAuthParam" }
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/InteropAuthParam" }, description: "Removed interop authentication parameters." }
      */
      const responseToSend = await interopAuthParamService.deleteInteropAuthParams(req.params.systemId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove interop authentication parameter, please contact system admin")
    }
  })

};



