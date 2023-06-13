# Financial Markets Interoperability Vision and Requirements

## Introduction to Harmonia

Banks need more efficient and cost-effective ways to store, manage, and transfer financial assets. For the past seven years, wholesale banks and other financial institutions have explored the potential of new technologies to bring about material change to capital markets. This work has spanned the full capital markets lifecycle - from tokenization, issuance and secondary market trading, through to post-trade and settlement.

However, most projects have been done in isolation, segmented to discrete networks and pieces of the full lifecycle. This has so far limited their impact and increased the risk of creating fragmented pools of liquidity.

Regulated networks will achieve greatest success when they can interconnect to form a network of regulated networks, each supporting different business needs but linked together as required to support complex value chains. 

Initiatives such as Hyperledger Cacti and YUI, a Hyperledger Lab, as well as the work of the Enterprise Ethereum Alliance and others have led the way in driving open standards and open-source general-purpose frameworks for interoperability.

And, in parallel, firms focused more tightly on solving interoperability challenges in the specific domain of the emerging regulated financial networks have been working on bilateral implementations, in many cases in tight collaboration with lawyers and domain specialists. This work has been mostly, although not exclusively, confidential.

As a result, those working on these initial regulated interoperability scenarios have not been able to fully exploit the work of the projects listed above, and those projects have not been privy to some of the unique requirements that have emerged, and design principles that they result in.

