# Bounded Visibility

A private, permissioned distributed ledger such as Corda’s does not expose the entire history of all ledger transactions to all nodes in the network. The ability to view a transaction is typically limited to network peers who are parties either to that transaction or to subsequent transactions which include that transaction in their backchain.

This means that we have to be precise about who, on a Corda network, is being asked to provide evidence that a transaction has taken place, which in turn means that we may have to consider whether they are a trustworthy informant.

Suppose that the USD network in our cross-chain swap example is a Corda network. Having prepared the transfer of the asset from Bob@GBP to Alice@GBP, we want to know that the transfer of the asset from Alice@USD to Bob@USD has also been prepared, completing the first phase of a two-phase commit, before proceeding to commit the transfer of the asset from Bob@GBP to Alice@GBP.

Because the interests of Alice@USD are aligned with those of Alice@GBP, there is an incentive for Alice@USD to give a false positive report so that Alice@GBP will unfairly receive the benefit of the swap, without Bob@USD receiving their side of the exchange.

If we instead rely on Bob@USD, who will also see the transaction which prepares the asset transfer from Alice@USD to Bob@USD, then we know that Bob@USD has no incentive to give a false negative report, since Bob@USD cannot receive the asset unless the transfer has genuinely been prepared.

Visibility may also be curtailed by events such as a node leaving a network, so that it is no longer available to consult, or ledger transactions moving out of a time-limited observation window and needing to be recalled from long-term storage.
