module.exports = function (validatorService) {
  let operations = {
    GET,
  };

  async function GET(req, res, next) {
    try{
      res.status(200).json(await validatorService.getValidators(req.query.blockHash));
    } catch(error){
      console.log(error)
      res.status(500).json("Server error, could not get validators, please contact system admin")
    }
  }

  GET.apiDoc = {
    summary: "Fetch list of validators",
    operationId: "getValidators",
    parameters: [
      {
        in: 'query',
        name: 'blockHash',
        required: false,
        type: 'string'
      }
    ],
    responses: {
      200: {
        description: "List of validators.",
        schema: {
          type: "array",
          items: {
            $ref: "#/definitions/Validators",
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

  return operations;
};
