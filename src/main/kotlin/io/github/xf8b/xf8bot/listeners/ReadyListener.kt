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

package io.github.xf8b.xf8bot.listeners

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.lifecycle.ReadyEvent
import io.github.xf8b.utils.semver.SemanticVersion
import io.github.xf8b.xf8bot.util.LoggerDelegate
import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.stream.Collectors

class ReadyListener(
    private val botActivity: String,
    private val botAdmins: List<Snowflake>,
    private val botVersion: SemanticVersion
) : EventListener<ReadyEvent> {
    private val logger: Logger by LoggerDelegate()

    override fun onEventFired(event: ReadyEvent): Mono<ReadyEvent> = event.toMono().doOnNext { readyEvent ->
        logger.info("Successfully started xf8bot version ${botVersion.toStringVersion()}!")
        logger.info("Logged in as ${readyEvent.self.username}#${readyEvent.self.discriminator}.")
        logger.info("Logged into ${readyEvent.guilds.size} guilds.")
        logger.info("Total shards: ${readyEvent.shardInfo.count}")
        logger.info("Bot arguments: ")
        logger.info("Activity: $botActivity")
        logger.info("Bot administrators: {}", botAdmins.stream()
            .map { it.asString() }
            .collect(Collectors.toUnmodifiableList()))
    }.thenReturn(event)
}