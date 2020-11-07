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

package io.github.xf8b.xf8bot.api.commands.parser

import com.google.common.collect.ImmutableMap
import io.github.xf8b.utils.optional.Result
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import net.jodah.typetools.TypeResolver

class FlagCommandParser : CommandParser<Flag<*>> {
    /**
     * Parses the string and returns a [Result] that contains the [Map] of [Flag]s to their values.
     *
     * You should check if the result type is [Result.ResultType.SUCCESS] before getting the [Map].
     *
     * @param command the command to parse [Flag]s for
     * @param toParse the string to parse for [Flag]s
     * @return the [Result] of parsing the [Flag]s from [toParse]
     */
    override fun parse(command: AbstractCommand, toParse: String): Result<Map<Flag<*>, Any>> {
        val flagMap: MutableMap<Flag<*>, Any> = HashMap()
        val missingFlags: MutableList<Flag<*>> = ArrayList()
        val invalidValues: MutableMap<Flag<*>, String> = HashMap()

        for (flag in command.flags) {
            val index = toParse.indexOf("--${flag.longName}").let { index ->
                if (index == -1) {
                    toParse.indexOf("-${flag.shortName}").let {
                        if (it != -1) it + "-${flag.shortName}".length
                        else it
                    }
                } else {
                    index + "--${flag.longName}".length
                }
            }

            if (index == -1) {
                if (flag.required) missingFlags.add(flag)
                continue
            } else {
                if (flag.requiresValue) {
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
                        missingFlags.add(flag)
                        continue
                    }
                } else {
                    flagMap[flag] = flag.defaultValue.get()
                }
            }
        }

        return when {
            // send failure when there are missing flags
            missingFlags.isNotEmpty() -> {
                val invalidFlagsNames = StringBuilder()
                missingFlags.forEach { flag ->
                    invalidFlagsNames.append("`").append(flag.shortName).append("`")
                        .append("/")
                        .append("`").append(flag.longName).append("`")
                        .append(" ")
                }
                Result.failure("Missing flag(s) ${invalidFlagsNames.toString().trim()}!")
            }

            // send failure when flag values are invalid
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = StringBuilder()
                invalidValues.forEach { (flag, invalidValue) ->
                    val requiredType = TypeResolver.resolveRawArgument(Flag::class.java, flag.javaClass).simpleName
                    invalidValuesFormatted.append("Flag: ")
                        .append("`").append(flag.shortName).append("`")
                        .append("/")
                        .append("`").append(flag.longName).append("`")
                        .append(" , Error message: ")
                        .append(
                            flag.getInvalidValueErrorMessage(invalidValue).format(
                                invalidValue.trim(),
                                requiredType
                            )
                        )
                        .append(" ")
                }
                Result.failure("Invalid value(s): $invalidValuesFormatted")
            }

            // send success when there are no errors and when everything is parsed
            else -> Result.success(ImmutableMap.copyOf(flagMap))
        }
    }

    /*
    override fun parse(command: AbstractCommand, stringToParse: String): Result<Map<Flag<*>, Any>> {
        val flagMap: MutableMap<Flag<*>, Any> = HashMap()
        val invalidFlags: MutableList<String> = ArrayList()
        val invalidValues: MutableMap<Flag<*>, Any> = HashMap()
        val missingFlags: MutableList<Flag<*>> = ArrayList()
        val matcher = Flag.REGEX.toPattern().matcher(stringToParse)

        while (matcher.find()) {
            val flagName = matcher.group(2)
            // find the flag that matches the name
            val flag: Flag<*>? = if (matcher.group(1) == "--") {
                command.flags.stream()
                        .filter(flagName::equals)
                        .findFirst()
                        .orElse(null)
            } else {
                command.flags.stream()
                        .filter(flagName::equals)
                        .findFirst()
                        .orElse(null)
            }
            // if no flag is found, add to invalid flags and continue
            if (flag == null) {
                invalidFlags.add(flagName)
                continue
            }
            val tempValue = matcher.group(3).trim()
            var value: Any
            // if value of flag is a string
            if (tempValue.matches("\"[\\w ]+\"".toRegex())) {
                // if flag does not take in a string, add to invalid values and continue
                if (TypeResolver.resolveRawArgument(Flag::class.java, flag.javaClass) == String::class.java) {
                    value = tempValue.substring(1, tempValue.length - 1)
                } else {
                    invalidValues[flag] = tempValue
                    continue
                }
            } else {
                // if value is valid, parse it
                // else add to invalid values and continue
                if (flag.isValidValue(tempValue)) {
                    value = flag.parse(tempValue)
                } else {
                    invalidValues[flag] = tempValue
                    continue
                }
            }
            // add to flag map the flag and the value
            flagMap[flag] = value
        }
        // if flag has not been parsed and is required, add to missing flags
        command.flags.forEach {
            if (!flagMap.containsKey(it)) {
                if (it.required) {
                    missingFlags.add(it)
                }
            }
        }
        return when {
            // send failure when there are missing flags
            missingFlags.isNotEmpty() -> {
                val invalidFlagsNames = StringBuilder()
                missingFlags.forEach(Consumer { flag: Flag<*> ->
                    invalidFlagsNames.append("`").append(flag.shortName).append("`")
                            .append("/")
                            .append("`").append(flag.longName).append("`")
                            .append(" ")
                })
                Result.failure("Missing flag(s) ${invalidFlagsNames.toString().trim()}!")
            }
            // send failure when flags are invalid
            invalidFlags.isNotEmpty() -> Result.failure("Invalid flag(s) `${invalidFlags.joinToString()}`!")
            // send failure when flag values are invalid
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = StringBuilder()
                invalidValues.forEach { (flag: Flag<*>, invalidValue: Any) ->
                    val clazz = TypeResolver.resolveRawArgument(Flag::class.java, flag.javaClass)
                    invalidValuesFormatted.append("Flag: ")
                            .append("`").append(flag.shortName).append("`")
                            .append("/")
                            .append("`").append(flag.longName).append("`")
                            .append(" , Error message: ")
                            .append(String.format(
                                    flag.getInvalidValueErrorMessage(invalidValue as String),
                                    invalidValue.trim { it <= ' ' },
                                    clazz.simpleName
                            ))
                            .append(" ")
                }
                Result.failure("Invalid value(s): $invalidValuesFormatted")
            }
            // send success when there are no errors and when everything is parsed
            else -> Result.success(ImmutableMap.copyOf(flagMap))
        }
    }
    */
}