# Requirements for interoperability between regulated platforms

## Introduction 
This description outlines the commercial and regulatory requirements that underpin atomic DvP between an R3 Corda based digital asset platform and a Private, Permissioned, Ethereum-based payment system, such as the Fnality Payment System (FnPS).

## Assumptions 
### General Assumptions 
In describing these flows, we would assume the following:
- **Existence of Rulebooks:** the "Two Systems" - Asset Side and Cash Side (from now on referred as "Two Systems" collectively) have Rulebooks (one per System) defining the rights and obligations that each participant in that System must comply with as well as the penalties for non-compliance. 
 - **Legal Basis:** There is a sound legal basis for the link between the Two Systems (as defined under Principle 20 of the PFMIs) that ensures, among other things, that the Rules of the respective systems are sufficiently aligned to enable final settlement on a DvP basis and, where relevant, the allocation of responsibilities and liabilities.
 - **Participation:** Each participant involved in a DvP settlement holds accounts on the Two Systems
 - **Trade agreement irrevocability:** a DvP trade between two bank participants, once agreed (executed), cannot be changed and must be settled. 

### Key requirements for DvP 
1. **Existence of a Unique Trade ID:** The DvP trade agreed by the two participants has a unique TradeID common for both legs.
2. **TradeID replay:** The two participants cannot use a TradeID already used in a previous DvP settlement attempt (whether the attempt ends successfully or not).
3. **Earmarking:** The Two Systems have the ability to allow some assets to be earmarked first, and then released depending on certain conditions being met.
4. **Proof of earmark:** The Two Systems can provide a cryptographic proof of assets being earmarked ( "proof of earmarking") for a specific TradeID. 
5. **Unilateral Earmark release:** Assets earmarked cannot be released unilaterally by the counterparty who initiated the earmark. The earmarks can only be cancelled in the exceptional case where the trade is cancelled and the cancellation must be based on the trade cancellation processes contained in the rulebooks of the Two Systems
6. **Settlement Finality:** Once earmarks have been placed on the Two Systems, there exists a point where both legs become irrevocable, and final.  Cancellation of earmarks (as per point 5) can only happen before the point of settlement finality.  Cancellation of an earmark on one System can only be done if and only if evidence (a "proof of cancellation") that the settlement has been cancelled on the other System is provided, and successfully processed. 
7. **Proof of earmark understandability:** Each system can understand the proofs provided by the other system (both proof of earmarking and proof of cancellation) and is able to verify those proofs.  This includes the ability to decode and verify signatures provided by other systems.
8. **Nature of the Proofs:** The proof construction is specific to each system.

### Network and connectivity assumptions
1. **Notary Signatures:** Enterprise Ethereum based platforms have a process to onboard and update relevant public keys (notary/counterparty) from Corda networks.
2. **Validators Signatures:** Corda base platforms have a process to onboard and update Validator public keys from Enterprise Ethereum networks.
3. **Connectivity:** participants involved in cross chain interoperability have connectivity to the Two Networks.
4. **Transaction Submission:** participants involved in cross chain interoperability can sign transactions for both networks.
