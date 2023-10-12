module.exports = function (cordaParticipantService) {
  let operations = {
    GET,
    POST,
    DELETE
  };

  async function GET(req, res, next) {
    try{ 
      const responseToSend = await cordaParticipantService.getCordaParticipant(req.params.systemId, req.query.foreignSystemId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to get corda participant, please contact system admin")
    }
  }

  async function POST(req, res, next) {
    try{ 
      const responseToSend = await cordaParticipantService.postCordaParticipant(req.params.systemId, req.body)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to create corda participant, please contact system admin")
    }
  }

  async function DELETE(req, res, next) {
    try{ 
      const responseToSend = await cordaParticipantService.deleteCordaParticipant(req.params.systemId, req.query.foreignSystemId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to delete corda participant, please contact system admin")
    }
  }

  GET.apiDoc = {
    summary: "Get whether a given public key is a participant on the given chain",
    operationId: "getCordaParticipant",
    parameters: [
      {
        in: 'path',
        name: 'systemId',
        required: true,
        type: 'string'
      },
      {
        in: 'query',
        name: 'foreignSystemId',
        required: true,
        type: 'number'
      },
      {
        in: 'query',
        name: 'publicKey',
        required: true,
        type: 'string'
      }
    ],
    responses: {
      200: {
        description: "Get whether a given public key is a participant on the given chain",
        schema: {
          type: "object",
          properties: {
            isParticipant: {
              type: "boolean",
            },
          },
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
    summary: "Add a corda participant",
    operationId: "postCordaParticipant",
    consumes: ["application/json"],
    parameters: [
      {
        in: 'path',
        name: 'systemId',
        required: true,
        type: 'string'
      },
      {
        in: "body",
        name: "cordaParticipant",
        schema: {
          $ref: "#/definitions/CordaParticipant",
        },
      }
    ],
    responses: {
      201: {
        description: "Created corda participant.",
        schema: {
          $ref: "#/definitions/CordaParticipant",
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

  DELETE.apiDoc = {
    summary: "Remove a corda participant",
    operationId: "deleteCordaParticipant",
    consumes: ["application/json"],
    parameters: [
      {
        in: 'path',
        name: 'systemId',
        required: true,
        type: 'string'
      },
      {
        in: 'query',
        name: 'foreignSystemId',
        required: true,
        type: 'number'
      },
      {
        in: 'query',
        name: 'publicKey',
        required: true,
        type: 'string'
      }
    ],
    responses: {
      201: {
        description: "Removed corda participant",
        schema: {
          $ref: "#/definitions/CordaParticipant",
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




