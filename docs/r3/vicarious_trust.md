# Vicarious trust

Recall that in our cross-chain swap scenario, the network identities P (on network A) and P' (on network B) have a “common interest”, as do Q and Q'. We describe these pairs as being in an “ego” / “alter ego” relationship: P' acts as P’s proxy (or “vicar”) on network B, through whom P can act vicariously.

Vicarious trust means that Q on network A can instruct Q' on network B to act on its behalf by preparing the transfer of an asset to P', which can only be committed on presentation of proof that P on network A has transferred a reciprocating asset to Q.

*TODO: DIAGRAM showing who P, Q etc are*

In defining the asset lock on network B which the proof from network A must unlock, Q' relies on Q’s knowledge of network A, and accepts on trust that a signature with a given key K on a given hash H is proof (as Q knows it to be) that the required action has taken place.

Vicarious trust means that the application on network B does not need to be able to inspect, decode and validate transactions recorded in network A. This supports our architectural goal that network-local knowledge about the structure and meaning of transaction data, and the identities of validators/notaries, should not leak across network boundaries.

In a Corda-EVM interop scenario, the ego/alter ego trust relationship may be directly realised through the Corda identity P holding the signing key of the EVM address P'. In this case, P' cannot help but “do” whatever P commands, since possession of a signing key is all that is needed to propose a transaction to the EVM global computer. (Unlike in Corda, where network nodes are in a one-to-one relationship with network identities, in Ethereum the identity X is purely the cryptographic capability to sign-as-X, and has no necessary connection with the nodes of the network).

## Breakdown of the vicarious trust model

On the Ethereum mainnet, observing finalisation of a transaction (i.e. that it “really happened” by consensus of the Ethereum network) involves gathering evidence that the chain to which the transaction belongs has been accepted by a majority of network peers. Typically this is done by waiting for a number of transaction blocks to have been added to the blockchain after the block in which the transaction was recorded, since the likelihood that consensus has been irrevocably reached increases exponentially with each new block added.

How do we then construct a proof, to be verified offline on another network, that a transaction has taken place? The initial parts of the proof are straightforward enough: given a block hash, provide a Merkle Patricia inclusion proof showing that the transaction is part of the block. But the final part depends on an observation: I connect to an Ethereum node and request its current version of the recent blockchain history. If I observe that the block I am interested in is now several blocks deep within that history, then I have reason to believe that the transaction has been finalised.

*TODO: Diagram showing relationship between proof and observation*

It is possible that a node subverted by an attacker might fabricate a false history of subsequent blocks to convince me that a transaction has been finalised when it has not. I can mitigate this risk by consulting multiple nodes and requiring that a quorum of them agree on the history.

*TODO: Diagram showing same scenario but with multiple nodes consulted*

However, the problem remains that I only know that a transaction has been finalised on the basis of direct observation.

Suppose that network A is a Corda network, and network B is the Ethereum mainnet. An asset is locked on network A which party Q can obtain by presenting proof that a specific transaction has been carried out on network B. Because Q trusts the consensus among the Ethereum nodes it has queried, it knows that the transaction has been finalised. However, Q’s trust in its own informants is not enough to release the lock, since Q could always fabricate a transaction and falsely claim to have observed that it was finalised. This violates reliable reciprocity (conditionality), since the second action (Q gets the asset) is no longer fully conditional on the first (the Ethereum transaction must have taken place).

We could turn to P, the original owner of the asset who placed it in the lock, and stipulate that the lock will only be released if P signs that it, too, has observed that the transaction has been finalised. But this means that P, by refusing to give its signature, can block Q from obtaining an asset to which it is entitled. This also violates reliable reciprocity (consequentiality), since the second action (Q gets the asset) can be blocked even though the first has taken place.

The only way out of this problem is to have a way of presenting proof of finality on the Ethereum network that does not depend on observations by potentially unreliable parties with a motive for deceit. Two approaches are available:

* On private EVM networks with deterministic finalisation based on BFT consensus between a pool of validators, we can depend on validator signatures provided that the validators' identities are known in advance of setting up the proof conditions.
* Otherwise, we can rely on 3rd-party observation and attestation on the network that will receive the proof: the intervention of what we will call “witnesses”.
