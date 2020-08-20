package io.github.xf8b.adminbot.events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.AdminBot;
import lombok.Getter;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class CommandFiredEvent extends MessageCreateEvent {
    @Getter
    private final AdminBot adminBot;

    public CommandFiredEvent(AdminBot adminBot, MessageCreateEvent event) {
        super(event.getClient(),
                event.getShardInfo(),
                event.getMessage(),
                event.getGuildId().map(Snowflake::asLong).orElse(null),
                event.getMember().orElse(null));
        this.adminBot = adminBot;
    }

    @Override
    public String toString() {
        return "CommandFiredEvent{" +
                "message=" + getMessage() +
                ", channel=" + getChannel() +
                ", author=" + getAuthor() +
                ", member=" + getMember() +
                ", guildId=" + getGuildId() +
                " adminBot=" + adminBot +
                "}";
    }

    public Mono<MessageChannel> getChannel() {
        return getMessage().getChannel();
    }

    public Optional<User> getAuthor() {
        return getMessage().getAuthor();
    }
}
