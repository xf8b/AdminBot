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

package io.github.xf8b.xf8bot.api.commands.arguments

import com.google.common.collect.Range
import java.util.function.Function
import java.util.function.Predicate

interface Argument<out T : Any> {
    val required: Boolean
    val index: Range<Int>
    val name: String
    val validityPredicate: Predicate<in String>
    val parseFunction: Function<in String, out T>
    val invalidValueErrorMessageFunction: Function<in String, out String>

    companion object {
        const val DEFAULT_INVALID_VALUE_ERROR_MESSAGE =
            "Invalid value `%s` for argument at index %s! Required value: %s."
    }

    /**
     * Checks if the value passed in is valid.
     *
     * Use this to send an error message from [getInvalidValueErrorMessage] if it is invalid!
     *
     * @param value the value to check if it is valid
     * @return if it is valid
     */
    fun isValidValue(value: String): Boolean = validityPredicate.test(value)

    /**
     * Parses the value and returns the value parsed.
     *
     * Value should be checked using [isValidValue] before using this.
     *
     * @param stringToParse the string to parse
     * @return the parsed value
     */
    fun parse(stringToParse: String): T = parseFunction.apply(stringToParse)

    /**
     * Gets the invalid value error message for the invalid value passed in.
     *
     * Defaults to [DEFAULT_INVALID_VALUE_ERROR_MESSAGE].
     *
     * @param invalidValue the invalid value to get the error message for
     * @return the invalid value error message
     */
    fun getInvalidValueErrorMessage(invalidValue: String): String = invalidValueErrorMessageFunction.apply(invalidValue)
}