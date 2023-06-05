# Beyond "command and control"
"Command and control"-style interoperation, where system A instructs system B to do something, or makes decisions based on observable properties of system B, is largely a solved problem at the high level. There is always work to do in defining message standards and communications protocols, but the basic co-ordination mechanisms are well-understood:

* **RPC (synchronous)**: A sends a request to B, which B executes immediately, returning a response indicating the outcome of the request.
* **RPC (asynchronous)**: A sends a request to B, which B reliably queues for processing. A may poll for updates on the processing status of the request, or B may send a message to A when an outcome is reached.
* **Event stream subscription**: A sends a subscription request to B, asking to be notified of events of a certain type, and receives a subscription reference in response. A may then either poll the subscription for new events, or receive a stream of messages from B providing updates as they occur.

The establishment of suitable command-and-control methods for systems to query, instruct and observe one another is necessary but not sufficient for interoperation when the requirements include reliable synchronisation of state changes across networks without a trusted central co-ordinator. Building on these primitives, we also require:

* **Proof of action**: the ability to construct and transmit across network boundaries proof that an action meeting specific criteria has been taken on a network.
* **Offline verification**: the ability to check a proof originating on a network, or from a part of a network, to which the checker does not have ongoing access.

These requirements are more subtle. Suppose we take as proof of action a digital record of a transaction signed by known validators whose signatures attest that it was finalised on its network's ledger. To know whether the record describes the specific action we need to verify, we must be able to interpret its data structure. Because validators typically sign over a network’s internal representations of its ledger transactions, we risk having to build a detailed model of one network’s ledger representation inside the smart contract logic of another.

To help us think more concretely about these questions, we now consider an example use case: the cross-network swap.

## Example: cross-network swap

Our motivating example here is a cross-network swap, in which party P on network A transfers an asset to party Q on network A, in exchange for party Q' transferring an asset to party P' on network B.

We make an assumption that P and P' have a common interest (for example, both represent token wallets held by the same commercial entity), as do Q and Q'.

*TODO: Diagram showing swap parties, asset movements and trust relationships*

The goal is to ensure that the asset transfer from P to Q, and the transfer from Q' to P', either succeed or fail together.

We begin by outlining a centralised solution involving a trusted intermediary. Although this is not the type of solution we are aiming at, it brings all of the problems we have to solve together in one place. Any solution without a trusted intermediary will have to provide the same guarantees via different mechanisms.

## Centralised solution: two-phase commit with a distributed transaction manager

A well-known centralised mechanism for reliable synchronisation of state changes is a two-phase commit orchestrated by an independent distributed transaction manager. We distinguish here between on-network transactions, which affect state in a single network and are finalised on that network, and cross-network transactions.

Each network has the ability to carry out an on-network transaction placing an asset in a "prepared" state from which a subsequent on-network transaction can either return it to its prior state (a "roll back"), or move it forward into its intended final state (a "commit").

In the first phase, the manager sends commands to each network instructing them to move the assets to be exchanged into the "prepared" state. This phase completes when all preparation commands have completed, either successfully or unsuccessfully. In the second phase, if all preparation commands have succeeded, the manager then instructs each network to commit; otherwise it commands them to roll back. The cross-network transaction is complete when all of these instructions have been carried out.

This approach has the following characteristics:

* **Centralised (or centrally delegated) authority**: the distributed transaction manager has the authority to command each network to perform “prepare”, “commit” and “roll back” transactions over the assets in question. It may be granted that authority on a per-transaction basis by each network issuing permission tokens it redeems in carrying out these actions.
* **Centralised state and trust**: the transaction manager keeps track of the state of the two-phase commit, and is trusted by both sides to advance that state in accordance with the protocol.
* **Trusted agents or centralised verification**: either each participant in the cross-network transaction must trust that that the other will accurately report the success or failure of commands issued by the transaction manager, or the transaction manager must be able to obtain and verify proofs from both sides that the actions it has requested have been carried out.

A decentralised solution might begin by removing the centralised intermediary and making one network responsible for managing the state of the swap protocol, for example through a contract on an EVM blockchain.

The contract itself, supported by the EVM network’s validation and consensus rules, could provide guarantees that the state of the protocol evolves correctly. The challenge in this case would be enabling that contract to verify that its commands have been completed properly by both sides.

In the solution we discuss here there is no separate EVM contract co-ordinating the swap. The working state of the protocol is represented by locks created, and then released, on each network’s ledger.
