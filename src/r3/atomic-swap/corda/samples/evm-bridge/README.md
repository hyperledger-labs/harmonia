<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# EVM Bridge

The EVM Bridge allows Corda and Ethereum networks interoperability.

## Build pre-requisites

Two of the build dependencies, currently require special configuration and steps that will be soon removed.

### Onixlabs packages

Requires to configure a GitHub personal access token with at least the `public_repo` and `read:packages` scopes.

If you don't have one, go to your GitHub `profile` -> `settings` -> `Developer settings` -> `personal access tokens` -> `generate new one`. Give it a name, select `public_repo` and `read:packages` scopes and you will receive the personal access token.

Then copy the CNO-Extension project root's github.properties.sample to github.properties, edit and replace the user and
the key with your ones.
```shell
gpr.user=<YOUR_GITHUB_USER_HERE>
gpr.key=<YOUR_GITHUB_TOKEN_KEY_HERE>
```

### R3 Corda finance framework (preview)

Clone the R3 Corda finance framework and publish locally.

To build this project a gradle.properties from the gradle home directory (~/.gradle/gradle.properties) needs to contain the github user/key pair similarly to the Onixlabs packages.

File: ~/.gradle/gradle.properties
```shell
gpr.user=<YOUR_GITHUB_USER_HERE>
gpr.key=<YOUR_GITHUB_TOKEN_KEY_HERE>
```

Either (for Corda 4.8 and min platform version 10)
```shell
git clone git@github.com:corda/r3-corda-finance-framework.git
git checkout iee/simplified-corda-4.8.5
./gradlew releaseLocal
```
or (for Corda 4.9 and min platform version 11)
```
git clone https://github.com/corda/r3-corda-finance-framework.git
git checkout simplified
./gradlew releaseLocal
```

**NOTE👋**: The current Evm Bridge is based on Corda 4.8, therefore minimum platform version 10.

## Build and Deploy

### Build

To build the samples without running the tests:

```
./gradlew samples:evm-bridge:bridge-workflows:build -x test
```

To build with tests use the following instead, but you'll need to setup a hardhat test network first and deploy some test tokens as shown in "[Setting up a local test network](#setting-up-a-local-test-network)".

````
./gradlew samples:evm-bridge:bridge-workflows:build
````

### Deploy

To deploy the samples with the Corda nodes without running the tests:

```
./gradlew :deploy:prepareDockerNodes -x test
```

To deploy the samples with the Corda nodes with tests use the following instead, but you'll need to setup a hardhat test network first and deploy some test tokens as shown in "[Setting up a local test network](#setting-up-a-local-test-network)".

````
./gradlew :deploy:prepareDockerNodes
````

At the end of the deployment, 4 nodes will be deployed under the following folders:
* `samples/evm-bridge/build/nodes/Alice`
* `samples/evm-bridge/build/nodes/Bob`
* `samples/evm-bridge/build/nodes/Bridge`
* `samples/evm-bridge/build/nodes/Notary`

## Setting up a local test network

For a simple way to set up a local Ethereum test network, follow these intstructions.

Make sure NodeJS v16.18.1 or greater is installed (best to use Node Version Manager tool).

If you don't have npx installed, form the terminal enter:
```
npm install -g npx
```

Open a terminal, navigate to the samples/evm-bridge/testnet folder and enter the following command:

```
npm install hardhat @nomiclabs/hardhat-waffle @openzeppelin/contracts
```

Once the hardhat node started it will show a mining prompt, and you can now open a new terminal and enter the following command:

```
npx hardhat run deploy.js --network localhost
```

If all steps were applied correctly, the deployment will produce the following output:

Gold Tethered (GLDT) Token deployed to: `0x5FbDB2315678afecb367f032d93F642f64180aa3`</br>
Silver Tethered (SLVT) Token deployed to: 0xc6e7DF5E7b4f2A278906862b61205850344D4e7d</br>
Bronze Tethered (BRZT) Token deployed to: 0x70e0bA845a1A0F2DA3359C97E0285013525FFC49</br>
Platinum Tethered (PLAT) Token deployed to: 0xcbEAF3BDe82155F56486Fb5a1072cb8baAf547cc</br>
Rhodium Tethered (RHDT) Token deployed to: 0x172076E0166D1F9Cc711C77Adf8488051744980C</br>

Bridge deployed to: `0xe8D2A1E88c91DCd5433208d4152Cc4F399a7e91d`

As a double check, verify that the Gold token is deployed to `0x5FbDB2315678afecb367f032d93F642f64180aa3` and that the Bridge is deployed to `0xe8D2A1E88c91DCd5433208d4152Cc4F399a7e91d` as tests depends on these addresses.