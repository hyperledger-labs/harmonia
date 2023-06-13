// SPDX-License-Identifier: MIT
pragma solidity ^0.8.16;

import "./ERC20.sol";

contract TestToken is ERC20 {
    uint256 constant _initial_supply = 1000000;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {
        _mint(msg.sender, _initial_supply * (10 ** decimals()));
    }
}
