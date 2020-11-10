/*
 * Copyright (c) 2020 xf8b.
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

package io.github.xf8b.xf8bot.api.commands.flags

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class IntegerFlagTest {
    @Test
    fun `test integer flag validity check`() {
        val integerFlag = IntegerFlag(
            shortName = "i",
            longName = "integer"
        )
        assertFalse(integerFlag.isValidValue("beans")) {
            """Unexpected result of true from validity predicate for input "beans""""
        }
        assertFalse(integerFlag.isValidValue("\"2\"")) {
            """Unexpected result of true from validity predicate for input "2""""
        }
        assertTrue(integerFlag.isValidValue("2")) {
            "Unexpected result of false from validity predicate for input 2"
        }
        assertTrue(integerFlag.isValidValue("-105")) {
            "Unexpected result of false from validity predicate for input -105"
        }
    }
}

class TimeFlagTest {
    @Test
    fun `test time flag parse result`() {
        val timeFlag = TimeFlag(
            shortName = "t",
            longName = "time"
        )
        assertTrue(timeFlag.parse("2d").first == 2L) {
            """Unexpected failure of parsing "2d""""
        }
        assertTrue(timeFlag.parse("2min").second == TimeUnit.MINUTES) {
            "Unexpected failure of parsing '2min'"
        }
    }
}