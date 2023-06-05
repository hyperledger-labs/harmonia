# Portable Proofs
A “portable proof” is a proof that can be checked outside of the context in which it was generated, by an offline verifier with no access to the proof’s network of origin.

Types of elementary portable proof include:

* **A signature on a hash**: proof that the holder of the private key corresponding to a known public key both saw the hash and agreed to sign it, since signatures are assumed to be cryptographically unforgeable. This can usually be taken as evidence that the signer saw and agreed to sign some larger piece of data - the hash preimage - since a signer would not ordinarily sign a hash without knowing what it represented.
* **The “secret” preimage of a predisclosed hash**: proof that the party that generated the predisclosed hash must have disclosed the secret preimage to whichever party now wishes to use it. The preimage must be substantially “unguessable” for this to be valuable.

We are often concerned with a chain of inferences which enables us to take proof-of-X as proof-of Y. For example, if it is known that:

* A given hash H is the hash of a Corda transaction T
* The transaction T has been verified as performing a specific action A
* A given key K is the public key of the notary on the Corda network whose signature on a transaction’s hash means that it has been finalised on the Corda ledger

then we can treat the signature of K on H (proof that the notary on the Corda network signed a transaction with that hash) as proof of action A (proof that a specific action has been taken on the Corda network).

*TODO: DIAGRAM showing sequence of inferences*

This is where vicarious trust becomes important. A peer on a Corda network, having access to a CorDapp’s application logic and the network map of the network, can examine an unsigned draft transaction T and verify all of the above conditions. It can then propose that the signature of K on H (which can be checked offline by an agent on another network) be accepted as proof of action. The actual proof then takes the form “X says that the signature of K on H proves that A, and I trust X, and have the signature of K on H, therefore A”.
