package io.github.xf8b.xf8bot.commands

import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.util.pagination.createPaginatedEmbed
import reactor.core.publisher.Mono

class TestPaginatedCommand : Command(
    name = "paginated embed",
    description = "ok",
    commandType = CommandType.OTHER
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.channel.flatMap { channel ->
        channel.createPaginatedEmbed(
            {
                title("hello")
                description("beans")
            },
            {
                title("how is your day")
                description("ok")
            },
            {
                title("good night")
                description("gn")
            }
        )
    }
}