package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

public class WarnsCommandHandler extends CommandHandler {
    public WarnsCommandHandler() {
        super(
                "${prefix}warns",
                "${prefix}warns <member>",
                "Gets the warns for the specified member.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.ADMINISTRATION,
                1
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            String content = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
            String guildId = guild.getId();
            Member author = event.getMember();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, author) &&
                    PermissionUtil.getAdministratorLevel(guild, author) >= this.getLevelRequired();
            if (content.trim().split(" ").length < 2) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                return;
            }
            if (isAdministrator) {
                String userId = content.trim().split(" ")[1].trim().replaceAll("[<@!>]", "").trim();
                try {
                    Long.parseLong(userId);
                } catch (NumberFormatException exception) {
                    channel.sendMessage("The member does not exist!").queue();
                    return;
                }
                guild.retrieveMemberById(userId).queue(member -> {
                    if (member == null) {
                        throw new IllegalStateException("Member is null!");
                    }
                });
                String username = event.getMessage().getContentDisplay().replace(AdminBot.getInstance().prefix + "warns", "").trim().replace("@", "").trim();
                Multimap<String, String> warns = WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId);
                if (warns.isEmpty()) {
                    channel.sendMessage("The user has no warnings.").queue();
                } else {
                    String[] warnsFormatted = {"", ""};
                    warns.forEach((reason, warnId) -> {
                        warnsFormatted[0] = warnsFormatted[0].concat(warnId + "\n");
                        warnsFormatted[1] = warnsFormatted[1].concat(reason + "\n");
                    });
                    warnsFormatted[0] = warnsFormatted[0].replaceAll("[\n]*$", "");
                    warnsFormatted[1] = warnsFormatted[1].replaceAll("[\n]*$", "");
                    MessageEmbed embed = new EmbedBuilder()
                            .setTitle("Warnings For `" + username + "`")
                            .addField("Warn ID", warnsFormatted[0], true)
                            .addField("Reason", warnsFormatted[1], true)
                            .setColor(Color.BLUE)
                            .build();
                    channel.sendMessage(embed).queue();
                }
            } else {
                channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
