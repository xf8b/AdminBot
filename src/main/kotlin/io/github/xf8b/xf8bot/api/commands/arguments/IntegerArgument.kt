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

package io.github.xf8b.xf8bot.api.commands.arguments

import com.google.common.collect.Range
import io.github.xf8b.xf8bot.util.functionReturning
import java.util.function.Function
import java.util.function.Predicate

class IntegerArgument(
    override val name: String,
    override val index: Range<Int>,
    override val required: Boolean = true,
    override val parseFunction: Function<in String, out Int> = DEFAULT_PARSE_FUNCTION,
    override val validityPredicate: Predicate<in String> = DEFAULT_VALIDITY_PREDICATE,
    override val errorMessageFunction: Function<in String, out String> = DEFAULT_INVALID_VALUE_ERROR_MESSAGE_FUNCTION,
) : Argument<Int> {
    companion object {
        private val DEFAULT_PARSE_FUNCTION: Function<in String, out Int> = Function { it.toInt() }
        private val DEFAULT_VALIDITY_PREDICATE: Predicate<in String> = Predicate { it.toIntOrNull() != null }
        private val DEFAULT_INVALID_VALUE_ERROR_MESSAGE_FUNCTION: Function<in String, out String> = functionReturning(
            Argument.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntegerArgument

        if (index != other.index) return false
        if (name != other.name) return false
        if (required != other.required) return false
        if (parseFunction != other.parseFunction) return false
        if (validityPredicate != other.validityPredicate) return false
        if (errorMessageFunction != other.errorMessageFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + required.hashCode()
        result = 31 * result + parseFunction.hashCode()
        result = 31 * result + validityPredicate.hashCode()
        result = 31 * result + errorMessageFunction.hashCode()
        return result
    }

    override fun toString(): String = "IntegerArgument(" +
            "index=$index, " +
            "name='$name', " +
            "required=$required, " +
            "parseFunction=$parseFunction, " +
            "validityPredicate=$validityPredicate, " +
            "errorMessageFunction=$errorMessageFunction" +
            ")"
}