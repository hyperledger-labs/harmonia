IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED
FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION, AND ANY UPDATES ARE PROVIDED "AS IS"
WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.

# Crosschain Interoperability

The PoC implemented here illustrates how a trade can be settled across two different DLT networks using the crosschain message protocol described in the [EEA DLT interoperability specification](https://entethalliance.github.io/crosschain-interoperability/draft_dlt-interop_techspec.html).

Payment versus Payment (PvP) crosschain interoperability involves payments taking place across two Ethereum networks.

Delivery versus Payment (DvP) crosschain interoperability involves transferring securities on a Corda network while the corresponding payment for the securities takes place on an Ethereum network.

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
