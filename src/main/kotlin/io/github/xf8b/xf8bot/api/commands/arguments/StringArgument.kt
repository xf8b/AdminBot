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

class StringArgument(
        index: Range<Int>?,
        name: String?,
        required: Boolean,
        parseFunction: Function<String, String>,
        validityPredicate: Predicate<String>,
        invalidValueErrorMessageFunction: Function<String, String>
) : Argument<String> {
    override val index: Range<Int>
    override val name: String
    override val required: Boolean
    override val parseFunction: Function<String, String>
    override val validityPredicate: Predicate<String>
    override val invalidValueErrorMessageFunction: Function<String, String>

    init {
        if (index == null || name == null) {
            throw NullPointerException("You are missing a index or name!")
        }
        this.index = index
        this.name = name
        this.required = required
        this.parseFunction = parseFunction
        this.validityPredicate = validityPredicate
        this.invalidValueErrorMessageFunction = invalidValueErrorMessageFunction
    }

    companion object {
        @JvmStatic
        fun builder(): StringArgumentBuilder = StringArgumentBuilder()
    }

    class StringArgumentBuilder {
        private var index: Range<Int>? = null
        private var name: String? = null
        private var required = true
        private var parseFunction = Function { stringToParse: String -> stringToParse }
        private var validityPredicate = Predicate { _: String -> true }
        private var invalidValueErrorMessageFunction = Function { _: String -> Argument.DEFAULT_INVALID_VALUE_ERROR_MESSAGE }

        fun setIndex(index: Range<Int>): StringArgumentBuilder = apply {
            this.index = index
        }

        fun setName(name: String): StringArgumentBuilder = apply {
            this.name = name
        }

        fun setRequired(required: Boolean): StringArgumentBuilder = apply {
            this.required = required
        }

        fun setParseFunction(parseFunction: Function<String, String>): StringArgumentBuilder = apply {
            this.parseFunction = parseFunction
        }

        fun setValidityPredicate(validityPredicate: Predicate<String>): StringArgumentBuilder = apply {
            this.validityPredicate = validityPredicate
        }

        fun setInvalidValueErrorMessageFunction(invalidValueErrorMessageFunction: Function<String, String>): StringArgumentBuilder = apply {
            this.invalidValueErrorMessageFunction = invalidValueErrorMessageFunction
        }

        fun build(): StringArgument = StringArgument(
                index,
                name,
                required,
                parseFunction,
                validityPredicate,
                invalidValueErrorMessageFunction
        )

        override fun toString(): String {
            return "StringArgument.StringArgumentBuilder(" +
                    "index=$index, " +
                    "name=$name, " +
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

        other as StringArgument

        if (index != other.index) return false
        if (name != other.name) return false
        if (required != other.required) return false
        if (parseFunction != other.parseFunction) return false
        if (validityPredicate != other.validityPredicate) return false
        if (invalidValueErrorMessageFunction != other.invalidValueErrorMessageFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + required.hashCode()
        result = 31 * result + parseFunction.hashCode()
        result = 31 * result + validityPredicate.hashCode()
        result = 31 * result + invalidValueErrorMessageFunction.hashCode()
        return result
    }

    override fun toString(): String {
        return "StringArgument(index=$index, " +
                "name='$name', " +
                "required=$required, " +
                "parseFunction=$parseFunction, " +
                "validityPredicate=$validityPredicate, " +
                "invalidValueErrorMessageFunction=$invalidValueErrorMessageFunction" +
                ")"
    }
}