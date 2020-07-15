package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;

@CommandHandler(
        name = "${prefix}nickname",
        usage = "${prefix}nickname <member> [nickname]",
        description = "Sets the nickname for the specified member, or resets it if none was specified.",
        aliases = {"${prefix}nick"}
)
public class NicknameCommandHandler {
    public static void onNicknameCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
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
        if (event.getMember().isOwner()) isAdministrator = true;
        if (content.trim().equals(command)) {
            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "nickname <member> [nickname]`.").queue();
            return;
        }
        if (isAdministrator) {
            String args = content.replace(command, "").trim();
            String userId = args.replaceAll("(<@!|>)", "").replaceAll("(?<=\\s).*", "").trim();
            String nickname = args.replaceAll("^[^ ]* ", "").trim();
            boolean resetNickname = false;
            try {
                Long.parseLong(userId);
            } catch (NumberFormatException exception) {
                channel.sendMessage("The member does not exist!").queue();
                return;
            }
            if (userId.equals("")) {
                channel.sendMessage("The member does not exist!").queue();
                return;
            }
            if (nickname.equals("")) {
                resetNickname = true;
            }
            boolean finalResetNickname = resetNickname;
            guild.retrieveMemberById(userId).queue(member -> {
                if (member == null) {
                    throw new IllegalStateException("Member is null!");
                }
                if (finalResetNickname) {
                    member.modifyNickname(member.getUser().getName()).queue(
                            success -> channel.sendMessage("Successfully reset nickname of " + member.getUser().getName() + "!").queue(),
                            failure -> channel.sendMessage("Failed to reset nickname of " + member.getUser().getName() + ".").queue());
                } else {
                    member.modifyNickname(nickname).queue(
                            success -> channel.sendMessage("Successfully set nickname of " + member.getUser().getName() + "!").queue(),
                            failure -> channel.sendMessage("Failed to set nickname of " + member.getUser().getName() + ".").queue());
                }
            });
        } else {
            channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
        }
    }
}
