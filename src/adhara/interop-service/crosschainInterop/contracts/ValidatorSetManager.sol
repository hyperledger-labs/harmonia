pragma solidity ^0.8.13;

import "contracts/interfaces/IValidatorSetManager.sol";
import "contracts/interfaces/IInteroperable.sol";
import "contracts/interfaces/ICrosschainFunctionCall.sol";
import "contracts/libraries/SolidityUtils.sol";
import "contracts/libraries/RLP.sol";

contract ValidatorSetManager is IValidatorSetManager
{
  using RLP for RLP.RLPItem;
  using RLP for RLP.Iterator;
  using RLP for bytes;

  uint256 private constant MAX_VALIDATOR_SET_SIZE = 50;
  address[] storedValidators;
  uint256 lastUpdateAtBlock = 0;
  address imContractAddress;

  function getLastUpdate()
  external view returns (uint256) {
    return lastUpdateAtBlock;
  }

  function getValidators()
  external view override returns (address[] memory) {
    uint256 setSize = storedValidators.length;
    address[] memory validatorSet = new address[](setSize);
    for (uint256 i = 0; i < setSize; i++) {
      validatorSet[i] = storedValidators[i];
    }
    return validatorSet;
  }

  function setValidators(address[] calldata validators)
  external override {
    require(validators.length > 0, "Validator set size must be greater than 0");
    require(validators.length <= MAX_VALIDATOR_SET_SIZE, "Validator set size exceeds maximum");
    uint256 i = 0;
    while (storedValidators.length > validators.length) {
      storedValidators.pop();
    }
    for (; i < storedValidators.length; i++) {
      storedValidators[i] = validators[i];
    }
    for (; i < validators.length; i++) {
      storedValidators.push(validators[i]);
    }
    lastUpdateAtBlock = block.number;
    emit SetValidatorsExecuted(lastUpdateAtBlock, validators);
  }

  /*
   * Set local validator set and start the process of updating the validator set configurations on remote destination chains.
   * @param {address[]} Contains new list of validators
   */
  function setValidatorsAndSyncRemotes(address[] calldata validators)
  external override {
    this.setValidators(validators);
    if (imContractAddress == address(0)) {
      revert("The interop manager is not configured correctly");
    }
    uint256 startIndex = 0;
    bool moreItems = true;
    uint256[] memory items;
    while (moreItems) {
      (items, moreItems, ,) = IInteroperable(imContractAddress).listRemoteDestinationNetworks(startIndex, 0);
      for (uint256 i=0; i<items.length; i++) {
        emit SetValidatorsAndSyncRemoteExecuted(items[i], lastUpdateAtBlock, validators);
        bytes memory functionCallData = abi.encodeWithSelector(
          getValidatorSetRemoteUpdateSelector(),
          items[i],
          lastUpdateAtBlock,
          validators
        );
        (address connectorAddress,) = IInteroperable(imContractAddress).getRemoteDestinationNetworkData(items[i]);
        ICrosschainFunctionCall(imContractAddress).outboundCall(items[i], connectorAddress, functionCallData);
      }
    }
    if (moreItems) {
      startIndex += startIndex + items.length;
    }
  }

  bytes4 private constant VALIDATOR_SET_REMOTE_UPDATE_SELECTOR = bytes4(keccak256(bytes("setValidatorList(uint256,uint256,address[])")));

  function getValidatorSetRemoteUpdateSelector() internal pure returns (bytes4) {
    return VALIDATOR_SET_REMOTE_UPDATE_SELECTOR;
  }

  function getValidatorSetRemoteUpdateOperationId(string memory operationId, uint256 remoteDestinationNetwork) internal pure returns (string memory) {
    return SolUtils.UIntToString(uint256(keccak256(abi.encodePacked(operationId, remoteDestinationNetwork))));
  }

  /* Indices into an Ethereum block header */
  uint256 private constant BLOCK_HEADER_EXTRA_DATA_INDEX = 12;
  uint256 private constant BLOCK_HEADER_BLOCK_NUMBER_INDEX = 8;

  /*
   * Start the process of updating the validator set configurations on remote destination chains.
   * @return {bool} Returns true if the process of updating the validator set configuration remotely, was successfully started.
   */
  function getValidatorsAndSyncRemotes()
  external override {
    if (imContractAddress == address(0)) {
      revert("The interop manager is not configured correctly");
    }
    address[] memory proposedValidatorList = this.getValidators();
    require(proposedValidatorList.length > 0, "Found no validators in contract");
    uint256 startIndex = 0;
    bool moreItems = true;
    uint256[] memory items;
    while (moreItems) {
      (items, moreItems, ,) = IInteroperable(imContractAddress).listRemoteDestinationNetworks(startIndex, 0);
      for (uint256 i=0; i<items.length; i++) {
        emit GetValidatorsAndSyncRemoteExecuted(items[i], lastUpdateAtBlock, proposedValidatorList);
        bytes memory functionCallData = abi.encodeWithSelector(
          getValidatorSetRemoteUpdateSelector(),
          items[i],
          lastUpdateAtBlock,
          proposedValidatorList
        );
        (address connectorAddress,) = IInteroperable(imContractAddress).getRemoteDestinationNetworkData(items[i]);
        ICrosschainFunctionCall(imContractAddress).outboundCall(items[i], connectorAddress, functionCallData);
      }
    }
    if (moreItems) {
      startIndex += startIndex + items.length;
    }
  }

  function getInteropManager()
  external view override returns (address) {
    return imContractAddress;
  }

  function setInteropManager(address interopManager)
  external override returns (bool)
  {
    require(
      isContract(interopManager),
      string(abi.encodePacked("Address does not correspond to a contract address"))
    );
    imContractAddress = interopManager;
    emit SetInteropManagerExecuted(imContractAddress);
    return true;
  }

  function isContract(address contractAddress)
  internal view returns (bool) {
    uint32 size;
    assembly ("memory-safe")  {
      size := extcodesize(contractAddress)
    }
    return (size > 0);
  }
}


