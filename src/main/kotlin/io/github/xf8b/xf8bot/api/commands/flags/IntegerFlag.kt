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

package io.github.xf8b.xf8bot.api.commands.flags

import io.github.xf8b.xf8bot.util.functionReturning
import java.util.function.Function
import java.util.function.Predicate

class IntegerFlag(
    override val shortName: String,
    override val longName: String,
    override val required: Boolean = true,
    override val requiresValue: Boolean = true,
    override val defaultValue: Int? = null,
    override val validityPredicate: Predicate<in String> = Predicate { it.toIntOrNull() != null },
    override val parseFunction: Function<in String, out Int> = Function { it.toInt() },
    override val errorMessageFunction: Function<in String, out String> = functionReturning(Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE)
) : Flag<Int> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntegerFlag

        if (shortName != other.shortName) return false
        if (longName != other.longName) return false
        if (required != other.required) return false
        if (requiresValue != other.requiresValue) return false
        if (defaultValue != other.defaultValue) return false
        if (parseFunction != other.parseFunction) return false
        if (validityPredicate != other.validityPredicate) return false
        if (errorMessageFunction != other.errorMessageFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shortName.hashCode()
        result = 31 * result + longName.hashCode()
        result = 31 * result + required.hashCode()
        result = 31 * result + requiresValue.hashCode()
        result = 31 * result + (defaultValue ?: 0)
        result = 31 * result + parseFunction.hashCode()
        result = 31 * result + validityPredicate.hashCode()
        result = 31 * result + errorMessageFunction.hashCode()
        return result
    }

    override fun toString(): String = "IntegerFlag(" +
            "shortName='$shortName', " +
            "longName='$longName', " +
            "required=$required, " +
            "requiresValue=$requiresValue, " +
            "defaultValue=$defaultValue, " +
            "parseFunction=$parseFunction, " +
            "validityPredicate=$validityPredicate, " +
            "errorMessageFunction=$errorMessageFunction" +
            ")"
}