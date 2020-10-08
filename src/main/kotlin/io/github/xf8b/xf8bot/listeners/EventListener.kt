package io.github.xf8b.xf8bot.listeners

import discord4j.core.event.domain.Event
import reactor.core.publisher.Mono

interface EventListener<E : Event> {
    fun onEventFired(event: E): Mono<E>
}
