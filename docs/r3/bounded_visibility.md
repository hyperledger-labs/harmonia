# Bounded Visibility

A private, permissioned distributed ledger such as Corda’s does not expose the entire history of all ledger transactions to all nodes in the network. The ability to view a transaction is typically limited to network peers who are parties either to that transaction or to subsequent transactions which include that transaction in their backchain.

This means that we have to be precise about who, on a Corda network, is being asked to provide evidence that a transaction has taken place, which in turn means that we may have to consider whether they are a trustworthy informant.

Suppose that network A in our cross-chain swap example is a Corda network. Having prepared the transfer of the asset from Q' to P' on network B, we want to know that the transfer of the asset from P to Q on network A has also been prepared, completing the first phase of a two-phase commit, before proceeding to commit the transfer of the asset from Q' to P' on network B.

Because the interests of P are aligned with those of P', there is an incentive for P to give false information so that P' will unfairly receive the benefit of the swap, without Q receiving their side of the exchange.

*TODO: DIAGRAM*

If we instead rely on Q, who will also see the transaction which prepares the asset transfer from P to Q, then we know that Q has no incentive to lie, since Q cannot receive the asset unless the transfer has genuinely been prepared.

Visibility may also be curtailed by events such as a node leaving a network, so that it is no longer available to consult, or ledger transactions moving out of a time-limited observation window and needing to be recalled from long-term storage.
