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

package io.github.xf8b.xf8bot.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class ReadyListener {
    @NotNull
    private final String botActivity;
    @NotNull
    private final List<Snowflake> botAdmins;
    @NotNull
    private final String botVersion;

    public Mono<ReadyEvent> onReadyEvent(@NotNull ReadyEvent event) {
        return Mono.fromRunnable(() -> {
            LOGGER.info("Successfully started xf8bot version {}!", botVersion);
            LOGGER.info("Logged in as {}#{}.", event.getSelf().getUsername(), event.getSelf().getDiscriminator());
            LOGGER.info("Logged into {} guilds.", event.getGuilds().size());
            LOGGER.info("Total shards: {}", event.getShardInfo().getCount());
            LOGGER.info("Bot arguments: ");
            LOGGER.info("Activity: {}", botActivity);
            LOGGER.info("Bot administrators: {}", botAdmins.stream()
                    .map(Snowflake::asLong)
                    .collect(Collectors.toUnmodifiableList()));
        }).thenReturn(event);
    }
}
