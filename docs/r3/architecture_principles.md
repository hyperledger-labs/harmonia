# Architecture Principles

The options and patterns discussed here are bounded by a set of background architectural assumptions and aspirations. We are concerned not only with the immediate functional realisability of interop use cases, but also with fostering an interop ecosystem that will remain viable in the longer term as it evolves and expands.

We identify five architectural principles:

1. **Respect finality**. No network should be required to repudiate a transaction finalised on that network because of the failure of a cross-network workflow.
2. **Avoid nondeterminism**. The success or failure of a cross-network workflow should depend entirely on the fully-orderable sequence of actions carried out within that workflow, and not on observable nondeterminism caused by temporal constraints (time locks).
3. **Eliminate unilateral bottlenecks**. The design of a cross-network workflow should avoid situations where one party alone can advance the state of the workflow, such that it might stall if that party became unavailable or maliciously sought to delay or halt progress.
4. **Leverage trust to simplify proof**. Where trust relationships can be established, use them to limit the need for external agents to access and utilise network-local and application-specific knowledge.
5. **Minimise modelling of one network inside another**. As far as possible, one network's smart contract logic should not require or implement a detailed model of another's data structures and consensus mechanisms.

We define some basic terms below, then discuss each of these principles in detail.

## Networks and cross-network workflows

At a very high level, in the context of distributed ledger technologies, a “network” combines the following functions:

* **Identity and messaging**: A DLT network connects a set of identities, establishing an identity model through which they are able to able to consistently refer to one another, and providing mechanisms for communication between identity-holders.
* **Ledger and consensus**. A DLT network maintains a universe of shared valid facts, in the form of a ledger, providing mechanisms through which consensus can be reached across the network about what these facts are.

See [Appendix 1, Identity, Messaging and Consensus on Corda and EVM networks](identity_messaging_and_consensus.md), for a discussion of how these concepts map into these specific cases.

In this document we are concerned with _cross-network worflows_: sequences of operations crossing network boundaries, for which there are no such common network-provided models or mechanisms. This means that:

* Mutually-recognised identities and lines of communication between them need to be explicitly configured: there is no centralised and global arbiter of identity, and management of such links must be carried out by the linked entities themselves.
* The acceptance, by consensus among peers, of a fact on one network does not mean that this fact can immediately be treated as an accepted fact on the other network.

The evidence that a fact has been accepted as valid on one network may not always be intelligible, or able to be comprehensively validated, on the other.

## 1. Respect finality

A key guarantee of many distributed ledger technologies is that a network's consensus on a transaction is final. Except in very exceptional cases (such as a network-wide fork of the ledger's transaction history), transactions cannot repudiated once finalised.

This means that any "hold" state awaiting cross-network consensus must typically be implemented as a _locally final_ transaction outcome which cannot be revoked, but only countered with a subsequent transaction.

This is because once an asset has been transferred in one transaction the recipient is usually immediately free to spend it elsewhere, and it might not then be recoverable by a second transaction intended to "roll back" some cross-network workflow.

If we tracked subsequent spends we could potentially reclaim them, but this would spread uncertainty and complexity throughout the network. Instead, we should distinguish between "hold" states in which an asset is temporarily reserved for a cross-network workflow and can only be moved in accordance with that workflow's rules, and "free" states in which the cross-network workflow has definitively concluded and the asset or balance can be freely moved by the resulting owner without risk of repudiation.

The risk then arises that a cross-network workflow might stall due to non-cooperation or unavailability of one party, leaving an asset or balance in a "hold" state from which its original owner or rightful recipient cannot recover it. It is tempting to try to resolve this possibility with a time-based automatic release mechanism, but our second principle cautions against this approach.

## 2. Avoid nondeterminism

The outcome of a cross-network workflow should never depend on observable nondeterminism such as that caused by variable network latency time or clock drift between two timestamp authorities.

In practice, this rules out timelock-based solutions where a message from one system transmitting proof that an action has been taken on that system might be in a race with a timestamp authority on the receiving system determining whether the time window for the reciprocating action is still open.

Although both sides can measure current network latency and try to ensure that they have plenty of time to act within the established time window, the fact remains that a possible outcome of any timelock-based system is that a message is lost or delayed to the point where the time window has expired, one side has acted (with local finality, i.e. the action cannot now be repudiated), and the other can no longer take the reciprocating action. Under some regulatory frameworks the possibility of this outcome is forbidden.

## 3. Eliminate unilateral bottlenecks

Wherever possible, the ability to advance the state of a cross-network workflow should not be restricted to only one "side" in the transaction. For example, either the sender or the receiver in an asset exchange should be able to initiate a "revert" causing assets to be recovered from  "held" states and returned to their initial owners.

Adherence to this principle remedies the "stalled workflow" problem in a number of cases, and eliminates the possibility of denial-of-service attacks in which one party maliciously delays advancing the state of a cross-network workflow in order to tie up the assets of another.

## 4. Leverage trust to simplify proof

Attestation of a fact by a trusted party is often easier to verify than the fact itself, particularly where network-local or application-specific knowledge is needed to establish the fact but verification must take place outside the bounds of the originating network and/or application.

This principle supports the next:

## 5. Minimise modelling of one network inside another

Cross-network workflows may rely on co-ordination mechanisms based on releasing a "hold" state on one network when proof is presented of action taken on another network. In this case:

* The action should be identified by the minimum set of observable parameters needed to distinguish it from other actions.
* The proof should rely to the smallest possible degree on understanding of the other network's ledger data structures and consensus mechanisms.

For example, rather than exhaustively defining the EVM transaction that must have occurred on another network to release a hold state on our network, we might stipulate that a transaction must have occurred which logs an event of a specific type, with a parameter containing an agreed reference ID. All other characteristics of the transaction are ignored.

Observing that an event has been logged as part of an EVM transaction involves gathering evidence that the event was recorded in a transaction belonging to a block that was finalised on the EVM network's ledger. The smart contract logic on the verifying network should ideally not have to know how to interpret and verify this evidence; instead it might rely (based on the previous principle) on an attestation from a trusted party that the log entry was recorded in a finalised transaction.
