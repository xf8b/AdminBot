package io.github.xf8b.adminbot.handler

import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent

class FakeSlapCommandHandler : AbstractCommandHandler(
        name = "\${prefix}slap",
        description = "Slaps the person.",
        commandType = CommandType.OTHER
) {
    override fun onCommandFired(event: CommandFiredEvent) {
    }
}
