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

/* Middlewares */
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

app.listen(3030, () => {
  console.log("Server is running!\nAPI documentation: http://localhost:3030/doc")
})

/* Endpoints */
const settlementInstructionService = require("./services/settlementInstructionService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/settlementInstructions')(app, settlementInstructionService)

const settlementObligationService = require("./services/settlementObligationService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/settlementObligations.js')(app, settlementObligationService)

const repoObligationService = require("./services/repoObligationService.js")(config, crosschainApplicationSDK)
require('./paths/{networkId}/repoObligations.js')(app, repoObligationService)



