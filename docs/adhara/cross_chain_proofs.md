# Cross chain proofs #

## Introduction ##

Cross chain interoperability has a number of trade offs. The two key trade offs are:
 - How much trust is vested in an intermediary notifying one chain that something happened on another chain versus
 - How much does one chain need to know about the mechanisms on another chain in order to verify that something happened on that chain

If we go too far to the left and trust an intermediary explicitly, we open the door for single point of failure where a bad actor can fraudulently declare truth on one chain that doesn't exist on another.

If we go too far to the right, we risk interlinking two systems and the interoperability process cannot scale and becomes too complex to maintain.

So the real question is, how do we get to a solution that can scale, but doesn't have a single point of failure?

In what follows, it is helpful to think of the problem from the perspective of a party _who is not a member of the network in question_ who wants to know if an event they care about has happened. That is: given a data structure purporting to be a block from the remote chain, 1) does it perform the action I care about and 2) if so, has the block actually been confirmed as part of the settled history of the chain?
    
In any distributed (e.g. like Bitcoin, Ethereum and Corda) system, there are authorities that verify that transactions are valid. In a public blockchain network that relies on a proof of work consensus mechanism, there are parties that propose the next block of transactions, however each node has the authority to verify and execute the block of transactions on their local copy of the blockchain. In such a system very little trust is required between nodes, however this comes at the expense of latency and only achieving probabilistic finality of blocks, at best.

In a permissioned network using a proof of authority consensus mechanism like QBFT, a chosen set of authorities/validators get to propose the next block of transactions, however it is still up to each node to verify and execute the block of transactions on their local copy of the blockchain. In such a system more trust is given to the authorities/validators, however each node can still verify each block and transaction, and there is a drastic reduction in latency and immediate finality of blocks can be achieved. Importantly, the validators are expected to sign each block they have verified

In a network where each pair of actors have a bilateral channel, it could simply be the two actors and a notary who confirms that a transaction (e.g. state update) took place. In such a system the two actors would need to have a high level of trust in the notary, however there is even futher improvements to latency, transaction throughput and privacy.

Taking it a step futher, how would the trust between two (potentially) different systems look like? As within a single distributed system where there are tradeoffs between decentralised (i.e. trustless) vs centralised (i.e trusted) trust/authority, there are also tradeoffs between different approaches to distributed system interoperability. On the one end of the spectrum we could "teach" each system to interpret and keep state of each other system, which would be extremely complex and hard to maintain. On the other end of the spectrum, we could trust an intermediary. However, this creates a single point of failure where a bad actor can fraudulently declare truth on one system that doesn't exist on another.  

As we will explore below, one can also imagine a scenario where an external system can be 'taught' how to determine the presence of certain types of events they care about, so that they don't rely on anybody else to tell them if the block does what they need it to, but where they do rely on one or more trusted parties to tell them whether the block was actually confirmed (and so was hence also valid).

## Some standard patterns ##

In Enterprise Ethereum based networks, there are some standard patterns that can assist with verifying that a transaction or event on an Enterprise Ethereum network is in fact valid. This pattern is repeatable for all types of events and is scalable because it doesn't rely on needing to know about all of the actors on (or rules of) a network, simply the validators on that network.

## Block header based proofs ##

### Introduction to block header based proofs ###

In Ethereum each transaction has the opportunity to create events, for example that a transfer did occur, or that a certain asset/token has been earmarked. Each event is recorded in such a manner that it can be mathematically verified to have occured in a given block header. Futhermore, in QBFT networks, block headers that have sufficient validator signatures can be deemed as part of the canonical blockchain. Taking these two pieces together, it means that if one receives an event and a signed block header, one can verify whether that event did indeed occur. 

The above approach sits somewhere in the middle of the trust spectrum, perhaps even somewhat on the complicated/decentralised side, but not to the extremes mentioned previously (insert section number?). This is because one still needs to trust the set of validators that signed the block header, verify their signatures and verify a mathematical proof. Nonetheless, one does not need to know (and follow) all the rules of the source (or originating) system; one merely needs to know enough to check for oneself that the block performs the business action that you care about.

This approach becomes compelling when one notes that the parties one must trust in such a case - the validators - are also the parties that participants on the network in question are already also implicitly trusting, as well as collectively monitoring. If the validators turn out to be untrustworthy then the participants in that chain will either discover it or will suffer the same consequence as you. So the risk of relying on the validator signature to answer the 'was it confirmed?' question is relatively low. 

By contrast, the question of whether a particular block contains an event that performs a particular business action is something that only a small number of parties care about, and the risk of relying on a third party to answer that question is much higher. So it makes sense to push that risk onto the party that cares about it: hence we permit a modest amount of coupling between the networks, sufficient to allow relying parties to check for themselves that a particular event is included, but without requiring them to know anything about the rules of the network in question.

### Generating a block header based proof ###

Each transaction that is executed on Ethereum results in a transaction receipt, which contains all events that the transaction created. The transaction receipts of all the transactions in a block is used to calculate a Patricia Merkle Tree root, which is included as one of the fields in the block header. Therefore, to prove an event is part of a block, one needs to construct a Patricia Merkle Tree proof.

### Verifying a block header based proof ###

Each QBFT block header is signed by (at least) a subset of validators of a network, therefore one would need to compare the signatories to an expected list of validators, as well as ensure the number of signatories exceeds a certain threshold, for a given network.

In addition, the Patricia Merkle Tree root would need to be recalculated, using the event in question, and then verify that this root matches the transaction receipt root in the previously verified block header. Recalculating the Patricia Merkle Tree root only requires a subset (only sibling and parent nodes of the transaction receipt containing the event in question) of the Tree, very similar to how a Merkle Tree proof would be verified.

## Limitation of security guarantees in distributed system interoperability ##

TODO

## References ##
EEA Crosschain Interoperability Technical Speciofication Draft [https://entethalliance.github.io/crosschain-interoperability/draft_crosschain_techspec.html](https://entethalliance.github.io/crosschain-interoperability/draft_crosschain_techspec.html) 
