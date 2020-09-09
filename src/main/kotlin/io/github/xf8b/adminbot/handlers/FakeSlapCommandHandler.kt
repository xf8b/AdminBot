package io.github.xf8b.adminbot.handlers

import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent

//doesn't actually do anything, only exists because i want it to be in help command
class FakeSlapCommandHandler : AbstractCommandHandler(
        name = "\${prefix}slap",
        description = "Slaps the person.",
        commandType = CommandType.OTHER
) {
    override fun onCommandFired(event: CommandFiredEvent) {
    }
}
