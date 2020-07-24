package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.PrefixesDatabaseHelper;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;

public class PrefixCommandHandler extends CommandHandler {
    public PrefixCommandHandler() {
        super(
                "${prefix}prefix",
                "${prefix}prefix <prefix>",
                "Sets the prefix to the specified prefix.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.OTHER,
                3
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            Message message = event.getMessage();
            String content = message.getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
            Member author = event.getMember();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, author) &&
                    PermissionUtil.getAdministratorLevel(guild, author) >= this.getLevelRequired();
            if (content.trim().split(" ").length < 2) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                return;
            }
            if (isAdministrator) {
                String previousPrefix = AdminBot.getInstance().prefix;
                String newPrefix = content.trim().split(" ")[1].trim();
                if (previousPrefix.equals(newPrefix)) {
                    event.getChannel().sendMessage("You can't set the prefix to the same thing, silly.").queue();
                } else {
                    AdminBot.getInstance().prefix = newPrefix;
                    PrefixesDatabaseHelper.overwritePrefixForGuild(event.getGuild().getId(), newPrefix);
                    event.getChannel().sendMessage("Successfully set prefix from " + previousPrefix + " to " + newPrefix + ".").queue();
                }
            } else {
                channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
