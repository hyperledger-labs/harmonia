IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED
FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION, AND ANY UPDATES ARE PROVIDED "AS IS"
WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.

# Crosschain Interoperability

## Introduction

The crosschainInterop project provides an implementation for performing operations that span more than one blockchain, for example performing a "Delivery vs Payment" (DVP) trade that spans Corda and Ethereum.

## Overview

The project has several important folders/files:
  - `api` - An API that provides a mechanism for interacting with the interop service, e.g. submitting a settlement instruction
  - `config` - Configuration for the interop service, e.g. configuration per system/blockchain
  - `contracts` - The solidity contracts for performing DVP
  - `src` - The source files of the interop service, used by the `api` and tests
  - `test` - Unit tests
  - `test-integration` - Integration tests

## Getting started

The steps listed here require [Node.js](https://nodejs.org/en) and [Truffle](https://trufflesuite.com/) to be installed locally.

### Step 1 

Provision a test environment as indicated [here](../README.md).

### Step 2

Manually copy the Corda participant keys, as obtained in the previous step, to the test configuration in `web3Setup.js`. 
 
### Step 3

Install needed packages.
```shell 
npm i
```
### Step 4

Compile the Solidity code, to generate contract build artifacts, needed by the project.
```shell
npx truffle compile
```
### Step 5

Run the unit tests.
```shell
npx truffle test --network besu_harmonia
```
### Step 6

Configure the test environment for integration tests.

The `web3Setup.js` script will deploy four contracts to each Besu network and configure them for use in interop tests, with
one another, and one other Corda Network, as per network configuration. The following contracts will be deployed on both 
Ethereum networks:

- Cross-chain function call contract
- Cross-chain messaging contract
- Cross-chain xvp contract
- Simplified asset token contract

Make sure the configuration in your script correctly updated with the public keys of parties on your Corda network before 
proceeding. They will change everytime you restart your Corda network.

```shell
node web3Setup
```

### Step 7 

Run the integration tests.
```shell
npm test 
```
