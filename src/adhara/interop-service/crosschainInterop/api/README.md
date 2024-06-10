IT IS UNDERSTOOD THAT THE PROOF OF CONCEPT SOFTWARE, DOCUMENTATION, AND ANY UPDATES MAY CONTAIN ERRORS AND ARE PROVIDED
FOR LIMITED EVALUATION ONLY. THE PROOF OF CONCEPT SOFTWARE, THE DOCUMENTATION, AND ANY UPDATES ARE PROVIDED "AS IS"
WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.

# Crosschain interop service

## Introduction

The crosschain interop service provides API endpoints to interact with the crosschain interop application SDK.

## Problem Description

The crosschain interop SDKs are decentralised and trustless for the most part, however certain onboarding and setup steps need to be performed.

Some of these steps are, but not limited to, onboarding proving schemes, onboarding event decoding schemes and adding remote to local account identity mappings.

In addition to the functioning of the crosschain interop SDK, there is also a need for parties to be able to submit settlement instructions, which is not trustless or decentralised.

## Methodology

### Setup

Refer to either [Payment versus Payment (PvP)](pvp/README.md) or [Delivery versus Payment (DvP)](dvp/README.md) for
specific instructions.

### Start the API

1. The API needs contract build artifacts from the `crosschainInterop` folder:

```bash
make setup && make compile
```

2. The application API can be started with:

```bash
npm run start-app
```

3. The admin API can be started with:

```bash
npm run start-admin
```
