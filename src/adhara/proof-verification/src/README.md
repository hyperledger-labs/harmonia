# Introduction

This folder provides some sample code snippets for verifying a block header proof. The first step is to verify the validator signatures on the block header and then verify the EVM event is part of the merkle particia tree routed at the transaction receipt root in the block header.

## Getting started

To run the sample, first check that NodeJS is installed:
```
$ node --version
v16.15.1
```

Next, perform an npm install:
```
npm install
```

Now the tests can be run:
```
npm test
```
