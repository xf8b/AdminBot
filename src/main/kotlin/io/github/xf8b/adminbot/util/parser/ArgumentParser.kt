package io.github.xf8b.adminbot.util.parser

import com.google.common.collect.ImmutableMap
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler
import io.github.xf8b.adminbot.api.commands.arguments.Argument
import io.github.xf8b.adminbot.api.commands.flags.Flag
import net.jodah.typetools.TypeResolver
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap

class ArgumentParser : Parser<Argument<*>> {
    override fun parse(messageChannel: MessageChannel, commandHandler: AbstractCommandHandler, messageContent: String): Map<Argument<*>, Any>? {
        val flagMap: MutableMap<Argument<*>, Any> = HashMap()
        val invalidValues: MutableMap<Argument<*>, Any> = HashMap()
        val missingArguments: MutableList<Argument<*>> = ArrayList()
        val strings = messageContent.replace(Flag.REGEX.toRegex(), "")
                .split(" ")
                .toTypedArray()
        val arguments = commandHandler.arguments
        arguments.forEach { argument: Argument<*> ->
            try {
                val stringAtIndexOfArgument = StringBuilder()
                //should be fixed
                //todo fix if it breaks
                if (!argument.index().hasUpperBound()) {
                    stringAtIndexOfArgument.append(Arrays.stream(strings)
                            .skip(argument.index().lowerEndpoint().toLong())
                            .collect(Collectors.joining(" ")))
                            .append(" ")
                } else {
                    var i = argument.index().lowerEndpoint()
                    while (argument.index().contains(i)) {
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
                if (argument.isRequired) {
                    missingArguments.add(argument)
                }
            }
        }
        return when {
            missingArguments.isNotEmpty() -> {
                val missingArgumentsIndexes = missingArguments.stream()
                        .map { it.index() }
                        .map { it.toString() }
                        .collect(Collectors.toUnmodifiableList())
                messageChannel.createMessage(String.format("Missing argument(s) at indexes %s!", java.lang.String.join(", ", missingArgumentsIndexes)))
                        .subscribe()
                null
            }
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = StringBuilder()
                invalidValues.forEach { (argument: Argument<*>, invalidValue: Any?) ->
                    val clazz = TypeResolver.resolveRawArgument(Argument::class.java, argument.javaClass)
                    invalidValuesFormatted.append("Argument at index ")
                            .append(argument.index().toString())
                            .append(", Error message: ")
                            .append(String.format(
                                    argument.getInvalidValueErrorMessage(invalidValue as String),
                                    invalidValue.trim { it <= ' ' },
                                    argument.index(),
                                    clazz.simpleName
                            ))
                            .append(" ")
                }
                messageChannel.createMessage(String.format("Invalid value(s): %s", invalidValuesFormatted.toString()))
                        .subscribe()
                null
            }
            else -> ImmutableMap.copyOf(flagMap)
        }
    }
}