const { ethers } = require('ethers');
const ethJsUtil = require('ethereumjs-util')

async function verifyValidatorSeals(signatureOrProof, chainHeadValidators){

  const BLOCK_HEADER_INDEX = 3
  const VALIDATOR_SIGNATURES_INDEX = 5
  const BLOCK_HASH_INDEX = 2

  let abiCoder = new ethers.AbiCoder
  /* The structure of the signature or proof is as follows:
  struct SignatureOrProof {
    bytes rlpSiblingNodes;
    bytes32 receiptsRoot;
    bytes32 blockHash;
    bytes rlpBlockHeader;
    bytes rlpBlockHeaderExcludingRound;
    bytes rlpValidatorSignatures;
  }
  */

  let decodedSignatureOrProof = abiCoder.decode(['bytes', 'bytes32', 'bytes32', 'bytes', 'bytes', 'bytes'], signatureOrProof);
  let rlpBlockHeader = decodedSignatureOrProof[BLOCK_HEADER_INDEX]
  let rlpValidatorSignatures = decodedSignatureOrProof[VALIDATOR_SIGNATURES_INDEX]

  let signedHash = ethers.keccak256(rlpBlockHeader)
  let validatorSignatures = ethers.decodeRlp(rlpValidatorSignatures)
  let validSeals = 0;
  let addressReuseCheck = []
  for (let i = 0; i < validatorSignatures.length; i++) {
    res = ethJsUtil.fromRpcSig(validatorSignatures[i])
    pub = ethJsUtil.ecrecover(ethJsUtil.toBuffer(signedHash), res.v, res.r, res.s);
    addrBuf = ethJsUtil.pubToAddress(pub);
    signatureAddress = ethJsUtil.bufferToHex(addrBuf);
    for (let j = 0; j < chainHeadValidators.length; j++) {
      if (signatureAddress == chainHeadValidators[j]) {
        for (let k = 0; k < i; k++) {
          if (addressReuseCheck[k] == signatureAddress) {
            console.log("Error: Not allowed to submit multiple seals from the same validator")
            break;
          }
        }
        validSeals = validSeals + 1;
        addressReuseCheck[i] = signatureAddress;
        break;
      }
    }
  }
  if (validSeals < chainHeadValidators.length / 2) {
    console.log("Error: Not enough valid validator seals");
    return false
  } else {
    return true
  }
}

module.exports = {
  verifyValidatorSeals
}
