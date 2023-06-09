# Remote Finality Guarantors

Remote finality guarantors may be needed in the following situation:

* A hold state on a network requires proof of action on another to be released
* Proof of action must include evidence that the action was fully finalised on the remote network
* The only feasible source of information about finality is an online query made against an active node (or quorum of active nodes) on the remote network

For the reasons discussed [previously](vicarious_trust.md), we cannot rely on observations made by either the sending or receiving party on the local network, since the receiver has an incentive to fabricate false positive observations, and should still be able to obtain and use a proof of action in the event that the sender becomes unavailable or uncooperative.

A neutral 3rd-party, trusted by both sides, can break the deadlock by acting as a guarantor.

Importantly, we do not need to make _every_ transaction dependent on the intervention of a guarantor. We can set up a lock in such a way that _either_ of the sender or the guarantor's signatures will be accepted as the attestation of a "remote finality observer" that the transaction on the remote network was observed to be finalised.

In the normal case, the receiver will request a signature from the sender, and the sender will check finality on the remote network and sign. If the sender is unavailable or uncooperative, the receiver can turn to the guarantor. This removes the sender's incentive to default, since they cannot effectively block the receiver's acquisition of the asset by doing so, and gives the receiver a reliable way to move forward if the sender becomes unavailable.

The guarantor does not have to be a single identity, but may be drawn from a pool of agreed identities. Among the options are:

* A single authoritative guarantor node is provided by an identity already acting as a source of trust on the network, e.g. the operator of a Corda notary
* A pool of guarantor nodes is volunteered by a subset of peers on the network, and a supermajority of them must sign to mitigate the risk that a single guarantor will collude with a dishonest party
* All nodes on the network offer guarantor services to all other nodes, and a subset is randomly chosen by both sides to ensure fairness.

## Corda/Ethereum asymmetry

Corda and EVM networks use asymmetric combinations of trust and proof to construct proofs of action.

Finality on a Corda network can be proven with the signature of a notary on a transaction hash, which can be checked offline provided that both the notary's public key and the transaction hash have been precomitted. Vicarious trust is sufficient in this case to establish what will constitute a complete proof of action on the Corda network.

Suppose that our two networks are a Corda network and an EVM network, and Alice@Corda has agreed to transfer an asset to Bob@Corda in exchange for Bob@EVM making a reciprocating transfer to Alice@EVM.

Alice@Corda constructs a _draft transaction_ which would place the asset on the Corda network in a lock, from which it could be released to Bob@Corda by presenting proof of action of the reciprocating transfer from Bob@EVM to Alice@EVM.

Bob@Corda verifies that the draft transaction will do what is agreed and that the lock conditions will be able to be satisfied by the agreed action of Bob@EVM. It then provides Bob@EVM with the hash of the draft transaction and the public key of the notary that will finalise it, and Bob@EVM sets up the lock on the other network using these values.

If Alice@Corda now notarises the draft transaction, they will obtain the notary's signature on the transaction hash, which they can pass to Alice@EVM to authorise the transfer of the asset on the EVM network to themselves. This in turn will publicly broadcast the EVM transfer transaction, so that Bob@Corda can obtain the proof of action needed to unlock the asset on the Corda network.

The complete proof of action on the Corda side consists of:

1. Proof that a transaction with a given hash _will perform_ the required action. This is obtained through vicarious trust: Bob@Corda validates the draft transaction and passes the transactino hash on to Bob@EVM, who proceeds on the basis of trust in Bob@Corda.
2. Proof that a transaction with the given hash _was finalised_. This is obtained through a combination of vicarious trust (Bob@Corda has supplied the public key of the notary whose signature means that the draft transaction has been finalised) and offline-verifiable cryptographic proof (the EVM contract checks that the provided transaction hash has been signed with this public key as a condition of completing the transfer transaction).

The complete proof of action on the EVM side consists of:

1. Proof that a transaction _has performed_ the required action. This is obtained by inspecting the events recorded within the transaction's data.
2. Proof that the transaction _was finalised_. This comes in two parts:
    * A Merkle Patricia inclusion proof that the hash of the transaction is included in a block.
    * Proof that the block having that hash was finalised on the EVM blockchain.

Only the last part of the EVM proof of action requires remote finality observation, and it does so only in the case that finality on the remote chain is not deterministically verifiable by presenting checking validator signatures on the block hash.


## Expanding the role of attestation

In the scenario described above, the remote finality observer (either the sender or the guarantor) completes a proof of action by adding attestion of finality on a remote EVM network to a proof that a transaction a) did something in particular, and b) and was included in the finalised block.

For the sake of simplifying the smart contract validation on the target network (e.g. the Corda smart contract that must check the proof of action), we can expand the role of attestation to cover other parts of the proof, i.e.:

1. The proof that the transaction _has performed_ the required action.
2. The Merkle Patricia inclusion proof that the hash of the transaction is included in the finalised block.

This is in line with [architecture principle 4](architecture_principles.md), "Leverage trust to simplify proof". Once Bob@Corda has observed that Alice@EVM has taken the asset from its hold state on the EVM network, it can simply ask Alice@Corda to check that its conditions have been met then sign over the asset owed to Bob@Corda on the Corda network. (Trivially, Alice@Corda trusts Alice@Corda and will accept their own signature as evidence that their conditions have been met). If Alice@Corda is unavailable or uncooperative, then a guarantor can once again be invoked.

In this case, the guarantor has an expanded role: it must not only observe finality, but verify that the finalised block contains a transaction having certain stated properties (for example, it must contain a transaction which emits a named log event containing specific parameter values). Bob@Corda sends the block hash together with a description of the properties that must be verified, and the guarantor attests that it has been able to assemble and verify a _complete_ proof of action meeting that description by signing the hash of the description itself together with the block hash containing the transaction satisfying that description.

The lock contract is set up so that it can be released either by Alice@Corda's signature, or by a guarantor's signature on a description / block hash pair matching a pre-agreed transaction description. As part of validating the draft transaction, Bob@Corda must satisfy themselves that the pre-agreed transaction description matches the action that Bob@EVM must actually take to meet Alice's conditions.

The guarantor in this case is a _transaction validating guarantor_ in addition to being a _remote finality guarantor_. As before, its only role is to step in if Alice@Corda cannot or will not sign the asset over themselves.
