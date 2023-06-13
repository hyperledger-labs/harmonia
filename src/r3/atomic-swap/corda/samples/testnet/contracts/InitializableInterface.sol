//SPDX-License-Identifier: Unlicense
pragma solidity ^0.8.16;

interface InitializableInterface {
    /**
     * @notice Used internally to initialize the contract instead of through a constructor
     * @dev This function is called by the deployer/factory when creating a contract
     * @param initPayload abi encoded payload to use for contract initilaization
     */
    function init(bytes memory initPayload) external returns (bytes4);
}

abstract contract Initializable is InitializableInterface {
    bool private _initialized = false;

    /**
     * @dev Constructor is left empty and init is used instead
     */
    constructor() {}

    /**
     * @notice Used internally to initialize the contract instead of through a constructor
     * @dev This function is called by the deployer/factory when creating a contract
     * @param initPayload abi encoded payload to use for contract initilaization
     */
    function init(bytes memory initPayload) external virtual override returns (bytes4);

    function _isInitialized() internal view returns (bool initialized) {
        initialized = _initialized;
    }

    function _setInitialized() internal {
        _initialized = true;
    }
}
