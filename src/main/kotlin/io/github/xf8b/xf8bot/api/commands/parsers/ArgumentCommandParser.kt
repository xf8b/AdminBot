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

package io.github.xf8b.xf8bot.api.commands.parsers

import com.google.common.collect.ImmutableMap
import io.github.xf8b.utils.optional.Result
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import net.jodah.typetools.TypeResolver

class ArgumentCommandParser : CommandParser<Argument<*>> {
    /**
     * Parses the string and returns a [Result] that contains the [Map] of [Argument]s to their values.
     *
     * You should check if the result type is [Result.ResultType.SUCCESS] before getting the [Map].
     *
     * @param command the command to parse [Argument]s for
     * @param toParse the string to parse for [Argument]s
     * @return the [Result] of parsing the [Argument]s from [toParse]
     */
    override fun parse(command: AbstractCommand, toParse: String): Result<Map<Argument<*>, Any>> {
        val argumentMap: MutableMap<Argument<*>, Any> = HashMap()
        val missingArguments: MutableList<Argument<*>> = ArrayList()
        val invalidValues: MutableMap<Argument<*>, String> = HashMap()
        val toParseSplit = removeFlags(command.flags, toParse.split(" "))

        for (argument in command.arguments) {
            try {
                val collectedValue = StringBuilder()

                if (!argument.index.hasUpperBound()) {
                    val sublist = toParseSplit.subList(argument.index.lowerEndpoint(), toParseSplit.size)
                        .filter { it.isNotBlank() }

                    if (sublist.isEmpty() && argument.required) {
                        missingArguments.add(argument)
                        continue
                    } else {
                        collectedValue.append(sublist.joinToString(separator = " "))
                    }
                } else {
                    var i = argument.index.lowerEndpoint()

                    while (argument.index.contains(i)) {
                        collectedValue.append(toParseSplit[i]).append(" ")
                        i++
                    }
                }

                val finalCollectedValue = collectedValue.toString().trim()

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
                val invalidArgumentsNames = StringBuilder()

                for (argument in missingArguments) {
                    invalidArgumentsNames
                        .append("`").append(argument.name).append("`")
                        .append(" ")
                }

                Result.failure("Missing argument(s) ${invalidArgumentsNames.toString().trim()}!")
            }

            // send failure when argument values are invalid
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = StringBuilder()

                for ((argument, invalidValue) in invalidValues) {
                    val requiredType = TypeResolver.resolveRawArgument(
                        Argument::class.java,
                        argument.javaClass
                    ).simpleName
                    invalidValuesFormatted
                        .append("Argument: ")
                        .append("`").append(argument.name).append("`")
                        .append(", Error message: ")
                        .append(
                            argument.getErrorMessage(invalidValue).format(
                                invalidValue.trim(),
                                argument.index.toString(),
                                requiredType
                            )
                        )
                        .append(" ")
                }

                Result.failure("Invalid value(s): $invalidValuesFormatted")
            }

            // send success when there are no errors and when everything is parsed
            else -> Result.success(ImmutableMap.copyOf(argumentMap))
        }
    }

    fun removeFlags(flags: List<Flag<*>>, splitString: List<String>): List<String> {
        val splitStringCopy = splitString.toMutableList()

        for (flag in flags) {
            val index = if (splitStringCopy.contains("--${flag.longName}")) {
                splitStringCopy.indexOf("--${flag.longName}")
            } else if (splitStringCopy.contains("-${flag.shortName}")) {
                splitStringCopy.indexOf("-${flag.shortName}")
            } else {
                continue
            }

            splitStringCopy.removeAt(index)

            if (flag.requiresValue) {
                val valueOfFlag = splitStringCopy[index]

                if (valueOfFlag.startsWith('"')) {
                    val toSearch = splitStringCopy.subList(index, splitStringCopy.size)
                    val toRemove = toSearch.takeWhile {
                        !it.endsWith('"')
                    } + (toSearch.find { it.endsWith('"') } ?: '"')

                    for (i in 1..toRemove.size) {
                        splitStringCopy.removeAt(index)
                    }
                } else {
                    splitStringCopy.removeAt(index)
                }
            }
        }

        return splitStringCopy
    }
}