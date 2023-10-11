// SPDX-License-Identifier: Apache-2.0

/******************************************************************************
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
 ******************************************************************************/

pragma solidity 0.8.20;

import "openzeppelin/token/ERC20/IERC20.sol";
import "openzeppelin/token/ERC20/extensions/IERC20Metadata.sol";
import "openzeppelin/token/ERC20/utils/SafeERC20.sol";
import "openzeppelin/token/ERC721/IERC721.sol";
import "openzeppelin/token/ERC1155/IERC1155.sol";
import "openzeppelin/token/ERC721/utils/ERC721Holder.sol";
import "openzeppelin/token/ERC1155/utils/ERC1155Holder.sol";

contract SwapVault {
    
    error ZERO_ADDRESS();
    error COMMIT_TO_SELF();
    error ALREADY_COMMITTED();
    error INVALID_AMOUNT();
    error INVALID_STATUS();
    error INVALID_CLAIMER();
    error UNSUPPORTED_INTERFACE();

    event Commit(string indexed swapId, bytes32 holdHash);
    event Transfer(string indexed swapId, bytes32 holdHash);
    event Revert(string indexed swapId, bytes32 holdHash);

    struct Commitment {
        string swapId;
        uint256 status;
        address owner;
        address recipient;
        uint256 amount;
        uint256 tokenId;
        address tokenAddress;
        uint256 signaturesThreshold;                
        bytes32[] signatures;
    }

    mapping(string => Commitment) _committmentState;

    function commit(string calldata swapId, address recipient, uint256 signaturesThreshold) external {
        _commit(swapId, recipient, signaturesThreshold);
    }

    function commitWithToken(string calldata swapId, address tokenAddress, uint256 amount, address recipient, uint256 signaturesThreshold) external {
        Commitment storage commitment =  _commit(swapId, recipient, signaturesThreshold);

        commitment.amount = amount;
        commitment.tokenAddress = tokenAddress;

        SafeERC20.safeTransferFrom(IERC20(commitment.tokenAddress), msg.sender, address(this), commitment.amount);

        emit Commit(swapId, commitmentHash(swapId));
    }

    function commitWithToken(string calldata swapId, address tokenAddress, uint256 tokenId, uint256 amount, address recipient, uint256 signaturesThreshold) external {
        Commitment storage commitment = _commit(swapId, recipient, signaturesThreshold);

        commitment.amount = amount;
        commitment.tokenId = tokenId;
        commitment.tokenAddress = tokenAddress;

        if(IERC165(tokenAddress).supportsInterface(0x80ac58cd)) { // ERC721
            // 
            if(commitment.amount != 1) revert INVALID_AMOUNT();
            IERC721(tokenAddress).transferFrom(msg.sender, address(this), tokenId);
        } else if(IERC165(tokenAddress).supportsInterface(0xd9b67a26)) { // ERC1155 
            //
            if(commitment.amount < 1) revert INVALID_AMOUNT();
            IERC1155(tokenAddress).safeTransferFrom(msg.sender, address(this), tokenId, amount, bytes(""));
        } else {
            revert UNSUPPORTED_INTERFACE();
        }

        emit Commit(swapId, commitmentHash(swapId));
    }

    function claimCommitment(string calldata swapId) external {
        Commitment storage commitment = _committmentState[swapId];
        
        address tokenAddress = commitment.tokenAddress;

        uint256 commitmentStatus = commitment.status++; // loads 1, stores 2
        if(commitmentStatus != 1) revert INVALID_STATUS();

        // TODO: Bob should not be able to transfer Alice-to-Bob commitment unless
        //       he has proofs about swap id being notarised on Corda
        if(msg.sender != commitment.owner) revert INVALID_CLAIMER();

        if(safeSupportsInterface(tokenAddress, 0x80ac58cd)) { // ERC721
            // 
            IERC721(tokenAddress).transferFrom(address(this), commitment.recipient, commitment.tokenId);
        } else if(safeSupportsInterface(tokenAddress, 0xd9b67a26)) { // ERC1155 
            //
            IERC1155(tokenAddress).safeTransferFrom(address(this), commitment.recipient, commitment.tokenId, commitment.amount, bytes(""));
        } else {
            SafeERC20.safeTransfer(IERC20(commitment.tokenAddress), commitment.recipient, commitment.amount);
        }

        emit Transfer(swapId, commitmentHash(swapId));
    }

    function revertCommitment(string calldata swapId) external {
        Commitment storage commitment = _committmentState[swapId];
        
        address tokenAddress = commitment.tokenAddress;

        uint256 commitmentStatus = commitment.status++; // loads 1, stores 2
        if(commitmentStatus != 1) revert INVALID_STATUS();

        if(safeSupportsInterface(tokenAddress, 0x80ac58cd)) { // ERC721
            // 
            IERC721(tokenAddress).transferFrom(address(this), commitment.owner, commitment.tokenId);
        } else if(safeSupportsInterface(tokenAddress, 0xd9b67a26)) { // ERC1155 
            //
            IERC1155(tokenAddress).safeTransferFrom(address(this), commitment.owner, commitment.tokenId, commitment.amount, bytes(""));
        } else {
            SafeERC20.safeTransfer(IERC20(commitment.tokenAddress), commitment.owner, commitment.amount);
        }

        emit Revert(swapId, commitmentHash(swapId));
    }

    function commitmentHash(string calldata swapId) public view returns (bytes32 hash) {
        Commitment storage commitment = _committmentState[swapId];
        
        if(commitment.status < 1) revert INVALID_STATUS(); // must be committed at least

        hash = keccak256(abi.encode(
            block.chainid,
            commitment.owner,
            commitment.recipient,
            commitment.amount, 
            commitment.tokenId,
            commitment.tokenAddress,
            commitment.signaturesThreshold
        ));
    }

    function _commit(string calldata swapId, address recipient, uint256 signaturesThreshold) internal returns (Commitment storage commitment) {
        commitment = _committmentState[swapId];
        uint256 commitmentStatus = commitment.status++; // loads 0, stores 1
        if(recipient == address(0)) revert ZERO_ADDRESS();
        if(recipient == msg.sender) revert COMMIT_TO_SELF();
        if(commitmentStatus != 0) revert ALREADY_COMMITTED(); // loaded must be 0
        
        commitment.swapId = swapId;
        commitment.owner = msg.sender;
        commitment.recipient = recipient;
        commitment.signaturesThreshold = signaturesThreshold;
    }
    
    function safeSupportsInterface(address addr, bytes4 interfaceId) internal view returns (bool supported) {
        (bool success, bytes memory result) = addr.staticcall(
                abi.encodeWithSelector(IERC165.supportsInterface.selector, interfaceId)
            );
        
        supported = success && result.length > 0 && abi.decode(result, (bool));
    }    
}

