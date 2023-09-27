module.exports = function (validatorUpdateService) {
  const operations = {
    GET,
    POST,
  };

  async function GET(req, res, next) {
    try{
      const systemId = req.params.systemId
      const operationId = req.query.operationId

      if (!operationId) {
        return res.status(400).json({
          'error': 'Please provide the [operationId] input obtained when the validator update instruction was submitted'
        })
      }
      const responseToSend = await validatorUpdateService.getValidatorUpdateInstruction(systemId, operationId)
      if (!responseToSend) {
        console.log('Validator update instruction from systemId [' + systemId + '] for [' + operationId + '] was not found')
        return res.status(400).json({
          'error:': 'Validator update instruction not found'
        })
      }
      return res.status(200).json(responseToSend)
    } catch (e){
      console.log(e)
      return res.status(500).json({
        'error': 'Internal Server error, please contact system admin'
      })
    }
  }

  async function POST(req, res, next) {
    try{
      const blockHeader = req.body.blockHeader
      if (!!blockHeader){

      }
      const responseToSend = await validatorUpdateService.postValidatorUpdateInstruction(req.params.systemId, req.body)
      return res.status(201).json(responseToSend)
    } catch (e){
      console.log(e)
      return res.status(500).json({
        'error': 'Internal Server error, please contact system admin'
      })
    }
  }

  GET.apiDoc = {
    summary: 'Fetch a validator update instruction by operationId',
    operationId: 'getSettlementInstruction',
    parameters: [
      {
        in: 'path',
        name: 'systemId',
        required: true,
        type: 'number',
      },
      {
        in: 'query',
        name: 'operationId',
        required: true,
        type: 'string'
      },
    ],
    responses: {
      200: {
        description: 'Validator update instruction response',
        schema: {
          $ref: '#/definitions/ValidatorUpdateInstructionResponse',
        },
      },
      400: {
        description: 'Bad request',
      },
      500: {
        description: 'Internal server error',
      },
      default: {
        description: 'An error occurred',
        schema: {
          additionalProperties: false
        }
      }
    },
  }

  POST.apiDoc = {
    summary: 'Post a remote validator update instruction',
    operationId: 'validatorUpdateInstruction',
    consumes: ['application/json'],
    parameters: [
      {
        in: 'path',
        name: 'systemId',
        required: true,
        type: 'number',
      },
      {
        in: 'body',
        name: 'validatorUpdateInstruction',
        schema: {
          $ref: '#/definitions/ValidatorUpdateInstructionRequest',
        },
      }
    ],
    responses: {
      201: {
        description: 'Successfully submitted a validator update instruction',
        schema: {
          $ref: '#/definitions/ValidatorUpdateInstructionResponse',
        },
      },
      400: {
        description: 'Bad request',
      },
      500: {
        description: 'Internal server error',
      },
      default: {
        description: 'An error occurred',
        schema: {
          additionalProperties: false
        }
      }
    },
  };

  return operations;
};


