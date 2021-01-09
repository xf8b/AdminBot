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

import com.google.common.collect.ImmutableList.of
import com.google.common.collect.Range
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.LoggerDelegate
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl
import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import javax.script.ScriptEngine
import javax.script.ScriptException

class EvalCommand : AbstractCommand(
    name = "\${prefix}eval",
    description = "Evaluates Groovy code. Bot administrators only!",
    commandType = CommandType.BOT_ADMINISTRATOR,
    aliases = of("\${prefix}evaluate"),
    arguments = of(CODE_TO_EVAL),
    botAdministratorOnly = true
) {
    companion object {
        private val CODE_TO_EVAL = StringArgument(
            name = "code to evaluate",
            index = Range.atLeast(1)
        )
        private val LOGGER: Logger by LoggerDelegate()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = mono {
        val thingToEval = event.getValueOfArgument(CODE_TO_EVAL).get()
        val engine: ScriptEngine = GroovyScriptEngineImpl()
        engine.put("event", event)
        engine.put("guild", event.guild.awaitSingle())
        engine.put("channel", event.channel.awaitSingle())
        engine.put("member", event.member.get())
        engine.eval(
            """
            import discord4j.common.util.Snowflake;
            $thingToEval
            """.trimIndent()
        )?.toString() ?: "null result"
    }.flatMap { result ->
        event.message
            .channel
            .flatMap { it.createMessage("Result: $result") }
            .then()
    }.onErrorResume(ScriptException::class) { exception ->
        Mono.fromRunnable<Void> { LOGGER.error("Script exception happened during evaluation of code!", exception) }
            .then(event.message
                .channel
                .flatMap { it.createMessage("ScriptException: $exception") }
                .then())
    }.onErrorResume { throwable ->
        Mono.fromRunnable<Void> { LOGGER.error("Error happened during evaluation of code!", throwable) }
            .then(event.message
                .channel
                .flatMap { it.createMessage("${throwable::class.java.simpleName}: ${throwable.message}") }
                .then())
    }
}