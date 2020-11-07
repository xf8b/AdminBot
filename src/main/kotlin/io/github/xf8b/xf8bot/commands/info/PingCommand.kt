/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Modifications copyright (c) 2020 xf8b
 * Changed to be in Kotlin, and not use JDA-Utilities or JDA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.xf8b.xf8bot.commands.info

import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import reactor.core.publisher.Mono
import java.time.temporal.ChronoUnit

class PingCommand : AbstractCommand(
    name = "\${prefix}ping",
    description = "Gets the ping. Pretty useless.",
    commandType = CommandType.INFO
) {
    override fun onCommandFired(context: CommandFiredContext): Mono<Void> = context.channel
        .flatMap { it.createMessage("Getting ping...") }
        .flatMap { message ->
            val gatewayPing = context.client
                .getGatewayClient(context.shardInfo.index)
                .orElseThrow { ThisShouldNotHaveBeenThrownException() }
                .responseTime
                .toMillis()
            val ping = context.message.timestamp.until(message.timestamp, ChronoUnit.MILLIS)
            message.edit { it.setContent("Ping: ${ping}ms, Websocket: ${gatewayPing}ms") }
        }.then()
}