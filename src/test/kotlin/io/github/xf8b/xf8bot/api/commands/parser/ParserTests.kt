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

package io.github.xf8b.xf8bot.api.commands.parser

import io.github.xf8b.utils.optional.Result
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.api.commands.flags.TimeFlag
import io.github.xf8b.xf8bot.util.toImmutableList
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import reactor.core.publisher.Mono

class FakeCommand : AbstractCommand(
    name = "fake command",
    description = "fake command",
    commandType = CommandType.OTHER,
    flags = (integerFlag to timeFlag and stringFlag).toImmutableList(),
) {
    companion object {
        val integerFlag = IntegerFlag(
            shortName = "n",
            longName = "number"
        )
        val timeFlag = TimeFlag(
            shortName = "t",
            longName = "time"
        )
        val stringFlag = StringFlag(
            shortName = "s",
            longName = "string"
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = error("how")
}

class FlagCommandParserTest {
    @Test
    fun `test flag parser`() {
        val flagParser = FlagCommandParser()
        val fakeCommand = FakeCommand()
        val result = flagParser.parse(fakeCommand, "-n 2 -t 2d --string \"hello\"")
        println(result.resultType)
        assertTrue(result.resultType == Result.ResultType.SUCCESS) {
            result.errorMessage
        }
        val flagMap = result.result!!
        assertTrue(flagMap[FakeCommand.integerFlag] as Int == 2) {
            "Unexpected value ${flagMap[FakeCommand.integerFlag]}"
        }
        assertTrue((flagMap[FakeCommand.timeFlag] as Pair<*, *>).first == 2L)  {
            "Unexpected value ${flagMap[FakeCommand.timeFlag]}"
        }
        assertTrue(flagMap[FakeCommand.stringFlag] as String == "hello") {
            "Unexpected value ${flagMap[FakeCommand.stringFlag]}"
        }
    }
}
