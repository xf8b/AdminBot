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

import com.google.common.base.Predicates
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

class StringFlag(
    override val shortName: String,
    override val longName: String,
    override val required: Boolean = true,
    override val requiresValue: Boolean = true,
    override val defaultValue: Supplier<out String> = DEFAULT_DEFAULT_VALUE,
    override val parseFunction: Function<in String, out String> = DEFAULT_PARSE_FUNCTION,
    override val validityPredicate: Predicate<in String> = DEFAULT_VALIDITY_PREDICATE,
    override val invalidValueErrorMessageFunction: Function<in String, out String> =
        DEFAULT_INVALID_VALUE_ERROR_MESSAGE_FUNCTION
) : Flag<String> {
    companion object {
        @JvmStatic
        fun builder(): StringFlagBuilder = StringFlagBuilder()

        val DEFAULT_DEFAULT_VALUE: Supplier<out String> = Supplier { throw NoSuchElementException() }
        val DEFAULT_PARSE_FUNCTION: Function<in String, out String> = Function { it }
        val DEFAULT_VALIDITY_PREDICATE: Predicate<in String> = Predicates.alwaysTrue()
        val DEFAULT_INVALID_VALUE_ERROR_MESSAGE_FUNCTION: Function<in String, out String> = Function {
            Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
        }

        class StringFlagBuilder {
            private var shortName: String? = null
            private var longName: String? = null
            private var required = true
            private var requiresValue = true
            private var defaultValue: Supplier<out String> = DEFAULT_DEFAULT_VALUE
            private var parseFunction: Function<in String, out String> = DEFAULT_PARSE_FUNCTION
            private var validityPredicate: Predicate<in String> = DEFAULT_VALIDITY_PREDICATE
            private var invalidValueErrorMessageFunction: Function<in String, out String> =
                DEFAULT_INVALID_VALUE_ERROR_MESSAGE_FUNCTION

            fun setShortName(shortName: String) = apply {
                this.shortName = shortName
            }

            fun setLongName(longName: String) = apply {
                this.longName = longName
            }

            fun setNotRequired() = setRequired(false)

            private fun setRequired(required: Boolean) = apply {
                this.required = required
            }

            fun setRequiresValue(requiresValue: Boolean) = apply {
                this.requiresValue = requiresValue
            }

            fun setDefaultValue(defaultValue: Supplier<out String>) = apply {
                this.defaultValue = defaultValue
            }

            fun setParseFunction(parseFunction: Function<in String, out String>) = apply {
                this.parseFunction = parseFunction
            }

            fun setValidityPredicate(validityPredicate: Predicate<in String>) = apply {
                this.validityPredicate = validityPredicate
            }

            fun setInvalidValueErrorMessageFunction(function: Function<in String, out String>) = apply {
                this.invalidValueErrorMessageFunction = function
            }

            fun build(): StringFlag = StringFlag(
                shortName ?: throw NullPointerException("A short name is required!"),
                longName ?: throw NullPointerException("A long name is required!"),
                required,
                requiresValue,
                defaultValue,
                parseFunction,
                validityPredicate,
                invalidValueErrorMessageFunction
            )

            override fun toString(): String {
                return "StringFlagBuilder(" +
                        "shortName=$shortName, " +
                        "longName=$longName, " +
                        "required=$required, " +
                        "requiresValue=$requiresValue, " +
                        "defaultValue=$defaultValue, " +
                        "parseFunction=$parseFunction, " +
                        "validityPredicate=$validityPredicate, " +
                        "invalidValueErrorMessageFunction=$invalidValueErrorMessageFunction" +
                        ")"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StringFlag

        if (shortName != other.shortName) return false
        if (longName != other.longName) return false
        if (required != other.required) return false
        if (requiresValue != other.requiresValue) return false
        if (defaultValue != other.defaultValue) return false
        if (parseFunction != other.parseFunction) return false
        if (validityPredicate != other.validityPredicate) return false
        if (invalidValueErrorMessageFunction != other.invalidValueErrorMessageFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shortName.hashCode()
        result = 31 * result + longName.hashCode()
        result = 31 * result + required.hashCode()
        result = 31 * result + requiresValue.hashCode()
        result = 31 * result + defaultValue.hashCode()
        result = 31 * result + parseFunction.hashCode()
        result = 31 * result + validityPredicate.hashCode()
        result = 31 * result + invalidValueErrorMessageFunction.hashCode()
        return result
    }

    override fun toString(): String {
        return "StringFlag(" +
                "shortName='$shortName', " +
                "longName='$longName', " +
                "required=$required, " +
                "requiresValue=$requiresValue, " +
                "defaultValue=$defaultValue, " +
                "parseFunction=$parseFunction, " +
                "validityPredicate=$validityPredicate, " +
                "invalidValueErrorMessageFunction=$invalidValueErrorMessageFunction" +
                ")"
    }
}