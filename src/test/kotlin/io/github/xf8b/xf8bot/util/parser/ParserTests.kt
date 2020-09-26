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

package io.github.xf8b.xf8bot.util.parser

import com.google.common.collect.ImmutableList
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.TimeFlag
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import reactor.core.publisher.Mono

class FakeCommandHandler : AbstractCommand(
        name = "fake command",
        description = "fake command",
        commandType = CommandType.OTHER,
        flags = ImmutableList.of(integerFlag, timeFlag),
) {
    companion object {
        val integerFlag = IntegerFlag.builder()
                .setShortName("n")
                .setLongName("number")
                .build()
        val timeFlag = TimeFlag.builder()
                .setShortName("t")
                .setLongName("time")
                .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = error("how")
}

class FlagParserTest {
    @Test
    fun `test flag parser`() {
        val flagParser = FlagParser()
        val fakeCommandHandler = FakeCommandHandler()
        val flagMap = flagParser.parse(fakeCommandHandler, "-n 2 -t 2d").result!!
        assertTrue(flagMap[FakeCommandHandler.integerFlag] as Int == 2)
        assertTrue((flagMap[FakeCommandHandler.timeFlag] as Pair<*, *>).first == 2L)
    }
}

