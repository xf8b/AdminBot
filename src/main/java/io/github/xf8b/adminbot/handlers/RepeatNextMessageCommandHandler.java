package io.github.xf8b.adminbot.handlers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;

public class RepeatNextMessageCommandHandler extends AbstractCommandHandler {
    public RepeatNextMessageCommandHandler() {
        super(
                "${prefix}repeat",
                "${prefix}repeat",
                "Repeats the next message. TODO: delete",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.OTHER,
                0,
                PermissionSet.none(),
                0
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        //TODO: fix
        /*
        event.getClient().on(MessageCreateEvent.class)
                .timeout(Duration.ofSeconds(2))
                .filterWhen(messageCreateEvent -> messageCreateEvent
                        .getMessage()
                        .getChannel()
                        .flux()
                        .any(messageChannel -> messageChannel == event.getChannel().block()))
                .flatMap(messageCreateEvent -> messageCreateEvent.getMessage().getChannel()
                        .flatMap(messageChannel -> messageChannel.createMessage(messageCreateEvent.getMessage()
                                .getContent())))
                .subscribe();
                */
        event.getChannel()
                .flatMap(messageChannel -> messageChannel.createMessage("Do not use this command!"))
                .subscribe();
    }
}
