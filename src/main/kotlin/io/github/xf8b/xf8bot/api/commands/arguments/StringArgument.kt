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

import com.google.common.base.Predicates
import com.google.common.collect.Range
import io.github.xf8b.xf8bot.util.functionReturning
import java.util.function.Function
import java.util.function.Predicate

class StringArgument(
    override val index: Range<Int>,
    override val name: String,
    override val required: Boolean = true,
    override val parseFunction: Function<in String, out String> = Function.identity(),
    override val validityPredicate: Predicate<in String> = Predicates.alwaysTrue(),
    override val errorMessageFunction: Function<in String, out String> = functionReturning(
        Argument.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
    )
) : Argument<String> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StringArgument

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

    override fun toString(): String = "StringArgument(index=$index, " +
            "name='$name', " +
            "required=$required, " +
            "parseFunction=$parseFunction, " +
            "validityPredicate=$validityPredicate, " +
            "errorMessageFunction=$errorMessageFunction" +
            ")"
}