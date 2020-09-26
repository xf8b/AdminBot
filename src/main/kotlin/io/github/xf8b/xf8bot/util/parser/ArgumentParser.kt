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
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.util.Result
import net.jodah.typetools.TypeResolver
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap

class ArgumentParser : Parser<Argument<*>> {
    override fun parse(command: AbstractCommand, stringToParse: String): Result<Map<Argument<*>, Any>> {
        val flagMap: MutableMap<Argument<*>, Any> = HashMap()
        val invalidValues: MutableMap<Argument<*>, Any> = HashMap()
        val missingArguments: MutableList<Argument<*>> = ArrayList()
        val strings = stringToParse.replace(Flag.REGEX, "")
                .split(" ")
                .toTypedArray()
        val arguments = command.arguments
        arguments.forEach { argument: Argument<*> ->
            try {
                val stringAtIndexOfArgument = StringBuilder()
                //should be fixed
                //todo fix if it breaks
                if (!argument.index.hasUpperBound()) {
                    stringAtIndexOfArgument.append(Arrays.stream(strings)
                            .skip(argument.index.lowerEndpoint().toLong())
                            .collect(Collectors.joining(" ")))
                            .append(" ")
                } else {
                    var i = argument.index.lowerEndpoint()
                    while (argument.index.contains(i)) {
                        stringAtIndexOfArgument.append(strings[i]).append(" ")
                        i++
                    }
                }
                if (argument.isValidValue(stringAtIndexOfArgument.toString().trim())) {
                    flagMap[argument] = argument.parse(stringAtIndexOfArgument.toString().trim())
                } else {
                    invalidValues[argument] = stringAtIndexOfArgument.toString().trim()
                }
            } catch (exception: IndexOutOfBoundsException) {
                if (argument.required) {
                    missingArguments.add(argument)
                }
            }
        }
        return when {
            missingArguments.isNotEmpty() -> {
                val missingArgumentsIndexes = missingArguments.stream()
                        .map { it.index }
                        .map { it.toString() }
                        .collect(Collectors.toUnmodifiableList())
                Result.failure("Missing argument(s) at indexes ${java.lang.String.join(", ", missingArgumentsIndexes)}!")
            }
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = StringBuilder()
                invalidValues.forEach { (argument: Argument<*>, invalidValue: Any?) ->
                    val clazz = TypeResolver.resolveRawArgument(Argument::class.java, argument.javaClass)
                    invalidValuesFormatted.append("Argument at index ")
                            .append(argument.index.toString())
                            .append(", Error message: ")
                            .append(String.format(
                                    argument.getInvalidValueErrorMessage(invalidValue as String),
                                    invalidValue.trim { it <= ' ' },
                                    argument.index,
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