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

package io.github.xf8b.xf8bot.util.parser

import com.google.common.collect.ImmutableMap
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.util.Result
import net.jodah.typetools.TypeResolver
import java.util.*
import java.util.function.Consumer
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class FlagParser : Parser<Flag<*>> {
    override fun parse(command: AbstractCommand, stringToParse: String): Result<Map<Flag<*>, Any>> {
        val flagMap: MutableMap<Flag<*>, Any> = HashMap()
        val invalidFlags: MutableList<String> = ArrayList()
        val invalidValues: MutableMap<Flag<*>, Any> = HashMap()
        val missingFlags: MutableList<Flag<*>> = ArrayList()
        val matcher = Flag.REGEX.toPattern().matcher(stringToParse)
        while (matcher.find()) {
            val flagName = matcher.group(2)
            val flag: Flag<*>? = if (matcher.group(1) == "--") {
                command.flags
                        .stream()
                        .filter { it.longName == flagName }
                        .findFirst()
                        .orElse(null)
            } else {
                command.flags
                        .stream()
                        .filter { it.shortName == flagName }
                        .findFirst()
                        .orElse(null)
            }
            if (flag == null) {
                invalidFlags.add(flagName)
                break
            }
            val tempValue = matcher.group(3).trim { it <= ' ' }
            var value: Any
            if (tempValue.matches(Regex("\"[\\w ]+\""))) {
                if (TypeResolver.resolveRawArgument(Flag::class.java, flag.javaClass) == String::class.java) {
                    value = tempValue.substring(1, tempValue.length - 1)
                } else {
                    invalidValues[flag] = tempValue
                    break
                }
            } else {
                if (flag.isValidValue(tempValue)) {
                    value = flag.parse(tempValue)
                } else {
                    invalidValues[flag] = tempValue
                    break
                }
            }
            flagMap[flag] = value
        }
        command.flags.forEach { flag: Flag<*> ->
            if (!flagMap.containsKey(flag)) {
                if (flag.required) {
                    missingFlags.add(flag)
                }
            }
        }
        return when {
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
            invalidFlags.isNotEmpty() -> Result.failure("Invalid flag(s) `${invalidFlags.joinToString()}`!")
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
            else -> Result.success(ImmutableMap.copyOf(flagMap))
        }
    }
}