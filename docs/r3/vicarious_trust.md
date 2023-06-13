# Vicarious trust

Recall that in our cross-chain swap scenario, the network identities ALice@USD and Alice@GBP have a “common interest”, as do Bob@USD and Bob@GBP. We describe these pairs as being in an “ego” / “alter ego” relationship: Alice@GBP acts as Alice@USD's proxy (or “vicar”) on the GBP network, through whom Alice@USD can enquire and act vicariously.

Vicarious trust means that Bob@USD can instruct Bob@GBP to act on its behalf by preparing the transfer of an asset to Alice@GBP, creating a "hold" state from which the asset can only be released on presentation of proof that Alice@USD has transferred a reciprocating asset to Bob@USD.

In defining the asset lock on the GBP network which the proof from the USD network must unlock, Bob@GBP relies on Bob@USD’s knowledge of the USD network, and accepts on trust that a signature with a given key K on a given hash H is proof (as Bob@USD knows it to be) that the required action has taken place.

Vicarious trust means that the application on the GBP network does not need to be able to inspect, decode and validate transactions recorded in the USD network. This supports our architectural goal (see [principle 5](architecture_principles.md)) that network-local knowledge about the structure and meaning of transaction data, and the identities of validators/notaries, should not leak across network boundaries.

In a Corda-EVM interop scenario, the ego/alter ego trust relationship may be directly realised through the Corda identity Alice@USD holding the signing key of the EVM address Alice@GBP. In this case, Alice@GBP cannot help but “do” whatever Alice@USD  commands, since possession of a signing key is all that is needed to propose a transaction to the EVM global computer.

## Breakdown of the vicarious trust model

The post-Merge Ethereum proof-of-stake consensus infrastructure is [complex](https://ethos.dev/beacon-chain), and the usual way of discovering the finalisation status of a block containing a transaction is to ask a node that implements the full set of rules, and has access to the full range of contextual information (e.g. which nodes are validator nodes, and how much ETH they have staked) needed to evaluate those rules. This makes offline validation of that status difficult.

The initial parts of the proof are straightforward enough: given a block hash, provide a Merkle Patricia inclusion proof showing that the transaction is part of the block. But the final part depends on an observation: I must query an Ethereum node to confirm the finalisation status of that block.

It is possible that a node subverted by an attacker might misreport that status. I can mitigate this risk by consulting multiple nodes and requiring that a quorum of them agree on the history. However, the problem remains that I only know that a transaction has been finalised on the basis of direct observation.

Suppose that the USD network in our cross-chain swap example is a Corda network, and the GBP network is the public Ethereum blockchain. An asset is locked on the Corda USD network which Bob@USD can obtain by presenting proof that a specific transaction has been carried out on the Ethereum GBP network. Because Bob@USD trusts the consensus among the Ethereum nodes it has queried, it knows that the transaction has been finalised. However, Bob@USD’s trust in its own informants is not enough to release the lock, since Bob@USD could always construct a forged transaction and falsely claim to have observed that it was finalised. This violates reliable reciprocity (conditionality), since the second action (Bob@USD gets the asset) is no longer fully conditional on the first (the Ethereum transaction must have taken place).

We could turn to Alice@USD, the original owner of the asset who placed it in the lock, and stipulate that the lock will only be released if Alice@USD signs that it, too, has observed that the transaction has been finalised. But this means that Alice@USD, by refusing to give its signature, can block Bob@USD from obtaining an asset to which they are entitled. This also violates reliable reciprocity (consequentiality), since the second action (Bob@USD gets the asset) can be blocked even though the first has taken place.

The only way out of this problem is to have a way of presenting proof of finality on the Ethereum network that does not depend on observations by potentially unreliable parties with a motive for deceit. Two approaches are available:

* On private EVM networks with deterministic finalisation based on BFT consensus between a pool of validators, we can depend on showing a supermajority of validator signatures provided that the validators' identities are known in advance of setting up the proof conditions.
* Otherwise, we can resort to trusted 3rd-party observation and attestation on the network that will receive the proof: the intervention of what we will call a [remote finality guarantor](remote_finality_guarantors.md).
