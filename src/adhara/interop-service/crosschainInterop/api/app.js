const express = require("express");
const cookieParser = require("cookie-parser");
const { initialize } = require("express-openapi");
const swaggerUi = require("swagger-ui-express");

const configPath = process.env.CONFIG_PATH ? process.env.CONFIG_PATH : '../config/harmonia-config.json'
const config = require(configPath)
const v1SettlementInstructionService = require("./api-v1/services/settlementInstructionService.js")
const v1SettlementObligationService = require("./api-v1/services/settlementObligationService.js")
const v1RepoObligationService = require("./api-v1/services/repoObligationService.js")

const v1ApiDoc = require("./api-v1/api-doc.js")

const app = express();

const Graph = require('../src/RunGraph')
const Logger = require('../src/CrosschainSDKUtils/logger.js')
const logger = Logger(config, {})
const graph = Graph(config, { logger })
const crosschainApplicationSDK = graph.crosschainApplicationSDK

app.listen(3030);
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*")
  res.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, OPTIONS")
  res.header("Access-Control-Allow-Headers", "Content-Type, api_key, Authorization")
  next()
})

// OpenAPI routes
initialize({
  app,
  apiDoc: v1ApiDoc,
  errorMiddleware: function(err, req, res, next) { // only handles errors for v1, so do something with err in a v1 way
    if (err.errors) {
      err.errors.forEach(function(e) {
        if (e.errorCode === 'type.openapi.requestValidation' || e.errorCode === 'required.openapi.requestValidation') {
          const errorString = 'OpenAPI spec validation failed in [' + e.location + '] for property [' + e.path + '] with reason [' + e.message + ']'
          console.log(Error(errorString))
          res.status(err.status).json({
            "error": errorString
          })
        } else {
          const errorString = 'An unidentified error [' + e.errorCode + '] occurred'
          console.log(Error(errorString))
          res.status(err.status).json({
            "error": errorString
          })
        }
      })
    }
  },
  dependencies: {
    settlementInstructionService: v1SettlementInstructionService(config, crosschainApplicationSDK),
    settlementObligationService: v1SettlementObligationService(config, crosschainApplicationSDK),
    repoObligationService: v1RepoObligationService(config, crosschainApplicationSDK)
  },
  paths: "./api-v1/paths/",
});

// OpenAPI UI
app.use(
  "/v1/api-documentation",
  swaggerUi.serve,
  swaggerUi.setup(null, {
    swaggerOptions: {
      url: "http://localhost:3030/v1/api-docs",
    },
  })
);

console.log("App running on port http://localhost:3030");
console.log(
  "OpenAPI documentation available in http://localhost:3030/v1/api-documentation"
);

//console.log("app.path:", app.path())

module.exports = app;
