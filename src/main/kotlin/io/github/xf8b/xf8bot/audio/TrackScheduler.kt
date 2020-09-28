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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque


class TrackScheduler(
        private val player: AudioPlayer,
        private val errorCallback: (String) -> Unit
) : AudioLoadResultHandler {
    val queue: BlockingQueue<AudioTrack> = LinkedBlockingDeque()

    override fun trackLoaded(track: AudioTrack) {
        player.playTrack(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult) {
            val tracks = mutableListOf(*playlist.tracks.toTypedArray())
            player.playTrack(tracks[0])
            tracks.removeAt(0)
            tracks.forEach { queue.put(it) }
        }
    }

    override fun noMatches() {
        errorCallback.invoke("No results found!")
    }

    override fun loadFailed(exception: FriendlyException) {
        errorCallback.invoke("Could not parse audio: ${exception.severity} - ${exception.message}!")
    }

    fun createListener(): MusicAudioPlayerListener = MusicAudioPlayerListener(this, errorCallback)
}