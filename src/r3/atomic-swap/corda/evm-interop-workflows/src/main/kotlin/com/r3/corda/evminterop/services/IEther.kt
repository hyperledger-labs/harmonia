package com.r3.corda.evminterop.services

import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

interface IEther {
    fun balanceOf(address: String): FlowExternalOperation<BigInteger>
}