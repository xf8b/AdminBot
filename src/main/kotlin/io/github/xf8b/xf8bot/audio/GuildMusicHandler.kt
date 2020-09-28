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

package io.github.xf8b.xf8bot.audio

import com.github.benmanes.caffeine.cache.Caffeine
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.xf8b.xf8bot.util.and
import java.util.concurrent.TimeUnit

class GuildMusicHandler(
        val guildId: Snowflake,
        val audioPlayerManager: AudioPlayerManager,
        var messageChannel: MessageChannel
) {
    private val audioPlayer: AudioPlayer = audioPlayerManager.createPlayer()
    val lavaPlayerAudioProvider: LavaPlayerAudioProvider = LavaPlayerAudioProvider(audioPlayer)
    private val scheduler: TrackScheduler = TrackScheduler(audioPlayer) {
        messageChannel.createMessage(it).subscribe()
    }

    init {
        audioPlayer.addListener(scheduler.createListener())
    }

    companion object {
        private val CACHE = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build<Triple<Snowflake, AudioPlayerManager, MessageChannel>, GuildMusicHandler> {
                    GuildMusicHandler(it.first, it.second, it.third)
                }

        @JvmStatic
        fun getMusicHandler(guildId: Snowflake, audioPlayerManager: AudioPlayerManager, messageChannel: MessageChannel): GuildMusicHandler =
                CACHE.get(guildId to audioPlayerManager and messageChannel)!!
                        .also { it.messageChannel = messageChannel }
    }

    /**
     * **Must be connected to a channel to use.**
     *
     * Plays the specified youtube video in the current voice channel.
     */
    fun playYoutubeVideo(identifier: String) {
        audioPlayerManager.loadItemOrdered(guildId, identifier, scheduler)
    }

    fun setVolume(volume: Int) {
        audioPlayer.volume = volume
    }

    fun isPaused() = audioPlayer.isPaused

    fun setPaused(paused: Boolean) {
        audioPlayer.isPaused = paused
    }

    fun stop() {
        audioPlayer.stopTrack()
    }
}