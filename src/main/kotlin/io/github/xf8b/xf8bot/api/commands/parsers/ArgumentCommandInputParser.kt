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

import com.google.common.collect.ImmutableMap
import io.github.xf8b.utils.optional.Result
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.util.resolveRawArgument

class ArgumentCommandInputParser : CommandInputParser<Argument<*>> {
    /**
     * Parses [input] for flags from [command] and returns a [Result] containing a map of [Argument]s to their values.
     * You must cast the value.
     *
     * You should check if the result type is [Result.ResultType.SUCCESS] before getting the [Map].
     */
    override fun parse(command: Command, input: String): Result<Map<Argument<*>, Any>> {
        val argumentMap = mutableMapOf<Argument<*>, Any>()
        val missingArguments = mutableListOf<Argument<*>>()
        val invalidValues = mutableMapOf<Argument<*>, String>()
        val cleanedInput = input.split(" ").toMutableList().apply { removeFlags(command.flags) }

        for (argument in command.arguments) {
            try {
                val collectedValue = if (!argument.index.hasUpperBound()) {
                    val sublist = cleanedInput.subList(argument.index.lowerEndpoint(), cleanedInput.size)
                        .filter(String::isNotBlank)

                    if (sublist.isEmpty() && argument.required) {
                        missingArguments.add(argument)
                        continue
                    } else {
                        sublist.joinToString(separator = " ")
                    }
                } else {
                    cleanedInput.subList(argument.index.lowerEndpoint(), argument.index.upperEndpoint() + 1)
                        .joinToString(separator = " ")
                }

                val finalCollectedValue = collectedValue.trim()

                if (finalCollectedValue.isEmpty()) throw IndexOutOfBoundsException() // to jump to the catch block

                if (argument.isValidValue(finalCollectedValue)) {
                    argumentMap[argument] = argument.parse(finalCollectedValue)
                } else {
                    invalidValues[argument] = finalCollectedValue
                }
            } catch (exception: IndexOutOfBoundsException) {
                if (argument.required) missingArguments.add(argument)
            }
        }

        return when {
            // send failure when there are missing arguments
            missingArguments.isNotEmpty() -> {
                val invalidArgumentsNames = missingArguments.joinToString { "`${it.name}`" }.trim()

                Result.failure("Missing argument(s) $invalidArgumentsNames!")
            }

            // send failure when argument values are invalid
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = invalidValues.map { (argument, invalidValue) ->
                    val requiredType = argument.javaClass.resolveRawArgument().simpleName
                    val errorMessage = argument.getErrorMessage(invalidValue)
                        .format(invalidValue.trim(), argument.index, requiredType)

                    "Argument: `${argument.name}`, Error message: $errorMessage"
                }.joinToString(separator = " ")

                Result.failure("Invalid value(s): $invalidValuesFormatted")
            }

            // send success when there are no errors and when everything is parsed
            else -> Result.success(ImmutableMap.copyOf(argumentMap))
        }
    }

    private fun MutableList<String>.removeFlags(flags: List<Flag<*>>) {
        flagLoop@ for (flag in flags) {
            val index = when {
                this.contains("--${flag.longName}") -> this.indexOf("--${flag.longName}")
                this.contains("-${flag.shortName}") -> this.indexOf("-${flag.shortName}")
                else -> continue@flagLoop
            }

            this.removeAt(index)

            if (flag.requiresValue) {
                try {
                    val valueOfFlag = this[index]

                    if (valueOfFlag.startsWith('"')) {
                        val toSearch = this.subList(index, this.size)
                        val toRemove = toSearch.takeWhile { !it.endsWith('"') }.toMutableList()
                        toSearch.find { it.endsWith('"') }?.let(toRemove::add)

                        for (i in 1..toRemove.size) this.removeAt(index)
                    } else {
                        this.removeAt(index)
                    }
                } catch (exception: IndexOutOfBoundsException) {
                    continue@flagLoop
                }
            }
        }
    }
}