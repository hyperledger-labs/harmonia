module.exports = function (app, interopAuthParamService) {

  app.get('/:networkId/interopAuthParams', async (req, res, next) => {
    /* #swagger.tags = ['InteropAuthParams']
       #swagger.summary = 'Fetch if is interop authentication parameters.' */
    // #swagger.operationId = "getInteropAuthParams"
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
    /* #swagger.parameters['remoteContractAddress'] = {
         in: 'query',
         required: true,
         type: 'string'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { isAuthParam: true }, description: "Fetched if is interop authentication parameters." }
      */
      const responseToSend = await interopAuthParamService.getInteropAuthParams(req.params.networkId, req.query.remoteNetworkId, req.query.remoteContractAddress)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get interop authentication parameter, please contact system admin")
    }
  })

  app.post('/:networkId/interopAuthParams', async (req, res, next) => {
    /* #swagger.tags = ['InteropAuthParams']
       #swagger.summary = 'Create interop authentication parameters.' */
    // #swagger.operationId = "postInteropAuthParams"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['networkId'] = {
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
      const responseToSend = await interopAuthParamService.postInteropAuthParams(req.params.networkId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create interop authentication parameter, please contact system admin")
    }
  })

  app.delete('/:networkId/interopAuthParams', async (req, res, next) => {
    /* #swagger.tags = ['InteropAuthParams']
       #swagger.summary = 'Remove interop authentication parameters.' */
    // #swagger.operationId = "deleteInteropAuthParams"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['networkId'] = {
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
      const responseToSend = await interopAuthParamService.deleteInteropAuthParams(req.params.networkId, req.body)
      res.status(200).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not remove interop authentication parameter, please contact system admin")
    }
  })

};



