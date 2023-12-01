# Corda-EVM Interoperability

## Introduction
This project is an experimental reference implementation of Corda-EVM interoperability. It is not intended for production use and may have limitations and bugs. Please use this code for reference and experimentation only.

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.

## Development Status

This project is an experimental reference implementation and is considered complete for its intended purpose. While it may undergo minor updates to address any critical issues that may arise, no major changes are expected in the near future.

### Component Overview

The following is a list of components included in this project. All of these are subject to future changes and are currently under development.

1. **EVM Commit and Claim Contracts**: These contracts allows a party to commit an asset to the contract and recall it any time if the expected recipient did not claim the asset. The committer therefore, keeps the ownership of the asset until the claim.

2. **EVM Commit and Transfer Tests**: These contract are the test logic for the Commit and Transfer Contracts.

## Build and Run

### Prerequisites

This project requires and has been tested with the following tools:

- Foundry - https://github.com/foundry-rs/foundry

### Building and Deploying the Project

This section assumes Foundry is installed.

#### Build

To build the project, enter the following command from the root folder:
```  
forge install && forge build  
```  

### Testing

```  
forge test -vvv  
```

## Integration Tests / Test Network Setup

To run the Corda integration tests you need to set up the EVM test environment first.

To set up the test environment proceed as follows:
- open two terminals in the root directory of the EVM project
- on the first terminal run `forge install && npm install` and wait for the required packages to be installed - this step is required once.
- again on the first terminal run `npx hardhat node` - it will print a number of default accounts
- on the second terminal, once the first the hardhat node is running, enter `npx hardhat run deploy.js --network localhost` and wait for the shell prompt to return (without errors)

If you followed the steps above correctly, on the second terminal you will see the following output:

Gold Tethered (GLDT) Token deployed to: 0x5FbDB2315678afecb367f032d93F642f64180aa3</br>    
Silver Tethered (SLVT) Token deployed to: 0xc6e7DF5E7b4f2A278906862b61205850344D4e7d</br>  
SwapVault deployed to: 0x70e0bA845a1A0F2DA3359C97E0285013525FFC49</br>

The run the integration tests, please refer to the Corda project's [README.md](../corda/README.md).
