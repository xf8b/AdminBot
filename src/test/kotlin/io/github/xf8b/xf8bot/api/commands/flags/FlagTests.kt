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

package io.github.xf8b.xf8bot.api.commands.flags

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.concurrent.TimeUnit


class FlagTest {
    @Test
    fun `test flag regex`() {
        val flagRegex = Flag.REGEX
        assertTrue(flagRegex.matches("-c blue"))
        assertTrue(flagRegex.matches("-f \"beans\""))
        assertTrue(flagRegex.matches("-m = \"bruhman\""))
        assertTrue(flagRegex.matches("--red=true"))
    }
}

class IntegerFlagTest {
    @Test
    fun `test integer flag validity check`() {
        val integerFlag = IntegerFlag.builder()
                .setShortName("i")
                .setLongName("integer")
                .build();
        assertFalse(integerFlag.isValidValue("beans"))
        assertFalse(integerFlag.isValidValue("\"2\""))
    }
}

class TimeFlagTest {
    @Test
    fun `test time flag parse result`() {
        val timeFlag = TimeFlag.builder()
                .setShortName("t")
                .setLongName("time")
                .build()
        assertTrue {
            timeFlag.parse("2d").first == 2L
        }
        assertTrue {
            timeFlag.parse("2min").second == TimeUnit.MINUTES
        }
    }
}