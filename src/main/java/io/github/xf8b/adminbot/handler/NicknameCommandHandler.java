package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.sql.SQLException;

public class NicknameCommandHandler extends CommandHandler {
    public NicknameCommandHandler() {
        super(
                "${prefix}nickname",
                "${prefix}nickname <member> [nickname]",
                "Sets the nickname of the specified member, or resets it if none was provided.",
                ImmutableMap.of(),
                ImmutableList.of("${prefix}nick"),
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
            Member author = event.getMember();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, author) &&
                    PermissionUtil.getAdministratorLevel(guild, author) >= this.getLevelRequired();
            if (content.trim().split(" ").length < 2) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                return;
            }
            if (isAdministrator) {
                String userId = content.trim().split(" ")[1].replaceAll("[<@!>]", "").trim();
                boolean resetNickname = false;
                String nickname = "";
                if (content.trim().split(" ").length < 3) {
                    resetNickname = true;
                } else {
                    nickname = content.trim().substring(content.trim().indexOf(" ", content.trim().indexOf(" ") + 1) + 1).trim();
                }
                try {
                    Long.parseLong(userId);
                } catch (NumberFormatException exception) {
                    channel.sendMessage("The member does not exist!").queue();
                    return;
                }
                boolean finalResetNickname = resetNickname;
                String finalNickname = nickname;
                guild.retrieveMemberById(userId).queue(member -> {
                    if (member == null) {
                        throw new IllegalStateException("Member is null!");
                    } else if (!guild.getSelfMember().canInteract(member)) {
                        channel.sendMessage("Cannot set/reset nickname of member due to insufficient permissions!").queue();
                        return;
                    }
                    if (finalResetNickname) {
                        member.modifyNickname(member.getUser().getName()).queue(
                                success -> channel.sendMessage("Successfully reset nickname of " + member.getUser().getName() + "!").queue(),
                                failure -> channel.sendMessage("Failed to reset nickname of " + member.getUser().getName() + ".").queue());
                    } else {
                        member.modifyNickname(finalNickname).queue(
                                success -> channel.sendMessage("Successfully set nickname of " + member.getUser().getName() + "!").queue(),
                                failure -> channel.sendMessage("Failed to set nickname of " + member.getUser().getName() + ".").queue());
                    }
                }, throwable -> {
                    if (throwable instanceof ErrorResponseException) {
                        if (((ErrorResponseException) throwable).getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) {
                            channel.sendMessage("The member is not in the guild!").queue();
                        }
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
