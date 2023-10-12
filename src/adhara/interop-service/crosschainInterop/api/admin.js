const express = require("express");
const cookieParser = require("cookie-parser");
const expressLogger = require("morgan");
const { initialize } = require("express-openapi");
const swaggerUi = require("swagger-ui-express");

const configPath = process.env.CONFIG_PATH ? process.env.CONFIG_PATH : '../config/harmonia-config.json'
const config = require(configPath)
const v1ValidatorService = require("./admin-v1/services/validatorService.js")
const v1InteropAuthParamService = require("./admin-v1/services/interopAuthParamService.js")
const v1InteropParticipantService = require("./admin-v1/services/interopParticipantService.js")
const v1CordaNotaryService = require("./admin-v1/services/cordaNotaryService.js")
const v1CordaParticipantService = require("./admin-v1/services/cordaParticipantService.js")
const v1CordaRegisteredFunctionService = require("./admin-v1/services/cordaRegisteredFunctionService.js")
const v1ValidatorUpdateService = require("./admin-v1/services/validatorUpdateService.js")
const v1AdminDoc = require("./admin-v1/api-doc.js")

const admin = express();

const Graph = require('../src/RunGraph')
const Logger = require('../src/CrosschainSDKUtils/logger.js')

const logger = Logger(config, {})
const graph = Graph(config, { logger })
const crosschainApplicationSDK = graph.crosschainApplicationSDK

admin.listen(3031);
admin.use(expressLogger("dev"));
admin.use(express.json());
admin.use(express.urlencoded({ extended: false }));
admin.use(cookieParser());
admin.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*")
  res.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, OPTIONS")
  res.header("Access-Control-Allow-Headers", "Content-Type, api_key, Authorization")
  next()
})

// OpenAPI routes
initialize({
  app: admin,
  apiDoc: v1AdminDoc,
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
    validatorService: v1ValidatorService(config, crosschainApplicationSDK),
    interopAuthParamService: v1InteropAuthParamService(config, crosschainApplicationSDK),
    interopParticipantService: v1InteropParticipantService(config, crosschainApplicationSDK),
    cordaNotaryService: v1CordaNotaryService(config, crosschainApplicationSDK),
    cordaParticipantService: v1CordaParticipantService(config, crosschainApplicationSDK),
    cordaRegisteredFunctionService: v1CordaRegisteredFunctionService(config, crosschainApplicationSDK),
    validatorUpdateService: v1ValidatorUpdateService(config, crosschainApplicationSDK)
  },
  paths: "./admin-v1/paths/",
});

// OpenAPI UI
admin.use(
  "/v1/admin-documentation",
  swaggerUi.serve,
  swaggerUi.setup(null, {
    swaggerOptions: {
      url: "http://localhost:3031/v1/api-docs",
    },
  })
);

console.log("App running on port http://localhost:3031");
console.log(
  "OpenAPI documentation available in http://localhost:3031/v1/admin-documentation"
);

//console.log("app.path:", app.path())

module.exports = admin;
