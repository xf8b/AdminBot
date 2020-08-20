package io.github.xf8b.adminbot.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class ReadyListener {
    private final String botActivity;
    private final List<Snowflake> botAdmins;
    private final String botVersion;

    public void onReadyEvent(ReadyEvent event) {
        LOGGER.info("Successfully started AdminBot version {}!", botVersion);
        LOGGER.info("Logged in as {}#{}.", event.getSelf().getUsername(), event.getSelf().getDiscriminator());
        LOGGER.info("Logged into {} guilds.", event.getGuilds().size());
        LOGGER.info("Total shards: {}", event.getShardInfo().getCount());
        LOGGER.info("Bot arguments: ");
        LOGGER.info("Activity: {}", botActivity);
        LOGGER.info("Bot administrators: {}", botAdmins.stream()
                .map(Snowflake::asLong)
                .collect(Collectors.toUnmodifiableList()));
    }
}
