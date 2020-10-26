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
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.arguments.Argument

class ArgumentCommandParser : CommandParser<Argument<*>> {
    /**
     * Parses the string and returns a [Result] that contains the [Map] of [Argument]s to their values.
     *
     * You should check if the result type is [Result.ResultType.SUCCESS] before getting the [Map].
     *
     * @param command the command to parse [Argument]s for
     * @param stringToParse the string to parse for [Argument]s
     * @return the [Result] of parsing the [Argument]s from [stringToParse]
     */
    override fun parse(command: AbstractCommand, stringToParse: String): Result<Map<Argument<*>, Any>> {
        val argumentMap: MutableMap<Argument<*>, Any> = HashMap()
        val missingArguments: MutableList<Argument<*>> = ArrayList()
        val invalidValues: MutableMap<Argument<*>, String> = HashMap()

        return Result.pass()
    }
}