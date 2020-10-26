package io.github.xf8b.xf8bot.commands.botadministrator

import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.micrometer.core.instrument.Metrics
import reactor.core.publisher.Mono

class ReactorMetricsCommand: AbstractCommand(
    name = "\${prefix}reactormetrics",
    description = "Gets the Reactor metrics. Bot administrators only!",
    commandType = CommandType.BOT_ADMINISTRATOR,
    aliases = "\${prefix}metrics".toSingletonImmutableList(),
    isBotAdministratorOnly = true
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
    }
}