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

package io.github.xf8b.xf8bot.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.util.Color
import io.github.xf8b.xf8bot.util.createEmbedDsl
import org.apache.commons.lang3.time.DurationFormatUtils
import reactor.core.publisher.Mono
import kotlin.reflect.KMutableProperty0

class MusicAudioPlayerListener(
    private val musicTrackScheduler: MusicTrackScheduler,
    private val messageChannelProperty: KMutableProperty0<MessageChannel>,
    private val messageCallback: (Mono<*>) -> Unit
) : AudioEventAdapter() {
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason == AudioTrackEndReason.FINISHED) {
            val queueTrack = musicTrackScheduler.queue.poll()
            if (queueTrack != null) player.playTrack(queueTrack)
        }
    }

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        messageCallback.invoke(messageChannelProperty.get().createEmbedDsl {
            title("Now Playing")
            url(track.info.uri)

            field("Title", track.info.title, inline = true)
            field("Length", DurationFormatUtils.formatDurationHMS(track.info.length), inline = true)
            field("Author", track.info.author, inline = true)

            color(Color.BLUE)
            timestamp()
        })
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        messageCallback.invoke(messageChannelProperty.get().createMessage("Track ${track.info.title} got stuck!"))
    }
}