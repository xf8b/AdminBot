package io.github.xf8b.xf8bot.api.commands

import reactor.core.publisher.Mono

interface Bot {
    fun start(): Mono<Void>
}