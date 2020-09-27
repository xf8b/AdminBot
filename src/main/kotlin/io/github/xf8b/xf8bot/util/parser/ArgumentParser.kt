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
        val argumentMap: MutableMap<Argument<*>, Any> = HashMap()
        val invalidValues: MutableMap<Argument<*>, Any> = HashMap()
        val missingArguments: MutableList<Argument<*>> = ArrayList()
        val strings = stringToParse.replace(Flag.REGEX, "")
                .split(" ")
                .toTypedArray()
        val arguments = command.arguments
        arguments.forEach {
            try {
                //find string at the index of the argument
                val stringAtIndexOfArgument = StringBuilder()
                //if argument is Range#atLeast(Int) then take all the stuff after the lower bound
                if (!it.index.hasUpperBound()) {
                    stringAtIndexOfArgument.append(Arrays.stream(strings)
                            .skip(it.index.lowerEndpoint().toLong())
                            .collect(Collectors.joining(" ")))
                            .append(" ")
                } else {
                    //take the stuff in the range of the argument
                    var i = it.index.lowerEndpoint()
                    while (it.index.contains(i)) {
                        stringAtIndexOfArgument.append(strings[i]).append(" ")
                        i++
                    }
                }
                //if value is valid, add to argument map
                //else add to invalid values
                if (it.isValidValue(stringAtIndexOfArgument.toString().trim())) {
                    argumentMap[it] = it.parse(stringAtIndexOfArgument.toString().trim())
                } else {
                    invalidValues[it] = stringAtIndexOfArgument.toString().trim()
                }
            } catch (exception: IndexOutOfBoundsException) {
                //if stuff in argument range is out of bounds and it is required, add to missing arguments
                if (it.required) {
                    missingArguments.add(it)
                }
            }
        }
        return when {
            //send failure when there are missing arguments
            missingArguments.isNotEmpty() -> {
                val missingArgumentsIndexes = missingArguments.stream()
                        .map { it.index }
                        .map { it.toString() }
                        .collect(Collectors.toUnmodifiableList())
                Result.failure("Missing argument(s) at indexes ${missingArgumentsIndexes.joinToString()}!")
            }
            //send failure when there are invalid values
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = StringBuilder()
                invalidValues.forEach { (argument: Argument<*>, invalidValue: Any) ->
                    val clazz = TypeResolver.resolveRawArgument(Argument::class.java, argument.javaClass)
                    invalidValuesFormatted.append("Argument at index ")
                            .append(argument.index.toString())
                            .append(", Error message: ")
                            .append(String.format(
                                    argument.getInvalidValueErrorMessage(invalidValue as String),
                                    invalidValue.trim(),
                                    argument.index,
                                    clazz.simpleName
                            ))
                            .append(" ")
                }
                Result.failure("Invalid value(s): $invalidValuesFormatted")
            }
            //send success when everything has successfully parsed
            else -> Result.success(ImmutableMap.copyOf(argumentMap))
        }
    }
}