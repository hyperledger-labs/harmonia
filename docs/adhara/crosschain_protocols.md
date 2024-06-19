# Crosschain Protocols 

## Introduction 

Crosschain interoperability has a number of tradeoffs. The two key tradeoffs involved are:
 - How much trust is vested in an intermediary notifying one network that something happened on another network.
 - How much does one network need to know about the mechanisms on another network in order to verify that something happened on that network.

If we go too far to the left and trust an intermediary explicitly, we open the door for a single point of failure where a bad actor can fraudulently declare truth on one network that doesn't exist on another.

If we go too far to the right, we risk interlinking two systems, resulting in an interoperability protocol that  cannot scale and becomes too complex to maintain.

So the real question is, how do we get to a solution that can scale, but doesn't have a single point of failure?

In what follows, it is helpful to think of the problem from the perspective of a party, who is not a member of the network in question, who wants to know if an event they care about has happened on that network. That is, given a data structure purporting to be a block, event or transaction from the remote network:
 - Does it perform the action I care about?
 - If so, has the block, event or transaction actually been confirmed as part of the settled history of that network?
    
In any DLT network (e.g. Bitcoin, Ethereum and Corda), there are authorities that verify that transactions are valid. 

In a public network, that relies on a proof of work (PoW) consensus mechanism, any (mining) node on the network can propose the next block of transactions, and every node has to verify and execute the block of transactions on their local copy of the network. In such a system very little trust is required between nodes, but it comes at the expense of high latency and only achieving probabilistic finality of blocks, at best.

In a permissioned network, that relies on a proof of authority (PoA) consensus mechanism (e.g. QBFT), a chosen set of trusted authorities or validators get to propose the next block of transactions. It is, however, still up to each node to verify and execute the block of transactions before adding it to their local copy of the network. In such a system more trust is given to the authorities or validators, but nodes still have the option to verify each block and transaction. It results in a drastic reduction in latency and immediate finality of blocks. Validators sign each block they have verified, and validity consensus is reached between validators on the network.

In a network where pairs of actors have a bilateral communication channel, it could simply be two actors and a notary who confirms that a transaction (e.g. state update) took place. In such a system the two actors would need to have a high level of trust in the notary, but there is even further improvements to latency, transaction throughput and privacy.

Taking it a step further, what would the level of trust between two (potentially) different DLT networks look like? As with a single distributed system, where there are tradeoffs between decentralised (i.e. trustless) and centralised (i.e. trusted) authority, there are also tradeoffs between different interoperability protocols use by distributed systems. On the one end, we could program each system to interpret and keep state of the other system, which would be extremely complex and hard to maintain. On the other end, we could trust an intermediary, creating a single point of failure.  

As we will explore below, one can find a middle ground, where an external system can be programmed to interpret certain types of events or transactions they care about. This protocol does not rely on an intermediary to confirm that a block or transaction does what it needs to, but instead rely on one or more (already) trusted parties, on the network where the event or transaction happened, to deem the block or transaction valid.

## Ethereum block header proofs 

There are some standard patterns that can assist with verifying that a transaction or event on an Enterprise Ethereum network is in fact valid. This pattern is repeatable for all types of transactions or events, and is therefor scalable because it doesn't rely on knowledge about the actors involved in the transaction or event, nor the rules of the network, only on the validators of that network.

Every Ethereum transaction has the opportunity to create events, e.g. to indicate that a transfer did occur, or that a certain asset/token has been earmarked. Each event is recorded in such a manner that it can be verified cryptographically to have occurred in a given block. Furthermore, in QBFT networks, block headers that have sufficient validator signatures can be deemed part of the canonical blockchain. This means that if one receives an event and a signed block header, one can verify whether that event did indeed occur. 

The above approach sits somewhere in the middle of the trust spectrum, perhaps even somewhat on the complicated/decentralised side, but not to the extremes where all state changes on one network need to be interpreted and recorded on another. Trust in the set of validators, that signed the block header, is still required. Their signatures, as part of a cryptographic proof, must still be trusted. Nonetheless, one does not need to know (and follow) all the rules of the source (or originating) system. It amounts to checking that the block performs the business action that you care about and trusting the validators that it is valid.

