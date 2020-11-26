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
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import net.jodah.typetools.TypeResolver

class FlagCommandParser : CommandParser<Flag<*>> {
    /**
     * Parses [toParse] for flags from [command] and returns a [Result] containing a map of [Flag]s to their values.
     * You must cast the value.
     *
     * You should check if the result type is [Result.ResultType.SUCCESS] before getting the [Map].
     */
    override fun parse(command: AbstractCommand, toParse: String): Result<Map<Flag<*>, Any>> {
        val flagMap: MutableMap<Flag<*>, Any> = HashMap()
        val missingFlags: MutableList<Flag<*>> = ArrayList()
        val invalidValues: MutableMap<Flag<*>, String> = HashMap()

        // TODO make new parser, maybe using String#split? not sure how to do it though

        for (flag in command.flags) {
            val index = toParse.indexOf("--${flag.longName}")
                .takeUnless((-1)::equals)
                ?.let { it + "--${flag.longName}".length }
                ?: toParse.indexOf("-${flag.shortName}")
                    .takeUnless((-1)::equals)
                    ?.let { it + "-${flag.shortName}".length }

            if (index == null) {
                if (flag.required) missingFlags.add(flag)
                continue
            } else {
                var i = index
                var amountOfQuotesSeen = 0
                var farEnough = false
                var collectedValue = ""
                parseValue@ while (true) {
                    try {
                        when (val char = toParse[i]) {
                            ' ', '=' -> {
                                if (farEnough) {
                                    collectedValue += char
                                }
                            }

                            '"' -> {
                                if (!farEnough) {
                                    if (amountOfQuotesSeen != 2) {
                                        amountOfQuotesSeen++
                                    } else {
                                        break@parseValue
                                    }
                                } else {
                                    if (amountOfQuotesSeen == 1) {
                                        break@parseValue
                                    }
                                    collectedValue += char
                                }
                            }

                            '-' -> {
                                if (amountOfQuotesSeen != 0) {
                                    collectedValue += char
                                } else {
                                    break@parseValue
                                }
                            }

                            else -> {
                                farEnough = true
                                collectedValue += char
                            }
                        }
                        i++
                    } catch (exception: IndexOutOfBoundsException) {
                        break@parseValue
                    }
                }

                collectedValue = collectedValue.trim()

                if (collectedValue.isNotEmpty()) {
                    if (collectedValue[0] == '"' && collectedValue[collectedValue.length - 1] == '=') {
                        collectedValue = collectedValue.substring(1, collectedValue.length - 1)
                    }

                    if (flag.isValidValue(collectedValue)) {
                        flagMap[flag] = flag.parse(collectedValue)
                    } else {
                        invalidValues[flag] = collectedValue
                    }
                } else {
                    if (flag.requiresValue) missingFlags.add(flag)
                    else flagMap[flag] = flag.defaultValue
                        ?: return Result.failure("Flag ${flag.shortName}/${flag.longName} requires a value.")
                    continue
                }
            }
        }

        return when {
            // send failure when there are missing flags
            missingFlags.isNotEmpty() -> {
                val invalidFlagsNames = missingFlags.joinToString(separator = " ") {
                    "`${it.shortName}`/`${it.longName}`"
                }

                Result.failure("Missing flag(s) ${invalidFlagsNames}!")
            }

            // send failure when flag values are invalid
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = invalidValues.map { (flag, value) ->
                    val requiredType = TypeResolver.resolveRawArgument(Flag::class.java, flag.javaClass).simpleName
                    val errorMessage = flag.getErrorMessage(value).format(value.trim(), requiredType)

                    "Flag: `${flag.shortName}`/`${flag.longName}`, Error message: $errorMessage"
                }.joinToString(separator = " ")

                Result.failure("Invalid value(s): $invalidValuesFormatted")
            }

            // send success when there are no errors and when everything is parsed
            else -> Result.success(ImmutableMap.copyOf(flagMap))
        }
    }
}