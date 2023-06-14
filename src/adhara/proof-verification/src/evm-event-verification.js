const { ethers } = require("ethers");

async function verifyEVMEvent(signatureOrProof){
    const RECEIPTS_ROOT_INDEX = 1
    const SIBLING_NODES_INDEX = 0
    const LEAF_NODE_VALUE_INDEX = 1

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

    let rlpSiblingNodes = decodedSignatureOrProof[SIBLING_NODES_INDEX]
    let decodedRlpSiblingNodes = ethers.decodeRlp(rlpSiblingNodes);

    let receiptsRoot = decodedSignatureOrProof[RECEIPTS_ROOT_INDEX]

    for(let i = 1; i <= decodedRlpSiblingNodes.length; i++){
        const childIndex = decodedRlpSiblingNodes.length - i
        const childNode = decodedRlpSiblingNodes[childIndex]
        const rlpEncodedChildNode = ethers.encodeRlp(childNode)
        const childHash = ethers.keccak256(rlpEncodedChildNode)
        //console.log({childHash})
        if(childIndex == 0) {
            //console.log("The Merkle Patricia tree only contains the leaf node")
            if(childHash == receiptsRoot) {
                //console.log("Proof has been verified. The child node equals the receiptsRoot.")
                //console.log(childHash)
                //console.log(receiptsRoot)
                return true
            }
        } else {
            console.log("The Merkle Patricia tree has multiple levels")
            let parentNode = decodedRlpSiblingNodes[childIndex - 1]
            console.log(childIndex)
            if(parentNode.includes(childHash)){
                //console.log("Parent contains hash of child")
            } else {
                return false
            }
            const rlpEncodedParentNode = ethers.encodeRlp(parentNode)
            const parentHash = ethers.keccak256(rlpEncodedParentNode)
            console.log({parentHash})
            if(parentHash === receiptsRoot){
                //console.log("Proof has been verified")
                return true
            }
        }
    }
    return false
}

module.exports = {
  verifyEVMEvent
}
