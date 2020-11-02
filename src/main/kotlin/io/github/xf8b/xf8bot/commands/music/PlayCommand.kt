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

package io.github.xf8b.xf8bot.commands.music

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.core.`object`.VoiceState
import discord4j.core.`object`.entity.Member
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.music.GuildMusicHandler
import reactor.core.publisher.Mono

class PlayCommand : AbstractCommand(
    name = "\${prefix}play",
    description = "Plays the specified music in the current VC.",
    commandType = CommandType.MUSIC,
    minimumAmountOfArgs = 1,
    arguments = ImmutableList.of(YOUTUBE_VIDEO_NAME_OR_LINK)
) {
    companion object {
        private val YOUTUBE_VIDEO_NAME_OR_LINK = StringArgument.builder()
            .setName("youtube video name or link")
            .setIndex(Range.atLeast(1))
            .build()
        private const val YOUTUBE_URL_REGEX = "https://(www\\.)?youtube\\.com/watch\\?v=.+"
        private const val OTHER_YOUTUBE_URL_REGEX = "https://(www\\.)?youtu\\.be/watch\\?v=.+"
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val guildId = event.guild.map { it.id }.block()!!
        val guildMusicHandler = GuildMusicHandler.getMusicHandler(
            guildId,
            event.xf8bot.audioPlayerManager,
            event.channel.block()!!
        )
        val temp = event.getValueOfArgument(YOUTUBE_VIDEO_NAME_OR_LINK).get()
        val videoUrlOrSearch: String = when {
            temp.matches(YOUTUBE_URL_REGEX.toRegex()) -> temp
            temp.matches(OTHER_YOUTUBE_URL_REGEX.toRegex()) -> temp
            else -> "ytsearch: $temp"
        }
        val playMono: Mono<Void> = guildMusicHandler.playYoutubeVideo(videoUrlOrSearch)
        return event.client.voiceConnectionRegistry.getVoiceConnection(guildId)
            .flatMap { playMono }
            .switchIfEmpty(Mono.justOrEmpty(event.member)
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap { voiceChannel ->
                    voiceChannel.join { spec ->
                        spec.setProvider(guildMusicHandler.lavaPlayerAudioProvider)
                    }.then(event.channel.flatMap {
                        it.createMessage("Successfully connected to your VC!")
                    }).flatMap { playMono }
                }.switchIfEmpty(event.channel.flatMap {
                    it.createMessage("You are not in a VC!")
                }.then())
            )
    }
}