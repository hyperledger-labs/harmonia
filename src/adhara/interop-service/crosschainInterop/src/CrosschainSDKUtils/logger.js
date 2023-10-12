const winston = require('winston');

function init(config, dependencies){

  if(config.logLevel !== 'error'
    && config.logLevel !== 'warn'
    && config.logLevel !== 'info'
    //&& config.logLevel !== 'http'
    //&& config.logLevel !== 'verbose'
    && config.logLevel !== 'debug'
    && config.logLevel !== 'silly'
    && config.logLevel !== 'silent'
  ){
    console.log('Unsupported logging level set in configuration')
    return Error('Unsupported logging level set in configuration')
  }

  let format = winston.format.combine(
                winston.format.timestamp(),
                winston.format.errors({stack: true}),
                winston.format.prettyPrint(),
                winston.format.align(),
                winston.format.printf((info) => `[${info.timestamp}] ${info.level}: ${info.message}`)
              )
  if (process.env.NODE_ENV === 'production') {
    format = winston.format.combine(
              winston.format.timestamp(),
              winston.format.errors({stack: true}),
              winston.format.json(),
              winston.format.prettyPrint(),
              winston.format.align(),
              winston.format.printf((info) => `[${info.timestamp}] ${info.level}: ${info.message}`)
            )
  }

  const logger = winston.createLogger({
    level: config.logLevel === 'silent' ? 'debug' : config.logLevel,
    format,
    transports: [
      new winston.transports.Console({level: config.logLevel === 'silent' ? 'debug' : config.logLevel, silent: (config.logLevel === 'silent')})
    ]
  })

  function log(level, message, ...meta){
    logger.log(level, message, meta)
  }

  return {
    log
  }
}

module.exports = init