Following this approach means that the trusted parties in this scheme, the validators, are also the trusted parties on the network in question, subject to collective monitoring. If the validators turn out to be untrustworthy then the participants in that network will either discover it or will suffer the same consequence. So the risk of relying on the validators, to confirm an action on the Ethereum network in question, is relatively low. 

By contrast, whether a particular block contains an event or transaction, that performs a particular business action, is something that only a small number of participants care about. The risk of relying on a third party to validate the event or transaction is much higher. So it makes sense to push that risk onto the party that cares about it, hence we permit a modest amount of coupling between the networks. Sufficiently so to allow parties to check for themselves that a particular event or transaction is included, but without requiring knowledge of the validating rules of the network in question.

See [Ethereum proofs](./ethereum_proof_verification.md) for technical details on Ethereum block header proofs.

## Corda transaction proofs

Interoperability with Corda based networks has, up to now mostly, made use of intermediary witness models, where third party relayers would attest to transactions occurring on the Corda network, or what is known as vicarious trust, where a party trusts their alter ego on another network.

Witness models come at the cost of adding a layer of trusted parties that does not form part of the original consensus mechanism of the network, and vicarious trust assumes that a participating party is connected to both networks.

The concept of trusting the original validating parties on the Corda network is natural when validating notaries are used in the network. In this case, we can use Corda notary signatures, in the same way we use Ethereum validator signatures, as proof that a transaction is valid and occurred on the Corda network. Unfortunately, very few Corda networks use validating notaries. This is due to the lack of privacy, as validating notaries need to have visibility on the transaction data, which is unfavourable in most Corda use cases.  

More generally, transaction uniqueness consensus is obtained by notary clusters, reaching certainty that a transaction contains only unique unconsumed input states, and transaction validity consensus is obtained by parties involved in the transaction, reaching certainty that the transaction is valid and contract rules were adhered to. 

This means, that to be able to use the same concept as on Ethereum networks, we would need to trust not only the network wide notary cluster members, but also (some of) the individual participants that signed the transaction. The tradeoff here is scalability. Interoperability with Ethereum networks requires the verifying network to maintaining a list of active network wide validator pool members, whereas interoperability with Corda networks requires the verifying network to maintain a list of network-wide notaries as well as individual parties that signed the transaction. The information that needs to be on-boarded and maintained on the target network can easily become unmanageable.

As with Ethereum proofs, where verified event data needs to be extracted from Ethereum logs and interpreted by business logic, there is verified transaction data that needs to be extracted from Corda transaction output states and interpreted in a similar way.   

The Ethereum verifier contract has no problem understanding its own native event structure as embedded in the transaction receipt logs. It is however difficult (or resource intensive) for the EVM to interpret the Corda AMQP-encoded structures that was signed over. It would be much more efficient to agree to, a once off trusted setup, verifying zero-knowledge (e.g. SNARKS and STARKS) proofs instead, where public input contains what is being verified by the underlying circuits. Any DLT network that supports verification of transactions or events in computationally verifiable circuits would be able to seamlessly integrate with interoperability protocols used by Enterprise Ethereum networks.

See [Corda proofs](./corda_proof_verification.md) for technical details on Corda transaction attestation proofs.

## Limitation of security guarantees in distributed system interoperability 

Block consensus finality in PoW Ethereum networks can impose risks of a block header being used that was not yet finalised by the network. It is important to consider the underlying consensus mechanism. Ethereum block header proving schemes referenced here are limited to IBFT 2.0 and QBFT block headers.

Corda transactions are not deemed valid just because they were signed by the required participants and notary. The notary signature gives us some assurance that the transaction occurred on the network but not that it was validated against contract rules by one of the participants. It is important that the Corda application flows include transaction validation as a requirement for it to be deemed a compatible CorDapp.

## References ##

1. [EEA Crosschain DLT Interoperability Technical Specification Draft](https://entethalliance.github.io/crosschain-interoperability/draft_dlt-interop_techspec.html) 
