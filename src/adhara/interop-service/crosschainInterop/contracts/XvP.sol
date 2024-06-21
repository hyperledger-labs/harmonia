/*
  IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION,
  AND ANY UPDATES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
*/

pragma solidity ^0.8.13;

import "contracts/CrosschainFunctionCall.sol";
import "contracts/interfaces/IToken.sol";
import "contracts/interfaces/IFeeManager.sol";

contract XvP {
  /* Owner of the contract. */
  address public owner;
  /* Reference to function call layer's contract. */
  address public functionCallContractAddress;
  /* Reference to asset token contract. */
  address public tokenContractAddress;
  /* Asset token hold notary identifier. */
  string public notaryId;
  /* Mapping from remote identifier to local identification. */
  mapping(string => string) public remoteToLocalAccountId;
  /* Mapping from trade identifier to trade cancellation status. */
  mapping(string => bool) public isCancelled;

  /* Events for debugging purposes. */
  event Bytes32(bytes32, string);
  event Bytes(bytes, string);
  event Bool(bool, string);
  event Uint(uint256, string);
  event Address(address, string);
  event String(string, string);

  /*
   * Structure for trade details.
   * @property {string} tradeId The trade identifier.
   * @property {string} sender The sending party in the trade.
   * @property {string} receiver The receiving party of the trade.
   * @property {uint256} amount The trading amount.
   */
  struct TradeDetails {
    string tradeId;
    string sender;
    string receiver;
    uint256 amount;
  }

  constructor() {
    owner = msg.sender;
  }

  /*
   * Set the asset token hold notary identifier. This method needs access controls.
   * @param {string} notary Asset token hold notary identifier.
   * @return {bool} Returns true if the asset token hold notary identifier was successfully updated.
   */
  function setNotaryId(
    string calldata notary
  ) public returns (bool) {
    require(
      msg.sender == owner,
      "Only the owner can set the notaryId of this contract"
    );
    notaryId = notary;
    return true;
  }

  /*
   * Add a remote to local mapping, from the remote account identifier to the local account identifier, in storage. This method needs access controls.
   * @param {string} localAccountId Local account identifier.
   * @param {string} remoteAccountId Remote account identifier.
   * @return {bool} Returns true if the remote to local mapping was successfully added.
   */
  function setRemoteAccountIdToLocalAccountId(
    string calldata localAccountId,
    string calldata remoteAccountId
  ) public returns (bool) {
    require(
      msg.sender == owner,
      "Only the owner can call setRemoteAccountIdToLocalAccountId"
    );
    remoteToLocalAccountId[remoteAccountId] = localAccountId;
    return true;
  }

  /*
   * Remove a remote to local mapping, from the remote account identifier to the local account identifier, in storage. This method needs access controls.
   * @param {string} remoteAccountId Remote account identifier.
   * @return {bool} Returns true if the remote to local mapping was successfully removed.
   */
  function removeRemoteAccountIdToLocalAccountId(
    string calldata remoteAccountId
  ) public returns (bool) {
    require(
      msg.sender == owner,
      "Only the owner can call removeRemoteAccountIdToLocalAccountId"
    );
    delete remoteToLocalAccountId[remoteAccountId];
    return true;
  }

  /*
   * Retrieve a remote to local mapping, for the given remote account identifier, in storage.
   * @param {string} remoteAccountId Remote account identifier.
   * @return {string} Returns the local account identifier as mapped to by the provided remote account identifier.
   */
  function getRemoteAccountIdToLocalAccountId(
    string calldata remoteAccountId
  ) public returns (string memory) {
    return remoteToLocalAccountId[remoteAccountId];
  }

  /*
   * Set the function call layer's contract address.
   * @param {address} contractAddress The function call layer's contract address.
   * @return {bool} Returns true if the function call layer's contract address was successfully updated.
   */
  function setFunctionCallContractAddress(
    address contractAddress
  ) public returns (bool) {
    require(
      msg.sender == owner,
      "Only the owner can set the functionCallContractAddress"
    );
    functionCallContractAddress = contractAddress;
    return true;
  }

  /*
   * Set the asset token contract address.
   * @param {address} contractAddress The asset token contract address.
   * @return {bool} Returns true if the asset token contract address was successfully updated.
   */
  function setTokenContractAddress(
    address contractAddress
  ) public returns (bool) {
    require(
      msg.sender == owner,
      "Only the owner can set the tokenContractAddress"
    );
    tokenContractAddress = contractAddress;
    return true;
  }

  /*
   * Check if a trade was cancelled against the given trade identifier.
   * @param {string} tradeId The trade identifier.
   * @return {bool} Returns true if a trade was cancelled against the given trade identifier.
   */
  function getIsCancelled(
    string calldata tradeId
  ) public view returns (bool) {
    return isCancelled[tradeId];
  }

  /*
   * Helper function to convert bytes to hex-encoded bytes.
   * @param {byte16} data The bytes to convert.
   * @return {bytes32} result The resulting hex-encoded bytes.
   */
  function toHex16(
    bytes16 data
  ) internal pure returns (bytes32 result) {
    result = bytes32(data) & 0xFFFFFFFFFFFFFFFF000000000000000000000000000000000000000000000000 |
    (bytes32(data) & 0x0000000000000000FFFFFFFFFFFFFFFF00000000000000000000000000000000) >> 64;
    result = result & 0xFFFFFFFF000000000000000000000000FFFFFFFF000000000000000000000000 |
    (result & 0x00000000FFFFFFFF000000000000000000000000FFFFFFFF0000000000000000) >> 32;
    result = result & 0xFFFF000000000000FFFF000000000000FFFF000000000000FFFF000000000000 |
    (result & 0x0000FFFF000000000000FFFF000000000000FFFF000000000000FFFF00000000) >> 16;
    result = result & 0xFF000000FF000000FF000000FF000000FF000000FF000000FF000000FF000000 |
    (result & 0x00FF000000FF000000FF000000FF000000FF000000FF000000FF000000FF0000) >> 8;
    result = (result & 0xF000F000F000F000F000F000F000F000F000F000F000F000F000F000F000F000) >> 4 |
    (result & 0x0F000F000F000F000F000F000F000F000F000F000F000F000F000F000F000F00) >> 8;
    result = bytes32(0x3030303030303030303030303030303030303030303030303030303030303030 +
    uint256(result) +
      (uint256(result) + 0x0606060606060606060606060606060606060606060606060606060606060606 >> 4 &
      0x0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F0F) * 39);
  }

  /*
   * Helper function to convert bytes to a hex-encoded string.
   * @param {byte32} data The bytes to convert.
   * @return {string} result A string representation of the resulting hex-encoded bytes.
   */
  function toHex(
    bytes32 data
  ) public pure returns (string memory) {
    return string(abi.encodePacked(toHex16(bytes16(data)), toHex16(bytes16(data << 128))));
  }

  /*
   * Helper function to generate an unique operation identifier from the given trade details.
   * @param {string} tradeId The trade identifier.
   * @param {string} sender The sending party's account identifier,
   * @param {string} receiver the receiving party's account identifier.
   * @return {string} The generated operation identifier.
   */
  function getOperationIdFromTradeId(
    string calldata tradeId,
    string calldata sender,
    string calldata receiver
  ) public view returns (string memory) {
    return toHex(keccak256(abi.encodePacked(tradeId, sender, receiver)));
  }

  /*
   * Structure for asset token holds.
   * @property {string} operationId The unique operation identifier.
   * @property {string} fromAccount The account to put tokens on hold from.
   * @property {string} toAccount The account to put token on hold for.
   * @property {string} notaryId The hold notary identifier.
   * @property {uint256} amount The amount of tokens to put on hold.
   * @property {bytes32} status The current status of the hold.
   */
  struct Hold {
    string operationId;
    string fromAccount;
    string toAccount;
    string notaryId;
    uint256 amount;
    bytes32 status;
  }

  /*
   * Retrieve asset token hold information for the given trade details.
   * @param {TradeDetails} tradeDetails The trade details.
   * @param {IToken} token The asset token contract to retrieve the hold details from.
   * @return {Hold} The hold information.
   */
  function getHold(
    TradeDetails calldata tradeDetails,
    IToken token
  ) public view returns (Hold memory){
    string memory operationId = getOperationIdFromTradeId(tradeDetails.tradeId, tradeDetails.sender, tradeDetails.receiver);
    (
    string memory fromAccount,
    string memory toAccount,
    string memory notaryId,
    uint256 amount,
    ,, bytes32 holdStatus,,
    ) = token.getHoldData(operationId);
    return Hold(operationId, fromAccount, toAccount, notaryId, amount, holdStatus);
  }

  /*
   * Verify the provided hold information, against the hold information in the asset token token contract, for the given trade details.
   * @param {Hold} The hold information.
   * @param {TradeDetails} tradeDetails The trade details.
   * @param {IToken} token The asset token contract to retrieve the hold details from.
   * @return {bool} Returns true if the hold information was verified successfully.
   */
  function checkHold(
    Hold calldata hold,
    TradeDetails calldata tradeDetails,
    IToken token
  ) public returns (bool) {
    string memory holdCheckRevertMessage = string(abi.encodePacked("Hold does not exist: ", hold.operationId));
    holdCheckRevertMessage = string(abi.encodePacked(holdCheckRevertMessage, " calculated using fromAccount: "));
    holdCheckRevertMessage = string(abi.encodePacked(holdCheckRevertMessage, hold.fromAccount));
    holdCheckRevertMessage = string(abi.encodePacked(holdCheckRevertMessage, " toAccount: "));
    holdCheckRevertMessage = string(abi.encodePacked(holdCheckRevertMessage, hold.toAccount));
    holdCheckRevertMessage = string(abi.encodePacked(holdCheckRevertMessage, " tradeId: "));
    holdCheckRevertMessage = string(abi.encodePacked(holdCheckRevertMessage, tradeDetails.tradeId));

    require(hold.status != token._HOLD_STATUS_NON_EXISTENT(), holdCheckRevertMessage);
    require(hold.status != token._HOLD_STATUS_CANCELLED(), "Hold was cancelled");
    require(hold.status == token._HOLD_STATUS_PERPETUAL(), "Hold is not perpetual");
    require(keccak256(abi.encodePacked(hold.fromAccount)) == keccak256(abi.encodePacked(tradeDetails.sender)), "Incorrect hold from account");
    require(keccak256(abi.encodePacked(hold.toAccount)) == keccak256(abi.encodePacked(tradeDetails.receiver)), "Incorrect hold to account");
    require(keccak256(abi.encodePacked(hold.notaryId)) == keccak256(abi.encodePacked(notaryId)), "Contract not set as notary for hold");

    return true;
  }

  /*
   * Check if a hold is perpetual in the asset token contract with given address.
   * A perpetual hold is a hold that can only be cancelled by the notary or the account holder to whom the tokens will move (once the hold is executed).
   * That is, the entity placing the hold may not cancel it, and the hold will not expire.
   * @param {string} operationId The operation identifier used to retrieve the hold details.
   * @param {address} tokenAddress The asset token contract address.
   * @return {bool} Returns true if the hold is perpetual.
   */
  function isHoldPerpetual(
    string calldata operationId,
    address tokenAddress
  ) public returns (bool) {
    IToken token = IToken(tokenAddress);
    (
    ,,,,,,bytes32 holdStatus,,
    ) = token.getHoldData(operationId);

    if (holdStatus == token._HOLD_STATUS_PERPETUAL()) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * Determines if the hold can be cancelled in the asset token contract with given address.
   * @param {string} operationId The operation identifier used to retrieve the hold details.
   * @param {address} tokenAddress The asset token contract address.
   * @return {bool} Returns true if the hold can be cancelled.
   */
  function isHoldCancellable(
    string calldata operationId,
    address tokenAddress
  ) public returns (bool) {
    IToken token = IToken(tokenAddress);
    (
    ,,,,,,bytes32 holdStatus,,
    ) = token.getHoldData(operationId);

    if (holdStatus != token._HOLD_STATUS_NON_EXISTENT() && holdStatus != token._HOLD_STATUS_CANCELLED() && holdStatus == token._HOLD_STATUS_PERPETUAL()) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * Determines if the hold can be executed in the asset token contract with given address.
   * @param {string} operationId The operation identifier used to retrieve the hold details.
   * @param {address} tokenAddress The asset token contract address.
   * @return {bool} Returns true if the hold can be executed.
   */
  function isHoldExecutable(
    string calldata operationId,
    address tokenAddress
  ) public returns (bool) {
    IToken token = IToken(tokenAddress);
    (
    ,,,,,,bytes32 holdStatus,,
    ) = token.getHoldData(operationId);

    if (holdStatus != token._HOLD_STATUS_NON_EXISTENT() && holdStatus != token._HOLD_STATUS_CANCELLED()) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * Determines if the hold exists in the asset token contract with given address.
   * @param {string} operationId The operation identifier used to retrieve the hold details.
   * @param {address} tokenAddress The asset token contract address.
   * @return {bool} Returns true if the hold exists.
   */
  function isHoldExisting(
    string calldata operationId,
    address tokenAddress
  ) public returns (bool) {
    IToken token = IToken(tokenAddress);
    (
    ,,,,,,bytes32 holdStatus,,
    ) = token.getHoldData(operationId);

    if (holdStatus != token._HOLD_STATUS_NON_EXISTENT()) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * Start the lead leg of a trade, with the given trade details, by emitting a CrosschainFunctionCall event.
   * @param {TradeDetails} tradeDetails The trade details containing the sender and receiver's local account identifiers.
   * @param {uint256} networkId The source network identifier.
   * @param {uint256} destinationNetworkId The destination network identifier.
   * @param {address} destinationContract The destination contract address.
   * @return {bool} Returns true if the lead leg was successfully started.
   */
  function startLeadLeg(
    TradeDetails calldata tradeDetails,
    uint256 sourceNetworkId,
    uint256 destinationNetworkId,
    address destinationContract
  ) public returns (bool) {

    IToken token = IToken(tokenContractAddress);
    Hold memory hold = getHold(tradeDetails, token);
    require(this.getIsCancelled(hold.operationId) == false, "OperationId for hold is marked as cancelled");
    require(this.checkHold(hold, tradeDetails, token) == true, "Hold for start leg failed checks");

    bytes4 SELECTOR = bytes4(
      keccak256(bytes("requestFollowLeg(string,string,string,address,uint256,uint256)"))
    );

    // Since we don't know on which remote network this will be consumed we can't change the sender and reciever to their remote account ids
    bytes memory functionCallData = abi.encodeWithSelector(
      SELECTOR,
      tradeDetails.tradeId,
      tradeDetails.receiver,
      tradeDetails.sender,
      address(this),
      sourceNetworkId,
      hold.amount
    );

    CrosschainFunctionCall functionCallContract = CrosschainFunctionCall(
      functionCallContractAddress
    );

    functionCallContract.outboundCall(
      destinationNetworkId,
      destinationContract,
      functionCallData
    );

    return true;
  }

  /*
   * Start the follow leg of a trade, with the given trade details, as instructed from a remote network.
   * The function should only be callable after a proof was verified.
   * @param {string} tradeId The trade identifier.
   * @param {string} sender The sending party's remote account identifier
   * @param {string} receiver The receiving party's remote account identifier.
   * @param {address} destinationContract The destination contract address.
   * @param {uint256} destinationNetworkId The destination network identifier.
   * @param {uint256} remoteNotional The nominal value of the trade on the remote network.
   * @return {bool} Returns true if the follow leg was successfully started.
   */
  function requestFollowLeg(
    string calldata tradeId,
    string calldata sender,
    string calldata receiver,
    address destinationContract,
    uint256 destinationNetworkId,
    uint256 remoteNotional
  ) public returns (bool) {

    TradeDetails memory tradeDetails = TradeDetails(tradeId, sender, receiver, remoteNotional);
    // First check that a mapping exists
    if (keccak256(abi.encodePacked(remoteToLocalAccountId[sender])) != keccak256(abi.encodePacked(""))) {
      tradeDetails.sender = remoteToLocalAccountId[sender];
    }
    if (keccak256(abi.encodePacked(remoteToLocalAccountId[receiver])) != keccak256(abi.encodePacked(""))) {
      tradeDetails.receiver = remoteToLocalAccountId[receiver];
    }

    IToken token = IToken(tokenContractAddress);
    Hold memory hold = this.getHold(tradeDetails, token);
    require(this.getIsCancelled(hold.operationId) == false, "OperationId for hold is marked as cancelled");
    require(this.checkHold(hold, tradeDetails, token) == true, "Hold for follow leg failed checks");

    string memory operationId = this.getOperationIdFromTradeId(tradeDetails.tradeId, tradeDetails.sender, tradeDetails.receiver);
    require(token.executeHold(operationId) == true, "Executing follow leg hold failed");

    bytes4 SELECTOR = bytes4(keccak256(bytes("completeLeadLeg(string,string,string,uint256)")));
    // Since we don't know on which remote network this will be consumed we can't change the sender and reciever to their remote account ids
    bytes memory functionCallData = abi.encodeWithSelector(
      SELECTOR,
      tradeDetails.tradeId,
      tradeDetails.receiver,
      tradeDetails.sender,
      hold.amount
    );

    CrosschainFunctionCall functionCallContract = CrosschainFunctionCall(
      functionCallContractAddress
    );
    functionCallContract.outboundCall(
      destinationNetworkId,
      destinationContract,
      functionCallData
    );

    return true;
  }

  /*
   * Complete the lead leg of a trade, with the given trade details, as instructed from a remote network.
   * @param {string} tradeId The trade identifier.
   * @param {string} sender The sending party's remote account identifier
   * @param {string} receiver The receiving party's remote account identifier.
   * @param {uint256} remoteNotional The nominal value of the trade on the remote network.
   * @return {bool} Returns true if the lead leg was successfully completed.
   */
  function completeLeadLeg(
    string calldata tradeId,
    string calldata sender,
    string calldata receiver,
    uint256 remoteNotional
  ) public returns (bool) {
    TradeDetails memory tradeDetails = TradeDetails(tradeId, sender, receiver, remoteNotional);
    // First check that a mapping exists
    if (keccak256(abi.encodePacked(remoteToLocalAccountId[sender])) != keccak256(abi.encodePacked(""))) {
      tradeDetails.sender = remoteToLocalAccountId[sender];
    }
    if (keccak256(abi.encodePacked(remoteToLocalAccountId[receiver])) != keccak256(abi.encodePacked(""))) {
      tradeDetails.receiver = remoteToLocalAccountId[receiver];
    }
    IToken token = IToken(tokenContractAddress);
    string memory operationId = this.getOperationIdFromTradeId(tradeDetails.tradeId, tradeDetails.sender, tradeDetails.receiver);
    require(this.getIsCancelled(operationId) == false, "OperationId for hold is marked as cancelled");
    require(this.isHoldExecutable(operationId, tokenContractAddress) == true, "Hold was not executable, or does not exist");
    require(token.executeHold(operationId) == true, "Executing hold on leader network failed");
    return true;
  }

  /*
   * Start the cancellation process of a trade, with the given trade details, by emitting a CrosschainFunctionCall event.
   * @param {string} tradeId The trade identifier.
   * @param {string} sender The sending party's local account identifier
   * @param {string} receiver The receiving party's local account identifier.
   * @param {uint256} networkId The source network identifier.
   * @param {uint256} destinationNetworkId The destination network identifier.
   * @param {address} destinationContract The destination contract address.
   * @return {bool} Returns true if the cancellation process was successfully started.
   */
  function startCancellation(
    string calldata tradeId,
    string calldata sender,
    string calldata receiver,
    uint256 sourceNetworkId,
    uint256 destinationNetworkId,
    address destinationContract
  ) public returns (bool) {
    TradeDetails memory tradeDetails = TradeDetails(tradeId, sender, receiver, 0);
    if (keccak256(abi.encodePacked(remoteToLocalAccountId[sender])) != keccak256(abi.encodePacked(""))) {
      tradeDetails.sender = remoteToLocalAccountId[sender];
    }
    if (keccak256(abi.encodePacked(remoteToLocalAccountId[receiver])) != keccak256(abi.encodePacked(""))) {
      tradeDetails.receiver = remoteToLocalAccountId[receiver];
    }
    string memory operationId = this.getOperationIdFromTradeId(tradeDetails.tradeId, tradeDetails.sender, tradeDetails.receiver);

    require(this.isHoldExisting(operationId, tokenContractAddress) == false, "Hold already exists, cannot start cancellation");
    isCancelled[operationId] = true;

    bytes4 SELECTOR = bytes4(
      keccak256(bytes("performCancellation(string,string,string)"))
    );

    // Since we don't know on which remote network this will be consumed we can't change the sender and receiver to their remote account ids
    bytes memory functionCallData = abi.encodeWithSelector(
      SELECTOR,
      tradeDetails.tradeId,
      tradeDetails.receiver,
      tradeDetails.sender
    );

    CrosschainFunctionCall functionCallContract = CrosschainFunctionCall(
      functionCallContractAddress
    );

    functionCallContract.outboundCall(
      destinationNetworkId,
      destinationContract,
      functionCallData
    );

    return true;
  }

  /*
   * Complete the cancellation process of a trade, with the given trade details, as instructed from a remote network.
   * The function should only be callable after a proof was verified.
   * @param {string} tradeId The trade identifier.
   * @param {string} sender The sending party's remote account identifier
   * @param {string} receiver The receiving party's remote account identifier.
   * @return {bool} Returns true if the cancellation process was successfully completed.
   */
  function performCancellation(
    string calldata tradeId,
    string calldata sender,
    string calldata receiver
  ) public returns (bool) {

    TradeDetails memory tradeDetails = TradeDetails(tradeId, sender, receiver, 0);
    if (keccak256(abi.encodePacked(remoteToLocalAccountId[sender])) != keccak256(abi.encodePacked(""))) {
      tradeDetails.sender = remoteToLocalAccountId[sender];
    }
    if (keccak256(abi.encodePacked(remoteToLocalAccountId[receiver])) != keccak256(abi.encodePacked(""))) {
      tradeDetails.receiver = remoteToLocalAccountId[receiver];
    }
    string memory operationId = this.getOperationIdFromTradeId(tradeDetails.tradeId, tradeDetails.sender, tradeDetails.receiver);

    if (msg.sender != functionCallContractAddress) {
      require(isCancelled[operationId] == true, "Operation id must be marked as cancelled when the msg.sender is not the function call contract");
    }
    isCancelled[operationId] = true;

    // Cancel only when the hold exists
    if (this.isHoldCancellable(operationId, tokenContractAddress) == true) {
      IToken token = IToken(tokenContractAddress);
      require(token.cancelHold(operationId) == true, "Hold cancellation failed");
    }

    return true;
  }
}
