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

package io.github.xf8b.xf8bot.listeners

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.lifecycle.ReadyEvent
import io.github.xf8b.xf8bot.util.LoggerDelegate
import org.slf4j.Logger
import reactor.core.publisher.Mono
import java.util.stream.Collectors

class ReadyListener(
    private val botActivity: String,
    private val botAdmins: List<Snowflake>,
    private val botVersion: String
) : EventListener<ReadyEvent> {
    private val logger: Logger by LoggerDelegate()

    override fun onEventFired(event: ReadyEvent): Mono<ReadyEvent> = Mono.fromRunnable<Void> {
        logger.info("Successfully started xf8bot version $botVersion!")
        logger.info("Logged in as ${event.self.username}#${event.self.discriminator}.")
        logger.info("Logged into ${event.guilds.size} guilds.")
        logger.info("Total shards: ${event.shardInfo.count}")
        logger.info("Bot arguments: ")
        logger.info("Activity: $botActivity")
        logger.info("Bot administrators: {}", botAdmins.stream()
            .map { it.asString() }
            .collect(Collectors.toUnmodifiableList()))
    }.thenReturn(event)
}