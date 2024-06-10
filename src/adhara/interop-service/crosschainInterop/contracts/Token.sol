import "contracts/interfaces/IToken.sol";

pragma solidity ^0.8.13;

contract Token is IToken {
  address public owner;

  struct Hold {
    string _fromAccount;
    string _toAccount;
    string notaryId;
    uint256 _amount;
    uint256 _expiryTimestamp;
    string _metaData;
    bytes32 _holdStatus;
    bytes32 _holdType;
    bytes32 _signer;
  }

  mapping(string => uint256) balances;
  mapping(string => Hold) holds;
  mapping(string => address) notaries;

  constructor() {
    owner = msg.sender;
  }

  function createHold(
    string calldata operationId,
    string calldata fromAccount,
    string calldata toAccount,
    string calldata notaryId,
    uint256 amount,
    uint256 duration,
    string calldata metaData
  ) external override returns (bool) {
    require(balances[fromAccount] >= amount, "Insufficient balance to place hold");
    balances[fromAccount] -= amount;
    Hold memory newHold = Hold(fromAccount, toAccount, notaryId, amount, uint256(0), metaData, IToken._HOLD_STATUS_PERPETUAL, IToken._HOLD_TYPE_NORMAL, "");
    holds[operationId] = newHold;
    //emit CreateHoldExecuted(operationId, fromAccount, toAccount, notaryId, amount, metaData);
    return true;
  }

  function executeHold(
    string calldata operationId
  ) external override returns (bool) {
    Hold memory holdToExecute = holds[operationId];
    require(keccak256(abi.encodePacked(holdToExecute._fromAccount)) != keccak256(abi.encodePacked("")), "Hold does not exist");
    balances[holdToExecute._toAccount] += holdToExecute._amount;
    delete holds[operationId];
    emit ExecuteHoldExecuted(operationId);
    return true;
  }

  function cancelHold(
    string calldata operationId
  ) external override returns (bool) {
    Hold memory holdToCancel = holds[operationId];
    require(keccak256(abi.encodePacked(holdToCancel._fromAccount)) != keccak256(abi.encodePacked("")), "Hold does not exist");
    balances[holdToCancel._fromAccount] += holdToCancel._amount;
    delete holds[operationId];
    emit CancelHoldExecuted(operationId);
    return true;
  }

  function getHoldData(string calldata operationId)
  external override view virtual
  returns (
    string memory fromAccount,
    string memory toAccount,
    string memory notaryId,
    uint256 amount,
    uint256 expiryTimestamp,
    string memory metaData,
    bytes32 holdStatus,
    bytes32 holdType,
    bytes32 signer
  ) {
    Hold memory holdToReturn = holds[operationId];

    if (keccak256(abi.encodePacked(holdToReturn._fromAccount)) == keccak256(abi.encodePacked(""))) {
      holdToReturn._holdStatus = IToken._HOLD_STATUS_NON_EXISTENT;
    }
    //require(holdToReturn._holdStatus != IToken._HOLD_STATUS_NON_EXISTENT, "Hold does not exist");
    return (holdToReturn._fromAccount,
            holdToReturn._toAccount,
            holdToReturn.notaryId,
            holdToReturn._amount,
            holdToReturn._expiryTimestamp,
            holdToReturn._metaData,
            holdToReturn._holdStatus,
            holdToReturn._holdType,
            holdToReturn._signer);
  }

  function addHoldNotary(
    string calldata notaryId,
    address holdNotaryAdminAddress
  ) external override returns (bool) {
    notaries[notaryId] = holdNotaryAdminAddress;
    return true;
  }

  function isHoldNotary(string calldata notaryId)
  external override view returns (bool)
  {
    return notaries[notaryId] != address(0);
  }

  function makeHoldPerpetual(string calldata operationId)
  external override returns (bool)
  {
    Hold memory holdToChange = holds[operationId];
    holdToChange._holdStatus = IToken._HOLD_STATUS_PERPETUAL;
    emit MakeHoldPerpetualExecuted(operationId);
    return true;
  }

  function create(
    string calldata operationId,
    string calldata toAccount,
    uint256 amount,
    string calldata metaData
  ) external override returns (bool) {
    require(msg.sender == owner, "Only the owner can create new tokens");
    balances[toAccount] += amount;
    return true;
  }

  function destroy(
    string calldata operationId,
    string calldata fromAccount,
    uint256 amount,
    string calldata metaData
  ) external override returns (bool) {
    require(msg.sender == owner, "Only the owner can destroy existing tokens");
    require(balances[fromAccount] >= amount, "Not enough tokens in existence to destroy");
    balances[fromAccount] -= amount;
    return true;
  }

  function transfer(
    string calldata operationId,
    string calldata fromAccount,
    string calldata toAccount,
    uint256 amount,
    string calldata metaData
  ) external override returns (bool) {
    require(msg.sender == owner, "Only the owner can transfer tokens");
    require(balances[fromAccount] >= amount, "Not enough tokens in existence to transfer");
    balances[fromAccount] -= amount;
    balances[toAccount] += amount;
    return true;
  }

  function getAvailableBalanceOf(string calldata account) external override view returns (uint256) {
    return balances[account];
  }
}

