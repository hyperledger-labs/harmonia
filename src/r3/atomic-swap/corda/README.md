# Corda-EVM Interoperability

## Introduction
This project is an experimental reference implementation of Corda-EVM interoperability. It is not intended for production use and may have limitations and bugs. Please use this code for reference and experimentation only.

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.

## Development Status

This project is an experimental reference implementation and is considered complete for its intended purpose. While it may undergo minor updates to address any critical issues that may arise, no major changes are expected in the near future.

### Component Overview

The following is a list of the main components included of this project. Some of these, while dev complete, are possibly subject to limitations or known issues that are highlighted in a separate section of this document.

1. **EVM Interoperability Service**: This service enables flows to execute asynchronous EVM transactions and calls, waiting for the result.

2. **Identity Module**: This module allows for the configuration of the EVM identity a flow will operate with. It also supports the implementation of custom identity modules leveraging Hardware Security Modules (HSMs) or other protocols to ensure the safety of the private key used for signing EVM transactions. The reference `UnsecureRemoteEvmIdentityFlow` allows you to specify the private key and RPC endpoint and is only meant as a reference for implementing safer options.

3. **Web3 Interfaces**: Current implementation supports direct interaction with ERC20 tokens, standard Web3 APIs like querying blocks and transactions, the EVM `SwapVault` contract which allows swapping ERC20, ERC721, ERC1155 assets against a Corda asset.

4. **Atomic Swap Flows**: Atomic swap flows for executing Corda-EVM DvP and PvP have been implemented. A sample project with tests demonstrates how an EVM asset is swapped with a Corda asset in a completely fair, risk-less, balanced way.

5. **Event Subscription Service**: there is no real, full support in this project for event subscription but rather a simplistic, incomplete interface for doing so. Triggering flows in response to EVM events can be done using existing projects like [Eventeum](https://github.com/eventeum/eventeum) or Web3 javascript frameworks like [Web3.js](https://web3js.org) or [Ethers.js](https://ethers.org)

6. **Patricia Merkle Trie Component**: Allows EVM events and transaction validation, production of events and transaction proofs, and verification of proofs.

7. **EVM / Solidity Project for Atomic Swap**: This project will implement atomic swap of EVM assets like ERC20, ERC721, ERC1155.

## Integration Tests

The integration tests in this project are currently hardcoded to a locally set up test EVM instance. Please refer to the EVM project's C for instructions in this regard before executing any Corda project test.
## Build and Run

### Prerequisites

This project requires and has been tested with the following tools:

- gradle - version v5.6.4
- node.js - version v16.19.0 (our recommended installer is NVM available [here](https://github.com/nvm-sh/nvm))
- npm - version v9.7.1 (comes with node.js)
- npx - version v9.7.1 (to install npx enter `npm install -g npx`)


### Building and Deploying the Project

#### Build

To build the Corda project, enter the following command from the root folder:

```  
./gradlew build -x test  
```  

or

```
./gradlew build 
```  

if you have manually prepared the EVM test environment.

#### Deploy

Plain and Dockerized deployment is under development

### Testing

To run the tests you need to set up the test environment first.

To set up the test environment proceed as follows:
- change directory to ../evm and open two terminals to that directory
- on the first terminal run `npm install` and wait for the required packages to be installed - this step is required once.
- again on the first terminal run `npx hardhat node` - it will print a number of accounts and will start printing block numbers in the form `Mined empty block range #m to #n`
- on the second terminal, once the first the hardhat node is running, enter `npx hardhat run deploy.js --network localhost` and wait for the shell prompt to return (without errors)

If you followed the steps above correctly, on the second terminal you will see the following output:

Gold Tethered (GLDT) Token deployed to: 0x5FbDB2315678afecb367f032d93F642f64180aa3</br>  
Silver Tethered (SLVT) Token deployed to: 0xc6e7DF5E7b4f2A278906862b61205850344D4e7d</br>


The test environment is now ready and you can enter the following command:

```  
./gradlew test
```
