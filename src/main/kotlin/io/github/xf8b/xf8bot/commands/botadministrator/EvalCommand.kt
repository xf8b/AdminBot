/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.commands.botadministrator

import com.google.common.collect.ImmutableList.of
import com.google.common.collect.Range
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import javax.script.ScriptEngine
import javax.script.ScriptException

class EvalCommand : AbstractCommand(
    name = "\${prefix}eval",
    description = "Evaluates code. Bot administrators only!",
    commandType = CommandType.BOT_ADMINISTRATOR,
    aliases = of("\${prefix}evaluate"),
    minimumAmountOfArgs = 1,
    arguments = of(CODE_TO_EVAL),
    isBotAdministratorOnly = true
) {
    companion object {
        private val CODE_TO_EVAL = StringArgument(
            name = "code to evaluate",
            index = Range.atLeast(1)
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> = mono {
        val thingToEval = context.getValueOfArgument(CODE_TO_EVAL).get()
        val engine: ScriptEngine = GroovyScriptEngineImpl()
        engine.put("context", context)
        engine.put("guild", context.guild.awaitSingle())
        engine.put("channel", context.channel.awaitSingle())
        engine.put("member", context.member.get())
        engine.eval(
            """
            import discord4j.common.util.Snowflake;
            $thingToEval
            """.trimIndent()
        )?.toString() ?: "null result"
    }.flatMap { result ->
        context.message
            .channel
            .flatMap { it.createMessage("Result: $result") }
            .then()
    }.onErrorResume(ScriptException::class) { exception ->
        context.message
            .channel
            .flatMap { it.createMessage("ScriptException: $exception") }
            .then()
    }
}