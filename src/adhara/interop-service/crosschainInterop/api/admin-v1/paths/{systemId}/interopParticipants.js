module.exports = function (interopParticipantService) {
  let operations = {
    GET,
    POST
  };

  async function GET(req, res, next) {
    try {
      const responseToSend = await interopParticipantService.getLocalAccountId(req.params.systemId, req.query.foreignAccountId)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to get interop participant, please contact system admin")
    }
  }

  async function POST(req, res, next) {
    try {
      const responseToSend = await interopParticipantService.postInteropParticipants(req.params.systemId, req.body) 
      res.status(201).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to create interop participant, please contact system admin")
    }
  }

  GET.apiDoc = {
    summary: "Fetch the local account id by foreign account id and local chain name",
    operationId: "getLocalAccountId",
    parameters: [
      {
        in: 'path',
        name: 'systemId',
        required: true,
        type: 'string'
      },
      {
        in: 'query',
        name: 'foreignAccountId',
        required: true,
        type: 'string'
      }
    ],
    responses: {
      200: {
        description: "local account id",
        schema: {
          $ref: "#/definitions/InteropParticipants",
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

  POST.apiDoc = {
    summary: "Add to list of interop participants",
    operationId: "postInteropParticipants",
    consumes: ["application/json"],
    parameters: [
      {
        in: "path",
        name: "systemId",
        required: true,
        type: "string",
      },
      {
        in: "body",
        name: "interopParticipant",
        schema: {
          $ref: "#/definitions/InteropParticipants",
        },
      }
    ],
    responses: {
      201: {
        description: "Created interop participant.",
        schema: {
          $ref: "#/definitions/InteropParticipants",
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

