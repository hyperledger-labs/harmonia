module.exports = function (interopAuthParamService) {
  let operations = {
    GET,
    POST,
    DELETE
  };

  async function GET(req, res, next) {
    try {
      const responseToSend = await interopAuthParamService.getInteropAuthParams(req.params.systemId, req.query.foreignSystemId, req.query.foreignContractAddress)
      res.status(200).json(responseToSend)
    } catch (e) {
      console.log(e)
      res.status(500).json("Server error, unable to get interop authentication parameter, please contact system admin")
    }
  }

  async function POST(req, res, next) {
    try {
      const responseToSend = await interopAuthParamService.postInteropAuthParams(req.params.systemId, req.body)
      res.status(201).json(responseToSend)
    } catch (e) {
      console.log(e)
      res.status(500).json("Server error, unable to create interop authentication parameter, please contact system admin")
    }
  }

  async function DELETE(req, res, next) {
    try {
      const responseToSend = await interopAuthParamService.deleteInteropAuthParams(req.params.systemId, req.body)
      res.status(200).json(responseToSend)
    } catch (e) {
      console.log(e)
      res.status(500).json("Server error, unable to delete interop authentication parameter, please contact system admin")
    }
  }

  GET.apiDoc = {
    summary: "Get whether a given blockchainId and contractAddress have been added as authentication parameters",
    operationId: "getInteropAuthParams",
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
        type: 'string'
      },
      {
        in: 'query',
        name: 'foreignContractAddress',
        required: true,
        type: 'string'
      }
    ],
    responses: {
      200: {
        description: "Get whether a given foreignSystemId and foreignContractAddress have been added as authentication parameters",
        schema: {
          type: "object",
          properties: {
            isAuthParams: {
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
    summary: "Add to list of interop authentication parameters",
    operationId: "postInteropAuthParams",
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
        name: "interopAuthParam",
        schema: {
          $ref: "#/definitions/InteropAuthParams",
        },
      }
    ],
    responses: {
      201: {
        description: "Created interop authentication parameters.",
        schema: {
          $ref: "#/definitions/InteropAuthParams",
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
    summary: "Remove interop authentication parameters",
    operationId: "deleteInteropAuthParams",
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
        name: "interopAuthParam",
        schema: {
          $ref: "#/definitions/InteropAuthParams",
        },
      }
    ],
    responses: {
      201: {
        description: "Removed interop authentication parameters",
        schema: {
          $ref: "#/definitions/InteropAuthParams",
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

