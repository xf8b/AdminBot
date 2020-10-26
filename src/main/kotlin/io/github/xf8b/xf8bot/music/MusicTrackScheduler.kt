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

package io.github.xf8b.xf8bot.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.`object`.entity.channel.MessageChannel
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.reflect.KMutableProperty0


class MusicTrackScheduler(
    private val player: AudioPlayer,
    private val messageChannelProperty: KMutableProperty0<MessageChannel>,
    private val messageCallback: (Mono<*>) -> Unit
) : AudioLoadResultHandler {
    val queue: Queue<AudioTrack> = ConcurrentLinkedDeque()

    override fun trackLoaded(track: AudioTrack) {
        player.playTrack(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult) {
            playlist.tracks.forEach { queue.add(it) }
            if (player.playingTrack == null) {
                val track = queue.poll()
                if (track != null) player.playTrack(track)
            }
        }
    }

    fun playNextAudioTrack(amountToGoForward: Int) {
        for (i in 1..amountToGoForward) {
            val track = queue.poll()
            if (i == amountToGoForward) {
                if (track != null) {
                    player.playTrack(track)
                }
            }
        }
    }

    override fun noMatches() {
        messageCallback.invoke(
            messageChannelProperty.get()
                .createMessage("No results found!")
        )
    }

    override fun loadFailed(exception: FriendlyException) {
        messageCallback.invoke(
            messageChannelProperty.get()
                .createMessage("Could not parse audio: ${exception.severity} - ${exception.message}!")
        )
    }

    fun createListener(): MusicAudioPlayerListener =
        MusicAudioPlayerListener(this, messageChannelProperty, messageCallback)
}