module.exports = function (cordaNotaryService) {
  let operations = {
    GET,
    POST,
    DELETE
  };

  async function GET(req, res, next) {
    try{ 
      const responseToSend = await cordaNotaryService.getCordaNotary(req.params.systemId, req.query.foreignSystemId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to get corda notary, please contact system admin")
    }
  }

  async function POST(req, res, next) {
    try{ 
      const responseToSend = await cordaNotaryService.postCordaNotary(req.params.systemId, req.body)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to create corda notary, please contact system admin")
    }
  }

  async function DELETE(req, res, next) {
    try{ 
      const responseToSend = await cordaNotaryService.deleteCordaNotary(req.params.systemId, req.query.foreignSystemId, req.query.publicKey)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json("Server error, unable to delete corda notary, please contact system admin")
    }
  }

  GET.apiDoc = {
    summary: "Get whether a given public key is a notary on the given chain",
    operationId: "getCordaNotary",
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
        description: "Get whether a given public key is a notary on the given chain",
        schema: {
          type: "object",
          properties: {
            isNotary: {
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
    summary: "Add a corda notary",
    operationId: "postCordaNotary",
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
        name: "cordaNotary",
        schema: {
          $ref: "#/definitions/CordaNotary",
        },
      }
    ],
    responses: {
      201: {
        description: "Created corda notary.",
        schema: {
          $ref: "#/definitions/CordaNotary",
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
    summary: "Remove a corda notary",
    operationId: "deleteCordaNotary",
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
        description: "Removed corda notary",
        schema: {
          $ref: "#/definitions/CordaNotary",
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



