module.exports = function (app, repoObligationService) {

  app.post('/:systemId/repoObligations', async (req, res, next) => {
    /* #swagger.tags = ['RepoObligations']
       #swagger.summary = 'Submit a repo obligation.' */
    // #swagger.operationId = "postRepoObligation"
    /* #swagger.parameters['systemId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['repoObligation'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/RepoObligation" }
       }
    */
    try {
      /* #swagger.responses[201] = {
         schema: { "$ref": "#/definitions/SettlementObligationResponse" }, description: "Created repo obligation." }
      */
      const responseToSend = await repoObligationService.postRepoObligation(req.params.systemId, req.body)
      res.status(201).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not create corda notary, please contact system admin")
    }
  })

};



