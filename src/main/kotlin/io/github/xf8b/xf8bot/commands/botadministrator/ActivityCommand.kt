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