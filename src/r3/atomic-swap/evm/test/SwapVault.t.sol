// SPDX-License-Identifier: Apache-2.0

/**
 *
 * Copyright 2023 R3 LLC                                                      *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 *
 */

pragma solidity ^0.8.0;

import "forge-std/Test.sol";
import "../src/SwapVault.sol";
import "openzeppelin/utils/cryptography/ECDSA.sol";
import "openzeppelin/token/ERC20/ERC20.sol";

contract MyERC20 is ERC20 {
    constructor(address alice, address bob) ERC20("My Token", "MTK") {
        _mint(msg.sender, 1000000 ether);
        _mint(alice, 1000000 ether);
        _mint(bob, 1000000 ether);
    }
}

contract SwapVaultTest is Test {
    SwapVault public messages;
    MyERC20 myERC20;

    uint256 bobPrivKey;
    address bobAddress;
    uint256 alicePrivKey;
    address aliceAddress;

    bytes32 public constant EXECUTE_TYPEHASH =
        keccak256("Execute(uint256 transactionId,address sender,address recipient,Commitment command)");

    event Setup();

    function setUp() public {
        messages = new SwapVault();
        bobPrivKey = vm.deriveKey(
            "mammal clap slam suspect crime proud acoustic baby gallery approve decrease kitchen brief defense life", 0
        );
        alicePrivKey = vm.deriveKey(
            "auction farm useless muffin eager zoo stadium indoor endless entire resist divert spin bundle foil", 0
        );
        bobAddress = vm.addr(bobPrivKey);
        aliceAddress = vm.addr(alicePrivKey);

        myERC20 = new MyERC20(aliceAddress, bobAddress);
        //myERC721 = new MyERC721(aliceAddress, bobAddress);

        vm.startPrank(aliceAddress);
        myERC20.approve(address(messages), type(uint256).max);
        vm.stopPrank();

        vm.startPrank(bobAddress);
        myERC20.approve(address(messages), type(uint256).max);
        vm.stopPrank();

        emit Setup();
    }

    function testERC20Commit() public {
        string memory transactionId = string("0x1234");

        vm.startPrank(aliceAddress);
        messages.commitWithToken(transactionId, address(myERC20), 1, bobAddress, 3);
        vm.stopPrank();
    }

    function testERC20AliceCanClaimCommitmentForRecipient() public {
        string memory transactionId = string("0x1234");

        uint256 aliceBalanceBefore = myERC20.balanceOf(aliceAddress);
        uint256 bobBalanceBefore = myERC20.balanceOf(bobAddress);

        vm.startPrank(aliceAddress);
        messages.commitWithToken(transactionId, address(myERC20), 1, bobAddress, 3);
        messages.claimCommitment(transactionId);
        vm.stopPrank();

        uint256 aliceBalanceAfter = myERC20.balanceOf(aliceAddress);
        uint256 bobBalanceAfter = myERC20.balanceOf(bobAddress);

        require(aliceBalanceAfter == aliceBalanceBefore - 1);
        require(bobBalanceAfter == bobBalanceBefore + 1);
    }

    function testERC20BobCanClaimCommitmentForRecipient() public {
        string memory transactionId = string("0x1234");

        uint256 aliceBalanceBefore = myERC20.balanceOf(aliceAddress);
        uint256 bobBalanceBefore = myERC20.balanceOf(bobAddress);

        vm.startPrank(aliceAddress);
        messages.commitWithToken(transactionId, address(myERC20), 1, bobAddress, 3);
        vm.stopPrank();

        // NOTE: Bob is the recipient of the asset, therefore he cannot claim the asset that
        //       Alice committed, by himself. He should provide proofs to show that the asset
        //       he had to lock, is now locked. Hence revert!
        vm.expectRevert();

        vm.startPrank(bobAddress);
        messages.claimCommitment(transactionId);
        vm.stopPrank();

        uint256 aliceBalanceAfter = myERC20.balanceOf(aliceAddress);
        uint256 bobBalanceAfter = myERC20.balanceOf(bobAddress);

        require(aliceBalanceAfter == aliceBalanceBefore - 1);
        require(bobBalanceAfter == bobBalanceBefore);
    }

    function testERC20AliceCanRevertCommitmentForOwner() public {
        string memory transactionId = string("0x1234");

        uint256 aliceBalanceBefore = myERC20.balanceOf(aliceAddress);
        uint256 bobBalanceBefore = myERC20.balanceOf(bobAddress);

        vm.startPrank(aliceAddress);
        messages.commitWithToken(transactionId, address(myERC20), 1, bobAddress, 3);
        messages.revertCommitment(transactionId);
        vm.stopPrank();

        uint256 aliceBalanceAfter = myERC20.balanceOf(aliceAddress);
        uint256 bobBalanceAfter = myERC20.balanceOf(bobAddress);

        require(aliceBalanceAfter == aliceBalanceBefore);
        require(bobBalanceAfter == bobBalanceBefore);
    }

    function testERC20BobCanRevertCommitmentForOwner() public {
        string memory transactionId = string("0x1234");

        uint256 aliceBalanceBefore = myERC20.balanceOf(aliceAddress);
        uint256 bobBalanceBefore = myERC20.balanceOf(bobAddress);

        vm.startPrank(aliceAddress);
        messages.commitWithToken(transactionId, address(myERC20), 1, bobAddress, 3);
        vm.stopPrank();

        vm.startPrank(bobAddress);
        messages.revertCommitment(transactionId);
        vm.stopPrank();

        uint256 aliceBalanceAfter = myERC20.balanceOf(aliceAddress);
        uint256 bobBalanceAfter = myERC20.balanceOf(bobAddress);

        require(aliceBalanceAfter == aliceBalanceBefore);
        require(bobBalanceAfter == bobBalanceBefore);
    }
}
