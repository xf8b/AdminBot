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

import io.github.xf8b.utils.optional.Result
import io.github.xf8b.xf8bot.api.commands.Command

fun interface CommandInputParser<E> {
    /**
     * Parses [input] for flags from [command] and returns a [Result] containing a map of [E]s to their values.
     * You must cast the value.
     *
     * You should check if the result type is [Result.ResultType.SUCCESS] before getting the [Map].
     */
    fun parse(command: Command, input: String): Result<Map<E, Any>>
}