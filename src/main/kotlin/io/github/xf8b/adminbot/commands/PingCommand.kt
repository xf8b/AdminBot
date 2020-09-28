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
import io.github.xf8b.adminbot.exceptions.ThisShouldNotHaveBeenThrownException
import reactor.core.publisher.Mono
import java.time.temporal.ChronoUnit

class PingCommand : AbstractCommand(
        name = "\${prefix}ping",
        description = "Gets the ping. Pretty useless.",
        commandType = CommandType.OTHER
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.channel
            .flatMap { it.createMessage("Getting ping...") }
            .flatMap { message ->
                val gatewayPing = event.client
                        .getGatewayClient(event.shardInfo.index)
                        .orElseThrow { ThisShouldNotHaveBeenThrownException() }
                        .responseTime
                        .toMillis()
                val ping = event.message.timestamp.until(message.timestamp, ChronoUnit.MILLIS)
                message.edit { it.setContent("Ping: ${ping}ms, Websocket: ${gatewayPing}ms") }
            }.then()
}