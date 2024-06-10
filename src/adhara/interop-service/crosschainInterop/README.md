IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED
FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION, AND ANY UPDATES ARE PROVIDED "AS IS"
WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.

# Crosschain Interoperability

The PoC implemented here illustrates how a trade can be settled across two different DLT networks using the crosschain message protocol described in the [EEA DTL interoperability specification](https://entethalliance.github.io/crosschain-interoperability/draft_dlt-interop_techspec.html). 

Payment versus Payment (PvP) crosschain interoperability involves payments taking place across two Ethereum networks.

Delivery versus Payment (DvP) crosschain interoperability involves transferring securities on a Corda network while the corresponding payment for the securities takes place on an Ethereum network.

## Introduction

The crosschain application layer XvP (PvP/DvP) implementation is based on a leader-follower approach in which one network is the leading network and the other is the follower network. Atomic settlement, in the financial sense, is achieved by ensuring that either both legs complete successfully or none does, in which case the trade is cancelled. 

### PvP

The crosschain PvP cash settlement flows, used in this PoC, make use of earmarks, where cash are placed on hold with the intent to transfer it to the entity it was earmarked for after receiving sufficient proof that a previous step in the PvP flow was executed.

The PvP settlement use case flow for a successful trade:

1. Party A and Party B agree to settle a PvP trade via off-chain orchestration.
2. Party B places the follow leg cash on hold for Party A, marking the XvP contract as notary on the follower Ethereum network.
3. Party A places the lead leg cash on hold for Party B, marking the XvP contract as notary on the leading Ethereum network.
4. Party A starts the lead leg on the leading Ethereum network, which will emit an event with a crosschain function call instruction to request the follow leg if, and only if, the lead leg hold is in place.
5. Party A constructs an Ethereum event attestation proof, using it as proof that the lead leg hold was placed on the leading Ethereum network.
6. Party A executes a crosschain function call on the follower Ethereum network to request the follow leg which, after successful verification of the proof, emits an event with a crosschain function call instruction to complete the lead leg if, and only if, the follow leg hold was executed. Party A receives the cash on the follower Ethereum network.
7. Party B constructs an Ethereum event attestation proof, using it as proof that the follow leg hold was executed on the follower Ethereum network.
8. Party B executes the crosschain function call on the leading Ethereum network to complete the lead leg which, after successful verification of the proof, executes the lead leg hold. Party B receives the cash on the leading Ethereum network.

### DvP

The crosschain DvP repo trade settlement flows, used in this PoC, make use of earmarks, where assets are placed on hold with the intent to transfer them to the entity it was earmarked for after receiving sufficient proof that a previous step in the DvP flow was executed.

The DvP settlement use case flow for a successful trade:

1. Party A and Party B agree to settle a DvP trade via off-chain orchestration.
2. Party B places the payment leg cash on hold for Party A, marking the XvP contract as notary on the Ethereum network.
3. Party A places the delivery leg securities on hold on the Corda network.
4. Party A constructs a Corda transaction attestation proof, using it as proof that the delivery leg hold was placed on the Corda network.
5. Party A executes a crosschain function call on the Ethereum network to request the payment leg which, after successful verification of the proof, emits an event with a crosschain function call instruction to complete the delivery leg if, and only if, the payment leg hold was executed. Party A receives the cash on the Ethereum network.
6. Party B constructs an Ethereum event attestation proof, using it as proof of the payment leg hold execution on the Ethereum network.
7. Party B executes the crosschain function call on the Corda network to complete the delivery leg which, after successful verification of the proof, executes the delivery leg hold. Party B receives the securities on the Corda network. 

### Cancellations

The crosschain cancellation flows, used in this PoC, aims at catering for the edge case where the hold for the lead (resp. delivery) leg, or follower (resp. cash) leg, can not be placed, or corrected, and in order to release the hold, the trade must be cancelled. More specifically, the hold for a trade can only be cancelled on a specific network, after the trade was cancelled on that network. And a trade can only be cancelled on a network, if the hold for that trade is not already placed on that network. This means that a trade, where the hold is already in place, can only be cancelled, through the crosschain interop protocol, by cancelling the trade on the other network.  

The XvP contract exposes the functionality to start a trade cancellation and perform a trade cancellation. The trade cancellation can only be started if it is the first step taken on a network, before creation of the hold. The trade cancellation can only be performed if it can be proven that the cancellation was previously started on either of the networks.

The PvP settlement use case flow for cancellation of a trade is the same whether it is started on the leading network or the following network:

1. Party A and Party B agree to settle a PvP trade via off-chain orchestration.
2. Party B places the follow leg cash on hold for Party A, marking the XvP contract as notary on the follower Ethereum network.
3. Party B starts trade cancellation on the leading Ethereum network which will emit an event with a crosschain function call instruction to complete the cancellation if, and only if, the trade was cancelled on the leading Ethereum network.
4. Party B constructs an Ethereum event attestation proof, using it as proof that the trade cancellation was started on the leading Ethereum network.
5. Party B executes the crosschain function call on the follower Ethereum network to perform the trade cancellation which, after successful verification of the proof, cancels the trade and follow leg hold on the follower Ethereum network.

The DvP settlement use case flow for starting cancellation of a trade on the leading Corda network:

1. Party A and Party B agree to settle a DvP trade via off-chain orchestration.
2. Party B places the payment leg cash on hold for Party A, marking the XvP contract as notary on the Ethereum network.
3. Party B starts trade cancellation on the Corda network which will cancels the trade. 
4. Party B constructs a Corda transaction attestation proof, using it as proof that the trade cancellation was started on the Corda network.
5. Party B executes a crosschain function call on the Ethereum network to perform the trade cancellation which, after successful verification of the proof, cancels the trade and payment leg hold on the Ethereum network.

The DvP settlement use case flow for starting cancellation of a trade on the following Ethereum network:

1. Party A and Party B agree to settle a DvP trade via off-chain orchestration.
2. Party A places the delivery leg securities on hold on the Corda network.
3. Party A starts trade cancellation on the Ethereum network which will emit an event with a crosschain function call instruction to complete the cancellation if, and only if, the trade was cancelled on the Ethereum network.
4. Party A constructs an Ethereum event attestation proof, using it as proof that the trade cancellation was started on the Ethereum network.
5. Party A executes a crosschain function call on the Corda network to perform the trade cancellation which, after successful verification of the proof, cancels the trade and delivery leg hold on the Corda network. 

## Setup

Coordination between steps in the above flows is handled by the crosschain interop service. This service is capable of receiving settlement instructions and, by implementing the crosschain interoperability protocol, is capable of automating a full end-to-end settlement flow across two different DTL networks as described above. 

The crosschain interop service provides API endpoints to interact with the crosschain interop SDKs which are decentralised and trustless, besides for onboarding steps. The service which allows parties to submit settlement instructions is however not trustless or decentralised.

## Evaluation

Instructions to locally start up required Corda and Ethereum services are outlined here.

### 1. Requirements

- Install Java 8 (Corda nodes)
- Install Java 11 (Corda decoder)

### 2. Setup

#### Corda Nodes

Create the Notary, PartyA and PartyB Corda nodes locally, and deploy the Corda app to the nodes by first setting the
`JAVA_HOME` environment variable to a Java 8 SDK.

From this repository, inside `corda-experiments/cordapp`, run:

```shell
./gradlew deployNodes
```

Once the build has completed with `BUILD SUCCESSFUL`, a `/build` folder will be created inside the `cordapp` folder.
It contains a `node` folder, which contains folders for each of the nodes and a `runnodes` shell script used to run the
nodes with the Corda app simultaneously.

From the `adhara-labs` repository, inside `corda-experiments/cordapp`, run:

```shell
./build/nodes/runnodes
```

Three separate windows should open to indicate the running nodes. Next run a Spring Boot server to interact with each of
the nodes.

In a new terminal, from this repository, inside `corda-experiments/cordapp`, run:

```shell
./gradlew runPartyAServer
```

In a new terminal, from this repository, inside `corda-experiments/cordapp`, run:

```shell
./gradlew runPartyBServer
```

Use the following curl commands to get the public keys for Party A, Party B and the Notary they will be used on the Corda network:

```shell
curl --location --request GET 'http://localhost:50005/keys' --header 'Content-Type: application/json'
curl --location --request GET 'http://localhost:50006/keys' --header 'Content-Type: application/json'
```

Copy the keys to `crosschainInterop/web3Setup.js` or as is needed by your application.

    remoteNetworks: [{
      cordaPartyAKey: '0xc8f98b0ef41b3fd57406a5f60fda4bd95ce85fc1992ba5ad003d9ff467431d3f',
      cordaPartyBKey: '0xaf7584abe93f9c6c6ecb9fd6c7c39e84d76d2ff5ac6bba47976b0fb18fcd4161',
      cordaNotaryKey: '0x6fa65c5977ef512cac1d4468b8f5337d7a2f3fa0434daf657cedb377ca512bc0',
    }]

Corda parties will use the same keys when settling on both Ethereum ledgers. Note that the public key configuration
can also be updated through the interop API after installation.

#### Corda Decoder

To run a Corda proof generator service, set the `JAVA_HOME` environment variable to the Java 11 SDK.

From this repository, inside `corda-experiments/corda-decoder`, run:

```shell
mvn spring-boot:run -Dlogging.level.org.springframework=INFO -Dlogging.level.org.springframework.data=INFO -Dlogging.level.root=INFO
```

#### Harmonia sandbox

Use the following scripts to provision local services, from this repository, inside `harmonia/services`.

To start the environment, run:
```shell
./start.sh
```

To stop the environment, run:

```shell
./stop.sh
```

#### Interop Services

If the interop contracts have changed since the last compile, use hardhat to compile the source files, which updates
the `./build` folder.

From this repository, inside `crosschainInterop`, run:

```shell
make setup && make compile
```

From this repository, inside `crosschainInterop/api`, start the application API service by running:

```shell
npm run start-app
```

From this repository, inside `crosschainInterop/api`, start the admin API service by running:

```shell
npm run start-admin
```

### 3. Running Test Scripts

Set up the environment by executing the following commands:

```shell
make setup && make compile
```

#### 3.1. Unit tests

From this repository, inside the `crosschainInterop` project, run the unit tests by executing:
```shell
make test
```

#### 3.2. Integration tests

From this repository, inside `crosschainInterop`, initialize the sandbox by running:

```shell
node web3Setup.js
```

It will deploy interop contracts on the configured networks and do the necessary on-boarding calls. Note that this only needs to be run once.

From this repository, inside `crosschainInterop`, run the integration tests by executing:

```shell
npm test
```
