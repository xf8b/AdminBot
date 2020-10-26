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

package io.github.xf8b.xf8bot.commands

import com.google.common.collect.ImmutableList.of
import com.google.common.collect.Range
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
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
        private val CODE_TO_EVAL = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("code to eval")
            .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = mono {
        val thingToEval = event.getValueOfArgument(CODE_TO_EVAL).get()
        val engine: ScriptEngine = ScriptEngineManager().getEngineByExtension("kts")
        try {
            engine.put("event", event)
            engine.put("guild", event.guild.awaitFirst())
            engine.put("channel", event.channel.awaitFirst())
            engine.put("member", event.member.get())
            val result = engine.eval(
                """
                import discord4j.common.util.Snowflake
                $thingToEval
                """.trimIndent()
            ).toString()
            event.message
                .channel
                .flatMap { it.createMessage("Result: $result") }
                .then()
                .awaitFirstOrNull()
        } catch (exception: ScriptException) {
            event.message
                .channel
                .flatMap { it.createMessage("ScriptException: $exception") }
                .then()
                .awaitFirstOrNull()
        }
    }
}