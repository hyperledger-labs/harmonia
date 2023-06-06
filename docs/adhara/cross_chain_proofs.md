#Cross chain proofs#

##Introduction##
Cross chain interoperability has a number of trade offs.  The two key trade offs are:
 - How much trust is vested in an intermediary notifying one chain that something happened on another chain versus
 - How much does one chain need to know about the mechanisms on another chain in order to verify that something happened on that chain

If we go too far to the left and trust an intermediary explicitly, we open the door for single point of failure where bad actor can fraudulently declare truth on one chain that doesn't exist on another.

If we go too far to the right, we risk interlinking two systems and the interoperability process cannot scale.

So the real question is, how do we get to a solution that can scale, but doesn't have a single point of failure?

##Discussion on authorities##
In any distributed system, there are authorities that verify that transactions are valid.  In a chain that relies on a proof of work consensus mechanism, the authority is the node who proved they had done the work to validate a block of transactions.  In a permissioned network using a proof of authority consensus mechanism, it is the delegated authority nodes who verify a block of transactions.  In a bilateral channel, it could simply be the two actors in a transaction who confirm that the transaction took place because there is a single shared record of that transaction.

The complication comes when transferring that varification to another system that may have different rules.  How does that other system "know" that the transaction is a valid transaction without having to duplicate all of the rules of the source system?

##Some standard patterns##
In Enterprise Ethereum based networks, there are some standard patterns that can assist with verifying that transaction or event on an Enterprise Ethereum network is in fact valid.  This pattern is repeatable for all types of events and is scalable because it doesn't rely on needing to know about all of the actors on a network, simply the validators on that network.

##Block header based proof verification##

###Introduction to block header based proofs###

###Generating a block header based proof###

###Verifying a block header based proof###

###References###
EEA interop working group submission
    
