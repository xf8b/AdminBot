package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class ClearCommandHandler extends CommandHandler {
    public ClearCommandHandler() {
        super(
                "${prefix}clear",
                "${prefix}clear <amount>",
                "Clears the specified amount of messages. The amount of messages to be cleared cannot exceed 100, or be below 1.",
                ImmutableMap.of(),
                ImmutableList.of("${prefix}purge"),
                CommandType.ADMINISTRATION,
                2
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            String content = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
            Member member = event.getMember();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, member) &&
                    PermissionUtil.getAdministratorLevel(guild, member) >= this.getLevelRequired();
            if (content.trim().split(" ").length < 2) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                return;
            }
            if (isAdministrator) {
                int amountToClear;
                try {
                    amountToClear = Integer.parseInt(content.trim().split(" ")[1].trim());
                } catch (NumberFormatException exception) {
                    channel.sendMessage("The amount of messages to be cleared is not a number!").queue();
                    return;
                }
                if (amountToClear > 100) {
                    channel.sendMessage("Sorry, but the limit for message clearing is 100 messages.").queue();
                    return;
                } else if (amountToClear < 1) {
                    channel.sendMessage("Sorry, but the minimum for message clearing is 1 message.").queue();
                    return;
                }
                channel.getHistory().retrievePast(amountToClear).queue(messages -> {
                    try {
                        int amountOfMessagesPurged = messages.size();
                        channel.purgeMessages(messages);
                        channel.sendMessage("Successfully purged " + amountOfMessagesPurged + " message(s).")
                                .queue(message -> message.delete().queueAfter(3, TimeUnit.SECONDS));
                    } catch (InsufficientPermissionException exception) {
                        channel.sendMessage("Cannot clear messages due to insufficient permissions!").queue();
                    }
                });
            } else {
                channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
