package io.github.xf8b.adminbot.util.parser

import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler

@FunctionalInterface
interface Parser<E> {
    fun parse(messageChannel: MessageChannel, commandHandler: AbstractCommandHandler, messageContent: String): Map<E, Any>?
}