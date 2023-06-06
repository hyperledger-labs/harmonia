# Problem Statement

If it is to provide meaningful guarantees of correct behaviour over and above those which are provided individually by the interoperating platforms, an interop solution must go beyond a “command and control” model focused on providing a repertoire of individual actions and mechanisms for carrying them out. It must also describe how cross-network workflows can prepare and reliably execute sequences of actions in which possible behaviour at each step is constrained by conditions created by previous steps. Crucially, the possible behaviour of one network must be able to be constrained by the state of another.

*TODO: DIAGRAM showing on-network transaction scope vs cross-network transaction scope*

This requirement is in tension with the architectural principle that networks should require very limited visibility over each other’s state, as well as the practical reality that some networks, such as Corda, restrict such visibility by intentional design.

We assume that it is not generally possible for an agent on one network to directly inspect and interpret the relevant parts of another’s ledger when it needs to satisfy itself that some condition has been met. Instead we must carefully outline the relationships of authority, visibility, trust and verifiability that enable “portable proofs” to be constructed and passed between networks.

## “Our Word Is Our Bond” versus “Reliable Reciprocity”

The problem we are concerned with is how to make cross-network workflows reliable in the absence of centralised co-ordination. The type of reliability that matters here is not just reliable delivery of individual messages and execution of individual commands, but the reliable setting and enforcement of conditions governing sequences of actions carried out between multiple parties across different network contexts.

Some of these parties may be mutual adversaries with incentives to lie, cheat, or deny service to each other. Even where legal identity and accountability means that deliberate malfeasance is unlikely, we must consider that a collaborator may have fallen prey to an attacker who does not care about their reputation, or may be otherwise unable to uphold their side of a bargain owing to circumstances beyond their control. We therefore want to be able to generate assurances that promised outcomes will be able to be realised even if the promiser is no longer willing or able to stand behind them.

In an interop environment in which there is always a higher authority to appeal to, with the power to set things right if promises are not kept, the role of technical solutions may be limited to making mishaps less likely, rather than providing strong guarantees that only valid outcomes will be reached. We may call these "Our Word Is Our Bond" environments. If it can be shown to the higher authority that an agreement was reached that something should happen, then it can act to restore fairness if one side of that agreement subsequently defaults. This reduces the technical problem to one of gathering and distributing evidence of consent to some course of action, usually in the form of digital signatures, and then making a best-effort to execute it reliably.

The patterns we consider here are intended to enable stronger guarantees than this to be given. I must be able to promise something conditionally in such a way that, if my conditions are met, the receiver of the promise is guaranteed to get what was promised without my further co-operation. We call this "Reliable Reciprocity".

## Reliable Reciprocity

In cross-network interoperation we are often concerned with pairs of reciprocating actions, where an action one network is coupled with an action on the other which reciprocates it: the quid pro quo of an asset swap, or the rewarding of a “burn” with a “mint”.

The condition of reliable reciprocity between a pair of actions is met when the following obtain:

* **Ordering**: The actions are taken separately, rather than transacted atomically, and are taken in order: one goes first, then the other goes second.

* **Conditionality**: The second action can only be taken if the first has been taken.

* **Consequentiality**: If the first action has been taken, the ability to take the second action must automatically become available as a consequence, and should not be able to be maliciously blocked.

Reliable reciprocity means that we can give guarantees such as “if you take the consideration I offer, I must as a result be able to take the asset for which it was offered without your further co-operation” where we cannot give guarantees such as “the exchange of asset for consideration occurs fully atomically” (e.g. within the scope of a single transaction on a single network).

Reliable reciprocity typically requires the ability to establish a lock on one network which will automatically be released on presentation of proof that an action meeting my conditions has occurred on another. This in turn introduces fundamental questions about how such a proof is to be constructed and validated.
