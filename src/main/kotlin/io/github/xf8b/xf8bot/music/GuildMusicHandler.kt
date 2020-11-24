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

package io.github.xf8b.xf8bot.music

import com.github.benmanes.caffeine.cache.Caffeine
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.data.Cache
import io.github.xf8b.xf8bot.util.toMono
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

class GuildMusicHandler(
    val guildId: Snowflake,
    val audioPlayerManager: AudioPlayerManager,
    var messageChannel: MessageChannel
) {
    private val audioPlayer: AudioPlayer = audioPlayerManager.createPlayer()
    val musicTrackScheduler: MusicTrackScheduler = MusicTrackScheduler(audioPlayer, ::messageChannel) {
        it.subscribe()
    }
    val lavaPlayerAudioProvider: LavaPlayerAudioProvider = LavaPlayerAudioProvider(audioPlayer)
    val currentlyPlaying get() = audioPlayer.playingTrack.toMono()
    var paused: Boolean
        get() = audioPlayer.isPaused
        set(value) {
            audioPlayer.isPaused = value
        }
    var volume: Int
        get() = audioPlayer.volume
        set(value) {
            audioPlayer.volume = value
        }

    init {
        audioPlayer.addListener(musicTrackScheduler.createListener())
    }

    companion object GuildMusicHandlerCache :
        Cache<Triple<Snowflake, AudioPlayerManager, MessageChannel>, GuildMusicHandler> {
        private val CACHE = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build<Triple<Snowflake, AudioPlayerManager, MessageChannel>, GuildMusicHandler> {
                GuildMusicHandler(it.first, it.second, it.third)
            }

        fun get(guildId: Snowflake, audioPlayerManager: AudioPlayerManager, messageChannel: MessageChannel) =
            get(guildId to audioPlayerManager and messageChannel)

        override fun get(key: Triple<Snowflake, AudioPlayerManager, MessageChannel>): GuildMusicHandler =
            CACHE.get(key)!!.apply { messageChannel = key.third }
    }

    fun playYoutubeVideo(identifier: String): Mono<Void> = Mono.fromRunnable {
        audioPlayerManager.loadItemOrdered(guildId, identifier, musicTrackScheduler)
    }

    fun skip(amountToGoForward: Int): Mono<Void> = Mono.fromRunnable {
        musicTrackScheduler.playNextAudioTrack(amountToGoForward)
    }

    fun stop(): Mono<Void> = Mono.fromRunnable {
        audioPlayer.stopTrack()
    }
}