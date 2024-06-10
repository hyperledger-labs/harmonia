IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED
FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION, AND ANY UPDATES ARE PROVIDED "AS IS"
WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.

# Crosschain Interoperability Project

## Introduction

The test environment provided here is focussed on testing minimum interoperability between Ethereum and Corda ledgers.

## Overview of contents

 - `besu` - Docker compose files to start two Besu nodes, note that there is no transaction signer
 - `corda-decoder` - Service that aids in building the corda proofs
 - `cordapp` - Sample corda application for use in "Delivery vs Payment" (DVP) trades
 - `crosschainInterop` - Crosschain interop service that contains a Solidity smart contracts, an API and some tests

## Test environment

The following needs to be installed before making attempt to provision a test environment:
 - Docker
 - Java
 - Gradle
 - Maven
  
### Ethereum services

The enclosed docker-compose files start up two Besu networks on port 7545 and 8545.

It can be started with the following command.

```shell
cd besu/services
./start.sh
```

Wait a few moments for the Besu nodes to produce ~30 blocks before running the tests.

The integration tests in this repo need a signer to run. An RPC server implementing the eth_signTransaction endpoint as documented here https://ethereum.org/en/developers/docs/apis/json-rpc/#eth_signtransaction. An open source Hyperledger signer to use with Besu services will be available soon.

### Corda services

Start three Corda nodes.
```shell
cd cordapp
export JAVA_HOME=/path/to/java/8
./gradlew deployNodes
./build/nodes/runnodes
```
In separate consoles, run
```shell
./gradlew runPartyAServer
```
and
```shell
./gradlew runPartyBServer
```
Retrieve the public keys of the notary and party A.
```shell
curl --location --request GET 'http://localhost:50005/keys' --header 'Content-Type: application/json'
{
  "notary" : "0x810a5b083e6630666f3c60f6a2af9ac28d16bbe33ec74f251131a44f64ff99c9",
  "name" : "O=PartyA, L=London, C=GB",
  "me" : "0x57869bb096486f4507bc922bb3d94358d595e592a386653c73706e5c1cdbccff"
}
```
Retrieve the public keys of the notary and party B.
```shell
curl --location --request GET 'http://localhost:50006/keys' --header 'Content-Type: application/json'
{
  "notary" : "0x810a5b083e6630666f3c60f6a2af9ac28d16bbe33ec74f251131a44f64ff99c9",
  "name" : "O=PartyB, L=New York, C=US",
  "me" : "0x26e72882f035ce3dcc6f9a33f89329e9c257a55440cfcd180f1dac52a4408157"
} 
```
These keys need to be onboarded onto every Ethereum ledger that a Corda transaction based proof (including these parties)
is submitted to.

### Corda decoder service

Start the Spring decoder application through the console.
```shell
cd corda-decoder
mvn spring-boot:run -Dlogging.level.org.springframework=INFO -Dlogging.level.org.springframework.data=INFO -Dlogging.level.root=INFO
```

