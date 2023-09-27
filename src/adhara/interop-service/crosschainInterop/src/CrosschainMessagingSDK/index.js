const Web3 = require('web3')
const rlp = require('rlp')
const { GetProof } = require('eth-proof')

function init(config, dependencies){

  const web3 = new Web3()
  const logger = dependencies.logger

  const extraDataVanityIndex = 0;
  const extraDataValidatorsIndex = 1;
  const extraDataVoteIndex = 2;
  const extraDataRoundIndex = 3;
  const extraDataSealsIndex = 4;
  const headerParentHashIndex = 0;
  const headerSha3UnclesIndex = 1;
  const headerMinerIndex = 2;
  const headerStateRootIndex = 3;
  const headerTransactionsRootIndex = 4;
  const headerReceiptsRoot = 5;
  const headerLogsBloom = 6;
  const headerDifficulty = 7;
  const headerNumber = 8;
  const headerGasLimit = 9;
  const headerGasUsed = 10;
  const headerTime = 11;
  const headerExtraData = 12;
  const headerMixedHash = 13;
  const headerNonce = 14;

  function decodeVanity(block){
      let decodedExtraData = rlp.decode(block.extraData)
      // vanity data
      return decodedExtraData[extraDataVanityIndex].toString('hex')
  }

  function decodeValidatorAddresses(block) {
      let decodedExtraData = rlp.decode(block.extraData)
      // validator addresses
      const decodedValidatorAddresses = decodedExtraData[extraDataValidatorsIndex]
      const decodedValidatorAddressArray = []
      for(let va of decodedValidatorAddresses) {
          decodedValidatorAddressArray.push(va.toString('hex'))
      }
      return decodedValidatorAddressArray
  }

  function decodeValidatorVotes(block) {
      let decodedExtraData = rlp.decode(block.extraData)
      // validator votes
      const decodedValidatorVotes = decodedExtraData[extraDataVoteIndex].toString('hex')
      return decodedValidatorVotes
  }

  function decodeRound(block)  {
      let decodedExtraData = rlp.decode(block.extraData)
      // the round the block was created on
      return decodedExtraData[extraDataRoundIndex].toString('hex')
  }

  function decodeValidatorSeals(block) {
      let decodedExtraData = rlp.decode(block.extraData)
      // seals of the validators are signed block hashes
      const decodedValidatorSeals = decodedExtraData[extraDataSealsIndex]
      const decodedValidatorSealArray = []
      for(let vs of decodedValidatorSeals) {
          decodedValidatorSealArray.push(vs.toString('hex'))
      }
      return decodedValidatorSealArray
  }

  function encodeValidatorSeals(block) {
    let decodedExtraData = rlp.decode(block.extraData)
    const decodedValidatorSeals = decodedExtraData[extraDataSealsIndex]
    const decodedValidatorSealArray = []
    for (let vs of decodedValidatorSeals) {
      decodedValidatorSealArray.push(vs)
    }
    return '0x' + rlp.encode(decodedValidatorSealArray).toString('hex')
  }

  function excludeValidatorSeals(block) {
      let decodedExtraData = rlp.decode(block.extraData)
      return '0x' + rlp.encode([decodedExtraData[extraDataVanityIndex], decodedExtraData[extraDataValidatorsIndex], decodedExtraData[extraDataVoteIndex], decodedExtraData[extraDataRoundIndex]]).toString('hex')
  }

  function excludeRoundAndValidatorSeals(consensus, block) {
    let decodedExtraData = rlp.decode(block.extraData)
    if (consensus === 'ibft') {
      return '0x' + rlp.encode([decodedExtraData[extraDataVanityIndex], decodedExtraData[extraDataValidatorsIndex], decodedExtraData[extraDataVoteIndex]]).toString('hex')
    } else if (consensus === 'qbft') {
      return '0x' + rlp.encode([decodedExtraData[extraDataVanityIndex], decodedExtraData[extraDataValidatorsIndex], decodedExtraData[extraDataVoteIndex], decodedExtraData[extraDataRoundIndex], []]).toString('hex')
    }
  }

  function extractValidatorAddresses(consensus, block) {
    if (consensus === 'qbft') {
      logger.log('warn', 'The returned list of validator addresses might be empty if contract validator selection is used' )
    }
    // Assuming block header validator selection
    return decodeValidatorAddresses(block)
  }

  function printExtraData(block) {
      // vanity
      const decodedVanity = decodeVanity(block)
      logger.log('debug', 'Vanity: '+ decodedVanity)
      // validator addresses
      const decodedValidatorAddresses = decodeValidatorAddresses(block)
      logger.log('debug', 'Validator Addresses: '+ decodedValidatorAddresses)
      // validator votes
      const decodedValidatorVotes = decodeValidatorVotes(block)
      logger.log('debug', 'Validator Votes: '+ decodedValidatorVotes)
      // the round the block was created on
      const decodedRound = decodeRound(block)
      logger.log('debug', 'Round: '+ decodedRound)
      // seals of the validators are signed block hashes
      const decodedValidatorSeals = decodeValidatorSeals(block)
      logger.log('debug', 'Validator Seals: '+ decodedValidatorSeals)
  }

  function formatBlockHeaderToArray(consensus, block) {
    block.extraDataExcludingValidatorSeals = excludeValidatorSeals(block)
    block.extraDataExcludingRoundAndValidatorSeals = excludeRoundAndValidatorSeals(consensus, block)
    block.extraDataValidatorAddresses = extractValidatorAddresses(consensus, block)
    const gasLimit = block.gasLimit === 0 ? '0x' : web3.utils.toHex(block.gasLimit)
    const gasUsed = block.gasUsed === 0 ? '0x' : web3.utils.toHex(block.gasUsed)
    const time = block.timestamp === 0 ? '0x' : web3.utils.toHex(block.timestamp)
    const difficulty = block.difficulty === 0 ? '0x' : web3.utils.toHex(block.difficulty)
    const number = block.number === 0 ? '0x' : web3.utils.toHex(block.number)
    let result = [];
    result[headerParentHashIndex] = block.parentHash;
    result[headerSha3UnclesIndex] = block.sha3Uncles;
    result[headerMinerIndex] = block.miner;
    result[headerStateRootIndex] = block.stateRoot;
    result[headerTransactionsRootIndex] = block.transactionsRoot;
    result[headerReceiptsRoot] = block.receiptsRoot;
    result[headerLogsBloom] = block.logsBloom;
    result[headerDifficulty] = difficulty;
    result[headerNumber] = number;
    result[headerGasLimit] = gasLimit;
    result[headerGasUsed] = gasUsed;
    result[headerTime] = time;
    result[headerExtraData] = block.extraData;
    result[headerMixedHash] = block.mixHash;
    result[headerNonce] = block.nonce;
    return result;
  }

  function getBlockHeaderObjFromBlock(consensus, block) {
    const rlpValidatorSignatures = encodeValidatorSeals(block);
    const blockHeader = formatBlockHeaderToArray(consensus, block)

    const blockHeaderExcludingSeals = [...blockHeader]
    blockHeaderExcludingSeals[headerExtraData] = block.extraDataExcludingValidatorSeals

    const blockHeaderExcludingRound = [...blockHeader]
    blockHeaderExcludingRound[headerExtraData] = block.extraDataExcludingRoundAndValidatorSeals

    const rlpBlockHeader = rlp.encode(blockHeader)
    const rlpBlockHeaderExcludingSeals = rlp.encode(blockHeaderExcludingSeals)
    const rlpBlockHeaderExcludingRound = rlp.encode(blockHeaderExcludingRound)

    return {
      rlpBlockHeader,
      rlpBlockHeaderExcludingSeals,
      rlpBlockHeaderExcludingRound,
      rlpValidatorSignatures
    }
  }

  function encodeReceiptProof(proof){
    // the path is HP encoded
    logger.log('silly', 'TxIndex:', proof.txIndex)
    const indexBuffer = proof.txIndex.slice(2);
    logger.log('silly', JSON.stringify({indexBuffer}, null, 2))
    const hpIndex = '0x' + (indexBuffer.startsWith('0') ? '1' + indexBuffer.slice(1) : '00' + indexBuffer);
    logger.log('silly', JSON.stringify({hpIndex}, null, 2))
    // The value is the second buffer in the leaf (last node)
    const value = '0x' + Buffer.from(proof.receiptProof[proof.receiptProof.length - 1][1]).toString('hex');
    // The parent nodes must be rlp encoded
    const parentNodes = rlp.encode(proof.receiptProof);
    return {
        path: hpIndex,
        rlpEncodedReceipt: value,
        witness: '0x'+parentNodes.toString('hex')
    };
  }

  async function createProof(chainName, block, txHash){
    logger.log('debug', 'Creating proof for system: ['+chainName+']')
    const ethProof = new GetProof(config[chainName].httpProvider)
    logger.log('silly', 'ReceiptsRoot:' + JSON.stringify(block.receiptsRoot, null, 2))
    const txProof = await ethProof.receiptProof(txHash)
    logger.log('silly', 'ReceiptProof:', txProof.receiptProof.toString('hex'))
    const encodedReceiptProof = encodeReceiptProof(txProof)
    logger.log('silly', JSON.stringify({encodedReceiptProof}, null, 2))
    return encodedReceiptProof;
  }

  return {
    decodeValidatorAddresses,
    decodeRound,
    decodeValidatorSeals,
    encodeValidatorSeals,
    excludeValidatorSeals,
    excludeRoundAndValidatorSeals,
    createProof,
    getBlockHeaderObjFromBlock,
    encodeReceiptProof
  }
}

module.exports = init
