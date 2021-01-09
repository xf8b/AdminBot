/*
 * Copyright (c) 2020, 2021 xf8b.
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
import discord4j.core.event.ReactiveEventAdapter
import discord4j.core.event.domain.lifecycle.ReadyEvent
import io.github.xf8b.utils.semver.SemanticVersion
import io.github.xf8b.xf8bot.util.LoggerDelegate
import org.slf4j.Logger
import reactor.core.publisher.Mono

class ReadyListener(
    private val botActivity: String,
    private val botAdmins: List<Snowflake>,
    private val botVersion: SemanticVersion
) : ReactiveEventAdapter() {
    override fun onReady(event: ReadyEvent): Mono<Void> = Mono.fromRunnable {
        LOGGER.info("Successfully started xf8bot version ${botVersion.toStringVersion()}!")
        LOGGER.info("Logged in as ${event.self.tag}.")
        LOGGER.info("Logged into ${event.guilds.size} guilds.")
        LOGGER.info("Total shards: ${event.shardInfo.count}")
        LOGGER.info("Bot arguments: ")
        LOGGER.info("Activity: $botActivity")
        LOGGER.info("Bot administrators: ${botAdmins.map(Snowflake::asString)}")
    }

    companion object {
        private val LOGGER: Logger by LoggerDelegate()
    }
}