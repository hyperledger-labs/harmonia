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

## Known Issues and Limitations

### Memory Storage / Persistence
Some services required by the application use memory storage for simplicity rather than persistent storage. Persistent storage is necessary to retain references to transactions and associated data (e.g., signatures, identity) between distinct flows. For non-experimental use, persistent storage is required.

### Nonce
The EVM transaction model uses the account nonce to maintain the order and uniqueness of transactions originating from a specific Ethereum address. The nonce represents the number of transactions sent from that address and ensures that transactions are executed in the correct sequence and not replayed. While the EVM interface available from the flows handles the asynchronous model of EVM transactions and calls, it lacks a recovery function should any issue arise with the nonce that is not already handled by the underlying Web3j framework.

### Checkpointing
While some further testing and investigation are required, Corda flows' checkpointing may, in some cases, cause an EVM transaction to be repeated. For non-experimental use, proper deduplication of the EVM interface calls should be ensured.

### EVM Events
The EVM interface, implemented as a Corda service, has a very simplistic and incomplete event registration mechanism. It is not intended for use outside of highly experimental cases, and we recommend that events be registered, filtered, and handled outside of a Corda node functioning as a coordination mechanism used to trigger related flows on Corda nodes.

### Tests and Test Network
Integration tests rely on a local Hardhat node instance running and initialized with a deployment script that deploys the required contracts. This implies that accounts, keys, and addresses used by the integration tests are hardcoded in a base class used by all the tests.

#### Manual Test Network Setup
Please refer to the EVM project's README.md for instructions in this regard before executing any Corda project test.
#### Docker Test Network Setup
The Corda project has Gradle tasks meant to start a Docker container with the test network ready for executing the integration tests included in the projects.

You can either start the docker instance by explicitly executing
```
./gradlew startDockerContainer
```
or passing the useDockerTestnet as a property to your gradle build or test commands, as
```
./gradlew build -PuseDockerTestnet
```
or
```
./gradlew test -PuseDockerTestnet
```

You must have a recent Docker version installed on your machine.
## Build and Run

### Prerequisites

This project requires and has been tested with the following tools:

- gradle - version v5.6.4
- if planning to setup the test network manually
    - node.js - version v16.19.0 (our recommended installer is NVM available [here](https://github.com/nvm-sh/nvm))
    - npm - version v9.7.1 (comes with node.js)
    - npx - version v9.7.1 (to install npx enter `npm install -g npx`)
- if planning to use the docker container to run the dockerized test network
    - Docker Desktop (tested with version 4.25.2)

### Building the Project

#### Build

To build the Corda project, enter the following command from the root folder:

```
./gradlew build -x test
```
or
```  
./gradlew build 
```    
if you have manually prepared the EVM test environment, or
```  
./gradlew build -PuseDockerTestnet
```
if you want to use Docker and have Docker installed on your machine.

### Testing

#### Manual Test Network Setup

To run the tests you need to set up the test environment first, refer to the [Integration Tests / Test Network Setup Section in the EVM Project's README.md](../evm/README.md#integration-tests--test-network-setup). Once you have setup the network manually, from the Corda project root enter:

```
./gradlew test
```

#### Automated Test Network Setup

Start the tests using the Gradle property useDockerTestnet:
```
./gradlew test -PuseDockerTestnet
```

Optionally start the network using Gradle and then run the tests:
```
./gradlew startDockerContainer
./gradlew test
```
