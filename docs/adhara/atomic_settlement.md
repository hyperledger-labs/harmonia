# Atomic Settlement - Two phase commit protocol

## Introduction 

Atomic settlement across two different platforms is well described below by [Paul Krzyzanowski](https://people.cs.rutgers.edu/~pxk/417/notes/transactions.html)

> A key facet of a transaction is that it keeps data consistent even in
> case of system failures. Transactions are atomic — all results must be
> made permanent (commit) and appear to anyone outside the transaction
> as an indivisible action. If a transaction cannot complete, it must
> abort, reverting the state of the system to that before it ran

The underlying mechanism used is a [Two Phase Commit protocol](https://en.wikipedia.org/wiki/Two-phase_commit_protocol).

## Building blocks for two phase commit 

### ERC 20 
The foundations for two phase commit building blocks in Ethereum Contracts was in the [ERC 20](https://docs.openzeppelin.com/contracts/4.x/api/token/erc20#IERC20) contract definition.  ERC 20 had two functions that were used in conjunction with each other
 - `approve`
 - `transferFrom`

This is used in the case where a sender wants to `approve` an amount to be transferred and gives the spender the authority to complete the transfer using the `transferFrom` function

### ERC 2020
This concept was further refined in the [ERC 2020](https://eips.ethereum.org/EIPS/eip-2020) contract definition which is otherwise called the E-Money Standard Token.  This token allows the wallet owner to place a hold on funds for a specific beneficiary.

> **Holds**: token balances can be put on hold, which will make the held amount unavailable for further use until the hold is resolved (i.e. either executed or released). Holds have a payer, a payee, and a notary who is in charge of resolving the hold. Holds also implement expiration periods, after which anyone can release the hold Holds are similar to escrows in that they are firm and lead to final settlement. Holds can also be used to implement collateralization.

In simple terms if Party A wants to transfer an asset, on one network to Party B, in exchange for a different asset on a different network, then the process would be:
1. Party A places a hold on asset 1 on network 1 with Party B as the beneficiary and Notary A as the notary.
2. Party B places a hold on asset 2 on network 2 with Party A as the beneficiary and Notary A as the notary.
3. Notary A checks both holds are in place and are correct and then executes both holds.
4. If Notary A decides that anything is wrong in the process, then Notary A cancels the holds, thereby releasing the assets to be used in other transactions.

### Proof based Atomic transactions 

The process above shows how the atomic settlement depends on the actions of a notary, which introduces risk into the system.

A way of mitigating this risk is to alter the process by allowing each network to contain some logic, embedded in a smart contract, that can prove the occurrence of an event on the other network.  That logic assumes the role of the designated notary on the transaction and can therefore ensure settlement happens on one network if and only if settlement can be guaranteed on the other network.

These proofs are covered in more detail [here]("./crosschain_proofs.md").

Once settlement has occurred on one network, a proof of settlement can be sent back to the other network, which completes the settlement process on that network.    

The beauty of this construct is that there is no requirement to have a trusted intermediary that could block settlement occurring on one or both networks.  Any entity can generate and transfer proofs between the two networks.
