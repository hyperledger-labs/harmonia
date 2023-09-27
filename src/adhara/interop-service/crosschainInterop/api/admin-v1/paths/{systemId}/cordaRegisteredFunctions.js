module.exports = function (cordaRegisteredFunctionService) {
  let operations = {
    GET,
    POST,
    DELETE
  };

  async function GET(req, res, next) {
    try{
      const responseToSend = await cordaRegisteredFunctionService.getRegisteredFunction(req.params.systemId, req.query.foreignSystemId, req.query.functionSignature, req.query.index)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json(e)
    }
  }

  async function POST(req, res, next) {
    try{
      const responseToSend = await cordaRegisteredFunctionService.postRegisteredFunction(req.params.systemId, req.query.foreignSystemId, req.query.functionSignature, req.body)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json(e)
    }
  }

  async function DELETE(req, res, next) {
    try{
      const responseToSend = await cordaRegisteredFunctionService.deleteRegisteredFunction(req.params.systemId, req.query.foreignSystemId, req.query.functionSignature)
      res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      res.status(500).json(e)
    }
  }

  GET.apiDoc = {
    summary: "Get the parameter handler at given index for a registered function with given function signature.",
    operationId: "getRegisteredFunction",
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
        name: 'functionSignature',
        required: true,
        type: 'number'
      },
      {
        in: 'query',
        name: 'index',
        required: true,
        type: 'number'
      }
    ],
    responses: {
      200: {
        description: "Obtained the parameter handler at given index for a registered function with given function signature.",
        schema: {
          $ref: "#/definitions/CordaParameterHandler",
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
    summary: "Add a Corda registered function.",
    operationId: "postRegisteredFunction",
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
        name: 'functionSignature',
        required: true,
        type: 'number'
      },
      {
        in: "body",
        name: "parameterHandlers",
        schema: {
          $ref: "#/definitions/CordaParameterHandlers",
        },
      }
    ],
    responses: {
      201: {
        description: "Added a Corda registered function.",
        schema: {
          $ref: "#/definitions/CordaRegisteredFunction",
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
    summary: "Remove a Corda registered function.",
    operationId: "deleteRegisteredFunction",
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
        name: 'functionSignature',
        required: true,
        type: 'number'
      },
    ],
    responses: {
      201: {
        description: "Removed a Corda registered function",
        schema: {
          $ref: "#/definitions/CordaRegisteredFunction",
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




