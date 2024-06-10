module.exports = function (app, settlementObligationService) {

  app.post('/:networkId/settlementObligations', async (req, res, next) => {
    /* #swagger.tags = ['SettlementObligations']
       #swagger.summary = 'Submit a settlement obligation.' */
    // #swagger.operationId = "postSettlementObligation"
    // #swagger.consumes = ["application/json"]
    /* #swagger.parameters['networkId'] = {
         in: 'path',
         required: true,
         type: 'string'
       }
    */
    /* #swagger.parameters['settlementObligation'] = {
         in: 'body',
         schema: { "$ref": "#/definitions/SettlementObligation" }
       }
    */
    try {
      /* #swagger.responses[201] = {
         schema: { "$ref": "#/definitions/SettlementObligationResponse" }, description: "Created settlement instruction." }
      */
      const responseToSend = await settlementObligationService.postSettlementObligation(req.params.networkId, req.body)
      res.status(201).json(responseToSend)
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, unable to create settlement obligation, please contact system admin")
    }
  })

};



