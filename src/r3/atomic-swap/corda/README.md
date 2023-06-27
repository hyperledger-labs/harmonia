# Corda-EVM interop

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.

## Development Status

The atomic swap reference code is currently under development but nearing completion. Some of its components are in the process of refinement and preparation for community access.

### Component Overview and Status

The following is a list of components included in this project. All of these are subject to future changes and are currently under development.

1. **EVM Interoperability Service**: This service enables flows to execute asynchronous EVM transactions and calls, waiting for the result. It is fully implemented and tested. 

2. **Identity Module**: This module allows for the configuration of the EVM identity a flow will operate with. It also supports the implementation of custom identity modules leveraging Hardware Security Modules (HSMs) or other protocols to ensure the safety of the private key used for signing EVM transactions. The basic module is fully implemented and tested, and other modules are being implemented.

3. **Web3 Interfaces**: Current implementation supports interaction with ERC20 tokens and standard Web3 APIs like querying blocks and transactions. These features are fully implemented and tested. Support for ERC721 and ERC1155 tokens is in the pipeline and will be added soon.

4. **Atomic Swap Flows**: Basic atomic swap flows for executing Corda-EVM DvP and PvP scenarios are under active development.

5. **Event Subscription Service**: This component is responsible for subscribing to and handling events. It is currently under active development and nearing completion. Some features are experimental and will be moved to a separate external module in future iterations.

6. **Patricia Merkle Trie Component**: This upcoming component will support EVM events and transaction validation, production of events and transaction proofs, and verification of proofs.

#### Coming Soon

7. **Full Atomic Swap Flows**: Full atomic swap flows for executing Corda-EVM DvP and PvP scenarios are in the pipeline and will be implemented in the near future.

8. **EVM / Solidity Project for Atomic Swap**: This project will implement atomic swap of EVM assets like ERC20, ERC721, ERC1155, with live upgradeability to extend with other assets. It is in finalization phase and will be released soon.

## Experimental Code

Please note that this project currently contains some experimental code, particularly related to EVM events subscription and handling. This code is intended to be moved to an external module in the future.

## Integration Tests

The integration tests in this project are currently hardcoded to a locally set up test EVM instance. Development is in progress to enable these tests to execute in a fully automated manner, independent of manually set up testing environment.

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
- change directory to samples/testnet and open two terminals to that directory
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
