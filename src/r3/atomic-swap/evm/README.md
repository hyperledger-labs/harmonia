# Corda-EVM interop

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.

## Development Status

The atomic swap reference code is currently under development but nearing completion. Some of its components are in the process of refinement and preparation for community access.

### Component Overview and Status

The following is a list of components included in this project. All of these are subject to future changes and are currently under development.

1. **EVM Commit and Transfer Contracts **: These contracts allows a party to commit an asset to the contract and recall it any time if the expected recipient did not claim the asset. The committer therefore, keeps the ownership of the asset until the claim.

2. **EVM Commit and Transfer Tests **: These contract are the test logic for the Commit and Transfer Contracts.

## Experimental Code

Please note that this project currently contains some experimental code.

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