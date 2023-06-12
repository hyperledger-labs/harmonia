# Appendix 1: Identity, Messaging and Consensus in Corda and EVM Networks

In Corda there is a one-to-one relationship between nodes in the network and permissioned identities maintained by the network manager. The CorDapp run by a node will run flows which exchange messages with other nodes/identities via the Corda platform's peer-to-peer messaging protocol. A "peer" in a Corda network is a node, addressable via RPC messages sent to the internet-addressable gateway of a Corda cluster.

Consensus in the Corda UTXO ledger is reached between identities with the additional mediation of Notary nodes which track the consumed states of transactions and interdict double-spends. A UTXO transaction is finalised when a Notary has signed it, and is distributed to those identities which are party to the transaction as part of the finalisation process.

In the EVM model, ground-level identity is given by EVM _addresses_ tied to signing keys. An EVM address is a digest of the public key in a cryptographic key pair, and whoever holds the corresponding private key has the right/ability to "act as" that address on an EVM network, by signing transactions proposed to the EVM network.

_Nodes_ in the EVM network have no necessary connection to identities, and may simply be part of the data distribution network through which ledger updates are propagated to interested parties. Some nodes may have a special role in implementing the consensus mechanisms of the network, e.g. "consensus clients" on the proof-of-stake post-Merge Ethereum mainnet, or "validator" nodes in some private blockchains.

Peer-to-peer messaging between EVM nodes is the anonymous propagation of EVM transactions and validation proofs. Messages between identities are commonly passed via transactions written to the ledger, which records not only updates to contract state but also log entries which are broadcast to all peers in the network.

Consensus on an EVM transaction is reached between network peers (in the first instance, those that apply the consensus algorithm), not identities.

An "agent" representing an EVM identity is an internet-addressable endpoint through which an authorised client can request that transactions be added to the EVM ledger using that identity's signing keys (held by the agent itself). Not all interoperability topologies will involve agents of this kind; for example, a Corda node representing a Corda network identity may directly hold and use (by making calls over the internet to one or more trusted VM nodes) the signing keys associated with a corresponding EVM identity.
