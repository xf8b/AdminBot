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

package io.github.xf8b.xf8bot.commands.botadministrator

import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.EnumFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.immutableListOf
import reactor.core.publisher.Mono
import java.util.*

class ActivityCommand : Command(
    name = "\${prefix}activity",
    description = "Sets the bot's activity",
    commandType = CommandType.BOT_ADMINISTRATOR,
    flags = immutableListOf(ACTIVITY_TYPE, CONTENT),
    botAdministratorOnly = true
) {
    enum class ActivityType {
        PLAYING,
        LISTENING,
        WATCHING,
        COMPETING;

        override fun toString() = name.toLowerCase(Locale.ROOT)
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val activityType = event[ACTIVITY_TYPE] ?: ActivityType.PLAYING
        val content = event[CONTENT]!!

        return when (activityType) {
            ActivityType.PLAYING -> event.client.updatePresence(Presence.online(Activity.playing(content)))
            ActivityType.LISTENING -> event.client.updatePresence(Presence.online(Activity.listening(content)))
            ActivityType.WATCHING -> event.client.updatePresence(Presence.online(Activity.watching(content)))
            ActivityType.COMPETING -> event.client.updatePresence(Presence.online(Activity.competing(content)))
        }.then(event.channel
            .flatMap { channel -> channel.createMessage("""Successfully set status to type $activityType with content "$content"!""") }
            .then())
    }

    companion object {
        private val ACTIVITY_TYPE = EnumFlag(
            shortName = "t",
            longName = "type",
            enumClass = ActivityType::class.java,
            required = false
        )

        private val CONTENT = StringFlag(
            shortName = "c",
            longName = "content"
        )
    }
}