package io.github.xf8b.xf8bot.util

import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

@FunctionalInterface
class Filter<T>(private val decideClosure: (T) -> FilterReply) : Filter<T>() {
    /**
     * If the decision is `[FilterReply.DENY]`, then the event will be
     * dropped. If the decision is `[FilterReply.NEUTRAL]`, then the next
     * filter, if any, will be invoked. If the decision is
     * `[FilterReply.ACCEPT]` then the event will be logged without
     * consulting with other filters in the chain.
     *
     * @param event
     * The event to decide upon.
     */
    override fun decide(event: T): FilterReply = decideClosure.invoke(event)
}