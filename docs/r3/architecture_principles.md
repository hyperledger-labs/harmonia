# Architecture Principles

The options and patterns discussed here are bounded by a set of background architectural assumptions and aspirations. We are concerned not only with the immediate functional realisability of interop use cases, but also with fostering an interop ecosystem that will remain viable in the longer term as it evolves and expands.

1. Anchored in Production implementation  
Standards need to be driven by actual implementations across production targeting business platforms not theory, ensuring that the practically implementation, performance and maintenance of these are well grounded.

2. Minimum (Wholesale) business flow scope 
Standards need to cover, at a minimum, the patterns of interop to enable intraday XvP for Repo, FX Swaps, Equity/Bond Settlement. This is therefore focused on the regulated wholesale capital markets segment, it does not intend to cover public network interop or bridges, it is assumed that for regulatory purposes the assets do not leave the originating network/chain.

3. Minimum protocol scope 
Standards need to cover, at a minimum, interop patterns across R3 Corda and EVM (HL BESU) driven by the majority of business platforms coming to market and testing via the TestNet. [Corda 4/5 to Corda 4/5, Corda 4/5 to EVM, EVM to EVM]

4. Aligned to relevant standards body 
Standards need to be seeded to the relevant standards body where it can be credibly supported. This standard is cross protocol (ie not EEA) and across capital markets business lines. [Note: One for the community to steer, options provided by working group].

5. Maintains community incentive alignment  
Standards & reference implementations drive aligned incentives for both those developing/maintaining the standards but also in terms of not locking in a piece of 3rd party software / tech required that can be monetised by one party only.

6. Reference implementations accelerate adoption  
Standards require reference implementations to accelerate the adoption within their protocols, these reference implementations should be open source 

7. Patterns anchored around Wallet/Asset holders not bridges in the first instance   
New DLT business platforms are natively designed to operate without message brokers and their standard patterns will revolve around the banks (wallet/asset holders) deploying the required interop orchestration (based on reference implementations)

