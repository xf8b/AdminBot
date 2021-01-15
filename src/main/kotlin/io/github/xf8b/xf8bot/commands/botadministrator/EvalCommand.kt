/*
 * Copyright (c) 2020, 2021 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.commands.botadministrator

import com.google.common.collect.Range
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.LoggerDelegate
import io.github.xf8b.xf8bot.util.set
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl
import reactor.core.publisher.Mono

class EvalCommand : Command(
    name = "\${prefix}eval",
    description = "Evaluates Groovy code. Bot administrators only!",
    commandType = CommandType.BOT_ADMINISTRATOR,
    aliases = "\${prefix}evaluate".toSingletonImmutableList(),
    arguments = CODE.toSingletonImmutableList(),
    botAdministratorOnly = true
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = mono {
        val code = event[CODE]!!
        val engine = GroovyScriptEngineImpl()

        engine["event"] = event
        engine["guild"] = event.guild.awaitSingle()
        engine["channel"] = event.channel.awaitSingle()
        engine["member"] = event.member.get()

        engine.eval(
            """
            import discord4j.common.util.Snowflake;
            $code
            """.trimIndent()
        )?.toString() ?: "no result"
    }
        .flatMap { result -> event.channel.flatMap { channel -> channel.createMessage("Result: $result") } }
        .doOnError { throwable -> LOGGER.error("Error happened during evaluation of code!", throwable) }
        .onErrorResume { throwable -> event.channel.flatMap { channel -> channel.createMessage("$throwable") } }
        .then()

    companion object {
        private val LOGGER by LoggerDelegate()

        private val CODE = StringArgument(
            name = "code",
            index = Range.atLeast(0)
        )
    }
}