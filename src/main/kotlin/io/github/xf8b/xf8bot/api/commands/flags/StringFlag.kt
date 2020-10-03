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

class StringFlag(
        shortName: String?,
        longName: String?,
        requiresValue: Boolean,
        required: Boolean,
        parseFunction: Function<String, String>,
        validityPredicate: Predicate<String>,
        invalidValueErrorMessageFunction: Function<String, String>
) : Flag<String> {
    override val required: Boolean
    override val requiresValue: Boolean
    override val validityPredicate: Predicate<String>
    override val shortName: String
    override val longName: String
    override val parseFunction: Function<String, out String>
    override val invalidValueErrorMessageFunction: Function<String, String>

    init {
        if (shortName == null || longName == null) {
            throw NullPointerException("The short name and/or long name was not set!")
        }
        this.shortName = shortName
        this.longName = longName
        this.required = required
        this.requiresValue = requiresValue
        this.parseFunction = parseFunction
        this.validityPredicate = validityPredicate
        this.invalidValueErrorMessageFunction = invalidValueErrorMessageFunction
    }

    companion object {
        @JvmStatic
        fun builder(): StringFlagBuilder = StringFlagBuilder()

        class StringFlagBuilder {
            private var shortName: String? = null
            private var longName: String? = null
            private var requiresValue = true
            private var required = true
            private var parseFunction = Function { s: String -> s }
            private var validityPredicate = Predicate { _: String -> true }
            private var invalidValueErrorMessageFunction = Function { _: String -> Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE }

            fun setShortName(shortName: String): StringFlagBuilder = apply {
                this.shortName = shortName
            }

            fun setLongName(longName: String): StringFlagBuilder = apply {
                this.longName = longName
            }

            fun setRequired(required: Boolean): StringFlagBuilder = apply {
                this.required = required
            }

            fun setParseFunction(parseFunction: Function<String, String>): StringFlagBuilder = apply {
                this.parseFunction = parseFunction
            }

            fun setRequiresValue(requiresValue: Boolean): StringFlagBuilder = apply {
                this.requiresValue = requiresValue
            }

            fun setValidityPredicate(validityPredicate: Predicate<String>): StringFlagBuilder = apply {
                this.validityPredicate = validityPredicate
            }

            fun setInvalidValueErrorMessageFunction(invalidValueErrorMessageFunction: Function<String, String>): StringFlagBuilder = apply {
                this.invalidValueErrorMessageFunction = invalidValueErrorMessageFunction
            }

            fun build(): StringFlag = StringFlag(
                    shortName,
                    longName,
                    requiresValue,
                    required,
                    parseFunction,
                    validityPredicate,
                    invalidValueErrorMessageFunction
            )

            override fun toString(): String = "StringFlagBuilder(" +
                    "shortName=$shortName, " +
                    "longName=$longName, " +
                    "requiresValue=$requiresValue, " +
                    "required=$required, " +
                    "parseFunction=$parseFunction, " +
                    "validityPredicate=$validityPredicate, " +
                    "invalidValueErrorMessageFunction=$invalidValueErrorMessageFunction" +
                    ")"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StringFlag

        if (required != other.required) return false
        if (requiresValue != other.requiresValue) return false
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
        result = 31 * result + validityPredicate.hashCode()
        result = 31 * result + shortName.hashCode()
        result = 31 * result + longName.hashCode()
        result = 31 * result + parseFunction.hashCode()
        result = 31 * result + invalidValueErrorMessageFunction.hashCode()
        return result
    }

    override fun toString(): String {
        return "StringFlag(" +
                "required=$required, " +
                "requiresValue=$requiresValue, " +
                "validityPredicate=$validityPredicate, " +
                "shortName='$shortName', " +
                "longName='$longName', " +
                "parseFunction=$parseFunction, " +
                "invalidValueErrorMessageFunction=$invalidValueErrorMessageFunction" +
                ")"
    }
}