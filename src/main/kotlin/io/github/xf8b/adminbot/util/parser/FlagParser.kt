package io.github.xf8b.adminbot.util.parser

import com.google.common.collect.ImmutableMap
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler
import io.github.xf8b.adminbot.api.commands.flags.Flag
import net.jodah.typetools.TypeResolver
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class FlagParser : Parser<Flag<*>> {
    override fun parse(messageChannel: MessageChannel, commandHandler: AbstractCommandHandler, messageContent: String): Map<Flag<*>, Any>? {
        val flagMap: MutableMap<Flag<*>, Any> = HashMap()
        val invalidFlags: MutableList<String> = ArrayList()
        val invalidValues: MutableMap<Flag<*>, Any> = HashMap()
        val missingFlags: MutableList<Flag<*>> = ArrayList()
        val matcher = Pattern.compile(Flag.REGEX).matcher(messageContent)
        while (matcher.find()) {
            val flagName = matcher.group(2)
            val flag: Flag<*>? = if (matcher.group(1) == "--") {
                commandHandler.flags
                        .stream()
                        .filter { it.longName() == flagName }
                        .findFirst()
                        .orElse(null)
            } else {
                commandHandler.flags
                        .stream()
                        .filter { it.shortName() == flagName }
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
        commandHandler.flags.forEach { flag: Flag<*> ->
            if (!flagMap.containsKey(flag)) {
                if (flag.isRequired) {
                    missingFlags.add(flag)
                }
            }
        }
        return when {
            missingFlags.isNotEmpty() -> {
                val invalidFlagsNames = StringBuilder()
                missingFlags.forEach(Consumer { flag: Flag<*> ->
                    invalidFlagsNames.append("`").append(flag.shortName()).append("`")
                            .append("/")
                            .append("`").append(flag.longName()).append("`")
                            .append(" ")
                })
                messageChannel.createMessage(String.format(
                        "Missing flag(s) %s!",
                        invalidFlagsNames.toString().trim { it <= ' ' }
                )).subscribe()
                null
            }
            invalidFlags.isNotEmpty() -> {
                messageChannel.createMessage(String.format("Invalid flag(s) `%s`!",
                        java.lang.String.join(", ", invalidFlags)))
                        .subscribe()
                null
            }
            invalidValues.isNotEmpty() -> {
                val invalidValuesFormatted = StringBuilder()
                invalidValues.forEach { (flag: Flag<*>, invalidValue: Any) ->
                    val clazz = TypeResolver.resolveRawArgument(Flag::class.java, flag.javaClass)
                    invalidValuesFormatted.append("Flag: ")
                            .append("`").append(flag.shortName()).append("`")
                            .append("/")
                            .append("`").append(flag.longName()).append("`")
                            .append(" , Error message: ")
                            .append(String.format(
                                    flag.getInvalidValueErrorMessage(invalidValue as String),
                                    invalidValue.trim { it <= ' ' },
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