package io.github.xf8b.adminbot.handlers;

import io.github.xf8b.adminbot.events.CommandFiredEvent;

public class RepeatNextMessageCommandHandler extends AbstractCommandHandler {
    public RepeatNextMessageCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}repeat")
                .setDescription("Repeats the next message. TODO: delete")
                .setCommandType(CommandType.OTHER));
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
