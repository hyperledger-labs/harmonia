package com.r3.corda.evmbridge.workflows.internal

class AggregateResult<TSource, TAccumulate>(val sequence: Collection<TSource>, val accumulator: TAccumulate) {
    operator fun component1(): Collection<TSource> = sequence
    operator fun component2(): TAccumulate = accumulator
}

/**
 * Conditional take-while aggregated sum of a sequence
 */
fun <TSource, TAccumulate> Sequence<TSource>.takeWhileAggregate(
    seed: TAccumulate,
    func: (TAccumulate, TSource) -> TAccumulate,
    predicate: (TAccumulate) -> Boolean
): AggregateResult<TSource, TAccumulate> {
    var accumulator = seed
    val result = mutableListOf<TSource>()
    for(it in this) {
        val tempAccumulator = func(accumulator, it)
        if (predicate(tempAccumulator)) {
            accumulator = tempAccumulator
            result.add(it)
        } else {
            break
        }
    }
    return AggregateResult(result, accumulator)
}

/**
 * Conditional take-until aggregated sum of a sequence
 */
fun <TSource, TAccumulate> Sequence<TSource>.takeUntilAggregate(
    seed: TAccumulate,
    func: (TAccumulate, TSource) -> TAccumulate,
    predicate: (TAccumulate) -> Boolean
): AggregateResult<TSource, TAccumulate> {
    var accumulator = seed
    val result = mutableListOf<TSource>()
    for(it in this) {
        result.add(it)
        accumulator = func(accumulator, it)
        if (predicate(accumulator)) {
            break
        }
    }
    return AggregateResult(result, accumulator)
}