module.exports = function (settlementObligationService) {
  let operations = {
    POST
  };

  async function POST(req, res, next) {
    try{
      const responseToSend = await settlementObligationService.postSettlementObligation(req.params.systemId, req.body)
      res.status(201).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to create settlement obligation, please contact system admin")
    }
  }

  POST.apiDoc = {
    summary: "Submit a settlement obligation",
    operationId: "postSettlementObligation",
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
        name: "settlementObligation",
        schema: {
          $ref: "#/definitions/SettlementObligation",
        },
      }
    ],
    responses: {
      201: {
        description: "Created settlement instruction.",
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


