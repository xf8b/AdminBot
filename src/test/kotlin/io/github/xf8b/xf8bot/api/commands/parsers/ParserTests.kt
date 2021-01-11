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

package io.github.xf8b.xf8bot.api.commands.parsers

import com.google.common.collect.Range
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.api.commands.flags.TimeFlag
import io.github.xf8b.xf8bot.util.toImmutableList
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

class FakeCommand : AbstractCommand(
    name = "fake command",
    description = "fake command",
    commandType = CommandType.OTHER,
    flags = (INTEGER_FLAG to TIME_FLAG and STRING_FLAG).toImmutableList(),
    arguments = (INTEGER_ARGUMENT to STRING_ARGUMENT).toImmutableList()
) {
    companion object {
        val INTEGER_FLAG = IntegerFlag(
            shortName = "n",
            longName = "number"
        )
        val TIME_FLAG = TimeFlag(
            shortName = "t",
            longName = "time"
        )
        val STRING_FLAG = StringFlag(
            shortName = "s",
            longName = "string"
        )
        val INTEGER_ARGUMENT = IntegerArgument(
            name = "integer arg",
            index = Range.singleton(0)
        )
        val STRING_ARGUMENT = StringArgument(
            name = "string arg",
            index = Range.atLeast(1)
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = error("how")
}

class FlagCommandInputParserTest {
    @Test
    fun `test flag parser`() {
        val flagParser = FlagCommandInputParser()
        val fakeCommand = FakeCommand()
        val result = flagParser.parse(
            fakeCommand,
            """-n 2 --string "459ad068-5355-4b2b-b080-54751138ddc2" -t 2d  --string ignored""""
        )
        println(result.resultType)
        assertTrue(result.isSuccess()) {
            "Unexpected result type ${result.resultType} - error: ${result.errorMessage}"
        }
        val flagMap = result.result!!
        assertTrue(flagMap[FakeCommand.INTEGER_FLAG] as Int == 2) {
            "Unexpected value ${flagMap[FakeCommand.INTEGER_FLAG]}, expected 2"
        }
        assertTrue((flagMap[FakeCommand.TIME_FLAG] as Pair<*, *>).first == 2L) {
            "Unexpected value ${flagMap[FakeCommand.TIME_FLAG]}, expected Pair.of(2L, TimeUnit.DAYS)"
        }
        assertTrue(flagMap[FakeCommand.STRING_FLAG] as String == "459ad068-5355-4b2b-b080-54751138ddc2") {
            """Unexpected value "${flagMap[FakeCommand.STRING_FLAG]}", expected "459ad068-5355-4b2b-b080-54751138ddc2""""
        }
    }
}

class ArgumentCommandInputParserTest {
    @Test
    fun `test argument parser`() {
        val argumentParser = ArgumentCommandInputParser()
        val fakeCommand = FakeCommand()
        val result = argumentParser.parse(
            fakeCommand,
            """23 hey beans bean bean hello --string ignored"""
        )
        println(result.resultType)
        assertTrue(result.isSuccess()) {
            "Unexpected result type ${result.resultType} - error: ${result.errorMessage}"
        }
        val argumentMap = result.result!!
        assertTrue(argumentMap[FakeCommand.INTEGER_ARGUMENT] as Int == 23) {
            "Unexpected value ${argumentMap[FakeCommand.INTEGER_ARGUMENT]}, expected 23"
        }
        assertTrue(argumentMap[FakeCommand.STRING_ARGUMENT] as String == "hey beans bean bean hello") {
            """Unexpected value ${argumentMap[FakeCommand.STRING_ARGUMENT]}, expected "hey beans bean bean hello""""
        }
    }
}