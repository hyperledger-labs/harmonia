module.exports = function (app, validatorService) {

  app.get('/validators', async (req, res, next) => {
    /* #swagger.tags = ['Validators']
       #swagger.summary = 'Fetch list of validators.' */
    // #swagger.operationId = "getValidators"
    /* #swagger.parameters['obj'] = {
        in: 'query',
        name: 'blockHash',
        required: false,
        type: 'string'
       }
    */
    try {
      /* #swagger.responses[200] = {
         schema: { "$ref": "#/definitions/Validators" }, description: "Fetched list of validators." }
      */
      res.status(200).json(await validatorService.getValidators(req.query.blockHash));
    } catch (error) {
      console.log(error)
      // #swagger.responses[500] = { description: "An error occurred." }
      res.status(500).json("Server error, could not get validators, please contact system admin")
    }
  })

}

