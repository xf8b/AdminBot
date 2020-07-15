package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.helper.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;

@CommandHandler(
        name = "${prefix}removewarn",
        usage = "${prefix}removewarn <member> <warnId> <reason>",
        description = "Removes the warning with the specified warnId and reason.\n" +
                "If the warnId is `all`, all of the warnings with the same reason will be removed.\n" +
                "If the reason is `all`, all warnings are removed.",
        aliases = {"${prefix}removewarnings", "${prefix}removewarns"}
)
public class RemoveWarnCommandHandler {
    public static void onRemoveWarnCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        boolean isAdministrator = false;
        String command = content.split(" ")[0];
        for (Role role : event.getMember().getRoles()) {
            String id = role.getId();
            if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, id)) {
                isAdministrator = true;
            }
        }
        if (content.trim().equals(command)) {
            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + command + " <member> <warnId> <reason>`.").queue();
            return;
        }
        if (event.getMember().isOwner() || isAdministrator) {
            String args = content.replace(command, "").trim();
            String userId = args.replaceAll("(<@!|>)", "").replaceAll("(?<=\\s).*", "").trim();
            String warnId = args.replaceAll("(<@!)\\d+(>)", "").trim().split(" ")[0];
            String reason = args.replaceAll("(<@!)\\d+(>)", "").trim().replaceFirst("(\\w+\\s+)", "");
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
            boolean checkIfWarnExists = true;
            if (reason.equals("all")) {
                checkIfWarnExists = false;
            }
            boolean removeAllWarnsWithSameName = false;
            if (warnId.equals("all")) {
                removeAllWarnsWithSameName = true;
            }
            if (!WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId, reason) && checkIfWarnExists) {
                channel.sendMessage("The user does not have a warn with that reason!").queue();
            } else {
                if (!checkIfWarnExists) {
                    WarnsDatabaseHelper.removeWarnsFromUserForGuild(guildId, userId, removeAllWarnsWithSameName ? null : warnId, null);
                } else {
                    WarnsDatabaseHelper.removeWarnsFromUserForGuild(guildId, userId, null, reason);
                }
                guild.retrieveMemberById(userId).queue(member -> channel.sendMessage("Successfully removed warn(s) for " + member.getAsMention() + ".").queue());
            }
        } else {
            channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
        }
    }
}
