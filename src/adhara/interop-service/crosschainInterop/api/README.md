IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED
FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION, AND ANY UPDATES ARE PROVIDED "AS IS"
WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.

# Crosschain interop service

## Introduction

The Crosschain interop service provides API endpoints to interact with the crosschain interop application SDK.

## Problem Description

The crosschain interop SDKs are decentralised and trustless for the most part, however certain onboarding and setup
steps need to be performed.
Some of these steps are, but not limited to, onboarding proving schemes, onboarding event decoding schemes and adding
foreign to local accountId mappings.

In addition to the functioning of the crosschain interop SDK, there is also a need for parties to be able to submit
settlement instructions, which is not trustless or decentralised.

## Methodology

### Start the API

1. The API needs nodejs (tested with v18.12.1) installed
2. Run `npm i` to install needed packages
3. The API needs contract build artifacts from the root crosschainInterop folder:

```bash
cd crosschainInterop
npx truffle compile
```

The api-v1 can be started by:

```bash
cd crosschainInterop/api
node app.js
```

The admin-v2 can be started by:

```bash
cd crosschainInterop/api/admin-v2
node index.js
```
