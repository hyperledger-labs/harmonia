module.exports = function (repoObligationService) {
  let operations = {
    POST
  };

  async function POST(req, res, next) {
    try{
      console.log("POSTing to repo obligation service")
      const responseToSend = await repoObligationService.postRepoObligation(req.params.systemId, req.body) 
      res.status(201).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json(e)
    }
  }

  POST.apiDoc = {
    summary: "Submit a repo obligation",
    operationId: "postRepoObligation",
    consumes: ["application/json"],
    parameters: [
      {
        in: "path",
        name: "systemId",
        required: true,
        type: "number",
      },
      {
        in: "body",
        name: "repoObligation",
        schema: {
          $ref: "#/definitions/RepoObligation",
        },
      }
    ],
    responses: {
      201: {
        description: "Created repo obligations.",
        schema: {
          $ref: "#/definitions/SettlementObligationResponse",
        },
      },
      default: {
        description: 'An error occurred.',
        schema: {
          additionalProperties: false
        }
      }
    },
  };

  return operations;
};



