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

import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

class BooleanFlag(
    override val shortName: String,
    override val longName: String,
    override val required: Boolean = false,
    override val requiresValue: Boolean = false,
    override val defaultValue: Supplier<out Boolean> = Supplier { true },
    override val parseFunction: Function<in String, out Boolean> = Function {
        it.toBoolean()
    },
    override val validityPredicate: Predicate<in String> = Predicate {
        it.equals("true", ignoreCase = true) || it.equals("false", ignoreCase = true)
    },
    override val invalidValueErrorMessageFunction: Function<in String, out String> = Function {
        Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE + " Valid values: `true`, `false`."
    }
) : Flag<Boolean> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BooleanFlag

        if (required != other.required) return false
        if (requiresValue != other.requiresValue) return false
        if (defaultValue != other.defaultValue) return false
        if (validityPredicate != other.validityPredicate) return false
        if (shortName != other.shortName) return false
        if (longName != other.longName) return false
        if (parseFunction != other.parseFunction) return false
        if (invalidValueErrorMessageFunction != other.invalidValueErrorMessageFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = required.hashCode()
        result = 31 * result + requiresValue.hashCode()
        result = 31 * result + defaultValue.hashCode()
        result = 31 * result + validityPredicate.hashCode()
        result = 31 * result + shortName.hashCode()
        result = 31 * result + longName.hashCode()
        result = 31 * result + parseFunction.hashCode()
        result = 31 * result + invalidValueErrorMessageFunction.hashCode()
        return result
    }

    override fun toString(): String = "BooleanFlag(" +
            "required=$required, " +
            "requiresValue=$requiresValue, " +
            "defaultValue=$defaultValue, " +
            "validityPredicate=$validityPredicate, " +
            "shortName='$shortName', " +
            "longName='$longName', " +
            "parseFunction=$parseFunction, " +
            "invalidValueErrorMessageFunction=$invalidValueErrorMessageFunction" +
            ")"
}