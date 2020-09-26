/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.commands

import io.github.xf8b.adminbot.api.commands.AbstractCommand
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent
import reactor.core.publisher.Mono
import kotlin.system.exitProcess

class ShutdownCommand : AbstractCommand(
        name = "\${prefix}shutdown",
        description = "Shuts down the bot. Bot administrators only!",
        commandType = CommandType.BOT_ADMINISTRATOR,
        isBotAdministratorOnly = true
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.channel
            .flatMap { it.createMessage("Shutting down!") }
            .doOnSuccess { exitProcess(0) }
            .then()
}