const { CustomError } = require('./errors');

function printEventEmitted(logger, txObj) {
  for(let log of txObj.receipt.logs){
    if(log.event === 'Uint'){
      logger.log('debug', 'Uint Event -> ' + log.args[1]+': '+log.args[0].toNumber())
    } else if(log.event === 'Bool'){
      logger.log('debug', 'Bool Event -> ' +log.args[1]+': '+log.args[0])
    } else if(log.event === 'Bytes'){
      logger.log('debug', 'Bytes Event -> ' +log.args[1]+': '+log.args[0])
    } else if(log.event === 'Bytes32'){
      logger.log('debug', 'Bytes32 Event -> ' +log.args[1]+': '+log.args[0])
    } else if(log.event === 'String'){
      logger.log('debug', 'String Event -> ' +log.args[0])
    } else if(log.event === 'Address'){
      logger.log('debug', 'Address Event -> ' +log.args[1]+': '+log.args[0])
    } else {
      logger.log('debug', 'Unhandled Log -> '+JSON.stringify(log, null, 2))
    }
  }
}

const UNKNOWN_ERROR_NAME = 'Unknown server error';
const UNKNOWN_ERROR_CODE = 500;
const CONNECTION_TIMEOUT_NAME = 'Connection to downstream node timed out';
const CONNECTION_TIMEOUT_CODE = 501;
const CONNECTION_FAILURE_NAME = 'Failed to connect to downstream node';
const CONNECTION_FAILURE_CODE = 502;
const SERVICE_UNAVAILABLE_NAME = 'Internal error';
const SERVICE_UNAVAILABLE_CODE = 503;

const INSUFFICIENT_GAS_NAME = 'after consuming all gas';
const INSUFFICIENT_GAS_CODE = 511;
const INSUFFICIENT_GAS_MSG = 'Insufficient gas';
const CHECK_GAS_OR_ABI_NAME = 'Check gas or abi';
const CHECK_GAS_OR_ABI_CODE = 512;
const INTRINSIC_GAS_EXCEEDS_GAS_LIMIT_NAME = 'Intrinsic gas exceeds gas limit';
const INTRINSIC_GAS_EXCEEDS_GAS_LIMIT_CODE = 513;
const REPLACEMENT_TRANSACTION_UNDERPRICED_NAME = 'Replacement transaction underpriced';
const REPLACEMENT_TRANSACTION_UNDERPRICED_CODE = 514;

const INSUFFICIENT_BALANCE_TO_PLACE_HOLD_NAME = 'Not enough tokens to hold from account';
const INSUFFICIENT_BALANCE_TO_PLACE_HOLD_CODE = 521;
const HOLD_ALREADY_EXIST_NAME = 'Hold already exist';
const HOLD_ALREADY_EXIST_CODE = 522;
const HOLD_DOES_NOT_EXIST_NAME = 'Hold does not exist';
const HOLD_DOES_NOT_EXIST_CODE = 523;
const INCORRECT_HOLD_AMOUNT_NAME = 'Incorrect hold amount';
const INCORRECT_HOLD_AMOUNT_CODE = 524;
const UNABLE_TO_MAKE_HOLD_PERPETUAL_NAME = 'Unable to make an expired hold perpetual';
const UNABLE_TO_MAKE_HOLD_PERPETUAL_CODE = 525;
const TRADE_CAN_ONLY_BE_CANCELLED_ONCE_NAME = 'Trade can only be cancelled once, by either the leader or the follower';
const TRADE_CAN_ONLY_BE_CANCELLED_ONCE_CODE = 526;
const CROSS_CHAIN_FUNCTION_CALL_FAILED_NAME = 'Crosschain function call failed';
const CROSS_CHAIN_FUNCTION_CALL_FAILED_CODE = 527;

function isError(error) {
  if (error && error.stack && error.message) {
    return true;
  }
  return false;
}

function isCheckGasOrAbiError(e) {
  return e instanceof CustomError && e.code === CHECK_GAS_OR_ABI_CODE;
}

const timeoutAfter = (prom, time, exception) => {
  let timer;
  return Promise.race([
    prom,
    new Promise((_r, rej) => timer = setTimeout(rej, time, exception))
  ]).finally(() => clearTimeout(timer));
}

