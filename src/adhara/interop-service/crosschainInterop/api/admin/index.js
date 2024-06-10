const cookieParser = require("cookie-parser");
const expressLogger = require("morgan");
const swaggerUi = require('swagger-ui-express')
const swaggerFile = require('./swagger-output.json')
const bodyParser = require('body-parser')
const express = require('express')
const app = express()

const configPath = process.env.CONFIG_PATH ? process.env.CONFIG_PATH : '../../config/config.json'
const config = require(configPath)

const Graph = require("../../src/RunGraph");
const Logger = require('../../src/CrosschainSDKUtils/logger.js')

const logger = Logger(config, {})
const graph = Graph(config, { logger })
const crosschainApplicationSDK = graph.crosschainApplicationSDK

/* Middleware */
app.use(bodyParser.json())
app.use(expressLogger("dev"));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*")
  res.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, OPTIONS")
  res.header("Access-Control-Allow-Headers", "Content-Type, api_key, Authorization")
  next()
})
app.use('/doc', swaggerUi.serve, swaggerUi.setup(swaggerFile))

app.listen(3031, () => {
  console.log("Server is running!\nAPI documentation: http://localhost:3031/doc")
})

/* Endpoints */
const validatorService = require("./services/validatorService.js")(config, crosschainApplicationSDK)
require('./paths/validators')(app, validatorService)

const cordaNotaryService = require("./services/cordaNotaryService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/cordaNotaries.js')(app, cordaNotaryService)

const cordaParticipantService = require("./services/cordaParticipantService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/cordaParticipants.js')(app, cordaParticipantService)

const cordaRegisteredFunctionService = require("./services/cordaRegisteredFunctionService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/cordaRegisteredFunctions.js')(app, cordaRegisteredFunctionService)

const interopAuthParamService = require("./services/interopAuthParamService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/interopAuthParams.js')(app, interopAuthParamService)

const interopParticipantService = require("./services/interopParticipantService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/interopParticipants.js')(app, interopParticipantService)

const validatorUpdateInstructionService = require("./services/validatorUpdateService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/validatorUpdateInstructions.js')(app, validatorUpdateInstructionService)

const validatorSetInstructionService = require("./services/validatorSetService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/validatorSetInstructions.js')(app, validatorSetInstructionService)
