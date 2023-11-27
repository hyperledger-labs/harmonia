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
import "./HexBytes.sol";

// TODO: change swapId to bytes32 ?
// TODO: add and change notary to bytes32 ?
// TODO: optimize storage access

contract SwapVault {
    error ZERO_ADDRESS();
    error COMMIT_TO_SELF();
    error ALREADY_COMMITTED();
    error INVALID_AMOUNT();
    error INVALID_STATUS();
    error INVALID_CLAIM();
    error NOT_ENOUGH_SIGS(uint256 actual, uint256 expected);
    error UNSUPPORTED_INTERFACE();

    error INVALID_THRESHOLD();
    error INSUFFICIENT_SIGNERS();

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
        string notary;
        uint256 signaturesThreshold;
        bytes32[] signatures;
        address[] signers;
    }

    mapping(string => Commitment) _committmentState;

    using HexBytes for string;
    using HexBytes for bytes32;

    function commit(string calldata swapId, address recipient, uint256 signaturesThreshold) external {
        address[] memory signers = new address[](0);
        _commit(swapId, recipient, signaturesThreshold, signers);
    }

    function commit(string calldata swapId, address recipient, uint256 signaturesThreshold, address[] calldata signers)
        external
    {
        _commit(swapId, recipient, signaturesThreshold, signers);
    }

    function _commit(string calldata swapId, address recipient, uint256 signaturesThreshold, address[] memory signers)
        internal
        returns (Commitment storage commitment)
    {
        commitment = _committmentState[swapId];
        uint256 commitmentStatus = commitment.status++; // loads 0, stores 1
        if (recipient == address(0)) revert ZERO_ADDRESS();
        if (recipient == msg.sender) revert COMMIT_TO_SELF();
        if (commitmentStatus != 0) revert ALREADY_COMMITTED(); // loaded must be 0
        if (signaturesThreshold < 1) revert INVALID_THRESHOLD(); // at least on signer required, enforced only for Corda side unlock.
            // Signers array can be empty and only affect the EVM claim side.

        commitment.swapId = swapId;
        commitment.owner = msg.sender;
        commitment.recipient = recipient;
        commitment.signaturesThreshold = signaturesThreshold;
        commitment.signers = signers;
    }

    function commitWithToken(
        string calldata swapId,
        address tokenAddress,
        uint256 amount,
        address recipient,
        uint256 signaturesThreshold
    ) external {
        address[] memory signers = new address[](0);
        _commitWithToken(swapId, tokenAddress, amount, recipient, signaturesThreshold, signers);
    }

    function commitWithToken(
        string calldata swapId,
        address tokenAddress,
        uint256 amount,
        address recipient,
        uint256 signaturesThreshold,
        address[] calldata signers
    ) external {
        _commitWithToken(swapId, tokenAddress, amount, recipient, signaturesThreshold, signers);
    }

    function _commitWithToken(
        string calldata swapId,
        address tokenAddress,
        uint256 amount,
        address recipient,
        uint256 signaturesThreshold,
        address[] memory signers
    ) internal {
        Commitment storage commitment = _commit(swapId, recipient, signaturesThreshold, signers);

        commitment.amount = amount;
        commitment.tokenAddress = tokenAddress;

        SafeERC20.safeTransferFrom(IERC20(commitment.tokenAddress), msg.sender, address(this), commitment.amount);

        emit Commit(swapId, commitmentHash(swapId));
    }

    function commitWithToken(
        string calldata swapId,
        address tokenAddress,
        uint256 tokenId,
        uint256 amount,
        address recipient,
        uint256 signaturesThreshold
    ) external {
        address[] memory signers = new address[](0);
        _commitWithToken(swapId, tokenAddress, tokenId, amount, recipient, signaturesThreshold, signers);
    }

    function commitWithToken(
        string calldata swapId,
        address tokenAddress,
        uint256 tokenId,
        uint256 amount,
        address recipient,
        uint256 signaturesThreshold,
        address[] calldata signers
    ) external {
        _commitWithToken(swapId, tokenAddress, tokenId, amount, recipient, signaturesThreshold, signers);
    }

    function _commitWithToken(
        string calldata swapId,
        address tokenAddress,
        uint256 tokenId,
        uint256 amount,
        address recipient,
        uint256 signaturesThreshold,
        address[] memory signers
    ) internal {
        Commitment storage commitment = _commit(swapId, recipient, signaturesThreshold, signers);

        commitment.amount = amount;
        commitment.tokenId = tokenId;
        commitment.tokenAddress = tokenAddress;

        if (IERC165(tokenAddress).supportsInterface(0x80ac58cd)) {
            // ERC721
            //
            if (commitment.amount != 1) revert INVALID_AMOUNT();
            IERC721(tokenAddress).transferFrom(msg.sender, address(this), tokenId);
        } else if (IERC165(tokenAddress).supportsInterface(0xd9b67a26)) {
            // ERC1155
            //
            if (commitment.amount < 1) revert INVALID_AMOUNT();
            IERC1155(tokenAddress).safeTransferFrom(msg.sender, address(this), tokenId, amount, bytes(""));
        } else {
            revert UNSUPPORTED_INTERFACE();
        }

        emit Commit(swapId, commitmentHash(swapId));
    }

    function claimCommitment(string calldata swapId) external {
        bytes[] memory signatures = new bytes[](0);
        _claimCommitment(swapId, signatures);
    }

    function claimCommitment(string calldata swapId, bytes[] calldata signatures) external {
        _claimCommitment(swapId, signatures);
    }

    function _claimCommitment(string calldata swapId, bytes[] memory signatures) internal {
        Commitment storage commitment = _committmentState[swapId];

        address tokenAddress = commitment.tokenAddress;

        uint256 commitmentStatus = commitment.status++; // loads 1, stores 2
        if (commitmentStatus != 1) revert INVALID_STATUS();

        if (
            msg.sender != commitment.owner
                && (msg.sender != commitment.recipient || !verifySignatureThreshold(swapId, signatures))
        ) {
            revert INVALID_CLAIM();
        }

        if (safeSupportsInterface(tokenAddress, 0x80ac58cd)) {
            // ERC721
            //
            IERC721(tokenAddress).transferFrom(address(this), commitment.recipient, commitment.tokenId);
        } else if (safeSupportsInterface(tokenAddress, 0xd9b67a26)) {
            // ERC1155
            //
            IERC1155(tokenAddress).safeTransferFrom(
                address(this), commitment.recipient, commitment.tokenId, commitment.amount, bytes("")
            );
        } else {
            SafeERC20.safeTransfer(IERC20(commitment.tokenAddress), commitment.recipient, commitment.amount);
        }

        emit Transfer(swapId, commitmentHash(swapId));
    }

    function verifySignatureThreshold(string calldata swapId, bytes[] memory signatures)
        internal
        view
        returns (bool success)
    {
        Commitment memory commitment = _committmentState[swapId];
        if (signatures.length < commitment.signaturesThreshold)
            revert NOT_ENOUGH_SIGS(signatures.length, commitment.signaturesThreshold);

        //string memory notaryPublicKey = ""; // TODO: add notary
        //bytes32 messageHash = keccak256(abi.encode(swapId, notaryPublicKey));
        bytes32 messageHash = keccak256(swapId.hexToBytes());

        bool[] memory signers = new bool[](commitment.signers.length);
        uint256 verifiedSignatures = 0;

        for (uint256 i; i < signatures.length; i++) {
            address signer = recoverSigner(messageHash, signatures[i]);
            for (uint256 j = 0; j < signers.length; j++) {
                if (signer == commitment.signers[j] && !signers[j]) {
                    signers[j] = true;
                    ++verifiedSignatures;
                    break;
                }
            }
        }

        success = verifiedSignatures >= commitment.signaturesThreshold;
    }

    function recoverSigner(bytes32 messageHash, bytes memory signature) public pure returns (address) {
        bytes32 r;
        bytes32 s;
        uint8 v;

        assembly {
            r := mload(add(signature, 32))
            s := mload(add(signature, 64))
            v := byte(0, mload(add(signature, 96)))
        }

        if (v != 27 && v != 28) {
            return address(0);
        }

        return ecrecover(messageHash, v, r, s);
    }

    function revertCommitment(string calldata swapId) external {
        Commitment storage commitment = _committmentState[swapId];

        address tokenAddress = commitment.tokenAddress;

        uint256 commitmentStatus = commitment.status++; // loads 1, stores 2
        if (commitmentStatus != 1) revert INVALID_STATUS();

        if (safeSupportsInterface(tokenAddress, 0x80ac58cd)) {
            // ERC721
            //
            IERC721(tokenAddress).transferFrom(address(this), commitment.owner, commitment.tokenId);
        } else if (safeSupportsInterface(tokenAddress, 0xd9b67a26)) {
            // ERC1155
            //
            IERC1155(tokenAddress).safeTransferFrom(
                address(this), commitment.owner, commitment.tokenId, commitment.amount, bytes("")
            );
        } else {
            SafeERC20.safeTransfer(IERC20(commitment.tokenAddress), commitment.owner, commitment.amount);
        }

        emit Revert(swapId, commitmentHash(swapId));
    }

    function commitmentHash(string calldata swapId) public view returns (bytes32 hash) {
        Commitment storage commitment = _committmentState[swapId];

        if (commitment.status < 1) revert INVALID_STATUS(); // must be committed at least

        hash = keccak256(
            abi.encode(
                block.chainid,
                commitment.owner,
                commitment.recipient,
                commitment.amount,
                commitment.tokenId,
                commitment.tokenAddress,
                commitment.signaturesThreshold,
                commitment.signers // TODO: sort?
            )
        );
    }

    function safeSupportsInterface(address addr, bytes4 interfaceId) internal view returns (bool supported) {
        (bool success, bytes memory result) =
            addr.staticcall(abi.encodeWithSelector(IERC165.supportsInterface.selector, interfaceId));

        supported = success && result.length > 0 && abi.decode(result, (bool));
    }
}