function identifyError(error) {
  if (error.includes(SERVICE_UNAVAILABLE_NAME)) {
    throw new CustomError(SERVICE_UNAVAILABLE_CODE, SERVICE_UNAVAILABLE_NAME, null);
  } else if (error.includes(CONNECTION_TIMEOUT_NAME)) {
    throw new CustomError(CONNECTION_TIMEOUT_CODE, CONNECTION_TIMEOUT_NAME, null);
  } else if (error.includes(CONNECTION_FAILURE_NAME)) {
    throw new CustomError(CONNECTION_FAILURE_CODE, CONNECTION_FAILURE_NAME, null);
  }
}

async function tryCatch(promise) {
  try {
    return await promise;
  } catch (e) {
    logger.log('debug', 'An unexpected error occurred')
    if (!e.reason) {
      if (isError(e)) {
        identifyError(e.message)
        if (e.message.includes(INTRINSIC_GAS_EXCEEDS_GAS_LIMIT_NAME)) {
          throw new CustomError(INTRINSIC_GAS_EXCEEDS_GAS_LIMIT_CODE, INTRINSIC_GAS_EXCEEDS_GAS_LIMIT_NAME, null);
        } else if (e.message.includes(INSUFFICIENT_GAS_NAME)) {
          throw new CustomError(INSUFFICIENT_GAS_CODE, INSUFFICIENT_GAS_MSG, null);
        } else if (e.receipt) {
          throw new CustomError(CHECK_GAS_OR_ABI_CODE, CHECK_GAS_OR_ABI_NAME, e.receipt.gasUsed)
        } else {
          throw new CustomError(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_NAME, e);
        }
      }
    } else if (e.reason.includes(INSUFFICIENT_BALANCE_TO_PLACE_HOLD_NAME)) {
      throw new CustomError(INSUFFICIENT_BALANCE_TO_PLACE_HOLD_CODE, INSUFFICIENT_BALANCE_TO_PLACE_HOLD_NAME, null);
    } else if (e.reason.includes(HOLD_ALREADY_EXIST_NAME)) {
      throw new CustomError(HOLD_ALREADY_EXIST_CODE, HOLD_ALREADY_EXIST_NAME, null);
    } else if (e.reason.includes(HOLD_DOES_NOT_EXIST_NAME)) {
      throw new CustomError(HOLD_DOES_NOT_EXIST_CODE, HOLD_DOES_NOT_EXIST_NAME, null);
    } else if (e.reason.includes(TRADE_CAN_ONLY_BE_CANCELLED_ONCE_NAME)) {
      throw new CustomError(TRADE_CAN_ONLY_BE_CANCELLED_ONCE_CODE, TRADE_CAN_ONLY_BE_CANCELLED_ONCE_NAME, null);
    } else if (e.reason.includes(UNABLE_TO_MAKE_HOLD_PERPETUAL_NAME)) {
      throw new CustomError(UNABLE_TO_MAKE_HOLD_PERPETUAL_CODE, UNABLE_TO_MAKE_HOLD_PERPETUAL_NAME, null);
    } else if (e.reason.includes(REPLACEMENT_TRANSACTION_UNDERPRICED_NAME)) {
      throw new CustomError(REPLACEMENT_TRANSACTION_UNDERPRICED_CODE, REPLACEMENT_TRANSACTION_UNDERPRICED_NAME, null);
    } else if (e.reason.includes(CROSS_CHAIN_FUNCTION_CALL_FAILED_NAME)) {
      throw new CustomError(CROSS_CHAIN_FUNCTION_CALL_FAILED_CODE, CROSS_CHAIN_FUNCTION_CALL_FAILED_NAME, null);
    } else if (e.reason.includes(INCORRECT_HOLD_AMOUNT_NAME)) {
      throw new CustomError(INCORRECT_HOLD_AMOUNT_CODE, INCORRECT_HOLD_AMOUNT_NAME, null);
    }
    throw e
  }
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function hexToBase64(str) {
  return Buffer.from(str, 'hex').toString('base64')
}

function base64ToHex(str) {
  return Buffer.from(str, 'base64').toString('hex')
}

function getDate() {
  const dt = new Date();
  let year = dt.getFullYear();
  let month = ('0' + (dt.getMonth() + 1)).slice(-2);
  let day = ('0' + dt.getDate()).slice(-2);
  let hour = dt.getHours();
  let minute = dt.getMinutes();
  let seconds = ('0' + dt.getSeconds()).slice(-2);
  return year + '-' + month + '-' + day + '-' + hour + '-' + minute + '-' + seconds;
}

async function postData(url = '', params = {}) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(params)
  });
  return await response.json();
}

module.exports = {
  hexToBase64,
  base64ToHex,
  tryCatch,
  identifyError,
  isCheckGasOrAbiError,
  printEventEmitted,
  sleep,
  getDate,
  postData
};