The purpose of [Harmonia](https://github.com/hyperledger-labs/hyperledger-labs.github.io/blob/main/labs/harmonia.md) is to bridge this gap, and facilitate a harmonisation of approaches to increase clarity and commonality for all participants.

In what follows, we outline our vision of a “Connected Digital Capital Markets Ecosystem” and elaborate on the requirements and high-level constraints that apply when regulated blockchain networks need to interoperate. We conclude by signposting the reader to other documents in this repository, which elaborate on these themes.

## The Connected Digital Capital Markets Ecosystem

We envision a world where the emerging networks supporting digital cash and digital assets interoperate to facilitate the creation of a single global digital liquidity pool. Whether in the form of natively issued assets or new digital wrappers for existing inventory, these will provide a foundation that enables unprecedented capital markets efficiency.  

This vision builds on the following principles:

### Accessible Foundations

The key building blocks for this ecosystem - digital cash and digital assets - will form a single global digital liquidity pool. Whether in the form of natively issued assets or new digital wrappers for existing inventory, these all act as the foundation for enabling capital markets collateral efficiencies.

They need to fulfil the promise that they can be used in many business platforms across many lifecycle stages from primary issuance or creation of the digital wrappers through each of the capital market lifecycle processes they are part of.

One of the base elements will be some degree of common methods across tokens / coins to enable interoperability, and perhaps in the future, mobility.

### Interoperable by Design

The foundational digital liquidity layer and digital business platform layer must be interoperable “out of the box”. The various assets in the liquidity pool need to be interoperable to enable collateral vs. collateral, collateral vs. cash, cash vs. cash swaps – the primary transaction types used in Capital Markets.

This interoperability model enables ecosystem actors to each focus on their core proposition but still take part in the wider value chain. For this to happen there needs to be legal and technical interoperability by design.

This should also be true where there are multiple technical platforms supporting the foundation assets and providing the lifecycle services. Different networks are using different DLT platforms to support their specific needs. Hence, cross platform interoperability is key.

### Standards adoption

There needs to be a standard and cost-effective mechanism for business platforms and foundation services to work together. Banks and other participants expect common standards for the processes they need to interact with, and ultimately the standardisation of components they need to install.

Often, only the technical element is focused on, but standards enabling the interoperability of business flows between the emerging wholesale digital ecosystem will need both legal (scheme/rule book) and technical standards to drive adoption. Legal and regulatory requirements can have a profound impact on the patterns adopted for different use cases.

Technical standards span at least three layers in addition to the application layer controlling the orchestration of the swap and providing any unique business logic required by the business scenario:

* The orchestration should use standardised patterns that can be used to safely and securely execute the swap and allow recovery in the event of any failure
* A standardised 'function call' layer that provide the building blocks required to support the different patterns e.g. ‘lock asset’, ‘provide proof to release asset’, etc., for each platform
* The underlying communications layer that enables two or more platforms to send commands and information between them to carry out the swap

## Example of financial-markets-driven requirements: Atomic Delivery-Versus-Payment

The ability to transfer an asset to a new owner is exchange for payment (an atomic exchange – Delivery versus Payment) is probably the most common financial transaction. This is just as true in Capital Markets as in other areas of business. Hence, DvP is the driving requirement for financial markets

The business, legal and regulatory frameworks in place for financial markets place specific constraints on process to carry out a swap. These are outlined in shared rule books which are based on Principles for Financial Market Infrastructures (PFMIs).

### Specific requirements for DvP (settlement) in the financial markets include:

* Proof constructs must be based on rule book/legal requirements.
* There need to be clear boundaries for liability at all stages of the business transaction
* There must be settlement finality. Probabilistic finality is not acceptable
* It must be possible to clearly demonstrate that the technical solution aligns with with business considerations, including but not limited to behaving predictably and in accordance with prevailing conventions in the case of bankrupty by one or other party.
* Approaches commonly used in public blockchains (hash timelocks – HTLCs – in particular) are not necessarily well suited to financial transactions as the time lock can lock up assets for extended periods and are prone to race conditions across the two networks at the timeout. Therefore, we prefer plain hash locks to hash timelocks.
* Ownership of an asset must be legally unambiguous at all times in order to ensure events such as interest payments can be correctly processed, making solutions dependent on technical ‘escrow’ functionality or teminology inappropriate 
* No (new) intermediate networks or third-party bridges, unless owned and controlled by the participants of the two networks or provided by a trusted party who already facilitates activity in the relevant market. The protocol should not mandate the usage/introduction of a new party solely for the purpose of the swap. There is a preference to solve the problem without forcing a change to the market structure


### Requirements associated with specific technology choices

In the context of our initial work, the networks we have been working with are Corda-based and EVM-based. This leads to some additional potential requirements:

* Approaches should be standards-based (including having a default towards adopting existing standards such as  the EEA Interop standard, with some modifications if necessary, for the cross-chain communication layer)
* The end-to-end interoperability protocol can be initiated and co-ordinated via either network, or it should be explicitly documented when this is not the case. In particular, it should be noted that Corda and EVM-based platforms make different promises re data visibility, etc., which means some approaches only work if a specific platform is in the ‘driving seat’
* Aim to minimise or remove coupling between networks where possible: determining if a transaction or block has been confirmed on another network can sometimes be straightforward, but determining validity requires a runtime with full knowledge of other network's rules and ability to execute them

#### Corda-specific observations
  * Corda privacy ensures transactions are only shared with those parties that ‘need to know’: so protocols that assume 'all' nodes can or will learn about an event on the network may not be valid
  * Corda serialisation and signature schemes may be expensive for EVMs to work with (gas costs)

#### EVM-specific observations
  * In an Ethereum permissioned network, establishing/maintaining the identity of the Validators is easier than establishing the identity of every wallet holder on the network
  * Having a well defined set of validators means that in a permissioned enterprise Ethereum network using a PoA consensus (IBFT/QBFT), a block-header based proof is more scalable and easier to verify that a transaction based proof

## How to navigate this repository

In the spirit of the Hyperledger Labs philosophy, the files initially contributed to this repository are, in many cases, at an early stage of elaboration. This reflects ongoing work by R3 and Adhara to make as many relevant internal documents as possible available to the community. As such, the structure and content of this repo will rapidly evolve in the coming weeks.

* A working paper elaborating on the themes introduced in this paper, and introducing a conceptual framework, is available [here](r3/contents.md).
* A summary of high-level interoperability business principles, endorsed by a working group of industry practitioners, is [here](adhara/interop_principles.md).
* A detailed discussion of the specific cross-chain proofs we have found to be important in Corda/EVM interactions is available [here](adhara/cross_chain_proofs.md).
* An example of an end-to-end DvP protocol between a Corda and private EVM network is available [here](r3/example_design_for_atomic_swap.md).



